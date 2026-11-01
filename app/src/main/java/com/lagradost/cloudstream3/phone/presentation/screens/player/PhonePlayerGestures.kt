package com.lagradost.cloudstream3.phone.presentation.screens.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.phone.presentation.theme.LocalPhoneAccent
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneColors
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneShapes
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneText
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.formatPlaybackTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// Thresholds and curves are the legacy player's (PlayerGestureHelper).
private const val MinimumSeekMs = 7_000L
private const val MinimumSwipePercentOfHeight = 2f
private const val SwipeMultiplier = 2f
private const val DoubleTapWindowMs = 200L
private const val TapMaxHoldMs = 350L
private const val DoubleTapPauseZone = 0.15f
private const val HoldSpeedDelayMs = 500L

/** The legacy gesture settings (Settings > Player), read once per player screen. */
internal data class PhoneGesturePrefs(
    val swipeVertical: Boolean,
    val swipeHorizontal: Boolean,
    val doubleTap: Boolean,
    val doubleTapPause: Boolean,
    val doubleTapSeekMs: Long,
    val holdSpeedUp: Boolean,
)

@Composable
internal fun rememberPhoneGesturePrefs(): PhoneGesturePrefs {
    val context = LocalContext.current
    return remember(context) {
        val sm = PreferenceManager.getDefaultSharedPreferences(context)
        PhoneGesturePrefs(
            swipeVertical = sm.getBoolean(context.getString(R.string.swipe_vertical_enabled_key), true),
            swipeHorizontal = sm.getBoolean(context.getString(R.string.swipe_enabled_key), true),
            doubleTap = sm.getBoolean(context.getString(R.string.double_tap_enabled_key), false),
            doubleTapPause = sm.getBoolean(context.getString(R.string.double_tap_pause_enabled_key), false),
            doubleTapSeekMs = sm.getInt(context.getString(R.string.double_tap_seek_time_key), 10).toLong() * 1000L,
            holdSpeedUp = sm.getBoolean(context.getString(R.string.speedup_key), false),
        )
    }
}

internal enum class SwipeKind { Volume, Brightness }

internal class PhoneGestureState {
    var kind by mutableStateOf<SwipeKind?>(null)
    var level by mutableStateOf(0f)
    var stamp by mutableIntStateOf(0)
    var seekPreview by mutableStateOf<String?>(null)
    var speedUp by mutableStateOf(false)
    var skipHint by mutableStateOf<String?>(null)
    fun showLevel(k: SwipeKind, value: Float) { kind = k; level = value; stamp++ }
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/**
 * Legacy player gestures on the video: vertical swipe (left half brightness, right half volume),
 * horizontal swipe seek, optional double tap seek / pause, optional hold for 2x speed, and a single tap.
 */
@Composable
internal fun Modifier.phonePlayerGestures(
    enabled: Boolean,
    player: ExoPlayer,
    prefs: PhoneGesturePrefs,
    state: PhoneGestureState,
    onSingleTap: () -> Unit,
    onSwipeStart: () -> Unit,
    onGestureEnd: (hadSwipe: Boolean) -> Unit,
): Modifier {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lastTapEnd = remember { longArrayOf(0L) }
    val pendingTap = remember { arrayOfNulls<Job>(1) }
    val audio = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    fun brightness(): Float {
        val lp = context.activity()?.window?.attributes
        if (lp != null && lp.screenBrightness >= 0f) return lp.screenBrightness
        return runCatching { Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f }.getOrDefault(0.5f)
    }
    fun setBrightness(value: Float) {
        val window = context.activity()?.window ?: return
        window.attributes = window.attributes.apply { screenBrightness = value.coerceIn(0.004f, 1f) }
    }
    fun skip(deltaMs: Long) {
        val target = (player.currentPosition + deltaMs).coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
        state.skipHint = (if (deltaMs > 0) "+" else "−") + (abs(deltaMs) / 1000) + " s"
    }

    return this.pointerInput(enabled, prefs, player) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = true)
            val w = size.width.toFloat()
            val h = size.height.toFloat()
            val start = down.position
            var last = start
            var kind: Int = -1 // 0 volume, 1 brightness, 2 time
            val startPositionMs = player.currentPosition
            val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            var volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume.toFloat()
            var light = brightness()
            var holdSpeed = false
            val previousSpeed = player.playbackParameters.speed
            var hold: Job? = null
            if (prefs.holdSpeedUp && player.isPlaying) {
                hold = scope.launch {
                    delay(HoldSpeedDelayMs)
                    if (kind == -1) { holdSpeed = true; player.setPlaybackSpeed(2f); state.speedUp = true }
                }
            }
            var newTimeMs: Long? = null
            var up: androidx.compose.ui.input.pointer.PointerInputChange? = null
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                // A child (option row scrolling, timeline drag) took the pointer: not a video gesture.
                if (change.isConsumed) break
                if (change.changedToUp()) { up = change; break }
                if (!change.pressed) break
                if (holdSpeed) continue
                val pos = change.position
                if (kind == -1) {
                    val dx = pos.x - start.x
                    val dy = pos.y - start.y
                    if (prefs.swipeVertical && abs(dy) * 100f / h > MinimumSwipePercentOfHeight) {
                        kind = if (start.x >= w / 2f) 0 else 1
                    } else if (prefs.swipeHorizontal && abs(dx) * 100f / h > MinimumSwipePercentOfHeight) {
                        kind = 2
                    }
                    if (kind != -1) { hold?.cancel(); onSwipeStart() }
                }
                when (kind) {
                    0 -> {
                        volume = (volume + (last.y - pos.y) * SwipeMultiplier / h).coerceIn(0f, 1f)
                        val step = (volume * maxVolume).roundToInt().coerceIn(0, maxVolume)
                        if (step != audio.getStreamVolume(AudioManager.STREAM_MUSIC)) audio.setStreamVolume(AudioManager.STREAM_MUSIC, step, 0)
                        state.showLevel(SwipeKind.Volume, volume)
                    }
                    1 -> {
                        light = (light + (last.y - pos.y) * SwipeMultiplier / h).coerceIn(0f, 1f)
                        setBrightness(light)
                        state.showLevel(SwipeKind.Brightness, light)
                    }
                    2 -> {
                        val duration = player.duration
                        if (duration > 0) {
                            val diffX = (pos.x - start.x) * SwipeMultiplier / w
                            val base = startPositionMs / 1000L * 1000L
                            val target = (base + (duration * diffX * diffX * (if (diffX < 0) -1 else 1)).toLong()).coerceIn(0L, duration)
                            newTimeMs = target
                            val skipMs = target - base
                            state.seekPreview = "${formatPlaybackTime(target)} [" +
                                (if (abs(skipMs) < 1000) "" else if (skipMs > 0) "+" else "-") + formatPlaybackTime(abs(skipMs)) + "]"
                        }
                    }
                }
                if (kind != -1) change.consume()
                last = pos
            }
            hold?.cancel()
            if (holdSpeed) { player.setPlaybackSpeed(previousSpeed); state.speedUp = false }
            state.seekPreview = null
            if (kind == 2) newTimeMs?.let { if (abs(it - startPositionMs) > MinimumSeekMs) player.seekTo(it) }
            val upChange = up
            if (upChange != null && kind == -1 && !holdSpeed && upChange.uptimeMillis - down.uptimeMillis < TapMaxHoldMs) {
                val x = upChange.position.x
                val anyDouble = prefs.doubleTap || prefs.doubleTapPause
                val now = System.currentTimeMillis()
                if (!anyDouble) {
                    onSingleTap()
                } else if (now - lastTapEnd[0] < DoubleTapWindowMs) {
                    pendingTap[0]?.cancel()
                    if (prefs.doubleTapPause) {
                        when {
                            x < w / 2f - DoubleTapPauseZone * w -> if (prefs.doubleTap) skip(-prefs.doubleTapSeekMs)
                            x > w / 2f + DoubleTapPauseZone * w -> if (prefs.doubleTap) skip(prefs.doubleTapSeekMs)
                            else -> if (player.playWhenReady) player.pause() else player.playWhenReady = true
                        }
                    } else if (prefs.doubleTap) {
                        skip(if (x < w / 2f) -prefs.doubleTapSeekMs else prefs.doubleTapSeekMs)
                    }
                    lastTapEnd[0] = 0L
                } else {
                    pendingTap[0]?.cancel()
                    pendingTap[0] = scope.launch { delay(DoubleTapWindowMs); onSingleTap() }
                    lastTapEnd[0] = now
                }
            }
            onGestureEnd(kind != -1)
        }
    }
}

