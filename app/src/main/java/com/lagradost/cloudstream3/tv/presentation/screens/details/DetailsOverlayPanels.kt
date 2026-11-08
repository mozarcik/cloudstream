package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.ui.Alignment
import androidx.tv.material3.Icon
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.material.icons.Icons
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.icons.CustomDownload
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionUiState
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatPanelItem
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.presentation.screens.movies.BookmarkStatusSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieActionsSidePanel
import com.lagradost.cloudstream3.ui.WatchType

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
        val currentTitle = panelsStateHolder.currentActionSelection?.title
            ?: stringResource(R.string.episode_more_options_des)
        val currentItems = panelsStateHolder.currentActionSelection?.options.orEmpty()

        val selection = panelsStateHolder.currentActionSelection
        val seasonNumber = selection?.targetSeasonNumber
        val episodeNumber = selection?.targetEpisodeNumber
        // Series only: a movie has no episode, so its panel shows just the title (mockup: "Title • S3:E2" for series).
        val episodeEyebrow = if (episodeNumber != null && mode.isSeriesContent(details)) {
            val tag = if (seasonNumber != null) {
                "${stringResource(R.string.season_short)}$seasonNumber:${stringResource(R.string.episode_short)}$episodeNumber"
            } else {
                "${stringResource(R.string.episode_short)}$episodeNumber"
            }
            "${details.name} \u2022 $tag"
        } else {
            null
        }

        MovieActionsSidePanel(
            visible = panelsStateHolder.isActionsPanelVisible,
            loading = panelsStateHolder.isPanelLoading,
            inProgress = panelsStateHolder.isActionInProgress,
            title = currentTitle,
            eyebrow = episodeEyebrow,
            // Series without an episode eyebrow show the series name; a movie already has it as the title.
            subtitle = if (episodeEyebrow == null && mode.isSeriesContent(details)) details.name else null,
            items = currentItems,
            onCloseRequested = onCloseActionsPanel,
            onActionSelected = onActionSelected,
            rowHeight = TvDimens.PanelMenuRowHeight,
        )
    }

    val downloadPanelTitle = downloadMirrorState.selectionRequest?.title
        ?: stringResource(R.string.episode_action_download_mirror)
    val skipLoadingItem = MovieDetailsCompatPanelItem(
        id = SkipDownloadLoadingActionId,
        label = stringResource(R.string.skip_loading),
        iconRes = R.drawable.ic_baseline_fast_forward_24
    )
    val downloadPanelContent = resolveDetailsDownloadPanelContent(
        state = downloadMirrorState,
        skipLoadingItem = skipLoadingItem,
    )
    val linkCount = downloadMirrorState.selectionRequest?.options.orEmpty().count { !it.isSectionHeader }

    MovieActionsSidePanel(
        visible = downloadMirrorState.isVisible,
        loading = downloadMirrorState.isLoading,
        inProgress = panelsStateHolder.isActionInProgress,
        title = downloadPanelTitle,
        subtitle = details.name,
        rowHeight = TvDimens.PanelMirrorRowHeight,
        fixedRowHeight = true,
        // After Skip Loading the list is final: show the link count, not the loading header.
        titleTrailing = if ((!downloadMirrorState.isLoading || downloadMirrorState.isLoadingUiSkipped) && linkCount > 0) {
            stringResource(R.string.tv_details_links_count, linkCount)
        } else {
            null
        },
        itemTrailingIcon = Icons.Filled.CustomDownload.takeIf {
            downloadPanelContent.items.firstOrNull()?.id != SkipDownloadLoadingActionId
        },
        items = downloadPanelContent.items,
        onCloseRequested = onCloseDownloadPanel,
        onActionSelected = { actionId ->
            if (actionId == SkipDownloadLoadingActionId) {
                onSkipDownloadLoading()
            } else {
                onDownloadActionSelected(actionId)
            }
        },
        panelTestTag = mode.downloadPanelTestTag,
        showItemsWhileLoading = downloadPanelContent.showItemsWhileLoading,
        closeOnFocusExit = false,
        headerContent = {
            if (downloadMirrorState.isLoading && !downloadMirrorState.isLoadingUiSkipped) {
                DownloadLoadingHeader(
                    sourcesFound = stringResource(
                        R.string.tv_player_loading_sources_progress,
                        downloadMirrorState.loadedSourcesCount
                    ),
                    modifier = Modifier.padding(
                        horizontal = TvDimens.PanelTitleHorizontalPadding,
                        vertical = TvDimens.PanelSectionVerticalPadding,
                    ),
                )
            }
        },
        emptyContent = {
            if (downloadMirrorState.isLoading) {
                Text(
                    text = stringResource(R.string.loading),
                    style = TvTextStyles.RowPanel,
                    modifier = Modifier.padding(horizontal = TvDimens.PanelTitleHorizontalPadding, vertical = TvDimens.PanelSectionVerticalPadding),
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(TvDimens.RowGap),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = TvDimens.PanelTitleHorizontalPadding, vertical = TvDimens.PanelSectionVerticalPadding),
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.tv_empty_no_links),
                        contentDescription = null,
                        modifier = Modifier.size(TvDimens.StateIllustrationSize),
                    )
                    Text(
                        text = stringResource(R.string.no_links_found_toast),
                        style = TvTextStyles.RowPanel,
                    )
                }
            }
        }
    )

    if (mode.allowsBookmark) {
        BookmarkStatusSidePanel(
            visible = panelsStateHolder.isBookmarkPanelVisible,
            currentStatus = details.bookmarkLabelRes.toWatchType(),
            subtitle = details.name,
            onCloseRequested = onCloseBookmarkPanel,
            onBookmarkSelected = { selectedStatus ->
                onBookmarkSelected(selectedStatus)
                onCloseBookmarkPanel()
            },
            panelTestTag = mode.bookmarkPanelTestTag ?: "bookmark_side_panel"
        )
    }
}

/** "Sources found: N" with a static ring while links load (no spinning, no animation). */
@Composable
private fun DownloadLoadingHeader(sourcesFound: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TvDimens.RowGap),
        ) {
            DownloadProgressRing(
                progress = LoadingRingFraction,
                accent = MaterialTheme.colorScheme.primary,
                track = MaterialTheme.colorScheme.onSurface,
                size = TvDimens.RowIconSize,
            )
            Text(text = stringResource(R.string.loading), style = TvTextStyles.RowPanel)
        }
        Text(
            text = sourcesFound,
            style = TvTextStyles.RowSubtitle,
            color = TvColors.Text2,
            modifier = Modifier.padding(top = TvDimens.PanelSupportingTopPadding),
        )
    }
}

private const val LoadingRingFraction = 0.25f
