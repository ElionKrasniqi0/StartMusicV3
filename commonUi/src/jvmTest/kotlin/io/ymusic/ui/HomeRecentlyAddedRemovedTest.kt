package io.ymusic.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import io.ymusic.dynamicengine.DynamicEngineManager
import io.ymusic.dynamicengine.embeddedEngineJsSource
import io.ymusic.mediastore.BetterMediaStore
import io.ymusic.mediastore.mediaStoreJdbcDriver
import io.ymusic.mediastore.model.BucketDraft
import io.ymusic.mediastore.model.MediaItemDraft
import io.ymusic.settings.InMemorySettingsStore
import kmpcommon.uri.Uri
import kotlinx.coroutines.runBlocking
import java.util.Locale
import kotlin.test.Test

/**
 * Regression: Home must not show a "Recently added" local-library shelf (removed).
 * Local items still exist in the media store — they must not surface as that Home section.
 */
@OptIn(ExperimentalTestApi::class)
class HomeRecentlyAddedRemovedTest {

    @Test
    fun home_does_not_show_recently_added_section() {
        Locale.setDefault(Locale.ENGLISH)
        val client = CannedHttp { request, _ ->
            val url = request.url.toString()
            if (request.method == "GET" && url.contains("www.youtube.com") && !url.contains("youtubei")) {
                textResponse(request, HOMEPAGE_FIXTURE, "text/html")
            } else {
                textResponse(request, "{}", "application/json")
            }
        }
        val engines = DynamicEngineManager(client, appVersion = 1, jsSource = embeddedEngineJsSource())
        runBlocking { engines.start() }
        val store = BetterMediaStore.create(mediaStoreJdbcDriver(":memory:"))
        runBlocking {
            val music = store.upsertBucket(BucketDraft(Uri.parse("file:///Music"), "Music"))
            store.upsertItem(
                music.id,
                MediaItemDraft(
                    uri = Uri.parse("file:///Music/fresh.mp3"),
                    title = "Fresh Local Track",
                    mimeType = "audio/mpeg",
                    fileSize = 10,
                    lastModified = 1L,
                ),
            )
        }
        try {
            runComposeUiTest {
                setContent {
                    App(
                        AppGraph(
                            settings = InMemorySettingsStore(),
                            engines = engines,
                            player = StubPlayer(),
                            playerSurface = {},
                            mediaStore = store,
                        ),
                    )
                }
                waitUntilAtLeastOneExists(hasText("Featured"), 20_000)
                waitUntilAtLeastOneExists(hasText("Recently played"), 20_000)
                onAllNodesWithText("Recently added").assertCountEquals(0)
                onAllNodesWithText("Fresh Local Track").assertCountEquals(0)
            }
        } finally {
            runBlocking { engines.close() }
        }
    }
}
