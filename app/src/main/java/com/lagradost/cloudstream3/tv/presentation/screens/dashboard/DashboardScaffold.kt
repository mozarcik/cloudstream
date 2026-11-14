package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

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
import androidx.compose.runtime.snapshotFlow
import com.lagradost.cloudstream3.tv.presentation.focus.TvScreenFocus
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** What the screen under the menu lends to the Dashboard: how focus moves into it and what Back does inside it. */
class DashboardContentEntry {
    /** Registered by the screen through [TvScreenFocusEffect]; null for screens that have nothing to say. */
    var screen: TvScreenFocus? = null
}

val LocalDashboardContentEntry = compositionLocalOf { DashboardContentEntry() }

/**
 * The Dashboard's Back. Each screen's wrapper ([dashboardComposable]) registers it: that is after the
 * NavHost's own handler (which would pop a section off to Home) and before the screen's panels and dialogs.
 */
val LocalDashboardBack = compositionLocalOf<() -> Unit> { {} }

/** Opens the navigation menu; screens call it for "Left from the first column". */
val LocalDashboardOpenMenu = compositionLocalOf<() -> Unit> { {} }

private const val ProgrammaticSearchNavigationGuardMs = 450L
private const val EnterContentRetryMs = 60L
// The old screen keeps focus until its fade-out (MaxAnimationMs) has finished.
private const val EnterContentSettleMs = TvDimens.MaxAnimationMs + 30L
private const val EnterContentMaxAttempts = 50
private const val EnterContentFallbackAttempt = 12
/** A restoring screen may wait for pages to load; after this long the Dashboard gives up and enters instead. */
private const val RestoreContentTimeoutMs = 8_000L
/** A screen that never registers (Profile) has nothing to restore: do not wait the whole timeout for it. */
private const val RestoreNoScreenAttempts = 20

/** Enter: a fresh entry to the screen (starting element). Restore: back to where focus was. */
private enum class ContentFocusKind { Enter, Restore }

/** One request to put focus into the content; a new [nonce] restarts the Dashboard's focus loop. */
private data class ContentFocusRequest(
    val kind: ContentFocusKind,
    /** Wait for the screen change to finish first (the old screen still holds focus until it has faded out). */
    val settle: Boolean,
    val nonce: Int,
)

internal fun shouldRestoreDashboardFocusAfterReturn(
    isComingBackFromDifferentScreen: Boolean,
    isProgrammaticSearchNavigationPending: Boolean,
): Boolean {
    return isComingBackFromDifferentScreen && !isProgrammaticSearchNavigationPending
}

/** Parts of the Dashboard a screen can act on. */
internal data class DashboardBodyContext(
    val homeContentRefreshToken: Int,
    val accountRefreshToken: Int,
    /** Settings left to Home: switch there and put focus into Home. */
    val goHomeAndEnterContent: () -> Unit,
    /** Shared with the menu: the Library screen lists and pins through it. */
    val libraryMenu: TvLibraryMenuViewModel,
    /** A list opened from the Library screen (not from a pinned menu row). */
    val openLibraryListGrid: (providerName: String, listKey: String) -> Unit,
)

