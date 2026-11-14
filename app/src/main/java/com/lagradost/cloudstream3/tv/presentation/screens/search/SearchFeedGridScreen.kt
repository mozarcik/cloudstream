package com.lagradost.cloudstream3.tv.presentation.screens.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.paging.compose.collectAsLazyPagingItems
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.screens.grid.TvShowMoreGridScreen

/** Search "Show more": the shared grid with one source's results, loaded page by page. */
@Composable
fun SearchFeedGridScreen(
    onMediaClick: (MediaItemCompat) -> Unit,
    onBack: () -> Unit,
) {
    val selectedSection by SearchFeedGridSelectionStore.selectedSection.collectAsState()
    val section = selectedSection
    if (section == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val pagingItems = remember(section) { SearchFeedGridSelectionStore.pagingData }.collectAsLazyPagingItems()

    TvShowMoreGridScreen(
        title = section.title,
        subtitle = SearchFeedGridSelectionStore.query.takeIf { it.isNotBlank() }?.let { "“$it”" },
        pagingItems = pagingItems,
        focusStore = SearchFeedGridSelectionStore.focus,
        onMediaClick = onMediaClick,
        screenKey = section.id,
    )
}
