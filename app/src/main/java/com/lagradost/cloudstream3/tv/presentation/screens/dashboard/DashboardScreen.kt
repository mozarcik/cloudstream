package com.lagradost.cloudstream3.tv.presentation.screens.dashboard
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.screens.Screens
import com.lagradost.cloudstream3.tv.presentation.screens.downloads.DownloadMediaType
import com.lagradost.cloudstream3.tv.presentation.screens.downloads.DownloadsScreen
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeedGridScreen
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeedGridSelectionStore
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeScreenV2
import com.lagradost.cloudstream3.tv.presentation.screens.library.LibraryFeedGridScreen
import com.lagradost.cloudstream3.tv.presentation.screens.library.LibraryFeedGridSelectionStore
import com.lagradost.cloudstream3.tv.presentation.screens.library.LibraryScreen
import com.lagradost.cloudstream3.tv.presentation.screens.search.SearchFeedGridScreen
import com.lagradost.cloudstream3.tv.presentation.screens.search.SearchFeedGridSelectionStore
import com.lagradost.cloudstream3.tv.presentation.screens.search.SearchScreen
import com.lagradost.cloudstream3.tv.presentation.screens.settings.SettingsScreen
import com.lagradost.cloudstream3.tv.presentation.utils.Padding

val ParentPadding = PaddingValues(vertical = 16.dp, horizontal = 58.dp)
private val DashboardTopBarHorizontalPadding = 48.dp
private val DashboardTopBarVerticalPadding = 10.dp

@Composable
fun rememberChildPadding(direction: LayoutDirection = LocalLayoutDirection.current): Padding {
    return remember {
        Padding(
            start = ParentPadding.calculateStartPadding(direction) + 8.dp,
            top = ParentPadding.calculateTopPadding(),
            end = ParentPadding.calculateEndPadding(direction) + 8.dp,
            bottom = ParentPadding.calculateBottomPadding()
        )
    }
}

