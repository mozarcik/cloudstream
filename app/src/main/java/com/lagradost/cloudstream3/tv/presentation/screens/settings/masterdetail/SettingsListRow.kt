package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

private val FocusInEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
private val FocusOutEasing = LinearOutSlowInEasing
private val FocusPivot = TransformOrigin(TvDimens.SettingsItemFocusPivotX, 0.5f)

/** Widget at the right edge of a [SettingsListRow] (all of them live in a 32 dp box). */
internal sealed interface SettingsRowWidget {
    data object None : SettingsRowWidget
    data object Chevron : SettingsRowWidget
    data class Switch(val on: Boolean) : SettingsRowWidget
    data class Radio(val selected: Boolean) : SettingsRowWidget
    data class Checkbox(val checked: Boolean) : SettingsRowWidget
    class Custom(val content: @Composable (focused: Boolean) -> Unit) : SettingsRowWidget
}

/**
 * The Settings row (system TvSettings model): a pill with a 40 dp icon circle, title + summary
 * and a widget on the right. Focus / pressed scale and shadow are applied in the draw phase only
 * ([graphicsLayer]), so moving focus never re-measures the list.
 *
 * [leading] is drawn inside the circle (glyph, initials, remote icon); null means no circle.
 */
@Composable
internal fun SettingsListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    leading: (@Composable (focused: Boolean) -> Unit)? = null,
    leadingCircleColor: Color? = null,
    widget: SettingsRowWidget = SettingsRowWidget.None,
    enabled: Boolean = true,
    destructive: Boolean = false,
    /** Content below the summary (inline slider track). */
    body: (@Composable (focused: Boolean) -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val panelFocusable = LocalSettingsPanelFocusable.current
    val hasLeading = leading != null
    val twoLines = summary != null || body != null

    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> TvDimens.SettingsItemPressedScale
            focused -> TvDimens.SettingsItemFocusScale
            else -> 1f
        },
        animationSpec = tween(
            durationMillis = if (pressed) TvDimens.SettingsPressMs else TvDimens.SettingsFocusInMs,
            easing = if (focused) FocusInEasing else FocusOutEasing,
        ),
        label = "settingsRowScale",
    )

    val titleColor = when {
        focused && enabled -> if (destructive) TvColors.DangerOnFocus else TvColors.OnFocus
        !enabled -> TvColors.Disabled
        destructive -> TvColors.Danger
        else -> TvColors.Text
    }
    val summaryColor = when {
        focused && enabled -> TvColors.OnFocusSecondary
        !enabled -> TvColors.Disabled
        else -> TvColors.RowSubtitle
    }
    val circleColor = when {
        focused && enabled -> TvColors.OnFocus
        else -> leadingCircleColor ?: TvColors.FillIcon
    }
    val glyphColor = if (destructive) TvColors.Danger else TvColors.Text

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .width(TvDimens.SettingsItemWidth)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = FocusPivot
                clip = false
            }
            .background(if (focused && enabled) TvColors.Focus else Color.Transparent, TvShapes.Pill)
            .then(
                if (focused && enabled) Modifier.border(TvDimens.SettingsDp1, TvColors.Focus, TvShapes.Pill) else Modifier
            )
            .heightIn(
                min = if (hasLeading || twoLines) TvDimens.SettingsItemTwoLineMinHeight else TvDimens.SettingsItemMinHeight
            )
            .focusProperties { canFocus = panelFocusable }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = true,
                role = Role.Button,
                onClick = { if (enabled) onClick() },
            )
            .padding(
                start = if (hasLeading) TvDimens.SettingsItemPaddingStartIcon else TvDimens.SettingsItemPaddingStartText,
                end = TvDimens.SettingsItemPaddingEnd,
                top = if (hasLeading) TvDimens.SettingsItemPaddingVerticalIcon else TvDimens.SettingsItemPaddingVerticalText,
                bottom = if (hasLeading) TvDimens.SettingsItemPaddingVerticalIcon else TvDimens.SettingsItemPaddingVerticalText,
            ),
    ) {
        if (leading != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(end = TvDimens.SettingsItemIconGap)
                    .size(TvDimens.SettingsItemIconSize)
                    .background(circleColor.copy(alpha = if (enabled) circleColor.alpha else circleColor.alpha * 0.5f), CircleShape),
            ) {
                CompositionLocalProvider(LocalContentColor provides glyphColor) { leading(focused) }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TvTextStyles.SettingsItemTitle,
                color = titleColor,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = TvTextStyles.SettingsItemSummary,
                    color = summaryColor,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (body != null) {
                CompositionLocalProvider(LocalContentColor provides titleColor) { body(focused) }
            }
        }
        if (widget != SettingsRowWidget.None && widget != SettingsRowWidget.Chevron) Spacer(Modifier.width(TvDimens.SettingsItemIconGap))
        SettingsRowWidgetContent(widget = widget, focused = focused && enabled, contentColor = titleColor)
    }
}

