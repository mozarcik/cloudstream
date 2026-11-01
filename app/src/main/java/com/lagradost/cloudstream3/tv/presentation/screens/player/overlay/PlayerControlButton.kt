package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.focus.tvFocusFill
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

/**
 * Pill of the controls row (mockups PlayerControls*): icon + label + optional value.
 * Focus: white fill with dark content, no scale. One Row, no wrappers.
 */
@Composable
internal fun PlayerOptionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    value: String? = null,
    minWidth: Dp = Dp.Unspecified,
    onFocused: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val content = if (focused) TvColors.OnFocus else TvColors.Text
    val valueColor = if (focused) TvColors.OnFocusSecondary else TvColors.PlayerValue

    Row(
        horizontalArrangement = Arrangement.spacedBy(TvDimens.PlayerOptionGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(TvDimens.PlayerOptionHeight)
            .defaultMinSize(minWidth = minWidth)
            .tvFocusFill(
                focused = focused,
                shape = TvShapes.Pill,
                container = TvColors.PlayerFillOption,
            )
            .focusProperties { canFocus = enabled }
            .onFocusChanged { if (it.isFocused) onFocused() }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(
                start = TvDimens.PlayerOptionPaddingStart,
                end = TvDimens.PlayerOptionPaddingEnd,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(TvDimens.PlayerOptionIconSize),
        )
        Text(
            text = label,
            style = TvTextStyles.PlayerOption,
            color = content,
            maxLines = 1,
        )
        if (value != null) {
            Text(
                text = value,
                style = TvTextStyles.PlayerValue,
                color = valueColor,
                maxLines = 1,
            )
        }
    }
}
