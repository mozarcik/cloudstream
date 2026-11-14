package com.lagradost.cloudstream3.tv.presentation.screens.search

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.focus.TvBackHandler
import com.lagradost.cloudstream3.tv.presentation.focus.TvScreenFocusEffect
import com.lagradost.cloudstream3.tv.presentation.focus.isItemComposed
import com.lagradost.cloudstream3.tv.presentation.focus.tryRequestFocus
import com.lagradost.cloudstream3.tv.presentation.focus.requestFocusWithRetry
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeedLoadState
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

@Composable
fun SearchScreen(
    onMediaClick: (MediaItemCompat) -> Unit,
    onOpenFeedGrid: (SearchSectionUiState) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(),
) {
    val searchNavLogTag = "TvSearchNav"
    val uiState by viewModel.uiState.collectAsState()
    val pendingPrefillQuery by SearchPrefillStore.pendingQuery.collectAsState()
    val listState = rememberLazyListState()
    val pillFocusRequester = remember { FocusRequester() }
    val textFocusRequester = remember { FocusRequester() }
    val firstResultFocusRequester = remember { FocusRequester() }

    // A source with no results is left out; failed ones stay (they show an error row).
    val sections = remember(uiState.sections) {
        uiState.sections.filterNot { it.state is HomeFeedLoadState.Success && it.items.isEmpty() }
    }
    // Down from the field lands on the first source in order; error rows are focusable too.
    val firstFocusableIndex = remember(sections) {
        sections.indexOfFirst { it.isInteractive || it.state is HomeFeedLoadState.Error }
    }

    // Fresh entry: the field. Coming back (menu closed, Back from a title / "Show more" grid): the row that had
    // focus, on its remembered poster; the field when focus was never in the results.
    TvScreenFocusEffect(
        enter = { pillFocusRequester.tryRequestFocus() },
        restore = {
            val sectionIndex = sections.indexOfFirst { it.id == viewModel.lastFocusedSectionId }
            if (sectionIndex < 0) {
                pillFocusRequester.tryRequestFocus()
            } else {
                // Item 0 is the field, the sections follow it.
                if (!listState.isItemComposed(sectionIndex + 1)) listState.scrollToItem(sectionIndex + 1)
                viewModel.sectionGroup(sections[sectionIndex].id).requestFocusWithRetry()
            }
        },
    )

    // Results arrive after the IME "search" action, so moving focus down right away is a no-op.
    var focusResultsPending by remember { mutableStateOf(false) }
    val hasResultSection = sections.any { it.isInteractive || it.state is HomeFeedLoadState.Error }
    LaunchedEffect(focusResultsPending, firstFocusableIndex, hasResultSection, uiState.isLoading) {
        if (focusResultsPending && firstFocusableIndex >= 0 && (hasResultSection || !uiState.isLoading)) {
            focusResultsPending = false
            firstResultFocusRequester.requestFocusWithRetry()
        }
    }

    LaunchedEffect(pendingPrefillQuery) {
        val query = pendingPrefillQuery?.trim().orEmpty()
        if (query.isBlank()) return@LaunchedEffect

        Log.d(searchNavLogTag, "search screen consuming prefill query=$query")
        viewModel.onQueryChanged(query)
        viewModel.onSearchSubmitted()
        SearchPrefillStore.clearPendingQuery()
        pillFocusRequester.requestFocusWithRetry()
    }

    // Back from the results goes to the field first; Back again leaves the screen.
    var contentHasFocus by remember { mutableStateOf(false) }
    var pillHasFocus by remember { mutableStateOf(false) }
    // Composed only while active: the handler is then registered after the Dashboard's own
    // Back handler (which sends focus to the top bar) and takes precedence over it.
    if (contentHasFocus && !pillHasFocus) {
        TvBackHandler { pillFocusRequester.requestFocus() }
    }

    val summary = when {
        !uiState.hasSearched || uiState.sections.isEmpty() -> null
        uiState.isLoading -> {
            val loading = uiState.sections.count { it.state is HomeFeedLoadState.Loading }
            pluralStringResource(R.plurals.tv_search_searching_sources, loading, loading)
        }

        else -> {
            val results = uiState.sections.sumOf { it.items.size }
            val sourceCount = uiState.sections.size
            pluralStringResource(R.plurals.tv_search_sources_count, sourceCount, sourceCount) +
                " • " + pluralStringResource(R.plurals.tv_search_results_count, results, results)
        }
    }
    val showEmpty = uiState.hasSearched && !uiState.isLoading && sections.isEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .onFocusChanged { contentHasFocus = it.hasFocus }
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = TvDimens.SearchFieldTop, bottom = TvDimens.SearchListBottomPadding),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "field") {
                SearchField(
                    value = uiState.query,
                    onValueChange = { value ->
                        // The IME re-commits the same text when it submits, which must not
                        // cancel the pending move to the results.
                        if (value != uiState.query) focusResultsPending = false
                        viewModel.onQueryChanged(value)
                    },
                    onSearchAction = {
                        viewModel.onSearchSubmitted()
                        focusResultsPending = true
                    },
                    pillFocusRequester = pillFocusRequester,
                    textFocusRequester = textFocusRequester,
                    downFocusRequester = if (firstFocusableIndex >= 0) firstResultFocusRequester else null,
                    onPillFocusChanged = { pillHasFocus = it },
                    summary = summary,
                    modifier = Modifier.padding(start = TvDimens.ContentStartWithRail),
                )
            }
            items(items = sections, key = { it.id }) { section ->
                val isFirstFocusable = sections.getOrNull(firstFocusableIndex)?.id == section.id
                SearchSection(
                    section = section,
                    onMediaClick = onMediaClick,
                    onShowMoreClick = {
                        SearchFeedGridSelectionStore.setQuery(uiState.submittedQuery)
                        onOpenFeedGrid(section)
                    },
                    group = viewModel.sectionGroup(section.id),
                    entryRequester = if (isFirstFocusable) firstResultFocusRequester else null,
                    onRowFocused = { viewModel.lastFocusedSectionId = section.id },
                    upFocusRequester = if (section.id == sections.firstOrNull()?.id) pillFocusRequester else null,
                )
            }
        }

        if (!uiState.hasSearched || showEmpty) {
            SearchStateMessage(
                illustration = if (showEmpty) R.drawable.tv_empty_no_results else R.drawable.tv_empty_search_start,
                title = stringResource(
                    if (showEmpty) R.string.tv_feed_empty else R.string.title_search
                ),
                message = if (showEmpty) {
                    stringResource(
                        R.string.tv_search_no_results_message,
                        uiState.sections.size,
                        uiState.submittedQuery,
                    )
                } else {
                    stringResource(R.string.tv_search_start_message)
                },
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

/** Start and "no results" text: not focusable, the field above stays the only focus target. */
@Composable
private fun SearchStateMessage(
    @androidx.annotation.DrawableRes illustration: Int,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(top = TvDimens.SearchStateTop),
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(illustration),
            contentDescription = null,
            modifier = Modifier.size(TvDimens.StateIllustrationSize),
        )
        Text(
            text = title,
            style = TvTextStyles.StateTitle,
            color = TvColors.Text,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = TvTextStyles.StateMessage,
            color = TvColors.Text2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = TvDimens.StateMessageGap),
        )
    }
}
