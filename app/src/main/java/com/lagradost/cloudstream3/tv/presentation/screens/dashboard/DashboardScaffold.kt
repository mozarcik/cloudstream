package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.tv.presentation.focus.requestFocusWithRetry
import com.lagradost.cloudstream3.tv.presentation.screens.Screens
import com.lagradost.cloudstream3.tv.presentation.screens.library.LibraryFeedGridSelectionStore
import com.lagradost.cloudstream3.tv.presentation.screens.search.SearchPrefillStore
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * What the screen under the menu lends to the Dashboard: how to move focus into it after a
 * screen change ([handler]) and what Back does inside it before the Dashboard takes over.
 */
class DashboardContentEntry {
    /** Moves focus to the right element (Resume, last focused feed item, ...) and returns true when it did. */
    var handler: (() -> Boolean)? = null

    /**
     * Back inside the screen (Home: rows scroll back to the hero). Returns true when it did
     * something, false when the screen is already at its start and Back belongs to the Dashboard.
     */
    var backHandler: (() -> Boolean)? = null

    /** Focus back to where it was after the menu closed; returns true when the screen did it itself. */
    var restoreFocus: (() -> Boolean)? = null
}

val LocalDashboardContentEntry = compositionLocalOf { DashboardContentEntry() }

/**
 * The Dashboard's Back. Each screen's wrapper ([dashboardComposable]) registers it: that is after the
 * NavHost's own handler (which would pop a section off to Home) and before the screen's panels and dialogs.
 */
val LocalDashboardBack = compositionLocalOf<() -> Unit> { {} }

/** Opens the navigation menu; screens call it for "Left from the first column". */
val LocalDashboardOpenMenu = compositionLocalOf<() -> Unit> { {} }

private const val SearchNavLogTag = "TvSearchNav"
private const val ProgrammaticSearchNavigationGuardMs = 450L
private const val EnterContentRetryMs = 60L
// The old screen keeps focus until its fade-out (MaxAnimationMs) has finished.
private const val EnterContentSettleMs = TvDimens.MaxAnimationMs + 30L
private const val EnterContentMaxAttempts = 50
private const val EnterContentFallbackAttempt = 12

internal fun shouldRestoreDashboardFocusAfterReturn(
    isComingBackFromDifferentScreen: Boolean,
    isProgrammaticSearchNavigationPending: Boolean,
): Boolean {
    return isComingBackFromDifferentScreen && !isProgrammaticSearchNavigationPending
}

/** Parts of the Dashboard a screen can act on. */
data class DashboardBodyContext(
    val homeRestoreFocusToken: Int,
    val homeFeedGridRestoreFocusToken: Int,
    val libraryFeedGridRestoreFocusToken: Int,
    val homeContentRefreshToken: Int,
    val accountRefreshToken: Int,
    /** Settings left to Home: switch there and put focus into Home. */
    val goHomeAndEnterContent: () -> Unit,
)

typealias DashboardBodyContent = @Composable (
    navController: NavHostController,
    modifier: Modifier,
    context: DashboardBodyContext,
) -> Unit

fun NavHostController.navigateToDashboardTab(screen: Screens) {
    val targetRoute = screen()
    if (currentDestination?.route == targetRoute) return

    navigate(targetRoute) {
        popUpTo(Screens.Home()) {
            saveState = true
        }
        launchSingleTop = true
        // Leaving a screen and coming back starts from the top; only Back from details restores focus.
        restoreState = false
    }
}

