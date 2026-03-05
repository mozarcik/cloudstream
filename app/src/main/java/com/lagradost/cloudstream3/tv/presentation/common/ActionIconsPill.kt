package com.lagradost.cloudstream3.tv.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsQuickAction

private const val ActionFocusAnimationMs = 180

@Immutable
data class ActionIconsPillStyle(
    val minHeight: Dp,
    val contentPadding: PaddingValues,
    val focusedContentPadding: PaddingValues,
    val iconSize: Dp,
    val iconSpacing: Dp,
    val typography: TextStyle,
)

object ActionIconsPillDefaults {
    @Composable
    fun default(): ActionIconsPillStyle {
        val typography = MaterialTheme.typography.titleSmall
        return remember(typography) {
            ActionIconsPillStyle(
                minHeight = 0.dp,
                contentPadding = OutlinedButtonDefaults.ContentPadding,
                focusedContentPadding = OutlinedButtonDefaults.ButtonWithIconContentPadding,
                iconSize = 24.dp,
                iconSpacing = OutlinedButtonDefaults.IconSpacing,
                typography = typography,
            )
        }
    }

    @Composable
    fun compact(): ActionIconsPillStyle {
        val typography = MaterialTheme.typography.bodySmall
        return remember(typography) {
            ActionIconsPillStyle(
                minHeight = 40.dp,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                focusedContentPadding = PaddingValues(start = 10.dp, top = 6.dp, end = 12.dp, bottom = 6.dp),
                iconSize = 18.dp,
                iconSpacing = 6.dp,
                typography = typography,
            )
        }
    }
}

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
    ActionIconsPill(
        actions = actions,
        style = ActionIconsPillDefaults.default(),
        modifier = modifier,
        onActionClick = onActionClick,
    )
}

@Composable
fun ActionIconsPill(
    actions: List<ActionIconSpec>,
    style: ActionIconsPillStyle,
    modifier: Modifier = Modifier,
    onActionClick: (MovieDetailsQuickAction) -> Unit = {},
) {
    if (actions.isEmpty()) return

    actions.forEach { action ->
        key(action.action) {
            ActionIconItem(
                action = action,
                style = style,
                modifier = modifier,
                onClick = { onActionClick(action.action) }
            )
        }
    }
}

@Composable
private fun ActionIconItem(
    action: ActionIconSpec,
    style: ActionIconsPillStyle,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val normalizedProgress = action.progressFraction.coerceIn(0f, 1f)
    val buttonModifier = modifier.heightIn(min = style.minHeight)

    if (action.action == MovieDetailsQuickAction.Download) {
        DownloadActionButton(
            action = action,
            isFocused = isFocused,
            progressFraction = normalizedProgress,
            style = style,
            interactionSource = interactionSource,
            modifier = buttonModifier,
            onClick = onClick
        )
        return
    }

    OutlinedButton(
        onClick = onClick,
        contentPadding = if (isFocused) style.focusedContentPadding else style.contentPadding,
        colors = OutlinedButtonDefaults.colors(),
        interactionSource = interactionSource,
        modifier = buttonModifier
    ) {
        ActionIconContent(
            icon = action.icon,
            label = action.label,
            isFocused = isFocused,
            style = style,
        )
    }
}

@Composable
internal fun ActionIconContent(
    icon: ImageVector,
    label: String,
    isFocused: Boolean,
    style: ActionIconsPillStyle,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(style.iconSize),
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
            Spacer(modifier = Modifier.width(style.iconSpacing))
            Text(
                text = label,
                style = style.typography,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
