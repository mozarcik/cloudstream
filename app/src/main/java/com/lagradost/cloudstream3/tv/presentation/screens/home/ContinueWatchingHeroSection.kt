package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
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
import com.lagradost.cloudstream3.isEpisodeBased
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamCardShape
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamSurfaceDefaults
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ContinueWatchingHeroShape = RoundedCornerShape(24.dp)
private val ContinueWatchingHeroHeight = 460.dp
private val ContinueWatchingCardWidth = 136.dp
private val ContinueWatchingCardHeight = 85.dp
private val ContinueWatchingInfoBottomGap = 20.dp
private val ContinueWatchingCardsBottomInset = 18.dp
private val ContinueWatchingPlaceholderColor = Color(0xFF1D2430)

@Composable
fun ContinueWatchingHeroSection(
    state: HomeFeedLoadState,
    resumeFocusRequester: FocusRequester,
    sourceButtonFocusRequester: FocusRequester,
    isInteractive: Boolean,
    modifier: Modifier = Modifier,
    onResumeClick: (MediaItemCompat) -> Unit = {},
    onDetailsClick: (MediaItemCompat) -> Unit = {},
    onCardClick: (MediaItemCompat) -> Unit = {},
    onMoveDownFromCards: () -> Unit = {},
    onHeroContentFocused: () -> Unit = {},
) {
    when (state) {
        HomeFeedLoadState.Loading -> {
            ContinueWatchingHeroPlaceholder(
                modifier = modifier
                    .fillMaxWidth()
                    .height(ContinueWatchingHeroHeight)
            )
        }

        HomeFeedLoadState.Error -> {
            ContinueWatchingHeroPlaceholder(
                message = stringResource(R.string.tv_home_failed_to_load),
                modifier = modifier
                    .fillMaxWidth()
                    .height(ContinueWatchingHeroHeight)
            )
        }

        is HomeFeedLoadState.Success -> {
            if (state.items.isEmpty()) {
                ContinueWatchingHeroPlaceholder(
                    message = stringResource(R.string.tv_home_empty_continue_watching),
                    modifier = modifier
                        .fillMaxWidth()
                        .height(ContinueWatchingHeroHeight)
                )
                return
            }

            ContinueWatchingHeroLoadedState(
                items = state.items,
                resumeFocusRequester = resumeFocusRequester,
                sourceButtonFocusRequester = sourceButtonFocusRequester,
                isInteractive = isInteractive,
                onResumeClick = onResumeClick,
                onDetailsClick = onDetailsClick,
                onCardClick = onCardClick,
                onMoveDownFromCards = onMoveDownFromCards,
                onHeroContentFocused = onHeroContentFocused,
                modifier = modifier
                    .fillMaxWidth()
                    .height(ContinueWatchingHeroHeight)
            )
        }
    }
}

