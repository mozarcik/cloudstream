package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import com.lagradost.cloudstream3.tv.presentation.common.TvButton
import com.lagradost.cloudstream3.tv.presentation.common.TvProgressBar
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.delay
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.isEpisodeBased
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.compat.home.preferredBackdropUriOrNull
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.focus.isItemComposed
import com.lagradost.cloudstream3.tv.presentation.focus.requestFocusWithRetry
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun ContinueWatchingHeroLoadedState(
    items: List<MediaItemCompat>,
    heroFocus: ContinueWatchingHeroFocus,
    cardsFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    sourceButtonFocusRequester: FocusRequester,
    isInteractive: Boolean,
    onResumeClick: (MediaItemCompat) -> Unit,
    onDetailsClick: (MediaItemCompat) -> Unit,
    onRemoveClick: (MediaItemCompat) -> Unit,
    onCardClick: (MediaItemCompat) -> Unit,
    onFocusTargetFocused: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardsGroup = HomeFocusStore.cardsGroup
    // A row that is created again (Back from a title) starts with the remembered card in view.
    val rowState = rememberLazyListState(
        initialFirstVisibleItemIndex = (cardsGroup.lastIndex.coerceIn(0, items.lastIndex) - 2).coerceAtLeast(0),
    )
    // Home's list disables Compose's focus scrolling; this row needs it: the focused card stays
    // fully visible with a 48 dp margin (ring included) on both sides.
    val density = LocalDensity.current
    val cardsBringIntoViewSpec = remember(density) {
        val marginPx = with(density) { HomeContentHorizontalPadding.toPx() }
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                val trailing = offset + size
                return when {
                    offset < marginPx -> offset - marginPx
                    trailing > containerSize - marginPx -> trailing - (containerSize - marginPx)
                    else -> 0f
                }
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var pendingRemoveFocus by rememberSaveable { mutableStateOf(false) }
    val remainingSuffix = stringResource(R.string.continue_watching_remaining_suffix)

    val safeSelectedIndex = selectedIndex.coerceIn(0, items.lastIndex)
    val selectedItem = items[safeSelectedIndex]
    val selectedBackdropUrl = selectedItem.preferredBackdropUriOrNull()
    val colorTokens = rememberContinueWatchingHeroColorTokens(
        artworkUrl = selectedBackdropUrl,
        baseColorScheme = MaterialTheme.colorScheme,
    )

    // Background and text follow the focused card after a short debounce (no animation).
    var focusedCardIndex by remember { mutableIntStateOf(selectedIndex) }
    LaunchedEffect(focusedCardIndex) {
        if (focusedCardIndex != selectedIndex) {
            delay(TvDimens.HeroSwapDebounceMs)
            selectedIndex = focusedCardIndex
        }
    }

    LaunchedEffect(items.size) {
        selectedIndex = selectedIndex.coerceIn(0, items.lastIndex)
        focusedCardIndex = focusedCardIndex.coerceIn(0, items.lastIndex)
        cardsGroup.lastIndex = cardsGroup.lastIndex.coerceIn(0, items.lastIndex)
    }

    fun cardTargetId(item: MediaItemCompat): String {
        return HomeFocusStore.continueWatchingCard(item)
    }

    // The row is a focus group that enters on the last focused card; that card must be composed.
    fun requestCardsFocus() {
        if (!isInteractive) return

        coroutineScope.launch {
            val targetIndex = cardsGroup.lastIndex.coerceIn(0, items.lastIndex)
            if (rowState.isItemOutsideViewport(targetIndex)) {
                rowState.scrollToItem(targetIndex)
            }
            cardsFocusRequester.requestFocusWithRetry(attempts = 18)
        }
    }

    // After "Remove" the focus moves to the neighbouring card (never to the top bar).
    LaunchedEffect(items.size) {
        if (pendingRemoveFocus) {
            pendingRemoveFocus = false
            selectedIndex = cardsGroup.lastIndex.coerceIn(0, items.lastIndex)
            focusedCardIndex = selectedIndex
            requestCardsFocus()
        }
    }

    Box(modifier = modifier) {
        ContinueWatchingHeroBackdrop(
            imageUrl = selectedBackdropUrl,
            modifier = Modifier.matchParentSize()
        )

        ContinueWatchingHeroInfo(
            item = selectedItem,
            colorTokens = colorTokens,
            remainingSuffix = remainingSuffix,
            isInteractive = isInteractive,
            upFocusRequester = upFocusRequester,
            resumeFocusRequester = heroFocus.resume,
            detailsFocusRequester = heroFocus.details,
            removeFocusRequester = heroFocus.remove,
            onResumeClick = onResumeClick,
            onDetailsClick = onDetailsClick,
            onRemoveClick = {
                if (items.size == 1 && isInteractive) {
                    sourceButtonFocusRequester.requestFocus()
                } else {
                    pendingRemoveFocus = true
                }
                onRemoveClick(it)
            },
            onFocusTargetFocused = onFocusTargetFocused,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = ContinueWatchingInfoTop)
        )

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(TvDimens.HeroSectionTitleGap),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = HomeContentHorizontalPadding, top = ContinueWatchingSectionTitleTop)
        ) {
            Text(
                text = stringResource(R.string.continue_watching),
                style = TvTextStyles.HeroSectionTitle,
                color = TvColors.Text,
            )
            Text(
                text = items.size.toString(),
                style = TvTextStyles.Meta,
                color = TvColors.Label,
            )
        }

        CompositionLocalProvider(LocalBringIntoViewSpec provides cardsBringIntoViewSpec) {
        LazyRow(
            state = rowState,
            horizontalArrangement = Arrangement.spacedBy(ContinueWatchingCardSpacing),
            contentPadding = PaddingValues(
                start = HomeContentHorizontalPadding,
                end = TvDimens.ScreenHorizontalPadding,
                top = ContinueWatchingCardsVerticalInset,
                bottom = ContinueWatchingCardsVerticalInset,
            ),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = ContinueWatchingCardsTop - ContinueWatchingCardsVerticalInset)
                .fillMaxWidth()
                // Entering the row (Down from the buttons, Up from the chips) lands on the last focused card.
                .focusRequester(cardsFocusRequester)
                .let { base -> with(cardsGroup) { base.tvFocusGroup(isItemComposed = rowState::isItemComposed) } }
        ) {
            itemsIndexed(
                items = items,
                key = { index, item -> "${item.id}_${item.apiName}_${item.url}_$index" }
            ) { index, item ->
                ContinueWatchingHeroCard(
                    item = item,
                    colorTokens = colorTokens,
                    isSelected = index == safeSelectedIndex,
                    focusRequester = cardsGroup.itemRequester(index),
                    upFocusRequester = heroFocus.resume,
                    downFocusRequester = sourceButtonFocusRequester,
                    isInteractive = isInteractive,
                    onFocused = {
                        focusedCardIndex = index
                        cardsGroup.onItemFocused(index)
                        onFocusTargetFocused(cardTargetId(item))
                    },
                    onClick = { onCardClick(item) },
                )
            }
        }
        }
    }
}

