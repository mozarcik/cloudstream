package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamCardShape
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamSurfaceDefaults

@Composable
internal fun ContinueWatchingHeroCard(
    item: MediaItemCompat,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    isInteractive: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val progress = (item.continueWatchingProgress ?: 0f).coerceIn(0f, 1f)
    val imageRequest = rememberContinueWatchingImageRequest(item.posterUri)

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = CloudStreamCardShape),
        scale = CloudStreamSurfaceDefaults.scale(),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
                ),
                shape = CloudStreamCardShape
            )
        ),
        colors = CloudStreamSurfaceDefaults.colors(),
        modifier = modifier
            .width(ContinueWatchingCardWidth)
            .height(ContinueWatchingCardHeight)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onFocused()
                }
            }
            .onPreviewKeyEvent { event ->
                if (!isInteractive || event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }

                if (event.key == Key.DirectionDown) {
                    onMoveDown()
                    true
                } else {
                    false
                }
            }
            .focusProperties {
                canFocus = isInteractive
                up = upFocusRequester
                down = downFocusRequester
            }
            .focusRequester(focusRequester)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (isFocused) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.08f))
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.52f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}
