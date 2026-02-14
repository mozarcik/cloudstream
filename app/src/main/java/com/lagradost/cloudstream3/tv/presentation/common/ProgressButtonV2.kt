package com.lagradost.cloudstream3.tv.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import com.lagradost.cloudstream3.tv.presentation.utils.Green300

private const val ProgressButtonFocusAnimationMs = 180
private const val ProgressButtonFocusedScale = 1.1f

private data class ProgressButtonClip(
    val scaledWidth: Float,
    val scaledHeight: Float,
    val backgroundLeft: Float,
    val backgroundTop: Float,
)

@Composable
internal fun ProgressButtonV2(
    action: ActionIconSpec,
    isFocused: Boolean,
    progressFraction: Float,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(ProgressButtonFocusAnimationMs),
        label = "download_progress_v2"
    )
    val progressScale by animateFloatAsState(
        targetValue = if (isFocused) ProgressButtonFocusedScale else 1f,
        animationSpec = tween(ProgressButtonFocusAnimationMs),
        label = "download_progress_v2_scale"
    )
    // In tv-material3 default OutlinedButton focused container is onSurface.
    val focusOverlayColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = if (isFocused) 0.42f else 0.28f
    )
    val contentPadding = if (isFocused) {
        OutlinedButtonDefaults.ButtonWithIconContentPadding
    } else {
        OutlinedButtonDefaults.ContentPadding
    }

    var buttonWidthPx by remember { mutableFloatStateOf(0f) }
    var buttonHeightPx by remember { mutableFloatStateOf(0f) }
    val clip = remember(buttonWidthPx, buttonHeightPx, progressScale) {
        calculateProgressButtonClip(
            width = buttonWidthPx,
            height = buttonHeightPx,
            scale = progressScale
        )
    }

    OutlinedButton(
        onClick = onClick,
        contentPadding = contentPadding,
        colors = OutlinedButtonDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            pressedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        interactionSource = interactionSource,
        modifier = modifier
            .onSizeChanged { size ->
                buttonWidthPx = size.width.toFloat()
                buttonHeightPx = size.height.toFloat()
            }
            .drawBehind {
                val buttonShapePath = createScaledCirclePath(
                    clip = clip,
                    layoutDirection = layoutDirection,
                    density = this
                )

                withTransform({
                    translate(
                        left = clip.backgroundLeft,
                        top = clip.backgroundTop
                    )
                }) {
                    clipPath(buttonShapePath) {
                        // Base layer: raw progress fill.
                        drawRect(
                            color = Green300,
                            size = Size(
                                width = clip.scaledWidth * animatedProgress,
                                height = clip.scaledHeight
                            )
                        )
                        // Top layer: translucent focused-container tint for readability.
                        drawRect(
                            color = focusOverlayColor,
                            size = Size(
                                width = clip.scaledWidth,
                                height = clip.scaledHeight
                            )
                        )
                    }
                }
            }
    ) {
        ActionIconContent(
            icon = action.icon,
            label = action.label,
            isFocused = isFocused,
        )
    }
}

private fun calculateProgressButtonClip(
    width: Float,
    height: Float,
    scale: Float,
): ProgressButtonClip {
    if (width <= 0f || height <= 0f) {
        return ProgressButtonClip(
            scaledWidth = 0f,
            scaledHeight = 0f,
            backgroundLeft = 0f,
            backgroundTop = 0f,
        )
    }

    val scaledWidth = width * scale
    val scaledHeight = height * scale

    return ProgressButtonClip(
        scaledWidth = scaledWidth,
        scaledHeight = scaledHeight,
        backgroundLeft = -((scaledWidth - width) / 2f),
        backgroundTop = -((scaledHeight - height) / 2f),
    )
}

private fun createScaledCirclePath(
    clip: ProgressButtonClip,
    layoutDirection: LayoutDirection,
    density: Density,
): Path {
    val outline = CircleShape.createOutline(
        size = Size(width = clip.scaledWidth, height = clip.scaledHeight),
        layoutDirection = layoutDirection,
        density = density
    )

    return when (outline) {
        is Outline.Generic -> outline.path
        is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
        is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
    }
}
