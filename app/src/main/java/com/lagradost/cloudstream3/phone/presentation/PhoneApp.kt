package com.lagradost.cloudstream3.phone.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.lagradost.cloudstream3.tv.presentation.screens.search.SearchPrefillStore
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.phone.presentation.screens.PhoneRoutes
import com.lagradost.cloudstream3.phone.presentation.screens.home.PhoneHomeScreen
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneColors
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.lagradost.cloudstream3.phone.presentation.screens.details.PhoneDetailsScreen
import com.lagradost.cloudstream3.phone.presentation.screens.settings.PhoneSettingsScreen
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepositoryImpl
import com.lagradost.cloudstream3.tv.presentation.screens.Screens
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsLoadingState
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsScreenMode
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsScreenViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsScreenViewModelFactory
import com.lagradost.cloudstream3.tv.presentation.screens.details.consumeDetailsLoadingState
import com.lagradost.cloudstream3.tv.presentation.screens.details.createDetailsSavedStateHandle
import com.lagradost.cloudstream3.tv.presentation.screens.details.saveDetailsLoadingState
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerScreenNavigation
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerStartTarget
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerScreen
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerScreenViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.player.createPlayerSavedStateHandle
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.lagradost.cloudstream3.phone.presentation.screens.grid.PhoneHomeFeedGridScreen
import com.lagradost.cloudstream3.phone.presentation.screens.grid.PhoneSearchGridScreen
import com.lagradost.cloudstream3.phone.presentation.screens.search.PhoneSearchScreen
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeedGridSelectionStore
import androidx.compose.material.icons.filled.Download
import com.lagradost.cloudstream3.phone.presentation.screens.downloads.PhoneDownloadsScreen
import com.lagradost.cloudstream3.tv.presentation.screens.downloads.DownloadItemUiModel
import com.lagradost.cloudstream3.tv.presentation.screens.downloads.DownloadMediaType
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Bookmark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagradost.cloudstream3.phone.presentation.common.CsKicker
import androidx.activity.compose.BackHandler
import com.lagradost.cloudstream3.phone.presentation.screens.grid.PhoneLibraryGridScreen
import com.lagradost.cloudstream3.tv.compat.home.SourceRepository
import com.lagradost.cloudstream3.tv.presentation.restoreResolveDetailsStateFrom
import com.lagradost.cloudstream3.tv.presentation.screens.details.DetailsRouteSource
import com.lagradost.cloudstream3.tv.presentation.screens.details.saveDetailsRouteSource
import com.lagradost.cloudstream3.tv.presentation.screens.details.saveDetailsTmdbResolveRequest
import com.lagradost.cloudstream3.tv.presentation.screens.library.LibraryFeedGridSelectionStore
import com.lagradost.cloudstream3.tv.presentation.screens.library.LibraryViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.library.TmdbLibraryNavigation
import com.lagradost.cloudstream3.phone.presentation.screens.player.PhonePlayerRoute
import com.lagradost.cloudstream3.tv.presentation.screens.profile.ProfileViewModel
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import com.lagradost.cloudstream3.phone.presentation.common.LocalPermanentMenu
import kotlinx.coroutines.launch

