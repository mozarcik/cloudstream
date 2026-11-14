package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat

/** Focus targets of the hero's buttons, created by Home so it can focus the exact one it left. */
internal class ContinueWatchingHeroFocus(
    val resume: FocusRequester = FocusRequester(),
    val details: FocusRequester = FocusRequester(),
    val remove: FocusRequester = FocusRequester(),
)

@Composable
internal fun ContinueWatchingHeroSection(
    state: HomeFeedLoadState,
    heroFocus: ContinueWatchingHeroFocus,
    cardsFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    sourceButtonFocusRequester: FocusRequester,
    isInteractive: Boolean,
    modifier: Modifier = Modifier,
    onResumeClick: (MediaItemCompat) -> Unit = {},
    onDetailsClick: (MediaItemCompat) -> Unit = {},
    onRemoveClick: (MediaItemCompat) -> Unit = {},
    onCardClick: (MediaItemCompat) -> Unit = {},
    onFocusTargetFocused: (String) -> Unit = {},
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
                heroFocus = heroFocus,
                cardsFocusRequester = cardsFocusRequester,
                upFocusRequester = upFocusRequester,
                sourceButtonFocusRequester = sourceButtonFocusRequester,
                isInteractive = isInteractive,
                onResumeClick = onResumeClick,
                onDetailsClick = onDetailsClick,
                onRemoveClick = onRemoveClick,
                onCardClick = onCardClick,
                onFocusTargetFocused = onFocusTargetFocused,
                modifier = modifier
                    .fillMaxWidth()
                    .height(ContinueWatchingHeroHeight)
            )
        }
    }
}
