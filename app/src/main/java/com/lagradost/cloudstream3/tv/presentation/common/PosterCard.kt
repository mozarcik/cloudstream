package com.lagradost.cloudstream3.tv.presentation.common

import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lagradost.cloudstream3.R
import kotlin.math.abs

private const val POSTER_IMAGE_ASPECT_RATIO = 2f / 3f
private const val POSTER_FOCUS_SCALE = 1.06f
private const val POSTER_FOCUS_ANIMATION_MS = 140
private const val POSTER_BORDER_WIDTH_DP = 2f
private const val POSTER_UNFOCUSED_BADGE_ALPHA = 0.72f
private const val POSTER_FOCUS_RECT_EPSILON_PX = 2f
private const val POSTER_FOCUS_UPDATE_MIN_INTERVAL_MS = 32L

private val PosterTitleAreaHeight = 40.dp
private val PosterRatingBadgeShape = RoundedCornerShape(10.dp)
private val PosterRatingBadgeBackground = Color.Black.copy(alpha = 0.70f)

@Immutable
data class PosterFocusInfo(
    val boundsInRoot: Rect,
    val accentColor: Color,
    val isFocused: Boolean,
)

private class PosterCardLayoutState {
    var imageBoundsInRoot: Rect = Rect.Zero
    var lastLoadedDrawable: Drawable? = null
    var isFocused: Boolean = false
    var lastFocusDispatchUptimeMs: Long = 0L
}

private class AccentColorHolder(
    var color: Color,
)

private fun Rect.isApproximatelyEqualTo(
    other: Rect,
    epsilonPx: Float = POSTER_FOCUS_RECT_EPSILON_PX,
): Boolean {
    return abs(left - other.left) <= epsilonPx &&
        abs(top - other.top) <= epsilonPx &&
        abs(right - other.right) <= epsilonPx &&
        abs(bottom - other.bottom) <= epsilonPx
}

