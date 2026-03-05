package com.lagradost.cloudstream3.tv.presentation.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DialogAnimationDurationMs = 180
private const val DialogEnterScale = 0.95f
private const val DialogExitScale = 0.95f
private val DialogPreferredWidth = 560.dp
private val DialogMinWidth = 520.dp
private val DialogMaxWidth = 640.dp
private val DialogPadding = 24.dp
private val DialogCornerRadius = 16.dp
private val DialogElevation = 8.dp
private val DialogTitleBottomSpacing = 12.dp
private val DialogDescriptionBottomSpacing = 24.dp
private val DialogButtonsSpacing = 12.dp
private val DialogButtonMinWidth = 96.dp
private val DialogButtonHeight = 48.dp
private val DialogScrimAlpha = 0.7f
private val DialogShape = RoundedCornerShape(DialogCornerRadius)

@Composable
fun TvConfirmDialog(
    title: String,
    description: String?,
    primaryText: String,
    secondaryText: String?,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val primaryFocusRequester = remember { FocusRequester() }
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

    LaunchedEffect(isVisible) {
        if (isVisible) {
            primaryFocusRequester.requestFocus()
        }
    }

    Dialog(
        onDismissRequest = {
            closeThen(onDismiss)
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = DialogScrimAlpha)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(DialogAnimationDurationMs)) +
                    scaleIn(
                        initialScale = DialogEnterScale,
                        animationSpec = tween(DialogAnimationDurationMs)
                    ),
                exit = fadeOut(animationSpec = tween(DialogAnimationDurationMs)) +
                    scaleOut(
                        targetScale = DialogExitScale,
                        animationSpec = tween(DialogAnimationDurationMs)
                    )
            ) {
                Surface(
                    shape = DialogShape,
                    tonalElevation = DialogElevation,
                    colors = SurfaceDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .width(DialogPreferredWidth)
                        .widthIn(min = DialogMinWidth, max = DialogMaxWidth)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DialogPadding)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                        )

                        description?.takeIf { it.isNotBlank() }?.let { body ->
                            Text(
                                text = body,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 4,
                                modifier = Modifier.padding(top = DialogTitleBottomSpacing)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                DialogButtonsSpacing
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = DialogDescriptionBottomSpacing)
                        ) {
                            OutlinedButton(
                                onClick = { closeThen(onPrimary) },
                                modifier = Modifier
                                    .focusRequester(primaryFocusRequester)
                            ) {
                                Text(
                                    text = primaryText,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                )
                            }

                            secondaryText?.let { secondaryLabel ->
                                OutlinedButton(
                                    onClick = { closeThen(onSecondary) },
                                ) {
                                    Text(
                                        text = secondaryLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
