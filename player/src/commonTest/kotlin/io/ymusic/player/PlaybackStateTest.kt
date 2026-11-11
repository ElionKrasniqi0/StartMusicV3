package io.ymusic.player

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackStateTest {
    @Test
    fun ready_to_load_matches_legacy_contract() {
        assertFalse(PlaybackState.Uninitialized(isInitializing = false).readyToLoad)
        assertFalse(PlaybackState.Uninitialized(isInitializing = true).readyToLoad)
        assertTrue(PlaybackState.Idle.readyToLoad)
        assertTrue(PlaybackState.Prepared(true, buffering = true, ended = false).readyToLoad)
        assertTrue(PlaybackState.Error(PlayerError("E1", canRetry = true)).readyToLoad)
        assertTrue(PlaybackState.Error(PlayerError("E1", canRetry = false)).readyToLoad)
        assertFalse(PlaybackState.Error(PlayerError("E1", canRetry = true, needReset = true)).readyToLoad)
    }

    @Test
    fun isPlaying_false_on_error_and_ended_even_if_intent_true() {
        val queue = QueueState(items = listOf(PlayableItem.YtMedia("vidAAAA1111")), currentIndex = 0)
        assertTrue(
            isPlaying(
                PlaybackStatus(
                    state = PlaybackState.Prepared(playWhenReady = true, buffering = false, ended = false),
                    playWhenReady = true,
                ),
                queue,
            ),
        )
        assertFalse(
            isPlaying(
                PlaybackStatus(
                    state = PlaybackState.Error(PlayerError("E60065", canRetry = true, needReset = true)),
                    playWhenReady = true,
                ),
                queue,
            ),
        )
        assertFalse(
            isPlaying(
                PlaybackStatus(
                    state = PlaybackState.Prepared(playWhenReady = false, buffering = false, ended = true),
                    playWhenReady = true,
                ),
                queue,
            ),
        )
    }
}
