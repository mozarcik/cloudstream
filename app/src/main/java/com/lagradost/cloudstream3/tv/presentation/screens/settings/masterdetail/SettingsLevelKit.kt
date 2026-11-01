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

/** Crumb ("Settings › Player"), 24 sp title and an optional description. */
@Composable
internal fun SettingsLevelHeader(
    title: String,
    modifier: Modifier = Modifier,
    crumb: String = LocalSettingsBreadcrumb.current,
    description: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TvDimens.SettingsHeaderHorizontalPadding)
            .padding(bottom = TvDimens.SettingsHeaderBottomGap)
    ) {
        if (crumb.isNotEmpty()) {
            Text(
                text = crumb,
                style = TvTextStyles.SettingsCrumb,
                color = TvColors.LabelPanel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = title,
            style = TvTextStyles.Level,
            color = TvColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = if (crumb.isNotEmpty()) TvDimens.SettingsDp3 else TvDimens.SettingsDp0),
        )
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = TvTextStyles.RowSubtitle,
                color = TvColors.RowSubtitle,
                modifier = Modifier.padding(top = TvDimens.SettingsDp4),
            )
        }
    }
}

/** Uppercase section label between groups of rows. */
@Composable
internal fun SettingsSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = TvTextStyles.PanelGroupHeader,
        color = TvColors.LabelPanel,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = TvDimens.SettingsHeaderHorizontalPadding,
                end = TvDimens.SettingsHeaderHorizontalPadding,
                top = TvDimens.SettingsSectionTop,
                bottom = TvDimens.SettingsSectionBottom,
            ),
    )
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
            .padding(horizontal = TvDimens.SettingsHeaderHorizontalPadding, vertical = TvDimens.SettingsDp7),
    ) {
        Text(
            text = title,
            style = TvTextStyles.RowPanel,
            color = TvColors.Text,
        )
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                style = TvTextStyles.RowSubtitle,
                color = TvColors.RowSubtitle,
                modifier = Modifier.padding(top = TvDimens.SettingsDp3),
            )
        }
    }
}

/** Error box (red tint) with an optional title, used for load / login errors. */
@Composable
internal fun SettingsStateBox(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TvDimens.SettingsDp7, vertical = TvDimens.SettingsDp3)
            .background(TvColors.ErrorFill, RoundedCornerShape(TvDimens.SettingsStateBoxRadius))
            .padding(TvDimens.SettingsStateBoxPadding),
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = TvTextStyles.SearchErrorRow,
                color = TvColors.ErrorText,
            )
        }
        Text(
            text = message,
            style = TvTextStyles.SearchErrorRow,
            color = if (title.isNullOrBlank()) TvColors.ErrorText else TvColors.ErrorText.copy(alpha = TvDimens.SettingsStateMessageAlpha),
            modifier = Modifier.padding(top = if (title.isNullOrBlank()) TvDimens.SettingsDp0 else TvDimens.SettingsDp3),
        )
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
    trailing: TvRowTrailing = TvRowTrailing.None,
    height: Dp = TvDimens.RowHeightLarge,
) {
    TvListRow(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        enabled = enabled,
        height = height,
        horizontalPadding = TvDimens.SettingsRowHorizontalPadding,
        gap = TvDimens.SettingsRowGap,
        trailing = trailing,
        trailingIconSize = TvDimens.SettingsRowChevronSize,
        tint = if (destructive) TvColors.Danger else null,
        focusedTint = if (destructive) TvColors.DangerOnFocus else null,
        leading = when {
            loading -> {
                { _ -> SettingsSpinner(color = androidx.tv.material3.LocalContentColor.current) }
            }
            icon != null -> {
                { _ ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(TvDimens.SettingsRowChevronSize),
                    )
                }
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

/** Skeleton rows of the Loading state (mockup SettingsStates: 4 bars of different widths). */
@Composable
internal fun SettingsSkeletonRows(modifier: Modifier = Modifier) {
    val widths = remember { listOf(TvDimens.SettingsDp223, TvDimens.SettingsDp309, TvDimens.SettingsDp257, TvDimens.SettingsDp291) }
    Column(modifier = modifier) {
        widths.forEach { width ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TvDimens.RowHeightLarge)
                    .padding(horizontal = TvDimens.SettingsRowHorizontalPadding),
                contentAlignment = Alignment.CenterStart,
            ) {
                Spacer(
                    modifier = Modifier
                        .size(width, TvDimens.SettingsSkeletonHeight)
                        .background(TvColors.Tile, RoundedCornerShape(TvDimens.SettingsDp3))
                )
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

/** Rounded square with 1-3 letters (accounts, plugins without an icon). */
@Composable
internal fun SettingsInitialsTile(
    initials: String,
    size: Dp,
    radius: Dp,
    modifier: Modifier = Modifier,
    background: Color = TvColors.Tile,
    contentColor: Color = TvColors.Meta,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(background, RoundedCornerShape(radius)),
    ) {
        Text(
            text = initials,
            style = TvTextStyles.SettingsTile,
            color = contentColor,
            maxLines = 1,
        )
    }
}

internal val SettingsCardShape = RoundedCornerShape(TvDimens.SettingsCardRadius)
