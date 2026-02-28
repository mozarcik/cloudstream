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
