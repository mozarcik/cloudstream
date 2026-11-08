package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.tv.material3.Icon
import coil3.compose.AsyncImage

/** Picture of the current profile, shown by the (non-focusable) rail and by the menu header. */
@Composable
fun AvatarImage(
    customImageUrl: String?,
    fallbackDrawableRes: Int?,
    modifier: Modifier = Modifier,
) {
    val normalizedCustomImageUrl = customImageUrl?.trim().takeUnless { it.isNullOrBlank() }
    when {
        normalizedCustomImageUrl != null -> {
            AsyncImage(
                model = normalizedCustomImageUrl,
                contentDescription = null,
                modifier = modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        fallbackDrawableRes != null -> {
            Image(
                painter = painterResource(fallbackDrawableRes),
                contentDescription = null,
                modifier = modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        else -> {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = modifier.fillMaxSize(),
            )
        }
    }
}
