package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    private var actionPanelStack by mutableStateOf<List<MovieDetailsCompatSelectionRequest>>(emptyList())
        private set
    val currentActionSelection: MovieDetailsCompatSelectionRequest?
        get() = actionPanelStack.lastOrNull()

    fun openActionsPanel() {
        isActionsPanelVisible = true
        actionPanelStack = emptyList()
    }

    fun closeActionsPanel() {
        actionPanelStack = emptyList()
        isActionsPanelVisible = false
    }

    fun navigateActionsBack() {
        if (actionPanelStack.size > 1) {
            actionPanelStack = actionPanelStack.dropLast(1)
            return
        }
        closeActionsPanel()
    }

    fun showRootActionSelection(request: MovieDetailsCompatSelectionRequest) {
        actionPanelStack = listOf(request)
        isActionsPanelVisible = true
    }

    fun showActionSelection(request: MovieDetailsCompatSelectionRequest) {
        actionPanelStack = if (actionPanelStack.isEmpty()) {
            listOf(request)
        } else {
            actionPanelStack + request
        }
        isActionsPanelVisible = true
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
        actionPanelStack = emptyList()
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
