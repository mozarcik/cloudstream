package com.lagradost.cloudstream3.tv.presentation.common

import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.asDrawable
import coil3.compose.AsyncImage
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.focus.tvCardFocus
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import kotlin.math.abs

private const val POSTER_IMAGE_ASPECT_RATIO = 2f / 3f
private const val POSTER_UNFOCUSED_BADGE_ALPHA = 0.72f
private const val POSTER_FOCUS_RECT_EPSILON_PX = 2f
private const val POSTER_FOCUS_UPDATE_MIN_INTERVAL_MS = 32L

private val PosterRatingBadgeShape = RoundedCornerShape(TvDimens.BadgeRadius)
private val PosterRatingBadgeBackground = Color.Black.copy(alpha = 0.70f)

@Immutable
data class PosterFocusInfo(
    val boundsInRoot: Rect,
    val accentColor: Color,
    val isFocused: Boolean,
)

private class PosterCardLayoutState {
    /** Bounds are read lazily (only while focused): reading them on every placement is costly while scrolling. */
    var coordinates: LayoutCoordinates? = null
    val imageBoundsInRoot: Rect
        get() = coordinates?.takeIf { it.isAttached }?.boundsInRoot() ?: Rect.Zero
    var lastDispatchedBounds: Rect = Rect.Zero
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
    focusableTestTag: String? = null,
    ringCornerRadius: Dp = TvDimens.CardRadius,
    /** The mockup shows the bare card; title (below) and rating badge are opt-in. */
    showTitle: Boolean = false,
    showRating: Boolean = false,
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

    fun emitFocus(isFocusedNow: Boolean, color: Color = accentColorHolder.color) {
        val imageBounds = layoutState.imageBoundsInRoot
        if (imageBounds.width <= 0f || imageBounds.height <= 0f) {
            return
        }
        layoutState.lastDispatchedBounds = imageBounds
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

    // Always attached so the modifier chain never changes on focus. Bounds are tracked
    // on every placement (cheap, keeps imageBoundsInRoot current); dispatch is focused-only.
    val focusedBoundsReporterModifier = Modifier.onGloballyPositioned { coordinates ->
        layoutState.coordinates = coordinates

        if (!layoutState.isFocused) {
            return@onGloballyPositioned
        }

        val bounds = coordinates.boundsInRoot()
        if (bounds.isApproximatelyEqualTo(layoutState.lastDispatchedBounds)) {
            return@onGloballyPositioned
        }

        val now = SystemClock.uptimeMillis()
        if (now - layoutState.lastFocusDispatchUptimeMs < POSTER_FOCUS_UPDATE_MIN_INTERVAL_MS) {
            return@onGloballyPositioned
        }

        layoutState.lastFocusDispatchUptimeMs = now
        emitFocus(isFocusedNow = true)
    }

    val onFocusChangedModifier = Modifier.onFocusChanged { focusState ->
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

    // One node is both the focus target and the visual: ring + scale on the poster itself.
    val poster: @Composable (Modifier) -> Unit = { posterModifier ->
        Box(
            modifier = posterModifier
                .fillMaxWidth()
                .aspectRatio(POSTER_IMAGE_ASPECT_RATIO)
                .then(
                    focusableTestTag?.let { testTag -> Modifier.testTag(testTag) } ?: Modifier
                )
                .then(onFocusChangedModifier)
                // Ring is drawn outside the poster, so it must come before the clip.
                .tvCardFocus(focused = isFocused, cornerRadius = ringCornerRadius)
                .clip(shape)
                .background(TvColors.PosterPlaceholder)
                .then(focusedBoundsReporterModifier)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
        ) {
            var imageFailed by remember(model) { mutableStateOf(false) }
            PosterImage(
                model = model,
                title = title,
                onError = { imageFailed = true },
                modifier = Modifier.fillMaxSize()
            )
            if (imageFailed) {
                Text(
                    text = title,
                    style = TvTextStyles.PosterPlaceholderTitle,
                    color = TvColors.Text2,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(TvDimens.PosterPlaceholderPadding),
                )
            }

            if (showRating && ratingLabel != null) {
                PosterRatingBadge(
                    ratingLabel = ratingLabel,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(TvDimens.BadgeMargin)
                        .graphicsLayer {
                            alpha = if (isFocused) 1f else POSTER_UNFOCUSED_BADGE_ALPHA
                        }
                )
            }
        }
    }

    if (showTitle) {
        Column(modifier = modifier) {
            poster(Modifier)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TvDimens.PosterTitleAreaHeight)
                    .padding(
                        top = TvDimens.PosterTitleTopPadding,
                        start = TvDimens.PosterTitleHorizontalPadding,
                        end = TvDimens.PosterTitleHorizontalPadding,
                    ),
                contentAlignment = Alignment.TopStart
            ) {
                if (isFocused) {
                    Text(
                        text = title,
                        style = TvTextStyles.Meta,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    } else {
        poster(modifier)
    }
}

@Composable
private fun PosterImage(
    model: Any,
    title: String,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val imageRequest = remember(context, model) {
        ImageRequest.Builder(context)
            .data(model)
            .crossfade(false)
            .build()
    }

    AsyncImage(
        model = imageRequest,
        contentDescription = title,
        contentScale = ContentScale.Crop,
        onError = { onError() },
        modifier = modifier,
    )
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
            .padding(horizontal = TvDimens.BadgeHorizontalPadding, vertical = TvDimens.BadgeVerticalPadding)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.bookmark_star_24px),
                contentDescription = null,
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(TvDimens.BadgeIconSize)
            )
            Text(
                text = ratingLabel,
                style = TvTextStyles.Caption,
                maxLines = 1,
                modifier = Modifier.padding(start = TvDimens.BadgeIconGap)
            )
        }
    }
}
