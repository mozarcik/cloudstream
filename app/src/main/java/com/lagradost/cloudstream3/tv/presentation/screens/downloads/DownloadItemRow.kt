package com.lagradost.cloudstream3.tv.presentation.screens.downloads

import android.content.Context
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.TvButton
import com.lagradost.cloudstream3.tv.presentation.common.TvProgressBar
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

private val DownloadThumbShape = RoundedCornerShape(TvDimens.DownloadThumbRadius)

/**
 * One download (movie or episode), a fixed-height row of the Downloads list (mockup Downloads).
 * OK opens the title details; Right enters the actions (Play only for finished files, Delete),
 * Left returns to the row. The actions column is always reserved, so the row never changes width.
 * Focus: translucent fill + ring (row), white fill without scale (action buttons).
 */
@Composable
fun DownloadItemRow(
    item: DownloadItemUiModel,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRowFocused: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val playRequester = remember { FocusRequester() }
    val deleteRequester = remember { FocusRequester() }
    val canPlay = item.state is DownloadState.Downloaded
    var hasFocus by remember { mutableStateOf(false) }
    val state = item.state

    Row(
        horizontalArrangement = Arrangement.spacedBy(TvDimens.DownloadRowContentGap),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(TvDimens.DownloadRowHeight)
            .focusRequester(focusRequester)
            .focusProperties { right = if (canPlay) playRequester else deleteRequester }
            .onFocusChanged { focusState ->
                hasFocus = focusState.hasFocus
                if (focusState.hasFocus) onRowFocused()
            }
            .drawRowFocus(visible = hasFocus)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = TvDimens.DownloadRowHorizontalPadding),
    ) {
        AsyncImage(
            model = item.backdropUrl?.takeIf { it.isNotBlank() } ?: item.posterUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(TvDimens.DownloadThumbWidth)
                .height(TvDimens.DownloadThumbHeight)
                .clip(DownloadThumbShape)
                .background(TvColors.PosterPlaceholder),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = TvTextStyles.DownloadTitle,
                color = TvColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildMetaLine(item = item, context = context),
                style = TvTextStyles.DownloadMeta,
                color = when {
                    state is DownloadState.Failed -> TvColors.ErrorText
                    hasFocus -> TvColors.Meta
                    else -> TvColors.Text2
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = TvDimens.DownloadMetaTopGap),
            )
            when (state) {
                is DownloadState.Downloading -> DownloadProgress(
                    progress = state.progress,
                    barColor = TvColors.Selected,
                    statusColor = TvColors.Meta,
                    status = buildProgressStatus(item, context),
                )

                is DownloadState.Paused -> DownloadProgress(
                    progress = state.progress,
                    barColor = TvColors.LabelPanel,
                    statusColor = TvColors.Text2,
                    status = buildProgressStatus(item, context),
                )

                is DownloadState.Downloaded -> item.description
                    ?.takeIf { it.isNotBlank() }
                    ?.let { description ->
                        Text(
                            text = description,
                            style = TvTextStyles.DownloadDescription,
                            color = if (hasFocus) TvColors.Text2 else TvColors.LabelPanel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = TvDimens.DownloadDescriptionTopGap),
                        )
                    }

                is DownloadState.Failed -> Unit
            }
        }

        // Reserved even when empty: the row keeps its width while the actions come and go.
        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier.width(TvDimens.DownloadActionsWidth),
        ) {
            // Always composed (so the Right target exists at once); shown and focusable with the row.
            run {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TvDimens.DownloadActionsGap),
                    modifier = Modifier.graphicsLayer { alpha = if (hasFocus) 1f else 0f },
                ) {
                    if (canPlay) {
                        TvButton(
                            text = stringResource(R.string.home_play),
                            onClick = onPlayClick,
                            icon = Icons.Default.PlayArrow,
                            height = TvDimens.DownloadActionHeight,
                            modifier = Modifier
                                .focusRequester(playRequester)
                                .focusProperties {
                                    canFocus = hasFocus
                                    left = focusRequester
                                    right = deleteRequester
                                },
                        )
                    }
                    TvButton(
                        text = stringResource(R.string.delete),
                        onClick = onDeleteClick,
                        icon = Icons.Default.DeleteOutline,
                        height = TvDimens.DownloadActionHeight,
                        modifier = Modifier
                            .focusRequester(deleteRequester)
                            .focusProperties {
                                canFocus = hasFocus
                                left = if (canPlay) playRequester else focusRequester
                                right = FocusRequester.Cancel
                            },
                    )
                }
            }
        }
    }
}

