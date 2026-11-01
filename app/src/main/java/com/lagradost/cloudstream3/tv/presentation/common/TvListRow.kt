package com.lagradost.cloudstream3.tv.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.focus.tvFocusFill
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

/** Indicator shown before the label (radio lists: Components, Sources, Sort, subtitle encoding). */
@Immutable
sealed interface TvRowLeading {
    data class Radio(val selected: Boolean) : TvRowLeading
    /** Multi-select checkbox (settings multi-select lists). */
    data class Checkbox(val checked: Boolean) : TvRowLeading
}

/** What a [TvListRow] shows at its trailing edge. */
@Immutable
sealed interface TvRowTrailing {
    data object None : TvRowTrailing
    data object Chevron : TvRowTrailing
    data class Check(val checked: Boolean = true) : TvRowTrailing
    data class Radio(val selected: Boolean) : TvRowTrailing
    data class Toggle(val on: Boolean) : TvRowTrailing
    data class Value(val text: String) : TvRowTrailing
    class Custom(val content: @Composable (focused: Boolean) -> Unit) : TvRowTrailing
}

/**
 * The one list row (settings, side panels, menus, season list):
 * optional leading icon, title + optional subtitle, trailing indicator.
 * Focus: white fill with dark text, no scale. [selected] draws the accent tint used for the
 * current season; radio/check rows express selection through the trailing indicator instead.
 */
@Composable
fun TvListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    leading: (@Composable (focused: Boolean) -> Unit)? = null,
    leadingIndicator: TvRowLeading? = null,
    trailing: TvRowTrailing = TvRowTrailing.None,
    selected: Boolean = false,
    enabled: Boolean = true,
    height: Dp = TvDimens.RowHeight,
    /** Exact [height] instead of a minimum (single-line rows that must not grow). */
    fixedHeight: Boolean = false,
    shape: Shape = TvShapes.Row,
    titleStyle: TextStyle = TvTextStyles.Row,
    titleMaxLines: Int = 1,
    supportingContent: (@Composable () -> Unit)? = null,
    /** Extra content below the subtitle (inline slider); receives the focus state. */
    bodyContent: (@Composable (focused: Boolean) -> Unit)? = null,
    horizontalPadding: Dp = TvDimens.RowHorizontalPadding,
    /** Extra start padding inside the fill (indented children, e.g. subtitles of a language group). */
    startInset: Dp = 0.dp,
    verticalPadding: Dp = 0.dp,
    gap: Dp = TvDimens.RowGap,
    trailingIconSize: Dp = TvDimens.RowTrailingIconSize,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    /** Label color while not focused / focused (destructive rows); defaults follow the theme. */
    tint: Color? = null,
    focusedTint: Color? = null,
    accent: Color = MaterialTheme.colorScheme.tertiary,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    val titleColor = when {
        focused && enabled -> focusedTint ?: colors.inverseOnSurface
        !enabled -> tint ?: TvColors.Disabled
        selected -> accent
        else -> tint ?: colors.onSurface
    }
    val subtitleColor = when {
        focused && enabled -> TvColors.OnFocusSecondary
        !enabled -> TvColors.DotSeparator
        else -> TvColors.RowSubtitle
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = verticalAlignment,
        modifier = modifier
            .fillMaxWidth()
            .then(if (fixedHeight) Modifier.height(height) else Modifier.heightIn(min = height))
            .tvFocusFill(
                focused = focused && enabled,
                shape = shape,
                container = if (selected) accent.copy(alpha = 0.14f) else Color.Transparent,
                focusedContainer = colors.inverseSurface,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(
                start = horizontalPadding + startInset,
                end = horizontalPadding,
                top = verticalPadding,
                bottom = verticalPadding,
            ),
    ) {
        when {
            leadingIndicator is TvRowLeading.Radio ->
                TvRadioIndicator(selected = leadingIndicator.selected, focused = focused)
            leadingIndicator is TvRowLeading.Checkbox ->
                TvCheckboxIndicator(checked = leadingIndicator.checked, focused = focused)
            leading != null -> CompositionLocalProvider(LocalContentColor provides titleColor) { leading(focused) }
            leadingIcon != null -> Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = titleColor,
                modifier = Modifier.size(TvDimens.RowIconSize),
            )
        }

        if (subtitle != null || supportingContent != null || bodyContent != null) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = titleStyle,
                    color = titleColor,
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
                if (supportingContent != null) {
                    supportingContent()
                } else if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = TvTextStyles.RowSubtitle,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = TvDimens.RowSubtitleTopPadding),
                    )
                }
                if (bodyContent != null) {
                    CompositionLocalProvider(LocalContentColor provides titleColor) { bodyContent(focused) }
                }
            }
        } else {
            Text(
                text = title,
                style = titleStyle,
                color = titleColor,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        TvRowTrailingContent(
            trailing = trailing,
            focused = focused,
            contentColor = titleColor,
            subtitleColor = subtitleColor,
            iconSize = trailingIconSize,
        )
    }
}

