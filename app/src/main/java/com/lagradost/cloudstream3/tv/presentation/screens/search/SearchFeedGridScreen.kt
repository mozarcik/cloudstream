package com.lagradost.cloudstream3.tv.presentation.screens.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.screens.grid.TvShowMoreGridScreen
import kotlinx.coroutines.flow.flowOf

/** Search "Show more": the shared grid showing the results one source already returned. */
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

    val pagingItems = remember(section.items) {
        flowOf(PagingData.from(section.items))
    }.collectAsLazyPagingItems()

    TvShowMoreGridScreen(
        title = section.title,
        subtitle = SearchFeedGridSelectionStore.query.takeIf { it.isNotBlank() }?.let { "“$it”" },
        pagingItems = pagingItems,
        focusStore = SearchFeedGridSelectionStore.focus,
        onMediaClick = onMediaClick,
        screenKey = section.id,
        // The search tab has no restore token of its own; a pending target only exists after
        // the user opened a title from this grid.
        restoreFocusToken = 1,
    )
}
