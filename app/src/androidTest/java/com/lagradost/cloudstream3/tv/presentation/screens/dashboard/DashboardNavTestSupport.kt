package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.input.key.Key
import androidx.test.platform.app.InstrumentationRegistry
import com.lagradost.cloudstream3.tv.presentation.screens.Screens

/** Shared helpers of the dashboard instrumentation tests (menu + rail navigation). */
internal class DashboardNavTestSupport(private val rule: ComposeContentTestRule) {
    var observedRoute: String? = null

    fun waitUntilRoute(screen: Screens, timeoutMillis: Long = 3_000L) {
        val expected = screen()
        rule.waitUntil(timeoutMillis) { observedRoute == expected }
    }

    fun waitUntilNodeExists(tag: String, timeoutMillis: Long = 3_000L) {
        rule.waitUntil(timeoutMillis) {
            rule.onAllNodes(hasTestTag(tag), useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    fun waitUntilNodeGone(tag: String, timeoutMillis: Long = 3_000L) {
        rule.waitUntil(timeoutMillis) {
            rule.onAllNodes(hasTestTag(tag), useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }
    }

    fun waitUntilNodeFocused(tag: String, timeoutMillis: Long = 3_000L) {
        try {
            rule.waitUntil(timeoutMillis) {
                rule.onAllNodes(hasTestTag(tag), useUnmergedTree = true)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .any { it.config.getOrElse(SemanticsProperties.Focused) { false } }
            }
        } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
            val tree = rule.onRoot(useUnmergedTree = true).printToString(maxDepth = 12)
            throw AssertionError("Node '$tag' never got focus (route=$observedRoute). Tree:\n$tree", e)
        }
        rule.onNodeWithTag(tag, useUnmergedTree = true).assertIsFocused()
    }

    fun pressKeyOn(tag: String, key: Key) {
        rule.onNodeWithTag(tag, useUnmergedTree = true).performKeyInput { pressKey(key) }
        rule.waitForIdle()
    }

    fun pressHardwareBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        rule.waitForIdle()
    }

    fun requestFocus(tag: String) {
        rule.onNodeWithTag(tag, useUnmergedTree = true).performSemanticsAction(SemanticsActions.RequestFocus)
        rule.waitForIdle()
    }

    fun click(tag: String) {
        rule.onNodeWithTag(tag, useUnmergedTree = true).performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
    }

    /** Left from the content's first column opens the menu; focus lands on the current screen's row. */
    fun openMenuFrom(focusedContentTag: String, selectedRowTag: String) {
        pauseBeforeEdgeExit()
        pressKeyOn(focusedContentTag, Key.DirectionLeft)
        waitUntilNodeFocused(selectedRowTag)
    }

    /** Left right after another horizontal press is blocked by design; let the debounce elapse. */
    fun pauseBeforeEdgeExit() {
        rule.mainClock.advanceTimeBy(EDGE_EXIT_DEBOUNCE_MS + 50)
    }

    /** Opens the menu from the focused content and picks [target]. */
    fun navigateViaMenu(focusedContentTag: String, selectedRowTag: String, target: TvNavTarget) {
        openMenuFrom(focusedContentTag, selectedRowTag)
        val rowTag = tvNavScreenTestTag(target)
        requestFocus(rowTag)
        waitUntilNodeFocused(rowTag)
        click(rowTag)
    }
}
