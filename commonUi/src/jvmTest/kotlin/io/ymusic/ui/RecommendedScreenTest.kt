package io.ymusic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.compose.ui.test.waitUntilDoesNotExist
import io.ymusic.dynamicengine.CacheStore
import io.ymusic.dynamicengine.DynamicEngineManager
import io.ymusic.dynamicengine.desktopEngineCacheStore
import io.ymusic.dynamicengine.embeddedEngineJsSource
import io.ymusic.dynamicengine.yt.YtFeedItem
import io.ymusic.dynamicengine.yt.YtFeedPage
import io.ymusic.dynamicengine.yt.YtSection
import io.ymusic.dynamicengine.yt.getRecommendedFeed
import io.ymusic.dynamicengine.yt.handleWebViewPlayNext
import io.ymusic.player.PlayableItem
import io.ymusic.player.QueueState
import io.ymusic.settings.InMemorySettingsStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import projectcommon.error.TechnicalException
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Discover's Recommended (Star) tab: empty/Reload must stay until usable recommendations exist.
 * Playing one song must never blank the page via `queue.currentItem != null`.
 */
@OptIn(ExperimentalTestApi::class)
class RecommendedScreenTest {

    private fun cacheStore(): CacheStore {
        val dir = java.io.File(System.getProperty("java.io.tmpdir"), "ymusic-recommended-ui-test-${System.nanoTime()}")
        dir.deleteRecursively()
        return desktopEngineCacheStore(dir)
    }

    private fun offlineClient() = CannedHttp { request, _ ->
        val url = request.url.toString()
        if (request.method == "GET" && url.contains("www.youtube.com") && !url.contains("youtubei")) {
            textResponse(request, HOMEPAGE_FIXTURE, "text/html")
        } else {
            textResponse(request, "{}", "application/json")
        }
    }

    private val watchNextContents = """
        [{"endScreenVideoRenderer":{"videoId":"N30CFk9fHwg","title":{"simpleText":"A related song"},
        "shortBylineText":{"runs":[{"text":"Trạm Acoustic"}]},"lengthText":{"simpleText":"52:30"},
        "shortViewCountText":{"simpleText":"108K views"},
        "thumbnail":{"thumbnails":[{"url":"https://i.ytimg.com/vi/N30CFk9fHwg/hqdefault.jpg","width":168,"height":94}]}}}]
    """.trimIndent().replace("\n", "")

    private fun ytSong(id: String, title: String) =
        PlayableItem.YtMedia(videoId = id, title = title, durationMs = 180_000L)

    private fun seedQueue(player: StubPlayer, vararg songs: PlayableItem.YtMedia) {
        player.queueState.value = QueueState(items = songs.toList(), currentIndex = 0)
    }

    @Test
    fun noSongsPlayed_showsEmptyStateAndReload() {
        Locale.setDefault(Locale.ENGLISH)
        val engines = DynamicEngineManager(offlineClient(), appVersion = 1, cacheStore = cacheStore(), jsSource = embeddedEngineJsSource())
        runBlocking { engines.start() }
        try {
            runComposeUiTest {
                setContent {
                    App(AppGraph(settings = InMemorySettingsStore(), engines = engines, player = StubPlayer(), playerSurface = {}))
                }
                onAllNodesWithActionLabel("Recommended").onFirst().performClick()
                waitUntilAtLeastOneExists(hasText("Play a few songs", substring = true), 20_000)
                onNodeWithText("Reload").assertIsDisplayed()
            }
        } finally {
            runBlocking { engines.close() }
        }
    }

    @Test
    fun playingExactlyOneSong_doesNotHideEmptyStateOrReload_whenRecommendationsAreUnavailable() {
        Locale.setDefault(Locale.ENGLISH)
        val engines = DynamicEngineManager(offlineClient(), appVersion = 1, cacheStore = cacheStore(), jsSource = embeddedEngineJsSource())
        val player = StubPlayer()
        runBlocking { engines.start() }
        try {
            runComposeUiTest {
                setContent {
                    App(AppGraph(settings = InMemorySettingsStore(), engines = engines, player = player, playerSurface = {}))
                }
                onAllNodesWithActionLabel("Recommended").onFirst().performClick()
                waitUntilAtLeastOneExists(hasText("Play a few songs", substring = true), 20_000)

                seedQueue(player, ytSong("abcdefghijk", "Only One"))
                waitForIdle()

                onNodeWithText("Play a few songs", substring = true).assertIsDisplayed()
                onNodeWithText("Reload").assertIsDisplayed()
            }
        } finally {
            runBlocking { engines.close() }
        }
    }

