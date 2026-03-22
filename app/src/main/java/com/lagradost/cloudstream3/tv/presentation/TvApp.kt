package com.lagradost.cloudstream3.tv.presentation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavBackStackEntry
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepositoryImpl
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.compat.home.SourceRepository
import com.lagradost.cloudstream3.tv.presentation.screens.Screens
import com.lagradost.cloudstream3.tv.presentation.screens.dashboard.DashboardScreen
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsLoadingState
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsRouteSource
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsScreen
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsScreenMode
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsScreenNavigation
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsScreenViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsScreenViewModelFactory
import com.lagradost.cloudstream3.tv.presentation.screens.details.consumeDetailsRouteSource
import com.lagradost.cloudstream3.tv.presentation.screens.details.consumeDetailsTmdbResolveRequest
import com.lagradost.cloudstream3.tv.presentation.screens.details.consumeDetailsLoadingState
import com.lagradost.cloudstream3.tv.presentation.screens.details.createDetailsSavedStateHandle
import com.lagradost.cloudstream3.tv.presentation.screens.details.saveDetailsRouteSource
import com.lagradost.cloudstream3.tv.presentation.screens.details.saveDetailsTmdbResolveRequest
import com.lagradost.cloudstream3.tv.presentation.screens.details.saveDetailsLoadingState
import com.lagradost.cloudstream3.tv.presentation.screens.library.TmdbLibraryNavigation
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerScreenNavigation
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerStartTarget
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerScreen
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerScreenViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.player.createPlayerSavedStateHandle
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private fun MediaItemCompat.toDetailsLoadingState(
    providerName: String? = apiName,
): DetailsLoadingState {
    val backdrop = backdropUri?.takeIf { it.isNotBlank() }

    return when (this) {
        is MediaItemCompat.Movie -> DetailsLoadingState(
            title = name,
            posterUri = posterUri,
            backdropUri = backdrop ?: posterUri.takeIf {
                continueWatching?.hasBackdrop == true && it.isNotBlank()
            },
            description = description,
            year = year,
            typeName = type?.name,
            providerName = providerName,
        )

        is MediaItemCompat.TvSeries -> DetailsLoadingState(
            title = name,
            posterUri = posterUri,
            backdropUri = backdrop ?: posterUri.takeIf {
                continueWatching?.hasBackdrop == true && it.isNotBlank()
            },
            description = description,
            year = year,
            typeName = type?.name,
            providerName = providerName,
        )

        is MediaItemCompat.Other -> DetailsLoadingState(
            title = name,
            posterUri = posterUri,
            backdropUri = backdrop,
            description = description,
            typeName = type?.name,
            providerName = providerName,
        )
    }
}

private fun NavBackStackEntry.restoreResolveDetailsStateFrom(
    previousEntry: NavBackStackEntry?,
): SavedStateHandle {
    val currentStateHandle = savedStateHandle
    if (currentStateHandle.contains(DetailsScreenNavigation.ResolveTmdbIdBundleKey)) {
        return currentStateHandle
    }

    val previousStateHandle = previousEntry?.savedStateHandle ?: return currentStateHandle
    currentStateHandle.saveDetailsLoadingState(previousStateHandle.consumeDetailsLoadingState())
    currentStateHandle.saveDetailsRouteSource(previousStateHandle.consumeDetailsRouteSource())
    currentStateHandle.saveDetailsTmdbResolveRequest(previousStateHandle.consumeDetailsTmdbResolveRequest())
    return currentStateHandle
}

