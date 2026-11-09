package io.ymusic.ui.playhistory

import io.ymusic.ui.toYtMedia
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Recently played / Top tracks ranking: recency order, single row per video, playable cards.
 */
class PlayHistoryStoreTest {

    private val driver = playHistoryJdbcDriver(":memory:")
    private val store = PlayHistoryStore(driver)

    @AfterTest
    fun close() {
        driver.close()
    }

    @Test
    fun trackYoutube_movesNewestFirst_andDedupes() = runBlocking {
        store.saveYoutubeMetadata("vidAAAA1111", "Song A", artist = "Artist A", durationMs = 120_000L)
        store.saveYoutubeMetadata("vidBBBB2222", "Song B", artist = "Artist B", durationMs = 90_000L)
        store.trackYoutube("vidAAAA1111", 1_000L)
        store.trackYoutube("vidBBBB2222", 1_000L)
        store.trackYoutube("vidAAAA1111", 500L)

        val recent = store.youtubeRecent(limit = 10)
        assertEquals(listOf("vidAAAA1111", "vidBBBB2222"), recent.map { it.id })
        assertEquals(1, recent.count { it.id == "vidAAAA1111" })
        assertEquals("Song A", recent.first().title)
        assertEquals("Artist A", recent.first().subtitleLine1)
    }

    @Test
    fun youtubeRecent_thinCardWhenMetadataMissing_stillPlayable() = runBlocking {
        store.trackYoutube("vidCCCC3333", 2_000L)
        val recent = store.youtubeRecent(limit = 5)
        assertEquals(1, recent.size)
        assertEquals("vidCCCC3333", recent.single().id)
        assertTrue(recent.single().thumbs.isNotEmpty(), "thumb URL for playback/UI")
    }

    @Test
    fun zeroWatchTime_doesNotCreateHistory() = runBlocking {
        store.saveYoutubeMetadata("vidDDDD4444", "Skipped", durationMs = 10_000L)
        store.trackYoutube("vidDDDD4444", 0L)
        assertTrue(store.youtubeRecent().isEmpty())
    }

    @Test
    fun recentCard_mapsToPlayableYtMedia() = runBlocking {
        store.saveYoutubeMetadata(
            "vidEEEE5555",
            "Playable",
            artist = "Band",
            thumbnailUrl = "https://example.com/t.jpg",
            durationMs = 61_000L,
        )
        store.trackYoutube("vidEEEE5555", 3_000L)
        val media = store.youtubeRecent().single().toYtMedia()
        assertEquals("vidEEEE5555", media.videoId)
        assertEquals("Playable", media.title)
        assertEquals("Band", media.artist)
        assertEquals("https://example.com/t.jpg", media.thumbnailUrl)
    }
}
