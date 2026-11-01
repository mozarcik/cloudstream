package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.preference.PreferenceManager
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.TvButton
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerIcons
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerUpNext
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.max

private const val UpNextAutoplayWindowMs = 8_000L
private const val UpNextManualWindowMs = 30_000L
private const val SeekHintVisibleMs = 1_200L

/** Seek while the controls are hidden (mockup PlayerErrors, last board): "+10 s ››" and a thin progress bar. */
@Composable
internal fun PlayerSeekHint(
    deltaMs: Long,
    exoPlayer: ExoPlayer,
    accent: Color,
    onHidden: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(deltaMs) {
        if (deltaMs != 0L) {
            delay(SeekHintVisibleMs)
            onHidden()
        }
    }
    if (deltaMs == 0L) return
    val position = exoPlayer.currentPosition.coerceAtLeast(0L)
    val duration = exoPlayer.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
    val minutesSecondsFormat = stringResource(R.string.tv_player_seek_min_sec)
    Box(modifier = modifier.fillMaxSize()) {
        Text(
            text = if (deltaMs < 0) {
                "‹‹ ${formatSeekDelta(deltaMs, minutesSecondsFormat)}"
            } else {
                "${formatSeekDelta(deltaMs, minutesSecondsFormat)} ››"
            },
            style = TvTextStyles.PlayerEpisode,
            color = TvColors.Text,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 48.dp)
                .background(TvColors.PlayerSeekBadge, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
        Text(
            text = formatPlaybackTime(position) + if (duration > 0L) " / ${formatPlaybackTime(duration)}" else "",
            style = TvTextStyles.PlayerInfo,
            color = TvColors.Text,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 10.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.25f))
                .drawBehind {
                    if (duration > 0L) {
                        drawRect(color = accent, size = Size(size.width * (position.toFloat() / duration).coerceIn(0f, 1f), size.height))
                    }
                },
        )
    }
}

/**
 * "Up next" card (mockup PlayerUpNext). With autoplay it appears for the last 8 s and counts down
 * ("Play now • 8 s"; the episode then starts when playback ends). Without autoplay it appears
 * for the last 30 s and stays: "Play next episode" / "Watch credits".
 */
@Composable
internal fun PlayerUpNextLayer(
    upNext: TvPlayerUpNext,
    exoPlayer: ExoPlayer,
    dismissed: Boolean,
    suppressed: Boolean,
    onVisibleChanged: (Boolean) -> Unit,
    onPlayNow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val autoplay = remember {
        PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(context.getString(R.string.autoplay_next_key), true)
    }
    val window = if (autoplay) UpNextAutoplayWindowMs else UpNextManualWindowMs
    val remainingMs by produceState(Long.MAX_VALUE, exoPlayer, window) {
        while (true) {
            val duration = exoPlayer.duration
            value = if (duration != C.TIME_UNSET && duration > 0L && exoPlayer.currentPosition * 2 >= duration) {
                duration - exoPlayer.currentPosition
            } else {
                Long.MAX_VALUE
            }
            delay(500)
        }
    }
    val visible = !dismissed && !suppressed && remainingMs <= window
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(visible) {
        onVisibleChanged(visible)
        if (visible) repeat(20) {
            if (focusRequester.requestFocus()) return@LaunchedEffect
            delay(50)
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { onVisibleChanged(false) } }
    if (!visible) return

    val seconds = max(1, ceil(remainingMs / 1000f).toInt())
    Row(
        horizontalArrangement = Arrangement.spacedBy(TvDimens.PlayerCardPadding),
        modifier = modifier
            .width(TvDimens.PlayerCardWidth)
            .background(TvColors.PlayerCard, RoundedCornerShape(TvDimens.PlayerCardRadius + 0.dp))
            .padding(TvDimens.PlayerCardPadding),
    ) {
        AsyncImage(
            model = upNext.posterUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(TvDimens.PlayerCardPosterWidth, TvDimens.PlayerCardPosterHeight)
                .clip(RoundedCornerShape(7.dp))
                .background(TvColors.PosterPlaceholder),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.tv_player_up_next).uppercase(),
                style = TvTextStyles.Eyebrow,
                color = TvColors.FeaturedEyebrow,
            )
            Text(
                text = listOfNotNull(upNext.label, upNext.title).joinToString(" • "),
                style = TvTextStyles.EpisodeTitle,
                color = TvColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            if (!upNext.description.isNullOrBlank()) {
                Text(
                    text = upNext.description,
                    style = TvTextStyles.PlayerPanelMeta.copy(lineHeight = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)),
                    color = TvColors.Text2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 10.dp)) {
                if (autoplay) {
                    TvButton(
                        text = stringResource(R.string.tv_player_up_next_play_now, seconds),
                        onClick = onPlayNow,
                        height = 32.dp,
                        iconContent = { color -> CountdownRing(fraction = seconds / 8f, color = color) },
                        modifier = Modifier.focusRequester(focusRequester).weight(1f, fill = false),
                    )
                    TvButton(text = stringResource(R.string.cancel), onClick = onDismiss, height = 32.dp)
                } else {
                    TvButton(
                        text = stringResource(R.string.tv_player_up_next_play_next),
                        onClick = onPlayNow,
                        height = 32.dp,
                        modifier = Modifier.focusRequester(focusRequester),
                    )
                    TvButton(text = stringResource(R.string.tv_player_up_next_watch_credits), onClick = onDismiss, height = 32.dp)
                }
            }
        }
    }
}

@Composable
private fun CountdownRing(fraction: Float, color: Color) {
    Canvas(modifier = Modifier.size(15.dp)) {
        val stroke = 1.75.dp.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = color.copy(alpha = 0.18f), startAngle = 0f, sweepAngle = 360f, useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize, style = Stroke(stroke),
        )
        drawArc(
            color = color, startAngle = -90f, sweepAngle = 360f * fraction.coerceIn(0f, 1f), useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset), size = arcSize,
            style = Stroke(stroke, cap = StrokeCap.Butt),
        )
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.40f, size.height * 0.33f)
            lineTo(size.width * 0.40f, size.height * 0.67f)
            lineTo(size.width * 0.67f, size.height * 0.5f)
            close()
        }
        drawPath(path, color = color, style = Fill)
    }
}