@Composable
fun TvApp(
    onBackPressed: () -> Unit
) {
    val searchNavLogTag = "TvSearchNav"
    val tmdbNavLogTag = "TvTmdbResolve"
    val navController = rememberNavController()
    var isComingBackFromDifferentScreen by remember { mutableStateOf(false) }
    var pendingSearchPrefillQuery by remember { mutableStateOf<String?>(null) }

    fun resolvePreferredSourceApiName(): String? {
        return runCatching { SourceRepository.getCurrentApiOrDefault().name }
            .getOrNull()
            ?: SourceRepository.getAvailableApis().firstOrNull()?.name
    }

    fun openTmdbLibraryDetails(item: MediaItemCompat): Boolean {
        val resolveRequest = TmdbLibraryNavigation.resolveDetailsRequest(
            item = item,
            preferredApiName = resolvePreferredSourceApiName(),
        ) ?: run {
            Log.w(
                tmdbNavLogTag,
                "failed to build tmdb resolve request itemApi=${item.apiName} itemType=${item.type} title=${item.name} itemTmdbId=${item.tmdbId} itemUrl=${item.url}"
            )
            return false
        }

        Log.d(
            tmdbNavLogTag,
            "open tmdb details itemApi=${item.apiName} itemType=${item.type} title=${item.name} itemTmdbId=${item.tmdbId} preferredApi=${resolveRequest.preferredApiName} itemUrl=${item.url}"
        )

        navController.currentBackStackEntry?.savedStateHandle?.apply {
            saveDetailsRouteSource(
                DetailsRouteSource(
                    url = item.url,
                    apiName = item.apiName,
                )
            )
            saveDetailsLoadingState(
                item.toDetailsLoadingState(providerName = resolveRequest.preferredApiName)
            )
            saveDetailsTmdbResolveRequest(resolveRequest)
        }

        val route = when (item) {
            is MediaItemCompat.Movie -> Screens.MovieDetailsResolve()
            is MediaItemCompat.TvSeries -> Screens.TvSeriesDetailsResolve()
            is MediaItemCompat.Other -> Screens.MediaDetailsResolve()
        }
        navController.navigate(route)
        return true
    }

    fun navigateToPlayer(
        source: DetailsRouteSource,
        playbackTarget: PlayerStartTarget,
    ) {
        val encodedUrl = URLEncoder.encode(source.url, StandardCharsets.UTF_8.toString())
        val encodedApiName = URLEncoder.encode(source.apiName, StandardCharsets.UTF_8.toString())
        val encodedPlaybackTarget = URLEncoder.encode(
            PlayerScreenNavigation.toNavigationArg(playbackTarget),
            StandardCharsets.UTF_8.toString()
        )
        navController.navigate(
            Screens.TvPlayer.withArgs(encodedUrl, encodedApiName, encodedPlaybackTarget)
        )
    }

    fun refreshDetailsScreen(
        route: Screens,
        source: DetailsRouteSource,
        movie: Movie,
    ) {
        navController.currentBackStackEntry?.savedStateHandle?.apply {
            saveDetailsLoadingState(
                DetailsLoadingState(
                    title = movie.name,
                    posterUri = movie.posterUri,
                    description = movie.description,
                    providerName = source.apiName,
                )
            )
        }
        val newEncodedUrl = URLEncoder.encode(movie.id, StandardCharsets.UTF_8.toString())
        val encodedApiName = URLEncoder.encode(source.apiName, StandardCharsets.UTF_8.toString())
        navController.navigate(route.withArgs(newEncodedUrl, encodedApiName))
    }

    NavHost(
        navController = navController,
        startDestination = Screens.Dashboard(),
        builder = {
            composable(route = Screens.Dashboard()) {
                DashboardScreen(
                    openCategoryMovieList = { categoryId ->
                        // TODO: Navigate to category movie list
                    },
                    openMovieDetailsScreen = { movie ->
                        val encodedUrl = URLEncoder.encode(movie.url, StandardCharsets.UTF_8.toString())
                        val encodedApiName = URLEncoder.encode(movie.apiName, StandardCharsets.UTF_8.toString())
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            saveDetailsLoadingState(movie.toDetailsLoadingState())
                        }
                        navController.navigate(Screens.MovieDetails.withArgs(encodedUrl, encodedApiName))
                    },
                    openTvSeriesDetailsScreen = { series ->
                        val encodedUrl = URLEncoder.encode(series.url, StandardCharsets.UTF_8.toString())
                        val encodedApiName = URLEncoder.encode(series.apiName, StandardCharsets.UTF_8.toString())
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            saveDetailsLoadingState(series.toDetailsLoadingState())
                        }
                        navController.navigate(Screens.TvSeriesDetails.withArgs(encodedUrl, encodedApiName))
                    },
                    openMediaDetailsScreen = { media ->
                        val encodedUrl = URLEncoder.encode(media.url, StandardCharsets.UTF_8.toString())
                        val encodedApiName = URLEncoder.encode(media.apiName, StandardCharsets.UTF_8.toString())
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            saveDetailsLoadingState(media.toDetailsLoadingState())
                        }
                        navController.navigate(Screens.MediaDetails.withArgs(encodedUrl, encodedApiName))
                    },
                    openTmdbLibraryDetailsScreen = ::openTmdbLibraryDetails,
                    openVideoPlayer = { url, apiName, playbackTarget ->
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        val encodedApiName = URLEncoder.encode(apiName, StandardCharsets.UTF_8.toString())
                        val encodedPlaybackTarget = URLEncoder.encode(
                            PlayerScreenNavigation.toNavigationArg(playbackTarget),
                            StandardCharsets.UTF_8.toString()
                        )
                        navController.navigate(
                            Screens.TvPlayer.withArgs(encodedUrl, encodedApiName, encodedPlaybackTarget)
                        )
                    },
                    onBackPressed = onBackPressed,
                    isComingBackFromDifferentScreen = isComingBackFromDifferentScreen,
                    resetIsComingBackFromDifferentScreen = {
                        isComingBackFromDifferentScreen = false
                    },
                    searchPrefillQuery = pendingSearchPrefillQuery,
                    onSearchPrefillConsumed = {
                        pendingSearchPrefillQuery = null
                    }
                )
            }
            
            // Movie Details Screen
            composable(
                route = Screens.MovieDetails(),
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("apiName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
                val encodedApiName = backStackEntry.arguments?.getString("apiName") ?: ""
                val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                val apiName = URLDecoder.decode(encodedApiName, StandardCharsets.UTF_8.toString())
                val loadingState = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.consumeDetailsLoadingState()
                    ?: DetailsLoadingState()
                val savedStateHandle = createDetailsSavedStateHandle(
                    url = url,
                    apiName = apiName,
                    loadingState = loadingState,
                )

                val viewModel: DetailsScreenViewModel = viewModel(
                    factory = DetailsScreenViewModelFactory(
                        savedStateHandle = savedStateHandle,
                        repository = MovieRepositoryImpl(),
                        mode = DetailsScreenMode.Movie,
                    )
                )

                DetailsScreen(
                    mode = DetailsScreenMode.Movie,
                    goToPlayer = { playbackTarget ->
                        val source = viewModel.currentSource ?: return@DetailsScreen
                        navigateToPlayer(source, playbackTarget)
                    },
                    onBackPressed = {
                        navController.popBackStack()
                        isComingBackFromDifferentScreen = true
                    },
                    onManualSearchRequested = { query ->
                        Log.d(searchNavLogTag, "manual search requested from movie details query=$query")
                        pendingSearchPrefillQuery = query
                        navController.popBackStack(Screens.Dashboard(), false)
                        isComingBackFromDifferentScreen = true
                    },
                    refreshScreenWithNewItem = { movie ->
                        val source = viewModel.currentSource ?: return@DetailsScreen
                        refreshDetailsScreen(
                            route = Screens.MovieDetails,
                            source = source,
                            movie = movie,
                        )
                    },
                    detailsScreenViewModel = viewModel
                )
            }

            composable(
                route = Screens.TvSeriesDetails(),
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("apiName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
                val encodedApiName = backStackEntry.arguments?.getString("apiName") ?: ""
                val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                val apiName = URLDecoder.decode(encodedApiName, StandardCharsets.UTF_8.toString())
                val loadingState = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.consumeDetailsLoadingState()
                    ?: DetailsLoadingState()
                val savedStateHandle = createDetailsSavedStateHandle(
                    url = url,
                    apiName = apiName,
                    loadingState = loadingState,
                )

                val viewModel: DetailsScreenViewModel = viewModel(
                    factory = DetailsScreenViewModelFactory(
                        savedStateHandle = savedStateHandle,
                        repository = MovieRepositoryImpl(),
                        mode = DetailsScreenMode.TvSeries,
                    )
                )

                DetailsScreen(
                    mode = DetailsScreenMode.TvSeries,
                    goToPlayer = { playbackTarget ->
                        val source = viewModel.currentSource ?: return@DetailsScreen
                        navigateToPlayer(source, playbackTarget)
                    },
                    onBackPressed = {
                        navController.popBackStack()
                        isComingBackFromDifferentScreen = true
                    },
                    onManualSearchRequested = { query ->
                        Log.d(searchNavLogTag, "manual search requested from tv details query=$query")
                        pendingSearchPrefillQuery = query
                        navController.popBackStack(Screens.Dashboard(), false)
                        isComingBackFromDifferentScreen = true
                    },
                    refreshScreenWithNewItem = { movie ->
                        val source = viewModel.currentSource ?: return@DetailsScreen
                        refreshDetailsScreen(
                            route = Screens.TvSeriesDetails,
                            source = source,
                            movie = movie,
                        )
                    },
                    detailsScreenViewModel = viewModel
                )
            }

            composable(
                route = Screens.MediaDetails(),
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("apiName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
                val encodedApiName = backStackEntry.arguments?.getString("apiName") ?: ""
                val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                val apiName = URLDecoder.decode(encodedApiName, StandardCharsets.UTF_8.toString())
                val loadingState = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.consumeDetailsLoadingState()
                    ?: DetailsLoadingState()
                val savedStateHandle = createDetailsSavedStateHandle(
                    url = url,
                    apiName = apiName,
                    loadingState = loadingState,
                )

                val viewModel: DetailsScreenViewModel = viewModel(
                    factory = DetailsScreenViewModelFactory(
                        savedStateHandle = savedStateHandle,
                        repository = MovieRepositoryImpl(),
                        mode = DetailsScreenMode.Media,
                    )
                )

                DetailsScreen(
                    mode = DetailsScreenMode.Media,
                    goToPlayer = { playbackTarget ->
                        val source = viewModel.currentSource ?: return@DetailsScreen
                        navigateToPlayer(source, playbackTarget)
                    },
                    onBackPressed = {
                        navController.popBackStack()
                        isComingBackFromDifferentScreen = true
                    },
                    onManualSearchRequested = { query ->
                        Log.d(searchNavLogTag, "manual search requested from media details query=$query")
                        pendingSearchPrefillQuery = query
                        navController.popBackStack(Screens.Dashboard(), false)
                        isComingBackFromDifferentScreen = true
                    },
                    refreshScreenWithNewItem = { movie ->
                        val source = viewModel.currentSource ?: return@DetailsScreen
                        refreshDetailsScreen(
                            route = Screens.MediaDetails,
                            source = source,
                            movie = movie,
                        )
                    },
                    detailsScreenViewModel = viewModel
                )
            }

            composable(route = Screens.MovieDetailsResolve()) { backStackEntry ->
                val savedStateHandle = backStackEntry.restoreResolveDetailsStateFrom(
                    navController.previousBackStackEntry
                )

                val viewModel: DetailsScreenViewModel = viewModel(
                    factory = DetailsScreenViewModelFactory(
                        savedStateHandle = savedStateHandle,
                        repository = MovieRepositoryImpl(),
                        mode = DetailsScreenMode.Movie,
                    )
                )

                DetailsScreen(
                    mode = DetailsScreenMode.Movie,
                    goToPlayer = { playbackTarget ->
                        val source = viewModel.currentSource ?: return@DetailsScreen
                        navigateToPlayer(source, playbackTarget)
                    },
                    onBackPressed = {
                        navController.popBackStack()
                        isComingBackFromDifferentScreen = true
                    },
                    onManualSearchRequested = { query ->
                        Log.d(searchNavLogTag, "manual search requested from movie resolve details query=$query")
                        pendingSearchPrefillQuery = query
                        navController.popBackStack(Screens.Dashboard(), false)
                        isComingBackFromDifferentScreen = true
                    },
                    refreshScreenWithNewItem = { movie ->
                        val source = viewModel.currentSource ?: return@DetailsScreen
                        refreshDetailsScreen(
                            route = Screens.MovieDetails,
                            source = source,
                            movie = movie,
                        )
                    },
                    detailsScreenViewModel = viewModel
                )
            }

            composable(route = Screens.TvSeriesDetailsResolve()) { backStackEntry ->
                val savedStateHandle = backStackEntry.restoreResolveDetailsStateFrom(
                    navController.previousBackStackEntry
                )

                val viewModel: DetailsScreenViewModel = viewModel(
                    factory = DetailsScreenViewModelFactory(
                        savedStateHandle = savedStateHandle,
                        repository = MovieRepositoryImpl(),
                        mode = DetailsScreenMode.TvSeries,
                    )
                )

                DetailsScreen(
                    mode = DetailsScreenMode.TvSeries,
                    goToPlayer = { playbackTarget ->
                        val source = viewModel.currentSource ?: return@DetailsScreen
                        navigateToPlayer(source, playbackTarget)
                    },
                    onBackPressed = {
                        navController.popBackStack()
                        isComingBackFromDifferentScreen = true
                    },
                    onManualSearchRequested = { query ->
                        Log.d(searchNavLogTag, "manual search requested from tv resolve details query=$query")
                        pendingSearchPrefillQuery = query
                        navController.popBackStack(Screens.Dashboard(), false)
                        isComingBackFromDifferentScreen = true
                    },
                    refreshScreenWithNewItem = { movie ->
                        val source = viewModel.currentSource ?: return@DetailsScreen
                        refreshDetailsScreen(
                            route = Screens.TvSeriesDetails,
                            source = source,
                            movie = movie,
                        )
                    },
                    detailsScreenViewModel = viewModel
                )
            }

            composable(route = Screens.MediaDetailsResolve()) { backStackEntry ->
                val savedStateHandle = backStackEntry.restoreResolveDetailsStateFrom(
                    navController.previousBackStackEntry
                )

                val viewModel: DetailsScreenViewModel = viewModel(
                    factory = DetailsScreenViewModelFactory(
                        savedStateHandle = savedStateHandle,
                        repository = MovieRepositoryImpl(),
                        mode = DetailsScreenMode.Media,
                    )
                )

                DetailsScreen(
                    mode = DetailsScreenMode.Media,
                    goToPlayer = { playbackTarget ->
                        val source = viewModel.currentSource ?: return@DetailsScreen
                        navigateToPlayer(source, playbackTarget)
                    },
                    onBackPressed = {
                        navController.popBackStack()
                        isComingBackFromDifferentScreen = true
                    },
                    onManualSearchRequested = { query ->
                        Log.d(searchNavLogTag, "manual search requested from media resolve details query=$query")
                        pendingSearchPrefillQuery = query
                        navController.popBackStack(Screens.Dashboard(), false)
                        isComingBackFromDifferentScreen = true
                    },
                    refreshScreenWithNewItem = { movie ->
                        val source = viewModel.currentSource ?: return@DetailsScreen
                        refreshDetailsScreen(
                            route = Screens.MediaDetails,
                            source = source,
                            movie = movie,
                        )
                    },
                    detailsScreenViewModel = viewModel
                )
            }

            composable(
                route = Screens.TvPlayer(),
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("apiName") { type = NavType.StringType },
                    navArgument("playbackTarget") { type = NavType.StringType },
                )
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
                val encodedApiName = backStackEntry.arguments?.getString("apiName") ?: ""
                val encodedPlaybackTarget = backStackEntry.arguments?.getString("playbackTarget") ?: ""

                val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                val apiName = URLDecoder.decode(encodedApiName, StandardCharsets.UTF_8.toString())
                val playbackTarget = URLDecoder.decode(
                    encodedPlaybackTarget,
                    StandardCharsets.UTF_8.toString()
                )

                val savedStateHandle = createPlayerSavedStateHandle(
                    url = url,
                    apiName = apiName,
                    playbackTarget = playbackTarget,
                )

                val viewModel: TvPlayerScreenViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return TvPlayerScreenViewModel(
                                savedStateHandle = savedStateHandle,
                            ) as T
                        }
                    }
                )

                TvPlayerScreen(
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    tvPlayerScreenViewModel = viewModel,
                )
            }
        }
    )
}
