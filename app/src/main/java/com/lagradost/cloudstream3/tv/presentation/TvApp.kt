package com.lagradost.cloudstream3.tv.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepositoryImpl
import com.lagradost.cloudstream3.tv.presentation.screens.Screens
import com.lagradost.cloudstream3.tv.presentation.screens.dashboard.DashboardScreen
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsLoadingState
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsScreen
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsScreenMode
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsScreenViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsScreenViewModelFactory
import com.lagradost.cloudstream3.tv.presentation.screens.details.consumeDetailsLoadingState
import com.lagradost.cloudstream3.tv.presentation.screens.details.createDetailsSavedStateHandle
import com.lagradost.cloudstream3.tv.presentation.screens.details.saveDetailsLoadingState
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerScreenNavigation
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerScreen
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerScreenViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.player.createPlayerSavedStateHandle
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun TvApp(
    onBackPressed: () -> Unit
) {
    val navController = rememberNavController()
    var isComingBackFromDifferentScreen by remember { mutableStateOf(false) }
    var pendingSearchPrefillQuery by remember { mutableStateOf<String?>(null) }

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
                            saveDetailsLoadingState(
                                DetailsLoadingState(
                                    title = movie.name,
                                    posterUri = movie.posterUri,
                                    backdropUri = movie.backdropUri?.takeIf { it.isNotBlank() }
                                        ?: movie.posterUri.takeIf {
                                            movie.continueWatchingHasBackdrop && it.isNotBlank()
                                        },
                                    description = movie.description,
                                    year = movie.year,
                                    typeName = movie.type?.name,
                                    providerName = movie.apiName,
                                )
                            )
                        }
                        navController.navigate(Screens.MovieDetails.withArgs(encodedUrl, encodedApiName))
                    },
                    openTvSeriesDetailsScreen = { series ->
                        val encodedUrl = URLEncoder.encode(series.url, StandardCharsets.UTF_8.toString())
                        val encodedApiName = URLEncoder.encode(series.apiName, StandardCharsets.UTF_8.toString())
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            saveDetailsLoadingState(
                                DetailsLoadingState(
                                    title = series.name,
                                    posterUri = series.posterUri,
                                    backdropUri = series.backdropUri?.takeIf { it.isNotBlank() }
                                        ?: series.posterUri.takeIf {
                                            series.continueWatchingHasBackdrop && it.isNotBlank()
                                        },
                                    description = series.description,
                                    year = series.year,
                                    typeName = series.type?.name,
                                    providerName = series.apiName,
                                )
                            )
                        }
                        navController.navigate(Screens.TvSeriesDetails.withArgs(encodedUrl, encodedApiName))
                    },
                    openMediaDetailsScreen = { media ->
                        val encodedUrl = URLEncoder.encode(media.url, StandardCharsets.UTF_8.toString())
                        val encodedApiName = URLEncoder.encode(media.apiName, StandardCharsets.UTF_8.toString())
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            saveDetailsLoadingState(
                                DetailsLoadingState(
                                    title = media.name,
                                    posterUri = media.posterUri,
                                    backdropUri = media.backdropUri?.takeIf { it.isNotBlank() },
                                    description = media.description,
                                    typeName = media.type?.name,
                                    providerName = media.apiName,
                                )
                            )
                        }
                        navController.navigate(Screens.MediaDetails.withArgs(encodedUrl, encodedApiName))
                    },
                    openVideoPlayer = { url, apiName, episodeData ->
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        val encodedApiName = URLEncoder.encode(apiName, StandardCharsets.UTF_8.toString())
                        val encodedEpisodeData = URLEncoder.encode(episodeData.orEmpty(), StandardCharsets.UTF_8.toString())
                        navController.navigate(
                            Screens.TvPlayer.withArgs(encodedUrl, encodedApiName, encodedEpisodeData)
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
                    goToPlayer = {
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        val encodedApiName = URLEncoder.encode(apiName, StandardCharsets.UTF_8.toString())
                        val encodedEpisodeData = URLEncoder.encode("", StandardCharsets.UTF_8.toString())
                        navController.navigate(
                            Screens.TvPlayer.withArgs(encodedUrl, encodedApiName, encodedEpisodeData)
                        )
                    },
                    onBackPressed = {
                        navController.popBackStack()
                        isComingBackFromDifferentScreen = true
                    },
                    onManualSearchRequested = { query ->
                        pendingSearchPrefillQuery = query
                        navController.popBackStack(Screens.Dashboard(), false)
                        isComingBackFromDifferentScreen = true
                    },
                    refreshScreenWithNewItem = { movie ->
                        val encodedApiName = URLEncoder.encode(apiName, StandardCharsets.UTF_8.toString())
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            saveDetailsLoadingState(
                                DetailsLoadingState(
                                    title = movie.name,
                                    posterUri = movie.posterUri,
                                    description = movie.description,
                                    providerName = apiName,
                                )
                            )
                        }
                        val newEncodedUrl = URLEncoder.encode(movie.id, StandardCharsets.UTF_8.toString())
                        navController.navigate(Screens.MovieDetails.withArgs(newEncodedUrl, encodedApiName))
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
                    goToPlayer = { episodeData ->
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        val encodedApiName = URLEncoder.encode(apiName, StandardCharsets.UTF_8.toString())
                        val encodedEpisodeData = URLEncoder.encode(episodeData.orEmpty(), StandardCharsets.UTF_8.toString())
                        navController.navigate(
                            Screens.TvPlayer.withArgs(encodedUrl, encodedApiName, encodedEpisodeData)
                        )
                    },
                    onBackPressed = {
                        navController.popBackStack()
                        isComingBackFromDifferentScreen = true
                    },
                    onManualSearchRequested = { query ->
                        pendingSearchPrefillQuery = query
                        navController.popBackStack(Screens.Dashboard(), false)
                        isComingBackFromDifferentScreen = true
                    },
                    refreshScreenWithNewItem = { movie ->
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            saveDetailsLoadingState(
                                DetailsLoadingState(
                                    title = movie.name,
                                    posterUri = movie.posterUri,
                                    description = movie.description,
                                    providerName = apiName,
                                )
                            )
                        }
                        val newEncodedUrl = URLEncoder.encode(movie.id, StandardCharsets.UTF_8.toString())
                        navController.navigate(Screens.TvSeriesDetails.withArgs(newEncodedUrl, URLEncoder.encode(apiName, StandardCharsets.UTF_8.toString())))
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
                    goToPlayer = { episodeData ->
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        val encodedApiName = URLEncoder.encode(apiName, StandardCharsets.UTF_8.toString())
                        val encodedEpisodeData = URLEncoder.encode(episodeData.orEmpty(), StandardCharsets.UTF_8.toString())
                        navController.navigate(
                            Screens.TvPlayer.withArgs(encodedUrl, encodedApiName, encodedEpisodeData)
                        )
                    },
                    onBackPressed = {
                        navController.popBackStack()
                        isComingBackFromDifferentScreen = true
                    },
                    onManualSearchRequested = { query ->
                        pendingSearchPrefillQuery = query
                        navController.popBackStack(Screens.Dashboard(), false)
                        isComingBackFromDifferentScreen = true
                    },
                    refreshScreenWithNewItem = { movie ->
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            saveDetailsLoadingState(
                                DetailsLoadingState(
                                    title = movie.name,
                                    posterUri = movie.posterUri,
                                    description = movie.description,
                                    providerName = apiName,
                                )
                            )
                        }
                        val newEncodedUrl = URLEncoder.encode(movie.id, StandardCharsets.UTF_8.toString())
                        navController.navigate(Screens.MediaDetails.withArgs(newEncodedUrl, URLEncoder.encode(apiName, StandardCharsets.UTF_8.toString())))
                    },
                    detailsScreenViewModel = viewModel
                )
            }

            composable(
                route = Screens.TvPlayer(),
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("apiName") { type = NavType.StringType },
                    navArgument("episodeData") { type = NavType.StringType },
                )
            ) { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
                val encodedApiName = backStackEntry.arguments?.getString("apiName") ?: ""
                val encodedEpisodeData = backStackEntry.arguments?.getString("episodeData") ?: ""

                val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                val apiName = URLDecoder.decode(encodedApiName, StandardCharsets.UTF_8.toString())
                val episodeData = URLDecoder.decode(encodedEpisodeData, StandardCharsets.UTF_8.toString())

                val savedStateHandle = createPlayerSavedStateHandle(
                    url = url,
                    apiName = apiName,
                    episodeData = episodeData,
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
