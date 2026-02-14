package com.lagradost.cloudstream3.tv.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsQuickAction

private const val ActionFocusAnimationMs = 180

data class ActionIconSpec(
    val icon: ImageVector,
    val label: String,
    val testTag: String,
    val action: MovieDetailsQuickAction,
    val progressFraction: Float = 0f,
)

@Composable
fun ActionIconsPill(
    actions: List<ActionIconSpec>,
    modifier: Modifier = Modifier,
    onActionClick: (MovieDetailsQuickAction) -> Unit = {},
) {
    if (actions.isEmpty()) return

    actions.forEach { action ->
        ActionIconItem(
            action = action,
            modifier = modifier,
            onClick = { onActionClick(action.action) }
        )
    }
}

@Composable
private fun ActionIconItem(
    action: ActionIconSpec,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val normalizedProgress = action.progressFraction.coerceIn(0f, 1f)

    if (normalizedProgress > 0f && action.action == MovieDetailsQuickAction.Download) {
        ProgressButtonV2(
            action = action,
            isFocused = isFocused,
            progressFraction = normalizedProgress,
            interactionSource = interactionSource,
            modifier = modifier,
            onClick = onClick
        )
        return
    }

    OutlinedButton(
        onClick = onClick,
        contentPadding = if (isFocused) {
            OutlinedButtonDefaults.ButtonWithIconContentPadding
        } else {
            OutlinedButtonDefaults.ContentPadding
        },
        colors = OutlinedButtonDefaults.colors(),
        interactionSource = interactionSource,
        modifier = modifier
    ) {
        ActionIconContent(
            icon = action.icon,
            label = action.label,
            isFocused = isFocused,
        )
    }
}

@Composable
internal fun ActionIconContent(
    icon: ImageVector,
    label: String,
    isFocused: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
        )
        AnimatedVisibility(
            visible = isFocused,
            enter = fadeIn(animationSpec = tween(ActionFocusAnimationMs)) +
                expandHorizontally(
                    animationSpec = tween(ActionFocusAnimationMs),
                    expandFrom = Alignment.Start
                ),
            exit = fadeOut(animationSpec = tween(ActionFocusAnimationMs)) +
                shrinkHorizontally(
                    animationSpec = tween(ActionFocusAnimationMs),
                    shrinkTowards = Alignment.Start
                )
        ) {
            Spacer(modifier = Modifier.width(OutlinedButtonDefaults.IconSpacing))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
