package com.lagradost.cloudstream3.tv.presentation.screens.home
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.FeedCategory
import com.lagradost.cloudstream3.tv.compat.home.FeedRepositoryImpl
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.utils.bringIntoViewIfChildrenAreFocused
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HOME_FEED_PLACEHOLDER_COUNT = 4

@Composable
fun HomeScreenV2(
    onMediaClick: (MediaItemCompat) -> Unit,
    onContinueWatchingPlay: (MediaItemCompat) -> Unit,
    onOpenFeedGrid: (FeedCategory) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeScreenV2ViewModel = viewModel(
        factory = HomeScreenV2ViewModelFactory(FeedRepositoryImpl())
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    val resumeFocusRequester = remember { FocusRequester() }
    val quickSourcesEntryFocusRequester = remember { FocusRequester() }
    val moreButtonFocusRequester = remember { FocusRequester() }
    val firstFeedCardFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var hasInitialFocusBeenRequested by rememberSaveable { mutableStateOf(false) }
    var isMovingDownFromCards by remember { mutableStateOf(false) }
    var shouldRestoreHeroOnFocus by remember { mutableStateOf(false) }
    var wasMorePanelOpen by rememberSaveable { mutableStateOf(false) }

    val isMorePanelOpen = uiState.isMorePanelOpen
    val loadingLabel = stringResource(id = R.string.loading)
    val noFeedsLabel = stringResource(id = R.string.tv_home_no_feeds)

    val hasContinueWatchingItems = (uiState.continueWatchingState as? HomeFeedLoadState.Success)
        ?.items
        ?.isNotEmpty() == true

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
        viewModel.setMorePanelOpen(false)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C1016))
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(end = if (isMorePanelOpen) 0.dp else 2.dp)
        ) {
            item {
                ContinueWatchingHeroSection(
                    state = uiState.continueWatchingState,
                    resumeFocusRequester = resumeFocusRequester,
                    sourceButtonFocusRequester = quickSourcesEntryFocusRequester,
                    isInteractive = !isMorePanelOpen,
                    modifier = Modifier.bringIntoViewIfChildrenAreFocused(),
                    onResumeClick = onContinueWatchingPlay,
                    onDetailsClick = onMediaClick,
                    onCardClick = onMediaClick,
                    onMoveDownFromCards = {
                        if (isMovingDownFromCards) return@ContinueWatchingHeroSection
                        coroutineScope.launch {
                            isMovingDownFromCards = true
                            if (listState.firstVisibleItemIndex < 1) {
                                listState.animateScrollToItem(1)
                            }
                            shouldRestoreHeroOnFocus = true
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
                        if (!shouldRestoreHeroOnFocus) return@ContinueWatchingHeroSection
                        coroutineScope.launch {
                            listState.scrollToItem(0)
                            shouldRestoreHeroOnFocus = false
                        }
                    }
                )
            }

            item {
                QuickSourcesRow(
                    quickSources = uiState.quickSources,
                    allSourcesCount = uiState.allSources.size,
                    selectedSource = uiState.selectedSource,
                    pinnedSourceIds = uiState.pinnedSourceIds,
                    rowEntryFocusRequester = quickSourcesEntryFocusRequester,
                    moreButtonFocusRequester = moreButtonFocusRequester,
                    isInteractive = !isMorePanelOpen,
                    downFocusRequester = if (uiState.feedSections.isNotEmpty()) {
                        firstFeedCardFocusRequester
                    } else {
                        null
                    },
                    onSourceSelected = { source ->
                        viewModel.selectSource(source)
                    },
                    onMoreClick = {
                        viewModel.setMorePanelOpen(true)
                    }
                )
            }

            if (uiState.feedSections.isEmpty() && uiState.isFeedListLoading) {
                items(count = HOME_FEED_PLACEHOLDER_COUNT) {
                    FeedSection(
                        title = loadingLabel,
                        state = HomeFeedLoadState.Loading,
                        onMediaClick = {},
                        onShowMoreClick = {},
                        isInteractive = false,
                    )
                }
            } else if (uiState.feedSections.isEmpty()) {
                item {
                    Text(
                        text = noFeedsLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.78f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            } else {
                itemsIndexed(
                    items = uiState.feedSections,
                    key = { _, section -> section.feed.id }
                ) { index, section ->
                    FeedSection(
                        title = section.feed.name,
                        state = section.state,
                        onMediaClick = onMediaClick,
                        onShowMoreClick = {
                            onOpenFeedGrid(section.feed)
                        },
                        isInteractive = !isMorePanelOpen,
                        firstItemFocusRequester = if (index == 0) {
                            firstFeedCardFocusRequester
                        } else {
                            null
                        }
                    )
                }
            }
        }

        SourcesMorePanel(
            visible = isMorePanelOpen,
            sources = uiState.morePanelSources,
            selectedSource = uiState.selectedSource,
            pinnedSourceIds = uiState.pinnedSourceIds,
            usageCountBySourceId = uiState.usageCountBySourceId,
            sortMode = uiState.sortMode,
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = viewModel::setSearchQuery,
            onSortModeChange = viewModel::setSortMode,
            onSourceSelected = { source ->
                viewModel.selectSource(source)
                viewModel.setMorePanelOpen(false)
            },
            onTogglePin = viewModel::togglePinned,
            onCloseRequested = {
                viewModel.setMorePanelOpen(false)
            }
        )
    }
}
