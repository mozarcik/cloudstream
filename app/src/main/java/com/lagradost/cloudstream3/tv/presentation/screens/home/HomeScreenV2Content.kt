package com.lagradost.cloudstream3.tv.presentation.screens.home

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.lagradost.cloudstream3.tv.presentation.common.HaloHost
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.utils.bringIntoViewIfChildrenAreFocused
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

private val HomeScreenSectionSpacing = 4.dp
private val ContinueWatchingToSourcesSpacing = 8.dp
private val ContinueWatchingToSourcesExtraTopPadding =
    ContinueWatchingToSourcesSpacing - HomeScreenSectionSpacing
private const val HomeContinueWatchingListIndex = 0
private const val HomeFocusDebugTag = "TvHomeFocus"

private enum class MorePanelCloseTarget {
    MoreButton,
    ContinueWatching,
}

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
    var wasMorePanelOpen by rememberSaveable { mutableStateOf(false) }
    var morePanelCloseTarget by rememberSaveable { mutableStateOf(MorePanelCloseTarget.MoreButton) }
    var featuredCenterJob by remember { mutableStateOf<Job?>(null) }

    val isMorePanelOpen = sourcesUiState.isMorePanelOpen
    val hasContinueWatchingItems = (continueWatchingUiState.state as? HomeFeedLoadState.Success)
        ?.items
        ?.isNotEmpty() == true
    val showQuickSourcesRow = sourcesUiState.allSources.size > 1
    val isMorePanelVisible = showQuickSourcesRow && isMorePanelOpen
    val hasQuickSourceTargets = showQuickSourcesRow && sourcesUiState.quickSources.isNotEmpty()
    val featuredItems = (featuredUiState.state as? HomeFeaturedLoadState.Success)?.items
    val hasFeaturedItems = featuredItems?.isNotEmpty() == true
    val loadingLabel = stringResource(id = R.string.loading)
    val noFeedsLabel = stringResource(id = R.string.tv_home_no_feeds)
    val pendingRestoreTargetId = HomeFocusStore.pendingRestoreTargetId
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
            pendingRestoreTargetId == null &&
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
            feedSectionsRestoreFocusToken = armedRestoreFocusToken
            Log.d(
                HomeFocusDebugTag,
                "home restore scroll -> feed section index=$homeFeedRestoreListIndex token=$feedSectionsRestoreFocusToken"
            )
        }
    }

    fun consumeArmedRestore(targetId: String) {
        if (armedRestoreTargetId != targetId) {
            return
        }
        HomeFocusStore.clearPendingRestore(targetId)
        armedRestoreTargetId = null
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

    HaloHost(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(HomeScreenSectionSpacing),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = if (isMorePanelVisible) 0.dp else 2.dp)
            ) {
                item {
                    ContinueWatchingHeroSection(
                        state = continueWatchingUiState.state,
                        resumeFocusRequester = resumeFocusRequester,
                        cardsFocusRequester = continueWatchingCardsFocusRequester,
                        upFocusRequester = topBarFocusRequester,
                        sourceButtonFocusRequester = continueWatchingDownFocusRequester,
                        isInteractive = !isMorePanelVisible,
                        pendingRestoreFocusTargetId = armedRestoreTargetId,
                        restoreFocusToken = armedRestoreFocusToken,
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
                        onHeroContentFocused = ::restoreContinueWatchingToTop,
                        onFocusTargetFocused = HomeFocusStore::onTargetFocused,
                        onRestoreFocusConsumed = { targetId ->
                            consumeArmedRestore(targetId)
                        },
                    )
                }

                if (showQuickSourcesRow) {
                    item {
                        Box(
                            modifier = Modifier.padding(top = ContinueWatchingToSourcesExtraTopPadding)
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
                            modifier = Modifier.bringIntoViewIfChildrenAreFocused(),
                            onFocused = {
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
                    loadingLabel = loadingLabel,
                    noFeedsLabel = noFeedsLabel,
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
                    pendingRestoreFocusTargetId = armedRestoreTargetId,
                    restoreFocusToken = feedSectionsRestoreFocusToken,
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
        if (showQuickSourcesRow) 1 else 0 +
        if (hasFeaturedItems) 1 else 0
}