@Composable
fun DashboardScreen(
    openCategoryMovieList: (categoryId: String) -> Unit,
    openMovieDetailsScreen: (movie: MediaItemCompat.Movie) -> Unit,
    openTvSeriesDetailsScreen: (series: MediaItemCompat.TvSeries) -> Unit,
    openMediaDetailsScreen: (mediaId: String) -> Unit,
    openVideoPlayer: (url: String, apiName: String, episodeData: String?) -> Unit,
    isComingBackFromDifferentScreen: Boolean,
    resetIsComingBackFromDifferentScreen: () -> Unit,
    onBackPressed: () -> Unit
) {
    val density = LocalDensity.current
    val navController = rememberNavController()

    var isTopBarVisible by remember { mutableStateOf(true) }
    var isTopBarFocused by remember { mutableStateOf(false) }
    var isTopBarFocusable by remember { mutableStateOf(true) }

    val homeTabIndex = remember { TopBarTabs.indexOf(Screens.Home).coerceAtLeast(0) }
    val libraryTabIndex = remember { TopBarTabs.indexOf(Screens.Library).coerceAtLeast(0) }
    val searchTabIndex = remember { TopBarTabs.indexOf(Screens.Search).coerceAtLeast(0) }
    var currentDestination: String? by remember { mutableStateOf(null) }
    val currentTopBarSelectedTabIndex by remember(
        currentDestination,
        homeTabIndex,
        libraryTabIndex,
        searchTabIndex
    ) {
        derivedStateOf {
            val destination = currentDestination ?: return@derivedStateOf homeTabIndex
            if (destination == Screens.HomeFeedGrid.name) {
                return@derivedStateOf homeTabIndex
            }
            if (destination == Screens.LibraryFeedGrid.name) {
                return@derivedStateOf libraryTabIndex
            }
            if (destination == Screens.SearchFeedGrid.name) {
                return@derivedStateOf searchTabIndex
            }

            val screen = runCatching { Screens.valueOf(destination) }.getOrNull()
            val tabIndex = screen?.let { TopBarTabs.indexOf(it) } ?: -1
            if (tabIndex >= 0) tabIndex else homeTabIndex
        }
    }

    DisposableEffect(Unit) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            currentDestination = destination.route
        }

        navController.addOnDestinationChangedListener(listener)

        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    BackPressHandledArea(
        // 1. On user's first back press, bring focus to the current selected tab, if TopBar is not
        //    visible, first make it visible, then focus the selected tab
        // 2. On second back press, bring focus back to the first displayed tab
        // 3. On third back press, exit the app
        onBackPressed = {
            val isOnTopBarTab = TopBarTabs.any { tab -> tab() == currentDestination }
            if (!isOnTopBarTab && navController.previousBackStackEntry != null) {
                navController.popBackStack()
            } else if (!isTopBarVisible) {
                isTopBarVisible = true
                TopBarFocusRequesters[currentTopBarSelectedTabIndex + 1].requestFocus()
            } else if (currentTopBarSelectedTabIndex == homeTabIndex) onBackPressed()
            else if (!isTopBarFocused) {
                TopBarFocusRequesters[currentTopBarSelectedTabIndex + 1].requestFocus()
            } else TopBarFocusRequesters[homeTabIndex + 1].requestFocus()
        }
    ) {
        // We do not want to focus the TopBar everytime we come back from another screen e.g.
        // MovieDetails, CategoryMovieList or VideoPlayer screen
        var wasTopBarFocusRequestedBefore by rememberSaveable { mutableStateOf(false) }

        var topBarHeightPx: Int by rememberSaveable { mutableIntStateOf(0) }

        // Used to show/hide DashboardTopBar
        val topBarYOffsetPx by animateIntAsState(
            targetValue = if (isTopBarVisible) 0 else -topBarHeightPx,
            animationSpec = tween(),
            label = "",
            finishedListener = {
                if (it == -topBarHeightPx && isComingBackFromDifferentScreen) {
                    resetIsComingBackFromDifferentScreen()
                }
            }
        )

        // Used to push down/pull up NavHost when DashboardTopBar is shown/hidden
        val navHostTopPaddingDp by animateDpAsState(
            targetValue = if (isTopBarVisible) with(density) { topBarHeightPx.toDp() } else 0.dp,
            animationSpec = tween(),
            label = "",
        )

        LaunchedEffect(Unit) {
            if (!wasTopBarFocusRequestedBefore) {
                TopBarFocusRequesters[currentTopBarSelectedTabIndex + 1].requestFocus()
                wasTopBarFocusRequestedBefore = true
            }
        }

        DashboardTopBar(
            modifier = Modifier
                .offset { IntOffset(x = 0, y = topBarYOffsetPx) }
                .onSizeChanged { topBarHeightPx = it.height }
                .onFocusChanged { isTopBarFocused = it.hasFocus }
                .padding(horizontal = DashboardTopBarHorizontalPadding)
                .padding(
                    top = DashboardTopBarVerticalPadding,
                    bottom = DashboardTopBarVerticalPadding
                ),
            selectedTabIndex = currentTopBarSelectedTabIndex,
            isFocusable = isTopBarFocusable,
        ) { screen ->
            val targetRoute = screen()
            if (currentDestination != targetRoute) {
                navController.navigate(targetRoute) {
                    if (screen == TopBarTabs[0]) popUpTo(TopBarTabs[0].invoke())
                    launchSingleTop = true
                }
            }
        }

        Body(
            openMovieDetailsScreen = openMovieDetailsScreen,
            openTvSeriesDetailsScreen = openTvSeriesDetailsScreen,
            openMediaDetailsScreen = openMediaDetailsScreen,
            openVideoPlayer = openVideoPlayer,
            updateTopBarVisibility = { isTopBarVisible = it },
            updateTopBarFocusable = { isTopBarFocusable = it },
            navController = navController,
            modifier = Modifier.padding(top = navHostTopPaddingDp),
        )
    }
}

@Composable
private fun BackPressHandledArea(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) =
    Box(
        modifier = Modifier
            .onPreviewKeyEvent {
                if (it.key == Key.Back && it.type == KeyEventType.KeyUp) {
                    onBackPressed()
                    true
                } else {
                    false
                }
            }
            .then(modifier),
        content = content
    )

