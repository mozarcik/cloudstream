package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.tv.compat.home.sourceId
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.FeedCategory
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.lagradost.cloudstream3.tv.presentation.common.TvEmptyState
import com.lagradost.cloudstream3.tv.presentation.common.tvSkeleton
import com.lagradost.cloudstream3.tv.presentation.focus.TvScreenFocusEffect
import com.lagradost.cloudstream3.tv.presentation.focus.isItemComposed
import com.lagradost.cloudstream3.tv.presentation.focus.requestFocusWithRetry
import com.lagradost.cloudstream3.tv.presentation.focus.tryRequestFocus
import com.lagradost.cloudstream3.tv.presentation.focus.tryRequestFocusWithRetry
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.screens.dashboard.DashboardContentTopInset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Gap between the top bar and the first row when there is no hero. */
private val TopBarToContentSpacing = TvDimens.TopBarToContentGap
/** Lets the last feed scroll to the top edge (screen 540 dp - feed item 476 dp). */
private val FeedPinBottomPadding = TvDimens.FeedPinBottomPadding
private val SourceChipSkeletonWidths = listOf(TvDimens.SkeletonChipWidthA, TvDimens.SkeletonChipWidthB, TvDimens.SkeletonChipWidthC)
/** List index 0 is the spacer under the top section (hero + source chips), i.e. the top of Home. */
private const val HomeContinueWatchingListIndex = 0
/** Feed rows follow the top-section spacer. */
private const val FeedSectionsStartIndex = 1

/**
 * Home scrolls itself (hero, pinned feeds). Compose's default focus
 * bring-into-view on TV (pivot at ~30% of the screen) would fight the pinned feed position.
 */
@OptIn(ExperimentalFoundationApi::class)
private val HomeBringIntoViewSpec = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}

