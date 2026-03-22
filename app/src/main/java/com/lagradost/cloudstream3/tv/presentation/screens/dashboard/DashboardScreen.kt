package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerStartTarget
import com.lagradost.cloudstream3.tv.presentation.screens.search.SearchFeedGridScreen
import com.lagradost.cloudstream3.tv.presentation.screens.search.SearchFeedGridSelectionStore
import com.lagradost.cloudstream3.tv.presentation.screens.search.SearchScreen
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.MasterDetailSettingsScreen
import com.lagradost.cloudstream3.tv.presentation.utils.Padding

val ParentPadding = PaddingValues(vertical = 16.dp, horizontal = 58.dp)
internal val DashboardTopBarHorizontalPadding = 48.dp
internal val DashboardTopBarVerticalPadding = 10.dp

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
    openMediaDetailsScreen: (media: MediaItemCompat.Other) -> Unit,
    openTmdbLibraryDetailsScreen: (item: MediaItemCompat) -> Boolean,
    openVideoPlayer: (url: String, apiName: String, playbackTarget: PlayerStartTarget) -> Unit,
    isComingBackFromDifferentScreen: Boolean,
    resetIsComingBackFromDifferentScreen: () -> Unit,
    searchPrefillQuery: String?,
    onSearchPrefillConsumed: () -> Unit,
    onBackPressed: () -> Unit
) {
    val navController = rememberNavController()

    DashboardScaffold(
        isComingBackFromDifferentScreen = isComingBackFromDifferentScreen,
        resetIsComingBackFromDifferentScreen = resetIsComingBackFromDifferentScreen,
        searchPrefillQuery = searchPrefillQuery,
        onSearchPrefillConsumed = onSearchPrefillConsumed,
        onBackPressed = onBackPressed,
        navController = navController,
    ) { controller, bodyModifier, bodyContext ->
        Body(
            openMovieDetailsScreen = openMovieDetailsScreen,
            openTvSeriesDetailsScreen = openTvSeriesDetailsScreen,
            openMediaDetailsScreen = openMediaDetailsScreen,
            openTmdbLibraryDetailsScreen = openTmdbLibraryDetailsScreen,
            openVideoPlayer = openVideoPlayer,
            updateTopBarVisibility = bodyContext.updateTopBarVisibility,
            updateTopBarFocusable = bodyContext.updateTopBarFocusable,
            updateTopBarDownNavigationEnabled = bodyContext.updateTopBarDownNavigationEnabled,
            topBarSelectedFocusRequester = bodyContext.topBarSelectedFocusRequester,
            homeRestoreFocusToken = bodyContext.homeRestoreFocusToken,
            libraryRestoreFocusToken = bodyContext.libraryRestoreFocusToken,
            homeFeedGridRestoreFocusToken = bodyContext.homeFeedGridRestoreFocusToken,
            libraryFeedGridRestoreFocusToken = bodyContext.libraryFeedGridRestoreFocusToken,
            requestHomeTopBarFocus = bodyContext.requestHomeTopBarFocus,
            navController = controller,
            modifier = bodyModifier,
        )
    }
}

