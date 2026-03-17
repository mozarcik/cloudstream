package com.lagradost.cloudstream3.tv.presentation.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.common.HaloHost
import com.lagradost.cloudstream3.tv.presentation.screens.home.FeedSection
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeedLoadState

private const val LIBRARY_PLACEHOLDER_COUNT = 4

@Composable
fun LibraryScreen(
    onMediaClick: (MediaItemCompat) -> Unit,
    onOpenFeedGrid: (LibrarySectionUiState) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    topBarFocusRequester: FocusRequester,
    restoreFocusToken: Int = 0,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val firstFeedCardFocusRequester = remember { FocusRequester() }
    val sourceRowEntryFocusRequester = remember { FocusRequester() }

    val loadingLabel = stringResource(id = R.string.loading)
    val emptyLibraryLabel = stringResource(id = R.string.empty_library_logged_in_message)
    val noLibraryAccountsLabel = stringResource(id = R.string.empty_library_no_accounts_message)
    val pendingRestoreTargetId = LibraryFocusStore.pendingRestoreTargetId
    val showSourceSelector = uiState.availableSources.size > 1
    val selectedSourceName = uiState.currentApiName.ifBlank {
        uiState.availableSources.firstOrNull().orEmpty()
    }

    LaunchedEffect(Unit) {
        onScroll(true)
    }

    val openDetailsWithRestore: (MediaItemCompat) -> Unit = { item ->
        LibraryFocusStore.scheduleRestoreToLastFocused()
        onMediaClick(item)
    }

    val openFeedGridWithRestore: (LibrarySectionUiState) -> Unit = { section ->
        LibraryFocusStore.scheduleRestoreToLastFocused()
        onOpenFeedGrid(section)
    }

    HaloHost(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (showSourceSelector) {
                    item(key = "library_source_selector") {
                        LibrarySourceSelectorRow(
                            sources = uiState.availableSources,
                            selectedSource = selectedSourceName,
                            rowEntryFocusRequester = sourceRowEntryFocusRequester,
                            isInteractive = true,
                            upFocusRequester = topBarFocusRequester,
                            downFocusRequester = firstFeedCardFocusRequester
                                .takeIf { uiState.sections.isNotEmpty() },
                            onSourceSelected = viewModel::switchSource,
                        )
                    }
                }

                when {
                    uiState.isLoading -> {
                        items(count = LIBRARY_PLACEHOLDER_COUNT) {
                            FeedSection(
                                title = loadingLabel,
                                state = HomeFeedLoadState.Loading,
                                onMediaClick = {},
                                onShowMoreClick = {},
                                isInteractive = false,
                            )
                        }
                    }

                    uiState.sections.isEmpty() -> {
                        item {
                            val message = when {
                                !uiState.isAnySyncApiAvailable -> noLibraryAccountsLabel
                                !uiState.errorMessage.isNullOrBlank() -> uiState.errorMessage
                                else -> emptyLibraryLabel
                            }

                            Text(
                                text = message.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        itemsIndexed(
                            items = uiState.sections,
                            key = { _, section -> section.id }
                        ) { index, section ->
                            FeedSection(
                                title = section.title,
                                state = HomeFeedLoadState.Success(section.previewItems),
                                onMediaClick = openDetailsWithRestore,
                                onShowMoreClick = {
                                    openFeedGridWithRestore(section)
                                },
                                isInteractive = true,
                                firstItemFocusRequester = if (index == 0) {
                                    firstFeedCardFocusRequester
                                } else {
                                    null
                                },
                                upFocusRequester = if (index == 0) {
                                    if (showSourceSelector) {
                                        sourceRowEntryFocusRequester
                                    } else {
                                        topBarFocusRequester
                                    }
                                } else {
                                    null
                                },
                                sectionFocusKey = "library:section:${section.id}",
                                pendingRestoreFocusTargetId = pendingRestoreTargetId,
                                restoreFocusToken = restoreFocusToken,
                                onItemFocused = { item ->
                                    LibraryFocusStore.onTargetFocused(
                                        LibraryFocusStore.feedPoster(section.id, item)
                                    )
                                },
                                onShowMoreFocused = {
                                    LibraryFocusStore.onTargetFocused(
                                        LibraryFocusStore.feedShowMore(section.id)
                                    )
                                },
                                onRestoreFocusConsumed = { targetId ->
                                    LibraryFocusStore.clearPendingRestore(targetId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
