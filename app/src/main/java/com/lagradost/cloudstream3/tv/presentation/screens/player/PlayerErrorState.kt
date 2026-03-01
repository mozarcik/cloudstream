package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R

@Composable
internal fun ErrorState(
    state: TvPlayerUiState.Error,
    onRetry: () -> Unit,
    onBackPressed: () -> Unit,
) {
    BackHandler(onBack = onBackPressed)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.metadata.title.isNotBlank()) {
                Text(
                    text = state.metadata.title,
                    style = MaterialTheme.typography.displaySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = stringResource(state.messageResId),
                style = MaterialTheme.typography.titleLarge,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onRetry) {
                    Text(
                        text = stringResource(R.string.tv_player_retry),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                Button(onClick = onBackPressed) {
                    Text(
                        text = stringResource(R.string.go_back),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}
