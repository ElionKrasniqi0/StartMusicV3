package io.ymusic.ui

import io.ymusic.ui.playhistory.PlayHistoryStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Featured (Discover Home shelves under the Featured header) is a cached [FeedSession]. Without a
 * listen-driven reload it keeps the process-bootstrap homepage until the app is killed. Recently
 * played already observes [PlayHistoryStore.changes]; Featured must too — via keep-content
 * [Pager.reload] (`refresh=true` scrape), not a full-screen [Pager.refresh].
 */
internal const val FeaturedHomeReloadDebounceMs = 1_500L

/**
 * Reloads the Home/Featured feed whenever watch-time ranking changes. No-op while the Home session
 * has not been created yet (first open still bootstraps normally). Debounced so pause/buffer
 * flush storms do not scrape youtube.com on every tick.
 */
@OptIn(FlowPreview::class)
internal fun CoroutineScope.bindFeaturedHomeReload(
    playHistory: PlayHistoryStore?,
    homeSession: () -> FeedSession?,
): Job? {
    val history = playHistory ?: return null
    return launch {
        history.changes.debounce(FeaturedHomeReloadDebounceMs).collect {
            homeSession()?.pager?.reload()
        }
    }
}
