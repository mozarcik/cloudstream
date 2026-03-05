package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.TvConfirmDialog

@Composable
internal fun SourceErrorDialog(
    state: TvPlayerSourceErrorDialog,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    val description = buildString {
        append(state.sourceLabel)
        if (state.message.isNotBlank()) {
            append('\n')
            append(state.message)
        }
    }

    TvConfirmDialog(
        title = stringResource(R.string.source_error),
        description = description,
        primaryText = stringResource(R.string.tv_player_retry),
        secondaryText = stringResource(R.string.go_back),
        onPrimary = onRetry,
        onSecondary = onDismiss,
        onDismiss = onDismiss
    )
}
