package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.common.TvListRow
import com.lagradost.cloudstream3.tv.presentation.common.TvRowTrailing
import com.lagradost.cloudstream3.tv.presentation.focus.TvInitialFocusEffect
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import kotlinx.coroutines.launch

/*
 * Building blocks shared by every Settings level (mockups Settings*): the level header
 * (crumb + title), section labels, dividers, state boxes, the scrollable text block, the
 * preview card pieces and a spinner. Screens with their own content (accounts, plugins,
 * add repository) are made of these plus [TvListRow].
 */

/** 28 sp title (Decor title) and an optional 14 sp description at alpha 0.6; no breadcrumb. */
@Composable
internal fun SettingsLevelHeader(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = TvDimens.SettingsPaneStartInset + TvDimens.SettingsTextIndent,
                end = TvDimens.SettingsPaneEndPadding,
                top = TvDimens.SettingsDecorTitleTop,
                bottom = TvDimens.SettingsItemGap * 2,
            )
    ) {
        Text(
            text = title,
            style = TvTextStyles.SettingsDecorTitle,
            color = TvColors.Text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = TvTextStyles.SettingsDecorSubtitle,
                color = TvColors.Text.copy(alpha = TvDimens.SettingsPreviewInactiveAlpha),
                modifier = Modifier.padding(top = TvDimens.SettingsDp8),
            )
        }
    }
}

/** Paragraph of an info panel (plugin description, authors, error): not focusable. */
@Composable
internal fun SettingsInfoParagraph(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TvColors.Body,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TvTextStyles.SettingsInfo,
        color = color,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = TvDimens.SettingsTextIndent,
                end = TvDimens.SettingsPaneEndPadding,
                bottom = TvDimens.SettingsItemGap,
            ),
    )
}

/** Category label (11 sp caps) in a 36 dp block, text at the bottom. */
@Composable
internal fun SettingsSectionLabel(text: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.BottomStart,
        modifier = modifier
            .fillMaxWidth()
            .height(TvDimens.SettingsCategoryHeight)
            .padding(
                start = TvDimens.SettingsTextIndent,
                end = TvDimens.SettingsPaneEndPadding,
                bottom = TvDimens.SettingsDp4,
            ),
    ) {
        Text(
            text = text.uppercase(),
            style = TvTextStyles.SettingsCategory,
            color = TvColors.LabelPanel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun SettingsDivider(
    modifier: Modifier = Modifier,
    top: Dp = TvDimens.SettingsDividerGap,
    bottom: Dp = TvDimens.SettingsDividerGap,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TvDimens.SettingsHeaderHorizontalPadding)
            .padding(top = top, bottom = bottom)
            .height(TvDimens.SettingsDividerHeight)
            .background(TvColors.SettingsDivider)
    )
}

/** Non-focusable text block (title + optional message) used by the Empty state and list notes. */
@Composable
internal fun SettingsMessageBlock(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = TvDimens.SettingsTextIndent, end = TvDimens.SettingsPaneEndPadding, top = TvDimens.SettingsDp8, bottom = TvDimens.SettingsDp8),
    ) {
        Text(
            text = title,
            style = TvTextStyles.SettingsItemTitle,
            color = TvColors.Text,
        )
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                style = TvTextStyles.SettingsItemSummary,
                color = TvColors.RowSubtitle,
                modifier = Modifier.padding(top = TvDimens.SettingsDp3),
            )
        }
    }
}

/** Row used by the screens with their own content (account actions, plugin actions, form fields). */
@Composable
internal fun SettingsActionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    destructive: Boolean = false,
    widget: SettingsRowWidget = SettingsRowWidget.None,
) {
    SettingsListRow(
        title = title,
        summary = subtitle,
        onClick = onClick,
        enabled = enabled,
        destructive = destructive,
        widget = widget,
        leading = when {
            loading -> {
                { _ -> SettingsSpinner(color = androidx.tv.material3.LocalContentColor.current) }
            }
            icon != null -> {
                { _ -> SettingsGlyph(imageVector = icon) }
            }
            else -> null
        },
        modifier = modifier,
    )
}

