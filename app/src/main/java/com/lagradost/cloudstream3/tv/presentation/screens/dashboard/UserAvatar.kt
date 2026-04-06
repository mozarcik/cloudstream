package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.tv.material3.Border
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.SelectableSurfaceDefaults
import androidx.tv.material3.Surface
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamBorderWidth

@Composable
fun UserAvatar(
    selected: Boolean,
    customImageUrl: String? = null,
    fallbackDrawableRes: Int? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val normalizedCustomImageUrl = customImageUrl?.trim().takeUnless { it.isNullOrBlank() }
    Surface(
        selected = selected,
        onClick = onClick,
        shape = SelectableSurfaceDefaults.shape(shape = CircleShape),
        border = SelectableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(
                    width = CloudStreamBorderWidth,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                shape = CircleShape
            ),
            selectedBorder = Border(
                border = BorderStroke(
                    width = CloudStreamBorderWidth,
                    color = MaterialTheme.colorScheme.primary
                ),
                shape = CircleShape
            ),
        ),
        scale = SelectableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = modifier,
    ) {
        when {
            normalizedCustomImageUrl != null -> {
                AsyncImage(
                    model = normalizedCustomImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            fallbackDrawableRes != null -> {
                Image(
                    painter = painterResource(fallbackDrawableRes),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            else -> {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
