package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun ContinueWatchingHeroLoadedState(
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
            if (rowState.isItemOutsideViewport(targetIndex)) {
                rowState.scrollToItem(targetIndex)
            }
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
            .background(MaterialTheme.colorScheme.surface)
    ) {
        ContinueWatchingHeroBackdrop(
            posterUrl = selectedItem.posterUri,
            applyBlur = !selectedItem.continueWatchingHasBackdrop,
            modifier = Modifier.matchParentSize()
        )

        ContinueWatchingHeroInfo(
            item = selectedItem,
            remainingSuffix = remainingSuffix,
            isInteractive = isInteractive,
            resumeFocusRequester = resumeFocusRequester,
            detailsFocusRequester = detailsFocusRequester,
            onHeroContentFocused = onHeroContentFocused,
            onResumeClick = onResumeClick,
            onDetailsClick = onDetailsClick,
            onRequestCardsFocus = ::requestCardsFocus,
            modifier = Modifier.align(Alignment.BottomStart)
        )

        LazyRow(
            state = rowState,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 8.dp,
                top = ContinueWatchingCardsTopInset,
                bottom = ContinueWatchingCardsBottomInset
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(bottom = ContinueWatchingCardsBottomInset)
                .height(ContinueWatchingCardHeight + ContinueWatchingCardsTopInset)
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
private fun ContinueWatchingHeroInfo(
    item: MediaItemCompat,
    remainingSuffix: String,
    isInteractive: Boolean,
    resumeFocusRequester: FocusRequester,
    detailsFocusRequester: FocusRequester,
    onHeroContentFocused: () -> Unit,
    onResumeClick: (MediaItemCompat) -> Unit,
    onDetailsClick: (MediaItemCompat) -> Unit,
    onRequestCardsFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth(0.46f)
            .padding(
                start = 32.dp,
                end = 16.dp,
                bottom = ContinueWatchingCardHeight + ContinueWatchingInfoBottomGap + ContinueWatchingCardsBottomInset
            )
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        item.continueWatchingMetadataLabel(remainingSuffix)?.let { metadata ->
            Text(
                text = metadata,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { onResumeClick(item) },
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                colors = ButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    focusedContainerColor = MaterialTheme.colorScheme.primary,
                    focusedContentColor = MaterialTheme.colorScheme.onPrimary,
                    pressedContainerColor = MaterialTheme.colorScheme.primary,
                    pressedContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
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
                            onRequestCardsFocus()
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
                onClick = { onDetailsClick(item) },
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
                            onRequestCardsFocus()
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
}