    @Test
    fun twoOrMoreSongsPlayed_stillShowsEmptyUntilRecommendationsArrive() {
        Locale.setDefault(Locale.ENGLISH)
        val engines = DynamicEngineManager(offlineClient(), appVersion = 1, cacheStore = cacheStore(), jsSource = embeddedEngineJsSource())
        val player = StubPlayer()
        runBlocking { engines.start() }
        try {
            runComposeUiTest {
                setContent {
                    App(AppGraph(settings = InMemorySettingsStore(), engines = engines, player = player, playerSurface = {}))
                }
                onAllNodesWithActionLabel("Recommended").onFirst().performClick()
                waitUntilAtLeastOneExists(hasText("Play a few songs", substring = true), 20_000)

                seedQueue(
                    player,
                    ytSong("abcdefghijk", "First"),
                    ytSong("lmnopqrstuv", "Second"),
                    ytSong("wxyzabcdefg", "Third"),
                )
                waitForIdle()

                onNodeWithText("Play a few songs", substring = true).assertIsDisplayed()
                onNodeWithText("Reload").assertIsDisplayed()
            }
        } finally {
            runBlocking { engines.close() }
        }
    }

    @Test
    fun recommendationsUnavailableAfterSeveralSongs_keepsEmptyAndReload() {
        Locale.setDefault(Locale.ENGLISH)
        val engines = DynamicEngineManager(offlineClient(), appVersion = 1, cacheStore = cacheStore(), jsSource = embeddedEngineJsSource())
        val player = StubPlayer()
        runBlocking { engines.start() }
        try {
            runComposeUiTest {
                setContent {
                    App(AppGraph(settings = InMemorySettingsStore(), engines = engines, player = player, playerSurface = {}))
                }
                onAllNodesWithActionLabel("Recommended").onFirst().performClick()
                waitUntilAtLeastOneExists(hasText("Play a few songs", substring = true), 20_000)
                seedQueue(
                    player,
                    ytSong("songAAAAAAA", "A"),
                    ytSong("songBBBBBBB", "B"),
                    ytSong("songCCCCCCC", "C"),
                    ytSong("songDDDDDDD", "D"),
                )
                waitForIdle()
                // Queue depth alone never unlocks recommendations — only harvested watch-next does.
                onNodeWithText("Play a few songs", substring = true).assertIsDisplayed()
                onNodeWithText("Reload").assertIsDisplayed()
            }
        } finally {
            runBlocking { engines.close() }
        }
    }

    @Test
    fun emptyFeedResponse_keepsEmptyStateAndReload() {
        Locale.setDefault(Locale.ENGLISH)
        val engines = DynamicEngineManager(offlineClient(), appVersion = 1, cacheStore = cacheStore(), jsSource = embeddedEngineJsSource())
        val player = StubPlayer()
        runBlocking { engines.start() }
        try {
            runComposeUiTest {
                setContent {
                    App(AppGraph(settings = InMemorySettingsStore(), engines = engines, player = player, playerSurface = {}))
                }
                onAllNodesWithActionLabel("Recommended").onFirst().performClick()
                waitUntilAtLeastOneExists(hasText("Play a few songs", substring = true), 20_000)
                seedQueue(player, ytSong("abcdefghijk", "One"))
                onNodeWithText("Reload").performClick()
                waitUntilAtLeastOneExists(hasText("Play a few songs", substring = true), 20_000)
                onNodeWithText("Reload").assertIsDisplayed()
            }
        } finally {
            runBlocking { engines.close() }
        }
    }

    @Test
    fun failedFeedRequest_keepsValidErrorUi() {
        Locale.setDefault(Locale.ENGLISH)
        val engines = DynamicEngineManager(offlineClient(), appVersion = 1, jsSource = embeddedEngineJsSource())
        val player = StubPlayer()
        seedQueue(player, ytSong("abcdefghijk", "One"))
        runComposeUiTest {
            setContent {
                YMusicTheme {
                    val graph = AppGraph(settings = InMemorySettingsStore(), engines = engines, player = player, playerSurface = {})
                    val scope = rememberCoroutineScope()
                    val coordinator = PlaybackCoordinator(graph, scope)
                    val pager = Pager(scope) {
                        throw TechnicalException("E29999", "recommended feed failed")
                    }.also { it.refresh() }
                    Box(Modifier.fillMaxSize()) {
                        FeedList(
                            pager,
                            coordinator,
                            emptyMessage = "Play a few songs and your recommendations will show up here",
                        )
                    }
                }
            }
            waitUntilAtLeastOneExists(hasText("Something went wrong", substring = true), 20_000)
            onNodeWithText("Retry").assertIsDisplayed()
        }
    }

