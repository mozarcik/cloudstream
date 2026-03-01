package com.lagradost.cloudstream3.tv.presentation.screens.player.overlay

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.CircleShape
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface

@Immutable
internal data class PlayerControlButtonStyle(
    val buttonSize: Dp,
    val iconSize: Dp,
    val focusedScale: Float,
    val containerColor: Color,
    val contentColor: Color,
    val focusedContainerColor: Color,
    val focusedContentColor: Color,
    val focusedBorder: Border = Border.None,
) {
    companion object {
        val Primary = PlayerControlButtonStyle(
            buttonSize = PlayerControlsTokens.PlayButtonSize,
            iconSize = PlayerControlsTokens.PlayIconSize,
            focusedScale = PlayerControlsTokens.PlayFocusScale,
            containerColor = Color.White,
            contentColor = Color.Black,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        )
        val Secondary = PlayerControlButtonStyle(
            buttonSize = PlayerControlsTokens.SecondaryButtonSize,
            iconSize = PlayerControlsTokens.SecondaryIconSize,
            focusedScale = PlayerControlsTokens.SecondaryFocusScale,
            containerColor = Color.White.copy(alpha = PlayerControlsTokens.SecondaryContainerAlpha),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        )
    }
}

@Composable
internal fun PlayerControlButton(
    icon: ImageVector,
    tooltipText: String,
    onClick: () -> Unit,
    controlsEnabled: Boolean,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    style: PlayerControlButtonStyle,
    onTooltipVisible: (String, Rect) -> Unit,
    onTooltipHidden: () -> Unit,
    onFocused: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    var buttonBoundsInRoot by remember { mutableStateOf<Rect?>(null) }

    Box(
        modifier = Modifier.size(style.buttonSize),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            enabled = controlsEnabled,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .onGloballyPositioned { coordinates ->
                    buttonBoundsInRoot = coordinates.boundsInRoot()
                    if (isFocused) {
                        buttonBoundsInRoot?.let { bounds ->
                            onTooltipVisible(tooltipText, bounds)
                        }
                    }
                }
                .onFocusChanged { focusState ->
                    val nowFocused = focusState.isFocused
                    if (isFocused == nowFocused) return@onFocusChanged

                    isFocused = nowFocused
                    if (nowFocused) {
                        onFocused()
                        buttonBoundsInRoot?.let { bounds ->
                            onTooltipVisible(tooltipText, bounds)
                        }
                    } else {
                        onTooltipHidden()
                    }
                }
                .horizontalFocusLink(
                    canFocus = controlsEnabled,
                    leftFocusRequester = leftFocusRequester,
                    rightFocusRequester = rightFocusRequester,
                ),
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = style.containerColor,
                contentColor = style.contentColor,
                focusedContainerColor = style.focusedContainerColor,
                focusedContentColor = style.focusedContentColor,
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = style.focusedBorder,
                pressedBorder = Border.None,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = style.focusedScale),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tooltipText,
                    modifier = Modifier.size(style.iconSize),
                )
            }
        }
    }
}

internal fun Modifier.horizontalFocusLink(
    canFocus: Boolean,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
): Modifier {
    if (canFocus && leftFocusRequester == null && rightFocusRequester == null) {
        return this
    }

    return focusProperties {
        this.canFocus = canFocus
        leftFocusRequester?.let { left = it }
        rightFocusRequester?.let { right = it }
    }
}
