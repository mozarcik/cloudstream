package com.lagradost.cloudstream3.phone.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneColors
import kotlin.math.roundToInt

/**
 * Top bar over a scrolling list: it slides away while scrolling down and comes back the moment the list scrolls up
 * (no need to reach the top to get to the menu). The bar sits under the status bar, which keeps a solid backdrop.
 * The content gets [PaddingValues] whose top is status bar + bar, so its first item starts below the bar.
 */
@Composable
fun HideOnScrollScaffold(
    modifier: Modifier = Modifier,
    barHeight: Dp = 64.dp,
    bar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val density = LocalDensity.current
    val barHeightPx = with(density) { barHeight.toPx() }
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var offsetPx by remember { mutableFloatStateOf(0f) }
    val connection = remember(barHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Scrolling the list down (finger up) gives a negative y: push the bar out; scrolling up pulls it back in.
                offsetPx = (offsetPx + available.y).coerceIn(-barHeightPx, 0f)
                return Offset.Zero
            }
        }
    }
    Box(modifier.fillMaxSize().nestedScroll(connection)) {
        content(PaddingValues(top = statusTop + barHeight))
        Box(
            Modifier.fillMaxWidth().height(barHeight)
                .offset { IntOffset(0, with(density) { statusTop.roundToPx() } + offsetPx.roundToInt()) }
                .background(PhoneColors.Ground),
        ) { bar() }
        // Solid status bar area above the sliding bar, so the bar slides out under it instead of over the clock.
        Box(Modifier.fillMaxWidth().height(statusTop).background(PhoneColors.Ground))
    }
}
