package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.screens.Screens
import com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail.settingsEntryTestTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TestHomeScreenTag = "dashboard_test_home_screen"
private const val TestLibraryScreenTag = "dashboard_test_library_screen"
private const val TestLibraryCardTag = "dashboard_test_library_card"
private const val TestDownloadsScreenTag = "dashboard_test_downloads_screen"

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class DashboardTopBarNavigationRegressionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var observedRoute: String? = null

    @Test
    fun settingsLanguage_backBackBack_returnsToHome() {
        setDashboardContent()

        navigateToAppLanguageScreen()

        pressHardwareBack()
        waitUntilNodeFocused(settingsEntryTestTag("general_app_language"))

        pressHardwareBack()
        waitUntilNodeExists(settingsEntryTestTag("root_general"))
        requestNodeFocus(settingsEntryTestTag("root_general"))
        waitUntilNodeFocused(settingsEntryTestTag("root_general"))

        pressHardwareBack()
        waitUntilRoute(Screens.Home)
    }

    @Test
    fun settingsLanguage_backBackLeft_staysInSettingsUntilTopBarFocused() {
        setDashboardContent()

        navigateToAppLanguageScreen()

        pressHardwareBack()
        waitUntilNodeFocused(settingsEntryTestTag("general_app_language"))

        pressHardwareBack()
        waitUntilNodeExists(settingsEntryTestTag("root_general"))
        requestNodeFocus(settingsEntryTestTag("root_general"))
        waitUntilNodeFocused(settingsEntryTestTag("root_general"))

        composeRule.onNodeWithTag(settingsEntryTestTag("root_general")).performKeyInput {
            pressKey(androidx.compose.ui.input.key.Key.DirectionLeft)
        }
        waitForTopBarNavigationSettled()
        waitUntilRoute(Screens.Settings)
        waitUntilNodeFocused(settingsEntryTestTag("root_general"))
    }

    @Test
    fun directLibraryPoster_backBack_staysOnHome() {
        setDashboardContent()

        composeRule.onNodeWithTag(dashboardTopBarTabTestTag(Screens.Home))
            .assertIsFocused()
            .performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionRight)
            }
        waitUntilRoute(Screens.Library)
        requestNodeFocus(dashboardTopBarTabTestTag(Screens.Library))

        composeRule.onNodeWithTag(dashboardTopBarTabTestTag(Screens.Library)).performKeyInput {
            pressKey(androidx.compose.ui.input.key.Key.DirectionDown)
        }
        waitUntilNodeFocused(TestLibraryCardTag)

        pressHardwareBack()
        waitUntilNodeFocused(dashboardTopBarTabTestTag(Screens.Library))

        pressHardwareBack()
        waitForTopBarNavigationSettled()
        waitUntilRoute(Screens.Home)
        waitUntilNodeFocused(dashboardTopBarTabTestTag(Screens.Home))
    }

    private fun setDashboardContent() {
        composeRule.setContent {
            MaterialTheme {
                DashboardScaffold(
                    isComingBackFromDifferentScreen = false,
                    resetIsComingBackFromDifferentScreen = {},
                    searchPrefillQuery = null,
                    onSearchPrefillConsumed = {},
                    onBackPressed = {},
                ) { navController, bodyModifier, bodyContext ->
                    DashboardTopBarTestBody(
                        navController = navController,
                        modifier = bodyModifier,
                        topBarSelectedFocusRequester = bodyContext.topBarSelectedFocusRequester,
                        libraryRestoreFocusToken = bodyContext.libraryRestoreFocusToken,
                        updateTopBarVisibility = bodyContext.updateTopBarVisibility,
                        updateTopBarFocusable = bodyContext.updateTopBarFocusable,
                        updateTopBarDownNavigationEnabled = bodyContext.updateTopBarDownNavigationEnabled,
                        requestHomeTopBarFocus = bodyContext.requestHomeTopBarFocus,
                    )
                }
            }
        }

        waitUntilRoute(Screens.Home)
        waitUntilNodeFocused(dashboardTopBarTabTestTag(Screens.Home))
    }

    @Composable
    private fun DashboardTopBarTestBody(
        navController: NavHostController,
        modifier: Modifier,
        topBarSelectedFocusRequester: FocusRequester,
        libraryRestoreFocusToken: Int,
        updateTopBarVisibility: (Boolean) -> Unit,
        updateTopBarFocusable: (Boolean) -> Unit,
        updateTopBarDownNavigationEnabled: (Boolean) -> Unit,
        requestHomeTopBarFocus: () -> Unit,
    ) {
        DisposableEffect(navController) {
            val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, _ ->
                observedRoute = destination.route
            }

            navController.addOnDestinationChangedListener(listener)

            onDispose {
                navController.removeOnDestinationChangedListener(listener)
            }
        }

        NavHost(
            navController = navController,
            startDestination = Screens.Home(),
            modifier = modifier
        ) {
            composable(Screens.Profile()) {
                TestStaticScreen(
                    tag = "dashboard_test_profile_screen",
                    title = "Profile",
                    updateTopBarVisibility = updateTopBarVisibility,
                    updateTopBarFocusable = updateTopBarFocusable,
                    updateTopBarDownNavigationEnabled = updateTopBarDownNavigationEnabled,
                )
            }
            composable(Screens.Home()) {
                TestStaticScreen(
                    tag = TestHomeScreenTag,
                    title = "Home",
                    updateTopBarVisibility = updateTopBarVisibility,
                    updateTopBarFocusable = updateTopBarFocusable,
                    updateTopBarDownNavigationEnabled = updateTopBarDownNavigationEnabled,
                )
            }
            composable(Screens.Library()) {
                TestLibraryScreen(
                    topBarFocusRequester = topBarSelectedFocusRequester,
                    restoreFocusToken = libraryRestoreFocusToken,
                    updateTopBarVisibility = updateTopBarVisibility,
                    updateTopBarFocusable = updateTopBarFocusable,
                    updateTopBarDownNavigationEnabled = updateTopBarDownNavigationEnabled,
                )
            }
            composable(Screens.Downloads()) {
                TestStaticScreen(
                    tag = TestDownloadsScreenTag,
                    title = "Downloads",
                    updateTopBarVisibility = updateTopBarVisibility,
                    updateTopBarFocusable = updateTopBarFocusable,
                    updateTopBarDownNavigationEnabled = updateTopBarDownNavigationEnabled,
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
            composable(Screens.Search()) {
                TestStaticScreen(
                    tag = "dashboard_test_search_screen",
                    title = "Search",
                    updateTopBarVisibility = updateTopBarVisibility,
                    updateTopBarFocusable = updateTopBarFocusable,
                    updateTopBarDownNavigationEnabled = updateTopBarDownNavigationEnabled,
                )
            }
        }
    }

    @Composable
    private fun TestStaticScreen(
        tag: String,
        title: String,
        updateTopBarVisibility: (Boolean) -> Unit,
        updateTopBarFocusable: (Boolean) -> Unit,
        updateTopBarDownNavigationEnabled: (Boolean) -> Unit,
    ) {
        LaunchedEffect(Unit) {
            updateTopBarVisibility(true)
            updateTopBarFocusable(true)
            updateTopBarDownNavigationEnabled(true)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(tag),
            contentAlignment = Alignment.Center
        ) {
            Text(text = title)
        }
    }

    @Composable
    private fun TestLibraryScreen(
        topBarFocusRequester: FocusRequester,
        restoreFocusToken: Int,
        updateTopBarVisibility: (Boolean) -> Unit,
        updateTopBarFocusable: (Boolean) -> Unit,
        updateTopBarDownNavigationEnabled: (Boolean) -> Unit,
    ) {
        val cardFocusRequester = FocusRequester()

        LaunchedEffect(Unit) {
            updateTopBarVisibility(true)
            updateTopBarFocusable(true)
            updateTopBarDownNavigationEnabled(true)
        }

        LaunchedEffect(restoreFocusToken) {
            if (restoreFocusToken > 0) {
                cardFocusRequester.requestFocus()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TestLibraryScreenTag)
                .padding(top = 96.dp, start = 48.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Button(
                onClick = {},
                modifier = Modifier
                    .testTag(TestLibraryCardTag)
                    .focusRequester(cardFocusRequester)
                    .focusProperties {
                        up = topBarFocusRequester
                    }
            ) {
                Text(text = "Library Card")
            }
        }
    }

    private fun navigateToAppLanguageScreen() {
        moveFocusToSettingsTab()

        composeRule.onNodeWithTag(dashboardTopBarTabTestTag(Screens.Settings))
            .assertIsFocused()
            .performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionDown)
            }
        waitUntilNodeFocused(settingsEntryTestTag("root_general"))

        composeRule.onNodeWithTag(settingsEntryTestTag("root_general"))
            .assertIsFocused()
            .performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionRight)
            }
        waitUntilNodeFocused(settingsEntryTestTag("general_app_language"))

        composeRule.onNodeWithTag(settingsEntryTestTag("general_app_language"))
            .assertIsFocused()
            .performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionRight)
            }
        waitUntilNodeExists(settingsEntryTestTag("general_language_en"))
    }

    private fun moveFocusToSettingsTab() {
        composeRule.onNodeWithTag(dashboardTopBarTabTestTag(Screens.Home))
            .assertIsFocused()
            .performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionRight)
            }
        composeRule.onNodeWithTag(dashboardTopBarTabTestTag(Screens.Library))
            .assertIsFocused()
            .performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionRight)
            }
        composeRule.onNodeWithTag(dashboardTopBarTabTestTag(Screens.Downloads))
            .assertIsFocused()
            .performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionRight)
            }

        waitUntilNodeFocused(dashboardTopBarTabTestTag(Screens.Settings))
        waitUntilNodeExists(settingsEntryTestTag("root_general"))
    }

    private fun pressHardwareBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()
    }

    private fun requestNodeFocus(tag: String) {
        composeRule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.waitForIdle()
    }

    private fun waitForTopBarNavigationSettled() {
        composeRule.mainClock.advanceTimeBy(500L)
        composeRule.waitForIdle()
    }

    private fun waitUntilNodeExists(tag: String, timeoutMillis: Long = 2_000L) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule
                .onAllNodes(hasTestTag(tag), useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private fun waitUntilRoute(screen: Screens, timeoutMillis: Long = 2_000L) {
        val expectedRoute = screen()
        composeRule.waitUntil(timeoutMillis) {
            observedRoute == expectedRoute
        }
    }

    private fun waitUntilNodeFocused(tag: String, timeoutMillis: Long = 2_000L) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule
                .onAllNodes(hasTestTag(tag), useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .any { node ->
                    node.config.getOrElse(SemanticsProperties.Focused) { false }
                }
        }
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertIsFocused()
    }
}
