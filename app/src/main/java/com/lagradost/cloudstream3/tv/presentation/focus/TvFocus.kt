package com.lagradost.cloudstream3.tv.presentation.focus

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import com.lagradost.cloudstream3.tv.presentation.screens.dashboard.LocalDashboardContentEntry
import kotlinx.coroutines.delay

/**
 * Focus memory of one lazy row or grid: which item had focus last, and a focus group that
 * enters on it, so one requestFocus() on the group lands on the remembered item.
 */
@Stable
class TvFocusGroup(initialIndex: Int = 0) {
    /** Index of the item that had focus last. A plain var: never read in composition. */
    var lastIndex: Int = initialIndex

    /** The group itself. */
    val requester = FocusRequester()

    private val itemRequesters = HashMap<Int, FocusRequester>()

    /** Requester of the item at [index]; stable per index. */
    fun itemRequester(index: Int): FocusRequester = itemRequesters.getOrPut(index) { FocusRequester() }

    /** Call from the item's existing onFocused callback (no extra modifier per item). */
    fun onItemFocused(index: Int) {
        lastIndex = index
    }

    /**
     * Focus the remembered item; false when the group is not attached / nothing to focus. A group that already
     * holds focus is left alone: Compose would not enter it again, and would move focus off the item it is on.
     */
    fun requestFocus(): Boolean = hasFocus || requester.tryRequestFocus()

    private var hasFocus = false

    /**
     * [requestFocus] again for a few frames: a row that was just created or scrolled into view needs time to
     * compose, and entering it before the remembered item is there would land on some other item.
     */
    suspend fun requestFocusWithRetry(attempts: Int = DefaultAttempts, delayMs: Long = DefaultDelayMs): Boolean {
        repeat(attempts) { attempt ->
            val ready = attempt == attempts - 1 || isItemComposedCheck?.invoke(lastIndex) != false
            if (ready && requestFocus()) return true
            delay(delayMs)
        }
        return false
    }

    private var isItemComposedCheck: ((Int) -> Boolean)? = null

    /**
     * Put on the lazy container. [isItemComposed] must tell whether index is currently composed
     * (lazy state's visibleItemsInfo). While [hold] is true and the remembered item is not
     * composed, entering is cancelled (a restore is waiting for data).
     */
    @OptIn(ExperimentalComposeUiApi::class)
    fun Modifier.tvFocusGroup(
        isItemComposed: (Int) -> Boolean,
        hold: () -> Boolean = { false },
    ): Modifier = this
        .focusRequester(requester)
        .onFocusChanged { hasFocus = it.hasFocus }
        .also { isItemComposedCheck = isItemComposed }
        .focusProperties {
            enter = {
                val index = lastIndex
                when {
                    isItemComposed(index) -> itemRequester(index)
                    hold() -> FocusRequester.Cancel
                    else -> FocusRequester.Default
                }
            }
        }
        .focusGroup()
}

internal fun LazyListState.isItemComposed(index: Int): Boolean =
    layoutInfo.visibleItemsInfo.any { it.index == index }

internal fun LazyGridState.isItemComposed(index: Int): Boolean =
    layoutInfo.visibleItemsInfo.any { it.index == index }

private const val DefaultAttempts = 18
private const val DefaultDelayMs = 16L

/** A requester that is not attached (its item is not composed yet) throws; that is just "not yet". */
fun FocusRequester.tryRequestFocus(): Boolean = runCatching { requestFocus() }.getOrDefault(false)

suspend fun FocusRequester.tryRequestFocusWithRetry(
    attempts: Int = DefaultAttempts,
    delayMs: Long = DefaultDelayMs,
): Boolean {
    repeat(attempts) {
        if (tryRequestFocus()) return true
        delay(delayMs)
    }
    return false
}

/** How a Dashboard screen answers "put focus into your content". All run on the main thread. */
interface TvScreenFocus {
    /** Fresh entry (app start, opened from the menu): the screen's starting element. */
    suspend fun enter(): Boolean

    /** Coming back (menu closed, Back from a title, Back from a child screen): where focus was. */
    suspend fun restore(): Boolean = enter()

    /** Back inside the screen; true when it did something. */
    fun back(): Boolean = false
}

/**
 * Registers the screen with the Dashboard; the lambdas may read state freely, the Dashboard always calls
 * the latest ones. `false` from [enter] / [restore] means "not yet" (data is loading): the Dashboard asks again.
 */
@Composable
fun TvScreenFocusEffect(
    enter: suspend () -> Boolean,
    restore: (suspend () -> Boolean)? = null,
    back: (() -> Boolean)? = null,
) {
    val entry = LocalDashboardContentEntry.current
    val latestEnter = rememberUpdatedState(enter)
    val latestRestore = rememberUpdatedState(restore)
    val latestBack = rememberUpdatedState(back)
    DisposableEffect(entry) {
        val screen = object : TvScreenFocus {
            override suspend fun enter(): Boolean = latestEnter.value()
            override suspend fun restore(): Boolean = latestRestore.value?.invoke() ?: latestEnter.value()
            override fun back(): Boolean = latestBack.value?.invoke() ?: false
        }
        entry.screen = screen
        onDispose { if (entry.screen === screen) entry.screen = null }
    }
}