@Composable
private fun ContinueWatchingHeroLoadedState(
    items: List<MediaItemCompat>,
    resumeFocusRequester: FocusRequester,
    sourceButtonFocusRequester: FocusRequester,
    isInteractive: Boolean,
    onResumeClick: (MediaItemCompat) -> Unit,
    onDetailsClick: (MediaItemCompat) -> Unit,
    onCardClick: (MediaItemCompat) -> Unit,
    onMoveDownFromCards: () -> Unit,
    onHeroContentFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowState = rememberLazyListState()
    val cardFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    val detailsFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var lastFocusedCardIndex by rememberSaveable { mutableIntStateOf(0) }
    val remainingSuffix = stringResource(R.string.continue_watching_remaining_suffix)

    val safeSelectedIndex = selectedIndex.coerceIn(0, items.lastIndex)
    val selectedItem = items[safeSelectedIndex]

    LaunchedEffect(items.size) {
        selectedIndex = selectedIndex.coerceIn(0, items.lastIndex)
        lastFocusedCardIndex = lastFocusedCardIndex.coerceIn(0, items.lastIndex)
    }

    fun requestCardsFocus() {
        if (!isInteractive) return
        coroutineScope.launch {
            val targetIndex = lastFocusedCardIndex.coerceIn(0, items.lastIndex)
            rowState.animateScrollToItem(targetIndex)
            repeat(18) {
                val requester = cardFocusRequesters[targetIndex]
                if (requester != null && requester.requestFocus()) {
                    return@launch
                }
                delay(16)
            }
        }
    }

    Box(
        modifier = modifier
            .clip(ContinueWatchingHeroShape)
            .background(Color(0xFF10151D))
    ) {
        ContinueWatchingHeroBackdrop(
            posterUrl = selectedItem.posterUri,
            applyBlur = !selectedItem.continueWatchingHasBackdrop,
            modifier = Modifier.matchParentSize()
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.46f)
                .padding(
                    start = 32.dp,
                    end = 16.dp,
                    bottom = ContinueWatchingCardHeight + ContinueWatchingInfoBottomGap + ContinueWatchingCardsBottomInset
                )
        ) {
            Text(
                text = selectedItem.name,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            selectedItem.continueWatchingMetadataLabel(remainingSuffix)?.let { metadata ->
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
                    onClick = { onResumeClick(selectedItem) },
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    modifier = Modifier
                        .focusRequester(resumeFocusRequester)
                        .focusProperties {
                            canFocus = isInteractive
                            right = detailsFocusRequester
                        }
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                onHeroContentFocused()
                            }
                        }
                        .onPreviewKeyEvent { event ->
                            if (!isInteractive || event.type != KeyEventType.KeyDown) {
                                return@onPreviewKeyEvent false
                            }
                            if (event.key == Key.DirectionDown) {
                                requestCardsFocus()
                                true
                            } else {
                                false
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.resume),
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                OutlinedButton(
                    onClick = { onDetailsClick(selectedItem) },
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    modifier = Modifier
                        .focusRequester(detailsFocusRequester)
                        .focusProperties {
                            canFocus = isInteractive
                            left = resumeFocusRequester
                        }
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                onHeroContentFocused()
                            }
                        }
                        .onPreviewKeyEvent { event ->
                            if (!isInteractive || event.type != KeyEventType.KeyDown) {
                                return@onPreviewKeyEvent false
                            }
                            if (event.key == Key.DirectionDown) {
                                requestCardsFocus()
                                true
                            } else {
                                false
                            }
                        }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_outline_info_24),
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(OutlinedButtonDefaults.IconSpacing))
                    Text(
                        text = stringResource(R.string.details),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }

        LazyRow(
            state = rowState,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = ContinueWatchingCardsBottomInset)
                .height(ContinueWatchingCardHeight + 14.dp)
        ) {
            itemsIndexed(
                items = items,
                key = { index, item -> "${item.id}_${item.apiName}_${item.url}_$index" }
            ) { index, item ->
                val cardFocusRequester = cardFocusRequesters.getOrPut(index) { FocusRequester() }
                ContinueWatchingHeroCard(
                    item = item,
                    focusRequester = cardFocusRequester,
                    upFocusRequester = resumeFocusRequester,
                    downFocusRequester = sourceButtonFocusRequester,
                    isInteractive = isInteractive,
                    onFocused = {
                        selectedIndex = index
                        lastFocusedCardIndex = index
                        onHeroContentFocused()
                    },
                    onClick = { onCardClick(item) },
                    onMoveDown = onMoveDownFromCards,
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingHeroBackdrop(
    posterUrl: String,
    applyBlur: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Box(
        modifier = modifier.background(Color(0xFF10151D))
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
                    .then(if (applyBlur) Modifier.blur(8.dp) else Modifier),
            ) {
                SubcomposeAsyncImageContent()
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xAD10151D),
                            Color(0x6B10151D),
                            Color(0xC610151D)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x5610151D),
                            Color(0xB010151D)
                        )
                    )
                )
        )
    }
}

@Composable
private fun ContinueWatchingHeroCard(
    item: MediaItemCompat,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    isInteractive: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val progress = (item.continueWatchingProgress ?: 0f).coerceIn(0f, 1f)

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = CloudStreamCardShape),
        scale = CloudStreamSurfaceDefaults.scale(),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)),
                shape = CloudStreamCardShape
            )
        ),
        colors = CloudStreamSurfaceDefaults.colors(),
        modifier = modifier
            .width(ContinueWatchingCardWidth)
            .height(ContinueWatchingCardHeight)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onFocused()
                }
            }
            .onPreviewKeyEvent { event ->
                if (!isInteractive || event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }

                if (event.key == Key.DirectionDown) {
                    onMoveDown()
                    true
                } else {
                    false
                }
            }
            .focusProperties {
                canFocus = isInteractive
                up = upFocusRequester
                down = downFocusRequester
            }
            .focusRequester(focusRequester)
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ContinueWatchingPlaceholderColor)
                    )
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ContinueWatchingPlaceholderColor)
                    )
                },
                success = {
                    SubcomposeAsyncImageContent()
                }
            )

            if (isFocused) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.08f))
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.Black.copy(alpha = 0.52f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(Color(0xFFFF5F3A))
                )
            }
        }
    }
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

@Composable
private fun ContinueWatchingHeroPlaceholder(
    message: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(ContinueWatchingHeroShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A202B),
                        Color(0xFF10151D)
                    )
                )
            )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.44f)
                .padding(
                    start = 32.dp,
                    end = 16.dp,
                    bottom = ContinueWatchingCardHeight + ContinueWatchingInfoBottomGap + ContinueWatchingCardsBottomInset
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.12f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                )
                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                )
            }
            if (!message.isNullOrBlank()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.84f)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = ContinueWatchingCardsBottomInset + 8.dp
                )
        ) {
            repeat(5) {
                Box(
                    modifier = Modifier
                        .width(ContinueWatchingCardWidth)
                        .height(ContinueWatchingCardHeight)
                        .clip(CloudStreamCardShape)
                        .background(Color.White.copy(alpha = 0.14f))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.Black.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}
