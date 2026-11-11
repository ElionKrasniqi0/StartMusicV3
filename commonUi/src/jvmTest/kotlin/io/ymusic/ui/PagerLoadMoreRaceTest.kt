package io.ymusic.ui

import io.ymusic.dynamicengine.yt.YtFeedItem
import io.ymusic.dynamicengine.yt.YtFeedPage
import io.ymusic.dynamicengine.yt.YtSection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * loadMore must not race a keep-content reload: blocking while refreshing prevents a stale
 * continuation append from overwriting a fresh page.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PagerLoadMoreRaceTest {

    private fun page(ids: List<String>, continuation: String? = null) = YtFeedPage(
        sections = listOf(
            YtSection(
                items = ids.map { id -> YtFeedItem(kind = "video", id = id, title = id) },
            ),
        ),
        continuation = continuation,
    )

    private suspend fun Pager.awaitReady(allowRefreshing: Boolean = false): Pager.State.Ready {
        val state = state.first {
            it is Pager.State.Ready && (allowRefreshing || !it.refreshing) || it is Pager.State.Failed
        }
        assertTrue(state is Pager.State.Ready, "$state")
        return state as Pager.State.Ready
    }

    @Test
    fun loadMore_blocked_while_refreshing() = runTest {
        val gate = CompletableDeferred<Unit>()
        var phase = 0
        val pager = Pager(backgroundScope) { token ->
            when {
                token != null -> page(listOf("stale-more"), continuation = null)
                phase == 0 -> {
                    phase = 1
                    page(listOf("fresh-a", "fresh-b"), continuation = "more")
                }
                else -> {
                    gate.await()
                    page(listOf("reloaded"), continuation = "more")
                }
            }
        }

        pager.refresh()
        assertEquals(listOf("fresh-a", "fresh-b"), pager.awaitReady().sections.single().items.map { it.id })

        pager.reload()
        val refreshing = pager.awaitReady(allowRefreshing = true)
        assertTrue(refreshing.refreshing)

        pager.loadMore()
        assertFalse(
            (pager.state.value as Pager.State.Ready).loadingMore,
            "loadMore must no-op while refreshing",
        )

        gate.complete(Unit)
        advanceUntilIdle()
        val ready = pager.awaitReady()
        assertFalse(ready.refreshing)
        assertEquals(listOf("reloaded"), ready.sections.single().items.map { it.id })
    }
}
