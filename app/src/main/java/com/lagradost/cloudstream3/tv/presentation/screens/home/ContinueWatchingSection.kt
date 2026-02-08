package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
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
import kotlinx.coroutines.launch

private val ContinueWatchingSectionShape = RoundedCornerShape(20.dp)
private val ContinueWatchingRowHeight = 228.dp
private val ContinueWatchingInfoPanelHeight = 216.dp
private val ContinueWatchingInfoToRowGap = 12.dp
private val ContinueWatchingCardWidth = 128.dp
private val ContinueWatchingRowSpacing = 12.dp
private const val ContinueWatchingFocusedCardScale = 1.1f
private const val ContinueWatchingCardAnimationMs = 180
private val ContinueWatchingPosterFallbackColors = listOf(
    Color(0xFF2E3440),
    Color(0xFF3B4252),
    Color(0xFF434C5E),
    Color(0xFF4C566A),
)

@Composable
fun ContinueWatchingSection(
    items: List<MediaItemCompat>,
    externalFocusRequestToken: Int,
    isInteractive: Boolean,
    resumeFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onResumeClick: (MediaItemCompat) -> Unit = {},
    onDetailsClick: (MediaItemCompat) -> Unit = {},
    onCardClick: (MediaItemCompat) -> Unit = {},
    onSectionContentFocused: () -> Unit = {},
    onOpenFeedMenu: () -> Unit = {},
    onMoveUpToBreadcrumb: () -> Unit = {},
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clip(ContinueWatchingSectionShape)
                .background(Color(0xFF12161D))
        )
        return
    }

    val rowState = rememberLazyListState()
    val cardFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    val scope = rememberCoroutineScope()
    val detailsFocusRequester = remember { FocusRequester() }
    var lastHandledExternalFocusToken by remember { mutableIntStateOf(0) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var lastFocusedCardIndex by rememberSaveable { mutableIntStateOf(0) }
    val remainingSuffix = stringResource(R.string.continue_watching_remaining_suffix)

    val safeSelectedIndex = selectedIndex.coerceIn(0, items.lastIndex)
    val selectedItem = items[safeSelectedIndex]

    fun requestSelectedCardFocus() {
        if (items.isEmpty() || !isInteractive) return
        scope.launch {
            val targetIndex = lastFocusedCardIndex.coerceIn(0, items.lastIndex)
            selectedIndex = targetIndex
            rowState.scrollToItem(targetIndex)

            repeat(18) {
                val cardRequester = cardFocusRequesters[targetIndex]
                if (cardRequester != null && cardRequester.requestFocus()) {
                    return@launch
                }
                delay(16)
            }
        }
    }

    LaunchedEffect(items.size) {
        selectedIndex = selectedIndex.coerceIn(0, items.lastIndex)
        lastFocusedCardIndex = lastFocusedCardIndex.coerceIn(0, items.lastIndex)
    }

    LaunchedEffect(externalFocusRequestToken, isInteractive, items.size) {
        if (!isInteractive || items.isEmpty()) return@LaunchedEffect
        if (externalFocusRequestToken <= lastHandledExternalFocusToken) return@LaunchedEffect

        repeat(18) {
            if (resumeFocusRequester.requestFocus()) {
                lastHandledExternalFocusToken = externalFocusRequestToken
                return@LaunchedEffect
            }
            delay(16)
        }
        lastHandledExternalFocusToken = externalFocusRequestToken
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(ContinueWatchingSectionShape)
            .background(Color(0xFF12161D))
    ) {
        ContinueWatchingBackground(
            posterUrl = selectedItem.posterUri,
            modifier = Modifier.matchParentSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 0.dp),
        ) {
            ContinueWatchingInfoPanel(
                title = selectedItem.name,
                metadata = selectedItem.continueWatchingMetadataLabel(remainingSuffix = remainingSuffix),
                isInteractive = isInteractive,
                resumeFocusRequester = resumeFocusRequester,
                detailsFocusRequester = detailsFocusRequester,
                onResumeClick = { onResumeClick(selectedItem) },
                onDetailsClick = { onDetailsClick(selectedItem) },
                onMoveDownToCards = { requestSelectedCardFocus() },
                onFocusedInPanel = onSectionContentFocused,
                onOpenFeedMenu = onOpenFeedMenu,
                onMoveUpToBreadcrumb = onMoveUpToBreadcrumb,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.56f)
                    .height(ContinueWatchingInfoPanelHeight)
                    .offset(y = -(ContinueWatchingRowHeight + ContinueWatchingInfoToRowGap))
            )

            LazyRow(
                state = rowState,
                horizontalArrangement = Arrangement.spacedBy(ContinueWatchingRowSpacing),
                verticalAlignment = Alignment.Bottom,
                contentPadding = PaddingValues(
                    start = 2.dp,
                    end = 2.dp,
                    top = 0.dp,
                    bottom = 8.dp
                ),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(ContinueWatchingRowHeight)
                    .testTag("continue_row")
            ) {
                itemsIndexed(
                    items = items,
                    key = { index, item ->
                        "${item.id}_${item.apiName}_${item.url}_$index"
                    }
                ) { index, item ->
                    val cardFocusRequester = cardFocusRequesters.getOrPut(index) { FocusRequester() }
                    ContinueWatchingPosterCard(
                        item = item,
                        index = index,
                        focusRequester = cardFocusRequester,
                        upFocusRequester = resumeFocusRequester,
                        isInteractive = isInteractive,
                        onFocused = {
                            selectedIndex = index
                            lastFocusedCardIndex = index
                            onSectionContentFocused()
                        },
                        onClick = { onCardClick(item) },
                        onOpenFeedMenu = onOpenFeedMenu,
                    )
                }
            }
        }
    }
}

