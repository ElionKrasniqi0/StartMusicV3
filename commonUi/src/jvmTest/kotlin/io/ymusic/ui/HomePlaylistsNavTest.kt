package io.ymusic.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import kotlin.test.assertEquals

/**
 * Home's Playlists quick chip must open Library on the Playlists tab — not Folders (the Library default).
 */
@OptIn(ExperimentalTestApi::class)
class HomePlaylistsNavTest {

    @Test
    fun home_playlists_chip_opens_library_playlists_tab() {
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
                    uri = Uri.parse("file:///Music/a.mp3"),
                    title = "Local A",
                    mimeType = "audio/mpeg",
                    fileSize = 10,
                    lastModified = 1L,
                ),
            )
        }
        try {
            runComposeUiTest {
                val section = mutableStateOf(AppSection.Discover)
                setContent {
                    App(
                        AppGraph(
                            settings = InMemorySettingsStore(),
                            engines = engines,
                            player = StubPlayer(),
                            playerSurface = {},
                            mediaStore = store,
                        ),
                        section,
                    )
                }

                waitUntilAtLeastOneExists(hasText("Playlists"), 20_000)
                onNodeWithText("Playlists").performClick()

                assertEquals(AppSection.Library, section.value)
                // Playlists toolbar only — Folders would show "+ Add folder" instead.
                waitUntilAtLeastOneExists(hasText("+  New playlist"), 10_000)
            }
        } finally {
            runBlocking { engines.close() }
        }
    }
}
