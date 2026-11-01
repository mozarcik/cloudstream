package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionUiState
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatPanelItem

internal const val SkipDownloadLoadingActionId = -10_001

@Immutable
internal data class DetailsDownloadPanelContent(
    val items: List<MovieDetailsCompatPanelItem>,
    val showItemsWhileLoading: Boolean,
)

internal fun resolveDetailsDownloadPanelContent(
    state: DownloadMirrorSelectionUiState,
    skipLoadingItem: MovieDetailsCompatPanelItem,
): DetailsDownloadPanelContent {
    val selectionItems = state.selectionRequest?.options.orEmpty()
    val hasSelectionItems = selectionItems.isNotEmpty()
    val showSkipLoadingAction = state.isLoading &&
        state.loadedSourcesCount > 0 &&
        (!state.isLoadingUiSkipped || !hasSelectionItems)

    val items = when {
        showSkipLoadingAction -> listOf(skipLoadingItem)
        hasSelectionItems -> selectionItems
        else -> emptyList()
    }

    return DetailsDownloadPanelContent(
        items = items,
        showItemsWhileLoading = state.isLoading && items.isNotEmpty(),
    )
}
