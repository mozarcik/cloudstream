package com.lagradost.cloudstream3.tv.presentation.screens.player.subtitlesync

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.focus.tvFocusFill
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.formatSubtitleDelayValue
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

/** Left column of the sync panel: dialogue lines; OK on a line aligns it with "now". */
@Composable
internal fun RowScope.PlayerSubtitleSyncDialogColumn(
    stateHolder: PlayerSubtitleSyncStateHolder,
    hasActiveSubtitleTrack: Boolean,
) {
    Column(modifier = Modifier.width(TvDimens.PlayerSyncListWidth)) {
        when {
            !hasActiveSubtitleTrack -> PlayerSubtitleSyncEmptyState(
                message = stringResource(R.string.tv_player_subtitle_sync_enable_subtitles),
            )
            stateHolder.subtitleCues.isEmpty() -> PlayerSubtitleSyncEmptyState(
                message = stringResource(R.string.no_subtitles_loaded),
            )
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = PlayerSubtitleSyncTokens.DialogListMaxHeight),
                    state = stateHolder.listState,
                ) {
                    itemsIndexed(
                        items = stateHolder.cueUiModels,
                        key = { index, cue -> "${cue.cue.startTimeMs}_${cue.cue.endTimeMs}_$index" },
                    ) { index, cueUi ->
                        val isActive = index == stateHolder.activeCueIndex
                        var focused by remember { mutableStateOf(false) }
                        val interactionSource = remember { MutableInteractionSource() }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tvFocusFill(
                                    focused = focused,
                                    shape = TvShapes.PanelRow,
                                    container = if (isActive) TvColors.PlayerCueNow else androidx.compose.ui.graphics.Color.Transparent,
                                )
                                .then(
                                    if (index == stateHolder.initialFocusCueIndex) {
                                        Modifier.focusRequester(stateHolder.firstDialogFocusRequester)
                                    } else {
                                        Modifier
                                    },
                                )
                                .focusProperties {
                                    right = if (stateHolder.controlsEnabled) {
                                        stateHolder.firstControlFocusRequester
                                    } else {
                                        FocusRequester.Default
                                    }
                                }
                                .onFocusChanged { state ->
                                    stateHolder.hasDialogListFocus = state.hasFocus
                                    focused = state.isFocused
                                }
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    enabled = hasActiveSubtitleTrack,
                                    role = Role.Button,
                                ) { stateHolder.syncToCue(cueUi.cue) }
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = if (isActive && !focused) {
                                    stringResource(R.string.tv_player_sync_now) + " • " + cueUi.timestampRange
                                } else {
                                    cueUi.timestampRange
                                },
                                style = TvTextStyles.PlayerSyncTimestamp,
                                color = when {
                                    focused -> TvColors.OnFocusSecondary
                                    isActive -> TvColors.Selected
                                    else -> TvColors.LabelPanel
                                },
                                maxLines = 1,
                            )
                            Text(
                                text = cueUi.joinedText,
                                style = TvTextStyles.PlayerSyncCue,
                                color = when {
                                    focused -> TvColors.OnFocus
                                    isActive -> TvColors.Text
                                    else -> TvColors.Text2
                                },
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                            if (focused) {
                                Text(
                                    text = stringResource(
                                        R.string.tv_player_sync_align_hint,
                                        formatSubtitleDelayValue(stateHolder.playerPositionMs - cueUi.cue.startTimeMs),
                                    ),
                                    style = TvTextStyles.PlayerSyncAction,
                                    color = TvColors.PlayerCueAction,
                                    maxLines = 1,
                                    modifier = Modifier.padding(top = 5.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerSubtitleSyncEmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PlayerSubtitleSyncTokens.EmptyStateMinHeight)
            .padding(top = PlayerSubtitleSyncTokens.EmptyStateTopPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = TvTextStyles.PlayerEpisode.copy(fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color = TvColors.Meta,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
