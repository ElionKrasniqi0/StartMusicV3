package io.ymusic.ios

import io.ymusic.player.Player
import io.ymusic.player.isPlaying
import io.ymusic.settings.AppSettings
import io.ymusic.settings.SettingsStore
import io.ymusic.settings.flow
import kmpcommon.log.Logger
import kmpcommon.log.info
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionOptionKey
import platform.AVFAudio.AVAudioSessionInterruptionOptionShouldResume
import platform.AVFAudio.AVAudioSessionInterruptionTypeBegan
import platform.AVFAudio.AVAudioSessionInterruptionTypeEnded
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.AVAudioSessionRouteChangeNotification
import platform.AVFAudio.AVAudioSessionRouteChangeReasonKey
import platform.AVFAudio.AVAudioSessionRouteChangeReasonOldDeviceUnavailable
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.darwin.NSObjectProtocol

/**
 * iOS counterpart of Android [io.ymusic.android.AudioFocusCoordinator]: pause on audio interruption
 * (calls / Siri / other apps) and when the output route disappears (headphones unplugged), with
 * optional resume when the system says the interruption ended cleanly. Does not replace
 * [connectIosMediaSession] — Lock Screen / Control Center stay separate.
 */
@OptIn(ExperimentalForeignApi::class)
fun connectIosAudioInterruptions(
    player: Player,
    settings: SettingsStore,
    scope: CoroutineScope,
    logger: Logger? = null,
) {
    val log = logger?.fork("audio-session")
    var resumeAfterInterruption = false
    val observers = mutableListOf<NSObjectProtocol>()

    fun playing(): Boolean = isPlaying(player.status.value, player.queue.value)

    val center = NSNotificationCenter.defaultCenter
    observers += center.addObserverForName(
        name = AVAudioSessionInterruptionNotification,
        `object` = AVAudioSession.sharedInstance(),
        queue = null,
    ) { notification ->
        val info = notification?.userInfo ?: return@addObserverForName
        val type = (info[AVAudioSessionInterruptionTypeKey] as? NSNumber)?.unsignedLongValue?.toLong()
        when (type) {
            AVAudioSessionInterruptionTypeBegan.toLong() -> {
                log?.info("interruption began playing=${playing()}")
                if (playing()) {
                    resumeAfterInterruption = true
                    player.pause()
                }
            }
            AVAudioSessionInterruptionTypeEnded.toLong() -> {
                val options = (info[AVAudioSessionInterruptionOptionKey] as? NSNumber)?.unsignedLongValue?.toLong() ?: 0L
                val shouldResume = (options and AVAudioSessionInterruptionOptionShouldResume.toLong()) != 0L
                log?.info("interruption ended shouldResume=$shouldResume resumeFlag=$resumeAfterInterruption")
                if (shouldResume && resumeAfterInterruption) {
                    resumeAfterInterruption = false
                    if (!playing()) player.play()
                } else {
                    resumeAfterInterruption = false
                }
            }
        }
    }

    observers += center.addObserverForName(
        name = AVAudioSessionRouteChangeNotification,
        `object` = AVAudioSession.sharedInstance(),
        queue = null,
    ) { notification ->
        val reason = (notification?.userInfo?.get(AVAudioSessionRouteChangeReasonKey) as? NSNumber)
            ?.unsignedLongValue?.toLong()
        if (reason == AVAudioSessionRouteChangeReasonOldDeviceUnavailable.toLong() && playing()) {
            log?.info("route old device unavailable, pause")
            resumeAfterInterruption = false
            player.pause()
        }
    }

    scope.launch {
        // Keep category active; mode changes from Settings are Android-specific — iOS always uses
        // playback category. Observing the setting still logs policy for diagnostics.
        settings.flow(AppSettings.audioFocus)
            .map { it }
            .distinctUntilChanged()
            .collect { mode ->
                log?.info("audioFocus setting=$mode (iOS uses AVAudioSession interruptions)")
            }
    }

    log?.info("AVAudioSession interruption + route observers hooked (${observers.size})")
}
