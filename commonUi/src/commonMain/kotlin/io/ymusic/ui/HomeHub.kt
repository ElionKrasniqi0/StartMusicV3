package io.ymusic.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.ymusic.dynamicengine.yt.YtFeedItem
import io.ymusic.mediastore.model.MediaStoreItem
import io.ymusic.ui.playhistory.PlayAgainSession
import io.ymusic.ui.resources.Res
import io.ymusic.ui.resources.about_tagline
import io.ymusic.ui.resources.app_name
import io.ymusic.ui.resources.cd_view_all
import io.ymusic.ui.resources.home_featured
import io.ymusic.ui.resources.home_quick_library
import io.ymusic.ui.resources.home_quick_play_again
import io.ymusic.ui.resources.home_quick_playlists
import io.ymusic.ui.resources.home_quick_recommended
import io.ymusic.ui.resources.home_quick_trending
import io.ymusic.ui.resources.home_recently_added
import io.ymusic.ui.resources.home_recently_played
import io.ymusic.ui.resources.home_search_hint
import io.ymusic.ui.resources.startmusic_logo
import kotlinx.coroutines.flow.onStart
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Discover Home header: brand, search entry, quick navigation, recently played, then the
 * engine Home shelves continue as featured content below ([FeedList] `topContent`).
 *
 * Responsive: chips scroll horizontally on narrow widths; recent cards shrink on compact panes.
 */
@Composable
fun HomeHub(
    graph: AppGraph,
    coordinator: PlaybackCoordinator,
    onSearch: () -> Unit,
    onSelectDiscoverTab: (DiscoverTab) -> Unit,
    onOpenFeedItem: ((YtFeedItem) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val navigate = LocalNavigateToSection.current
    val guest = graph.guest
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val compact = maxWidth < 600.dp
        val recentCardWidth = if (maxWidth < 400.dp) 120.dp else if (maxWidth < 840.dp) 140.dp else 156.dp
        Column(
            Modifier
                .fillMaxWidth()
                .padding(
                    start = DesignSpacing.MarginCompact,
                    end = DesignSpacing.MarginCompact,
                    top = DesignSpacing.Space16,
                    bottom = DesignSpacing.Space8,
                ),
            verticalArrangement = Arrangement.spacedBy(DesignSpacing.Space16),
        ) {
            HomeBrandRow(compact = compact)
            HomeSearchField(onSearch = onSearch)
            HomeQuickNav(
                guest = guest,
                onRecommended = { onSelectDiscoverTab(DiscoverTab.Recommended) },
                onTrending = { onSelectDiscoverTab(DiscoverTab.Music) },
                onLibrary = { navigate(AppSection.Library) },
                onPlaylists = { navigate(AppSection.Library) },
                onPlayAgain = { navigate(AppSection.PlayAgain) },
            )
            HomeRecentlyPlayed(
                graph = graph,
                coordinator = coordinator,
                cardWidth = recentCardWidth,
                onOpenFeedItem = onOpenFeedItem,
                onSeeAll = {
                    if (!guest) onSelectDiscoverTab(DiscoverTab.TopTracks) else navigate(AppSection.PlayAgain)
                },
            )
            HomeRecentlyAdded(
                graph = graph,
                coordinator = coordinator,
                cardWidth = recentCardWidth,
            )
            Text(
                stringResource(Res.string.home_featured),
                style = Brand.Headline.copy(fontWeight = FontWeight.SemiBold, color = DesignColors.InkStrong),
                modifier = Modifier.padding(top = DesignSpacing.Space4),
            )
        }
    }
}