@Composable
private fun Body(
    openMovieDetailsScreen: (movie: MediaItemCompat.Movie) -> Unit,
    openTvSeriesDetailsScreen: (series: MediaItemCompat.TvSeries) -> Unit,
    openMediaDetailsScreen: (mediaId: String) -> Unit,
    openVideoPlayer: (url: String, apiName: String, episodeData: String?) -> Unit,
    updateTopBarVisibility: (Boolean) -> Unit,
    updateTopBarFocusable: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) =
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = Screens.Home(),
    ) {
        composable(Screens.Profile()) {
            PlaceholderScreen("Profile")
        }
        composable(Screens.Sources()) {
             PlaceholderScreen("Sources - Coming Soon")
        }
        composable(Screens.Home()) {
            HomeScreenV2(
                onMediaClick = { item ->
                    when (item) {
                        is MediaItemCompat.Movie -> {
                            openMovieDetailsScreen(item)
                        }
                        is MediaItemCompat.TvSeries -> {
                            openTvSeriesDetailsScreen(item)
                        }
                        is MediaItemCompat.Other -> {
                            openMediaDetailsScreen("${item.url}|${item.apiName}")
                        }
                    }
                },
                onContinueWatchingPlay = { item ->
                    openVideoPlayer(item.url, item.apiName, null)
                },
                onOpenFeedGrid = { feed ->
                    HomeFeedGridSelectionStore.setSelectedFeed(feed)
                    navController.navigate(Screens.HomeFeedGrid())
                },
                onScroll = updateTopBarVisibility
            )
        }
        composable(Screens.HomeFeedGrid()) {
            HomeFeedGridScreen(
                onMediaClick = { item ->
                    when (item) {
                        is MediaItemCompat.Movie -> {
                            openMovieDetailsScreen(item)
                        }

                        is MediaItemCompat.TvSeries -> {
                            openTvSeriesDetailsScreen(item)
                        }

                        is MediaItemCompat.Other -> {
                            openMediaDetailsScreen("${item.url}|${item.apiName}")
                        }
                    }
                },
                onBack = {
                    navController.popBackStack()
                },
                onScroll = updateTopBarVisibility
            )
        }
        composable(Screens.Library()) {
            LibraryScreen(
                onMediaClick = { item ->
                    when (item) {
                        is MediaItemCompat.Movie -> {
                            openMovieDetailsScreen(item)
                        }
                        is MediaItemCompat.TvSeries -> {
                            openTvSeriesDetailsScreen(item)
                        }
                        is MediaItemCompat.Other -> {
                            openMediaDetailsScreen("${item.url}|${item.apiName}")
                        }
                    }
                },
                onOpenFeedGrid = { section ->
                    LibraryFeedGridSelectionStore.setSelectedSection(section)
                    navController.navigate(Screens.LibraryFeedGrid())
                },
                onScroll = updateTopBarVisibility
            )
        }
        composable(Screens.LibraryFeedGrid()) {
            LibraryFeedGridScreen(
                onMediaClick = { item ->
                    when (item) {
                        is MediaItemCompat.Movie -> {
                            openMovieDetailsScreen(item)
                        }
                        is MediaItemCompat.TvSeries -> {
                            openTvSeriesDetailsScreen(item)
                        }
                        is MediaItemCompat.Other -> {
                            openMediaDetailsScreen("${item.url}|${item.apiName}")
                        }
                    }
                },
                onBack = {
                    navController.popBackStack()
                },
                onScroll = updateTopBarVisibility
            )
        }
        composable(Screens.Downloads()) {
            DownloadsScreen(
                onOpenDetails = { item ->
                    if (item.sourceUrl.isNotBlank() && item.apiName.isNotBlank()) {
                        when (item.mediaType) {
                            DownloadMediaType.Movie -> {
                                openMovieDetailsScreen(
                                    MediaItemCompat.Movie(
                                        id = item.id,
                                        url = item.sourceUrl,
                                        apiName = item.apiName,
                                        name = item.title,
                                        posterUri = item.posterUrl.orEmpty(),
                                        type = null,
                                        score = null
                                    )
                                )
                            }

                            DownloadMediaType.Series -> {
                                openTvSeriesDetailsScreen(
                                    MediaItemCompat.TvSeries(
                                        id = item.id,
                                        url = item.sourceUrl,
                                        apiName = item.apiName,
                                        name = item.title,
                                        posterUri = item.posterUrl.orEmpty(),
                                        type = null,
                                        score = null
                                    )
                                )
                            }

                            DownloadMediaType.Media -> {
                                openMediaDetailsScreen("${item.sourceUrl}|${item.apiName}")
                            }
                        }
                    }
                },
                onScroll = updateTopBarVisibility
            )
        }
        composable(Screens.Search()) {
            SearchScreen(
                onMediaClick = { item ->
                    when (item) {
                        is MediaItemCompat.Movie -> {
                            openMovieDetailsScreen(item)
                        }
                        is MediaItemCompat.TvSeries -> {
                            openTvSeriesDetailsScreen(item)
                        }
                        is MediaItemCompat.Other -> {
                            openMediaDetailsScreen("${item.url}|${item.apiName}")
                        }
                    }
                },
                onOpenFeedGrid = { section ->
                    SearchFeedGridSelectionStore.setSelectedSection(section)
                    navController.navigate(Screens.SearchFeedGrid())
                },
                onScroll = updateTopBarVisibility
            )
        }
        composable(Screens.SearchFeedGrid()) {
            SearchFeedGridScreen(
                onMediaClick = { item ->
                    when (item) {
                        is MediaItemCompat.Movie -> {
                            openMovieDetailsScreen(item)
                        }
                        is MediaItemCompat.TvSeries -> {
                            openTvSeriesDetailsScreen(item)
                        }
                        is MediaItemCompat.Other -> {
                            openMediaDetailsScreen("${item.url}|${item.apiName}")
                        }
                    }
                },
                onBack = {
                    navController.popBackStack()
                },
                onScroll = updateTopBarVisibility
            )
        }
        composable(Screens.Settings()) {
            SettingsScreen(
                onExitSettings = {
                    updateTopBarVisibility(true)
                    updateTopBarFocusable(true)
                },
                onTopBarFocusableChanged = { focusable ->
                    updateTopBarFocusable(focusable)
                }
            )
        }
    }

@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$name - Coming Soon",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
