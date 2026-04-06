package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.lagradost.cloudstream3.MainActivity
import com.lagradost.cloudstream3.tv.presentation.focus.rememberFocusRequesters
import com.lagradost.cloudstream3.tv.presentation.screens.Screens
import com.lagradost.cloudstream3.tv.presentation.screens.search.SearchPrefillStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SearchNavLogTag = "TvSearchNav"
private const val ProgrammaticSearchNavigationGuardMs = 450L
private const val ProgrammaticTopBarFocusNavigationGuardMs = 450L

internal fun shouldRestoreDashboardFocusAfterReturn(
    isComingBackFromDifferentScreen: Boolean,
    isProgrammaticSearchNavigationPending: Boolean,
): Boolean {
    return isComingBackFromDifferentScreen && !isProgrammaticSearchNavigationPending
}

internal enum class DashboardBackAction {
    PopBackStack,
    ShowTopBarAndFocusSelectedTab,
    ExitApp,
    FocusSelectedTab,
    FocusHomeTab,
}

internal fun resolveDashboardBackAction(
    isOnTopBarTab: Boolean,
    hasPreviousBackStackEntry: Boolean,
    isTopBarVisible: Boolean,
    currentTopBarSelectedTabIndex: Int,
    homeTabIndex: Int,
    isTopBarFocused: Boolean,
): DashboardBackAction {
    return when {
        currentTopBarSelectedTabIndex < 0 && !isTopBarFocused -> DashboardBackAction.FocusSelectedTab
        !isOnTopBarTab && hasPreviousBackStackEntry -> DashboardBackAction.PopBackStack
        !isTopBarVisible -> DashboardBackAction.ShowTopBarAndFocusSelectedTab
        !isTopBarFocused -> DashboardBackAction.FocusSelectedTab
        currentTopBarSelectedTabIndex == homeTabIndex -> DashboardBackAction.ExitApp
        else -> DashboardBackAction.FocusHomeTab
    }
}

