package com.lagradost.cloudstream3.tv.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lagradost.cloudstream3.tv.data.util.StringConstants
import com.lagradost.cloudstream3.tv.presentation.utils.ourColors
import coil.compose.AsyncImage

/**
 * Individual row component for settings items.
 * Used in both interactive (left panel) and non-interactive (right panel preview) modes.
 */
@Composable
fun SettingsRow(
    node: SettingsNode,
    isSelected: Boolean,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }

    val textColor = if (isEnabled) {
        if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    val descriptionColor = if (isEnabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    val iconTint = if (isEnabled) {
        if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    // Content lambda - same for both enabled/disabled
    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Icon rendering with fallback support
            android.util.Log.d("SettingsRow", "${node.title}: icon=${node.icon != null}, iconUrl=null?${node.iconUrl == null}, fallback=${node.fallbackIconRes}")
            
            when {
                node.icon != null -> {
                    android.util.Log.d("SettingsRow", "${node.title}: Using ImageVector icon")
                    Icon(
                        imageVector = node.icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                !node.iconUrl.isNullOrBlank() -> {
                    android.util.Log.d("SettingsRow", "${node.title}: Using AsyncImage from URL")
                    AsyncImage(
                        modifier = Modifier
                            .size(24.dp)
                            .then(
                                if (!isEnabled) Modifier.alpha(0.6f) else Modifier
                            ),
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(node.iconUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "setting node icon",
                        contentScale = ContentScale.Fit,
                        error = if (node.fallbackIconRes != null) {
                            painterResource(node.fallbackIconRes)
                        } else null
                    )
                }
                node.fallbackIconRes != null -> {
                    android.util.Log.d("SettingsRow", "${node.title}: Using fallback icon ${node.fallbackIconRes}")
                    // Use Image instead of Icon to avoid tint conflicts with vector drawable
                    androidx.compose.foundation.Image(
                        painter = painterResource(node.fallbackIconRes),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        alpha = if (!isEnabled) 0.6f else 1f
                    )
                }
            }


            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )
                node.description?.let { desc ->
                    Text(
                        text = desc,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = descriptionColor
                    )
                }
            }
        }
    }

    // For non-interactive items (preview), use plain Box without Surface background
    if (!isEnabled) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    // For interactive items, use Surface with focus handling and background
    val backgroundColor = when {
        isFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        isSelected -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                onFocusChanged(focusState.isFocused)
            },
        enabled = true,
        colors = ClickableSurfaceDefaults.colors(
            containerColor = backgroundColor,
            focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ),
        shape = ClickableSurfaceDefaults.shape(
            shape = RoundedCornerShape(8.dp)
        )
    ) {
        content()
    }
}
