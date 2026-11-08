package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.screens.Screens
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.settingsEntryTestTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TestHomeButtonTag = "dashboard_test_home_button"
private const val TestDownloadsButtonTag = "dashboard_test_downloads_button"

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class DashboardNavMenuNavigationRegressionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val support by lazy { DashboardNavTestSupport(composeRule) }
    private var exitBackPressCount = 0

    private val homeRow = tvNavScreenTestTag(TvNavTarget.Home)
    private val settingsRow = tvNavScreenTestTag(TvNavTarget.Settings)
    private val downloadsRow = tvNavScreenTestTag(TvNavTarget.Downloads)

    @Test
    fun settingsLanguage_backChain_levelsThenMenuThenHomeThenExit() {
        setDashboardContent()
        navigateToAppLanguageScreen()

        support.pressHardwareBack()
        support.waitUntilNodeFocused(settingsEntryTestTag("general_app_language"))

        support.pressHardwareBack()
        support.waitUntilNodeExists(settingsEntryTestTag("root_general"))
        support.requestFocus(settingsEntryTestTag("root_general"))
        support.waitUntilNodeFocused(settingsEntryTestTag("root_general"))

        // Settings root: Back opens the menu on the Settings row (no top bar to stop at any more).
        support.pressHardwareBack()
        support.waitUntilNodeFocused(settingsRow)
        support.waitUntilRoute(Screens.Settings)

        // Menu on another section: Back switches to Home, focus stays in the menu on the Home row.
        support.pressHardwareBack()
        support.waitUntilRoute(Screens.Home)
        support.waitUntilNodeFocused(homeRow)
        composeRule.waitUntil(2_000L) { exitBackPressCount == 0 }

        // Menu on Home: one more Back leaves the app, with no confirmation step.
        support.pressHardwareBack()
        composeRule.waitUntil(2_000L) { exitBackPressCount == 1 }
    }

    @Test
    fun home_backFromContent_opensMenuThenExitsAfterOneMoreBack() {
        setDashboardContent()

        support.pressHardwareBack()
        support.waitUntilNodeFocused(homeRow)
        composeRule.waitUntil(2_000L) { exitBackPressCount == 0 }

        support.pressHardwareBack()
        composeRule.waitUntil(2_000L) { exitBackPressCount == 1 }
    }

    @Test
    fun settingsRoot_slowLeft_opensMenuAndRightReturnsToSameRow() {
        setDashboardContent()
        navigateToSettingsRoot()

        support.pauseBeforeEdgeExit()
        support.pressKeyOn(settingsEntryTestTag("root_general"), Key.DirectionLeft)
        support.waitUntilNodeFocused(settingsRow)
        support.waitUntilRoute(Screens.Settings)

        // Right hands focus back to the remembered row and collapses the menu.
        support.pressKeyOn(settingsRow, Key.DirectionRight)
        support.waitUntilNodeFocused(settingsEntryTestTag("root_general"))
        support.waitUntilNodeGone(settingsRow)
    }

    @Test
    fun quickLeftAfterHorizontalMove_doesNotLeaveContent() {
        setDashboardContent()

        support.pauseBeforeEdgeExit()
        support.pressKeyOn(TestHomeButtonTag, Key.DirectionRight)
        support.pressKeyOn(TestHomeButtonTag, Key.DirectionLeft) // < EDGE_EXIT_DEBOUNCE_MS later
        support.waitUntilNodeFocused(TestHomeButtonTag)
        support.waitUntilNodeGone(homeRow)

        // After the pause the same key does open the menu.
        support.pauseBeforeEdgeExit()
        support.pressKeyOn(TestHomeButtonTag, Key.DirectionLeft)
        support.waitUntilNodeFocused(homeRow)
    }

    @Test
    fun menu_downExpandsAndMovesFocus_okOnActiveSectionReturnsToContent() {
        setDashboardContent()
        support.openMenuFrom(TestHomeButtonTag, homeRow)

        // The first Down expands the menu and moves to the next row in the same press.
        support.pressKeyOn(homeRow, Key.DirectionDown)
        support.waitUntilNodeFocused(tvNavScreenTestTag(TvNavTarget.Search))

        support.pressKeyOn(tvNavScreenTestTag(TvNavTarget.Search), Key.DirectionUp)
        support.waitUntilNodeFocused(homeRow)
        support.click(homeRow)
        support.waitUntilNodeFocused(TestHomeButtonTag)
        support.waitUntilNodeGone(homeRow)
        support.waitUntilRoute(Screens.Home)
    }

    @Test
    fun otherScreen_backOpensMenu_thenHome_thenExit() {
        setDashboardContent()

        support.navigateViaMenu(TestHomeButtonTag, homeRow, TvNavTarget.Downloads)
        support.waitUntilRoute(Screens.Downloads)
        support.waitUntilNodeFocused(TestDownloadsButtonTag)

        support.pressHardwareBack()
        support.waitUntilNodeFocused(downloadsRow)

        support.pressHardwareBack()
        support.waitUntilRoute(Screens.Home)
        support.waitUntilNodeFocused(homeRow)
        composeRule.waitUntil(2_000L) { exitBackPressCount == 0 }

        support.pressHardwareBack()
        composeRule.waitUntil(2_000L) { exitBackPressCount == 1 }
    }

    private fun setDashboardContent() {
        exitBackPressCount = 0
        composeRule.setContent {
            MaterialTheme {
                DashboardScaffold(
                    isComingBackFromDifferentScreen = false,
                    resetIsComingBackFromDifferentScreen = {},
                    searchPrefillQuery = null,
                    onSearchPrefillConsumed = {},
                    onBackPressed = { exitBackPressCount += 1 },
                ) { navController, bodyModifier, _ ->
                    DashboardTestBody(navController = navController, modifier = bodyModifier)
                }
            }
        }

        support.waitUntilRoute(Screens.Home)
        support.waitUntilNodeFocused(TestHomeButtonTag)
    }

    @Composable
    private fun DashboardTestBody(navController: NavHostController, modifier: Modifier) {
        DisposableEffect(navController) {
            val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
                support.observedRoute = destination.route
            }
            navController.addOnDestinationChangedListener(listener)
            onDispose { navController.removeOnDestinationChangedListener(listener) }
        }

        NavHost(
            navController = navController,
            startDestination = Screens.Home(),
            modifier = modifier,
        ) {
            dashboardComposable(Screens.Home()) { TestButtonScreen(TestHomeButtonTag, "Home") }
            dashboardComposable(Screens.Downloads()) { TestButtonScreen(TestDownloadsButtonTag, "Downloads") }
            dashboardComposable(Screens.Search()) { TestButtonScreen("dashboard_test_search_button", "Search") }
            dashboardComposable(Screens.Settings()) {
                DashboardSettingsRouteScreen(
                    onExitSettings = {
                        navController.navigateToDashboardTab(Screens.Home)
                    },
                )
            }
        }
    }

    @Composable
    private fun TestButtonScreen(tag: String, title: String) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = {}, modifier = Modifier.testTag(tag)) { Text(text = title) }
        }
    }

    private fun navigateToSettingsRoot() {
        support.navigateViaMenu(TestHomeButtonTag, tvNavScreenTestTag(TvNavTarget.Home), TvNavTarget.Settings)
        support.waitUntilRoute(Screens.Settings)
        support.waitUntilNodeExists(settingsEntryTestTag("root_general"))
        support.requestFocus(settingsEntryTestTag("root_general"))
        support.waitUntilNodeFocused(settingsEntryTestTag("root_general"))
    }

    private fun navigateToAppLanguageScreen() {
        navigateToSettingsRoot()

        support.pressKeyOn(settingsEntryTestTag("root_general"), Key.DirectionRight)
        support.waitUntilNodeFocused(settingsEntryTestTag("general_app_language"))

        support.pressKeyOn(settingsEntryTestTag("general_app_language"), Key.DirectionRight)
        support.waitUntilNodeExists(settingsEntryTestTag("general_language_en"))
    }
}
