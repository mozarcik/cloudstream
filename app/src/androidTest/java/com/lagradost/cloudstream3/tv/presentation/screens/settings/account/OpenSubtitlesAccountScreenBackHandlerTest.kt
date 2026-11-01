package com.lagradost.cloudstream3.tv.presentation.screens.settings.account

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.syncproviders.AuthAPI
import com.lagradost.cloudstream3.syncproviders.AuthRepo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenSubtitlesAccountScreenBackHandlerTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun back_hidesLoginForm_thenNavigatesBack() {
        val prefix = "test_opensubtitles_back_${System.nanoTime()}"
        seedEmptyAccountState(prefix)
        val viewModel = OpenSubtitlesAccountViewModel(testAuthRepo(prefix))
        var backCount = 0

        composeRule.setContent {
            MaterialTheme {
                OpenSubtitlesAccountScreen(
                    stateFlow = viewModel.uiState,
                    viewModel = viewModel,
                    providerName = "OpenSubtitles",
                    createAccountUrl = null,
                    isPreview = false,
                    onBack = { backCount += 1 },
                    onAccountChanged = {},
                    onOpenCreateAccount = {},
                )
            }
        }

        composeRule.runOnIdle {
            assertTrue(viewModel.uiState.value.showLoginForm)
            assertEquals(0, backCount)
        }

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertFalse(viewModel.uiState.value.showLoginForm)
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

    private fun seedEmptyAccountState(prefix: String) {
        AccountManager.cachedAccounts[prefix] = emptyArray()
        AccountManager.cachedAccountIds[prefix] = AccountManager.NONE_ID
    }

    private fun testAuthRepo(prefix: String): AuthRepo {
        val api = object : AuthAPI() {
            override val name: String = "Test OpenSubtitles"
            override val idPrefix: String = prefix
        }

        return object : AuthRepo(api) {}
    }
}
