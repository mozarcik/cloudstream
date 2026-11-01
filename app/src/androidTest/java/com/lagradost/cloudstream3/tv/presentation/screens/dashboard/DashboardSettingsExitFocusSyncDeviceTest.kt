package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.screens.Screens
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val FakeSettingsRootTag = "dashboard_fake_settings_root"
private const val TestHomePosterTag = "dashboard_test_home_poster"

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class DashboardSettingsExitFocusSyncDeviceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var observedRoute: String? = null
    private var exitBackPressCount = 0

    @Test
    fun backFromSettings_restoresHomeFocusAndHorizontalNavigation() {
        setDashboardContent()

        moveFocusToSettingsTab()

        composeRule.onNodeWithTag(dashboardTopBarTabTestTag(Screens.Settings))
            .assertIsFocused()
            .performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionDown)
            }
        waitUntilNodeFocused(FakeSettingsRootTag)

        pressHardwareBack()
        waitForTopBarNavigationSettled()
        waitUntilRoute(Screens.Home)
        waitUntilNodeFocused(dashboardTopBarTabTestTag(Screens.Home))

        composeRule.onNodeWithTag(dashboardTopBarTabTestTag(Screens.Home)).performKeyInput {
            pressKey(androidx.compose.ui.input.key.Key.DirectionLeft)
        }
        waitUntilNodeFocused(DashboardTopBarAvatarTestTag)

        composeRule.onNodeWithTag(DashboardTopBarAvatarTestTag).performKeyInput {
            pressKey(androidx.compose.ui.input.key.Key.DirectionRight)
        }
        waitUntilNodeFocused(dashboardTopBarTabTestTag(Screens.Home))
    }

    @Test
    fun homePoster_backFocusesTopBar_andSecondBackInvokesExitCallback() {
        setDashboardContent()

        composeRule.onNodeWithTag(dashboardTopBarTabTestTag(Screens.Home))
            .assertIsFocused()
            .performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionDown)
            }
        waitUntilNodeFocused(TestHomePosterTag)

        pressHardwareBack()
        waitForTopBarNavigationSettled()
        waitUntilNodeFocused(dashboardTopBarTabTestTag(Screens.Home))
        composeRule.waitUntil(timeoutMillis = 2_000L) { exitBackPressCount == 0 }

        pressHardwareBack()
        composeRule.waitUntil(timeoutMillis = 2_000L) { exitBackPressCount == 1 }
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
                ) { navController, bodyModifier, bodyContext ->
                    DashboardSettingsExitFocusSyncHarness(
                        navController = navController,
                        modifier = bodyModifier,
                        context = bodyContext,
                    )
                }
            }
        }

        waitUntilRoute(Screens.Home)
        waitUntilNodeFocused(dashboardTopBarTabTestTag(Screens.Home))
    }

    @Composable
    private fun DashboardSettingsExitFocusSyncHarness(
        navController: NavHostController,
        modifier: Modifier,
        context: DashboardBodyContext,
    ) {
        LaunchedEffect(navController) {
            navController.addOnDestinationChangedListener { _, destination, _ ->
                observedRoute = destination.route
            }
        }

        NavHost(
            navController = navController,
            startDestination = Screens.Home(),
            modifier = modifier,
        ) {
            composable(Screens.Home()) {
                TestHomeScreen(
                    topBarFocusRequester = context.topBarSelectedFocusRequester,
                    restoreFocusToken = context.homeRestoreFocusToken,
                )
            }
            composable(Screens.Library()) {
                TestStaticScreen(title = "Library")
            }
            composable(Screens.Downloads()) {
                TestStaticScreen(title = "Downloads")
            }
            composable(Screens.Search()) {
                TestStaticScreen(title = "Search")
            }
            composable(Screens.Settings()) {
                FakeSettingsScreen(
                    updateTopBarVisibility = context.updateTopBarVisibility,
                    updateTopBarFocusable = context.updateTopBarFocusable,
                    updateTopBarDownNavigationEnabled = context.updateTopBarDownNavigationEnabled,
                    onExitSettings = {
                        navController.navigateToDashboardTab(Screens.Home)
                        context.requestHomeTopBarFocus()
                    },
                )
            }
        }
    }

    @Composable
    private fun TestHomeScreen(
        topBarFocusRequester: FocusRequester,
        restoreFocusToken: Int,
    ) {
        val posterFocusRequester = remember { FocusRequester() }

        LaunchedEffect(restoreFocusToken) {
            if (restoreFocusToken > 0) {
                posterFocusRequester.requestFocus()
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Button(
                onClick = {},
                modifier = Modifier
                    .testTag(TestHomePosterTag)
                    .focusRequester(posterFocusRequester)
                    .focusProperties {
                        up = topBarFocusRequester
                    },
            ) {
                Text(text = "Home Poster")
            }
        }
    }

    @Composable
    private fun TestStaticScreen(
        title: String,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = title)
        }
    }

    @Composable
    private fun FakeSettingsScreen(
        updateTopBarVisibility: (Boolean) -> Unit,
        updateTopBarFocusable: (Boolean) -> Unit,
        updateTopBarDownNavigationEnabled: (Boolean) -> Unit,
        onExitSettings: () -> Unit,
    ) {
        val focusRequester = FocusRequester()

        LaunchedEffect(Unit) {
            updateTopBarVisibility(true)
            updateTopBarFocusable(true)
            updateTopBarDownNavigationEnabled(true)
            focusRequester.requestFocus()
        }

        BackHandler(onBack = onExitSettings)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Button(
                onClick = {},
                modifier = Modifier
                    .testTag(FakeSettingsRootTag)
                    .focusRequester(focusRequester),
            ) {
                Text(text = "Settings Root")
            }
        }
    }

    private fun moveFocusToSettingsTab() {
        composeRule.onNodeWithTag(dashboardTopBarTabTestTag(Screens.Home))
            .assertIsFocused()
            .performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionRight)
            }
        waitUntilRoute(Screens.Library)
        waitUntilNodeFocused(dashboardTopBarTabTestTag(Screens.Library))

        composeRule.onNodeWithTag(dashboardTopBarTabTestTag(Screens.Library)).performKeyInput {
            pressKey(androidx.compose.ui.input.key.Key.DirectionRight)
        }
        waitUntilRoute(Screens.Downloads)
        requestNodeFocus(dashboardTopBarTabTestTag(Screens.Downloads))
        waitUntilNodeFocused(dashboardTopBarTabTestTag(Screens.Downloads))

        composeRule.onNodeWithTag(dashboardTopBarTabTestTag(Screens.Downloads)).performKeyInput {
            pressKey(androidx.compose.ui.input.key.Key.DirectionRight)
        }
        waitUntilRoute(Screens.Settings)
        requestNodeFocus(dashboardTopBarTabTestTag(Screens.Settings))
        waitUntilNodeFocused(dashboardTopBarTabTestTag(Screens.Settings))
    }

    private fun pressHardwareBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        composeRule.waitForIdle()
    }

    private fun waitForTopBarNavigationSettled() {
        composeRule.mainClock.advanceTimeBy(500L)
        composeRule.waitForIdle()
    }

    private fun requestNodeFocus(tag: String) {
        composeRule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)
        composeRule.waitForIdle()
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
    }
}
