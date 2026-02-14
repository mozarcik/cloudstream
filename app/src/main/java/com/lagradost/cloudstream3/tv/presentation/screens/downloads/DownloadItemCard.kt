package com.lagradost.cloudstream3.tv.presentation.screens.downloads

import android.content.Context
import android.text.format.Formatter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.lagradost.cloudstream3.R

private const val CardFocusAnimationMs = 180
private const val ActionFocusAnimationMs = 140
private const val CardFocusedScale = 1.04f
private const val ActionFocusedScale = 1.08f

private val DownloadCardShape = RoundedCornerShape(18.dp)
private val DownloadPosterShape = RoundedCornerShape(12.dp)
private val ProgressShape = RoundedCornerShape(percent = 50)

@Composable
fun DownloadItemCard(
    item: DownloadItemUiModel,
    onCardClick: () -> Unit,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCardFocused: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val context = LocalContext.current
    val cardFocusRequester = focusRequester ?: remember { FocusRequester() }
    val playButtonFocusRequester = remember { FocusRequester() }
    val deleteButtonFocusRequester = remember { FocusRequester() }
    val canPlay = item.state is DownloadState.Downloaded

    var isCardFocused by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isCardFocused) CardFocusedScale else 1f,
        animationSpec = tween(durationMillis = CardFocusAnimationMs),
        label = "downloads_card_scale"
    )

    Surface(
        onClick = onCardClick,
        shape = ClickableSurfaceDefaults.shape(shape = DownloadCardShape),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)),
                shape = DownloadCardShape
            )
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevation = 8.dp,
                elevationColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.84f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = modifier
            .height(236.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .focusRequester(cardFocusRequester)
            .focusProperties {
                right = playButtonFocusRequester
            }
            .onFocusChanged { state ->
                isCardFocused = state.isFocused
                if (state.isFocused) {
                    onCardFocused()
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DownloadPoster(
                posterUrl = item.posterUrl,
                title = item.title,
                modifier = Modifier
                    .width(132.dp)
                    .fillMaxHeight()
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = buildMetaLine(item = item, context = context),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                item.description
                    ?.takeIf { it.isNotBlank() }
                    ?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                if (item.state is DownloadState.Downloading) {
                    DownloadProgress(
                        progress = item.state.progress,
                        downloadedBytes = item.state.downloadedBytes,
                        totalBytes = item.state.totalBytes,
                        context = context
                    )
                }

                Spacer(modifier = Modifier.weight(1f, fill = true))

                DownloadActionsRow(
                    canPlay = canPlay,
                    onPlayClick = onPlayClick,
                    onDeleteClick = onDeleteClick,
                    cardFocusRequester = cardFocusRequester,
                    playButtonFocusRequester = playButtonFocusRequester,
                    deleteButtonFocusRequester = deleteButtonFocusRequester
                )
            }
        }
    }
}

@Composable
private fun DownloadPoster(
    posterUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(posterUrl)
            .crossfade(true)
            .build(),
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .shadow(10.dp, DownloadPosterShape)
            .clip(DownloadPosterShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
    ) {
        if (painter.state is coil.compose.AsyncImagePainter.State.Success) {
            SubcomposeAsyncImageContent()
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f))
            )
        }
    }
}

@Composable
private fun DownloadProgress(
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long?,
    context: Context,
) {
    val normalized = progress.coerceIn(0f, 1f)
    val percent = (normalized * 100f).toInt()
    val downloadedLabel = formatBytes(context, downloadedBytes)
    val totalLabel = totalBytes?.takeIf { it > 0L }?.let { formatBytes(context, it) } ?: "?"

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(ProgressShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.17f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(normalized)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        Text(
            text = "$percent% • $downloadedLabel / $totalLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DownloadActionsRow(
    canPlay: Boolean,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    cardFocusRequester: FocusRequester,
    playButtonFocusRequester: FocusRequester,
    deleteButtonFocusRequester: FocusRequester,
) {
    val playLabel = stringResource(R.string.home_play)
    val deleteLabel = stringResource(R.string.delete)

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            enabled = canPlay,
            onClick = onPlayClick,
            contentPadding = OutlinedButtonDefaults.ButtonWithIconContentPadding,
            colors = OutlinedButtonDefaults.colors(),
            modifier = Modifier
                .focusRequester(playButtonFocusRequester)
                .focusProperties {
                    left = cardFocusRequester
                    right = deleteButtonFocusRequester
                }
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(OutlinedButtonDefaults.IconSpacing))
            Text(
                text = playLabel,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                softWrap = false
            )
        }

        OutlinedButton(
            onClick = onDeleteClick,
            contentPadding = OutlinedButtonDefaults.ButtonWithIconContentPadding,
            colors = OutlinedButtonDefaults.colors(),
            modifier = Modifier
                .focusRequester(deleteButtonFocusRequester)
                .focusProperties {
                    left = playButtonFocusRequester
                }
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(OutlinedButtonDefaults.IconSpacing))
            Text(
                text = deleteLabel,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun buildMetaLine(
    item: DownloadItemUiModel,
    context: Context,
): String {
    val typeLabel = when (item.mediaType) {
        DownloadMediaType.Movie -> stringResource(R.string.movies_singular)
        DownloadMediaType.Series -> {
            val episodeLabel = stringResource(R.string.episode)
            item.episodeNumber?.let { episode -> "$episodeLabel $episode" } ?: episodeLabel
        }

        DownloadMediaType.Media -> null
    }

    val sizeLabel = when (val state = item.state) {
        is DownloadState.Downloaded -> formatBytes(context, state.fileSizeBytes)
        is DownloadState.Downloading -> {
            val sizeBytes = state.totalBytes?.takeIf { it > 0L } ?: state.downloadedBytes
            formatBytes(context, sizeBytes)
        }

        is DownloadState.Paused -> {
            val sizeBytes = state.totalBytes?.takeIf { it > 0L } ?: state.downloadedBytes
            formatBytes(context, sizeBytes)
        }

        is DownloadState.Failed -> null
    }

    val statusLabel = when (val state = item.state) {
        is DownloadState.Downloaded -> stringResource(R.string.downloaded)
        is DownloadState.Downloading -> {
            val percent = (state.progress.coerceIn(0f, 1f) * 100f).toInt()
            "${stringResource(R.string.downloading)} $percent%"
        }

        is DownloadState.Paused -> {
            val percent = (state.progress.coerceIn(0f, 1f) * 100f).toInt()
            "${stringResource(R.string.download_paused)} $percent%"
        }

        is DownloadState.Failed -> stringResource(R.string.download_failed)
    }

    return listOfNotNull(typeLabel, sizeLabel, statusLabel)
        .joinToString(" • ")
}

private fun formatBytes(context: Context, bytes: Long): String {
    return Formatter.formatShortFileSize(context, bytes.coerceAtLeast(0L))
}
