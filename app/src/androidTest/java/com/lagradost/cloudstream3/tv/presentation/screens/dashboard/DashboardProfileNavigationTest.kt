package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.screens.Screens
import com.lagradost.cloudstream3.tv.presentation.screens.profile.ProfileScreen
import com.lagradost.cloudstream3.utils.DataStoreHelper
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val DashboardProfileTestHomeTag = "dashboard_profile_test_home"
private const val ProfileAccountDefaultTag = "profile_account_0"

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class DashboardProfileNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var observedRoute: String? = null
    private lateinit var originalAccounts: Array<DataStoreHelper.Account>
    private var originalSelectedKeyIndex: Int = 0

    @Before
    fun setUp() {
        originalAccounts = DataStoreHelper.accounts
        originalSelectedKeyIndex = DataStoreHelper.selectedKeyIndex
        DataStoreHelper.accounts = arrayOf(
            DataStoreHelper.Account(
                keyIndex = 0,
                name = "Default",
                defaultImageIndex = 0,
            ),
            DataStoreHelper.Account(
                keyIndex = 1,
                name = "Kid",
                defaultImageIndex = 1,
            ),
        )
        DataStoreHelper.selectedKeyIndex = 0
    }

    @After
    fun tearDown() {
        DataStoreHelper.accounts = originalAccounts
        DataStoreHelper.selectedKeyIndex = originalSelectedKeyIndex
    }

    @Test
    fun avatarOpenProfile_downFocusesFirstAccount() {
        setDashboardContent()

        composeRule.onNodeWithTag(dashboardTopBarTabTestTag(Screens.Home))
            .assertIsFocused()
            .performKeyInput {
                pressKey(androidx.compose.ui.input.key.Key.DirectionLeft)
            }
        waitUntilNodeFocused(DashboardTopBarAvatarTestTag)

        composeRule.onNodeWithTag(DashboardTopBarAvatarTestTag)
            .performSemanticsAction(SemanticsActions.OnClick)
        waitUntilRoute(Screens.Profile)

        composeRule.onNodeWithTag(DashboardTopBarAvatarTestTag).performKeyInput {
            pressKey(androidx.compose.ui.input.key.Key.DirectionDown)
        }
        waitUntilNodeFocused(ProfileAccountDefaultTag)
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
                    DashboardProfileNavigationHarness(
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
    private fun DashboardProfileNavigationHarness(
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
                ProfileTestHomeScreen(
                    updateTopBarVisibility = context.updateTopBarVisibility,
                    updateTopBarFocusable = context.updateTopBarFocusable,
                    updateTopBarDownNavigationEnabled = context.updateTopBarDownNavigationEnabled,
                )
            }
            composable(Screens.Profile()) {
                ProfileScreen(
                    topBarFocusRequester = context.topBarSelectedFocusRequester,
                    isTopBarFocused = context.isTopBarFocused,
                    onTopBarVisibilityChanged = context.updateTopBarVisibility,
                    onTopBarFocusableChanged = context.updateTopBarFocusable,
                    onTopBarDownNavigationEnabledChanged = context.updateTopBarDownNavigationEnabled,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    @Composable
    private fun ProfileTestHomeScreen(
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
                .testTag(DashboardProfileTestHomeTag),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Home")
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