private fun enc(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
private fun dec(value: String?) = URLDecoder.decode(value.orEmpty(), StandardCharsets.UTF_8.toString())

private fun MediaItemCompat.detailsRoute(): Screens = when (this) {
    is MediaItemCompat.Movie -> Screens.MovieDetails
    is MediaItemCompat.TvSeries -> Screens.TvSeriesDetails
    is MediaItemCompat.Other -> Screens.MediaDetails
}

private fun MediaItemCompat.toLoadingState() = DetailsLoadingState(
    title = name,
    posterUri = posterUri,
    backdropUri = backdropUri,
    description = description,
    typeName = type?.name,
    providerName = apiName,
)

private fun DownloadItemUiModel.toMediaItem(): MediaItemCompat = when (mediaType) {
    DownloadMediaType.Movie -> MediaItemCompat.Movie(id = id, url = sourceUrl, apiName = apiName, name = title, posterUri = posterUrl.orEmpty(), type = null, score = null, backdropUri = backdropUrl, description = description)
    DownloadMediaType.Series -> MediaItemCompat.TvSeries(id = id, url = sourceUrl, apiName = apiName, name = title, posterUri = posterUrl.orEmpty(), type = null, score = null, backdropUri = backdropUrl, description = description)
    DownloadMediaType.Media -> MediaItemCompat.Other(id = id, url = sourceUrl, apiName = apiName, name = title, posterUri = posterUrl.orEmpty(), type = null, score = null, backdropUri = backdropUrl, description = description)
}

private val detailsArgs = listOf(
    navArgument("url") { type = NavType.StringType },
    navArgument("apiName") { type = NavType.StringType },
)

@Composable
fun PhoneApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentRoute by navController.currentBackStackEntryAsState()
    val route = currentRoute?.destination?.route

    fun go(target: String) {
        scope.launch { drawerState.close() }
        if (route == target) return
        // Home is the graph root: restoring saved state here would bring back the library grid.
        if (target == PhoneRoutes.Home) {
            navController.popBackStack(PhoneRoutes.Home, inclusive = false)
        } else {
            navController.navigate(target) {
                popUpTo(PhoneRoutes.Home) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    fun openDetails(item: MediaItemCompat) {
        navController.currentBackStackEntry?.savedStateHandle?.saveDetailsLoadingState(item.toLoadingState())
        navController.navigate(item.detailsRoute().withArgs(enc(item.url), enc(item.apiName)))
    }

    fun openPlayer(url: String, apiName: String, target: PlayerStartTarget) {
        navController.navigate(
            Screens.TvPlayer.withArgs(enc(url), enc(apiName), enc(PlayerScreenNavigation.toNavigationArg(target)))
        )
    }

    @Composable
    fun DetailsHost(vm: DetailsScreenViewModel, mode: DetailsScreenMode, route: Screens) {
        PhoneDetailsScreen(
            mode = mode,
            viewModel = vm,
            onBack = { navController.popBackStack() },
            onPlay = { target -> vm.currentSource?.let { openPlayer(it.url, it.apiName, target) } },
            onSearch = { query ->
                SearchPrefillStore.setPendingQuery(query)
                navController.navigate(PhoneRoutes.Search) { popUpTo(PhoneRoutes.Home); launchSingleTop = true }
            },
            onSimilarClick = { movie ->
                vm.currentSource?.let { navController.navigate(route.withArgs(enc(movie.id), enc(it.apiName))) }
            },
        )
    }

    @Composable
    fun DetailsRoute(entry: NavBackStackEntry, mode: DetailsScreenMode, route: Screens) {
        val url = dec(entry.arguments?.getString("url"))
        val apiName = dec(entry.arguments?.getString("apiName"))
        val loading = navController.previousBackStackEntry?.savedStateHandle?.consumeDetailsLoadingState()
            ?: DetailsLoadingState()
        val handle = createDetailsSavedStateHandle(url = url, apiName = apiName, loadingState = loading)
        val vm: DetailsScreenViewModel = viewModel(
            factory = DetailsScreenViewModelFactory(handle, MovieRepositoryImpl(), mode)
        )
        DetailsHost(vm, mode, route)
    }

    /** TMDB list items carry no provider URL: the details view model resolves them through a source first. */
    @Composable
    fun ResolveRoute(entry: NavBackStackEntry, mode: DetailsScreenMode, route: Screens) {
        val handle = entry.restoreResolveDetailsStateFrom(navController.previousBackStackEntry)
        val vm: DetailsScreenViewModel = viewModel(
            factory = DetailsScreenViewModelFactory(handle, MovieRepositoryImpl(), mode)
        )
        DetailsHost(vm, mode, route)
    }

    fun openLibraryItem(item: MediaItemCompat) {
        val preferredApi = runCatching { SourceRepository.getCurrentApiOrDefault().name }.getOrNull()
            ?: SourceRepository.getAvailableApis().firstOrNull()?.name
        val request = TmdbLibraryNavigation.resolveDetailsRequest(item, preferredApi)
        if (request == null) {
            openDetails(item)
            return
        }
        navController.currentBackStackEntry?.savedStateHandle?.apply {
            saveDetailsRouteSource(DetailsRouteSource(url = item.url, apiName = item.apiName))
            saveDetailsLoadingState(item.toLoadingState().copy(providerName = request.preferredApiName))
            saveDetailsTmdbResolveRequest(request)
        }
        navController.navigate(
            when (item) {
                is MediaItemCompat.Movie -> Screens.MovieDetailsResolve()
                is MediaItemCompat.TvSeries -> Screens.TvSeriesDetailsResolve()
                is MediaItemCompat.Other -> Screens.MediaDetailsResolve()
            }
        )
    }

    val libraryViewModel: LibraryViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val library by libraryViewModel.uiState.collectAsStateWithLifecycle()

    // Tablets (medium / expanded width) get the permanent menu; phones the modal one.
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val isPlayer = route?.startsWith(Screens.TvPlayer.name) == true
    val navHostContent: @Composable () -> Unit = {
        NavHost(
            navController = navController,
            startDestination = PhoneRoutes.Home,
            // Edge to edge: content may sit under the navigation bar, scrolling lists end with a navigation-bar-high gap.
            modifier = Modifier.fillMaxSize().background(PhoneColors.Ground),
        ) {
            composable(PhoneRoutes.Home) {
                PhoneHomeScreen(
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    onOpenSearch = { navController.navigate(PhoneRoutes.Search) },
                    onMediaClick = ::openDetails,
                    onContinueWatchingPlay = { item ->
                        val cw = item.continueWatching
                        val target = when {
                            cw?.episodeId == null -> PlayerStartTarget.Default
                            cw.isFromDownload -> PlayerStartTarget.DownloadedEpisode(cw.episodeId!!)
                            else -> PlayerStartTarget.ResumeEpisode(cw.episodeId!!)
                        }
                        openPlayer(item.url, item.apiName, target)
                    },
                    onOpenFeedGrid = { feed ->
                        HomeFeedGridSelectionStore.setSelectedFeed(feed)
                        navController.navigate(PhoneRoutes.HomeGrid)
                    },
                )
            }
            composable(PhoneRoutes.Search) {
                PhoneSearchScreen(
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    onMediaClick = ::openDetails,
                    onShowMore = { navController.navigate(PhoneRoutes.SearchGrid) },
                )
            }
            composable(PhoneRoutes.HomeGrid) {
                PhoneHomeFeedGridScreen(onMediaClick = ::openDetails, onBack = { navController.popBackStack() })
            }
            composable(PhoneRoutes.LibraryGrid) {
                PhoneLibraryGridScreen(onMediaClick = ::openLibraryItem, onBack = { navController.popBackStack() }, onOpenMenu = { scope.launch { drawerState.open() } })
            }
            composable(Screens.MovieDetailsResolve()) { ResolveRoute(it, DetailsScreenMode.Movie, Screens.MovieDetails) }
            composable(Screens.TvSeriesDetailsResolve()) { ResolveRoute(it, DetailsScreenMode.TvSeries, Screens.TvSeriesDetails) }
            composable(Screens.MediaDetailsResolve()) { ResolveRoute(it, DetailsScreenMode.Media, Screens.MediaDetails) }
            composable(PhoneRoutes.SearchGrid) {
                PhoneSearchGridScreen(onMediaClick = ::openDetails, onBack = { navController.popBackStack() })
            }
            composable(PhoneRoutes.Downloads) {
                PhoneDownloadsScreen(
                    onOpenMenu = { scope.launch { drawerState.open() } },
                    onOpenDetails = { item ->
                        if (item.sourceUrl.isNotBlank() && item.apiName.isNotBlank()) openDetails(item.toMediaItem())
                    },
                    onPlayDownloaded = { item ->
                        openPlayer(
                            item.sourceUrl.ifBlank { "download://local" },
                            item.apiName.ifBlank { "download" },
                            PlayerStartTarget.DownloadedEpisode(item.episodeId),
                        )
                    },
                )
            }
            composable(PhoneRoutes.Settings) {
                PhoneSettingsScreen(onOpenMenu = { scope.launch { drawerState.open() } })
            }
            composable(Screens.MovieDetails(), arguments = detailsArgs) { DetailsRoute(it, DetailsScreenMode.Movie, Screens.MovieDetails) }
            composable(Screens.TvSeriesDetails(), arguments = detailsArgs) { DetailsRoute(it, DetailsScreenMode.TvSeries, Screens.TvSeriesDetails) }
            composable(Screens.MediaDetails(), arguments = detailsArgs) { DetailsRoute(it, DetailsScreenMode.Media, Screens.MediaDetails) }
            composable(
                Screens.TvPlayer(),
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("apiName") { type = NavType.StringType },
                    navArgument("playbackTarget") { type = NavType.StringType },
                ),
            ) { entry ->
                val handle = createPlayerSavedStateHandle(
                    url = dec(entry.arguments?.getString("url")),
                    apiName = dec(entry.arguments?.getString("apiName")),
                    playbackTarget = dec(entry.arguments?.getString("playbackTarget")),
                )
                val vm: TvPlayerScreenViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            TvPlayerScreenViewModel(savedStateHandle = handle) as T
                    }
                )
                PhonePlayerRoute(onBackPressed = { navController.popBackStack() }, viewModel = vm)
            }
        }
    }

    BackHandler(enabled = !wide && drawerState.isOpen) { scope.launch { drawerState.close() } }

    CompositionLocalProvider(LocalPermanentMenu provides wide) {
        if (wide) {
            Row(Modifier.fillMaxSize().background(PhoneColors.Ground)) {
                if (!isPlayer) {
                    Box(Modifier.width(248.dp).fillMaxHeight().background(PhoneColors.Sheet).navigationBarsPadding()) {
                        PhoneDrawerContent(
            route = route, library = library, profileViewModel = profileViewModel,
            onNavigate = ::go,
            onSwitchProvider = libraryViewModel::switchSource,
            onOpenList = { section ->
                scope.launch { drawerState.close() }
                LibraryFeedGridSelectionStore.setAccountName(library.currentApiName)
                LibraryFeedGridSelectionStore.setSelectedSection(section)
                navController.navigate(PhoneRoutes.LibraryGrid) { popUpTo(PhoneRoutes.Home); launchSingleTop = true }
            },
        )
                    }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) { navHostContent() }
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = route == PhoneRoutes.Home || route == PhoneRoutes.LibraryGrid || route == PhoneRoutes.Search,
                drawerContent = {
                    ModalDrawerSheet(drawerContainerColor = PhoneColors.Sheet) {
                        PhoneDrawerContent(
            route = route, library = library, profileViewModel = profileViewModel,
            onNavigate = ::go,
            onSwitchProvider = libraryViewModel::switchSource,
            onOpenList = { section ->
                scope.launch { drawerState.close() }
                LibraryFeedGridSelectionStore.setAccountName(library.currentApiName)
                LibraryFeedGridSelectionStore.setSelectedSection(section)
                navController.navigate(PhoneRoutes.LibraryGrid) { popUpTo(PhoneRoutes.Home); launchSingleTop = true }
            },
        )
                    }
                },
            ) { navHostContent() }
        }
    }
}
