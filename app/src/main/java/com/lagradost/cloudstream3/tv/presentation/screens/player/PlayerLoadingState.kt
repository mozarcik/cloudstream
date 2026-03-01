package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.screens.movies.DotSeparatedRow
import kotlinx.coroutines.delay

@Composable
internal fun LoadingSourcesState(
    state: TvPlayerUiState.LoadingSources,
    onSkipLoading: () -> Unit,
    onBackPressed: () -> Unit,
) {
    val loadingMetaTexts = listOfNotNull(
        state.metadata.year?.toString()?.takeIf { it.isNotBlank() },
        state.metadata.apiName.takeIf { it.isNotBlank() },
    )
    val skipFocusRequester = remember { FocusRequester() }

    LaunchedEffect(state.canSkip) {
        if (!state.canSkip) return@LaunchedEffect

        repeat(20) {
            if (skipFocusRequester.requestFocus()) {
                return@LaunchedEffect
            }
            delay(16)
        }
    }

    BackHandler(onBack = onBackPressed)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomStart,
    ) {
        AsyncImage(
            model = state.metadata.backdropUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.88f),
                        ),
                        startY = 220f,
                    )
                )
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Transparent,
                        ),
                        endX = 850f,
                    )
                )
        )

        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .padding(horizontal = 48.dp, vertical = 42.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.metadata.title.isNotBlank()) {
                Text(
                    text = state.metadata.title,
                    style = MaterialTheme.typography.headlineLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (loadingMetaTexts.isNotEmpty()) {
                DotSeparatedRow(
                    texts = loadingMetaTexts,
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f),
                    ),
                )
            }

            Text(
                text = stringResource(R.string.tv_player_loading_sources_progress, state.loadedSources),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
            )

            Button(
                onClick = onSkipLoading,
                enabled = state.canSkip,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .focusRequester(skipFocusRequester),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Text(
                    text = stringResource(R.string.skip_loading),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}