@Composable
private fun TvRowTrailingContent(
    trailing: TvRowTrailing,
    focused: Boolean,
    contentColor: Color,
    subtitleColor: Color,
    iconSize: Dp = TvDimens.RowTrailingIconSize,
) {
    when (trailing) {
        TvRowTrailing.None -> Unit
        TvRowTrailing.Chevron -> Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(iconSize),
        )
        is TvRowTrailing.Check -> if (trailing.checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(iconSize),
            )
        }
        is TvRowTrailing.Radio -> TvRadioIndicator(selected = trailing.selected, focused = focused)
        is TvRowTrailing.Toggle -> TvToggleIndicator(on = trailing.on, focused = focused)
        is TvRowTrailing.Value -> Text(
            text = trailing.text,
            style = TvTextStyles.Meta,
            color = subtitleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        is TvRowTrailing.Custom -> CompositionLocalProvider(LocalContentColor provides contentColor) {
            trailing.content(focused)
        }
    }
}

/** 16 dp radio (mockup 32 px, border 3 px, dot 14 px); sits left of the label. */
@Composable
fun TvRadioIndicator(selected: Boolean, modifier: Modifier = Modifier, focused: Boolean = false) {
    Canvas(modifier = modifier.size(TvDimens.RadioSize)) {
        val border = TvDimens.RadioBorder.toPx()
        val color = when {
            selected -> TvColors.Selected
            focused -> TvColors.RadioBorderFocused
            else -> TvColors.RadioBorder
        }
        drawCircle(color = color, radius = (size.minDimension - border) / 2f, style = Stroke(width = border))
        if (selected) {
            drawCircle(color = color, radius = TvDimens.RadioDot.toPx() / 2f)
        }
    }
}

/** 17 dp checkbox (mockup 34 px, radius 8 px, border 3 px); checked = blue fill with a dark tick. */
@Composable
fun TvCheckboxIndicator(checked: Boolean, modifier: Modifier = Modifier, focused: Boolean = false) {
    Canvas(modifier = modifier.size(TvDimens.CheckboxSize)) {
        val radius = CornerRadius(TvDimens.CheckboxRadius.toPx())
        if (checked) {
            drawRoundRect(color = TvColors.Selected, cornerRadius = radius)
            val w = size.width
            val h = size.height
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.22f, h * 0.52f)
                lineTo(w * 0.42f, h * 0.71f)
                lineTo(w * 0.79f, h * 0.30f)
            }
            drawPath(
                path = path,
                color = TvColors.OnFocus,
                style = Stroke(width = TvDimens.CheckboxTick.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
            )
        } else {
            val border = TvDimens.RadioBorder.toPx()
            drawRoundRect(
                color = if (focused) TvColors.RadioBorderFocused else TvColors.RadioBorder,
                topLeft = Offset(border / 2f, border / 2f),
                size = androidx.compose.ui.geometry.Size(size.width - border, size.height - border),
                cornerRadius = radius,
                style = Stroke(width = border),
            )
        }
    }
}

/** 36x20 dp toggle (mockup 72x40). */
@Composable
fun TvToggleIndicator(on: Boolean, modifier: Modifier = Modifier, focused: Boolean = false) {
    Canvas(modifier = modifier.size(TvDimens.ToggleWidth, TvDimens.ToggleHeight)) {
        drawRoundRect(
            color = when {
                on -> TvColors.ToggleOn
                focused -> TvColors.RadioBorder.copy(alpha = 0.45f)
                else -> TvColors.ProgressTrack
            },
            cornerRadius = CornerRadius(size.height / 2f),
        )
        val pad = TvDimens.TogglePadding.toPx()
        val thumb = TvDimens.ToggleThumb.toPx()
        drawCircle(
            color = if (!on && focused) TvColors.RadioBorderFocused else TvColors.ToggleThumb,
            radius = thumb / 2f,
            center = Offset(if (on) size.width - pad - thumb / 2f else pad + thumb / 2f, size.height / 2f),
        )
    }
}
