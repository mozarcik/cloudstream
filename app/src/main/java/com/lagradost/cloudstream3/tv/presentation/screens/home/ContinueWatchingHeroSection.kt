package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat

@Composable
fun ContinueWatchingHeroSection(
    state: HomeFeedLoadState,
    resumeFocusRequester: FocusRequester,
    cardsFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    sourceButtonFocusRequester: FocusRequester,
    isInteractive: Boolean,
    pendingRestoreFocusTargetId: String? = null,
    restoreFocusToken: Int = 0,
    modifier: Modifier = Modifier,
    onResumeClick: (MediaItemCompat) -> Unit = {},
    onDetailsClick: (MediaItemCompat) -> Unit = {},
    onRemoveClick: (MediaItemCompat) -> Unit = {},
    onCardClick: (MediaItemCompat) -> Unit = {},
    onHeroContentFocused: () -> Unit = {},
    onFocusTargetFocused: (String) -> Unit = {},
    onRestoreFocusConsumed: (String) -> Unit = {},
) {
    when (state) {
        HomeFeedLoadState.Loading -> {
            ContinueWatchingHeroPlaceholder(
                modifier = modifier
                    .fillMaxWidth()
                    .height(ContinueWatchingHeroHeight)
            )
        }

        // Nothing to continue: Home starts from the source chips (see HomeScreenV2Content).
        HomeFeedLoadState.Error -> Unit

        is HomeFeedLoadState.Success -> {
            if (state.items.isEmpty()) return

            ContinueWatchingHeroLoadedState(
                items = state.items,
                resumeFocusRequester = resumeFocusRequester,
                cardsFocusRequester = cardsFocusRequester,
                upFocusRequester = upFocusRequester,
                sourceButtonFocusRequester = sourceButtonFocusRequester,
                isInteractive = isInteractive,
                pendingRestoreFocusTargetId = pendingRestoreFocusTargetId,
                restoreFocusToken = restoreFocusToken,
                onResumeClick = onResumeClick,
                onDetailsClick = onDetailsClick,
                onRemoveClick = onRemoveClick,
                onCardClick = onCardClick,
                onHeroContentFocused = onHeroContentFocused,
                onFocusTargetFocused = onFocusTargetFocused,
                onRestoreFocusConsumed = onRestoreFocusConsumed,
                modifier = modifier
                    .fillMaxWidth()
                    .height(ContinueWatchingHeroHeight)
            )
        }
    }
}
