package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    private val support by lazy { DashboardNavTestSupport(composeRule) }
    private var exitBackPressCount = 0

    @Test
    fun backFromSettings_restoresHomeFocusAndMenuStillOpensWithLeft() {
        setDashboardContent()

        support.navigateViaMenu(TestHomePosterTag, tvNavScreenTestTag(TvNavTarget.Home), TvNavTarget.Settings)
        support.waitUntilRoute(Screens.Settings)
        support.waitUntilNodeFocused(FakeSettingsRootTag)

        support.pressHardwareBack()
        support.waitUntilRoute(Screens.Home)
        support.waitUntilNodeFocused(TestHomePosterTag)

        // The menu is reachable again after returning from Settings.
        support.openMenuFrom(TestHomePosterTag, tvNavScreenTestTag(TvNavTarget.Home))
        support.pressKeyOn(tvNavScreenTestTag(TvNavTarget.Home), Key.DirectionRight)
        support.waitUntilNodeGone(tvNavScreenTestTag(TvNavTarget.Home))
        support.waitUntilNodeFocused(TestHomePosterTag)
    }

    @Test
    fun homePoster_backOpensMenu_andSecondBackInvokesExitCallback() {
        setDashboardContent()

        support.waitUntilNodeFocused(TestHomePosterTag)

        // Back at the start of Home opens the menu (focus on the current row); nothing exits yet.
        support.pressHardwareBack()
        support.waitUntilNodeFocused(tvNavScreenTestTag(TvNavTarget.Home))
        composeRule.waitUntil(timeoutMillis = 2_000L) { exitBackPressCount == 0 }

        // Back in the menu on Home leaves the app: no extra confirmation.
        support.pressHardwareBack()
        composeRule.waitUntil(timeoutMillis = 2_000L) { exitBackPressCount == 1 }
    }

    @Test
    fun menuOpenedWithLeft_backOnHomeExitsWithoutConfirmation() {
        setDashboardContent()

        support.openMenuFrom(TestHomePosterTag, tvNavScreenTestTag(TvNavTarget.Home))

        support.pressHardwareBack()
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

        support.waitUntilRoute(Screens.Home)
        support.waitUntilNodeFocused(TestHomePosterTag)
    }

    @Composable
    private fun DashboardSettingsExitFocusSyncHarness(
        navController: NavHostController,
        modifier: Modifier,
        context: DashboardBodyContext,
    ) {
        LaunchedEffect(navController) {
            navController.addOnDestinationChangedListener { _, destination, _ ->
                support.observedRoute = destination.route
            }
        }

        NavHost(
            navController = navController,
            startDestination = Screens.Home(),
            modifier = modifier,
        ) {
            dashboardComposable(Screens.Home()) { TestHomeScreen() }
            dashboardComposable(Screens.Library()) { TestStaticScreen(title = "Library") }
            dashboardComposable(Screens.Downloads()) { TestStaticScreen(title = "Downloads") }
            dashboardComposable(Screens.Search()) { TestStaticScreen(title = "Search") }
            dashboardComposable(Screens.Settings()) {
                FakeSettingsScreen(onExitSettings = context.goHomeAndEnterContent)
            }
        }
    }

    @Composable
    private fun TestHomeScreen() {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = {}, modifier = Modifier.testTag(TestHomePosterTag)) {
                Text(text = "Home Poster")
            }
        }
    }

    @Composable
    private fun TestStaticScreen(title: String) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = title)
        }
    }

    @Composable
    private fun FakeSettingsScreen(onExitSettings: () -> Unit) {
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        BackHandler(onBack = onExitSettings)

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
}