    @Test
    fun refreshInProgress_keepsEmptyStateVisible() {
        Locale.setDefault(Locale.ENGLISH)
        val engines = DynamicEngineManager(offlineClient(), appVersion = 1, jsSource = embeddedEngineJsSource())
        val player = StubPlayer()
        seedQueue(player, ytSong("abcdefghijk", "One"))
        val gate = CompletableDeferred<Unit>()
        val fetches = AtomicInteger(0)
        runComposeUiTest {
            setContent {
                YMusicTheme {
                    val graph = AppGraph(settings = InMemorySettingsStore(), engines = engines, player = player, playerSurface = {})
                    val scope = rememberCoroutineScope()
                    val coordinator = PlaybackCoordinator(graph, scope)
                    val pager = Pager(scope) {
                        fetches.incrementAndGet()
                        if (fetches.get() == 1) {
                            YtFeedPage(sections = emptyList())
                        } else {
                            gate.await()
                            YtFeedPage(sections = emptyList())
                        }
                    }.also { it.refresh() }
                    Box(Modifier.fillMaxSize()) {
                        FeedList(
                            pager,
                            coordinator,
                            emptyMessage = "Play a few songs and your recommendations will show up here",
                        )
                    }
                }
            }
            waitUntilAtLeastOneExists(hasText("Play a few songs", substring = true), 20_000)
            onNodeWithText("Reload").performClick()
            waitForIdle()
            // Keep-content reload: empty message stays while the second fetch is gated.
            onNodeWithText("Play a few songs", substring = true).assertIsDisplayed()
            onNodeWithText("Reload").assertIsDisplayed()
            gate.complete(Unit)
            waitUntilAtLeastOneExists(hasText("Play a few songs", substring = true), 20_000)
        }
    }

    @Test
    fun recommendationsSuccessfullyLoaded_hidesEmptyState() {
        Locale.setDefault(Locale.ENGLISH)
        val engines = DynamicEngineManager(offlineClient(), appVersion = 1, cacheStore = cacheStore(), jsSource = embeddedEngineJsSource())
        runBlocking { engines.start() }
        try {
            runComposeUiTest {
                val player = StubPlayer()
                val graph = AppGraph(settings = InMemorySettingsStore(), engines = engines, player = player, playerSurface = {})
                setContent { App(graph) }

                onAllNodesWithActionLabel("Recommended").onFirst().performClick()
                waitUntilAtLeastOneExists(hasText("Play a few songs", substring = true), 20_000)

                runBlocking { awaitHarvest(engines, "yuuWdm5tBD0", watchNextContents) }
                onNodeWithActionLabel("Home").performClick()
                onAllNodesWithActionLabel("Recommended").onFirst().performClick()
                waitUntilAtLeastOneExists(hasText("A related song", substring = true), 20_000)
                waitUntilDoesNotExist(hasText("Play a few songs", substring = true), 5_000)
            }
        } finally {
            runBlocking { engines.close() }
        }
    }

    @Test
    fun playbackContinuesWhileReloadIsPressed() {
        Locale.setDefault(Locale.ENGLISH)
        val engines = DynamicEngineManager(offlineClient(), appVersion = 1, cacheStore = cacheStore(), jsSource = embeddedEngineJsSource())
        val player = StubPlayer()
        val song = ytSong("abcdefghijk", "Still Playing")
        seedQueue(player, song)
        runBlocking { engines.start() }
        try {
            runComposeUiTest {
                setContent {
                    App(AppGraph(settings = InMemorySettingsStore(), engines = engines, player = player, playerSurface = {}))
                }
                onAllNodesWithActionLabel("Recommended").onFirst().performClick()
                waitUntilAtLeastOneExists(hasText("Play a few songs", substring = true), 20_000)
                onNodeWithText("Reload").performClick()
                waitForIdle()
                assertEquals(song, player.queueState.value.currentItem)
                assertEquals(0, player.queueState.value.currentIndex)
                onNodeWithText("Play a few songs", substring = true).assertIsDisplayed()
            }
        } finally {
            runBlocking { engines.close() }
        }
    }

    @Test
    fun leaveAndReturn_toStarTab_keepsEmptyUntilRecommendationsExist() {
        Locale.setDefault(Locale.ENGLISH)
        val engines = DynamicEngineManager(offlineClient(), appVersion = 1, cacheStore = cacheStore(), jsSource = embeddedEngineJsSource())
        val player = StubPlayer()
        seedQueue(player, ytSong("abcdefghijk", "One"))
        runBlocking { engines.start() }
        try {
            runComposeUiTest {
                setContent {
                    App(AppGraph(settings = InMemorySettingsStore(), engines = engines, player = player, playerSurface = {}))
                }
                onAllNodesWithActionLabel("Recommended").onFirst().performClick()
                waitUntilAtLeastOneExists(hasText("Play a few songs", substring = true), 20_000)
                onNodeWithActionLabel("Home").performClick()
                onAllNodesWithActionLabel("Recommended").onFirst().performClick()
                waitUntilAtLeastOneExists(hasText("Play a few songs", substring = true), 20_000)
                onNodeWithText("Reload").assertIsDisplayed()
            }
        } finally {
            runBlocking { engines.close() }
        }
    }