@Composable
private fun Body(
    openMovieDetailsScreen: (movie: MediaItemCompat.Movie) -> Unit,
    openTvSeriesDetailsScreen: (series: MediaItemCompat.TvSeries) -> Unit,
    openMediaDetailsScreen: (media: MediaItemCompat.Other) -> Unit,
    openTmdbLibraryDetailsScreen: (item: MediaItemCompat) -> Boolean,
    openVideoPlayer: (url: String, apiName: String, playbackTarget: PlayerStartTarget) -> Unit,
    updateTopBarVisibility: (Boolean) -> Unit,
    updateTopBarFocusable: (Boolean) -> Unit,
    updateTopBarDownNavigationEnabled: (Boolean) -> Unit,
    topBarSelectedFocusRequester: FocusRequester,
    homeRestoreFocusToken: Int,
    libraryRestoreFocusToken: Int,
    homeFeedGridRestoreFocusToken: Int,
    libraryFeedGridRestoreFocusToken: Int,
    requestHomeTopBarFocus: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
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
                            openMediaDetailsScreen(item)
                        }
                    }
                },
                onContinueWatchingPlay = { item ->
                    val playbackTarget = item.continueWatching?.let { continueWatching ->
                        val episodeId = continueWatching.episodeId ?: return@let PlayerStartTarget.Default
                        if (continueWatching.isFromDownload) {
                            PlayerStartTarget.DownloadedEpisode(episodeId)
                        } else {
                            PlayerStartTarget.ResumeEpisode(episodeId)
                        }
                    } ?: PlayerStartTarget.Default
                    openVideoPlayer(item.url, item.apiName, playbackTarget)
                },
                onOpenFeedGrid = { feed ->
                    HomeFeedGridSelectionStore.setSelectedFeed(feed)
                    navController.navigate(Screens.HomeFeedGrid())
                },
                onScroll = updateTopBarVisibility,
                topBarFocusRequester = topBarSelectedFocusRequester,
                restoreFocusToken = homeRestoreFocusToken
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
                            openMediaDetailsScreen(item)
                        }
                    }
                },
                onBack = {
                    navController.popBackStack()
                },
                onScroll = updateTopBarVisibility,
                restoreFocusToken = homeFeedGridRestoreFocusToken
            )
        }
        composable(Screens.Library()) {
            LibraryScreen(
                onMediaClick = { item ->
                    if (openTmdbLibraryDetailsScreen(item)) {
                        return@LibraryScreen
                    }
                    when (item) {
                        is MediaItemCompat.Movie -> {
                            openMovieDetailsScreen(item)
                        }
                        is MediaItemCompat.TvSeries -> {
                            openTvSeriesDetailsScreen(item)
                        }
                        is MediaItemCompat.Other -> {
                            openMediaDetailsScreen(item)
                        }
                    }
                },
                onOpenFeedGrid = { section ->
                    LibraryFeedGridSelectionStore.setSelectedSection(section)
                    navController.navigate(Screens.LibraryFeedGrid())
                },
                onScroll = updateTopBarVisibility,
                topBarFocusRequester = topBarSelectedFocusRequester,
                restoreFocusToken = libraryRestoreFocusToken
            )
        }
        composable(Screens.LibraryFeedGrid()) {
            LibraryFeedGridScreen(
                onMediaClick = { item ->
                    if (openTmdbLibraryDetailsScreen(item)) {
                        return@LibraryFeedGridScreen
                    }
                    when (item) {
                        is MediaItemCompat.Movie -> {
                            openMovieDetailsScreen(item)
                        }
                        is MediaItemCompat.TvSeries -> {
                            openTvSeriesDetailsScreen(item)
                        }
                        is MediaItemCompat.Other -> {
                            openMediaDetailsScreen(item)
                        }
                    }
                },
                onBack = {
                    navController.popBackStack()
                },
                onScroll = updateTopBarVisibility,
                topBarFocusRequester = topBarSelectedFocusRequester,
                restoreFocusToken = libraryFeedGridRestoreFocusToken
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
                                        score = null,
                                        backdropUri = item.backdropUrl,
                                        description = item.description
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
                                        score = null,
                                        backdropUri = item.backdropUrl,
                                        description = item.description
                                    )
                                )
                            }

                            DownloadMediaType.Media -> {
                                openMediaDetailsScreen(
                                    MediaItemCompat.Other(
                                        id = item.id,
                                        url = item.sourceUrl,
                                        apiName = item.apiName,
                                        name = item.title,
                                        posterUri = item.posterUrl.orEmpty(),
                                        type = null,
                                        score = null,
                                        backdropUri = item.backdropUrl,
                                        description = item.description
                                    )
                                )
                            }
                        }
                    }
                },
                onPlayDownloaded = { item ->
                    openVideoPlayer(
                        item.sourceUrl.ifBlank { "download://local" },
                        item.apiName.ifBlank { "download" },
                        PlayerStartTarget.DownloadedEpisode(item.episodeId)
                    )
                },
                onScroll = updateTopBarVisibility,
                topBarFocusRequester = topBarSelectedFocusRequester,
                onTopBarDownNavigationEnabledChanged = updateTopBarDownNavigationEnabled
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
                            openMediaDetailsScreen(item)
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
                            openMediaDetailsScreen(item)
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
            DashboardSettingsRouteScreen(
                updateTopBarVisibility = updateTopBarVisibility,
                updateTopBarFocusable = updateTopBarFocusable,
                updateTopBarDownNavigationEnabled = updateTopBarDownNavigationEnabled,
                onExitSettings = {
                    navController.navigateToDashboardTab(Screens.Home)
                    requestHomeTopBarFocus()
                },
            )
        }
    }
}

@Composable
fun DashboardSettingsRouteScreen(
    updateTopBarVisibility: (Boolean) -> Unit,
    updateTopBarFocusable: (Boolean) -> Unit,
    updateTopBarDownNavigationEnabled: (Boolean) -> Unit,
    onExitSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        MasterDetailSettingsScreen(
            onExitSettings = {
                updateTopBarVisibility(true)
                updateTopBarFocusable(true)
                updateTopBarDownNavigationEnabled(true)
                onExitSettings()
            },
            onTopBarFocusableChanged = updateTopBarFocusable,
            onTopBarDownNavigationEnabledChanged = updateTopBarDownNavigationEnabled,
            modifier = Modifier.fillMaxSize()
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
