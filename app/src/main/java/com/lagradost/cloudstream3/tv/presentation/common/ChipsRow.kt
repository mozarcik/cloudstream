package com.lagradost.cloudstream3.tv.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

// ---------- Public API ----------

@Composable
fun ChipsRow(
    modifier: Modifier = Modifier,
    minGap: Dp = 8.dp,
    content: ChipsRowScope.() -> Unit
) {
    val scope = ChipsRowScopeImpl().apply(content)

    SubcomposeLayout(modifier) { constraints ->
        scope.measureAndLayout(this, constraints, minGap)
    }
}

interface ChipsRowScope {
    fun item(key: Any? = null, content: @Composable () -> Unit)

    fun <T> items(
        list: List<T>,
        key: ((T) -> Any)? = null,
        itemContent: @Composable (T) -> Unit
    )

    /** Rendered ONLY if overflow actually happens. */
    fun overflow(content: @Composable () -> Unit)
}

// ---------- Implementation ----------

private data class Entry(
    val key: Any?,
    val content: @Composable () -> Unit
)

private class ChipsRowScopeImpl : ChipsRowScope {
    private val entries = ArrayList<Entry>()
    private var overflowContent: (@Composable () -> Unit)? = null

    override fun item(key: Any?, content: @Composable () -> Unit) {
        entries += Entry(key, content)
    }

    override fun <T> items(list: List<T>, key: ((T) -> Any)?, itemContent: @Composable (T) -> Unit) {
        list.forEachIndexed { index, t ->
            val k = key?.invoke(t) ?: index
            entries += Entry(k) { itemContent(t) }
        }
    }

    override fun overflow(content: @Composable () -> Unit) {
        overflowContent = content
    }

    fun measureAndLayout(
        scope: androidx.compose.ui.layout.SubcomposeMeasureScope,
        constraints: Constraints,
        minGap: Dp
    ): androidx.compose.ui.layout.MeasureResult = with(scope) {
        val maxW = constraints.maxWidth
        val childConstraints = constraints.copy(minWidth = 0)

        val minGapPx = with(this@with) { minGap.roundToPx() }

        // Measure overflow ("more") once; place only if overflow happens.
        val overflowComposable = overflowContent
        val overflowPlaceable: Placeable? = overflowComposable
            ?.let { subcompose(OverflowSlot, it).first().measure(childConstraints) }

        val visibleChips = ArrayList<Placeable>(entries.size)
        var sumChipsW = 0
        var overflow = false

        for (i in entries.indices) {
            val entry = entries[i]
            val hasMoreAfter = i < entries.lastIndex
            val canShowOverflow = overflowPlaceable != null
            val reserveOverflow = canShowOverflow && hasMoreAfter
            val reserveOverflowW = if (reserveOverflow) overflowPlaceable.width else 0

            val p = subcompose(ChipSlot(entry.key ?: i), entry.content)
                .first()
                .measure(childConstraints)

            val tentativeChipsCount = visibleChips.size + 1
            val visibleCountIfAccept = tentativeChipsCount + if (reserveOverflow) 1 else 0
            val gapsCountIfAccept = max(0, visibleCountIfAccept - 1)

            val requiredW =
                (sumChipsW + p.width) +
                        reserveOverflowW +
                        gapsCountIfAccept * minGapPx

            if (requiredW <= maxW) {
                visibleChips += p
                sumChipsW += p.width
            } else {
                overflow = reserveOverflow
                break
            }
        }

        val visible: List<Placeable> =
            if (overflow) visibleChips + requireNotNull(overflowPlaceable) else visibleChips

        val count = visible.size
        val gapsCount = max(0, count - 1)

        val rowH = visible.maxOfOrNull { it.height } ?: 0
        val totalW = visible.sumOf { it.width }

        // Ensure at least minGapPx between items, then distribute remaining pixels perfectly.
        val minGapsW = gapsCount * minGapPx
        val extra = (maxW - (totalW + minGapsW)).coerceAtLeast(0)

        val baseExtra = if (gapsCount > 0) extra / gapsCount else 0
        val remainder = if (gapsCount > 0) extra % gapsCount else 0

        layout(width = maxW, height = rowH) {
            var x = 0
            visible.forEachIndexed { index, p ->
                p.placeRelative(x, (rowH - p.height) / 2)

                if (index < visible.lastIndex) {
                    val plusOne = if (index < remainder) 1 else 0
                    val gap = minGapPx + baseExtra + plusOne
                    x += p.width + gap
                }
            }
        }
    }

    private object OverflowSlot
    private data class ChipSlot(val key: Any)
}