private enum class MorePanelCloseTarget {
    MoreButton,
    ContinueWatching,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreenV2Content(
    sourcesUiState: HomeSourcesUiState,
    continueWatchingUiState: HomeContinueWatchingUiState,
    feedsUiState: HomeFeedsUiState,
    onMediaClick: (MediaItemCompat) -> Unit,
    onContinueWatchingPlay: (MediaItemCompat) -> Unit,
    onOpenFeedGrid: (FeedCategory) -> Unit,
    onSourceSelected: (MainAPI) -> Unit,
    onMorePanelOpenChange: (Boolean) -> Unit,
    onTogglePin: (MainAPI) -> Unit,
    onRefreshFeed: (FeedCategory) -> Unit,
    onRemoveContinueWatching: (MediaItemCompat) -> Unit,
    modifier: Modifier = Modifier,
) {
    val heroFocus = remember { ContinueWatchingHeroFocus() }
    val resumeFocusRequester = heroFocus.resume
    val continueWatchingCardsFocusRequester = remember { FocusRequester() }
    val quickSourcesEntryFocusRequester = remember { FocusRequester() }
    val moreButtonFocusRequester = remember { FocusRequester() }
    val firstFeedCardFocusRequester = remember { FocusRequester() }
    val feedRowFocusRequesters = remember { HashMap<String, FocusRequester>() }
    fun feedRowFocusRequester(feedIndex: Int, feedId: String): FocusRequester =
        if (feedIndex == 0) firstFeedCardFocusRequester else feedRowFocusRequesters.getOrPut(feedId) { FocusRequester() }
    val listState = rememberLazyListState(prefetchStrategy = remember { NeighbourRowsPrefetchStrategy() })
    val coroutineScope = rememberCoroutineScope()
    var topSectionHeightPx by remember { mutableIntStateOf(0) }

    var hasInitialFocusBeenRequested by rememberSaveable { mutableStateOf(false) }
    var moreButtonFocusRequestToken by remember { mutableIntStateOf(0) }
    var continueWatchingCardsFocusRequestToken by remember { mutableIntStateOf(0) }
    var feedPinJob by remember { mutableStateOf<Job?>(null) }
    var feedNavJob by remember { mutableStateOf<Job?>(null) }
    // True only while a feed-change slide runs; a pin during that would cancel and restart it.
    var isFeedSliding by remember { mutableStateOf(false) }
    var wasMorePanelOpen by rememberSaveable { mutableStateOf(false) }
    var morePanelCloseTarget by rememberSaveable { mutableStateOf(MorePanelCloseTarget.MoreButton) }

    val isMorePanelOpen = sourcesUiState.isMorePanelOpen
    val hasContinueWatchingItems = (continueWatchingUiState.state as? HomeFeedLoadState.Success)
        ?.items
        ?.isNotEmpty() == true
    // While loading we show the skeleton; once loaded without items the hero disappears.
    val showContinueWatchingHero = hasContinueWatchingItems ||
        continueWatchingUiState.state is HomeFeedLoadState.Loading
    val showQuickSourcesRow = sourcesUiState.allSources.size > 1
    val isMorePanelVisible = showQuickSourcesRow && isMorePanelOpen
    val hasQuickSourceTargets = showQuickSourcesRow && sourcesUiState.quickSources.isNotEmpty()
    val loadingLabel = stringResource(id = R.string.loading)
    val isNoSources = sourcesUiState.isBootstrapped && sourcesUiState.allSources.isEmpty()
    // Nothing below / above: stop, never jump somewhere unexpected.
    val continueWatchingDownFocusRequester = when {
        showQuickSourcesRow -> quickSourcesEntryFocusRequester
        feedsUiState.feedSections.isNotEmpty() -> firstFeedCardFocusRequester
        else -> FocusRequester.Cancel
    }
    val firstFeedUpFocusRequester = when {
        hasQuickSourceTargets -> quickSourcesEntryFocusRequester
        hasContinueWatchingItems -> continueWatchingCardsFocusRequester
        else -> FocusRequester.Cancel
    }
    fun restoreContinueWatchingToTop() {
        if (listState.isScrolledToTop()) return

        coroutineScope.launch {
            listState.animateScrollToItem(HomeContinueWatchingListIndex)
        }
    }

    LaunchedEffect(showQuickSourcesRow, isMorePanelOpen) {
        if (!showQuickSourcesRow && isMorePanelOpen) {
            onMorePanelOpenChange(false)
            return@LaunchedEffect
        }

        if (!isMorePanelVisible && wasMorePanelOpen) {
            when (morePanelCloseTarget) {
                MorePanelCloseTarget.MoreButton -> {
                    moreButtonFocusRequestToken += 1
                }

                MorePanelCloseTarget.ContinueWatching -> {
                    if (hasContinueWatchingItems) {
                        listState.animateScrollToItem(HomeContinueWatchingListIndex)
                        continueWatchingCardsFocusRequestToken += 1
                    } else {
                        moreButtonFocusRequestToken += 1
                    }
                }
            }
            morePanelCloseTarget = MorePanelCloseTarget.MoreButton
        }
        wasMorePanelOpen = isMorePanelVisible
    }

    FocusRequestEffect(
        requester = moreButtonFocusRequester,
        requestKey = moreButtonFocusRequestToken,
        enabled = moreButtonFocusRequestToken > 0
    )

    FocusRequestEffect(
        requester = continueWatchingCardsFocusRequester,
        requestKey = continueWatchingCardsFocusRequestToken,
        enabled = continueWatchingCardsFocusRequestToken > 0
    )

    FocusRequestEffect(
        requester = resumeFocusRequester,
        requestKey = hasContinueWatchingItems to isMorePanelVisible,
        enabled = hasContinueWatchingItems &&
            !isMorePanelVisible &&
            !hasInitialFocusBeenRequested &&
            // Coming back to Home (Back from a title): the screen restores focus itself, not Resume.
            HomeFocusStore.lastTargetId == null,
        onFocused = {
            hasInitialFocusBeenRequested = true
        }
    )

    // Keeps the active feed pinned under the top edge (header at 22 dp), 150 ms transform.
    fun pinFeedToTop(feedId: String) {
        val sectionIndex = feedsUiState.feedSections.indexOfFirst { it.feed.id == feedId }
        if (sectionIndex < 0) return
        val itemIndex = FeedSectionsStartIndex + sectionIndex
        if (isFeedSliding) return
        feedPinJob?.cancel()
        feedPinJob = coroutineScope.launch {
            listState.slideToItem(itemIndex)
        }
    }

    // Used only for Down from the source chips: the first feed may not be composed yet, so slide the
    // list to it while the focus request waits.
    fun focusFeedRow(listIndex: Int, requester: FocusRequester) {
        feedNavJob?.cancel()
        feedPinJob?.cancel()
        val isComposed = listState.layoutInfo.visibleItemsInfo.any { it.index == listIndex }
        feedNavJob = coroutineScope.launch {
            if (!isComposed) {
                launch {
                    isFeedSliding = true
                    try {
                        listState.slideToItem(listIndex)
                    } finally {
                        isFeedSliding = false
                    }
                }
            }
            requester.requestFocusWithRetry(attempts = 18)
        }
    }

    // Up/Down between feeds is the default focus search. The only exception: a neighbour that is still
    // loading has nothing focusable, so the key is consumed instead of jumping past it.
    fun shouldBlockVerticalMove(feedIndex: Int, up: Boolean): Boolean {
        val neighbour = feedsUiState.feedSections.getOrNull(if (up) feedIndex - 1 else feedIndex + 1)
        return neighbour?.state is HomeFeedLoadState.Loading
    }

    // The feed rows are not recomposed when the feeds finish loading, so they must call the latest version
    // (an old one would still see every feed as loading).
    // (A lambda, not a function reference: references compare equal and the state would never update.)
    val latestShouldBlockVerticalMove by rememberUpdatedState { feedIndex: Int, up: Boolean -> shouldBlockVerticalMove(feedIndex, up) }

    // The feed's row gets focus and enters on its remembered poster. A row that is not on screen is scrolled
    // to first; a visible one stays where it is (closing the menu must not move anything).
    suspend fun focusFeed(feedId: String): Boolean {
        val sectionIndex = feedsUiState.feedSections.indexOfFirst { it.feed.id == feedId }
        if (sectionIndex < 0 || feedsUiState.feedSections[sectionIndex].state is HomeFeedLoadState.Loading) return false
        val listIndex = FeedSectionsStartIndex + sectionIndex
        if (!listState.isItemComposed(listIndex)) {
            feedPinJob?.cancel()
            listState.scrollToItem(listIndex)
        }
        // A feed with posters is a focus group that enters on its remembered poster; an empty / failed one is one box.
        val section = feedsUiState.feedSections[sectionIndex]
        val hasPosters = (section.state as? HomeFeedLoadState.Success)?.items?.isNotEmpty() == true
        if (!hasPosters) return feedRowFocusRequester(sectionIndex, feedId).tryRequestFocusWithRetry()
        val sourceId = sourcesUiState.selectedSource?.sourceId()
        return HomeFocusStore.rowGroup(feedRowKey(sourceId, feedId)).requestFocusWithRetry()
    }

    // Entering Home from the menu: Resume while the hero is on screen, otherwise back to where the
    // focus was (feed item / Source), otherwise the first visible section.
    suspend fun enterContent(): Boolean {
        if (hasContinueWatchingItems && listState.isScrolledToTop()) {
            return resumeFocusRequester.tryRequestFocus()
        }
        val feedId = HomeFocusStore.feedIdFromTarget(HomeFocusStore.lastTargetId)
        if (feedId != null && feedsUiState.feedSections.any { it.feed.id == feedId }) return focusFeed(feedId)
        val (index, requester) = when {
            hasContinueWatchingItems -> HomeContinueWatchingListIndex to resumeFocusRequester
            hasQuickSourceTargets -> HomeContinueWatchingListIndex to quickSourcesEntryFocusRequester
            feedsUiState.feedSections.isNotEmpty() -> FeedSectionsStartIndex to firstFeedCardFocusRequester
            else -> return false
        }
        listState.scrollToItem(index)
        return requester.tryRequestFocusWithRetry()
    }

    // Back from a title / the grid / the menu: focus goes to exactly where it was. false = the target is not
    // there yet (still loading), the Dashboard asks again.
    suspend fun restoreContent(): Boolean {
        val last = HomeFocusStore.lastTargetId ?: return enterContent()
        val feedId = HomeFocusStore.feedIdFromTarget(last)
        return when {
            feedId != null -> focusFeed(feedId)
            last == HomeFocusStore.Sources -> when {
                hasQuickSourceTargets -> quickSourcesEntryFocusRequester.tryRequestFocusWithRetry()
                sourcesUiState.isBootstrapped -> enterContent()
                else -> false
            }
            !hasContinueWatchingItems ->
                if (continueWatchingUiState.state is HomeFeedLoadState.Loading) false else enterContent()
            else -> when (last) {
                HomeFocusStore.ContinueWatchingResume -> heroFocus.resume
                HomeFocusStore.ContinueWatchingDetails -> heroFocus.details
                HomeFocusStore.ContinueWatchingRemove -> heroFocus.remove
                else -> null
            }?.tryRequestFocusWithRetry() ?: HomeFocusStore.cardsGroup.requestFocusWithRetry()
        }
    }

    // Back from the rows goes back to the top of Home (the hero's Resume, or the source pills without a
    // hero); from there Back belongs to the Dashboard (it opens the menu).
    fun backToTop(): Boolean {
        val last = HomeFocusStore.lastTargetId
        val isAtTop = if (hasContinueWatchingItems) {
            HomeFocusStore.isHeroButtonTarget(last)
        } else {
            !hasQuickSourceTargets || last == null || last == HomeFocusStore.Sources
        }
        if (isAtTop) return false
        val target = if (hasContinueWatchingItems) resumeFocusRequester else quickSourcesEntryFocusRequester
        HomeFocusStore.resetLastFocused()
        coroutineScope.launch {
            listState.scrollToItem(0)
            target.requestFocusWithRetry(attempts = 18)
        }
        return true
    }
    TvScreenFocusEffect(enter = ::enterContent, restore = ::restoreContent, back = ::backToTop)

    BackHandler(enabled = isMorePanelVisible) {
        morePanelCloseTarget = MorePanelCloseTarget.MoreButton
        onMorePanelOpenChange(false)
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        if (!isNoSources) {
            // Hero and source chips live outside the list (always composed, no pinning needed) and
            // follow its scroll through the placement-phase offset below.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = if (isMorePanelVisible) 0.dp else TvDimens.ScrollbarGutter)
                    // Measured before the offset, so it is the content height.
                    .onSizeChanged { topSectionHeightPx = it.height }
                    .offset {
                        // Read the scroll offset only while the spacer item is the first visible one, so scrolling
                        // deeper in the list does not re-place the top section on every frame.
                        val y = if (listState.firstVisibleItemIndex == 0) {
                            listState.firstVisibleItemScrollOffset
                        } else {
                            topSectionHeightPx
                        }
                        IntOffset(0, -y)
                    }
                    // Hero and chips are not list items, so the list does not scroll to them by itself.
                    .onFocusChanged { if (it.hasFocus) restoreContinueWatchingToTop() }
            ) {
                if (!showContinueWatchingHero) {
                    Spacer(Modifier.height(DashboardContentTopInset + TopBarToContentSpacing))
                }

                if (showContinueWatchingHero) {
                    ContinueWatchingHeroSection(
                        state = continueWatchingUiState.state,
                        heroFocus = heroFocus,
                        cardsFocusRequester = continueWatchingCardsFocusRequester,
                        upFocusRequester = FocusRequester.Cancel,
                        sourceButtonFocusRequester = continueWatchingDownFocusRequester,
                        isInteractive = !isMorePanelVisible,
                        onResumeClick = onContinueWatchingPlay,
                        onDetailsClick = onMediaClick,
                        onRemoveClick = onRemoveContinueWatching,
                        onCardClick = onMediaClick,
                        onFocusTargetFocused = HomeFocusStore::onTargetFocused,
                    )
                }

                if (!sourcesUiState.isBootstrapped) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TvDimens.SourceChipGap),
                        modifier = Modifier.padding(start = HomeContentHorizontalPadding)
                    ) {
                        SourceChipSkeletonWidths.forEach { width ->
                            Spacer(
                                Modifier
                                    .width(width)
                                    .height(TvDimens.ChipHeight)
                                    .tvSkeleton(CircleShape)
                            )
                        }
                    }
                }