data class DashboardBodyContext(
    val topBarSelectedFocusRequester: FocusRequester,
    val isTopBarFocused: Boolean,
    val homeRestoreFocusToken: Int,
    val libraryRestoreFocusToken: Int,
    val homeFeedGridRestoreFocusToken: Int,
    val libraryFeedGridRestoreFocusToken: Int,
    val homeContentRefreshToken: Int,
    val accountRefreshToken: Int,
    val updateTopBarVisibility: (Boolean) -> Unit,
    val updateTopBarFocusable: (Boolean) -> Unit,
    val updateTopBarDownNavigationEnabled: (Boolean) -> Unit,
    val requestHomeTopBarFocus: () -> Unit,
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
        restoreState = true
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
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val topBarFocusRequesters = rememberFocusRequesters(count = TopBarTabs.size + 1)

    var isTopBarVisible by remember { mutableStateOf(true) }
    var isTopBarFocused by remember { mutableStateOf(false) }
    var focusedTopBarTabIndex by remember { mutableStateOf<Int?>(null) }
    var isTopBarFocusable by remember { mutableStateOf(true) }
    var isTopBarDownNavigationEnabled by remember { mutableStateOf(true) }

    val homeTabIndex = remember { TopBarTabs.indexOf(Screens.Home).coerceAtLeast(0) }
    val libraryTabIndex = remember { TopBarTabs.indexOf(Screens.Library).coerceAtLeast(0) }
    val searchTabIndex = remember { TopBarTabs.indexOf(Screens.Search).coerceAtLeast(0) }
    var currentDestination: String? by remember { mutableStateOf(null) }
    var previousDestination: String? by remember { mutableStateOf(null) }
    var isProgrammaticSearchNavigationPending by rememberSaveable { mutableStateOf(false) }
    var isProgrammaticTopBarFocusNavigationPending by rememberSaveable { mutableStateOf(false) }
    var programmaticTopBarFocusGuardRequestId by rememberSaveable { mutableIntStateOf(0) }
    var homeRestoreFocusToken by rememberSaveable { mutableIntStateOf(0) }
    var libraryRestoreFocusToken by rememberSaveable { mutableIntStateOf(0) }
    var homeFeedGridRestoreFocusToken by rememberSaveable { mutableIntStateOf(0) }
    var libraryFeedGridRestoreFocusToken by rememberSaveable { mutableIntStateOf(0) }
    var homeContentRefreshToken by rememberSaveable { mutableIntStateOf(0) }
    var accountRefreshToken by rememberSaveable { mutableIntStateOf(0) }
    var pendingTopBarFocusTabIndex by rememberSaveable { mutableIntStateOf(-1) }
    var topBarFocusRestorerResetToken by rememberSaveable { mutableIntStateOf(0) }
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
            if (destination == Screens.Profile.name) {
                return@derivedStateOf -1
            }

            val screen = runCatching { Screens.valueOf(destination) }.getOrNull()
            val tabIndex = screen?.let { TopBarTabs.indexOf(it) } ?: -1
            if (tabIndex >= 0) tabIndex else homeTabIndex
        }
    }
    val currentTopBarFocusRequester = topBarFocusRequesters[
        (currentTopBarSelectedTabIndex + 1).coerceIn(0, topBarFocusRequesters.lastIndex)
    ]

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
        isTopBarDownNavigationEnabled = true
        when {
            previousDestination == Screens.Settings.name &&
                currentDestination == Screens.Home.name -> {
                focusedTopBarTabIndex = null
                topBarFocusRestorerResetToken += 1
                isProgrammaticTopBarFocusNavigationPending = true
                programmaticTopBarFocusGuardRequestId += 1
                pendingTopBarFocusTabIndex = homeTabIndex
            }

            previousDestination == Screens.HomeFeedGrid.name &&
                currentDestination == Screens.Home.name -> {
                homeRestoreFocusToken += 1
            }

            previousDestination == Screens.LibraryFeedGrid.name &&
                currentDestination == Screens.Library.name -> {
                libraryRestoreFocusToken += 1
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

    LaunchedEffect(programmaticTopBarFocusGuardRequestId) {
        if (!isProgrammaticTopBarFocusNavigationPending) return@LaunchedEffect

        delay(ProgrammaticTopBarFocusNavigationGuardMs)
        isProgrammaticTopBarFocusNavigationPending = false
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

            Screens.Library.name -> {
                libraryRestoreFocusToken += 1
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

    suspend fun requestTopBarFocusWithRetry(tabIndex: Int): Boolean {
        val requesterIndex = (tabIndex + 1).coerceIn(0, topBarFocusRequesters.lastIndex)
        repeat(120) {
            topBarFocusRequesters[requesterIndex].requestFocus()
            delay(16L)
            if (focusedTopBarTabIndex == tabIndex) {
                return true
            }
        }

        return focusedTopBarTabIndex == tabIndex
    }

    fun startProgrammaticTopBarFocusNavigationGuard() {
        isProgrammaticTopBarFocusNavigationPending = true
        programmaticTopBarFocusGuardRequestId += 1
    }

    fun requestTopBarFocus(tabIndex: Int) {
        focusedTopBarTabIndex = null
        topBarFocusRestorerResetToken += 1
        startProgrammaticTopBarFocusNavigationGuard()
        focusManager.clearFocus(force = true)
        coroutineScope.launch {
            requestTopBarFocusWithRetry(tabIndex)
        }
    }

    LaunchedEffect(
        pendingTopBarFocusTabIndex,
        isTopBarFocusable,
        currentTopBarSelectedTabIndex,
    ) {
        val tabIndex = pendingTopBarFocusTabIndex
        if (tabIndex < 0 || !isTopBarFocusable) return@LaunchedEffect
        if (currentTopBarSelectedTabIndex != tabIndex) return@LaunchedEffect

        if (requestTopBarFocusWithRetry(tabIndex)) {
            pendingTopBarFocusTabIndex = -1
        }
    }

    BackPressHandledArea(
        modifier = modifier.fillMaxSize(),
        onBackPressed = {
            val action = resolveDashboardBackAction(
                isOnTopBarTab = TopBarTabs.any { tab -> tab() == currentDestination },
                hasPreviousBackStackEntry = navController.previousBackStackEntry != null,
                isTopBarVisible = isTopBarVisible,
                currentTopBarSelectedTabIndex = currentTopBarSelectedTabIndex,
                homeTabIndex = homeTabIndex,
                isTopBarFocused = isTopBarFocused,
            )
            when (action) {
                DashboardBackAction.PopBackStack -> navController.popBackStack()
                DashboardBackAction.ShowTopBarAndFocusSelectedTab -> {
                    isTopBarVisible = true
                    startProgrammaticTopBarFocusNavigationGuard()
                    pendingTopBarFocusTabIndex = currentTopBarSelectedTabIndex
                }

                DashboardBackAction.ExitApp -> onBackPressed()
                DashboardBackAction.FocusSelectedTab -> {
                    requestTopBarFocus(currentTopBarSelectedTabIndex)
                }

                DashboardBackAction.FocusHomeTab -> {
                    startProgrammaticTopBarFocusNavigationGuard()
                    navController.navigateToDashboardTab(Screens.Home)
                    pendingTopBarFocusTabIndex = homeTabIndex
                }
            }
        }
    ) {
        var wasTopBarFocusRequestedBefore by rememberSaveable { mutableStateOf(false) }
        var topBarHeightPx: Int by rememberSaveable { mutableIntStateOf(0) }

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

        val navHostTopPaddingDp by animateDpAsState(
            targetValue = if (isTopBarVisible) with(density) { topBarHeightPx.toDp() } else 0.dp,
            animationSpec = tween(),
            label = "",
        )

        LaunchedEffect(Unit) {
            if (!wasTopBarFocusRequestedBefore) {
                requestTopBarFocus(currentTopBarSelectedTabIndex)
                wasTopBarFocusRequestedBefore = true
            }
        }

        DashboardTopBar(
            modifier = Modifier
                .offset { IntOffset(x = 0, y = topBarYOffsetPx) }
                .onSizeChanged { topBarHeightPx = it.height }
                .onFocusChanged {
                    isTopBarFocused = it.hasFocus
                    if (!it.hasFocus) {
                        focusedTopBarTabIndex = null
                    }
                }
                .padding(horizontal = DashboardTopBarHorizontalPadding)
                .padding(
                    top = DashboardTopBarVerticalPadding,
                    bottom = DashboardTopBarVerticalPadding
                ),
            selectedTabIndex = currentTopBarSelectedTabIndex,
            accountRefreshToken = accountRefreshToken,
            focusRequesters = topBarFocusRequesters,
            isFocusable = isTopBarFocusable,
            isDownNavigationEnabled = isTopBarDownNavigationEnabled,
            suppressDelayedNavigation = isProgrammaticSearchNavigationPending ||
                isProgrammaticTopBarFocusNavigationPending,
            focusRestorerResetToken = topBarFocusRestorerResetToken,
            onFocusedTabIndexChanged = { focusedTopBarTabIndex = it },
        ) { screen ->
            navController.navigateToDashboardTab(screen)
        }

        body(
            navController,
            Modifier.padding(top = navHostTopPaddingDp),
            DashboardBodyContext(
                topBarSelectedFocusRequester = currentTopBarFocusRequester,
                isTopBarFocused = isTopBarFocused,
                homeRestoreFocusToken = homeRestoreFocusToken,
                libraryRestoreFocusToken = libraryRestoreFocusToken,
                homeFeedGridRestoreFocusToken = homeFeedGridRestoreFocusToken,
                libraryFeedGridRestoreFocusToken = libraryFeedGridRestoreFocusToken,
                homeContentRefreshToken = homeContentRefreshToken,
                accountRefreshToken = accountRefreshToken,
                updateTopBarVisibility = { isTopBarVisible = it },
                updateTopBarFocusable = { isTopBarFocusable = it },
                updateTopBarDownNavigationEnabled = { isTopBarDownNavigationEnabled = it },
                requestHomeTopBarFocus = {
                    focusedTopBarTabIndex = null
                    topBarFocusRestorerResetToken += 1
                    startProgrammaticTopBarFocusNavigationGuard()
                    focusManager.clearFocus(force = true)
                    pendingTopBarFocusTabIndex = homeTabIndex
                },
            )
        )
    }
}

@Composable
private fun BackPressHandledArea(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier,
    ) {
        content()
        BackHandler(onBack = onBackPressed)
    }
}
