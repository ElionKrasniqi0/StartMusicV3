package io.ymusic.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import io.ymusic.dynamicengine.yt.YtFeedItem
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertTrue

/** Channel feed rows must fire the Subscribe pill — not a decorative no-op. */
@OptIn(ExperimentalTestApi::class)
class ChannelSubscribeCtaTest {

    @Test
    fun subscribe_pill_invokes_onCta() {
        Locale.setDefault(Locale.ENGLISH)
        var clicked = false
        runComposeUiTest {
            setContent {
                YMusicTheme {
                    FeedListItem(
                        item = YtFeedItem(
                            kind = YtFeedItem.KIND_CHANNEL,
                            id = "UCtestChannel01",
                            title = "Test Channel",
                            ctaText = "Subscribe",
                        ),
                        actions = ItemActions(emptyList()),
                        onClick = {},
                        onCta = { clicked = true },
                    )
                }
            }
            onNodeWithText("Subscribe").performClick()
            assertTrue(clicked, "Subscribe pill must call onCta")
        }
    }
}
