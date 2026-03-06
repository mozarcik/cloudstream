package com.lagradost.cloudstream3.tv.presentation.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.delay

private const val DefaultFocusRequestAttempts = 20
private const val DefaultFocusRequestDelayMs = 16L

@Composable
fun rememberFocusRequesters(count: Int): List<FocusRequester> {
    return remember(count) {
        List(size = count) { FocusRequester() }
    }
}

@Composable
fun <K> rememberFocusRequesterMap(keys: List<K>): Map<K, FocusRequester> {
    val cachedRequesters = remember { LinkedHashMap<K, FocusRequester>() }

    return remember(keys) {
        val currentKeySet = keys.toSet()
        cachedRequesters.keys.retainAll(currentKeySet)
        keys.associateWith { key ->
            cachedRequesters.getOrPut(key) { FocusRequester() }
        }
    }
}

suspend fun FocusRequester.requestFocusWithRetry(
    attempts: Int = DefaultFocusRequestAttempts,
    retryDelayMs: Long = DefaultFocusRequestDelayMs,
): Boolean {
    repeat(attempts) {
        if (requestFocus()) {
            return true
        }
        delay(retryDelayMs)
    }

    return false
}

@Composable
fun FocusRequestEffect(
    requester: FocusRequester?,
    requestKey: Any?,
    enabled: Boolean = true,
    attempts: Int = DefaultFocusRequestAttempts,
    retryDelayMs: Long = DefaultFocusRequestDelayMs,
    onFocused: (() -> Unit)? = null,
) {
    LaunchedEffect(requester, requestKey, enabled) {
        if (!enabled || requester == null || requestKey == null) {
            return@LaunchedEffect
        }

        if (requester.requestFocusWithRetry(attempts = attempts, retryDelayMs = retryDelayMs)) {
            onFocused?.invoke()
        }
    }
}

fun <K> resolveAdjacentFocusKey(
    keys: List<K>,
    removedKey: K,
): K? {
    val removedIndex = keys.indexOf(removedKey)
    if (removedIndex < 0) {
        return keys.firstOrNull()
    }

    return keys.getOrNull(removedIndex + 1) ?: keys.getOrNull(removedIndex - 1)
}

fun <K> resolveAvailableFocusKey(
    availableKeys: List<K>,
    vararg preferredKeys: K?,
): K? {
    preferredKeys.forEach { preferredKey ->
        if (preferredKey != null && preferredKey in availableKeys) {
            return preferredKey
        }
    }

    return availableKeys.firstOrNull()
}
