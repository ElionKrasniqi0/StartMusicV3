package io.ymusic.ui.storage

import io.ymusic.download.DownloadService
import io.ymusic.download.DownloadState
import io.ymusic.dynamicengine.yt.ytLogout
import io.ymusic.settings.AppSettings
import io.ymusic.settings.StringBackedSettingsStore
import io.ymusic.settings.ensureUiLanguage
import io.ymusic.settings.put
import io.ymusic.ui.AppGraph
import io.ymusic.ui.PlaybackAudioOnlyKey
import io.ymusic.ui.PlaybackEqKey
import io.ymusic.ui.PlaybackLoopModeKey
import io.ymusic.ui.PlaybackPitchKey
import io.ymusic.ui.PlaybackQualitySettingKey
import io.ymusic.ui.PlaybackShuffleKey
import io.ymusic.ui.PlaybackSpeedKey
import io.ymusic.ui.PlaybackVolumeKey
import io.ymusic.ui.defaultUiLanguageTag
import io.ymusic.ytmedia.cache.MediaCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import projectcommon.error.TechnicalException

/** Outcome of Clear Cache / Clear App Data. */
data class StorageOpResult(
    val freedBytes: Long = 0L,
    val skippedInUse: Int = 0,
    val errorMessage: String? = null,
) {
    operator fun plus(other: StorageOpResult) = StorageOpResult(
        freedBytes = freedBytes + other.freedBytes,
        skippedInUse = skippedInUse + other.skippedInUse,
        errorMessage = listOfNotNull(errorMessage, other.errorMessage)
            .joinToString("; ")
            .ifBlank { null },
    )

    val ok: Boolean get() = errorMessage == null
}

/**
 * Platform pieces the shared maintenance API cannot own (HTTP/image/engine dirs, cookies, scratch).
 * Shells wire a real instance on [AppGraph.storageHooks]; tests use [Noop].
 */
class AppStorageHooks(
    val measureCacheBytes: suspend () -> Long = { 0L },
    val measureAppDataBytes: suspend () -> Long = { 0L },
    val clearPlatformCaches: suspend (activeDownloadIds: Set<String>) -> StorageOpResult =
        { StorageOpResult() },
    val signOutCookies: suspend () -> Unit = {},
    val wipeEphemeralFiles: suspend () -> Unit = {},
    val deleteDownloadFiles: suspend (uris: List<String>) -> StorageOpResult = { StorageOpResult() },
) {
    companion object {
        val Noop = AppStorageHooks()
    }
}

/** Evict regenerable player-cache spans; pinned (in-use) spans are skipped by LRU. */
internal fun clearPlayerCacheBlobs(cache: MediaCache): StorageOpResult {
    val before = runCatching { cache.blobs.totalSize() }.getOrDefault(0L)
    val max = cache.blobs.maxSize
    return runCatching {
        cache.blobs.setMaxSize(0L)
        cache.index.cleanup(cache.blobs.keys())
        cache.blobs.setMaxSize(max)
        val after = cache.blobs.totalSize()
        StorageOpResult(
            freedBytes = (before - after).coerceAtLeast(0L),
            skippedInUse = if (after > 0L) 1 else 0,
        )
    }.getOrElse { e ->
        runCatching { cache.blobs.setMaxSize(max) }
        StorageOpResult(errorMessage = e.message ?: e.toString())
    }
}

/**
 * Clear regenerable caches only — downloads, playlists, settings, and cookies stay.
 * Safe to call while playback continues; in-use player spans are skipped.
 */
suspend fun AppGraph.clearCache(): StorageOpResult = withContext(Dispatchers.IO) {
    var result = StorageOpResult()
    playerCache?.let { result += clearPlayerCacheBlobs(it) }
    // Protect any non-terminal scratch (active + paused/failed-resumable), not only isActive.
    // Fail closed if the queue cannot be read — never delete every scratch by accident.
    val protectedDownloads = when (val service = downloads) {
        null -> emptySet()
        else -> {
            val snap = runCatching { service.snapshot() }
            if (snap.isFailure) {
                result += StorageOpResult(
                    errorMessage = snap.exceptionOrNull()?.message ?: "download snapshot failed",
                )
                imageLoader?.trimMemoryCache(0f)
                return@withContext result
            }
            snap.getOrDefault(emptyList()).filter { !it.state.isTerminal }.map { it.id }.toSet()
        }
    }
    result += runCatching { storageHooks.clearPlatformCaches(protectedDownloads) }
        .getOrElse { StorageOpResult(errorMessage = it.message ?: it.toString()) }
    imageLoader?.trimMemoryCache(0f)
    result
}

