package com.lagradost.cloudstream3.tv.presentation.screens.home

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
import com.lagradost.cloudstream3.tv.presentation.utils.bringIntoViewIfChildrenAreFocused
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private val HomeScreenSectionSpacing = 4.dp
private val ContinueWatchingToSourcesSpacing = 8.dp
private val ContinueWatchingToSourcesExtraTopPadding =
    ContinueWatchingToSourcesSpacing - HomeScreenSectionSpacing
private const val HomeContinueWatchingListIndex = 0
private const val HomeFeaturedListIndex = 2

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
    onSourceSelected: (MainAPI) -> Unit,
    onMorePanelOpenChange: (Boolean) -> Unit,
    onTogglePin: (MainAPI) -> Unit,
    onRemoveContinueWatching: (MediaItemCompat) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resumeFocusRequester = remember { FocusRequester() }
    val quickSourcesEntryFocusRequester = remember { FocusRequester() }
    val moreButtonFocusRequester = remember { FocusRequester() }
    val featuredFocusRequester = remember { FocusRequester() }
    val firstFeedCardFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var hasInitialFocusBeenRequested by rememberSaveable { mutableStateOf(false) }
    var isMovingDownFromCards by remember { mutableStateOf(false) }
    var shouldRestoreHeroOnFocus by remember { mutableStateOf(false) }
    var wasMorePanelOpen by rememberSaveable { mutableStateOf(false) }
    var featuredCenterJob by remember { mutableStateOf<Job?>(null) }

    val isMorePanelOpen = sourcesUiState.isMorePanelOpen
    val hasContinueWatchingItems = (continueWatchingUiState.state as? HomeFeedLoadState.Success)
        ?.items
        ?.isNotEmpty() == true
    val featuredItems = (featuredUiState.state as? HomeFeaturedLoadState.Success)?.items
    val hasFeaturedItems = featuredItems?.isNotEmpty() == true
    val loadingLabel = stringResource(id = R.string.loading)
    val noFeedsLabel = stringResource(id = R.string.tv_home_no_feeds)

    LaunchedEffect(Unit) {
        onScroll(true)
    }

    LaunchedEffect(isMorePanelOpen) {
        if (!isMorePanelOpen && wasMorePanelOpen) {
            delay(80)
            moreButtonFocusRequester.requestFocus()
        }
        wasMorePanelOpen = isMorePanelOpen
    }

    LaunchedEffect(hasContinueWatchingItems, isMorePanelOpen, hasInitialFocusBeenRequested) {
        if (!hasContinueWatchingItems || isMorePanelOpen || hasInitialFocusBeenRequested) {
            return@LaunchedEffect
        }

        repeat(20) {
            if (resumeFocusRequester.requestFocus()) {
                hasInitialFocusBeenRequested = true
                return@LaunchedEffect
            }
            delay(16)
        }
    }

    BackHandler(enabled = isMorePanelOpen) {
        onMorePanelOpenChange(false)
    }

    fun markHeroForScrollRestore() {
        shouldRestoreHeroOnFocus = true
    }

    fun restoreHeroScrollIfNeeded() {
        if (!shouldRestoreHeroOnFocus) return

        coroutineScope.launch {
            if (!listState.isScrolledToTop()) {
                listState.scrollToItem(HomeContinueWatchingListIndex)
            }
            shouldRestoreHeroOnFocus = false
        }
    }

    fun centerFeaturedInViewport() {
        if (!hasFeaturedItems) return

        featuredCenterJob?.cancel()
        featuredCenterJob = coroutineScope.launch {
            listState.centerItemInViewport(itemIndex = HomeFeaturedListIndex)
        }
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
                    .padding(end = if (isMorePanelOpen) 0.dp else 2.dp)
            ) {
                item {
                    ContinueWatchingHeroSection(
                        state = continueWatchingUiState.state,
                        resumeFocusRequester = resumeFocusRequester,
                        sourceButtonFocusRequester = quickSourcesEntryFocusRequester,
                        isInteractive = !isMorePanelOpen,
                        modifier = Modifier.bringIntoViewIfChildrenAreFocused(),
                        onResumeClick = onContinueWatchingPlay,
                        onDetailsClick = onMediaClick,
                        onRemoveClick = onRemoveContinueWatching,
                        onCardClick = onMediaClick,
                        onMoveDownFromCards = {
                            if (isMovingDownFromCards) return@ContinueWatchingHeroSection
                            coroutineScope.launch {
                                isMovingDownFromCards = true
                                markHeroForScrollRestore()
                                repeat(16) {
                                    if (quickSourcesEntryFocusRequester.requestFocus()) {
                                        isMovingDownFromCards = false
                                        return@launch
                                    }
                                    delay(16)
                                }
                                isMovingDownFromCards = false
                            }
                        },
                        onHeroContentFocused = {
                            restoreHeroScrollIfNeeded()
                        }
                    )
                }

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
                            isInteractive = !isMorePanelOpen,
                            downFocusRequester = if (hasFeaturedItems) {
                                featuredFocusRequester
                            } else if (feedsUiState.feedSections.isNotEmpty()) {
                                firstFeedCardFocusRequester
                            } else {
                                null
                            },
                            onMoveDown = if (hasFeaturedItems) {
                                {
                                    coroutineScope.launch {
                                        repeat(12) {
                                            if (featuredFocusRequester.requestFocus()) {
                                                return@launch
                                            }
                                            delay(16)
                                        }
                                    }
                                }
                            } else {
                                null
                            },
                            onSourceSelected = onSourceSelected,
                            onMoreClick = {
                                onMorePanelOpenChange(true)
                            }
                        )
                    }
                }

                if (hasFeaturedItems) {
                    item {
                        FeaturedCarousel(
                            items = featuredItems.orEmpty(),
                            focusRequester = featuredFocusRequester,
                            upFocusRequester = quickSourcesEntryFocusRequester,
                            downFocusRequester = if (feedsUiState.feedSections.isNotEmpty()) {
                                firstFeedCardFocusRequester
                            } else {
                                null
                            },
                            isInteractive = !isMorePanelOpen,
                            modifier = Modifier.bringIntoViewIfChildrenAreFocused(),
                            onFocused = ::centerFeaturedInViewport,
                            onItemClick = onMediaClick
                        )
                    }
                }

                homeFeedSections(
                    feedsUiState = feedsUiState,
                    loadingLabel = loadingLabel,
                    noFeedsLabel = noFeedsLabel,
                    isMorePanelOpen = isMorePanelOpen,
                    firstFeedCardFocusRequester = firstFeedCardFocusRequester,
                    onMediaClick = onMediaClick,
                    onOpenFeedGrid = onOpenFeedGrid,
                )
            }

            SourcesMorePanel(
                visible = isMorePanelOpen,
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