/** Volume / brightness bar, seek preview, double-tap hint and the 2x badge. */
@Composable
internal fun PhoneGestureOverlay(state: PhoneGestureState, modifier: Modifier = Modifier) {
    LaunchedEffect(state.stamp) { if (state.kind != null) { delay(1_500); state.kind = null } }
    LaunchedEffect(state.skipHint) { if (state.skipHint != null) { delay(700); state.skipHint = null } }
    Box(modifier.fillMaxSize()) {
        state.kind?.let { kind ->
            Column(
                Modifier.align(if (kind == SwipeKind.Volume) Alignment.CenterEnd else Alignment.CenterStart).padding(horizontal = 40.dp)
                    .clip(PhoneShapes.Card).background(Color(0x99000000)).padding(vertical = 14.dp, horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(if (kind == SwipeKind.Volume) Icons.Default.VolumeUp else Icons.Default.BrightnessMedium, null, tint = PhoneColors.Text, modifier = Modifier.size(24.dp))
                Box(Modifier.padding(top = 10.dp).width(6.dp).height(120.dp).clip(PhoneShapes.Pill).background(Color(0x40FFFFFF)), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.width(6.dp).height((120 * state.level).dp).background(LocalPhoneAccent.current))
                }
            }
        }
        state.seekPreview?.let { Text(it, style = PhoneText.Level.copy(fontSize = 28.sp), color = PhoneColors.Text, modifier = Modifier.align(Alignment.Center).clip(PhoneShapes.Card).background(Color(0x99000000)).padding(horizontal = 18.dp, vertical = 10.dp)) }
        state.skipHint?.let { Text(it, style = PhoneText.Level.copy(fontSize = 24.sp), color = PhoneColors.Text, modifier = Modifier.align(Alignment.Center).clip(PhoneShapes.Card).background(Color(0x99000000)).padding(horizontal = 18.dp, vertical = 10.dp)) }
        if (state.speedUp) Text("2x", style = PhoneText.Button.copy(fontSize = 16.sp), color = PhoneColors.Text, modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp).clip(PhoneShapes.Pill).background(Color(0x99000000)).padding(horizontal = 16.dp, vertical = 8.dp))
    }
}
