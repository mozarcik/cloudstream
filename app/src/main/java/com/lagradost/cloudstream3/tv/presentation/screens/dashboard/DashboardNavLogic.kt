package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

/** Threshold between horizontal presses below which Left may not leave the content (tune 200-300 ms). */
internal const val EDGE_EXIT_DEBOUNCE_MS = 250L

/**
 * Decides whether Left at the content's left edge may move focus to the menu.
 *
 * Every horizontal KeyDown (also a blocked one, so a burst stays blocked until it ends) is fed to
 * [onHorizontalKey]; Up / Down are never fed. A held key (repeatCount > 0) is always blocked.
 */
internal class EdgeExitGuard(private val debounceMs: Long = EDGE_EXIT_DEBOUNCE_MS) {
    private var lastHorizontalMs = Long.MIN_VALUE / 2
    private var bouncedInSeries = false

    /** True when the press that was just fed in must not leave the content. */
    var blocked = false
        private set

    fun onHorizontalKey(eventTimeMs: Long, repeatCount: Int) {
        blocked = repeatCount > 0 || eventTimeMs - lastHorizontalMs < debounceMs
        lastHorizontalMs = eventTimeMs
        if (!blocked) bouncedInSeries = false
    }

    /** Feedback ("bounce") is shown once per blocked series; returns true the first time. */
    fun consumeBounce(): Boolean {
        if (!blocked || bouncedInSeries) return false
        bouncedInSeries = true
        return true
    }
}

internal enum class DashboardBackAction {
    /** Menu focused on a section other than Home: switch to Home, focus stays in the menu. */
    MenuGoHome,

    /** Menu focused on Home: leave the app (one Back, no confirmation). */
    ExitApp,

    /** A grid opened from a tile: back to where it was opened from. */
    PopBackStack,

    /** The screen consumed Back itself (scrolled content -> top). */
    ContentHandled,

    /** Content is at the top of a section's main screen: focus goes to the menu. */
    OpenMenu,
}

/**
 * Back, first match wins (panels and dialogs have their own handlers that run before this one):
 * menu on a non-Home section -> Home • menu on Home -> exit • sub-screen -> pop • scrolled content ->
 * top • content at the top -> menu.
 */
internal fun resolveDashboardBackAction(
    isMenuFocused: Boolean,
    isOnHome: Boolean,
    isOnTopLevelScreen: Boolean,
    hasPreviousBackStackEntry: Boolean,
    contentHandledBack: Boolean,
): DashboardBackAction = when {
    isMenuFocused -> if (isOnHome) DashboardBackAction.ExitApp else DashboardBackAction.MenuGoHome
    !isOnTopLevelScreen && hasPreviousBackStackEntry -> DashboardBackAction.PopBackStack
    contentHandledBack -> DashboardBackAction.ContentHandled
    else -> DashboardBackAction.OpenMenu
}
