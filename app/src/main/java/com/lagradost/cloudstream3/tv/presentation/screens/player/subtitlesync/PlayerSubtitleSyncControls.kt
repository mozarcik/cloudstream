package com.lagradost.cloudstream3.tv.presentation.screens.player.subtitlesync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R

@Composable
internal fun RowScope.PlayerSubtitleSyncControlsColumn(
    stateHolder: PlayerSubtitleSyncStateHolder,
) {
    Column(
        modifier = Modifier
            .weight(PlayerSubtitleSyncTokens.ColumnWeightControls)
            .fillMaxHeight(),
    ) {
        Text(
            text = stringResource(R.string.subtitle_offset_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = formatSubtitleDelayLabel(stateHolder.subtitleDelayMs),
            modifier = Modifier.padding(
                top = PlayerSubtitleSyncTokens.DelayValueTopPadding,
                bottom = PlayerSubtitleSyncTokens.DelayValueBottomPadding,
            ),
            style = MaterialTheme.typography.headlineMedium,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(PlayerSubtitleSyncTokens.ControlsButtonsSpacing),
        ) {
            OutlinedButton(
                onClick = {
                    stateHolder.updateSubtitleDelay(
                        stateHolder.subtitleDelayMs - PlayerSubtitleSyncTokens.LargeStepMs,
                    )
                },
                enabled = stateHolder.controlsEnabled,
                contentPadding = PlayerSubtitleSyncTokens.ButtonsContentPadding,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(stateHolder.minusLargeStepFocusRequester)
                    .focusProperties {
                        left = stateHolder.firstDialogFocusRequester
                    },
            ) {
                Text(
                    text = "-1.0 s",
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            OutlinedButton(
                onClick = {
                    stateHolder.updateSubtitleDelay(
                        stateHolder.subtitleDelayMs + PlayerSubtitleSyncTokens.LargeStepMs,
                    )
                },
                enabled = stateHolder.controlsEnabled,
                contentPadding = PlayerSubtitleSyncTokens.ButtonsContentPadding,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(stateHolder.firstControlFocusRequester)
                    .focusProperties {
                        left = stateHolder.minusLargeStepFocusRequester
                    },
            ) {
                Text(
                    text = "+1.0 s",
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = PlayerSubtitleSyncTokens.ControlsButtonsSpacing),
            horizontalArrangement = Arrangement.spacedBy(PlayerSubtitleSyncTokens.ControlsButtonsSpacing),
        ) {
            Button(
                onClick = {
                    stateHolder.updateSubtitleDelay(
                        stateHolder.subtitleDelayMs - PlayerSubtitleSyncTokens.SmallStepMs,
                    )
                },
                enabled = stateHolder.controlsEnabled,
                contentPadding = PlayerSubtitleSyncTokens.ButtonsContentPadding,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(stateHolder.minusSmallStepFocusRequester)
                    .focusProperties {
                        left = stateHolder.firstDialogFocusRequester
                    },
            ) {
                Text(
                    text = "-100 ms",
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Button(
                onClick = {
                    stateHolder.updateSubtitleDelay(
                        stateHolder.subtitleDelayMs + PlayerSubtitleSyncTokens.SmallStepMs,
                    )
                },
                enabled = stateHolder.controlsEnabled,
                contentPadding = PlayerSubtitleSyncTokens.ButtonsContentPadding,
                modifier = Modifier
                    .weight(1f)
                    .focusProperties {
                        left = stateHolder.minusSmallStepFocusRequester
                    },
            ) {
                Text(
                    text = "+100 ms",
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }

        OutlinedButton(
            onClick = { stateHolder.updateSubtitleDelay(0L) },
            enabled = stateHolder.controlsEnabled,
            modifier = Modifier
                .padding(top = PlayerSubtitleSyncTokens.ControlsButtonsSpacing)
                .fillMaxWidth()
                .focusProperties {
                    left = stateHolder.firstDialogFocusRequester
                },
        ) {
            Text(
                text = "${stringResource(R.string.reset_btn)} (0 ms)",
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}
