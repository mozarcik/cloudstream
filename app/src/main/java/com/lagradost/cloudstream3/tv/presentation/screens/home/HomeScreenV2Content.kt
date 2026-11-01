package com.lagradost.cloudstream3.tv.presentation.screens.home

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.lagradost.cloudstream3.tv.presentation.screens.dashboard.LocalDashboardContentEntry
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.MainAPI
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
import com.lagradost.cloudstream3.tv.presentation.focus.requestFocusWithRetry
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.screens.dashboard.DashboardTopBarHeight
import com.lagradost.cloudstream3.tv.presentation.screens.dashboard.LocalDashboardTopBarOpaque
import com.lagradost.cloudstream3.tv.presentation.utils.bringIntoViewIfChildrenAreFocused
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Gap between the source chips and the featured carousel (Home board, 36 px). */
private val SourcesToFeaturedSpacing = TvDimens.SourcesToFeaturedGap
/** Gap between the top bar and the first row when there is no hero. */
private val TopBarToContentSpacing = TvDimens.TopBarToContentGap
/** Lets the last feed scroll to the top edge (screen 540 dp - feed item 476 dp). */
private val FeedPinBottomPadding = TvDimens.FeedPinBottomPadding
private val SourceChipSkeletonWidths = listOf(TvDimens.SkeletonChipWidthA, TvDimens.SkeletonChipWidthB, TvDimens.SkeletonChipWidthC)
/** Longer than the navigation transition that keeps the previous screen composed. */
private const val TopBarReHideDelayMs = 600L
private const val HomeContinueWatchingListIndex = 0
private const val HomeFocusDebugTag = "TvHomeFocus"

