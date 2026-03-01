package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.ShapeDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R

@Composable
internal fun SourceErrorDialog(
    state: TvPlayerSourceErrorDialog,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = ShapeDefaults.Large,
            colors = SurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
            ),
            modifier = Modifier.widthIn(max = 700.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.source_error),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = state.sourceLabel,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.go_back),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.tv_player_retry),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }
}
