package com.lagradost.cloudstream3.phone.presentation.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.preference.PreferenceManager
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.phone.presentation.common.CsButton
import com.lagradost.cloudstream3.phone.presentation.theme.LocalPhoneAccent
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneColors
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneText
import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerUpNext
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.max

private const val AutoplayWindowMs = 8_000L
private const val ManualWindowMs = 30_000L

/**
 * "Up next" card, the same timing as TV: with autoplay it shows for the last 8 s and counts down, otherwise
 * for the last 30 s with "Play next episode" / "Watch credits".
 */
@Composable
internal fun PhoneUpNextCard(
    upNext: TvPlayerUpNext,
    exoPlayer: ExoPlayer,
    dismissed: Boolean,
    suppressed: Boolean,
    onVisibleChanged: (Boolean) -> Unit,
    onPlayNow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val autoplay = remember { PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.autoplay_next_key), true) }
    val window = if (autoplay) AutoplayWindowMs else ManualWindowMs
    val remainingMs by produceState(Long.MAX_VALUE, exoPlayer, window) {
        while (true) {
            val duration = exoPlayer.duration
            value = if (duration != C.TIME_UNSET && duration > 0L && exoPlayer.currentPosition * 2 >= duration) duration - exoPlayer.currentPosition else Long.MAX_VALUE
            delay(500)
        }
    }
    val visible = !dismissed && !suppressed && remainingMs <= window
    LaunchedEffect(visible) { onVisibleChanged(visible) }
    DisposableEffect(Unit) { onDispose { onVisibleChanged(false) } }
    if (!visible) return

    val seconds = max(1, ceil(remainingMs / 1000f).toInt())
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.widthIn(max = 420.dp).background(Color(0xF5171A21), RoundedCornerShape(20.dp)).padding(12.dp),
    ) {
        AsyncImage(upNext.posterUri, null, contentScale = ContentScale.Crop, modifier = Modifier.size(64.dp, 96.dp).clip(RoundedCornerShape(10.dp)).background(PhoneColors.Placeholder))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.tv_player_up_next).uppercase(), style = PhoneText.Kicker, color = LocalPhoneAccent.current)
            Text(listOfNotNull(upNext.label, upNext.title).joinToString(" • "), style = PhoneText.Row.copy(fontSize = 15.sp), color = PhoneColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            if (!upNext.description.isNullOrBlank()) Text(upNext.description, style = PhoneText.Meta.copy(fontSize = 12.sp), color = PhoneColors.Text2, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (autoplay) {
                    CsButton(stringResource(R.string.tv_player_up_next_play_now, seconds), onPlayNow, primary = true, height = 40.dp)
                    CsButton(stringResource(R.string.cancel), onDismiss, height = 40.dp)
                } else {
                    CsButton(stringResource(R.string.tv_player_up_next_play_next), onPlayNow, primary = true, height = 40.dp)
                    CsButton(stringResource(R.string.tv_player_up_next_watch_credits), onDismiss, height = 40.dp)
                }
            }
        }
    }
}