@Composable
private fun HomeBrandRow(compact: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Image(
            painterResource(Res.drawable.startmusic_logo),
            contentDescription = stringResource(Res.string.app_name),
            modifier = Modifier
                .size(if (compact) 40.dp else 48.dp)
                .clip(RoundedCornerShape(Brand.ThumbCorner)),
        )
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(Res.string.app_name),
                style = Brand.Title.copy(
                    fontSize = if (compact) Brand.Headline.fontSize else Brand.Title.fontSize,
                    fontWeight = FontWeight.Bold,
                    color = DesignColors.InkStrong,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(Res.string.about_tagline),
                style = Brand.Caption.copy(color = DesignColors.InkSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeSearchField(onSearch: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(DesignColors.FieldPill)
            .border(1.dp, DesignColors.OutlineSoft.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
            .clickable(onClick = onSearch)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Default.Search, contentDescription = null, Modifier.size(20.dp), tint = DesignColors.InkFaint)
        Text(
            stringResource(Res.string.home_search_hint),
            style = Brand.Body.copy(color = DesignColors.InkFaint),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HomeQuickNav(
    guest: Boolean,
    onRecommended: () -> Unit,
    onTrending: () -> Unit,
    onLibrary: () -> Unit,
    onPlaylists: () -> Unit,
    onPlayAgain: () -> Unit,
) {
    data class Chip(val label: String, val icon: ImageVector, val onClick: () -> Unit)
    val chips = buildList {
        add(Chip(stringResource(Res.string.home_quick_recommended), Icons.Outlined.StarBorder, onRecommended))
        add(Chip(stringResource(Res.string.home_quick_trending), Icons.Outlined.MusicNote, onTrending))
        add(Chip(stringResource(Res.string.home_quick_library), Icons.Outlined.LibraryMusic, onLibrary))
        add(Chip(stringResource(Res.string.home_quick_playlists), Icons.AutoMirrored.Outlined.PlaylistPlay, onPlaylists))
        if (!guest) {
            add(Chip(stringResource(Res.string.home_quick_play_again), Icons.Outlined.History, onPlayAgain))
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { chip ->
            val interaction = remember(chip.label) { MutableInteractionSource() }
            val hovered by interaction.collectIsHoveredAsState()
            Row(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (hovered) DesignColors.SurfaceElevated.copy(alpha = 1f)
                        else DesignColors.SurfaceElevated,
                    )
                    .border(
                        1.dp,
                        if (hovered) DesignColors.Accent.copy(alpha = 0.55f) else DesignColors.OutlineSoft.copy(alpha = 0.5f),
                        RoundedCornerShape(999.dp),
                    )
                    .hoverable(interaction)
                    .clickable(interactionSource = interaction, onClick = chip.onClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(chip.icon, null, Modifier.size(16.dp), tint = DesignColors.Accent)
                Text(chip.label, style = Brand.Label.copy(color = DesignColors.Ink), maxLines = 1)
            }
        }
    }
}

@Composable
private fun HomeRecentlyPlayed(
    graph: AppGraph,
    coordinator: PlaybackCoordinator,
    cardWidth: androidx.compose.ui.unit.Dp,
    onOpenFeedItem: ((YtFeedItem) -> Unit)?,
    onSeeAll: () -> Unit,
) {
    var recent by remember { mutableStateOf<List<YtFeedItem>>(emptyList()) }
    var sessions by remember { mutableStateOf<List<PlayAgainSession>>(emptyList()) }
    LaunchedEffect(graph.playHistory) {
        val history = graph.playHistory
        if (history == null) {
            recent = emptyList()
            sessions = emptyList()
            return@LaunchedEffect
        }
        recent = runCatching { history.youtubeRecent(limit = 12) }.getOrDefault(emptyList())
        sessions = runCatching { history.playAgain.sessions() }.getOrDefault(emptyList()).take(8)
    }
    if (recent.isEmpty() && sessions.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(Res.string.home_recently_played),
                style = Brand.Headline.copy(fontWeight = FontWeight.SemiBold, color = DesignColors.InkStrong),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onSeeAll) {
                Text(stringResource(Res.string.cd_view_all), style = Brand.Label.copy(color = DesignColors.Accent))
            }
        }
        LazyRow(
            contentPadding = PaddingValues(end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (recent.isNotEmpty()) {
                items(recent, key = { "yt-${it.id}" }) { item ->
                    HomeRecentVideoCard(
                        item = item,
                        width = cardWidth,
                        onClick = {
                            if (onOpenFeedItem != null) onOpenFeedItem(item)
                            else coordinator.playNow(item)
                        },
                    )
                }
            } else {
                items(sessions, key = { "pa-${it.queueId}" }) { session ->
                    HomeRecentSessionCard(
                        session = session,
                        width = cardWidth,
                        onClick = onSeeAll,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeRecentlyAdded(
    graph: AppGraph,
    coordinator: PlaybackCoordinator,
    cardWidth: androidx.compose.ui.unit.Dp,
) {
    val store = graph.mediaStore ?: return
    var items by remember { mutableStateOf<List<MediaStoreItem>>(emptyList()) }
    LaunchedEffect(store) {
        store.itemsChanges().onStart { emit(Unit) }.collect {
            items = runCatching {
                store.allItems().sortedByDescending { it.createdAt }.take(12)
            }.getOrDefault(emptyList())
        }
    }
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(Res.string.home_recently_added),
            style = Brand.Headline.copy(fontWeight = FontWeight.SemiBold, color = DesignColors.InkStrong),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        LazyRow(
            contentPadding = PaddingValues(end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items, key = { "local-${it.id}" }) { song ->
                HomeLocalCard(
                    title = song.title,
                    subtitle = song.artist,
                    artUrl = localArtUrl(song),
                    width = cardWidth,
                    onClick = { coordinator.playLocalSingle(song) },
                )
            }
        }
    }
}

@Composable
private fun HomeLocalCard(
    title: String,
    subtitle: String?,
    artUrl: String?,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .width(width)
            .clip(RoundedCornerShape(Brand.CardCorner))
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(width)
                .clip(RoundedCornerShape(Brand.ArtCorner))
                .background(DesignColors.Placeholder),
            contentAlignment = Alignment.Center,
        ) {
            if (!artUrl.isNullOrBlank()) {
                ArtImage(artUrl, RoundedCornerShape(Brand.ArtCorner)) {
                    ThumbPlaceholder(Icons.Outlined.MusicNote, 24.dp)
                }
            } else {
                ThumbPlaceholder(Icons.Outlined.MusicNote, 24.dp)
            }
        }
        Text(
            title,
            style = Brand.Label.copy(fontWeight = FontWeight.SemiBold, color = DesignColors.InkStrong),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = Brand.Caption.copy(color = DesignColors.InkSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeRecentVideoCard(
    item: YtFeedItem,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .width(width)
            .clip(RoundedCornerShape(Brand.CardCorner))
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(width * 9f / 16f)
                .clip(RoundedCornerShape(Brand.ArtCorner))
                .background(DesignColors.Placeholder),
            contentAlignment = Alignment.Center,
        ) {
            ThumbImage(item.thumbs, RoundedCornerShape(Brand.ArtCorner)) {
                ThumbPlaceholder(Icons.Outlined.Image, 24.dp)
            }
        }
        Text(
            item.title,
            style = Brand.Label.copy(fontWeight = FontWeight.SemiBold, color = DesignColors.InkStrong),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        item.subtitleLine1?.let { sub ->
            Text(
                sub,
                style = Brand.Caption.copy(color = DesignColors.InkSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeRecentSessionCard(
    session: PlayAgainSession,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .width(width)
            .clip(RoundedCornerShape(Brand.CardCorner))
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(width * 9f / 16f)
                .clip(RoundedCornerShape(Brand.ArtCorner))
                .background(DesignColors.Placeholder),
            contentAlignment = Alignment.Center,
        ) {
            val url = session.thumbnailUrl
            if (!url.isNullOrBlank()) {
                ArtImage(url, RoundedCornerShape(Brand.ArtCorner)) {
                    ThumbPlaceholder(Icons.Outlined.History, 24.dp)
                }
            } else {
                ThumbPlaceholder(Icons.Outlined.History, 24.dp)
            }
        }
        Text(
            session.title,
            style = Brand.Label.copy(fontWeight = FontWeight.SemiBold, color = DesignColors.InkStrong),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        session.subtitle?.let { sub ->
            Text(
                sub,
                style = Brand.Caption.copy(color = DesignColors.InkSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
