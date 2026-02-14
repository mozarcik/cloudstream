package com.lagradost.cloudstream3.tv.presentation.screens.library

import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat

data class LibrarySectionUiState(
    val id: String,
    val title: String,
    val items: List<MediaItemCompat>,
)
