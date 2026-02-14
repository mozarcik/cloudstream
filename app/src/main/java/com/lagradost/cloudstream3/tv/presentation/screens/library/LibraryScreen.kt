package com.lagradost.cloudstream3.tv.presentation.screens.library

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.screens.home.FeedSection
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeedLoadState

private const val LIBRARY_PLACEHOLDER_COUNT = 4

@Composable
fun LibraryScreen(
    onMediaClick: (MediaItemCompat) -> Unit,
    onOpenFeedGrid: (LibrarySectionUiState) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val firstFeedCardFocusRequester = remember { FocusRequester() }

    val loadingLabel = stringResource(id = R.string.loading)
    val emptyLibraryLabel = stringResource(id = R.string.empty_library_logged_in_message)
    val noLibraryAccountsLabel = stringResource(id = R.string.empty_library_no_accounts_message)

    LaunchedEffect(Unit) {
        onScroll(true)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C1016))
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
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
                            color = Color.White.copy(alpha = 0.78f)
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
                            state = HomeFeedLoadState.Success(section.items),
                            onMediaClick = onMediaClick,
                            onShowMoreClick = {
                                onOpenFeedGrid(section)
                            },
                            isInteractive = true,
                            firstItemFocusRequester = if (index == 0) {
                                firstFeedCardFocusRequester
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
    }
}
