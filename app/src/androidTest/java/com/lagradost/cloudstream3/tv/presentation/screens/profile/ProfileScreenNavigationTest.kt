package com.lagradost.cloudstream3.tv.presentation.screens.profile

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
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
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.utils.DataStoreHelper
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val ProfileAccountDefaultTag = "profile_account_0"
private const val ProfileAccountKidTag = "profile_account_1"
private const val ProfileAddAccountTag = "profile_add_account"
private const val ProfileManageButtonTag = "profile_manage_button"
private const val ProfileEditorNameTag = "profile_editor_name"

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class ProfileScreenNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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
    fun dpadMovesAcrossProfileCardsAndEntersManageEditor() {
        composeRule.setContent {
            MaterialTheme {
                val viewModel = remember { ProfileViewModel() }
                val topBarFocusRequester = remember { FocusRequester() }
                ProfileScreen(
                    topBarFocusRequester = topBarFocusRequester,
                    onTopBarVisibilityChanged = {},
                    onTopBarFocusableChanged = {},
                    onTopBarDownNavigationEnabledChanged = {},
                    viewModel = viewModel,
                )
            }
        }

        waitUntilNodeExists(ProfileAccountDefaultTag)
        requestNodeFocus(ProfileAccountDefaultTag)

        composeRule.onNodeWithTag(ProfileAccountDefaultTag).performKeyInput {
            pressKey(androidx.compose.ui.input.key.Key.DirectionDown)
        }
        waitUntilNodeFocused(ProfileAccountKidTag)

        composeRule.onNodeWithTag(ProfileAccountKidTag).performKeyInput {
            pressKey(androidx.compose.ui.input.key.Key.DirectionDown)
        }
        waitUntilNodeFocused(ProfileAddAccountTag)

        requestNodeFocus(ProfileAccountDefaultTag)
        composeRule.onNodeWithTag(ProfileAccountDefaultTag).performKeyInput {
            pressKey(androidx.compose.ui.input.key.Key.DirectionRight)
        }
        waitUntilNodeFocused(ProfileManageButtonTag)

        composeRule.onNodeWithTag(ProfileManageButtonTag)
            .performSemanticsAction(SemanticsActions.OnClick)
        waitUntilNodeFocused(ProfileEditorNameTag)
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
