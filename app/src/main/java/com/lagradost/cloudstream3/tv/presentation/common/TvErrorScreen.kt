package com.lagradost.cloudstream3.tv.presentation.common

import androidx.compose.ui.text.TextStyle
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.R

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
    messageStyle: TextStyle = TvTextStyles.DialogBody,
    messageMaxLines: Int = Int.MAX_VALUE,
) {
    BackHandler(onBack = onBackPressed)

    TvErrorState(
        message = error.message,
        title = title,
        messageStyle = messageStyle,
        messageMaxLines = messageMaxLines,
        retryText = stringResource(R.string.tv_player_retry),
        onRetry = onRetry.takeIf { error.canRetry },
        secondaryText = stringResource(R.string.go_back),
        onSecondary = onBackPressed,
        retryModifier = Modifier.testTag("tv_error_retry_button"),
        secondaryModifier = Modifier.testTag("tv_error_back_button"),
        modifier = modifier.background(backgroundColor),
    )
}