/** Glyph inside the icon circle. */
@Composable
internal fun SettingsGlyph(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector? = null,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
) {
    val modifier = Modifier.size(TvDimens.SettingsItemGlyphSize)
    val tint = LocalContentColor.current
    when {
        imageVector != null -> Icon(imageVector, null, modifier, tint)
        painter != null -> Icon(painter, null, modifier, tint)
    }
}

@Composable
private fun SettingsRowWidgetContent(widget: SettingsRowWidget, focused: Boolean, contentColor: Color) {
    when (widget) {
        SettingsRowWidget.None -> Unit
        SettingsRowWidget.Chevron -> Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(TvDimens.SettingsDp24),
        )
        is SettingsRowWidget.Switch -> SettingsSwitch(on = widget.on, focused = focused)
        is SettingsRowWidget.Radio -> SettingsRadio(selected = widget.selected, focused = focused)
        is SettingsRowWidget.Checkbox -> SettingsCheckbox(checked = widget.checked, focused = focused)
        is SettingsRowWidget.Custom -> widget.content(focused)
    }
}

@Composable
internal fun SettingsSwitch(on: Boolean, focused: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(TvDimens.SettingsControlBox)) {
        val trackW = TvDimens.SettingsSwitchTrackWidth.toPx()
        val trackH = TvDimens.SettingsSwitchTrackHeight.toPx()
        val left = (size.width - trackW) / 2f
        val top = (size.height - trackH) / 2f
        val radius = CornerRadius(trackH / 2f)
        val stroke = TvDimens.SettingsDp2.toPx()
        val trackColor = when {
            on && focused -> TvColors.OnFocus
            on -> TvColors.ToggleOn
            focused -> TvColors.OnFocusSecondary
            else -> TvColors.RadioBorder
        }
        val thumbColor = when {
            on && focused -> TvColors.Focus
            on -> TvColors.ToggleThumb
            focused -> TvColors.OnFocusSecondary
            else -> TvColors.Text2
        }
        if (on) {
            drawRoundRect(trackColor, Offset(left, top), Size(trackW, trackH), radius)
        } else {
            drawRoundRect(
                trackColor,
                Offset(left + stroke / 2f, top + stroke / 2f),
                Size(trackW - stroke, trackH - stroke),
                CornerRadius((trackH - stroke) / 2f),
                style = Stroke(stroke),
            )
        }
        val thumbD = (if (on) 24.dp else 16.dp).toPx()
        val cx = if (on) left + trackW - trackH / 2f else left + trackH / 2f
        drawCircle(thumbColor, radius = thumbD / 2f, center = Offset(cx, size.height / 2f))
    }
}

@Composable
internal fun SettingsRadio(selected: Boolean, focused: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(TvDimens.SettingsControlBox)) {
        val ring = 18.dp.toPx()
        val stroke = TvDimens.SettingsDp2.toPx()
        val color = when {
            selected && focused -> TvColors.OnFocus
            selected -> TvColors.Selected
            focused -> TvColors.RadioBorderFocused
            else -> TvColors.RadioBorder
        }
        drawCircle(color, radius = (ring - stroke) / 2f, style = Stroke(stroke))
        if (selected) drawCircle(color, radius = 10.dp.toPx() / 2f)
    }
}

@Composable
internal fun SettingsCheckbox(checked: Boolean, focused: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(TvDimens.SettingsControlBox)) {
        val box = 18.dp.toPx()
        val left = (size.width - box) / 2f
        val top = (size.height - box) / 2f
        val radius = CornerRadius(TvDimens.SettingsDp2.toPx())
        if (checked) {
            drawRoundRect(if (focused) TvColors.OnFocus else TvColors.Selected, Offset(left, top), Size(box, box), radius)
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(left + box * 0.22f, top + box * 0.52f)
                lineTo(left + box * 0.42f, top + box * 0.71f)
                lineTo(left + box * 0.79f, top + box * 0.30f)
            }
            drawPath(
                path,
                color = if (focused) TvColors.Focus else TvColors.OnFocus,
                style = Stroke(1.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        } else {
            val stroke = TvDimens.SettingsDp2.toPx()
            drawRoundRect(
                if (focused) TvColors.RadioBorderFocused else TvColors.RadioBorder,
                Offset(left + stroke / 2f, top + stroke / 2f),
                Size(box - stroke, box - stroke),
                radius,
                style = Stroke(stroke),
            )
        }
    }
}