/** Row fill (white 7%) and a ring drawn outside the bounds while the row or its actions have focus. */
private fun Modifier.drawRowFocus(visible: Boolean): Modifier = if (!visible) this else drawBehind {
    val radius = TvDimens.DownloadRowRadius.toPx()
    drawRoundRect(color = TvColors.FillRow, cornerRadius = CornerRadius(radius))
    val stroke = TvDimens.DownloadRowRingWidth.toPx()
    drawRoundRect(
        color = TvColors.Focus,
        topLeft = Offset(-stroke / 2f, -stroke / 2f),
        size = Size(size.width + stroke, size.height + stroke),
        cornerRadius = CornerRadius(radius + stroke / 2f),
        style = Stroke(width = stroke),
    )
}

@Composable
private fun DownloadProgress(
    progress: Float,
    barColor: Color,
    statusColor: Color,
    status: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.DownloadProgressGap),
        modifier = Modifier.padding(top = TvDimens.DownloadProgressTopGap),
    ) {
        TvProgressBar(
            progress = progress.coerceIn(0f, 1f),
            accent = barColor,
            track = TvColors.BorderChip,
            height = TvDimens.DownloadProgressHeight,
            modifier = Modifier.width(TvDimens.DownloadProgressWidth),
        )
        Text(
            text = status,
            style = TvTextStyles.Meta.copy(fontWeight = FontWeight.SemiBold),
            color = statusColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun buildMetaLine(
    item: DownloadItemUiModel,
    context: Context,
): String {
    val typeLabel = when (item.mediaType) {
        DownloadMediaType.Movie -> stringResource(R.string.movies_singular)
        DownloadMediaType.Series -> {
            val seasonLabel = stringResource(R.string.season)
            val episodeLabel = stringResource(R.string.episode)
            when {
                item.seasonNumber != null && item.episodeNumber != null ->
                    "$seasonLabel ${item.seasonNumber} $episodeLabel ${item.episodeNumber}"

                item.seasonNumber != null -> "$seasonLabel ${item.seasonNumber}"
                item.episodeNumber != null -> "$episodeLabel ${item.episodeNumber}"
                else -> null
            }
        }

        DownloadMediaType.Media -> null
    }

    return when (val state = item.state) {
        is DownloadState.Downloaded -> listOfNotNull(
            typeLabel,
            formatBytes(context, state.fileSizeBytes),
            stringResource(R.string.downloaded),
        )

        // The progress line below carries the status; here the type and the source.
        is DownloadState.Downloading -> listOfNotNull(
            typeLabel,
            item.apiName.takeIf { it.isNotBlank() },
            state.speedBytesPerSec?.takeIf { it > 0L }?.let {
                stringResource(R.string.tv_downloads_speed_format, formatBytes(context, it))
            },
            state.etaSeconds?.takeIf { it > 0L }?.let {
                stringResource(R.string.tv_downloads_time_left_format, formatEta(it))
            },
        )

        is DownloadState.Paused -> listOfNotNull(
            typeLabel,
            item.apiName.takeIf { it.isNotBlank() },
        )

        is DownloadState.Failed -> listOfNotNull(
            stringResource(R.string.download_failed),
            state.errorMessage?.takeIf { it.isNotBlank() },
        )
    }.joinToString(" • ")
}

/** "Downloading 42% • 1.2 GB / 2.8 GB" next to the bar (speed and ETA live on the meta line). */
@Composable
internal fun buildProgressStatus(item: DownloadItemUiModel, context: Context): String {
    return when (val state = item.state) {
        is DownloadState.Downloading -> {
            val percent = (state.progress.coerceIn(0f, 1f) * 100f).toInt()
            listOfNotNull(
                "${stringResource(R.string.downloading)} $percent%",
                sizeProgress(context, state.downloadedBytes, state.totalBytes),
            ).joinToString(" • ")
        }

        is DownloadState.Paused -> {
            val percent = (state.progress.coerceIn(0f, 1f) * 100f).toInt()
            listOfNotNull(
                "${stringResource(R.string.download_paused)} $percent%",
                sizeProgress(context, state.downloadedBytes, state.totalBytes),
            ).joinToString(" • ")
        }

        else -> ""
    }
}

private fun sizeProgress(context: Context, downloadedBytes: Long, totalBytes: Long?): String {
    val total = totalBytes?.takeIf { it > 0L }?.let { formatBytes(context, it) } ?: "?"
    return "${formatBytes(context, downloadedBytes)} / $total"
}

@Composable
private fun formatEta(totalSeconds: Long): String {
    val hours = (totalSeconds / 3600).toInt()
    val minutes = ((totalSeconds % 3600) / 60).toInt()
    val seconds = (totalSeconds % 60).toInt()
    return when {
        hours > 0 -> stringResource(R.string.download_time_left_hour_min_sec_format, hours, minutes, seconds)
        minutes > 0 -> stringResource(R.string.download_time_left_min_sec_format, minutes, seconds)
        else -> stringResource(R.string.download_time_left_sec_format, seconds)
    }
}

private fun formatBytes(context: Context, bytes: Long): String {
    return Formatter.formatShortFileSize(context, bytes.coerceAtLeast(0L))
}
