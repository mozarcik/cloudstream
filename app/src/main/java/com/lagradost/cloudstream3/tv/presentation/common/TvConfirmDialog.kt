package com.lagradost.cloudstream3.tv.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.focus.tvFocusTrap
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DialogAnimationDurationMs = TvDimens.FocusAnimationMs

/**
 * Confirmation dialog (delete download, remove plugin, ...). Fade only, <= 150 ms, focus trapped,
 * Back dismisses. Set [destructive] to paint the primary action red and
 * [focusSecondaryFirst] to start on the safe action (Cancel), as in the mockup.
 */
@Composable
fun TvConfirmDialog(
    title: String,
    description: String?,
    primaryText: String,
    secondaryText: String?,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    focusSecondaryFirst: Boolean = false,
    /** Distance from the top of the screen; null centers the dialog (mockup delete dialog: 330 px). */
    topOffset: Dp? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val primaryFocusRequester = remember { FocusRequester() }
    val secondaryFocusRequester = remember { FocusRequester() }
    var isVisible by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }

    fun closeThen(action: () -> Unit) {
        if (isClosing) return
        isClosing = true
        isVisible = false
        coroutineScope.launch {
            delay(DialogAnimationDurationMs.toLong())
            action()
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    FocusRequestEffect(
        requester = if (focusSecondaryFirst && secondaryText != null) {
            secondaryFocusRequester
        } else {
            primaryFocusRequester
        },
        requestKey = isVisible,
        enabled = isVisible
    )

    val dialogAlpha = animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(DialogAnimationDurationMs),
        label = "tv-dialog-alpha",
    )

    Dialog(
        onDismissRequest = {
            closeThen(onDismiss)
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TvColors.DialogScrim)
                .padding(top = topOffset ?: 0.dp),
            contentAlignment = if (topOffset != null) Alignment.TopCenter else Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .graphicsLayer { alpha = dialogAlpha.value }
                    .width(TvDimens.DialogWidth)
                    .tvFocusTrap()
                    .background(TvColors.DialogSurface, TvShapes.Dialog)
                    .padding(
                        horizontal = TvDimens.DialogHorizontalPadding,
                        vertical = TvDimens.DialogVerticalPadding,
                    )
            ) {
                Text(
                    text = title,
                    style = TvTextStyles.DialogTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )

                description?.takeIf { it.isNotBlank() }?.let { body ->
                    Text(
                        text = body,
                        style = TvTextStyles.DialogMessage,
                        color = TvColors.Meta,
                        maxLines = 4,
                        modifier = Modifier.padding(top = TvDimens.DialogTitleBodyGap)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(TvDimens.DialogButtonGap, Alignment.End),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = TvDimens.DialogBodyButtonsGap)
                ) {
                    TvButton(
                        text = primaryText,
                        onClick = { closeThen(onPrimary) },
                        variant = if (destructive) TvButtonVariant.Destructive else TvButtonVariant.Default,
                        height = TvDimens.ButtonHeight,
                        modifier = Modifier.focusRequester(primaryFocusRequester),
                    )

                    secondaryText?.let { secondaryLabel ->
                        TvButton(
                            text = secondaryLabel,
                            onClick = { closeThen(onSecondary) },
                            height = TvDimens.ButtonHeight,
                            modifier = Modifier.focusRequester(secondaryFocusRequester),
                        )
                    }
                }
            }
        }
    }
}
