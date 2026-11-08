package io.ymusic.ui.playhistory

import io.ymusic.player.PlayableItem
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Round-trip + corrupt-JSON soft-fail for Play again (Phase 11/12). A bad row must not take down
 * the sessions list or crash resume.
 */
class PlayAgainStoreTest {

    private val driver = playHistoryJdbcDriver(":memory:")
    private val store = PlayAgainStore(driver)

    @AfterTest
    fun close() {
        driver.close()
    }

    private fun session(
        id: String = "q1",
        size: Int = 1,
        thumbs: List<String?> = listOf("https://example.com/a.jpg"),
    ) = PlayAgainSession(
        queueId = id,
        title = "Saved",
        subtitle = null,
        thumbnailUrl = thumbs.firstOrNull(),
        queueIndex = 0,
        queueSize = size,
        positionMs = 0L,
        durationMs = 60_000L,
        stripStart = 0,
        stripThumbs = thumbs,
        sourceToken = null,
    )

    @Test
    fun save_load_round_trips_local_and_yt_items() = runBlocking {
        val items = listOf(
            PlayableItem.LocalMedia(uri = "file:///a.mp3", title = "A", durationMs = 1_000L),
            PlayableItem.YtMedia(videoId = "abcdefghijk", title = "B", durationMs = 2_000L),
        )
        store.save(session(size = 2, thumbs = listOf(null, null)), items)

        val loaded = store.load("q1")
        assertNotNull(loaded)
        assertEquals("Saved", loaded.first.title)
        assertEquals(2, loaded.second.size)
        assertEquals("file:///a.mp3", (loaded.second[0] as PlayableItem.LocalMedia).uri)
        assertEquals("abcdefghijk", (loaded.second[1] as PlayableItem.YtMedia).videoId)

        assertEquals(1, store.sessions().size)
    }

    @Test
    fun corrupt_items_json_yields_empty_queue_not_crash() = runBlocking {
        store.save(session(), listOf(PlayableItem.LocalMedia(uri = "file:///a.mp3")))
        driver.execute(null, "UPDATE play_again SET items = ? WHERE queue_id = ?", 2) {
            bindString(0, "{not-json")
            bindString(1, "q1")
        }

        val loaded = store.load("q1")
        assertNotNull(loaded, "session metadata still loads")
        assertTrue(loaded.second.isEmpty(), "corrupt items → empty queue")
        assertEquals(1, store.sessions().size, "sessions() survives corrupt thumbs/items path")
    }

    @Test
    fun unknown_item_type_is_skipped() = runBlocking {
        store.save(session(), listOf(PlayableItem.LocalMedia(uri = "file:///a.mp3")))
        driver.execute(null, "UPDATE play_again SET items = ? WHERE queue_id = ?", 2) {
            bindString(0, """[{"type":"alien","uri":"x"},{"type":"local","uri":"file:///ok.mp3"}]""")
            bindString(1, "q1")
        }

        val loaded = store.load("q1")
        assertNotNull(loaded)
        assertEquals(1, loaded.second.size)
        assertEquals("file:///ok.mp3", (loaded.second.single() as PlayableItem.LocalMedia).uri)
    }

    @Test
    fun corrupt_strip_thumbs_lists_session_with_empty_strip() = runBlocking {
        store.save(session(), listOf(PlayableItem.LocalMedia(uri = "file:///a.mp3")))
        driver.execute(null, "UPDATE play_again SET strip_thumbs = ? WHERE queue_id = ?", 2) {
            bindString(0, "[[[")
            bindString(1, "q1")
        }

        val sessions = store.sessions()
        assertEquals(1, sessions.size)
        assertTrue(sessions.single().stripThumbs.isEmpty())
    }

    @Test
    fun missing_queue_returns_null() = runBlocking {
        assertNull(store.load("nope"))
    }
}