@Composable
fun ContinueWatchingBackground(
    posterUrl: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .testTag("continue_bg")
            .background(Color(0xFF12161D))
    ) {
        if (posterUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(posterUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer(alpha = 0.95f)
                    .blur(6.dp),
            ) {
                SubcomposeAsyncImageContent()
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xF212161D),
                            Color(0xD812161D),
                            Color(0xF212161D),
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x7A12161D),
                            Color(0xF012161D),
                        )
                    )
                )
        )
    }
}

@Composable
fun ContinueWatchingInfoPanel(
    title: String,
    metadata: String?,
    isInteractive: Boolean,
    resumeFocusRequester: FocusRequester,
    detailsFocusRequester: FocusRequester,
    onResumeClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onMoveDownToCards: () -> Unit,
    onFocusedInPanel: () -> Unit,
    onOpenFeedMenu: () -> Unit,
    onMoveUpToBreadcrumb: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isResumeFocused by remember { mutableStateOf(false) }
    var isDetailsFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp, alignment = Alignment.Bottom)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("continue_title")
        )

        if (!metadata.isNullOrBlank()) {
            Text(
                text = metadata,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onResumeClick,
                modifier = Modifier
                    .focusRequester(resumeFocusRequester)
                    .focusProperties {
                        canFocus = isInteractive
                        right = detailsFocusRequester
                    }
                    .onFocusChanged { focusState ->
                        isResumeFocused = focusState.isFocused
                        if (focusState.isFocused) {
                            onFocusedInPanel()
                        }
                    }
                    .onPreviewKeyEvent { event ->
                        if (!isInteractive || event.type != KeyEventType.KeyDown) {
                            return@onPreviewKeyEvent false
                        }

                        when (event.key) {
                            Key.DirectionDown -> {
                                onMoveDownToCards()
                                true
                            }

                            Key.DirectionUp -> {
                                onMoveUpToBreadcrumb()
                                true
                            }

                            Key.DirectionLeft -> {
                                onOpenFeedMenu()
                                true
                            }

                            else -> false
                        }
                    }
                    .testTag("continue_resume"),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_play_arrow_24),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.resume),
                    color = if (isResumeFocused) Color.Black else Color.White
                )
            }

            OutlinedButton(
                onClick = onDetailsClick,
                modifier = Modifier
                    .focusRequester(detailsFocusRequester)
                    .focusProperties {
                        canFocus = isInteractive
                        left = resumeFocusRequester
                    }
                    .onFocusChanged { focusState ->
                        isDetailsFocused = focusState.isFocused
                        if (focusState.isFocused) {
                            onFocusedInPanel()
                        }
                    }
                    .onPreviewKeyEvent { event ->
                        if (!isInteractive || event.type != KeyEventType.KeyDown) {
                            return@onPreviewKeyEvent false
                        }

                        when (event.key) {
                            Key.DirectionDown -> {
                                onMoveDownToCards()
                                true
                            }

                            Key.DirectionUp -> {
                                onMoveUpToBreadcrumb()
                                true
                            }

                            else -> false
                        }
                    }
                    .testTag("continue_details"),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_info_24),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.details),
                    color = if (isDetailsFocused) Color.Black else Color.White
                )
            }
        }
    }
}

