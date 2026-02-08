package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.FeedRepositoryImpl
import com.lagradost.cloudstream3.tv.compat.home.SourceRepository
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.presentation.common.Loading

@Composable
fun HomeScreen(
    onMediaClick: (com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat) -> Unit,
    goToVideoPlayer: (movie: Movie) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
    feedViewModel: FeedViewModel = viewModel(
        factory = FeedViewModelFactory(FeedRepositoryImpl())
    ),
    mediaGridViewModel: MediaGridViewModel = viewModel(
        factory = MediaGridViewModelFactory(FeedRepositoryImpl())
    ),
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Task 1.5: Observe API changes
    val currentApi by SourceRepository.selectedApi.collectAsState(initial = null)
    
    // Task 3.4: Observe feeds from ViewModel
    val feeds by feedViewModel.feeds.collectAsState()
    val selectedFeed by mediaGridViewModel.selectedFeed.collectAsState()
    var selectedFeedIndex by remember { mutableStateOf(0) }
    
    // Task 5.1: Observe paging data from MediaGridViewModel
    val pagingItems = mediaGridViewModel.pagingData.collectAsLazyPagingItems()
    
    // Task 4.4: Overlay state
    var feedMenuState by remember { mutableStateOf<FeedMenuState>(FeedMenuState.Closed) }
    var isFirstGridRowFocused by remember { mutableStateOf(true) }
    var availableApis by remember { mutableStateOf<List<MainAPI>>(emptyList()) }
    var isSourcePickerExpanded by remember { mutableStateOf(false) }
    var pendingGridRestoreAfterResume by rememberSaveable { mutableStateOf(false) }
    var resumeToken by rememberSaveable { mutableIntStateOf(0) }
    var lastClickedGridIndex by rememberSaveable { mutableIntStateOf(-1) }
    var lastClickedGridItemKey by rememberSaveable { mutableStateOf<String?>(null) }
    
    // Task 4.2: Focus requesters
    val feedFocusRequester = remember { FocusRequester() }
    val gridFocusRequester = remember { FocusRequester() }
    val breadcrumbFocusRequester = remember { FocusRequester() }
    var gridFocusRequestToken by remember { mutableIntStateOf(0) }
    val gridState = rememberLazyGridState()
    val isFeedMenuOpened = feedMenuState is FeedMenuState.Opened
    val breadcrumbState by remember(isFirstGridRowFocused) {
        derivedStateOf {
            if (isFirstGridRowFocused) {
                onScroll(true)
                BreadcrumbState.Expanded
            } else {
                onScroll(false)
                BreadcrumbState.Compact
            }
        }
    }
    val isBreadcrumbCompact by remember(
        breadcrumbState,
        pendingGridRestoreAfterResume,
        gridState.firstVisibleItemIndex
    ) {
        derivedStateOf {
            breadcrumbState is BreadcrumbState.Compact ||
                (pendingGridRestoreAfterResume && gridState.firstVisibleItemIndex > 0)
        }
    }
    val gridTopPadding by animateDpAsState(
        targetValue = if (isBreadcrumbCompact) 0.dp else 68.dp,
        label = "home_grid_top_padding"
    )
    
    // Load default feed once feeds are available, and keep local index in sync.
    LaunchedEffect(feeds, selectedFeed?.id) {
        if (feeds.isEmpty()) return@LaunchedEffect

        val selectedIndex = selectedFeed?.id?.let { selectedFeedId ->
            feeds.indexOfFirst { it.id == selectedFeedId }
        } ?: -1

        if (selectedIndex >= 0) {
            selectedFeedIndex = selectedIndex
            return@LaunchedEffect
        }

        selectedFeedIndex = 0
        mediaGridViewModel.selectFeed(feeds.first())
    }
    
    LaunchedEffect(currentApi) {
        android.util.Log.d("HomeScreen", "API changed to: ${currentApi?.name}")
    }

    LaunchedEffect(Unit) {
        onScroll(true)
    }

    LaunchedEffect(feedMenuState) {
        if (feedMenuState is FeedMenuState.Closed) {
            isSourcePickerExpanded = false
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(100)
        availableApis = SourceRepository.getAvailableApis()
    }

    val feedOverlayWidthFraction by animateFloatAsState(
        targetValue = if (isSourcePickerExpanded) 0.78f else 0.38f,
        label = "home_feed_overlay_width"
    )
    
    val isGridLoading = pagingItems.loadState.refresh is LoadState.Loading
    val canFocusGrid = pagingItems.itemCount > 0 && !isGridLoading
    val restorePreferredFocusIndex = if (pendingGridRestoreAfterResume) lastClickedGridIndex else -1
    val restorePreferredFocusItemKey = if (pendingGridRestoreAfterResume) lastClickedGridItemKey else null
    val focusFeedId = remember(feeds, selectedFeedIndex, selectedFeed?.id) {
        feeds.getOrNull(selectedFeedIndex)?.id ?: selectedFeed?.id ?: "no_feed"
    }
    val mediaFocusContextKey = remember(currentApi?.name, focusFeedId) {
        "${currentApi?.name.orEmpty()}|$focusFeedId"
    }

    fun openFeedMenu() {
        feedMenuState = FeedMenuState.Opened
    }

    fun closeFeedMenu() {
        feedMenuState = FeedMenuState.Closed
        isSourcePickerExpanded = false
    }

    fun requestGridFocus() {
        if (!canFocusGrid) return
        gridFocusRequestToken += 1
    }

    // Focus selected feed row whenever feed menu opens.
    LaunchedEffect(feedMenuState, feeds.isNotEmpty()) {
        if (feedMenuState is FeedMenuState.Opened && feeds.isNotEmpty()) {
            kotlinx.coroutines.delay(140)
            feedFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(canFocusGrid, isFeedMenuOpened, pendingGridRestoreAfterResume) {
        if (canFocusGrid && !isFeedMenuOpened) {
            requestGridFocus()
        }
    }

    val latestPendingGridRestoreAfterResume by rememberUpdatedState(pendingGridRestoreAfterResume)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && latestPendingGridRestoreAfterResume) {
                resumeToken += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(resumeToken, canFocusGrid, isFeedMenuOpened, pendingGridRestoreAfterResume) {
        if (resumeToken == 0 || !pendingGridRestoreAfterResume) return@LaunchedEffect
        if (!canFocusGrid || isFeedMenuOpened) return@LaunchedEffect
        kotlinx.coroutines.delay(80)
        requestGridFocus()
    }

    // Overlay should close first when Back is pressed.
    BackHandler(enabled = isFeedMenuOpened) {
        closeFeedMenu()
    }

    // Task 4.1/4.3/4.4: Grid + Overlay layout
    when {
        feeds.isEmpty() -> {
            // Show loading while feeds are loading
            Loading(modifier = Modifier.fillMaxSize())
        }
        else -> {
            Box(modifier = Modifier.fillMaxSize()) {
                // Task 5.1: MediaGrid with paging (always visible, dimmed when overlay shown)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = gridTopPadding)
                        .alpha(if (isFeedMenuOpened) 0.3f else 1f)
                        .focusProperties { canFocus = !canFocusGrid }
                        .focusable()
                ) {
                    MediaGrid(
                        pagingItems = pagingItems,
                        onMediaClick = { item, index, itemKey ->
                            android.util.Log.d("HomeScreen", "Clicked: ${item.name}, url=${item.url}, api=${item.apiName}")
                            lastClickedGridIndex = index
                            lastClickedGridItemKey = itemKey
                            pendingGridRestoreAfterResume = true
                            onMediaClick(item)
                        },
                        onOpenFeedMenu = { openFeedMenu() },
                        onFirstRowFocusChanged = { isFirstRowFocused ->
                            isFirstGridRowFocused = isFirstRowFocused
                        },
                        onGridItemFocused = {
                            if (pendingGridRestoreAfterResume) {
                                pendingGridRestoreAfterResume = false
                                lastClickedGridIndex = -1
                                lastClickedGridItemKey = null
                            }
                        },
                        breadcrumbFocusRequester = breadcrumbFocusRequester,
                        focusContextKey = mediaFocusContextKey,
                        isFeedMenuOpen = isFeedMenuOpened,
                        externalFocusRequestToken = gridFocusRequestToken,
                        preferredFocusIndex = restorePreferredFocusIndex,
                        preferredFocusItemKey = restorePreferredFocusItemKey,
                        focusRequester = gridFocusRequester,
                        gridState = gridState,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                BrowseBreadcrumbBar(
                    currentSource = currentApi?.name ?: "Loading",
                    currentFeed = selectedFeed?.name ?: feeds.firstOrNull()?.name ?: "Loading",
                    compact = isBreadcrumbCompact,
                    onOpenFeedMenu = { openFeedMenu() },
                    onMoveToGrid = {
                        requestGridFocus()
                    },
                    focusRequester = breadcrumbFocusRequester,
                    downFocusRequester = gridFocusRequester,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp),
                    isInteractive = feedMenuState is FeedMenuState.Closed
                )
                
                // Task 4.4: Sidebar overlay with animation
                AnimatedVisibility(
                    visible = isFeedMenuOpened,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
                    ) {
                    }
                }

                AnimatedVisibility(
                    visible = isFeedMenuOpened,
                    enter = slideInHorizontally() + fadeIn(),
                    exit = slideOutHorizontally() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        FeedSidebar(
                            feeds = feeds,
                            selectedIndex = selectedFeedIndex,
                            currentSourceName = currentApi?.name ?: "Loading",
                            availableSources = availableApis,
                            selectedSource = currentApi,
                            onFeedSelected = { index ->
                                selectedFeedIndex = index
                                android.util.Log.d("HomeScreen", "Selected feed: ${feeds[index].name}")
                                // Task 5.1: Load data for selected feed
                                mediaGridViewModel.selectFeed(feeds[index])
                                pendingGridRestoreAfterResume = false
                                lastClickedGridIndex = -1
                                lastClickedGridItemKey = null
                                closeFeedMenu()
                            },
                            onSourceSelected = { source ->
                                if (source.name != currentApi?.name) {
                                    SourceRepository.selectApi(source)
                                    selectedFeedIndex = 0
                                }
                                pendingGridRestoreAfterResume = false
                                lastClickedGridIndex = -1
                                lastClickedGridItemKey = null
                                closeFeedMenu()
                            },
                            onCloseRequested = { closeFeedMenu() },
                            onSourcePickerVisibilityChanged = { expanded ->
                                isSourcePickerExpanded = expanded
                            },
                            focusRequester = feedFocusRequester,
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(feedOverlayWidthFraction)
                                .align(Alignment.CenterStart)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseBreadcrumbBar(
    currentSource: String,
    currentFeed: String,
    compact: Boolean,
    onOpenFeedMenu: () -> Unit,
    onMoveToGrid: () -> Unit,
    focusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    isInteractive: Boolean,
) {
    var isFocused by remember { mutableStateOf(false) }
    val isFocusable = !compact && isInteractive
    val shape = RoundedCornerShape(if (compact) 16.dp else 20.dp)
    val horizontalPadding by animateDpAsState(
        targetValue = if (compact) 10.dp else 14.dp,
        label = "breadcrumb_horizontal_padding"
    )
    val verticalPadding by animateDpAsState(
        targetValue = if (compact) 7.dp else 10.dp,
        label = "breadcrumb_vertical_padding"
    )
    val contentModifier = Modifier.fillMaxWidth()

    Surface(
        onClick = onOpenFeedMenu,
        modifier = modifier
            .then(contentModifier)
            .focusProperties {
                canFocus = isFocusable
                if (isFocusable) {
                    down = downFocusRequester
                }
            }
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (!isFocusable) {
                    return@onPreviewKeyEvent false
                }
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }

                when (event.key) {
                    Key.DirectionLeft -> {
                        onOpenFeedMenu()
                        true
                    }

                    Key.DirectionDown,
                    Key.DirectionRight -> {
                        onMoveToGrid()
                        true
                    }

                    else -> false
                }
            }
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = Color.White.copy(alpha = if (isFocused) 0.9f else 0.22f),
                shape = shape
            )
            .clip(shape)
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (compact) {
                        listOf(
                            Color.Black.copy(alpha = 0.72f),
                            Color.Black.copy(alpha = 0.28f)
                        )
                    } else {
                        listOf(
                            Color.Black.copy(alpha = 0.86f),
                            Color.Black.copy(alpha = 0.6f)
                        )
                    }
                )
            )
            .animateContentSize(),
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        scale = ClickableSurfaceDefaults.scale(
            focusedScale = if (compact) 1.02f else 1.01f
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_keyboard_arrow_left_24),
                contentDescription = null,
                tint = Color.White
            )
            Text(
                text = "Browse",
                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier.padding(start = 4.dp, end = 8.dp)
            )
            BreadcrumbSeparator()
            Text(
                text = currentSource,
                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            BreadcrumbSeparator()
            Text(
                text = currentFeed,
                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun BreadcrumbSeparator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.9f))
    )
}

private sealed interface FeedMenuState {
    data object Opened : FeedMenuState
    data object Closed : FeedMenuState
}

private sealed interface BreadcrumbState {
    data object Expanded : BreadcrumbState
    data object Compact : BreadcrumbState
}