    @Test
    fun recommended_starts_empty_then_fills_from_what_played() {
        Locale.setDefault(Locale.ENGLISH)
        val engines = DynamicEngineManager(offlineClient(), appVersion = 1, cacheStore = cacheStore(), jsSource = embeddedEngineJsSource())
        runBlocking { engines.start() }
        try {
            runComposeUiTest {
                val player = StubPlayer()
                val graph = AppGraph(settings = InMemorySettingsStore(), engines = engines, player = player, playerSurface = {})
                setContent { App(graph) }

                onAllNodesWithActionLabel("Recommended").onFirst().performClick()
                waitUntilAtLeastOneExists(hasText("Play a few songs", substring = true), 20_000)

                runBlocking { awaitHarvest(engines, "yuuWdm5tBD0", watchNextContents) }

                onNodeWithActionLabel("Home").performClick()
                onAllNodesWithActionLabel("Recommended").onFirst().performClick()
                waitUntilAtLeastOneExists(hasText("A related song", substring = true), 20_000)
            }
        } finally {
            runBlocking { engines.close() }
        }
    }

    /** Land watch-next in the engine cache before the Star tab rebuilds from it. */
    private suspend fun awaitHarvest(engines: DynamicEngineManager, videoId: String, contents: String) {
        engines.handleWebViewPlayNext(videoId, contents)
        withTimeout(10_000) {
            while (engines.getRecommendedFeed().sections.all { it.items.isEmpty() }) {
                delay(50)
            }
        }
    }

    @Test
    fun duplicateReload_doesNotClearQueueOrBlankPage() {
        Locale.setDefault(Locale.ENGLISH)
        val engines = DynamicEngineManager(offlineClient(), appVersion = 1, jsSource = embeddedEngineJsSource())
        val player = StubPlayer()
        seedQueue(player, ytSong("abcdefghijk", "One"))
        val inFlight = AtomicInteger(0)
        val maxInFlight = AtomicInteger(0)
        runComposeUiTest {
            setContent {
                YMusicTheme {
                    val graph = AppGraph(settings = InMemorySettingsStore(), engines = engines, player = player, playerSurface = {})
                    val scope = rememberCoroutineScope()
                    val coordinator = PlaybackCoordinator(graph, scope)
                    val pager = Pager(scope) {
                        inFlight.incrementAndGet()
                        maxInFlight.updateAndGet { maxOf(it, inFlight.get()) }
                        try {
                            delay(50)
                            YtFeedPage(sections = emptyList())
                        } finally {
                            inFlight.decrementAndGet()
                        }
                    }.also { it.refresh() }
                    Box(Modifier.fillMaxSize()) {
                        FeedList(
                            pager,
                            coordinator,
                            emptyMessage = "Play a few songs and your recommendations will show up here",
                        )
                    }
                }
            }
            waitUntilAtLeastOneExists(hasText("Play a few songs", substring = true), 20_000)
            repeat(3) { onNodeWithText("Reload").performClick() }
            waitForIdle()
            assertEquals(ytSong("abcdefghijk", "One"), player.queueState.value.currentItem)
            onNodeWithText("Play a few songs", substring = true).assertIsDisplayed()
            // Pager cancels prior jobs; at most one fetch should be live at a time.
            assertTrue(maxInFlight.get() <= 1, "concurrent fetches=${maxInFlight.get()}")
        }
    }

    @Test
    fun usableRecommendations_hideEmptyMessage() {
        Locale.setDefault(Locale.ENGLISH)
        val engines = DynamicEngineManager(offlineClient(), appVersion = 1, jsSource = embeddedEngineJsSource())
        val player = StubPlayer()
        seedQueue(player, ytSong("abcdefghijk", "One"))
        val item = YtFeedItem(
            kind = YtFeedItem.KIND_VIDEO,
            id = "N30CFk9fHwg",
            title = "A related song",
        )
        runComposeUiTest {
            setContent {
                YMusicTheme {
                    val graph = AppGraph(settings = InMemorySettingsStore(), engines = engines, player = player, playerSurface = {})
                    val scope = rememberCoroutineScope()
                    val coordinator = PlaybackCoordinator(graph, scope)
                    val pager = Pager(scope) {
                        YtFeedPage(sections = listOf(YtSection(items = listOf(item))))
                    }.also { it.refresh() }
                    Box(Modifier.fillMaxSize()) {
                        FeedList(
                            pager,
                            coordinator,
                            emptyMessage = "Play a few songs and your recommendations will show up here",
                        )
                    }
                }
            }
            waitUntilAtLeastOneExists(hasText("A related song", substring = true), 20_000)
            onAllNodes(hasText("Play a few songs", substring = true)).assertCountEquals(0)
        }
    }
}
