@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.lagradost.cloudstream3.tv.presentation.common

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.Icon
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

/**
 * Source / filter chip (28 dp, pill). Selected chips show a check and the [accent] color;
 * focus turns the chip white (no scale), styled by [DesignChipStyle].
 */
@Composable
fun TvChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    height: Dp = TvDimens.ChipHeight,
    /** Marker shown instead of the check mark when [selected] (Home sources: an accent dot). */
    selectedTrailing: (@Composable () -> Unit)? = null,
    /** Icon before the text while not selected. */
    leadingIcon: ImageVector? = null,
    /** Selected look other than the accent tint (Home sources: white 14%, no outline). */
    selectedFill: Color? = null,
    selectedBorderColor: Color? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        shape = DesignChipStyle.shape,
        colors = if (selectedFill != null) DesignChipStyle.colors(accent, selectedFill) else DesignChipStyle.colors(accent),
        border = if (selectedBorderColor != null) DesignChipStyle.border(accent, selectedBorderColor) else DesignChipStyle.border(accent),
        scale = DesignChipStyle.scale,
        leadingIcon = when {
            selected && selectedTrailing == null -> {
                {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = null,
                        modifier = Modifier.size(TvDimens.ChipIconSize),
                    )
                }
            }

            !selected && leadingIcon != null -> {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(TvDimens.ChipIconSize),
                    )
                }
            }

            else -> null
        },
        trailingIcon = if (selected) selectedTrailing else null,
        modifier = modifier.height(height),
    ) {
        ProvideTextStyle(value = TvTextStyles.Chip) {
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // FilterChip adds vertical padding that does not fit a 26-28 dp chip: centre the line instead of clipping it.
                modifier = Modifier.wrapContentHeight(align = Alignment.CenterVertically, unbounded = true),
            )
        }
    }
}
