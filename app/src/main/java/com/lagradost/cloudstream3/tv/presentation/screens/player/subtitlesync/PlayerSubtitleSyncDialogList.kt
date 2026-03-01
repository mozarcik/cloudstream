package com.lagradost.cloudstream3.tv.presentation.screens.player.subtitlesync

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R

@Composable
internal fun RowScope.PlayerSubtitleSyncDialogColumn(
    stateHolder: PlayerSubtitleSyncStateHolder,
    hasActiveSubtitleTrack: Boolean,
) {
    Column(
        modifier = Modifier
            .weight(PlayerSubtitleSyncTokens.ColumnWeightDialogs)
            .fillMaxHeight(),
    ) {
        Text(
            text = stringResource(R.string.subtitle_offset),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when {
            !hasActiveSubtitleTrack -> {
                PlayerSubtitleSyncEmptyState(
                    message = stringResource(R.string.tv_player_subtitle_sync_enable_subtitles),
                )
            }
            stateHolder.subtitleCues.isEmpty() -> {
                PlayerSubtitleSyncEmptyState(
                    message = stringResource(R.string.no_subtitles_loaded),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = PlayerSubtitleSyncTokens.DialogListTopPadding)
                        .heightIn(max = PlayerSubtitleSyncTokens.DialogListMaxHeight),
                    state = stateHolder.listState,
                ) {
                    itemsIndexed(
                        items = stateHolder.cueUiModels,
                        key = { index, cue -> "${cue.cue.startTimeMs}_${cue.cue.endTimeMs}_$index" },
                    ) { index, cueUi ->
                        val isActive = index == stateHolder.activeCueIndex
                        val progressFraction = cueUi.cue.progressFor(stateHolder.subtitleTimelinePositionMs)
                        var isItemFocused by remember(cueUi.cue.startTimeMs, cueUi.cue.endTimeMs, index) {
                            mutableStateOf(false)
                        }
                        ListItem(
                            selected = isActive,
                            onClick = {
                                stateHolder.syncToCue(cueUi.cue)
                            },
                            enabled = hasActiveSubtitleTrack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (index == stateHolder.initialFocusCueIndex) {
                                        Modifier.focusRequester(stateHolder.firstDialogFocusRequester)
                                    } else {
                                        Modifier
                                    }
                                )
                                .focusProperties {
                                    right = if (stateHolder.controlsEnabled) {
                                        stateHolder.firstControlFocusRequester
                                    } else {
                                        FocusRequester.Default
                                    }
                                }
                                .onFocusChanged { focusState ->
                                    stateHolder.hasDialogListFocus = focusState.hasFocus
                                    isItemFocused = focusState.isFocused
                                },
                            headlineContent = {
                                Column {
                                    Text(
                                        text = cueUi.joinedText,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(
                                        text = cueUi.timestampRange,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.alpha(0.72f),
                                    )
                                    if (isActive) {
                                        PlayerSubtitleSyncProgressBar(
                                            progress = progressFraction,
                                            isFocused = isItemFocused,
                                            modifier = Modifier.padding(top = PlayerSubtitleSyncTokens.ActiveProgressTopPadding),
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerSubtitleSyncEmptyState(
    message: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PlayerSubtitleSyncTokens.EmptyStateMinHeight)
            .padding(top = PlayerSubtitleSyncTokens.EmptyStateTopPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
}

@Composable
private fun PlayerSubtitleSyncProgressBar(
    progress: Float,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
) {
    val indicatorColor = MaterialTheme.colorScheme.primary
    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) {
            PlayerSubtitleSyncTokens.ActiveProgressHeight.value / PlayerSubtitleSyncTokens.InactiveProgressHeight.value
        } else {
            1f
        },
        animationSpec = tween(durationMillis = PlayerSubtitleSyncTokens.ProgressFocusAnimationMs),
        label = "subtitle_sync_progress_focus_scale",
    )
    val trackAlpha = if (isFocused) 0.32f else 0.18f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PlayerSubtitleSyncTokens.InactiveProgressHeight)
            .graphicsLayer {
                scaleY = focusScale
            }
            .subtitleSyncProgressTrack(
                color = indicatorColor.copy(alpha = trackAlpha),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .subtitleSyncProgressTrack(color = indicatorColor),
        )
    }
}
