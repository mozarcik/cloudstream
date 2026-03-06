package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
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
import com.lagradost.cloudstream3.tv.presentation.common.ActionIconContent
import com.lagradost.cloudstream3.tv.presentation.common.ActionIconsPillDefaults
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.focus.rememberFocusRequesterMap
import com.lagradost.cloudstream3.tv.presentation.focus.requestFocusWithRetry
import kotlinx.coroutines.launch

@Composable
internal fun ContinueWatchingHeroLoadedState(
    items: List<MediaItemCompat>,
    resumeFocusRequester: FocusRequester,
    cardsFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    sourceButtonFocusRequester: FocusRequester,
    isInteractive: Boolean,
    pendingRestoreFocusTargetId: String? = null,
    restoreFocusToken: Int = 0,
    onResumeClick: (MediaItemCompat) -> Unit,
    onDetailsClick: (MediaItemCompat) -> Unit,
    onRemoveClick: (MediaItemCompat) -> Unit,
    onCardClick: (MediaItemCompat) -> Unit,
    onHeroContentFocused: () -> Unit,
    onFocusTargetFocused: (String) -> Unit,
    onRestoreFocusConsumed: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowState = rememberLazyListState()
    val detailsFocusRequester = remember { FocusRequester() }
    val removeFocusRequester = remember { FocusRequester() }
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

    fun cardTargetId(item: MediaItemCompat): String {
        return HomeFocusStore.continueWatchingCard(item)
    }

    val cardTargetIds = remember(items) { items.map(::cardTargetId) }
    val cardFocusRequesters = rememberFocusRequesterMap(cardTargetIds)

    val restoreFocusRequester = remember(
        pendingRestoreFocusTargetId,
        restoreFocusToken,
        items,
        lastFocusedCardIndex
    ) {
        when (pendingRestoreFocusTargetId) {
            HomeFocusStore.ContinueWatchingResume -> resumeFocusRequester
            HomeFocusStore.ContinueWatchingDetails -> detailsFocusRequester
            HomeFocusStore.ContinueWatchingRemove -> removeFocusRequester
            null -> null
            else -> items
                .indexOfFirst { item -> cardTargetId(item) == pendingRestoreFocusTargetId }
                .takeIf { it >= 0 }
                ?.let { index ->
                    if (index == lastFocusedCardIndex) {
                        cardsFocusRequester
                    } else {
                        cardFocusRequesters[cardTargetId(items[index])]
                    }
                }
        }
    }

    FocusRequestEffect(
        requester = restoreFocusRequester,
        requestKey = restoreFocusToken to pendingRestoreFocusTargetId,
        enabled = restoreFocusRequester != null && restoreFocusToken > 0,
        onFocused = {
            pendingRestoreFocusTargetId?.let(onRestoreFocusConsumed)
        }
    )

    fun requestCardsFocus() {
        if (!isInteractive) return

        coroutineScope.launch {
            val targetIndex = lastFocusedCardIndex.coerceIn(0, items.lastIndex)
            if (rowState.isItemOutsideViewport(targetIndex)) {
                rowState.scrollToItem(targetIndex)
            }
            val requester = if (targetIndex == lastFocusedCardIndex) {
                cardsFocusRequester
            } else {
                cardFocusRequesters[cardTargetId(items[targetIndex])]
            }
            if (requester != null && requester.requestFocusWithRetry(attempts = 18)) {
                return@launch
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
            upFocusRequester = upFocusRequester,
            resumeFocusRequester = resumeFocusRequester,
            detailsFocusRequester = detailsFocusRequester,
            removeFocusRequester = removeFocusRequester,
            onHeroContentFocused = onHeroContentFocused,
            onResumeClick = onResumeClick,
            onDetailsClick = onDetailsClick,
            onRemoveClick = {
                if (items.size == 1 && isInteractive) {
                    sourceButtonFocusRequester.requestFocus()
                }
                onRemoveClick(it)
            },
            onRequestCardsFocus = ::requestCardsFocus,
            onFocusTargetFocused = onFocusTargetFocused,
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
                val cardFocusRequester = cardFocusRequesters[cardTargetId(item)] ?: return@itemsIndexed
                ContinueWatchingHeroCard(
                    item = item,
                    focusRequester = if (index == lastFocusedCardIndex) {
                        cardsFocusRequester
                    } else {
                        cardFocusRequester
                    },
                    upFocusRequester = resumeFocusRequester,
                    downFocusRequester = sourceButtonFocusRequester,
                    isInteractive = isInteractive,
                    onFocused = {
                        selectedIndex = index
                        lastFocusedCardIndex = index
                        onHeroContentFocused()
                        onFocusTargetFocused(cardTargetId(item))
                    },
                    onClick = { onCardClick(item) },
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
    upFocusRequester: FocusRequester,
    resumeFocusRequester: FocusRequester,
    detailsFocusRequester: FocusRequester,
    removeFocusRequester: FocusRequester,
    onHeroContentFocused: () -> Unit,
    onResumeClick: (MediaItemCompat) -> Unit,
    onDetailsClick: (MediaItemCompat) -> Unit,
    onRemoveClick: (MediaItemCompat) -> Unit,
    onRequestCardsFocus: () -> Unit,
    onFocusTargetFocused: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val removeInteractionSource = remember { MutableInteractionSource() }
    val isRemoveButtonFocused by removeInteractionSource.collectIsFocusedAsState()
    val removeLabel = stringResource(R.string.action_remove_watching)

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
                        up = upFocusRequester
                        right = detailsFocusRequester
                    }
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onHeroContentFocused()
                            onFocusTargetFocused(HomeFocusStore.ContinueWatchingResume)
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
                        up = upFocusRequester
                        right = removeFocusRequester
                    }
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onHeroContentFocused()
                            onFocusTargetFocused(HomeFocusStore.ContinueWatchingDetails)
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

            OutlinedButton(
                onClick = { onRemoveClick(item) },
                contentPadding = if (isRemoveButtonFocused) {
                    OutlinedButtonDefaults.ButtonWithIconContentPadding
                } else {
                    OutlinedButtonDefaults.ContentPadding
                },
                interactionSource = removeInteractionSource,
                modifier = Modifier
                    .focusRequester(removeFocusRequester)
                    .focusProperties {
                        canFocus = isInteractive
                        left = detailsFocusRequester
                        up = upFocusRequester
                    }
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onHeroContentFocused()
                            onFocusTargetFocused(HomeFocusStore.ContinueWatchingRemove)
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
                ActionIconContent(
                    icon = Icons.Default.DeleteOutline,
                    label = removeLabel,
                    isFocused = isRemoveButtonFocused,
                    style = ActionIconsPillDefaults.default(),
                )
            }
        }
    }
}
