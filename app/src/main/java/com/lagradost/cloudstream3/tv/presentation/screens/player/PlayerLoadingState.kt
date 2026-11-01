package com.lagradost.cloudstream3.tv.presentation.screens.player

import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerOptionButton
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerTitleBlock
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.PlayerTopMask
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.bulletJoined
import androidx.compose.foundation.layout.height
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.TvButton
import com.lagradost.cloudstream3.tv.presentation.screens.player.overlay.bulletJoined
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import kotlinx.coroutines.delay

@Composable
internal fun LoadingSourcesState(
    state: TvPlayerUiState.LoadingSources,
    onSkipLoading: () -> Unit,
    onBackPressed: () -> Unit,
) {
    PlayerKeepScreenOnEffect(keepScreenOn = true)

    val episodeLabel = when {
        state.metadata.season != null && state.metadata.episode != null -> {
            "S${state.metadata.season}:E${state.metadata.episode}"
        }

        state.metadata.episode != null -> {
            "E${state.metadata.episode}"
        }

        else -> null
    }
    val episodeLine = bulletJoined(
        listOfNotNull(
            episodeLabel,
            state.metadata.episodeTitle?.takeIf { title -> title.isNotBlank() },
        ),
    ).text.takeIf { it.isNotBlank() }
    // Loading line "year • provider" (mockup PlayerLoading); same place and size as the player's "source • quality".
    val infoLine = bulletJoined(
        listOfNotNull(
            state.metadata.year?.toString()?.takeIf { it.isNotBlank() },
            state.metadata.apiName.takeIf { it.isNotBlank() },
        ),
    ).text.takeIf { it.isNotBlank() }
    val skipFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        repeat(20) {
            if (skipFocusRequester.requestFocus()) {
                return@LaunchedEffect
            }
            delay(16)
        }
    }

    BackHandler(onBack = onBackPressed)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (!state.useBlackBackground && !state.metadata.backdropUri.isNullOrBlank()) {
            AsyncImage(
                model = state.metadata.backdropUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.6f,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(TvDimens.PlayerLoadingGradientHeight)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.85f),
                    )
                ),
        )
        PlayerTopMask(modifier = Modifier.align(Alignment.TopStart))
        PlayerTitleBlock(
            title = state.metadata.title,
            episodeLine = episodeLine,
            infoLine = infoLine,
            modifier = Modifier.align(Alignment.TopStart).padding(start = TvDimens.PlayerSidePadding),
        )
        // Status row at y = 876 px and the skip button at y = 950 px (mockup PlayerLoading).
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = TvDimens.PlayerSidePadding, top = TvDimens.PlayerLoadingStatusTop),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                modifier = Modifier.height(TvDimens.PlayerLoadingStatusHeight),
            ) {
                PlayerSpinner(
                    size = 14.dp,
                    strokeWidth = 2.5.dp,
                    color = TvColors.FeaturedEyebrow,
                    trackColor = Color.White.copy(alpha = 0.2f),
                )
                Text(
                    text = stringResource(R.string.tv_player_loading_links),
                    style = TvTextStyles.PlayerEpisode,
                    color = TvColors.Text,
                )
                Text(
                    text = stringResource(R.string.tv_player_loading_sources_progress, state.loadedSources),
                    style = TvTextStyles.PlayerInfo,
                    color = TvColors.PlayerValue,
                )
            }
            PlayerOptionButton(
                icon = PlayerIcons.Next,
                label = stringResource(R.string.skip_loading),
                // Always focusable (white, as in the mockup); the click works once skipping is possible.
                enabled = true,
                onClick = { if (state.canSkip) onSkipLoading() },
                modifier = Modifier.focusRequester(skipFocusRequester),
            )
        }
    }
}