@Composable
fun DashboardScaffold(
    isComingBackFromDifferentScreen: Boolean,
    resetIsComingBackFromDifferentScreen: () -> Unit,
    searchPrefillQuery: String?,
    onSearchPrefillConsumed: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    body: DashboardBodyContent,
) {
    val contentEntry = remember { DashboardContentEntry() }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val contentFocusRequester = remember { FocusRequester() }

    var isMenuOpen by remember { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    val edgeExitGuard = remember { EdgeExitGuard() }
    val bounce = remember { Animatable(0f) }
    val bouncePx = with(LocalDensity.current) { 6.dp.toPx() }
    var contentHasFocus by remember { mutableStateOf(false) }
    var currentDestination: String? by remember { mutableStateOf(null) }
    var previousDestination: String? by remember { mutableStateOf(null) }
    var isProgrammaticSearchNavigationPending by rememberSaveable { mutableStateOf(false) }
    var homeRestoreFocusToken by rememberSaveable { mutableIntStateOf(0) }
    var homeFeedGridRestoreFocusToken by rememberSaveable { mutableIntStateOf(0) }
    var libraryFeedGridRestoreFocusToken by rememberSaveable { mutableIntStateOf(0) }
    var homeContentRefreshToken by rememberSaveable { mutableIntStateOf(0) }
    var accountRefreshToken by rememberSaveable { mutableIntStateOf(0) }
    var enterContentToken by remember { mutableIntStateOf(0) }
    var isFallbackFocusable by remember { mutableStateOf(false) }
    var fallbackHasFocus by remember { mutableStateOf(false) }
    val fallbackFocusRequester = remember { FocusRequester() }

    val libraryMenu: TvLibraryMenuViewModel = viewModel()
    val libraryProviders by libraryMenu.providers.collectAsState()
    val listGroups = remember(libraryProviders) {
        libraryProviders.map { provider ->
            TvNavListGroup(
                providerName = provider.providerName,
                lists = provider.sections.map { section ->
                    TvNavList(
                        key = libraryMenuListKey(provider.providerName, section.id),
                        title = section.title,
                        count = section.gridItems.size,
                    )
                },
                isLoading = provider.isLoading,
                hasError = provider.hasError,
            )
        }
    }
    var openedListKey by remember { mutableStateOf<String?>(null) }
    val selection by remember {
        derivedStateOf { navSelectionForRoute(currentDestination, openedListKey) }
    }

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            currentDestination = destination.route
        }

        navController.addOnDestinationChangedListener(listener)

        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    DisposableEffect(Unit) {
        val reloadHomeObserver: (Boolean) -> Unit = {
            homeContentRefreshToken += 1
        }
        val reloadAccountObserver: (Boolean) -> Unit = {
            accountRefreshToken += 1
            homeContentRefreshToken += 1
        }

        MainActivity.reloadHomeEvent += reloadHomeObserver
        MainActivity.reloadAccountEvent += reloadAccountObserver

        onDispose {
            MainActivity.reloadHomeEvent -= reloadHomeObserver
            MainActivity.reloadAccountEvent -= reloadAccountObserver
        }
    }

    LaunchedEffect(currentDestination) {
        when {
            // Back from Search / Downloads / Settings / a list: the NavHost popped to Home on its own.
            currentDestination == Screens.Home.name &&
                previousDestination != null &&
                previousDestination != Screens.HomeFeedGrid.name -> enterContentToken += 1

            previousDestination == Screens.HomeFeedGrid.name &&
                currentDestination == Screens.Home.name -> homeRestoreFocusToken += 1
        }
        if (currentDestination != null) {
            previousDestination = currentDestination
        }
    }

    LaunchedEffect(searchPrefillQuery) {
        val query = searchPrefillQuery?.trim().orEmpty()
        if (query.isBlank()) return@LaunchedEffect

        isProgrammaticSearchNavigationPending = true
        Log.d(
            SearchNavLogTag,
            "dashboard received search prefill query=$query currentDestination=${navController.currentDestination?.route}"
        )
        SearchPrefillStore.setPendingQuery(query)
        navController.navigate(Screens.Search()) {
            launchSingleTop = true
        }
        Log.d(SearchNavLogTag, "dashboard navigated to search query=$query")
        onSearchPrefillConsumed()
    }

    LaunchedEffect(currentDestination, isProgrammaticSearchNavigationPending) {
        if (isProgrammaticSearchNavigationPending && currentDestination == Screens.Search.name) {
            delay(ProgrammaticSearchNavigationGuardMs)
            isProgrammaticSearchNavigationPending = false
        }
    }

    LaunchedEffect(
        isComingBackFromDifferentScreen,
        currentDestination,
        isProgrammaticSearchNavigationPending
    ) {
        if (
            !shouldRestoreDashboardFocusAfterReturn(
                isComingBackFromDifferentScreen = isComingBackFromDifferentScreen,
                isProgrammaticSearchNavigationPending = isProgrammaticSearchNavigationPending,
            )
        ) {
            if (isComingBackFromDifferentScreen && currentDestination == Screens.Search.name) {
                resetIsComingBackFromDifferentScreen()
            }
            return@LaunchedEffect
        }

        when (currentDestination) {
            Screens.Home.name -> {
                homeRestoreFocusToken += 1
            }

            Screens.HomeFeedGrid.name -> {
                homeFeedGridRestoreFocusToken += 1
            }

            Screens.LibraryFeedGrid.name -> {
                libraryFeedGridRestoreFocusToken += 1
            }
        }
        resetIsComingBackFromDifferentScreen()
    }

    // After a screen change from the menu the new screen composes (and may load) asynchronously:
    // keep asking it for focus until it has it.
    LaunchedEffect(enterContentToken) {
        if (enterContentToken == 0) return@LaunchedEffect
        isFallbackFocusable = false
        // The old screen is still composed (and answers for itself) until the screen change has settled.
        delay(EnterContentSettleMs)
        repeat(EnterContentMaxAttempts) { attempt ->
            if (contentHasFocus && !fallbackHasFocus) return@LaunchedEffect
            // The user already opened the menu: never pull focus out of it.
            if (isMenuOpen) return@LaunchedEffect
            val entered = contentEntry.handler?.invoke() == true
            if (!entered) runCatching { contentFocusRequester.requestFocus() }
            delay(EnterContentRetryMs)
            // A screen with nothing to focus (empty Downloads) still has to hear Left, or the menu is unreachable.
            if (attempt == EnterContentFallbackAttempt && !contentHasFocus) {
                isFallbackFocusable = true
                delay(EnterContentRetryMs)
                runCatching { fallbackFocusRequester.requestFocus() }
            }
        }
    }

    // Nothing else gives the first screen its focus now that the top bar is gone.
    LaunchedEffect(Unit) { enterContentToken += 1 }

    fun openMenu() {
        isMenuExpanded = false
        isMenuOpen = true
    }

    fun closeMenuAndRestoreFocus() {
        isMenuOpen = false
        isMenuExpanded = false
        coroutineScope.launch {
            contentFocusRequester.requestFocusWithRetry(attempts = 18)
            // A screen whose own position is lost with its items (scrolled grid) puts focus back itself.
            contentEntry.restoreFocus?.invoke()
        }
    }

    fun openLibraryList(providerName: String, listKey: String) {
        val section = libraryMenu.section(listKey) ?: return
        // The list that is already open: just close the menu, the grid keeps its place.
        if (currentDestination == Screens.LibraryFeedGrid.name && openedListKey == listKey) {
            closeMenuAndRestoreFocus()
            return
        }
        LibraryFeedGridSelectionStore.setAccountName(providerName)
        LibraryFeedGridSelectionStore.setSelectedSection(section)
        openedListKey = listKey
        isMenuOpen = false
        isMenuExpanded = false
        navController.navigate(Screens.LibraryFeedGrid()) {
            popUpTo(Screens.Home()) { saveState = false }
            launchSingleTop = true
        }
        enterContentToken += 1
    }

    fun navigateFromMenu(target: TvNavTarget) {
        // OK on the section that is already active: just hand focus back to the remembered card.
        if (selection == TvNavSelection.Screen(target) && currentDestination == target.screen.name) {
            closeMenuAndRestoreFocus()
            return
        }
        isMenuOpen = false
        isMenuExpanded = false
        navController.navigateToDashboardTab(target.screen)
        enterContentToken += 1
    }

    /** Left at the content's edge: opens the menu unless the press was part of a quick / held burst. */
    fun requestMenuFromEdge() {
        if (isMenuOpen) return
        if (!edgeExitGuard.blocked) {
            openMenu()
        } else if (edgeExitGuard.consumeBounce()) {
            coroutineScope.launch {
                bounce.snapTo(0f)
                bounce.animateTo(-bouncePx, tween(50))
                bounce.animateTo(0f, tween(70))
            }
        }
    }

    fun handleBack() {
        val route = currentDestination
        val isOnTopLevelScreen = route == Screens.Home.name ||
            route == Screens.Search.name ||
            route == Screens.Downloads.name ||
            route == Screens.Settings.name
        val contentHandledBack = !isMenuOpen && isOnTopLevelScreen &&
            contentEntry.backHandler?.invoke() == true
        val action = resolveDashboardBackAction(
            isMenuFocused = isMenuOpen,
            isOnHome = route == Screens.Home.name,
            isOnTopLevelScreen = isOnTopLevelScreen,
            hasPreviousBackStackEntry = navController.previousBackStackEntry != null,
            contentHandledBack = contentHandledBack,
        )
        when (action) {
            // The menu stays open with focus on the Home row (the drawer follows the selection).
            DashboardBackAction.MenuGoHome -> navController.navigateToDashboardTab(Screens.Home)
            DashboardBackAction.ExitApp -> onBackPressed()
            DashboardBackAction.PopBackStack -> navController.popBackStack()
            DashboardBackAction.ContentHandled -> Unit
            DashboardBackAction.OpenMenu -> openMenu()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Registered before the screens' own handlers, so panels and dialogs get Back first.
        BackHandler(onBack = ::handleBack)

        CompositionLocalProvider(
            LocalDashboardContentEntry provides contentEntry,
            LocalDashboardOpenMenu provides ::requestMenuFromEdge,
            LocalDashboardBack provides ::handleBack,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = bounce.value }
                    // Every horizontal press is timed before the screens see it (see EdgeExitGuard).
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown &&
                            (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)
                        ) {
                            val native = event.nativeKeyEvent
                            edgeExitGuard.onHorizontalKey(native.eventTime, native.repeatCount)
                        }
                        false
                    }
                    // Left with nowhere to go (first column / first item) opens the menu.
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft && !isMenuOpen) {
                            if (!focusManager.moveFocus(FocusDirection.Left)) requestMenuFromEdge()
                            true
                        } else {
                            false
                        }
                    }
                    .focusRequester(contentFocusRequester)
                    .focusRestorer()
                    // onFocusChanged reports the focus target that follows it, hence the explicit group.
                    // The wrapper holding focus itself is not a screen that took focus (nothing in it can be focused).
                    .onFocusChanged { contentHasFocus = it.hasFocus && !it.isFocused }
                    .focusGroup()
            ) {
                // Invisible focus holder, enabled only when the screen has nothing focusable of its own.
                Box(
                    Modifier
                        .size(1.dp)
                        .focusRequester(fallbackFocusRequester)
                        .focusProperties { canFocus = isFallbackFocusable }
                        .onFocusChanged { fallbackHasFocus = it.isFocused }
                        .focusable()
                )
                body(
                    navController,
                    Modifier,
                    DashboardBodyContext(
                        homeRestoreFocusToken = homeRestoreFocusToken,
                        homeFeedGridRestoreFocusToken = homeFeedGridRestoreFocusToken,
                        libraryFeedGridRestoreFocusToken = libraryFeedGridRestoreFocusToken,
                        homeContentRefreshToken = homeContentRefreshToken,
                        accountRefreshToken = accountRefreshToken,
                        goHomeAndEnterContent = {
                            navController.navigateToDashboardTab(Screens.Home)
                            enterContentToken += 1
                        },
                    )
                )
            }
        }

        // Composed only while the menu is open, so it is registered after the NavHost's own Back handler
        // (which would otherwise pop the screen underneath the open menu) and takes precedence over it.
        if (isMenuOpen) {
            BackHandler(onBack = ::handleBack)
        }

        // The collapsed menu is the rail itself, focusable: the passive rail is only drawn while it is closed.
        // Settings use the full width (panels start at x = 0): the menu is drawn over them only when opened.
        if (!isMenuOpen && currentDestination != Screens.Settings.name) TvNavRail(selection = selection, accountRefreshToken = accountRefreshToken)
        TvNavDrawer(
            open = isMenuOpen,
            expanded = isMenuExpanded,
            onExpand = { isMenuExpanded = true },
            selection = selection,
            listGroups = listGroups,
            accountRefreshToken = accountRefreshToken,
            onOpenProfile = {
                isMenuOpen = false
                isMenuExpanded = false
                navController.navigate(Screens.Profile()) { launchSingleTop = true }
                enterContentToken += 1
            },
            onNavigate = ::navigateFromMenu,
            onOpenList = ::openLibraryList,
            onDismiss = ::closeMenuAndRestoreFocus,
        )
    }
}
