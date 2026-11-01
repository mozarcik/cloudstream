package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.TvButton
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.bulletJoined
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.playerEpisodeLabel
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import kotlinx.coroutines.delay

/** "No Links Found" / playback error screen (mockup PlayerErrors, first board). */
@Composable
internal fun ErrorState(
    state: TvPlayerUiState.Error,
    onRetry: () -> Unit,
    onBackPressed: () -> Unit,
) {
    BackHandler(onBack = onBackPressed)
    val metadata = state.metadata
    val subtitle = bulletJoined(
        listOfNotNull(
            metadata.title.takeIf { it.isNotBlank() },
            playerEpisodeLabel(metadata).takeIf { metadata.isEpisodeBased },
            metadata.apiName.takeIf { it.isNotBlank() && !it.equals(metadata.title, ignoreCase = true) },
        ),
    )
    val retryFocusRequester = remember { FocusRequester() }
    val backFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        repeat(20) {
            if ((if (state.error.canRetry) retryFocusRequester else backFocusRequester).requestFocus()) {
                return@LaunchedEffect
            }
            delay(50)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().background(TvColors.Ground),
    ) {
        Icon(
            imageVector = PlayerIcons.Error,
            contentDescription = null,
            tint = TvColors.Danger,
            modifier = Modifier.size(TvDimens.StateIconSize),
        )
        Text(
            text = state.error.message,
            style = TvTextStyles.StateTitle,
            color = TvColors.Text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = TvDimens.StateTitleGap).widthIn(max = TvDimens.StateMaxWidth),
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = TvTextStyles.StateMessage,
                color = TvColors.Text2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = TvDimens.StateMessageGap).widthIn(max = TvDimens.StateMaxWidth),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(TvDimens.StateButtonGap),
            modifier = Modifier.padding(top = TvDimens.StateActionGap),
        ) {
            if (state.error.canRetry) {
                TvButton(
                    text = stringResource(R.string.tv_player_retry),
                    onClick = onRetry,
                    height = TvDimens.ButtonHeightLarge,
                    modifier = Modifier.focusRequester(retryFocusRequester).testTag("tv_error_retry_button"),
                )
            }
            TvButton(
                text = stringResource(R.string.go_back),
                onClick = onBackPressed,
                height = TvDimens.ButtonHeightLarge,
                modifier = Modifier.focusRequester(backFocusRequester).testTag("tv_error_back_button"),
            )
        }
    }
}