/**
 * Controlled app reset (not an OS dataDir wipe). Stops playback/downloads, clears user-resettable
 * stores, signs out, recreates defaults. [deleteDownloads] also removes completed download files.
 */
suspend fun AppGraph.clearAppData(deleteDownloads: Boolean): StorageOpResult {
    stopPlayerForDataReset()
    return withContext(Dispatchers.IO) {
        try {
            temporaryUris.value = emptySet()
            val downloadUris = mutableListOf<String>()
            downloads?.let { service ->
                val tasks = runCatching { service.snapshot() }.getOrDefault(emptyList())
                for (task in tasks) {
                    if (!task.state.isTerminal) runCatching { service.cancel(task.id) }
                    task.fileUri?.let { downloadUris += it }
                }
            }

            var result = clearCache()
            runCatching { storageHooks.wipeEphemeralFiles() }

            playHistory?.clearAllUserHistory()
            mediaStore?.clearAllUserData()
            sponsorBlock.clearAll()
            downloads?.let { clearDownloadQueue(it) }
            if (deleteDownloads && downloadUris.isNotEmpty()) {
                result += storageHooks.deleteDownloadFiles(downloadUris)
            }

            resetSettingsToDefaults()
            // Engine logout drops auth cookies in the jar; shell hook wipes the platform store.
            runCatching { engines.ytLogout() }
            storageHooks.signOutCookies()

            localeEpoch.value++
            account.invalidateAfterDataReset()

            result
        } catch (c: kotlinx.coroutines.CancellationException) {
            throw c
        } catch (t: Throwable) {
            logger?.fork("storage-reset")?.captureError(t, "clear app data failed")
            throw TechnicalException("E50009", "clear app data failed", cause = t)
        }
    }
}

/** Player SPI is main-thread only; JVM unit tests may lack a Main dispatcher (StubPlayer is fine). */
private suspend fun AppGraph.stopPlayerForDataReset() {
    try {
        withContext(Dispatchers.Main) {
            runCatching { player.pause() }
            runCatching { player.clearQueue() }
        }
    } catch (_: IllegalStateException) {
        runCatching { player.pause() }
        runCatching { player.clearQueue() }
    }
}

private suspend fun AppGraph.resetSettingsToDefaults() {
    val store = settings
    if (store is StringBackedSettingsStore) {
        store.clearAll()
    } else {
        store.put(AppSettings.searchHistory, "")
        store.put(AppSettings.libraryFavorites, emptySet())
        store.put(AppSettings.downloadLocation, "")
        store.put(AppSettings.quickDownloadPreset, "")
        store.put(AppSettings.deviceId, "")
        store.put(AppSettings.theme, AppSettings.theme.default)
        store.put(AppSettings.themeAccent, "")
        store.put(AppSettings.uiLanguage, "")
        store.put(AppSettings.playerCacheMaxMb, AppSettings.playerCacheMaxMb.default)
        store.put(AppSettings.crashReports, AppSettings.crashReports.default)
        store.put(AppSettings.stats, AppSettings.stats.default)
        store.put(AppSettings.enablePip, AppSettings.enablePip.default)
        store.put(AppSettings.sponsorBlockEnabled, AppSettings.sponsorBlockEnabled.default)
        store.put(AppSettings.showFps, AppSettings.showFps.default)
        store.put(AppSettings.autoDownloadUpdates, AppSettings.autoDownloadUpdates.default)
        store.put(AppSettings.audioFocus, AppSettings.audioFocus.default)
        store.put(AppSettings.systemAudioEffects, AppSettings.systemAudioEffects.default)
        store.putString(PlaybackQualitySettingKey, "")
        store.putString(PlaybackAudioOnlyKey, "")
        store.putString(PlaybackVolumeKey, "")
        store.putString(PlaybackSpeedKey, "")
        store.putString(PlaybackPitchKey, "")
        store.putString(PlaybackEqKey, "")
        store.putString(PlaybackShuffleKey, "")
        store.putString(PlaybackLoopModeKey, "")
    }
    store.ensureUiLanguage { defaultUiLanguageTag() }
}

private suspend fun clearDownloadQueue(service: DownloadService) {
    val tasks = runCatching { service.snapshot() }.getOrDefault(emptyList())
    for (task in tasks) {
        when (task.state) {
            DownloadState.COMPLETED,
            DownloadState.FAILED,
            DownloadState.PAUSED,
            DownloadState.QUEUED,
            DownloadState.CANCELED,
            -> runCatching { service.remove(task.id) }
            else -> {
                runCatching { service.cancel(task.id) }
                runCatching { service.remove(task.id) }
            }
        }
    }
}
