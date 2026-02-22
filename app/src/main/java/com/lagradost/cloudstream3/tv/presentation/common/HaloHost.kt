package com.lagradost.cloudstream3.tv.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

private const val HALO_RENDERING_ENABLED = true
private const val HALO_FOCUS_DEBOUNCE_MS = 160L
private const val HALO_CLEAR_DEBOUNCE_MS = 48L
private const val HALO_STATE_RECT_EPSILON_PX = 2f

@Immutable
data class HaloState(
    val key: Any,
    val rectInRoot: Rect,
    val color: Color,
)

@Stable
interface HaloController {
    fun onItemFocused(
        key: Any,
        rectInRoot: Rect,
        color: Color,
    )

    fun onItemFocusCleared(key: Any) = Unit
}

private object NoOpHaloController : HaloController {
    override fun onItemFocused(key: Any, rectInRoot: Rect, color: Color) = Unit
}

val LocalHaloController = staticCompositionLocalOf<HaloController> {
    NoOpHaloController
}

val LocalHaloEnabled = staticCompositionLocalOf { true }

@Composable
fun HaloHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!HALO_RENDERING_ENABLED) {
        CompositionLocalProvider(
            LocalHaloController provides NoOpHaloController,
            LocalHaloEnabled provides false
        ) {
            Box(modifier = modifier) {
                Box(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
        }
        return
    }

    val haloStateFlow = remember { MutableStateFlow<HaloState?>(null) }
    val hostBoundsFlow = remember { MutableStateFlow(Rect.Zero) }
    val runtimeState = remember { HaloHostRuntimeState() }
    val coroutineScope = rememberCoroutineScope()

    val controller = remember {
        object : HaloController {
            override fun onItemFocused(key: Any, rectInRoot: Rect, color: Color) {
                if (rectInRoot.width <= 0f || rectInRoot.height <= 0f) {
                    return
                }

                runtimeState.clearJob?.cancel()
                val nextState = HaloState(
                    key = key,
                    rectInRoot = rectInRoot,
                    color = color
                )

                val wasFocusedKey = runtimeState.focusedKey
                runtimeState.focusedKey = key
                if (wasFocusedKey == key) {
                    // Ten sam item aktualizujemy natychmiast (scroll / relayout).
                    runtimeState.pendingFocusState = null
                    runtimeState.focusDebounceJob?.cancel()
                    if (haloStateFlow.value?.isEquivalentTo(nextState) == true) {
                        return
                    }
                    haloStateFlow.value = nextState
                    return
                }

                // Zmianę aktywnego itemu debouncujemy, żeby szybkie ruchy D-pad
                // nie przepalały zmian koloru i pozycji halo.
                runtimeState.pendingFocusState = nextState
                runtimeState.focusDebounceJob?.cancel()
                runtimeState.focusDebounceJob = coroutineScope.launch {
                    delay(HALO_FOCUS_DEBOUNCE_MS)
                    val pendingState = runtimeState.pendingFocusState ?: return@launch
                    if (runtimeState.focusedKey != pendingState.key) {
                        return@launch
                    }
                    if (haloStateFlow.value?.isEquivalentTo(pendingState) == true) {
                        return@launch
                    }
                    haloStateFlow.value = pendingState
                }
            }

            override fun onItemFocusCleared(key: Any) {
                val isCurrentFocusedKey = runtimeState.focusedKey == key
                val isPendingFocusedKey = runtimeState.pendingFocusState?.key == key
                if (!isCurrentFocusedKey && !isPendingFocusedKey) {
                    return
                }

                if (isCurrentFocusedKey) {
                    runtimeState.focusedKey = null
                }
                if (isPendingFocusedKey) {
                    runtimeState.pendingFocusState = null
                }

                runtimeState.focusDebounceJob?.cancel()
                runtimeState.clearJob?.cancel()
                runtimeState.clearJob = coroutineScope.launch {
                    delay(HALO_CLEAR_DEBOUNCE_MS)
                    if (runtimeState.focusedKey == null && runtimeState.pendingFocusState == null) {
                        haloStateFlow.value = null
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                hostBoundsFlow.value = coordinates.boundsInRoot()
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            HaloBackgroundFromFlow(
                haloStateFlow = haloStateFlow,
                hostBoundsInRootFlow = hostBoundsFlow,
                modifier = Modifier
                    .fillMaxSize()
            )
        }

        CompositionLocalProvider(
            LocalHaloController provides controller,
            LocalHaloEnabled provides true
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

@Composable
private fun HaloBackgroundFromFlow(
    haloStateFlow: StateFlow<HaloState?>,
    hostBoundsInRootFlow: StateFlow<Rect>,
    modifier: Modifier = Modifier,
) {
    val haloState = haloStateFlow.collectAsState().value
    val hostBoundsInRoot = hostBoundsInRootFlow.collectAsState().value

    HaloBackground(
        state = haloState,
        hostBoundsInRoot = hostBoundsInRoot,
        modifier = modifier
    )
}

@Composable
fun HaloBackground(state: HaloState?) {
    HaloBackground(
        state = state,
        hostBoundsInRoot = Rect.Zero,
        modifier = Modifier
    )
}

@Composable
fun HaloBackground(
    state: HaloState?,
    hostBoundsInRoot: Rect,
    modifier: Modifier = Modifier,
) {
    AmbientHaloLayer(
        state = state,
        hostBoundsInRoot = hostBoundsInRoot,
        modifier = modifier
    )
}

private class HaloHostRuntimeState {
    var focusedKey: Any? = null
    var pendingFocusState: HaloState? = null
    var focusDebounceJob: Job? = null
    var clearJob: Job? = null
}

private fun HaloState.isEquivalentTo(other: HaloState): Boolean {
    return key == other.key &&
        color == other.color &&
        rectInRoot.isApproximatelyEqualTo(other.rectInRoot, HALO_STATE_RECT_EPSILON_PX)
}

private fun Rect.isApproximatelyEqualTo(
    other: Rect,
    epsilonPx: Float,
): Boolean {
    return abs(left - other.left) <= epsilonPx &&
        abs(top - other.top) <= epsilonPx &&
        abs(right - other.right) <= epsilonPx &&
        abs(bottom - other.bottom) <= epsilonPx
}
