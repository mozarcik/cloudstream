package com.lagradost.cloudstream3.tv.presentation.screens.settings.account

import androidx.activity.ComponentActivity
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.syncproviders.AuthAPI
import com.lagradost.cloudstream3.syncproviders.AuthData
import com.lagradost.cloudstream3.syncproviders.AuthLoginRequirement
import com.lagradost.cloudstream3.syncproviders.AuthRepo
import com.lagradost.cloudstream3.syncproviders.AuthToken
import com.lagradost.cloudstream3.syncproviders.AuthUser
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val ProviderAccountOneTag = "provider_account_101"
private const val ProviderAccountTwoTag = "provider_account_202"

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class ProviderAccountScreenNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun dpadMovesAcrossAccountsAndActions() {
        val prefix = "test_provider_nav_${System.nanoTime()}"
        seedAccounts(prefix)
        val viewModel = ProviderAccountViewModel(testAuthRepo(prefix))

        composeRule.setContent {
            MaterialTheme {
                ProviderAccountScreen(
                    stateFlow = viewModel.uiState,
                    viewModel = viewModel,
                    providerName = "Test Provider",
                    createAccountUrl = "https://example.com/signup",
                    isPreview = false,
                    onBack = {},
                    onAccountChanged = {},
                    onOpenCreateAccount = {},
                )
            }
        }

        requestNodeFocus(ProviderAccountNoneTag)

        composeRule.onNodeWithTag(ProviderAccountNoneTag).performKeyInput {
            pressKey(androidx.compose.ui.input.key.Key.DirectionDown)
        }
        waitUntilNodeFocused(ProviderAccountOneTag)

        composeRule.onNodeWithTag(ProviderAccountOneTag).performKeyInput {
            pressKey(androidx.compose.ui.input.key.Key.DirectionDown)
        }
        waitUntilNodeFocused(ProviderAccountTwoTag)

        composeRule.onNodeWithTag(ProviderAccountTwoTag).performKeyInput {
            pressKey(androidx.compose.ui.input.key.Key.DirectionDown)
        }
        waitUntilNodeFocused(ProviderAccountPrimaryActionTag)

        composeRule.onNodeWithTag(ProviderAccountPrimaryActionTag).performKeyInput {
            pressKey(androidx.compose.ui.input.key.Key.DirectionDown)
        }
        waitUntilNodeFocused(ProviderAccountLogoutActionTag)

        composeRule.onNodeWithTag(ProviderAccountLogoutActionTag).performKeyInput {
            pressKey(androidx.compose.ui.input.key.Key.DirectionDown)
        }
        waitUntilNodeFocused(ProviderAccountCreateActionTag)
    }

    @Test
    fun backClosesInAppForm_thenNavigatesBack() {
        val prefix = "test_provider_back_${System.nanoTime()}"
        seedAccounts(prefix)
        val viewModel = ProviderAccountViewModel(testAuthRepo(prefix))
        var backCount = 0

        composeRule.setContent {
            MaterialTheme {
                ProviderAccountScreen(
                    stateFlow = viewModel.uiState,
                    viewModel = viewModel,
                    providerName = "Test Provider",
                    createAccountUrl = "https://example.com/signup",
                    isPreview = false,
                    onBack = { backCount += 1 },
                    onAccountChanged = {},
                    onOpenCreateAccount = {},
                )
            }
        }

        composeRule.onNodeWithTag(ProviderAccountPrimaryActionTag)
            .performSemanticsAction(SemanticsActions.OnClick)
        waitUntilNodeExists(ProviderAccountUsernameFieldTag)

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        waitUntilNodeDoesNotExist(ProviderAccountUsernameFieldTag)
        composeRule.runOnIdle {
            assertEquals(0, backCount)
        }

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(1, backCount)
        }
    }

    private fun seedAccounts(prefix: String) {
        AccountManager.cachedAccounts[prefix] = arrayOf(
            AuthData(
                user = AuthUser(
                    name = "Main",
                    id = 101,
                ),
                token = AuthToken(accessToken = "token-101")
            ),
            AuthData(
                user = AuthUser(
                    name = "Alt",
                    id = 202,
                ),
                token = AuthToken(accessToken = "token-202")
            ),
        )
        AccountManager.cachedAccountIds[prefix] = 101
    }

    private fun testAuthRepo(prefix: String): AuthRepo {
        val api = object : AuthAPI() {
            override val name: String = "Test Provider"
            override val idPrefix: String = prefix
            override val hasInApp: Boolean = true
            override val inAppLoginRequirement: AuthLoginRequirement =
                AuthLoginRequirement(username = true, password = true)
        }

        return object : AuthRepo(api) {}
    }

    private fun requestNodeFocus(tag: String) {
        composeRule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)
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

    private fun waitUntilNodeDoesNotExist(tag: String, timeoutMillis: Long = 2_000L) {
        composeRule.waitUntil(timeoutMillis) {
            composeRule
                .onAllNodes(hasTestTag(tag), useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
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
