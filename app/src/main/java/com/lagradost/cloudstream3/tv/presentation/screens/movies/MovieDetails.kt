package com.lagradost.cloudstream3.tv.presentation.screens.movies

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.lagradost.cloudstream3.tv.presentation.utils.Padding

val ParentPadding = PaddingValues(vertical = 16.dp, horizontal = 58.dp)

/** Actions of the details hero and of an episode row. */
enum class MovieDetailsQuickAction {
    Bookmark,
    Favorite,
    Search,
    Download,
    More,
    MarkAsWatched,
    MarkWatchedUpToThisEpisode,
    RemoveFromWatched,
    RemoveWatchedUpToThisEpisode,
}

sealed interface MovieDetailsDownloadActionState {
    data object Idle : MovieDetailsDownloadActionState
    data class Downloading(val progress: Float) : MovieDetailsDownloadActionState
    data object Downloaded : MovieDetailsDownloadActionState
    data object Failed : MovieDetailsDownloadActionState
}

@Composable
fun rememberChildPadding(direction: LayoutDirection = LocalLayoutDirection.current): Padding {
    return remember {
        Padding(
            start = ParentPadding.calculateStartPadding(direction) + 8.dp,
            top = ParentPadding.calculateTopPadding(),
            end = ParentPadding.calculateEndPadding(direction) + 8.dp,
            bottom = ParentPadding.calculateBottomPadding()
        )
    }
}
