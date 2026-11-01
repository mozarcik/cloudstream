package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.data.entities.TvSeason
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.focus.rememberTvFocusMemory
import com.lagradost.cloudstream3.tv.presentation.focus.requestFocusWithRetry
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsDownloadActionState
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerStartTarget
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import androidx.tv.material3.MaterialTheme

private val DividerHeight = 1.dp
private const val PanelCloseFocusDelayMs = 50L
private const val PanelResultSettleMs = 1500L
private const val SimilarKeyPrefix = "similar:"

/** Where the tab content starts once the page is scrolled (tabs at 64 dp + tab + gap + divider + gap). */
private val CollapsedContentTop = TvDimens.DetailsCollapsedTabsTop + TvDimens.DetailsTabHeight +
    TvDimens.DetailsTabsToDivider + DividerHeight + TvDimens.DetailsDividerToContent

/**
 * The details page (movie and series). It is one column: hero (actions at the top state), tabs, tab content.
 * When focus enters a tab's content the whole column slides up once (150 ms) so the tabs sit under a one-line
 * header; focus back in the hero slides it down. Nothing here scrolls the page itself, the content scrolls
 * its own lists.
 */
@Composable
internal fun DetailsScreenBody(
    mode: DetailsScreenMode,
    details: MovieDetails,
    isSeriesContent: Boolean,
    isSecondaryContentLoading: Boolean,
    listState: LazyListState,
    seasons: List<TvSeason>,
    selectedSeasonId: String?,
    selectedEpisodes: List<TvEpisode>,
    heroState: DetailsHeroUiState,
    resolveEpisodeDownloadState: (TvEpisode) -> DetailsDownloadButtonUiState,
    resolveEpisodeWatchedState: (TvEpisode) -> Boolean,
    toDownloadUiState: (DetailsDownloadButtonUiState) -> MovieDetailsDownloadActionState,
    onAction: (DetailsUiAction) -> Unit,
    onSeasonSelected: (TvSeason) -> Unit,
    onEpisodeSelected: (TvEpisode) -> Unit,
    refreshScreenWithNewItem: (Movie) -> Unit,
    goToPlayer: (PlayerStartTarget) -> Unit,
    isPanelVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val availableTabs = remember(isSeriesContent, isSecondaryContentLoading, details.cast, details.similarMovies) {
        buildList {
            if (isSeriesContent) add(DetailsTab.Episodes)
            add(DetailsTab.Info)
            if (!isSecondaryContentLoading) {
                if (details.cast.isNotEmpty()) add(DetailsTab.CastAndCrew)
                if (details.similarMovies.isNotEmpty()) add(DetailsTab.MoreLikeThis)
            }
        }
    }
    var requestedTab by rememberSaveable(details.id) { mutableStateOf<DetailsTab?>(null) }
    val selectedTab = requestedTab?.takeIf { it in availableTabs } ?: availableTabs.first()
    var collapsed by rememberSaveable(details.id) { mutableStateOf(false) }
    var returnEpisodeId by rememberSaveable(details.id) { mutableStateOf<String?>(null) }
    var returnSeasonId by rememberSaveable(details.id) { mutableStateOf<String?>(null) }

    val progress = animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        animationSpec = tween(TvDimens.MaxAnimationMs),
        label = "details-collapse",
    )
    val shiftPx = with(LocalDensity.current) {
        (TvDimens.DetailsHeroHeight - TvDimens.DetailsCollapsedTabsTop).toPx()
    }
    val liftPx = with(LocalDensity.current) { TvDimens.DetailsTopStateContentLift.toPx() }
    val contentHeight = LocalConfiguration.current.screenHeightDp.dp - CollapsedContentTop

    // Remembers the last focused element that can open a panel (hero buttons, episode actions) or another title
    // (More like this), so focus returns there after a panel closes or after coming back from that title.
    val focusMemory = rememberTvFocusMemory()
    val similarGridState = rememberLazyGridState()
    val heroFocus = remember { DetailsHeroFocus() }
    val tabRequesters = remember(availableTabs) { availableTabs.associateWith { FocusRequester() } }
    val castEntry = remember { FocusRequester() }
    val similarEntry = remember { FocusRequester() }
    val episodesFocus = remember(details.id) { DetailsEpisodesFocusController() }
    val actions = DetailsHeroActions(
        showPlay = !details.comingSoon,
        showWatchlist = mode.allowsBookmark,
        showDownload = !details.comingSoon,
        showMore = mode.allowsExtendedActions,
    )
    val initialFocus = heroFocus.initial(actions.showPlay, actions.showWatchlist)
    val tabUpRequester = initialFocus

    // Coming back (player, another title): the page was left scrolled into a tab's content, so focus goes
    // back there (the episode the user came from, the tab's list, or the tab itself); otherwise to Play.
    val restoreIntoContent = remember(details.id) { collapsed }
    FocusRequestEffect(
        requester = initialFocus,
        requestKey = details.id,
        enabled = !restoreIntoContent,
    )
    LaunchedEffect(details.id) {
        if (!restoreIntoContent) return@LaunchedEffect
        val episodeId = returnEpisodeId
        when {
            selectedTab == DetailsTab.Episodes && episodeId != null -> {
                seasons.firstOrNull { it.id == returnSeasonId }?.let(onSeasonSelected)
                episodesFocus.requestEpisode(episodeId)
            }

            selectedTab == DetailsTab.CastAndCrew -> castEntry.requestFocusWithRetry()
            selectedTab == DetailsTab.MoreLikeThis -> {
                val key = focusMemory.lastKey?.takeIf { it.startsWith(SimilarKeyPrefix) }
                val index = key?.let { k -> details.similarMovies.indexOfFirst { similarKey(it.id) == k } } ?: -1
                if (index >= 0) {
                    similarGridState.scrollToItem(index)
                    focusMemory.requesterFor(key!!).requestFocusWithRetry()
                } else {
                    similarEntry.requestFocusWithRetry()
                }
            }
            else -> tabRequesters.getValue(selectedTab).requestFocusWithRetry()
        }
    }

    // Choosing a status changes `details` a moment after the panel closes and that recreates the hero buttons,
    // which drops focus; restore once more when the result lands.
    var restoreUntilMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(details.isBookmarked, details.bookmarkLabelRes, details.isFavorite) {
        if (SystemClock.uptimeMillis() >= restoreUntilMs) return@LaunchedEffect
        val key = focusMemory.lastKey ?: return@LaunchedEffect
        delay(PanelCloseFocusDelayMs)
        focusMemory.requesterFor(key).requestFocusWithRetry()
    }

    // A panel (Watchlist, mirrors, More) was closed: put focus back on the button that opened it.
    var panelWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(isPanelVisible) {
        if (isPanelVisible) {
            panelWasVisible = true
            return@LaunchedEffect
        }
        if (!panelWasVisible) return@LaunchedEffect
        panelWasVisible = false
        val key = focusMemory.lastKey ?: return@LaunchedEffect
        // The panel keeps its focus trap until its slide-out ends.
        delay(TvDimens.MaxAnimationMs.toLong() + PanelCloseFocusDelayMs)
        focusMemory.requesterFor(key).requestFocusWithRetry()
        restoreUntilMs = SystemClock.uptimeMillis() + PanelResultSettleMs
    }

    fun requestOrFalse(requester: FocusRequester): Boolean =
        runCatching { requester.requestFocus() }.getOrDefault(false)

    val enterContent: (DetailsTab) -> Boolean = { tab ->
        when (tab) {
            DetailsTab.Episodes -> {
                if (selectedEpisodes.isEmpty()) {
                    false
                } else {
                    val target = resolveEpisodeToFocus(
                        episodes = selectedEpisodes,
                        lastFocusedId = episodesFocus.lastFocused(selectedSeasonId),
                        currentEpisodeNumber = details.currentEpisode,
                        isCurrentSeason = seasons.firstOrNull { it.id == selectedSeasonId }
                            ?.let { (it.displaySeasonNumber ?: it.seasonNumber) == details.currentSeason } ?: false,
                        isWatched = resolveEpisodeWatchedState,
                    )
                    target?.let { episodesFocus.requestEpisode(it.id) }
                    target != null
                }
            }

            DetailsTab.CastAndCrew -> requestOrFalse(castEntry)
            DetailsTab.MoreLikeThis -> requestOrFalse(similarEntry)
            DetailsTab.Info -> {
                // Nothing to focus: Down only slides the page up to read the full details.
                collapsed = true
                true
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        DetailsBackdrop(
            uri = details.backdropUri.ifBlank { details.posterUri },
            headers = details.posterHeaders,
            isSeries = isSeriesContent,
            dim = { progress.value },
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(Alignment.Top, unbounded = true)
                .graphicsLayer { translationY = -shiftPx * progress.value },
        ) {
            DetailsHero(
                details = details,
                isSeries = isSeriesContent,
                playLabel = heroState.playButtonLabel,
                downloadState = heroState.downloadActionState,
                actions = actions,
                focus = heroFocus,
                downRequester = tabRequesters.getValue(selectedTab),
                onPlayClick = { goToPlayer(resolveDefaultPlaybackTarget(details)) },
                onQuickAction = { action -> onAction(DetailsUiAction.HeroQuickAction(action)) },
                onFocused = { collapsed = false },
                memory = focusMemory,
                modifier = Modifier.graphicsLayer { alpha = 1f - progress.value },
            )
            DetailsTabsRow(
                tabs = availableTabs,
                selected = selectedTab,
                requesters = tabRequesters,
                upRequester = tabUpRequester,
                // Focusing a tab (Down from the hero lands here) slides the page up at once, so the tab's full
                // content is visible without a second Down.
                onTabFocused = { tab ->
                    requestedTab = tab
                    collapsed = true
                },
                onEnterContent = enterContent,
                modifier = Modifier.padding(start = TvDimens.DetailsContentStart - TvDimens.DetailsTabHorizontalPadding),
            )
            Spacer(Modifier.height(TvDimens.DetailsTabsToDivider))
            Box(
                modifier = Modifier
                    .padding(start = TvDimens.DetailsContentStart, end = TvDimens.DetailsContentEnd)
                    .fillMaxWidth()
                    .height(DividerHeight)
                    .graphicsLayer { alpha = progress.value }
                    .background(TvColors.DetailsDivider),
            )
            Spacer(Modifier.height(TvDimens.DetailsDividerToContent))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentHeight)
                    .graphicsLayer { translationY = -liftPx * (1f - progress.value) }
                    .onFocusChanged { state -> if (state.hasFocus) collapsed = true },
            ) {
                val tabTop = tabRequesters.getValue(selectedTab)
                when (selectedTab) {
                    DetailsTab.Episodes -> DetailsEpisodesTab(
                        seasons = seasons,
                        selectedSeasonId = selectedSeasonId,
                        episodes = selectedEpisodes,
                        nextAiring = details.nextAiring,
                        currentSeasonNumber = details.currentSeason,
                        currentEpisodeNumber = details.currentEpisode,
                        listState = listState,
                        controller = episodesFocus,
                        upRequester = tabTop,
                        resolveDownloadState = { episode -> toDownloadUiState(resolveEpisodeDownloadState(episode)) },
                        resolveWatched = resolveEpisodeWatchedState,
                        onSeasonSelected = onSeasonSelected,
                        onEpisodeClick = onEpisodeSelected,
                        onEpisodeAction = { episode, action ->
                            onAction(DetailsUiAction.EpisodeQuickAction(episode, action))
                        },
                        memory = focusMemory,
                        onEpisodeFocused = { seasonId, episode ->
                            returnSeasonId = seasonId
                            returnEpisodeId = episode.id
                        },
                    )

                    DetailsTab.CastAndCrew -> DetailsCastTab(
                        cast = details.cast,
                        entryRequester = castEntry,
                        upRequester = tabTop,
                    )

                    DetailsTab.MoreLikeThis -> DetailsSimilarTab(
                        title = details.name,
                        movies = details.similarMovies,
                        entryRequester = similarEntry,
                        upRequester = tabTop,
                        onMovieSelected = refreshScreenWithNewItem,
                        memory = focusMemory,
                        gridState = similarGridState,
                    )

                    DetailsTab.Info -> DetailsInfoTab(details = details)
                }
            }
        }

        DetailsCompactHeader(
            details = details,
            isSeries = isSeriesContent,
            modifier = Modifier.graphicsLayer { alpha = progress.value },
        )
    }
}
