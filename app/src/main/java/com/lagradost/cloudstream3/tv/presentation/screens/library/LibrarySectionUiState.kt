package com.lagradost.cloudstream3.tv.presentation.screens.library

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import kotlinx.collections.immutable.PersistentList

@Immutable
data class LibrarySectionUiState(
    val id: String,
    val title: String,
    val items: PersistentList<MediaItemCompat>,
)
