package com.lagradost.cloudstream3.tv.presentation.common

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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R

private object TvErrorScreenTokens {
    val ContentWidth = 760.dp
    val HorizontalPadding = 40.dp
    val ContentSpacing = 16.dp
    val ActionSpacing = 16.dp
}

@Immutable
data class TvErrorUiModel(
    val message: String,
    val canRetry: Boolean = true,
)

@Composable
fun TvErrorScreen(
    error: TvErrorUiModel,
    onRetry: (() -> Unit)?,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
) {
    BackHandler(onBack = onBackPressed)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = TvErrorScreenTokens.ContentWidth)
                .padding(horizontal = TvErrorScreenTokens.HorizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TvErrorScreenTokens.ContentSpacing),
        ) {
            title?.takeIf { it.isNotBlank() }?.let { header ->
                Text(
                    text = header,
                    style = MaterialTheme.typography.displaySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = error.message,
                style = MaterialTheme.typography.titleLarge,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(TvErrorScreenTokens.ActionSpacing)) {
                if (error.canRetry && onRetry != null) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.testTag("tv_error_retry_button")
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.tv_player_retry),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }

                OutlinedButton(
                    onClick = onBackPressed,
                    modifier = Modifier.testTag("tv_error_back_button"),
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.go_back),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
    }
}
