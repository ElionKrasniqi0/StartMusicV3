package io.ymusic.ui

import io.ymusic.dynamicengine.yt.YtFeedItem
import io.ymusic.dynamicengine.yt.YtFeedPage
import io.ymusic.dynamicengine.yt.YtSection
import io.ymusic.ui.playhistory.PlayHistoryStore
import io.ymusic.ui.playhistory.playHistoryJdbcDriver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Featured (Home shelves) must keep-content reload when listening history changes — without
 * waiting for process death. Recently played already observes the same [PlayHistoryStore.changes].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FeaturedHomeReloadTest {

    private val driver = playHistoryJdbcDriver(":memory:")
    private val history = PlayHistoryStore(driver)

    @AfterTest
    fun close() {
        driver.close()
    }

    private fun page(title: String) = YtFeedPage(
        sections = listOf(
            YtSection(
                items = listOf(
                    YtFeedItem(kind = YtFeedItem.KIND_VIDEO, id = "abcdefghijk", title = title),
                ),
            ),
        ),
    )

    private suspend fun FeedSession.awaitReady(): Pager.State.Ready {
        val state = pager.state.first { it is Pager.State.Ready && !it.refreshing }
        return state as Pager.State.Ready
    }

    @Test
    fun featured_updatesAutomaticallyAfterListening_withoutApplicationRestart() = runTest {
        var generation = 0
        val featured = FeedSession(
            backgroundScope,
            reloadFetch = {
                generation++
                page("FeaturedGen$generation")
            },
            fetch = {
                generation++
                page("FeaturedGen$generation")
            },
        )
        featured.pager.refresh()
        runCurrent()
        featured.awaitReady()
        val afterBootstrap = generation
        assertEquals(1, afterBootstrap)

        backgroundScope.bindFeaturedHomeReload(history) { featured }

        history.saveYoutubeMetadata("vidAAAA1111", "Song One", durationMs = 180_000L)
        history.trackYoutube("vidAAAA1111", 5_000L)
        advanceTimeBy(FeaturedHomeReloadDebounceMs)
        runCurrent()
        val ready = featured.awaitReady()

        assertTrue(generation > afterBootstrap, "Featured keep-content reload after listen")
        assertEquals("FeaturedGen$generation", ready.sections.single().items.single().title)
    }

    @Test
    fun oneSong_triggersFeaturedReload() = runTest {
        var reloads = 0
        val featured = FeedSession(
            backgroundScope,
            reloadFetch = {
                reloads++
                page("AfterListen")
            },
            fetch = { page("Bootstrap") },
        )
        featured.pager.refresh()
        runCurrent()
        featured.awaitReady()

        backgroundScope.bindFeaturedHomeReload(history) { featured }
        history.trackYoutube("vidAAAA1111", 2_000L)
        advanceTimeBy(FeaturedHomeReloadDebounceMs)
        runCurrent()
        val ready = featured.awaitReady()

        assertEquals(1, reloads)
        assertEquals("AfterListen", ready.sections.single().items.single().title)
    }

    @Test
    fun twoSongs_updateFeaturedWithoutRestart() = runTest {
        var reloads = 0
        val featured = FeedSession(
            backgroundScope,
            reloadFetch = {
                reloads++
                page("AfterTwo")
            },
            fetch = { page("Bootstrap") },
        )
        featured.pager.refresh()
        runCurrent()
        featured.awaitReady()

        backgroundScope.bindFeaturedHomeReload(history) { featured }
        history.trackYoutube("vidAAAA1111", 1_000L)
        history.trackYoutube("vidBBBB2222", 1_000L)
        advanceTimeBy(FeaturedHomeReloadDebounceMs)
        runCurrent()
        featured.awaitReady()

        assertTrue(reloads >= 1)
        assertEquals("AfterTwo", (featured.pager.state.value as Pager.State.Ready).sections.single().items.single().title)
    }

    @Test
    fun fiveSongs_coalesceIntoDebouncedReloads() = runTest {
        var reloads = 0
        val featured = FeedSession(
            backgroundScope,
            reloadFetch = {
                reloads++
                page("Batch")
            },
            fetch = { page("Bootstrap") },
        )
        featured.pager.refresh()
        runCurrent()
        featured.awaitReady()

        backgroundScope.bindFeaturedHomeReload(history) { featured }
        repeat(5) { i ->
            val id = "vid${('A' + i)}${"x".repeat(9)}"
            history.trackYoutube(id, 1_000L)
        }
        advanceTimeBy(FeaturedHomeReloadDebounceMs)
        runCurrent()
        featured.awaitReady()

        assertTrue(reloads in 1..2, "reloads=$reloads")
    }

    @Test
    fun missingHomeSession_doesNotCrash() = runTest {
        backgroundScope.bindFeaturedHomeReload(history) { null }
        history.trackYoutube("vidAAAA1111", 1_000L)
        advanceTimeBy(FeaturedHomeReloadDebounceMs)
        runCurrent()
    }

    @Test
    fun nullPlayHistory_bindsNothing() = runTest {
        assertEquals(null, backgroundScope.bindFeaturedHomeReload(null) { error("unused") })
    }
}
