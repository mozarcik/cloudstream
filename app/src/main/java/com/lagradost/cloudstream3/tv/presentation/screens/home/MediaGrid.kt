/*
 * CloudStream TV - Media Grid Component
 * Displays media items (movies/series) in a grid layout
 */

package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.isEpisodeBased
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamCardShape
import kotlinx.coroutines.delay

private const val MEDIA_GRID_COLUMNS = 6
private const val FOCUSED_CARD_SCALE = 1.1f
private const val CARD_ANIMATION_MS = 180
private val PosterFallbackColors = listOf(
    Color(0xFF2E3440),
    Color(0xFF3B4252),
    Color(0xFF434C5E),
    Color(0xFF4C566A),
)

/**
 * Grid component displaying media items (movies/TV series)
 * Task 5.1: Paging support with LazyPagingItems
 * Task 5.2: Loading states with shimmer and progress indicators
 *
 * @param pagingItems Paging items to display
 * @param onMediaClick Callback when media item is clicked
 * @param focusRequester Focus requester for first item
 * @param modifier Modifier for the grid container
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaGrid(
    pagingItems: LazyPagingItems<MediaItemCompat>,
    onMediaClick: (MediaItemCompat, Int, String) -> Unit,
    onOpenFeedMenu: () -> Unit,
    onFirstRowFocusChanged: (Boolean) -> Unit,
    onGridItemFocused: () -> Unit,
    breadcrumbFocusRequester: FocusRequester,
    focusContextKey: String,
    isFeedMenuOpen: Boolean,
    externalFocusRequestToken: Int,
    preferredFocusIndex: Int,
    preferredFocusItemKey: String?,
    focusRequester: FocusRequester,
    gridState: LazyGridState,
    modifier: Modifier = Modifier
) {
    val itemFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    var lastFocusedIndex by rememberSaveable(focusContextKey) { mutableIntStateOf(-1) }
    var lastClickedIndex by rememberSaveable(focusContextKey) { mutableIntStateOf(-1) }
    var lastClickedItemKey by rememberSaveable(focusContextKey) { mutableStateOf<String?>(null) }
    var restoreTargetIndex by remember { mutableIntStateOf(0) }
    var focusRestoreToken by remember { mutableIntStateOf(0) }
    var lastHandledExternalFocusToken by remember { mutableIntStateOf(0) }
    var wasFeedMenuOpen by remember { mutableStateOf(isFeedMenuOpen) }

    LaunchedEffect(focusContextKey) {
        focusRestoreToken = 0
        restoreTargetIndex = 0
        onFirstRowFocusChanged(true)
    }

    LaunchedEffect(isFeedMenuOpen) {
        when {
            isFeedMenuOpen && !wasFeedMenuOpen -> {
                restoreTargetIndex = if (lastFocusedIndex >= 0) lastFocusedIndex else 0
            }
            !isFeedMenuOpen && wasFeedMenuOpen -> {
                focusRestoreToken += 1
            }
        }
        wasFeedMenuOpen = isFeedMenuOpen
    }

    LaunchedEffect(focusRestoreToken) {
        if (focusRestoreToken == 0 || pagingItems.itemCount == 0) return@LaunchedEffect

        val targetIndex = restoreTargetIndex.coerceIn(0, pagingItems.itemCount - 1)
        val isTargetVisible = gridState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
        if (!isTargetVisible) {
            gridState.scrollToItem(targetIndex)
        }

        repeat(24) {
            val targetRequester = itemFocusRequesters[targetIndex]
            if (targetRequester != null) {
                val focused = targetRequester.requestFocus()
                if (focused) {
                    return@LaunchedEffect
                }
            }
            delay(16)
        }

        // Fallback when target card is not yet composed.
        focusRequester.requestFocus()
    }

    LaunchedEffect(externalFocusRequestToken, pagingItems.itemCount) {
        if (
            externalFocusRequestToken == 0 ||
            externalFocusRequestToken <= lastHandledExternalFocusToken ||
            pagingItems.itemCount == 0
        ) {
            return@LaunchedEffect
        }

        val indexFromClickedItem = lastClickedItemKey?.let { clickedKey ->
            pagingItems.itemSnapshotList.items.indexOfFirst { it.toFocusAnchorKey() == clickedKey }
                .takeIf { it >= 0 }
        }
        val indexFromPreferredItem = preferredFocusItemKey?.let { preferredKey ->
            pagingItems.itemSnapshotList.items.indexOfFirst { it.toFocusAnchorKey() == preferredKey }
                .takeIf { it >= 0 }
        }

        val requestedIndex = when {
            indexFromPreferredItem != null -> indexFromPreferredItem
            preferredFocusIndex >= 0 -> preferredFocusIndex
            indexFromClickedItem != null -> indexFromClickedItem
            lastClickedIndex >= 0 -> lastClickedIndex
            lastFocusedIndex >= 0 -> lastFocusedIndex
            else -> 0
        }

        // Wait for data to reach the requested index instead of coercing to the first row.
        if (requestedIndex >= pagingItems.itemCount) {
            return@LaunchedEffect
        }

        val targetIndex = requestedIndex.coerceIn(0, pagingItems.itemCount - 1)
        val isTargetVisible = gridState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
        if (!isTargetVisible) {
            gridState.scrollToItem(targetIndex)
        }

        var focused = false
        for (attempt in 0 until 24) {
            val targetRequester = itemFocusRequesters[targetIndex]
            if (targetRequester != null) {
                focused = targetRequester.requestFocus()
                if (focused) {
                    break
                }
            }
            if (attempt < 23) {
                delay(16)
            }
        }

        if (!focused) {
            focusRequester.requestFocus()
        }
        lastHandledExternalFocusToken = externalFocusRequestToken
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(MEDIA_GRID_COLUMNS),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        // Task 5.1: Paging items
        items(
            count = pagingItems.itemCount,
            key = { index ->
                pagingItems[index]?.let { item ->
                    "${item.id}_${item.apiName}_${item.url}_$index"
                } ?: "grid_placeholder_$index"
            }
        ) { index ->
            val item = pagingItems[index]
            if (item != null) {
                HomeMediaGridCard(
                    item = item,
                    index = index,
                    onMediaClick = onMediaClick,
                    onOpenFeedMenu = onOpenFeedMenu,
                    onFocused = { focusedIndex ->
                        lastFocusedIndex = focusedIndex
                        onGridItemFocused()
                        onFirstRowFocusChanged(focusedIndex < MEDIA_GRID_COLUMNS)
                    },
                    onClicked = { clickedIndex, clickedItem ->
                        lastClickedIndex = clickedIndex
                        lastClickedItemKey = clickedItem.toFocusAnchorKey()
                    },
                    breadcrumbFocusRequester = breadcrumbFocusRequester,
                    onFocusRequesterReady = { cardIndex, requester ->
                        itemFocusRequesters[cardIndex] = requester
                    },
                    onFocusRequesterDisposed = { cardIndex ->
                        itemFocusRequesters.remove(cardIndex)
                    },
                    focusRequester = focusRequester
                )
            } else {
                // Task 5.2: Placeholder for loading items
                ShimmerCard(Modifier.aspectRatio(2f / 3f))
            }
        }

        // Task 5.2: Loading indicator when appending next page
        if (pagingItems.loadState.append is LoadState.Loading) {
            item(span = { GridItemSpan(MEDIA_GRID_COLUMNS) }) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ShimmerCard(Modifier.aspectRatio(2f / 3f))
                }
            }
        }

        // Task 5.3: Error state - just show shimmer for now
        if (pagingItems.loadState.append is LoadState.Error) {
            item(span = { GridItemSpan(MEDIA_GRID_COLUMNS) }) {
                // Placeholder for error
                ShimmerCard(Modifier.aspectRatio(2f / 3f))
            }
        }
    }
}

@Composable
private fun HomeMediaGridCard(
    item: MediaItemCompat,
    index: Int,
    onMediaClick: (MediaItemCompat, Int, String) -> Unit,
    onOpenFeedMenu: () -> Unit,
    onFocused: (Int) -> Unit,
    onClicked: (Int, MediaItemCompat) -> Unit,
    breadcrumbFocusRequester: FocusRequester,
    onFocusRequesterReady: (Int, FocusRequester) -> Unit,
    onFocusRequesterDisposed: (Int) -> Unit,
    focusRequester: FocusRequester,
) {
    var isFocused by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val interLight = remember { FontFamily(Font(R.font.inter_light, FontWeight.Light)) }
    val itemFocusRequester = remember(index) {
        if (index == 0) focusRequester else FocusRequester()
    }

    DisposableEffect(index, itemFocusRequester) {
        onFocusRequesterReady(index, itemFocusRequester)
        onDispose {
            onFocusRequesterDisposed(index)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) FOCUSED_CARD_SCALE else 1f,
        animationSpec = tween(
            durationMillis = CARD_ANIMATION_MS,
            easing = FastOutSlowInEasing
        ),
        label = "grid_card_scale"
    )
    val shadowElevationDp by animateDpAsState(
        targetValue = if (isFocused) 18.dp else 0.dp,
        animationSpec = tween(
            durationMillis = 140,
            easing = LinearOutSlowInEasing
        ),
        label = "grid_card_shadow"
    )

    val ratingValue = item.score?.toStringNull(minScore = 0.1, maxScore = 10, decimals = 1)
        ?: stringResource(R.string.tv_grid_rating_unknown_short)
    val metadataText = item.yearOrNull()?.let { year ->
        stringResource(R.string.tv_grid_metadata_rating_year, ratingValue, year)
    } ?: stringResource(R.string.tv_grid_metadata_rating_only, ratingValue)
    val typeBadge = if (item.isSeriesLike()) {
        stringResource(R.string.tv_grid_badge_series)
    } else {
        stringResource(R.string.tv_grid_badge_movie)
    }
    val posterFallbackColor = remember(item.id, item.apiName, item.url) {
        PosterFallbackColors[(item.toFocusAnchorKey().hashCode().and(Int.MAX_VALUE)) % PosterFallbackColors.size]
    }

    Surface(
        onClick = {
            onClicked(index, item)
            onMediaClick(item, index, item.toFocusAnchorKey())
        },
        shape = ClickableSurfaceDefaults.shape(shape = CloudStreamCardShape),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(0.dp, Color.Transparent),
                shape = CloudStreamCardShape
            )
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        ),
        modifier = Modifier
            .aspectRatio(2f / 3f)
            .zIndex(if (isFocused) 1f else 0f)
            .onPreviewKeyEvent { keyEvent ->
                if (
                    index % MEDIA_GRID_COLUMNS == 0 &&
                    keyEvent.key == Key.DirectionLeft &&
                    keyEvent.type == KeyEventType.KeyDown
                ) {
                    onOpenFeedMenu()
                    true
                } else {
                    false
                }
            }
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onFocused(index)
                }
            }
            .focusProperties {
                if (index < MEDIA_GRID_COLUMNS) {
                    up = breadcrumbFocusRequester
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin.Center
                shadowElevation = with(density) { shadowElevationDp.toPx() }
                spotShadowColor = Color.White
                ambientShadowColor = Color.White
                shape = CloudStreamCardShape
                clip = false
            }
            .focusRequester(itemFocusRequester)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.posterUri.takeIf { it.isNotBlank() })
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    ShimmerCard(modifier = Modifier.fillMaxSize())
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(posterFallbackColor)
                    )
                },
                success = {
                    SubcomposeAsyncImageContent()
                }
            )

            AnimatedVisibility(
                visible = isFocused,
                enter = fadeIn(animationSpec = tween(170)),
                exit = fadeOut(animationSpec = tween(120)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.38f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Black.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
            }

            AnimatedVisibility(
                visible = isFocused,
                enter = fadeIn(animationSpec = tween(170)),
                exit = fadeOut(animationSpec = tween(120)),
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = interLight,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Light,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.9f),
                                offset = Offset(0f, 1.5f),
                                blurRadius = 4f
                            )
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = metadataText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = interLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Light,
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.85f),
                                offset = Offset(0f, 1f),
                                blurRadius = 3f
                            )
                        ),
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isFocused,
                enter = fadeIn(animationSpec = tween(170)),
                exit = fadeOut(animationSpec = tween(120)),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(bottomEnd = 8.dp))
                        .background(Color.Black.copy(alpha = 0.62f))
                ) {
                    Text(
                        text = typeBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = interLight,
                            fontSize = 8.sp,
                            letterSpacing = 0.4.sp,
                            fontWeight = FontWeight.Light
                        ),
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isFocused,
                enter = fadeIn(animationSpec = tween(120)),
                exit = fadeOut(animationSpec = tween(90))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.9f)),
                            shape = CloudStreamCardShape
                        )
                )
            }
        }
    }
}

private fun MediaItemCompat.isSeriesLike(): Boolean {
    return this is MediaItemCompat.TvSeries || this.type?.isEpisodeBased() == true
}

private fun MediaItemCompat.yearOrNull(): Int? {
    return when (this) {
        is MediaItemCompat.Movie -> this.year
        is MediaItemCompat.TvSeries -> this.year
        is MediaItemCompat.Other -> null
    }
}

private fun MediaItemCompat.toFocusAnchorKey(): String {
    return "${this.id}|${this.apiName}|${this.url}"
}