                if (showQuickSourcesRow) {
                    Box(
                        modifier = Modifier
                            .padding(start = HomeContentHorizontalPadding)
                            // The first feed is below the hero page and may not be composed yet.
                            .onPreviewKeyEvent { event ->
                                if (
                                    event.type == KeyEventType.KeyDown &&
                                    event.key == Key.DirectionDown &&
                                    !isMorePanelVisible &&
                                    feedsUiState.feedSections.isNotEmpty()
                                ) {
                                    focusFeedRow(FeedSectionsStartIndex, firstFeedCardFocusRequester)
                                    true
                                } else {
                                    false
                                }
                            }
                            .onFocusChanged { if (it.hasFocus) HomeFocusStore.onTargetFocused(HomeFocusStore.Sources) }
                    ) {
                        QuickSourcesRow(
                            quickSources = sourcesUiState.quickSources,
                            allSourcesCount = sourcesUiState.allSources.size,
                            selectedSource = sourcesUiState.selectedSource,
                            rowEntryFocusRequester = quickSourcesEntryFocusRequester,
                            moreButtonFocusRequester = moreButtonFocusRequester,
                            isInteractive = !isMorePanelVisible,
                            upFocusRequester = if (hasContinueWatchingItems) {
                                continueWatchingCardsFocusRequester
                            } else {
                                FocusRequester.Cancel
                            },
                            onSourceSelected = onSourceSelected,
                            onMoreClick = {
                                morePanelCloseTarget = MorePanelCloseTarget.MoreButton
                                onMorePanelOpenChange(true)
                            }
                        )
                    }
                }
            }
        }

        CompositionLocalProvider(LocalBringIntoViewSpec provides HomeBringIntoViewSpec) {
            LazyColumn(
                state = listState,
                // No top padding: the first item is a spacer the height of the top section (the hero bleeds
                // under the transparent top bar), so a pinned feed sits exactly at the top edge.
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = if (isMorePanelVisible) 0.dp else TvDimens.ScrollbarGutter)
            ) {
                if (isNoSources) {
                    item {
                        TvEmptyState(
                            illustration = R.drawable.tv_empty_no_sources,
                            title = stringResource(R.string.tv_home_no_sources_found),
                            message = stringResource(R.string.tv_home_no_sources_message),
                            modifier = Modifier.fillParentMaxSize(),
                        )
                    }
                    return@LazyColumn
                }

                item(key = "top_section_space") {
                    Spacer(Modifier.height(with(LocalDensity.current) { topSectionHeightPx.toDp() }))
                }

                homeFeedSections(
                    feedsUiState = feedsUiState,
                    sourceName = sourcesUiState.selectedSource?.name,
                    sourceId = sourcesUiState.selectedSource?.sourceId(),
                    isMorePanelOpen = isMorePanelVisible,
                    feedRowFocusRequester = ::feedRowFocusRequester,
                    firstSectionUpFocusRequester = firstFeedUpFocusRequester,
                    onMediaClick = onMediaClick,
                    onOpenFeedGrid = onOpenFeedGrid,
                    onSectionFocusChanged = { feedId, hasFocus ->
                        if (hasFocus) pinFeedToTop(feedId)
                    },
                    shouldBlockVerticalMove = { feedIndex, up -> latestShouldBlockVerticalMove(feedIndex, up) },
                    onItemFocused = { feed, item ->
                        HomeFocusStore.onTargetFocused(
                            HomeFocusStore.feedPoster(feed.id, item)
                        )
                    },
                    onRefreshFeed = onRefreshFeed,
                    onShowAllFocused = { feed ->
                        HomeFocusStore.onTargetFocused(
                            HomeFocusStore.feedShowMore(feed.id)
                        )
                    },
                )

                // Lets the last feed scroll up to the top edge (a spacer, not contentPadding, so
                // Compose's focus bring-into-view keeps using the whole viewport).
                if (feedsUiState.feedSections.isNotEmpty()) {
                    item { Spacer(Modifier.height(FeedPinBottomPadding)) }
                }
            }
        }

        SourcesMorePanel(
            visible = isMorePanelVisible,
            sources = sourcesUiState.morePanelSources,
            selectedSource = sourcesUiState.selectedSource,
            pinnedSourceIds = sourcesUiState.pinnedSourceIds,
            onSourceSelected = { source ->
                onSourceSelected(source)
                onMorePanelOpenChange(false)
            },
            onTogglePin = onTogglePin,
            onCloseRequested = {
                morePanelCloseTarget = MorePanelCloseTarget.MoreButton
                onMorePanelOpenChange(false)
            },
            onExitUpRequested = {
                morePanelCloseTarget = MorePanelCloseTarget.ContinueWatching
                onMorePanelOpenChange(false)
            }
        )
    }
}

private fun LazyListState.isScrolledToTop(): Boolean {
    return firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
}


