package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatPanelItem
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatSelectionRequest

@Stable
internal class DetailsPanelsStateHolder {
    var isActionsPanelVisible by mutableStateOf(false)
        private set
    var isBookmarkPanelVisible by mutableStateOf(false)
        private set
    var isActionInProgress by mutableStateOf(false)
        private set
    var isPanelLoading by mutableStateOf(false)
        private set
    var panelItems by mutableStateOf<List<MovieDetailsCompatPanelItem>>(emptyList())
        private set
    var panelSelection by mutableStateOf<MovieDetailsCompatSelectionRequest?>(null)
        private set

    fun openActionsPanel() {
        isActionsPanelVisible = true
        panelSelection = null
        panelItems = emptyList()
    }

    fun closeActionsPanel() {
        panelSelection = null
        isActionsPanelVisible = false
    }

    fun navigateActionsBack() {
        if (panelSelection != null) {
            panelSelection = null
            return
        }
        closeActionsPanel()
    }

    fun showActionSelection(request: MovieDetailsCompatSelectionRequest) {
        panelSelection = request
        isActionsPanelVisible = true
    }

    fun updatePanelItems(items: List<MovieDetailsCompatPanelItem>) {
        panelItems = items
    }

    fun updatePanelLoading(isLoading: Boolean) {
        isPanelLoading = isLoading
    }

    fun updateActionInProgress(inProgress: Boolean) {
        isActionInProgress = inProgress
    }

    fun openBookmarkPanel() {
        isBookmarkPanelVisible = true
    }

    fun closeBookmarkPanel() {
        isBookmarkPanelVisible = false
    }

    fun resetTransientState() {
        closeActionsPanel()
        closeBookmarkPanel()
        isActionInProgress = false
        isPanelLoading = false
        panelItems = emptyList()
    }
}

@Composable
internal fun rememberDetailsPanelsStateHolder(
    detailsId: String,
    mode: DetailsScreenMode,
): DetailsPanelsStateHolder {
    return remember(detailsId, mode) {
        DetailsPanelsStateHolder()
    }
}