internal typealias DashboardBodyContent = @Composable (
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
internal fun DashboardScaffold(
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
    var homeContentRefreshToken by rememberSaveable { mutableIntStateOf(0) }
    var accountRefreshToken by rememberSaveable { mutableIntStateOf(0) }
    var focusRequest by remember { mutableStateOf<ContentFocusRequest?>(null) }
    // Focus work started outside the request loop (the menu closing); opening the menu cancels it.
    var menuFocusJob by remember { mutableStateOf<Job?>(null) }
    var isFallbackFocusable by remember { mutableStateOf(false) }
    var fallbackHasFocus by remember { mutableStateOf(false) }
    val fallbackFocusRequester = remember { FocusRequester() }

    val libraryMenu: TvLibraryMenuViewModel = viewModel()
    val listGroups by libraryMenu.menuGroups.collectAsState()
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

    fun requestContentFocus(kind: ContentFocusKind, settle: Boolean = true) {
        focusRequest = ContentFocusRequest(kind, settle, (focusRequest?.nonce ?: 0) + 1)
    }

    LaunchedEffect(currentDestination) {
        val from = previousDestination
        val to = currentDestination
        if (from != null && to != null) {
            when {
                // Back from a grid: the screen under it puts focus back on the item the grid was opened from.
                from == Screens.HomeFeedGrid.name && to == Screens.Home.name ||
                    from == Screens.LibraryFeedGrid.name && to == Screens.Library.name ||
                    from == Screens.SearchFeedGrid.name && to == Screens.Search.name ->
                    requestContentFocus(ContentFocusKind.Restore)

                // Back from Search / Downloads / Settings / a list: the NavHost popped to Home on its own.
                to == Screens.Home.name -> requestContentFocus(ContentFocusKind.Enter)
            }
        }
        if (currentDestination != null) {
            previousDestination = currentDestination
        }
    }

    LaunchedEffect(searchPrefillQuery) {
        val query = searchPrefillQuery?.trim().orEmpty()
        if (query.isBlank()) return@LaunchedEffect

        isProgrammaticSearchNavigationPending = true
        SearchPrefillStore.setPendingQuery(query)
        navController.navigate(Screens.Search()) {
            launchSingleTop = true
        }
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

        // Back from a title / the player: the screen puts focus back where it was.
        if (currentDestination == null) return@LaunchedEffect
        requestContentFocus(ContentFocusKind.Restore, settle = false)
        resetIsComingBackFromDifferentScreen()
    }

    // The old screen may still be answering for itself, the new one composes (and may load) asynchronously:
    // keep asking the current one until it has put focus where it belongs.
    LaunchedEffect(focusRequest) {
        val request = focusRequest ?: return@LaunchedEffect
        isFallbackFocusable = false

        suspend fun enterContent() {
            repeat(EnterContentMaxAttempts) { attempt ->
                if (contentHasFocus && !fallbackHasFocus) return
                val entered = contentEntry.screen?.enter() == true
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

        // Nothing else may take focus meanwhile: the screen's own memory is what the user expects.
        suspend fun restoreContent(): Boolean = withTimeoutOrNull(RestoreContentTimeoutMs) {
            var attemptsWithoutScreen = 0
            while (true) {
                val screen = contentEntry.screen
                if (screen?.restore() == true) return@withTimeoutOrNull true
                if (screen == null && ++attemptsWithoutScreen >= RestoreNoScreenAttempts) return@withTimeoutOrNull false
                delay(EnterContentRetryMs)
            }
            @Suppress("UNREACHABLE_CODE")
            false
        } == true

        coroutineScope {
            val work = launch {
                if (request.settle) delay(EnterContentSettleMs)
                when (request.kind) {
                    ContentFocusKind.Enter -> enterContent()
                    ContentFocusKind.Restore -> if (!restoreContent()) enterContent()
                }
            }
            // The user already opened the menu: never pull focus out of it.
            val guard = launch {
                snapshotFlow { isMenuOpen }.first { it }
                work.cancel()
            }
            work.join()
            guard.cancel()
        }
    }

    // Nothing else gives the first screen its focus now that the top bar is gone. A Dashboard that is created
    // again after Back from a title restores instead (the effect above).
    LaunchedEffect(Unit) {
        if (!shouldRestoreDashboardFocusAfterReturn(isComingBackFromDifferentScreen, isProgrammaticSearchNavigationPending)) {
            requestContentFocus(ContentFocusKind.Enter)
        }
    }

    fun openMenu() {
        // The menu opens expanded (labels visible) as soon as it takes focus.
        isMenuExpanded = true
        isMenuOpen = true
        menuFocusJob?.cancel()
    }

    fun closeMenuAndRestoreFocus() {
        isMenuOpen = false
        isMenuExpanded = false
        // The screen puts focus back where it was, right away. Compose's own focus restoring cannot do it
        // for lazy lists: their rows look the same to it, so it would pick the first one.
        menuFocusJob?.cancel()
        menuFocusJob = coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val restored = contentEntry.screen?.restore() == true
            if (!restored) contentFocusRequester.requestFocusWithRetry(attempts = 18)
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
        requestContentFocus(ContentFocusKind.Enter)
    }

    fun openLibraryListGrid(providerName: String, listKey: String) {
        val section = libraryMenu.section(listKey) ?: return
        LibraryFeedGridSelectionStore.setAccountName(providerName)
        LibraryFeedGridSelectionStore.setSelectedSection(section)
        openedListKey = null
        navController.navigate(Screens.LibraryFeedGrid())
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
        requestContentFocus(ContentFocusKind.Enter)
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
            route == Screens.Library.name ||
            route == Screens.Settings.name
        val contentHandledBack = !isMenuOpen && isOnTopLevelScreen &&
            contentEntry.screen?.back() == true
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
                        homeContentRefreshToken = homeContentRefreshToken,
                        accountRefreshToken = accountRefreshToken,
                        goHomeAndEnterContent = {
                            navController.navigateToDashboardTab(Screens.Home)
                            requestContentFocus(ContentFocusKind.Enter)
                        },
                        libraryMenu = libraryMenu,
                        openLibraryListGrid = ::openLibraryListGrid,
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
        if (!isMenuOpen) TvNavRail(
            selection = selection,
            accountRefreshToken = accountRefreshToken,
            hasPinnedLists = listGroups.isNotEmpty(),
        )
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
                requestContentFocus(ContentFocusKind.Enter)
            },
            onNavigate = ::navigateFromMenu,
            onOpenList = ::openLibraryList,
            onDismiss = ::closeMenuAndRestoreFocus,
        )
    }
}
