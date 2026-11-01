package com.lagradost.cloudstream3.tv.presentation.screens.player.subtitlesync

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.focus.tvFocusFill
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerIcons
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.formatSubtitleDelayValue
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

/** Right column of the sync panel (mockup PlayerSync): value, +/- steps, custom value, reset. */
@Composable
internal fun RowScope.PlayerSubtitleSyncControlsColumn(
    stateHolder: PlayerSubtitleSyncStateHolder,
    onCustomValueRequested: () -> Unit,
) {
    val enabled = stateHolder.controlsEnabled
    val toDialogs = Modifier.focusProperties { left = stateHolder.firstDialogFocusRequester }

    Column(modifier = Modifier.weight(1f).padding(top = 4.dp)) {
        Text(
            text = stringResource(R.string.subtitle_offset_title).uppercase(),
            style = TvTextStyles.SettingsLabelSmall,
            color = TvColors.LabelPanel,
        )
        Text(
            text = formatSubtitleDelayValue(stateHolder.subtitleDelayMs),
            style = TvTextStyles.PlayerSyncValue,
            color = TvColors.Text,
            modifier = Modifier.padding(top = 3.dp),
        )
        Text(
            text = stringResource(R.string.tv_player_subtitle_sync_controls_hint),
            style = TvTextStyles.PlayerPanelOption.copy(fontSize = androidx.compose.ui.unit.TextUnit(10.5f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = TvColors.Text2,
            modifier = Modifier.padding(top = 15.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
        ) {
            listOf(R.string.tv_player_subtitle_sync_controls_too_late, R.string.tv_player_subtitle_sync_controls_too_early).forEach { label ->
                Text(
                    text = stringResource(label),
                    style = TvTextStyles.PlayerSyncAction,
                    color = TvColors.Meta,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(top = 7.dp)) {
            SyncButton(
                text = "−1.0 s", outlined = true, enabled = enabled,
                onClick = { stateHolder.updateSubtitleDelay(stateHolder.subtitleDelayMs - PlayerSubtitleSyncTokens.LargeStepMs) },
                modifier = Modifier.weight(1f).focusRequester(stateHolder.minusLargeStepFocusRequester).then(toDialogs),
            )
            SyncButton(
                text = "+1.0 s", outlined = true, enabled = enabled,
                onClick = { stateHolder.updateSubtitleDelay(stateHolder.subtitleDelayMs + PlayerSubtitleSyncTokens.LargeStepMs) },
                modifier = Modifier.weight(1f).focusRequester(stateHolder.firstControlFocusRequester),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            SyncButton(
                text = "−100 ms", outlined = false, enabled = enabled,
                onClick = { stateHolder.updateSubtitleDelay(stateHolder.subtitleDelayMs - PlayerSubtitleSyncTokens.SmallStepMs) },
                modifier = Modifier.weight(1f).focusRequester(stateHolder.minusSmallStepFocusRequester).then(toDialogs),
            )
            SyncButton(
                text = "+100 ms", outlined = false, enabled = enabled,
                onClick = { stateHolder.updateSubtitleDelay(stateHolder.subtitleDelayMs + PlayerSubtitleSyncTokens.SmallStepMs) },
                modifier = Modifier.weight(1f),
            )
        }
        SyncButton(
            text = stringResource(R.string.tv_player_sync_custom_value),
            icon = PlayerIcons.Edit, outlined = false, enabled = enabled,
            onClick = onCustomValueRequested,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).then(toDialogs),
        )
        SyncButton(
            text = "${stringResource(R.string.reset_btn)} (0 ms)", outlined = true, enabled = enabled,
            onClick = { stateHolder.updateSubtitleDelay(0L) },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).then(toDialogs),
        )
    }
}

/** 32 dp pill; outlined = 1 dp border, filled = translucent fill; white fill on focus. */
@Composable
private fun SyncButton(
    text: String,
    outlined: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val content = if (focused) TvColors.OnFocus else TvColors.Text
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(32.dp)
            .then(if (outlined && !focused) Modifier.border(1.dp, TvColors.SyncButtonBorder, TvShapes.Pill) else Modifier)
            .tvFocusFill(
                focused = focused,
                shape = TvShapes.Pill,
                container = if (outlined) androidx.compose.ui.graphics.Color.Transparent else TvColors.SyncButtonFill,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(12.dp))
        }
        Text(text = text, style = TvTextStyles.Button, color = content, maxLines = 1)
    }
}