@Composable
fun PosterCard(
    model: Any,
    title: String,
    ratingLabel: String?,
    shape: Shape,
    onClick: () -> Unit,
    onFocus: (PosterFocusInfo) -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    paletteCache: PaletteCache = DefaultPosterPaletteCache,
) {
    val onFocusUpdated by rememberUpdatedState(onFocus)
    val fallbackAccentColor = MaterialTheme.colorScheme.primary
    val contentKey = remember(model) { model.toString() }
    val layoutState = remember { PosterCardLayoutState() }

    var isFocused by remember { mutableStateOf(false) }
    val accentColorHolder = remember(contentKey, fallbackAccentColor) {
        AccentColorHolder(color = paletteCache.get(contentKey) ?: fallbackAccentColor)
    }

    val focusScale by animateFloatAsState(
        targetValue = if (isFocused) POSTER_FOCUS_SCALE else 1f,
        animationSpec = if (isFocused) {
            tween(durationMillis = POSTER_FOCUS_ANIMATION_MS, easing = FastOutSlowInEasing)
        } else {
            snap()
        },
        label = "poster-focus-scale"
    )
    val ratingAlpha = if (isFocused) 1f else POSTER_UNFOCUSED_BADGE_ALPHA

    fun emitFocus(isFocusedNow: Boolean, color: Color = accentColorHolder.color) {
        val imageBounds = layoutState.imageBoundsInRoot
        if (imageBounds.width <= 0f || imageBounds.height <= 0f) {
            return
        }
        onFocusUpdated(
            PosterFocusInfo(
                boundsInRoot = imageBounds,
                accentColor = color,
                isFocused = isFocusedNow,
            )
        )
    }

    fun requestAccentResolve(
        drawable: Drawable?,
        highPriority: Boolean,
    ) {
        paletteCache.resolveFromDrawableAsync(
            contentKey = contentKey,
            drawable = drawable,
            fallbackColor = fallbackAccentColor,
            highPriority = highPriority
        ) { resolvedColor ->
            if (resolvedColor != accentColorHolder.color) {
                accentColorHolder.color = resolvedColor
            }
            if (layoutState.isFocused) {
                emitFocus(isFocusedNow = true, color = resolvedColor)
            }
        }
    }

    val onPosterImageSuccess = remember(
        paletteCache,
        contentKey,
        fallbackAccentColor
    ) {
        { drawable: Drawable? ->
            layoutState.lastLoadedDrawable = drawable
            requestAccentResolve(
                drawable = drawable,
                highPriority = layoutState.isFocused
            )
        }
    }
    val focusedBoundsReporterModifier = if (isFocused) {
        Modifier.onGloballyPositioned { coordinates ->
            val previousBounds = layoutState.imageBoundsInRoot
            val bounds = coordinates.boundsInRoot()
            layoutState.imageBoundsInRoot = bounds

            if (bounds.isApproximatelyEqualTo(previousBounds)) {
                return@onGloballyPositioned
            }

            val now = SystemClock.uptimeMillis()
            if (now - layoutState.lastFocusDispatchUptimeMs < POSTER_FOCUS_UPDATE_MIN_INTERVAL_MS) {
                return@onGloballyPositioned
            }

            layoutState.lastFocusDispatchUptimeMs = now
            emitFocus(isFocusedNow = true)
        }
    } else {
        Modifier
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = focusScale
                    scaleY = focusScale
                    transformOrigin = TransformOrigin(0.5f, 0.35f)
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(POSTER_IMAGE_ASPECT_RATIO)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(focusedBoundsReporterModifier)
            ) {
                PosterImage(
                    model = model,
                    title = title,
                    onImageSuccess = onPosterImageSuccess,
                    modifier = Modifier.fillMaxSize()
                )

                if (ratingLabel != null) {
                    PosterRatingBadge(
                        ratingLabel = ratingLabel,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .graphicsLayer { alpha = ratingAlpha }
                    )
                }

                if (isFocused) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .focusBorderOverlay(shape = shape, color = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PosterTitleAreaHeight)
                    .padding(top = 6.dp, start = 2.dp, end = 2.dp),
                contentAlignment = Alignment.TopStart
            ) {
                if (isFocused) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Surface(
            onClick = onClick,
            shape = ClickableSurfaceDefaults.shape(shape = shape),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border.None,
                pressedBorder = Border.None,
            ),
            glow = ClickableSurfaceDefaults.glow(
                glow = Glow.None,
                focusedGlow = Glow.None,
                pressedGlow = Glow.None,
            ),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                pressedContainerColor = Color.Transparent
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(POSTER_IMAGE_ASPECT_RATIO)
                .align(Alignment.TopCenter)
                .onFocusChanged { focusState ->
                    if (layoutState.isFocused == focusState.isFocused) {
                        return@onFocusChanged
                    }

                    layoutState.isFocused = focusState.isFocused
                    isFocused = focusState.isFocused

                    if (focusState.isFocused) {
                        layoutState.lastFocusDispatchUptimeMs = SystemClock.uptimeMillis()
                        val cachedAccent = paletteCache.get(contentKey)
                        if (cachedAccent != null) {
                            accentColorHolder.color = cachedAccent
                        }
                        val shouldResolveHighPriority = cachedAccent == null ||
                            (cachedAccent == fallbackAccentColor && layoutState.lastLoadedDrawable != null)
                        if (shouldResolveHighPriority) {
                            requestAccentResolve(
                                drawable = layoutState.lastLoadedDrawable,
                                highPriority = true
                            )
                        }
                    } else {
                        onFocusUpdated(
                            PosterFocusInfo(
                                boundsInRoot = layoutState.imageBoundsInRoot,
                                accentColor = accentColorHolder.color,
                                isFocused = false,
                            )
                        )
                    }
                }
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }
    }

    if (onLongClick != null) {
        // WHY: tv.material3 Surface w tym module nie obsługuje natywnego long click bez dodatkowej warstwy.
    }
}

@Composable
private fun PosterImage(
    model: Any,
    title: String,
    onImageSuccess: (Drawable?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageRequest = remember(context, model) {
        ImageRequest.Builder(context)
            .data(model)
            .crossfade(false)
            .build()
    }
    val onImageSuccessState by rememberUpdatedState(onImageSuccess)

    AsyncImage(
        model = imageRequest,
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        onSuccess = { imageState ->
            onImageSuccessState(imageState.result.drawable)
        }
    )
}

private fun Modifier.focusBorderOverlay(
    shape: Shape,
    color: Color,
): Modifier {
    return drawWithCache {
        val borderStroke = Stroke(
            width = POSTER_BORDER_WIDTH_DP.dp.toPx(),
            cap = StrokeCap.Round
        )
        val outline = shape.createOutline(size = size, layoutDirection = layoutDirection, density = this)

        onDrawBehind {
            when (outline) {
                is Outline.Rectangle -> {
                    drawRect(color = color, style = borderStroke)
                }

                is Outline.Rounded -> {
                    drawRoundRect(
                        color = color,
                        size = size,
                        cornerRadius = outline.roundRect.topLeftCornerRadius,
                        style = borderStroke
                    )
                }

                is Outline.Generic -> {
                    drawPath(path = outline.path, color = color, style = borderStroke)
                }
            }
        }
    }
}

@Composable
private fun PosterRatingBadge(
    ratingLabel: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = PosterRatingBadgeBackground,
                shape = PosterRatingBadgeShape
            )
            .padding(horizontal = 5.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.bookmark_star_24px),
                contentDescription = null,
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = ratingLabel,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
