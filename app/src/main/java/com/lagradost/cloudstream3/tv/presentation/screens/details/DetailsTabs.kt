package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.focus.tvFocusFill
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

internal enum class DetailsTab {
    Episodes,
    CastAndCrew,
    MoreLikeThis,
    Info,
}

@Composable
internal fun DetailsTab.label(): String = when (this) {
    DetailsTab.Episodes -> stringResource(R.string.episodes)
    DetailsTab.CastAndCrew -> stringResource(R.string.tv_details_tab_cast)
    DetailsTab.MoreLikeThis -> stringResource(R.string.tv_details_tab_more_like_this)
    DetailsTab.Info -> stringResource(R.string.details)
}

/**
 * Tabs under the action buttons. Focusing a tab switches the content immediately; the selected tab keeps an
 * accent underline while focus is elsewhere, a focused tab is a white pill (no scale).
 * [onEnterContent] runs on Down and returns true when it moved focus itself.
 */
@Composable
internal fun DetailsTabsRow(
    tabs: List<DetailsTab>,
    selected: DetailsTab,
    requesters: Map<DetailsTab, FocusRequester>,
    upRequester: FocusRequester,
    onTabFocused: (DetailsTab) -> Unit,
    onEnterContent: (DetailsTab) -> Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(TvDimens.DetailsTabGap),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        tabs.forEach { tab ->
            DetailsTabItem(
                tab = tab,
                selected = tab == selected,
                requester = requesters.getValue(tab),
                upRequester = upRequester,
                onFocused = { onTabFocused(tab) },
                onEnterContent = { onEnterContent(tab) },
            )
        }
    }
}

@Composable
private fun DetailsTabItem(
    tab: DetailsTab,
    selected: Boolean,
    requester: FocusRequester,
    upRequester: FocusRequester,
    onFocused: () -> Unit,
    onEnterContent: () -> Boolean,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    val accent = colors.primary
    val textColor = when {
        focused -> colors.inverseOnSurface
        selected -> colors.onSurface
        else -> colors.onSurfaceVariant
    }
    Text(
        text = tab.label(),
        style = TvTextStyles.Tab,
        color = textColor,
        maxLines = 1,
        modifier = Modifier
            .height(TvDimens.DetailsTabHeight)
            .focusRequester(requester)
            .focusProperties { up = upRequester }
            .onFocusChanged { state -> if (state.isFocused) onFocused() }
            .onPreviewKeyEvent { event ->
                event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown && onEnterContent()
            }
            .testTag("details_tab_${tab.name}")
            .tvFocusFill(focused = focused, shape = TvShapes.Pill, focusedContainer = colors.inverseSurface)
            .drawBehind {
                if (selected && !focused) {
                    val inset = TvDimens.DetailsTabHorizontalPadding.toPx()
                    val thickness = TvDimens.DetailsTabUnderlineHeight.toPx()
                    drawRoundRect(
                        color = accent,
                        topLeft = Offset(inset, size.height - TvDimens.DetailsTabUnderlineBottom.toPx() - thickness),
                        size = Size(size.width - inset * 2f, thickness),
                        cornerRadius = CornerRadius(thickness / 2f),
                    )
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onFocused,
            )
            .padding(horizontal = TvDimens.DetailsTabHorizontalPadding)
            .wrapContentHeight(Alignment.CenterVertically),
    )
}
