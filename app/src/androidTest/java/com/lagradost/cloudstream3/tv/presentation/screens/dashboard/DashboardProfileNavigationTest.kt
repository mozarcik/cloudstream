package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.lagradost.cloudstream3.tv.presentation.screens.profile.ProfileScreen
import com.lagradost.cloudstream3.utils.DataStoreHelper
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val DashboardProfileTestHomeTag = "dashboard_profile_test_home"
private const val DashboardProfileTestHomeButtonTag = "dashboard_profile_test_home_button"
private const val ProfileAccountDefaultTag = "profile_account_0"

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class DashboardProfileNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val support by lazy { DashboardNavTestSupport(composeRule) }
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
    fun menuProfileRow_opensProfile_andFocusesFirstAccount() {
        setDashboardContent()

        // Left from the first column opens the menu; Up from the current row reaches the pinned profile row.
        support.openMenuFrom(DashboardProfileTestHomeButtonTag, tvNavScreenTestTag(TvNavTarget.Home))
        support.pressKeyOn(tvNavScreenTestTag(TvNavTarget.Home), androidx.compose.ui.input.key.Key.DirectionUp)
        support.waitUntilNodeFocused(TvNavProfileTestTag)

        support.click(TvNavProfileTestTag)
        support.waitUntilRoute(Screens.Profile)
        support.waitUntilNodeGone(TvNavProfileTestTag)

        // The menu closed and the profile screen put focus on the active account.
        support.waitUntilNodeFocused(ProfileAccountDefaultTag)
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

        support.waitUntilRoute(Screens.Home)
        support.waitUntilNodeFocused(DashboardProfileTestHomeButtonTag)
    }

    @Composable
    private fun DashboardProfileNavigationHarness(
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
            dashboardComposable(Screens.Home()) {
                ProfileTestHomeScreen()
            }
            dashboardComposable(Screens.Profile()) {
                ProfileScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }

    @Composable
    private fun ProfileTestHomeScreen() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(DashboardProfileTestHomeTag),
            contentAlignment = Alignment.Center,
        ) {
            Button(onClick = {}, modifier = Modifier.testTag(DashboardProfileTestHomeButtonTag)) {
                Text(text = "Home")
            }
        }
    }
}
