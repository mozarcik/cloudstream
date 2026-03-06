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
