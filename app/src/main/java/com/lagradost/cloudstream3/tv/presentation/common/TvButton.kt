package com.lagradost.cloudstream3.tv.presentation.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.focus.tvFocusFill
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

enum class TvButtonVariant {
    /** Translucent fill (call to action next to a primary one). */
    Default,

    /** Accent fill ([TvButton.accent]); turns white on focus like every other button. */
    Primary,

    /** Default fill with red label, for destructive actions in dialogs. */
    Destructive,
}

/**
 * Pill button of the redesign: icon + label, optional fixed [width], 3 variants + disabled.
 * Focus: white fill with dark content, no scale. Flat tree: one Row.
 */
@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    variant: TvButtonVariant = TvButtonVariant.Default,
    enabled: Boolean = true,
    width: Dp? = null,
    height: Dp = TvDimens.ButtonHeight,
    textStyle: TextStyle = TvTextStyles.Button,
    accent: Color = MaterialTheme.colorScheme.primary,
    onAccent: Color = MaterialTheme.colorScheme.onPrimary,
    /** Content color while not focused (state tint, e.g. green "Downloaded"); the focused color wins. */
    tint: Color? = null,
    /** Replaces [icon] with custom content (e.g. a progress ring); receives the current content color. */
    iconContent: (@Composable (Color) -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    val container = when {
        !enabled -> TvColors.FillButtonDisabled
        variant == TvButtonVariant.Primary -> accent
        else -> TvColors.FillButton
    }
    val content = when {
        focused && enabled -> colors.inverseOnSurface
        !enabled -> TvColors.Disabled
        variant == TvButtonVariant.Primary -> onAccent
        tint != null -> tint
        variant == TvButtonVariant.Destructive -> TvColors.Danger
        else -> colors.onSurface
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(TvDimens.ButtonIconGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .height(height)
            .tvFocusFill(focused = focused && enabled, shape = TvShapes.Pill, container = container, focusedContainer = colors.inverseSurface)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(
                PaddingValues(
                    start = if (icon != null || iconContent != null) TvDimens.ButtonPaddingStart else TvDimens.ButtonPaddingEnd,
                    end = TvDimens.ButtonPaddingEnd,
                )
            ),
    ) {
        if (iconContent != null) {
            iconContent(content)
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(TvDimens.ButtonIconSize),
            )
        }
        Text(
            text = text,
            style = textStyle,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