@Composable
fun ContinueWatchingPosterCard(
    item: MediaItemCompat,
    index: Int,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    isInteractive: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onOpenFeedMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val scale by animateFloatAsState(
        targetValue = if (isFocused) ContinueWatchingFocusedCardScale else 1f,
        animationSpec = tween(
            durationMillis = ContinueWatchingCardAnimationMs,
            easing = FastOutSlowInEasing
        ),
        label = "continue_card_scale"
    )
    val shadowElevationDp by animateDpAsState(
        targetValue = if (isFocused) 18.dp else 0.dp,
        animationSpec = tween(
            durationMillis = 140,
            easing = LinearOutSlowInEasing
        ),
        label = "continue_card_shadow"
    )
    val progress = (item.continueWatchingProgress ?: 0f).coerceIn(0f, 1f)
    val context = LocalContext.current
    val posterFallbackColor = remember(item.id, item.apiName, item.url) {
        ContinueWatchingPosterFallbackColors[
            (item.toFocusAnchorKey().hashCode().and(Int.MAX_VALUE)) % ContinueWatchingPosterFallbackColors.size
        ]
    }

    Surface(
        onClick = onClick,
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
        modifier = modifier
            .width(ContinueWatchingCardWidth)
            .aspectRatio(2f / 3f)
            .zIndex(if (isFocused) 1f else 0f)
            .onFocusChanged { state ->
                isFocused = state.isFocused
                if (state.isFocused) {
                    onFocused()
                }
            }
            .onPreviewKeyEvent { event ->
                if (!isInteractive || index != 0 || event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }

                if (event.key == Key.DirectionLeft) {
                    onOpenFeedMenu()
                    true
                } else {
                    false
                }
            }
            .focusProperties {
                canFocus = isInteractive
                up = upFocusRequester
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                shadowElevation = with(density) { shadowElevationDp.toPx() }
                spotShadowColor = Color.White
                ambientShadowColor = Color.White
                shape = CloudStreamCardShape
                clip = false
            }
            .focusRequester(focusRequester)
            .testTag("continue_card_$index"),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.posterUri.takeIf { it.isNotBlank() })
                    .crossfade(true)
                    .build(),
                contentDescription = null,
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

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(Color(0xFFFF5F3A))
                )
            }
        }
    }
}

private fun MediaItemCompat.toFocusAnchorKey(): String {
    return "${this.id}|${this.apiName}|${this.url}"
}

private fun MediaItemCompat.continueWatchingMetadataLabel(remainingSuffix: String): String? {
    val isSeriesContent = this is MediaItemCompat.TvSeries || this.type?.isEpisodeBased() == true
    val safeSeason = continueWatchingSeason?.takeIf { it > 0 }
    val safeEpisode = continueWatchingEpisode?.takeIf { it > 0 }
    val seasonEpisode = when {
        isSeriesContent && safeSeason != null && safeEpisode != null -> {
            "S${safeSeason}:E${safeEpisode}"
        }

        isSeriesContent && safeSeason != null -> "S${safeSeason}"
        isSeriesContent && safeEpisode != null -> "E${safeEpisode}"
        else -> null
    }
    val remaining = continueWatchingRemainingMs?.toRemainingLabel(remainingSuffix = remainingSuffix)

    return when {
        !seasonEpisode.isNullOrBlank() && !remaining.isNullOrBlank() -> "$seasonEpisode • $remaining"
        !seasonEpisode.isNullOrBlank() -> seasonEpisode
        !remaining.isNullOrBlank() -> remaining
        else -> null
    }
}

private fun Long.toRemainingLabel(remainingSuffix: String): String {
    val totalSeconds = (this / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    return if (hours > 0) {
        "${hours}h ${minutes}m $remainingSuffix"
    } else {
        String.format("%d:%02d %s", minutes, seconds, remainingSuffix)
    }
}