/** Small rotating arc (action in progress). */
@Composable
internal fun SettingsSpinner(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "settings-spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing), RepeatMode.Restart),
        label = "settings-spinner-angle",
    )
    Canvas(modifier = modifier.size(TvDimens.SettingsSpinnerSize)) {
        val stroke = TvDimens.SettingsDp1_5.toPx()
        val inset = stroke / 2f
        val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
        rotate(angle) {
            drawArc(
                color = color.copy(alpha = TvDimens.SettingsSpinnerTrackAlpha),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = TvColors.ToggleOn,
                startAngle = -90f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

/** Skeleton rows of the Loading state: the layout of an item (40 dp circle + two bars) in `Tile`. */
@Composable
internal fun SettingsSkeletonRows(modifier: Modifier = Modifier) {
    val widths = remember { listOf(TvDimens.SettingsDp223, TvDimens.SettingsDp257, TvDimens.SettingsDp223, TvDimens.SettingsDp257) }
    Column(
        verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsItemGap),
        modifier = modifier.padding(start = TvDimens.SettingsPaneStartInset, top = TvDimens.SettingsDp4),
    ) {
        widths.forEach { width ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .size(TvDimens.SettingsItemWidth, TvDimens.SettingsItemTwoLineMinHeight)
                    .padding(horizontal = TvDimens.SettingsItemPaddingStartIcon),
            ) {
                Spacer(Modifier.size(TvDimens.SettingsItemIconSize).background(TvColors.Tile, CircleShape))
                Column(
                    verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsDp8),
                    modifier = Modifier.padding(start = TvDimens.SettingsItemIconGap),
                ) {
                    Spacer(Modifier.size(width / 2, TvDimens.SettingsSkeletonHeight).background(TvColors.Tile, RoundedCornerShape(TvDimens.SettingsDp3)))
                    Spacer(Modifier.size(width / 3, TvDimens.SettingsSkeletonHeight).background(TvColors.Tile, RoundedCornerShape(TvDimens.SettingsDp3)))
                }
            }
        }
    }
}

/**
 * Scrollable text (Disclaimer): one focusable block with a white ring; Up / Down scroll it
 * and a "more below" arrow is shown while there is text left.
 */
@Composable
internal fun SettingsTextBlock(
    text: String,
    modifier: Modifier = Modifier,
    requestInitialFocus: Boolean = true,
) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val requester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val panelFocusable = LocalSettingsPanelFocusable.current
    val stepPx = with(androidx.compose.ui.platform.LocalDensity.current) { TvDimens.SettingsDp64.toPx() }

    if (requestInitialFocus) {
        TvInitialFocusEffect(requester = requester)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = TvDimens.SettingsDp4)
                .focusRequester(requester)
                .onFocusChanged { focused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> if (scroll.canScrollForward) {
                            scope.launch { scroll.animateScrollBy(stepPx, tween(TvDimens.FocusAnimationMs)) }
                            true
                        } else {
                            false
                        }
                        Key.DirectionUp -> if (scroll.canScrollBackward) {
                            scope.launch { scroll.animateScrollBy(-stepPx, tween(TvDimens.FocusAnimationMs)) }
                            true
                        } else {
                            false
                        }
                        else -> false
                    }
                }
                .focusProperties { canFocus = panelFocusable }
                .focusable()
                .then(
                    if (focused) {
                        Modifier.border(
                            width = TvDimens.SettingsTextBlockRing,
                            color = TvColors.Focus,
                            shape = RoundedCornerShape(TvDimens.SettingsTextBlockRadius),
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(TvDimens.SettingsTextBlockRing)
                .verticalScroll(scroll)
                .padding(TvDimens.SettingsTextBlockPadding)
        ) {
            Text(
                text = text,
                style = TvTextStyles.Body,
                color = TvColors.Body,
            )
        }
        if (scroll.canScrollForward) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TvColors.LabelPanel,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = TvDimens.SettingsDp10, bottom = TvDimens.SettingsDp6)
                    .size(TvDimens.SettingsRowChevronSize),
            )
        }
    }
}

// ---- Preview card pieces -------------------------------------------------------------------

@Composable
internal fun SettingsPreviewHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = TvTextStyles.SettingsPreviewHeader,
        color = TvColors.SettingsPreviewText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TvDimens.SettingsPreviewRowHorizontalPadding)
            .padding(bottom = TvDimens.SettingsPreviewHeaderBottomGap),
    )
}

@Composable
internal fun SettingsPreviewSection(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = TvTextStyles.SettingsPreviewSection,
        color = TvColors.SettingsPreviewSubtext,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = TvDimens.SettingsPreviewRowHorizontalPadding,
                end = TvDimens.SettingsPreviewRowHorizontalPadding,
                top = TvDimens.SettingsDp14,
                bottom = TvDimens.SettingsDp3,
            ),
    )
}

/** Preview row: dimmed title + subtitle, 42 dp, no focus. */
@Composable
internal fun SettingsPreviewRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    height: Dp = TvDimens.SettingsPreviewRowHeight,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.SettingsDp10),
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = TvDimens.SettingsPreviewRowHorizontalPadding),
    ) {
        leading?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = TvTextStyles.SettingsPreviewRow,
                color = TvColors.SettingsPreviewText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = TvTextStyles.SettingsPreviewSubtitle,
                    color = TvColors.SettingsPreviewSubtext,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = TvDimens.SettingsDp2),
                )
            }
        }
    }
}
