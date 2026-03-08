package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionUiState
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatPanelItem
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.presentation.screens.movies.BookmarkStatusSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieActionsSidePanel
import com.lagradost.cloudstream3.ui.WatchType

private const val SkipDownloadLoadingActionId = -10_001

@Composable
internal fun DetailsOverlayPanels(
    mode: DetailsScreenMode,
    details: MovieDetails,
    panelsStateHolder: DetailsPanelsStateHolder,
    downloadMirrorState: DownloadMirrorSelectionUiState,
    onActionSelected: (Int) -> Unit,
    onDownloadActionSelected: (Int) -> Unit,
    onCloseActionsPanel: () -> Unit,
    onCloseDownloadPanel: () -> Unit,
    onSkipDownloadLoading: () -> Unit,
    onCloseBookmarkPanel: () -> Unit,
    onBookmarkSelected: (WatchType) -> Unit,
) {
    if (mode.allowsExtendedActions) {
        val currentTitle = panelsStateHolder.panelSelection?.title
            ?: stringResource(R.string.episode_more_options_des)
        val currentItems = panelsStateHolder.panelSelection?.options
            ?: panelsStateHolder.panelItems

        MovieActionsSidePanel(
            visible = panelsStateHolder.isActionsPanelVisible,
            loading = panelsStateHolder.isPanelLoading,
            inProgress = panelsStateHolder.isActionInProgress,
            title = currentTitle,
            items = currentItems,
            onCloseRequested = onCloseActionsPanel,
            onActionSelected = onActionSelected,
        )
    }

    val downloadPanelTitle = downloadMirrorState.selectionRequest?.title
        ?: stringResource(R.string.episode_action_download_mirror)
    val downloadPanelItems = downloadMirrorState.selectionRequest?.options ?: emptyList()
    val skipLoadingItem = MovieDetailsCompatPanelItem(
        id = SkipDownloadLoadingActionId,
        label = stringResource(R.string.skip_loading),
        iconRes = R.drawable.ic_baseline_fast_forward_24
    )
    val showSkipLoadingAction = downloadMirrorState.isLoading &&
        !downloadMirrorState.isLoadingUiSkipped &&
        downloadMirrorState.loadedSourcesCount > 0
    val downloadPanelActionItems = when {
        showSkipLoadingAction -> listOf(skipLoadingItem)
        else -> downloadPanelItems
    }

    MovieActionsSidePanel(
        visible = downloadMirrorState.isVisible,
        loading = downloadMirrorState.isLoading,
        inProgress = panelsStateHolder.isActionInProgress,
        title = downloadPanelTitle,
        items = downloadPanelActionItems,
        onCloseRequested = onCloseDownloadPanel,
        onActionSelected = { actionId ->
            if (actionId == SkipDownloadLoadingActionId) {
                onSkipDownloadLoading()
            } else {
                onDownloadActionSelected(actionId)
            }
        },
        panelTestTag = mode.downloadPanelTestTag,
        showItemsWhileLoading = downloadMirrorState.loadedSourcesCount > 0,
        headerContent = {
            if (downloadMirrorState.isLoading) {
                Text(
                    text = stringResource(
                        R.string.tv_player_loading_sources_progress,
                        downloadMirrorState.loadedSourcesCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        },
        emptyContent = {
            if (downloadMirrorState.isLoading) {
                Text(
                    text = stringResource(R.string.loading),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.no_links_found_toast),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    )

    if (mode.allowsBookmark) {
        BookmarkStatusSidePanel(
            visible = panelsStateHolder.isBookmarkPanelVisible,
            currentStatus = details.bookmarkLabelRes.toWatchType(),
            onCloseRequested = onCloseBookmarkPanel,
            onBookmarkSelected = { selectedStatus ->
                onBookmarkSelected(selectedStatus)
                onCloseBookmarkPanel()
            },
            panelTestTag = mode.bookmarkPanelTestTag ?: "bookmark_side_panel"
        )
    }
}
