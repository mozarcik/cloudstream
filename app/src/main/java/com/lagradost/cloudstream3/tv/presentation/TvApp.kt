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
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepositoryImpl
import com.lagradost.cloudstream3.tv.presentation.screens.Screens
import com.lagradost.cloudstream3.tv.presentation.screens.dashboard.DashboardScreen
import com.lagradost.cloudstream3.tv.presentation.screens.media.MediaDetailsScreen
import com.lagradost.cloudstream3.tv.presentation.screens.media.MediaDetailsScreenViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsScreen
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsScreenViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerScreen
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerScreenViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.TvSeriesDetailsScreen
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.TvSeriesDetailsScreenViewModel
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
                            set(MovieDetailsScreen.LoadingTitleBundleKey, movie.name)
                            set(MovieDetailsScreen.LoadingPosterBundleKey, movie.posterUri)
                            set(
                                MovieDetailsScreen.LoadingBackdropBundleKey,
                                movie.backdropUri?.takeIf { it.isNotBlank() }
                                    ?: movie.posterUri.takeIf {
                                        movie.continueWatchingHasBackdrop && it.isNotBlank()
                                    }
                            )
                            set(MovieDetailsScreen.LoadingDescriptionBundleKey, movie.description)
                            set(MovieDetailsScreen.LoadingYearBundleKey, movie.year)
                            set(MovieDetailsScreen.LoadingTypeBundleKey, movie.type?.name)
                            set(MovieDetailsScreen.LoadingProviderBundleKey, movie.apiName)
                        }
                        navController.navigate(Screens.MovieDetails.withArgs(encodedUrl, encodedApiName))
                    },
                    openTvSeriesDetailsScreen = { series ->
                        val encodedUrl = URLEncoder.encode(series.url, StandardCharsets.UTF_8.toString())
                        val encodedApiName = URLEncoder.encode(series.apiName, StandardCharsets.UTF_8.toString())
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            set(TvSeriesDetailsScreen.LoadingTitleBundleKey, series.name)
                            set(TvSeriesDetailsScreen.LoadingPosterBundleKey, series.posterUri)
                            set(
                                TvSeriesDetailsScreen.LoadingBackdropBundleKey,
                                series.backdropUri?.takeIf { it.isNotBlank() }
                                    ?: series.posterUri.takeIf {
                                        series.continueWatchingHasBackdrop && it.isNotBlank()
                                    }
                            )
                            set(TvSeriesDetailsScreen.LoadingDescriptionBundleKey, series.description)
                            set(TvSeriesDetailsScreen.LoadingYearBundleKey, series.year)
                            set(TvSeriesDetailsScreen.LoadingTypeBundleKey, series.type?.name)
                            set(TvSeriesDetailsScreen.LoadingProviderBundleKey, series.apiName)
                        }
                        navController.navigate(Screens.TvSeriesDetails.withArgs(encodedUrl, encodedApiName))
                    },
                    openMediaDetailsScreen = { media ->
                        val encodedUrl = URLEncoder.encode(media.url, StandardCharsets.UTF_8.toString())
                        val encodedApiName = URLEncoder.encode(media.apiName, StandardCharsets.UTF_8.toString())
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            set(MediaDetailsScreen.LoadingTitleBundleKey, media.name)
                            set(MediaDetailsScreen.LoadingPosterBundleKey, media.posterUri)
                            set(
                                MediaDetailsScreen.LoadingBackdropBundleKey,
                                media.backdropUri?.takeIf { it.isNotBlank() }
                            )
                            set(MediaDetailsScreen.LoadingDescriptionBundleKey, media.description)
                            set(MediaDetailsScreen.LoadingTypeBundleKey, media.type?.name)
                            set(MediaDetailsScreen.LoadingProviderBundleKey, media.apiName)
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
                val previousSavedStateHandle = navController.previousBackStackEntry?.savedStateHandle
                val loadingTitle = previousSavedStateHandle
                    ?.remove<String>(MovieDetailsScreen.LoadingTitleBundleKey)
                val loadingPoster = previousSavedStateHandle
                    ?.remove<String>(MovieDetailsScreen.LoadingPosterBundleKey)
                val loadingBackdrop = previousSavedStateHandle
                    ?.remove<String>(MovieDetailsScreen.LoadingBackdropBundleKey)
                val loadingDescription = previousSavedStateHandle
                    ?.remove<String>(MovieDetailsScreen.LoadingDescriptionBundleKey)
                val loadingYear = previousSavedStateHandle
                    ?.remove<Int>(MovieDetailsScreen.LoadingYearBundleKey)
                val loadingType = previousSavedStateHandle
                    ?.remove<String>(MovieDetailsScreen.LoadingTypeBundleKey)
                val loadingProvider = previousSavedStateHandle
                    ?.remove<String>(MovieDetailsScreen.LoadingProviderBundleKey)

                val savedStateHandle = SavedStateHandle().apply {
                    set(MovieDetailsScreen.UrlBundleKey, url)
                    set(MovieDetailsScreen.ApiNameBundleKey, apiName)
                    set(MovieDetailsScreen.LoadingTitleBundleKey, loadingTitle)
                    set(MovieDetailsScreen.LoadingPosterBundleKey, loadingPoster)
                    set(MovieDetailsScreen.LoadingBackdropBundleKey, loadingBackdrop)
                    set(MovieDetailsScreen.LoadingDescriptionBundleKey, loadingDescription)
                    set(MovieDetailsScreen.LoadingYearBundleKey, loadingYear)
                    set(MovieDetailsScreen.LoadingTypeBundleKey, loadingType)
                    set(MovieDetailsScreen.LoadingProviderBundleKey, loadingProvider)
                }
                
                val viewModel: MovieDetailsScreenViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return MovieDetailsScreenViewModel(
                                savedStateHandle = savedStateHandle,
                                repository = MovieRepositoryImpl()
                            ) as T
                        }
                    }
                )
                
                MovieDetailsScreen(
                    goToMoviePlayer = {
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
                    refreshScreenWithNewMovie = { movie ->
                        val encodedApiName = URLEncoder.encode(apiName, StandardCharsets.UTF_8.toString())
                        navController.currentBackStackEntry?.savedStateHandle?.apply {
                            set(MovieDetailsScreen.LoadingTitleBundleKey, movie.name)
                            set(MovieDetailsScreen.LoadingPosterBundleKey, movie.posterUri)
                            set(MovieDetailsScreen.LoadingBackdropBundleKey, null as String?)
                            set(MovieDetailsScreen.LoadingDescriptionBundleKey, movie.description)
                            set(MovieDetailsScreen.LoadingYearBundleKey, null as Int?)
                            set(MovieDetailsScreen.LoadingTypeBundleKey, null as String?)
                            set(MovieDetailsScreen.LoadingProviderBundleKey, apiName)
                        }
                        val newEncodedUrl = URLEncoder.encode(movie.id, StandardCharsets.UTF_8.toString())
                        navController.navigate(Screens.MovieDetails.withArgs(newEncodedUrl, encodedApiName))
                    },
                    movieDetailsScreenViewModel = viewModel
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
                val previousSavedStateHandle = navController.previousBackStackEntry?.savedStateHandle
                val loadingTitle = previousSavedStateHandle
                    ?.remove<String>(TvSeriesDetailsScreen.LoadingTitleBundleKey)
                val loadingPoster = previousSavedStateHandle
                    ?.remove<String>(TvSeriesDetailsScreen.LoadingPosterBundleKey)
                val loadingBackdrop = previousSavedStateHandle
                    ?.remove<String>(TvSeriesDetailsScreen.LoadingBackdropBundleKey)
                val loadingDescription = previousSavedStateHandle
                    ?.remove<String>(TvSeriesDetailsScreen.LoadingDescriptionBundleKey)
                val loadingYear = previousSavedStateHandle
                    ?.remove<Int>(TvSeriesDetailsScreen.LoadingYearBundleKey)
                val loadingType = previousSavedStateHandle
                    ?.remove<String>(TvSeriesDetailsScreen.LoadingTypeBundleKey)
                val loadingProvider = previousSavedStateHandle
                    ?.remove<String>(TvSeriesDetailsScreen.LoadingProviderBundleKey)

                val savedStateHandle = SavedStateHandle().apply {
                    set(TvSeriesDetailsScreen.UrlBundleKey, url)
                    set(TvSeriesDetailsScreen.ApiNameBundleKey, apiName)
                    set(TvSeriesDetailsScreen.LoadingTitleBundleKey, loadingTitle)
                    set(TvSeriesDetailsScreen.LoadingPosterBundleKey, loadingPoster)
                    set(TvSeriesDetailsScreen.LoadingBackdropBundleKey, loadingBackdrop)
                    set(TvSeriesDetailsScreen.LoadingDescriptionBundleKey, loadingDescription)
                    set(TvSeriesDetailsScreen.LoadingYearBundleKey, loadingYear)
                    set(TvSeriesDetailsScreen.LoadingTypeBundleKey, loadingType)
                    set(TvSeriesDetailsScreen.LoadingProviderBundleKey, loadingProvider)
                }

                val viewModel: TvSeriesDetailsScreenViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return TvSeriesDetailsScreenViewModel(
                                savedStateHandle = savedStateHandle,
                                repository = MovieRepositoryImpl()
                            ) as T
                        }
                    }
                )

                TvSeriesDetailsScreen(
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
                            set(TvSeriesDetailsScreen.LoadingTitleBundleKey, movie.name)
                            set(TvSeriesDetailsScreen.LoadingPosterBundleKey, movie.posterUri)
                            set(TvSeriesDetailsScreen.LoadingBackdropBundleKey, null as String?)
                            set(TvSeriesDetailsScreen.LoadingDescriptionBundleKey, movie.description)
                            set(TvSeriesDetailsScreen.LoadingYearBundleKey, null as Int?)
                            set(TvSeriesDetailsScreen.LoadingTypeBundleKey, null as String?)
                            set(TvSeriesDetailsScreen.LoadingProviderBundleKey, apiName)
                        }
                        val newEncodedUrl = URLEncoder.encode(movie.id, StandardCharsets.UTF_8.toString())
                        navController.navigate(Screens.TvSeriesDetails.withArgs(newEncodedUrl, URLEncoder.encode(apiName, StandardCharsets.UTF_8.toString())))
                    },
                    tvSeriesDetailsScreenViewModel = viewModel
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
                val previousSavedStateHandle = navController.previousBackStackEntry?.savedStateHandle
                val loadingTitle = previousSavedStateHandle
                    ?.remove<String>(MediaDetailsScreen.LoadingTitleBundleKey)
                val loadingPoster = previousSavedStateHandle
                    ?.remove<String>(MediaDetailsScreen.LoadingPosterBundleKey)
                val loadingBackdrop = previousSavedStateHandle
                    ?.remove<String>(MediaDetailsScreen.LoadingBackdropBundleKey)
                val loadingDescription = previousSavedStateHandle
                    ?.remove<String>(MediaDetailsScreen.LoadingDescriptionBundleKey)
                val loadingYear = previousSavedStateHandle
                    ?.remove<Int>(MediaDetailsScreen.LoadingYearBundleKey)
                val loadingType = previousSavedStateHandle
                    ?.remove<String>(MediaDetailsScreen.LoadingTypeBundleKey)
                val loadingProvider = previousSavedStateHandle
                    ?.remove<String>(MediaDetailsScreen.LoadingProviderBundleKey)

                val savedStateHandle = SavedStateHandle().apply {
                    set(MediaDetailsScreen.UrlBundleKey, url)
                    set(MediaDetailsScreen.ApiNameBundleKey, apiName)
                    set(MediaDetailsScreen.LoadingTitleBundleKey, loadingTitle)
                    set(MediaDetailsScreen.LoadingPosterBundleKey, loadingPoster)
                    set(MediaDetailsScreen.LoadingBackdropBundleKey, loadingBackdrop)
                    set(MediaDetailsScreen.LoadingDescriptionBundleKey, loadingDescription)
                    set(MediaDetailsScreen.LoadingYearBundleKey, loadingYear)
                    set(MediaDetailsScreen.LoadingTypeBundleKey, loadingType)
                    set(MediaDetailsScreen.LoadingProviderBundleKey, loadingProvider)
                }

                val viewModel: MediaDetailsScreenViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return MediaDetailsScreenViewModel(
                                savedStateHandle = savedStateHandle,
                                repository = MovieRepositoryImpl()
                            ) as T
                        }
                    }
                )

                MediaDetailsScreen(
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
                            set(MediaDetailsScreen.LoadingTitleBundleKey, movie.name)
                            set(MediaDetailsScreen.LoadingPosterBundleKey, movie.posterUri)
                            set(MediaDetailsScreen.LoadingBackdropBundleKey, null as String?)
                            set(MediaDetailsScreen.LoadingDescriptionBundleKey, movie.description)
                            set(MediaDetailsScreen.LoadingYearBundleKey, null as Int?)
                            set(MediaDetailsScreen.LoadingTypeBundleKey, null as String?)
                            set(MediaDetailsScreen.LoadingProviderBundleKey, apiName)
                        }
                        val newEncodedUrl = URLEncoder.encode(movie.id, StandardCharsets.UTF_8.toString())
                        navController.navigate(Screens.MediaDetails.withArgs(newEncodedUrl, URLEncoder.encode(apiName, StandardCharsets.UTF_8.toString())))
                    },
                    mediaDetailsScreenViewModel = viewModel
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

                val savedStateHandle = SavedStateHandle().apply {
                    set(TvPlayerScreen.UrlBundleKey, url)
                    set(TvPlayerScreen.ApiNameBundleKey, apiName)
                    set(TvPlayerScreen.EpisodeDataBundleKey, episodeData)
                }

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
