package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * 24x24 icons copied from the Player mockups (SVG paths), so controls and panels match 1:1.
 * Stroke icons use a 2 px round stroke unless noted; the tint comes from the `Icon` composable.
 */
internal object PlayerIcons {
    private fun fmt(value: Float) = String.format(Locale.US, "%s", value)

    private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float): String {
        val (x2, y2) = fmt(x + w) to fmt(y + h)
        return "M${fmt(x + r)} ${fmt(y)}H${fmt(x + w - r)}A${fmt(r)} ${fmt(r)} 0 0 1 $x2 ${fmt(y + r)}" +
            "V${fmt(y + h - r)}A${fmt(r)} ${fmt(r)} 0 0 1 ${fmt(x + w - r)} ${y2}H${fmt(x + r)}" +
            "A${fmt(r)} ${fmt(r)} 0 0 1 ${fmt(x)} ${fmt(y + h - r)}V${fmt(y + r)}" +
            "A${fmt(r)} ${fmt(r)} 0 0 1 ${fmt(x + r)} ${fmt(y)}Z"
    }

    private fun circle(cx: Float, cy: Float, r: Float): String =
        "M${fmt(cx - r)} ${fmt(cy)}a${fmt(r)} ${fmt(r)} 0 1 0 ${fmt(r * 2)} 0a${fmt(r)} ${fmt(r)} 0 1 0 ${fmt(-r * 2)} 0Z"

    private fun stroke(name: String, width: Float = 2f, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = width,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun fill(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d -> addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black)) }
        }.build()

    val Pause: ImageVector by lazy { fill("PlayerPause", rect(6f, 4.5f, 4f, 15f, 1f), rect(14f, 4.5f, 4f, 15f, 1f)) }
    val Play: ImageVector by lazy { fill("PlayerPlay", "M7 4.5v15l13-7.5z") }
    val Restart: ImageVector by lazy { stroke("PlayerRestart", 2f, "M4 12a8 8 0 1 0 2.3-5.7M4 4v5h5") }
    val Video: ImageVector by lazy { stroke("PlayerVideo", 2f, rect(3f, 6f, 13f, 12f, 2f), "m16 10 5-3v10l-5-3") }
    val Sources: ImageVector by lazy { stroke("PlayerSources", 2f, "M4 6h16M4 12h16M4 18h10") }
    val Next: ImageVector by lazy { fill("PlayerNext", "M5 5v14l10-7zM17 5h2.5v14H17z") }
    val Subtitles: ImageVector by lazy {
        stroke("PlayerSubtitles", 2f, rect(3f, 5f, 18f, 14f, 2f), "M7 12h4M13 12h4M7 15h7")
    }
    val Sync: ImageVector by lazy { stroke("PlayerSync", 2f, circle(12f, 13f, 8f), "M12 9v4l3 2M10 2h4") }
    val Audio: ImageVector by lazy { stroke("PlayerAudio", 2f, "M4 9v6h4l5 4V5L8 9zM17 9a4 4 0 0 1 0 6") }
    val Aspect: ImageVector by lazy { stroke("PlayerAspect", 2f, "M4 9V5h4M20 9V5h-4M4 15v4h4M20 15v4h-4") }

    val Encoding: ImageVector by lazy { stroke("PlayerEncoding", 2f, "M4 7V5h16v2M9 19h6M12 5v14") }
    val Download: ImageVector by lazy { stroke("PlayerDownload", 2f, "M12 4v11m-5-5 5 5 5-5M5 20h14") }
    val CloudDownload: ImageVector by lazy {
        stroke("PlayerCloudDownload", 2f, "M7 18a5 5 0 0 1-.5-10A6 6 0 0 1 18 9a4 4 0 0 1-.5 9M12 11v8m-3-3 3 3 3-3")
    }
    val FirstAvailable: ImageVector by lazy { stroke("PlayerFirstAvailable", 2.2f, "m2 12 5 5 9-10M10 15l2 2 9-10") }
    val ChevronRight: ImageVector by lazy { stroke("PlayerChevronRight", 2.4f, "m9 5 7 7-7 7") }
    val ChevronLeft: ImageVector by lazy { stroke("PlayerChevronLeft", 2.4f, "m15 5-7 7 7 7") }
    val ChevronUp: ImageVector by lazy { stroke("PlayerChevronUp", 2.4f, "m5 15 7-7 7 7") }
    val ChevronDown: ImageVector by lazy { stroke("PlayerChevronDown", 2.4f, "m5 9 7 7 7-7") }
    val Error: ImageVector by lazy { stroke("PlayerError", 2f, circle(12f, 12f, 9f), "M12 7v6M12 16.5v.5") }
    val Check: ImageVector by lazy { stroke("PlayerCheck", 2.8f, "m5 12 5 5 9-10") }
    val Lock: ImageVector by lazy { stroke("PlayerLock", 2f, rect(5f, 11f, 14f, 10f, 2f), "M8 11V8a4 4 0 0 1 8 0v3") }
    val Search: ImageVector by lazy { stroke("PlayerSearch", 2.2f, circle(11f, 11f, 7f), "m20 20-3.5-3.5") }
    val Globe: ImageVector by lazy {
        stroke("PlayerGlobe", 1.8f, circle(12f, 12f, 9f), "M3 12h18M12 3c3 3.2 3 14.8 0 18M12 3c-3 3.2-3 14.8 0 18")
    }
    val Edit: ImageVector by lazy { stroke("PlayerEdit", 2f, "M4 20h4l10-10-4-4L4 16zM13 7l4 4") }
    val Retry: ImageVector by lazy { stroke("PlayerRetry", 2f, "M4 12a8 8 0 1 0 2.3-5.7M4 4v5h5") }
}
