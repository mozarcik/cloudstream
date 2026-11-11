package com.lagradost.cloudstream3.tv.presentation.focus

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged

/**
 * D-pad navigation infrastructure shared by every TV screen:
 *
 * - initial focus: [TvInitialFocusEffect] (or [TvFocusMemory.RestoreEffect] when a screen can be re-entered),
 * - remembering the last focused element across Back / re-entry: [TvFocusMemory],
 * - focus trap for overlays (panels, dialogs): [Modifier.tvFocusTrap],
 * - consistent Back: [TvBackHandler].
 */

/** Requests focus once when the screen enters composition (or when [key] changes). */
@Composable
fun TvInitialFocusEffect(
    requester: FocusRequester,
    key: Any? = Unit,
    enabled: Boolean = true,
    onFocused: (() -> Unit)? = null,
) {
    FocusRequestEffect(
        requester = requester,
        requestKey = key,
        enabled = enabled,
        onFocused = onFocused,
    )
}

/**
 * Remembers which element (by string key) had focus last, survives configuration changes and
 * navigation back-stack restores (when hoisted above the NavHost entry or saved with the entry).
 */
@Stable
class TvFocusMemory(initialKey: String? = null) {
    var lastKey: String? by mutableStateOf(initialKey)
        private set

    private val requesters = HashMap<String, FocusRequester>()

    fun requesterFor(key: String): FocusRequester =
        requesters.getOrPut(key) { FocusRequester() }

    /** Marks [key] as the remembered element whenever it (or a descendant) holds focus. */
    fun Modifier.track(key: String): Modifier = this
        .focusRequester(requesterFor(key))
        .onFocusChanged { state ->
            if (state.hasFocus) lastKey = key
        }

    fun forget() {
        lastKey = null
    }

    /**
     * Restores focus to the remembered element, or to [fallbackKey] on first entry.
     * Re-runs when [restoreToken] changes.
     */
    @Composable
    fun RestoreEffect(
        fallbackKey: String?,
        restoreToken: Any? = Unit,
        enabled: Boolean = true,
    ) {
        val target = lastKey ?: fallbackKey
        FocusRequestEffect(
            requester = target?.let(::requesterFor),
            requestKey = restoreToken,
            enabled = enabled && target != null,
        )
    }
}

@Composable
fun rememberTvFocusMemory(initialKey: String? = null): TvFocusMemory {
    var saved by rememberSaveable { mutableStateOf(initialKey) }
    val memory = remember { TvFocusMemory(saved) }
    LaunchedEffect(memory.lastKey) { saved = memory.lastKey }
    return memory
}

/**
 * Keeps D-pad focus inside this subtree (side panels, dialogs). Combine with [TvBackHandler].
 * While [enabled] is false (a panel sliding out) focus may leave, also by [FocusRequester.requestFocus].
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.tvFocusTrap(enabled: Boolean = true): Modifier = this
    .focusProperties { exit = { if (enabled) FocusRequester.Cancel else FocusRequester.Default } }
    .focusGroup()

/** Single place for Back handling so overlays close the same way everywhere. */
@Composable
fun TvBackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