@Composable
private fun ContinueWatchingHeroInfo(
    item: MediaItemCompat,
    colorTokens: ContinueWatchingHeroColorTokens,
    remainingSuffix: String,
    isInteractive: Boolean,
    upFocusRequester: FocusRequester,
    resumeFocusRequester: FocusRequester,
    detailsFocusRequester: FocusRequester,
    removeFocusRequester: FocusRequester,
    onResumeClick: (MediaItemCompat) -> Unit,
    onDetailsClick: (MediaItemCompat) -> Unit,
    onRemoveClick: (MediaItemCompat) -> Unit,
    onFocusTargetFocused: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(ContinueWatchingInfoWidth)
            .padding(start = HomeContentHorizontalPadding)
    ) {
        Text(
            text = stringResource(R.string.continue_watching).uppercase(),
            style = TvTextStyles.HeroEyebrow,
            color = colorTokens.progressIndicatorColor,
        )
        Spacer(Modifier.height(TvDimens.HeroEyebrowGap))
        Text(
            text = item.name,
            style = TvTextStyles.Hero,
            color = TvColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // Fixed one-line slot (mockup line-height 1): the block below never moves.
            modifier = Modifier
                .height(TvDimens.HeroTitleHeight)
                .wrapContentHeight(Alignment.CenterVertically, unbounded = true)
        )
        Spacer(Modifier.height(TvDimens.HeroMetaTopGap))
        ContinueWatchingMetaRow(
            item = item,
            remainingSuffix = remainingSuffix,
            progressColor = colorTokens.progressIndicatorColor,
        )
        Spacer(Modifier.height(TvDimens.HeroButtonsTopGap))

        fun Modifier.heroButton(
            requester: FocusRequester,
            targetId: String,
            tag: String,
        ): Modifier = this
            .testTag(tag)
            .focusRequester(requester)
            .focusProperties {
                canFocus = isInteractive
                up = upFocusRequester
            }
            .onFocusChanged { focusState ->
                if (focusState.isFocused) onFocusTargetFocused(targetId)
            }

        Row(horizontalArrangement = Arrangement.spacedBy(ContinueWatchingButtonSpacing)) {
            TvButton(
                text = stringResource(R.string.resume),
                icon = Icons.Default.PlayArrow,
                onClick = { onResumeClick(item) },
                height = TvDimens.ButtonHeightLarge,
                textStyle = TvTextStyles.ButtonLarge,
                modifier = Modifier.heroButton(
                    requester = resumeFocusRequester,
                    targetId = HomeFocusStore.ContinueWatchingResume,
                    tag = "home_continue_watching_resume_button",
                ),
            )
            TvButton(
                text = stringResource(R.string.details),
                icon = Icons.Outlined.Info,
                onClick = { onDetailsClick(item) },
                height = TvDimens.ButtonHeightLarge,
                textStyle = TvTextStyles.ButtonLarge,
                modifier = Modifier.heroButton(
                    requester = detailsFocusRequester,
                    targetId = HomeFocusStore.ContinueWatchingDetails,
                    tag = "home_continue_watching_details_button",
                ),
            )
            TvButton(
                text = stringResource(R.string.action_remove_watching),
                icon = Icons.Default.DeleteOutline,
                onClick = { onRemoveClick(item) },
                height = TvDimens.ButtonHeightLarge,
                textStyle = TvTextStyles.ButtonLarge,
                modifier = Modifier.heroButton(
                    requester = removeFocusRequester,
                    targetId = HomeFocusStore.ContinueWatchingRemove,
                    tag = "home_continue_watching_remove_button",
                ),
            )
        }
    }
}