/**
 * Home scrolls itself (hero, centred Featured, pinned feeds). Compose's default focus
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
    featuredUiState: HomeFeaturedUiState,
    feedsUiState: HomeFeedsUiState,
    onMediaClick: (MediaItemCompat) -> Unit,
    onContinueWatchingPlay: (MediaItemCompat) -> Unit,
    onOpenFeedGrid: (FeedCategory) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    topBarFocusRequester: FocusRequester,
    onSourceSelected: (MainAPI) -> Unit,
    onMorePanelOpenChange: (Boolean) -> Unit,
    onTogglePin: (MainAPI) -> Unit,
    onRemoveContinueWatching: (MediaItemCompat) -> Unit,
    restoreFocusToken: Int = 0,
    modifier: Modifier = Modifier,
) {
    val resumeFocusRequester = remember { FocusRequester() }
    val continueWatchingCardsFocusRequester = remember { FocusRequester() }
    val quickSourcesEntryFocusRequester = remember { FocusRequester() }
    val moreButtonFocusRequester = remember { FocusRequester() }
    val featuredFocusRequester = remember { FocusRequester() }
    val firstFeedCardFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var hasInitialFocusBeenRequested by rememberSaveable { mutableStateOf(false) }
    var moreButtonFocusRequestToken by remember { mutableIntStateOf(0) }
    var continueWatchingCardsFocusRequestToken by remember { mutableIntStateOf(0) }
    var feedSectionsRestoreFocusToken by remember { mutableIntStateOf(0) }
    var armedRestoreFocusToken by rememberSaveable { mutableIntStateOf(0) }
    var armedRestoreTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    var feedNavTargetId by remember { mutableStateOf<String?>(null) }
    var feedNavToken by remember { mutableIntStateOf(0) }
    var isTopBarHiddenByFeeds by remember { mutableStateOf(false) }
    var feedPinJob by remember { mutableStateOf<Job?>(null) }
    var wasMorePanelOpen by rememberSaveable { mutableStateOf(false) }
    var morePanelCloseTarget by rememberSaveable { mutableStateOf(MorePanelCloseTarget.MoreButton) }
    var featuredCenterJob by remember { mutableStateOf<Job?>(null) }

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
    val featuredItems = (featuredUiState.state as? HomeFeaturedLoadState.Success)?.items
    val hasFeaturedItems = featuredItems?.isNotEmpty() == true
    val loadingLabel = stringResource(id = R.string.loading)
    val isNoSources = sourcesUiState.isBootstrapped && sourcesUiState.allSources.isEmpty()
    val pendingRestoreTargetId = HomeFocusStore.pendingRestoreTargetId
    val hasPendingExternalRestore = restoreFocusToken > 0 && pendingRestoreTargetId != null
    val continueWatchingRestoreTargetId = armedRestoreTargetId
        ?.takeIf { HomeFocusStore.isContinueWatchingTarget(it) }
    val continueWatchingRestoreFocusToken = if (continueWatchingRestoreTargetId != null) {
        armedRestoreFocusToken
    } else {
        0
    }
    val continueWatchingDownFocusRequester = when {
        showQuickSourcesRow -> quickSourcesEntryFocusRequester
        hasFeaturedItems -> featuredFocusRequester
        feedsUiState.feedSections.isNotEmpty() -> firstFeedCardFocusRequester
        else -> topBarFocusRequester
    }
    val featuredUpFocusRequester = when {
        hasQuickSourceTargets -> quickSourcesEntryFocusRequester
        hasContinueWatchingItems -> continueWatchingCardsFocusRequester
        else -> topBarFocusRequester
    }
    val firstFeedUpFocusRequester = when {
        hasFeaturedItems -> featuredFocusRequester
        hasQuickSourceTargets -> quickSourcesEntryFocusRequester
        hasContinueWatchingItems -> continueWatchingCardsFocusRequester
        else -> topBarFocusRequester
    }
    val featuredListIndex = remember(showQuickSourcesRow) {
        homeFeaturedListIndex(showQuickSourcesRow = showQuickSourcesRow)
    }
    val feedSectionsStartIndex = remember(showQuickSourcesRow, hasFeaturedItems) {
        homeFeedSectionsStartIndex(
            showQuickSourcesRow = showQuickSourcesRow,
            hasFeaturedItems = hasFeaturedItems
        )
    }
    val homeFeedRestoreListIndex = remember(
        armedRestoreTargetId,
        feedsUiState.feedSections,
        feedSectionsStartIndex
    ) {
        val feedId = HomeFocusStore.feedIdFromTarget(armedRestoreTargetId) ?: return@remember null
        val sectionIndex = feedsUiState.feedSections.indexOfFirst { section ->
            section.feed.id == feedId
        }.takeIf { it >= 0 } ?: return@remember null

        feedSectionsStartIndex + sectionIndex
    }

    fun restoreContinueWatchingToTop() {
        if (listState.isScrolledToTop() || listState.isScrollInProgress) return

        coroutineScope.launch {
            listState.animateScrollToItem(HomeContinueWatchingListIndex)
        }
    }

    fun centerFeaturedInViewport() {
        if (!hasFeaturedItems) return

        featuredCenterJob?.cancel()
        featuredCenterJob = coroutineScope.launch {
            listState.centerItemInViewport(itemIndex = featuredListIndex)
        }
    }

    LaunchedEffect(Unit) {
        onScroll(true)
    }

    val setTopBarOpaque = LocalDashboardTopBarOpaque.current
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 24
        }.collect { setTopBarOpaque(it) }
    }

    LaunchedEffect(restoreFocusToken) {
        if (restoreFocusToken <= 0 || restoreFocusToken == armedRestoreFocusToken) {
            return@LaunchedEffect
        }

        armedRestoreFocusToken = restoreFocusToken
        armedRestoreTargetId = HomeFocusStore.pendingRestoreTargetId
        Log.d(
            HomeFocusDebugTag,
            "home arm restore token=$armedRestoreFocusToken target=$armedRestoreTargetId"
        )
    }

    LaunchedEffect(showQuickSourcesRow, isMorePanelOpen) {
        if (!showQuickSourcesRow && isMorePanelOpen) {
            onMorePanelOpenChange(false)
            return@LaunchedEffect
        }

        if (!isMorePanelVisible && wasMorePanelOpen) {
            when (morePanelCloseTarget) {
                MorePanelCloseTarget.MoreButton -> {
                    Log.d(HomeFocusDebugTag, "more panel close -> more button")
                    moreButtonFocusRequestToken += 1
                }

                MorePanelCloseTarget.ContinueWatching -> {
                    if (hasContinueWatchingItems) {
                        Log.d(HomeFocusDebugTag, "more panel close -> continue watching")
                        listState.animateScrollToItem(HomeContinueWatchingListIndex)
                        continueWatchingCardsFocusRequestToken += 1
                    } else {
                        Log.d(HomeFocusDebugTag, "more panel close fallback -> more button")
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
        enabled = continueWatchingCardsFocusRequestToken > 0,
        onFocused = {
            restoreContinueWatchingToTop()
        }
    )

    FocusRequestEffect(
        requester = resumeFocusRequester,
        requestKey = hasContinueWatchingItems to isMorePanelVisible,
        enabled = hasContinueWatchingItems &&
            !isMorePanelVisible &&
            !hasInitialFocusBeenRequested &&
            !hasPendingExternalRestore &&
            armedRestoreTargetId == null,
        onFocused = {
            hasInitialFocusBeenRequested = true
        }
    )

    LaunchedEffect(armedRestoreFocusToken, armedRestoreTargetId, homeFeedRestoreListIndex) {
        if (armedRestoreFocusToken <= 0) return@LaunchedEffect
        Log.d(
            HomeFocusDebugTag,
            "home restore token=$armedRestoreFocusToken target=$armedRestoreTargetId feedIndex=$homeFeedRestoreListIndex"
        )
        if (HomeFocusStore.isContinueWatchingTarget(armedRestoreTargetId)) {
            listState.scrollToItem(HomeContinueWatchingListIndex)
            Log.d(HomeFocusDebugTag, "home restore scroll -> continue watching")
            return@LaunchedEffect
        }
        if (armedRestoreTargetId == HomeFocusStore.Featured) {
            listState.scrollToItem(featuredListIndex)
            Log.d(HomeFocusDebugTag, "home restore scroll -> featured")
            return@LaunchedEffect
        }
        if (homeFeedRestoreListIndex != null) {
            listState.scrollToItem(homeFeedRestoreListIndex)
            feedSectionsRestoreFocusToken += 1
            Log.d(
                HomeFocusDebugTag,
                "home restore scroll -> feed section index=$homeFeedRestoreListIndex token=$feedSectionsRestoreFocusToken"
            )
        }
    }

    fun consumeArmedRestore(targetId: String) {
        if (feedNavTargetId == targetId) {
            feedNavTargetId = null
        }
        if (HomeFocusStore.feedIdFromTarget(targetId) != null && armedRestoreTargetId == targetId) {
            // Back from "Show more"/details: the screen we came from shows the top bar when it
            // leaves composition (after the transition), so hide it again for the restored feed.
            coroutineScope.launch {
                // The state may be fresh after the other screen (flag false), so hide unconditionally, and
                // once more for slow transitions.
                repeat(2) {
                    delay(TopBarReHideDelayMs)
                    isTopBarHiddenByFeeds = true
                    onScroll(false)
                }
            }
        }
        if (armedRestoreTargetId != targetId) {
            return
        }
        HomeFocusStore.clearPendingRestore(targetId)
        armedRestoreTargetId = null
    }

    // Keeps the active feed pinned under the top edge (header at 22 dp), 150 ms transform.
    fun pinFeedToTop(feedId: String) {
        val sectionIndex = feedsUiState.feedSections.indexOfFirst { it.feed.id == feedId }
        if (sectionIndex < 0) return
        val itemIndex = feedSectionsStartIndex + sectionIndex
        feedPinJob?.cancel()
        feedPinJob = coroutineScope.launch {
            val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == itemIndex }
                ?: return@launch
            val delta = (item.offset - listState.layoutInfo.viewportStartOffset).toFloat()
            if (abs(delta) > 1f) {
                listState.animateScrollBy(delta, tween(durationMillis = TvDimens.MaxAnimationMs))
            }
        }
    }

    // Up/Down out of a mini-grid. Neighbouring feeds may not be composed (the list is pinned), so
    // the list is scrolled first and the focus is restored to the same column.
    val feedRequesters = remember { HashMap<String, FocusRequester>() }
    fun feedRequester(feedId: String) = feedRequesters.getOrPut(feedId) { FocusRequester() }

    fun navigateFromFeed(feedIndex: Int, up: Boolean, column: Int): Boolean {
        val sections = feedsUiState.feedSections
        val targetIndex = if (up) feedIndex - 1 else feedIndex + 1
        val target = sections.getOrNull(targetIndex)
        if (target == null) {
            if (!up || feedIndex != 0) return false
            val scrollIndex = when {
                hasFeaturedItems -> featuredListIndex
                hasQuickSourceTargets -> HomeContinueWatchingListIndex + 1
                hasContinueWatchingItems -> HomeContinueWatchingListIndex
                else -> return false
            }
            coroutineScope.launch {
                listState.animateScrollToItem(scrollIndex)
                firstFeedUpFocusRequester.requestFocusWithRetry(attempts = 18)
            }
            return true
        }
        val success = target.state as? HomeFeedLoadState.Success
        val config = success?.let { resolveFeedMiniGridConfig(it.items) }
        if (target.state is HomeFeedLoadState.Loading) return false
        if (config == null || config.totalSlots == 0) {
            // Empty / failed feed: its box is the focus target; scroll to it with the same slide.
            val requester = if (targetIndex == 0) firstFeedCardFocusRequester else feedRequester(target.feed.id)
            coroutineScope.launch {
                listState.slideToItem(feedSectionsStartIndex + targetIndex)
                requester.requestFocusWithRetry(attempts = 18)
            }
            return true
        }
        val slot = feedEntrySlot(config, fromAbove = !up, column = column)
        feedNavTargetId = feedSlotTargetId("home:feed:${target.feed.id}", config, slot)
        feedNavToken += 1
        return true
    }

    // "Down" from the top bar: Resume while the hero is on screen, otherwise back to where the
    // focus was (feed item / Featured / Source), otherwise the first visible section.
    fun enterFromTopBar(): Boolean {
        val last = HomeFocusStore.lastTargetId
        if (hasContinueWatchingItems && listState.isScrolledToTop()) {
            return resumeFocusRequester.tryRequestFocus()
        }
        val feedId = HomeFocusStore.feedIdFromTarget(last)
        if (feedId != null && feedsUiState.feedSections.any { it.feed.id == feedId }) {
            feedNavTargetId = last
            feedNavToken += 1
            return true
        }
        val (index, requester) = when {
            last == HomeFocusStore.Featured && hasFeaturedItems -> featuredListIndex to featuredFocusRequester
            hasContinueWatchingItems -> HomeContinueWatchingListIndex to resumeFocusRequester
            hasQuickSourceTargets -> (HomeContinueWatchingListIndex + 1) to quickSourcesEntryFocusRequester
            hasFeaturedItems -> featuredListIndex to featuredFocusRequester
            feedsUiState.feedSections.isNotEmpty() -> feedSectionsStartIndex to firstFeedCardFocusRequester
            else -> return false
        }
        coroutineScope.launch {
            listState.scrollToItem(index)
            requester.requestFocusWithRetry(attempts = 18)
        }
        return true
    }
    val contentEntry = LocalDashboardContentEntry.current
    // Re-registered whenever what it reads changes (a stale closure would see the loading state).
    DisposableEffect(
        contentEntry, hasContinueWatchingItems, hasQuickSourceTargets, hasFeaturedItems,
        feedsUiState.feedSections, featuredListIndex, feedSectionsStartIndex,
    ) {
        val handler: () -> Boolean = { enterFromTopBar() }
        contentEntry.handler = handler
        onDispose { if (contentEntry.handler === handler) contentEntry.handler = null }
    }

    LaunchedEffect(feedNavToken) {
        if (feedNavToken <= 0) return@LaunchedEffect
        val feedId = HomeFocusStore.feedIdFromTarget(feedNavTargetId) ?: return@LaunchedEffect
        val sectionIndex = feedsUiState.feedSections.indexOfFirst { it.feed.id == feedId }
        if (sectionIndex < 0) return@LaunchedEffect
        listState.slideToItem(feedSectionsStartIndex + sectionIndex)
        feedSectionsRestoreFocusToken += 1
    }

    // The top bar hides while a feed has focus and returns when focus moves to the hero, source
    // chips or Featured. Driven by focus gain only, so leaving Home never fights another screen.
    fun showTopBar() {
        if (isTopBarHiddenByFeeds) {
            isTopBarHiddenByFeeds = false
            onScroll(true)
        }
    }

    DisposableEffect(contentEntry) {
        val reset: () -> Unit = {
            HomeFocusStore.resetLastFocused()
            showTopBar()
            coroutineScope.launch { listState.scrollToItem(0) }
        }
        contentEntry.resetToTop = reset
        onDispose { if (contentEntry.resetToTop === reset) contentEntry.resetToTop = null }
    }


    FocusRequestEffect(
        requester = if (armedRestoreTargetId == HomeFocusStore.Featured) {
            featuredFocusRequester
        } else {
            null
        },
        requestKey = armedRestoreFocusToken to armedRestoreTargetId,
        enabled = armedRestoreFocusToken > 0 && armedRestoreTargetId == HomeFocusStore.Featured,
        onFocused = {
            centerFeaturedInViewport()
            consumeArmedRestore(HomeFocusStore.Featured)
        }
    )

    BackHandler(enabled = isMorePanelVisible) {
        morePanelCloseTarget = MorePanelCloseTarget.MoreButton
        onMorePanelOpenChange(false)
    }

    run {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            CompositionLocalProvider(LocalBringIntoViewSpec provides HomeBringIntoViewSpec) {
            LazyColumn(
                state = listState,
                // No top padding: the first item is the hero (bleeds under the transparent top bar)
                // or a spacer, so a pinned feed sits exactly at the top edge.
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = if (isMorePanelVisible) 0.dp else TvDimens.ScrollbarGutter)
            ) {
                if (isNoSources) {
                    item {
                        TvEmptyState(
                            icon = Icons.Default.Extension,
                            title = stringResource(R.string.tv_home_no_sources_found),
                            message = stringResource(R.string.tv_home_no_sources_message),
                            modifier = Modifier.fillParentMaxSize(),
                        )
                    }
                    return@LazyColumn
                }

                if (!showContinueWatchingHero) {
                    item { Spacer(Modifier.height(DashboardTopBarHeight + TopBarToContentSpacing)) }
                }

                if (showContinueWatchingHero) item {
                    ContinueWatchingHeroSection(
                        state = continueWatchingUiState.state,
                        resumeFocusRequester = resumeFocusRequester,
                        cardsFocusRequester = continueWatchingCardsFocusRequester,
                        upFocusRequester = topBarFocusRequester,
                        sourceButtonFocusRequester = continueWatchingDownFocusRequester,
                        isInteractive = !isMorePanelVisible,
                        pendingRestoreFocusTargetId = continueWatchingRestoreTargetId,
                        restoreFocusToken = continueWatchingRestoreFocusToken,
                        modifier = Modifier.bringIntoViewIfChildrenAreFocused(),
                        onResumeClick = { item ->
                            HomeFocusStore.scheduleRestoreToLastFocused()
                            onContinueWatchingPlay(item)
                        },
                        onDetailsClick = { item ->
                            HomeFocusStore.scheduleRestoreToLastFocused()
                            onMediaClick(item)
                        },
                        onRemoveClick = onRemoveContinueWatching,
                        onCardClick = { item ->
                            HomeFocusStore.scheduleRestoreToLastFocused()
                            onMediaClick(item)
                        },
                        onHeroContentFocused = {
                            showTopBar()
                            restoreContinueWatchingToTop()
                        },
                        onFocusTargetFocused = HomeFocusStore::onTargetFocused,
                        onRestoreFocusConsumed = ::consumeArmedRestore,
                    )
                }

                if (!sourcesUiState.isBootstrapped) {
                    item {
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
                }

                if (showQuickSourcesRow) {
                    item {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = HomeContentHorizontalPadding - TvDimens.SourceRowInset)
                                // Chips sit at the bottom of the hero page: bring it back into view.
                                .onFocusChanged { focusState ->
                                    if (focusState.hasFocus) showTopBar()
                                    if (focusState.hasFocus && !listState.isScrolledToTop()) {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(HomeContinueWatchingListIndex)
                                        }
                                    }
                                }
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
                                    topBarFocusRequester
                                },
                                downFocusRequester = if (hasFeaturedItems) {
                                    featuredFocusRequester
                                } else if (feedsUiState.feedSections.isNotEmpty()) {
                                    firstFeedCardFocusRequester
                                } else {
                                    null
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

                if (hasFeaturedItems) {
                    item {
                        FeaturedCarousel(
                            items = featuredItems.orEmpty(),
                            focusRequester = featuredFocusRequester,
                            upFocusRequester = featuredUpFocusRequester,
                            downFocusRequester = if (feedsUiState.feedSections.isNotEmpty()) {
                                firstFeedCardFocusRequester
                            } else {
                                null
                            },
                            isInteractive = !isMorePanelVisible,
                            modifier = Modifier
                                .padding(
                                    start = HomeContentHorizontalPadding,
                                    end = HomeContentHorizontalPadding,
                                    top = SourcesToFeaturedSpacing,
                                    bottom = TvDimens.FeedSectionBottomPadding,
                                )
                                .bringIntoViewIfChildrenAreFocused(),
                            onFocused = {
                                // Mockup HomeFeatured has no bar: hide it so "Source" is fully visible.
                                onScroll(false)
                                isTopBarHiddenByFeeds = true
                                HomeFocusStore.onTargetFocused(HomeFocusStore.Featured)
                                centerFeaturedInViewport()
                            },
                            onItemClick = { item ->
                                HomeFocusStore.scheduleRestoreToLastFocused()
                                onMediaClick(item)
                            }
                        )
                    }
                }

                homeFeedSections(
                    feedsUiState = feedsUiState,
                    sourceName = sourcesUiState.selectedSource?.name,
                    loadingLabel = loadingLabel,
                    isMorePanelOpen = isMorePanelVisible,
                    firstFeedCardFocusRequester = firstFeedCardFocusRequester,
                    firstSectionUpFocusRequester = firstFeedUpFocusRequester,
                    onMediaClick = { item ->
                        HomeFocusStore.scheduleRestoreToLastFocused()
                        onMediaClick(item)
                    },
                    onOpenFeedGrid = { feed ->
                        HomeFocusStore.scheduleRestoreToLastFocused()
                        onOpenFeedGrid(feed)
                    },
                    pendingRestoreFocusTargetId = feedNavTargetId ?: armedRestoreTargetId,
                    restoreFocusToken = feedSectionsRestoreFocusToken,
                    onSectionFocusChanged = { feedId, hasFocus ->
                        if (hasFocus) {
                            // Always (not only on change): Back shows the bar without leaving the feed state.
                            onScroll(false)
                            isTopBarHiddenByFeeds = true
                            pinFeedToTop(feedId)
                        }
                    },
                    onVerticalExit = ::navigateFromFeed,
                    feedFocusRequester = ::feedRequester,
                    onItemFocused = { feed, item ->
                        HomeFocusStore.onTargetFocused(
                            HomeFocusStore.feedPoster(feed.id, item)
                        )
                    },
                    onShowMoreFocused = { feed ->
                        HomeFocusStore.onTargetFocused(
                            HomeFocusStore.feedShowMore(feed.id)
                        )
                    },
                    onRestoreFocusConsumed = { targetId ->
                        consumeArmedRestore(targetId)
                    }
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
                usageCountBySourceId = sourcesUiState.usageCountBySourceId,
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
}

private fun LazyListState.isScrolledToTop(): Boolean {
    return firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
}

private suspend fun LazyListState.centerItemInViewport(
    itemIndex: Int,
) {
    var targetItem = layoutInfo.visibleItemsInfo.firstOrNull { item -> item.index == itemIndex }
    if (targetItem == null) {
        animateScrollToItem(itemIndex)
        targetItem = layoutInfo.visibleItemsInfo.firstOrNull { item -> item.index == itemIndex }
            ?: return
    }

    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
    val itemCenter = targetItem.offset + (targetItem.size / 2f)
    val delta = itemCenter - viewportCenter

    if (abs(delta) > 1f) {
        animateScrollBy(delta)
    }
}

private fun homeFeaturedListIndex(
    showQuickSourcesRow: Boolean,
): Int {
    return HomeContinueWatchingListIndex + 1 + if (showQuickSourcesRow) 1 else 0
}

private fun homeFeedSectionsStartIndex(
    showQuickSourcesRow: Boolean,
    hasFeaturedItems: Boolean,
): Int {
    return HomeContinueWatchingListIndex +
        1 +
        (if (showQuickSourcesRow) 1 else 0) +
        (if (hasFeaturedItems) 1 else 0)
}

private fun FocusRequester.tryRequestFocus(): Boolean = runCatching { requestFocus() }.getOrDefault(false)
