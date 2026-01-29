/*
 * CloudStream TV - Simple Media Grid (without paging for testing)
 */

package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.common.MovieCard

/**
 * Simple grid without paging for testing with old APIRepository
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SimpleMediaGrid(
    items: List<MediaItemCompat>,
    onMediaClick: (MediaItemCompat) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        items(items.size) { index ->
            val item = items[index]
            var isFocused by remember { mutableStateOf(false) }
            
            MovieCard(
                onClick = { onMediaClick(item) },
                modifier = Modifier
                    .aspectRatio(2f / 3f)  // Portrait poster ratio
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                    }
                    .border(
                        width = if (isFocused) 4.dp else 0.dp,
                        color = if (isFocused) Color.White else Color.Transparent
                    )
                    .let {
                        if (index == 0) it.focusRequester(focusRequester)
                        else it
                    }
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.posterUri)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