@Composable
private fun ContinueWatchingMetaRow(
    item: MediaItemCompat,
    remainingSuffix: String,
    progressColor: Color,
) {
    val typeLabel = when {
        item is MediaItemCompat.TvSeries || item.type?.isEpisodeBased() == true ->
            stringResource(R.string.tv_series_singular)
        else -> stringResource(R.string.movies_singular)
    }
    val year = when (item) {
        is MediaItemCompat.Movie -> item.year
        is MediaItemCompat.TvSeries -> item.year
        else -> null
    }
    val seasonEpisode = item.continueWatchingSeasonEpisodeLabel()
    val remaining = item.continueWatchingRemainingLabel(remainingSuffix)
    val progress = (item.continueWatching?.progress ?: 0f).coerceIn(0f, 1f)

    val parts = listOfNotNull(typeLabel, seasonEpisode, year?.toString())
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.HeroMetaGap),
    ) {
        parts.forEach { part ->
            Text(text = part, style = TvTextStyles.HeroMeta, color = TvColors.Meta, maxLines = 1, softWrap = false)
            Text(text = "•", style = TvTextStyles.HeroMeta, color = ContinueWatchingMetaDot)
        }
        TvProgressBar(
            progress = progress,
            accent = progressColor,
            modifier = Modifier.width(ContinueWatchingMetaProgressWidth),
        )
        if (remaining != null) {
            Text(text = remaining, style = TvTextStyles.HeroMeta, color = TvColors.Meta, maxLines = 1, softWrap = false)
        }
    }
}
