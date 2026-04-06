package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import com.lagradost.cloudstream3.tv.presentation.common.TvErrorScreen

@Composable
internal fun ErrorState(
    state: TvPlayerUiState.Error,
    onRetry: () -> Unit,
    onBackPressed: () -> Unit,
) {
    TvErrorScreen(
        error = state.error,
        onRetry = onRetry,
        onBackPressed = onBackPressed,
        title = state.metadata.title,
        backgroundColor = Color.Black,
        modifier = Modifier,
    )
}
