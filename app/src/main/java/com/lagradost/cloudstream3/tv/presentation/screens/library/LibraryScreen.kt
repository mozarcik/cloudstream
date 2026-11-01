@file:OptIn(ExperimentalFoundationApi::class)

package com.lagradost.cloudstream3.tv.presentation.screens.library

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.common.TvButton
import com.lagradost.cloudstream3.tv.presentation.common.TvSkeletonBlock
import com.lagradost.cloudstream3.tv.presentation.focus.requestFocusWithRetry
import com.lagradost.cloudstream3.tv.presentation.screens.dashboard.LocalDashboardContentEntry
import com.lagradost.cloudstream3.tv.presentation.screens.home.FEED_GRID_COLUMNS
import com.lagradost.cloudstream3.tv.presentation.screens.home.FEED_GRID_SIZE
import com.lagradost.cloudstream3.tv.presentation.screens.home.FEED_VISIBLE_ITEMS_BEFORE_SHOW_MORE
import com.lagradost.cloudstream3.tv.presentation.screens.home.FeedMiniGridPlaceholder
import com.lagradost.cloudstream3.tv.presentation.screens.home.FeedSection
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeContentHorizontalPadding
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeedLoadState
import com.lagradost.cloudstream3.tv.presentation.screens.home.feedEntrySlot
import com.lagradost.cloudstream3.tv.presentation.screens.home.feedSectionHeight
import com.lagradost.cloudstream3.tv.presentation.screens.home.feedSlotTargetId
import com.lagradost.cloudstream3.tv.presentation.screens.home.resolveFeedMiniGridConfig
import com.lagradost.cloudstream3.tv.presentation.screens.home.slideToItem
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val LIBRARY_PLACEHOLDER_COUNT = 2
/** Longer than the navigation transition that keeps the previous screen composed. */
private const val TopBarReHideDelayMs = 600L

/** The screen pins sections itself (see [LibraryScreen]); focus must not scroll the list on its own. */
private val LibraryBringIntoViewSpec = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}

/**
 * Library: source row ("LISTS FROM" + one chip per sync account) and one 2x6 mini-grid per list.
 * Like Home, the focused list is pinned under the top edge (150 ms) and the top bar hides then.
 */
@Composable
fun LibraryScreen(
    onMediaClick: (MediaItemCompat) -> Unit,
    onOpenFeedGrid: (LibrarySectionUiState) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    topBarFocusRequester: FocusRequester,
    onOpenAccountsSettings: () -> Unit = {},
    restoreFocusToken: Int = 0,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val firstFeedCardFocusRequester = remember { FocusRequester() }
    val sourceRowEntryFocusRequester = remember { FocusRequester() }
    val accountsButtonFocusRequester = remember { FocusRequester() }

    // A source whose lists are all empty gets the "This list is empty" state instead of empty grids.
    val sections: List<LibrarySectionUiState> = uiState.sections
        .takeUnless { all -> all.all { it.previewItems.isEmpty() } }
        .orEmpty()
    val showSourceSelector = uiState.availableSources.isNotEmpty()
    val selectedSourceName = uiState.currentApiName.ifBlank {
        uiState.availableSources.firstOrNull().orEmpty()
    }

    val shouldShowTopBar by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 100
        }
    }
    LaunchedEffect(shouldShowTopBar) { onScroll(shouldShowTopBar) }

    // Focus target pushed into the grids: a neighbouring section (Up/Down) or the poster that was
    // open before navigating away. The list is scrolled first so the target is composed.
    var focusTargetId by remember { mutableStateOf<String?>(null) }
    var focusToken by remember { mutableIntStateOf(0) }
    var handledRestoreToken by rememberSaveable { mutableIntStateOf(0) }
    var pinJob by remember { mutableStateOf<Job?>(null) }

    fun sectionListIndex(sectionIndex: Int) = (if (showSourceSelector) 1 else 0) + sectionIndex

    fun pinSectionToTop(sectionIndex: Int, row: Int) {
        // Asserted on every focus (not only on change): the Dashboard shows the bar again when
        // returning from details / "Show more", which would leave it over a pinned list.
        onScroll(sectionIndex == 0 && row == 0)
        pinJob?.cancel()
        pinJob = coroutineScope.launch {
            if (sectionIndex == 0 && row == 0) {
                // First row of the first list: back to the unscrolled screen with the source row.
                if (!listState.isScrolledToTop()) {
                    listState.scrollToItem(0)
                }
                return@launch
            }
            val item = listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == sectionListIndex(sectionIndex) }
                ?: return@launch
            val delta = (item.offset - listState.layoutInfo.viewportStartOffset).toFloat()
            if (abs(delta) > 1f) {
                listState.animateScrollBy(delta, tween(durationMillis = TvDimens.MaxAnimationMs))
            }
        }
    }

    fun openGridWithRestore(section: LibrarySectionUiState) {
        LibraryFocusStore.scheduleRestoreToLastFocused()
        LibraryFeedGridSelectionStore.setAccountName(selectedSourceName)
        onOpenFeedGrid(section)
    }

    fun openDetailsWithRestore(item: MediaItemCompat) {
        LibraryFocusStore.scheduleRestoreToLastFocused()
        onMediaClick(item)
    }

    LaunchedEffect(restoreFocusToken, sections) {
        if (restoreFocusToken <= 0 || restoreFocusToken == handledRestoreToken) return@LaunchedEffect
        val target = LibraryFocusStore.pendingRestoreTargetId ?: return@LaunchedEffect
        val sectionIndex = sections.indexOfFirst { target.startsWith("library:section:${it.id}:") }
        if (sectionIndex < 0) return@LaunchedEffect
        handledRestoreToken = restoreFocusToken
        focusTargetId = target
        listState.scrollToItem(if (sectionIndex == 0) 0 else sectionListIndex(sectionIndex))
        focusToken += 1
        // The bar comes back with the navigation transition; hide it again for a pinned list.
        repeat(2) {
            delay(TopBarReHideDelayMs)
            if (!listState.isScrolledToTop()) onScroll(false)
        }
    }

    // Up/Down out of a mini-grid. The neighbouring list may not be composed (the list is pinned),
    // so it is scrolled into place first and focus goes to the same column.
    fun navigateFromSection(sectionIndex: Int, up: Boolean, column: Int): Boolean {
        val target = sections.getOrNull(if (up) sectionIndex - 1 else sectionIndex + 1)
        if (target == null) {
            if (!up) return false
            coroutineScope.launch {
                listState.slideToItem(0)
                val requester = if (showSourceSelector) sourceRowEntryFocusRequester else topBarFocusRequester
                requester.requestFocusWithRetry(attempts = 18)
            }
            return true
        }
        val config = resolveFeedMiniGridConfig(target.previewItems)
        if (config.totalSlots == 0) return false
        val slot = feedEntrySlot(config, fromAbove = !up, column = column)
        focusTargetId = feedSlotTargetId("library:section:${target.id}", config, slot)
        coroutineScope.launch {
            val targetIndex = sections.indexOf(target)
            listState.slideToItem(if (targetIndex == 0) 0 else sectionListIndex(targetIndex))
            focusToken += 1
        }
        return true
    }

    // Room under the last list so even a short one can be pinned to the top edge.
    val density = LocalDensity.current
    val viewportHeight = with(density) { listState.layoutInfo.viewportSize.height.toDp() }
    val lastRows = sections.lastOrNull()?.let { resolveFeedMiniGridConfig(it.previewItems).rowCount } ?: 2
    val bottomSpacer = (viewportHeight - feedSectionHeight(lastRows)).coerceAtLeast(TvDimens.FeedPinBottomPadding)

    // Down from the top bar: with no accounts the only target is the "Accounts and Security" button.
    val noAccounts = !uiState.isLoading && !uiState.isAnySyncApiAvailable
    val contentEntry = LocalDashboardContentEntry.current
    DisposableEffect(contentEntry, noAccounts) {
        val handler: () -> Boolean = {
            if (noAccounts) {
                coroutineScope.launch { accountsButtonFocusRequester.requestFocusWithRetry(attempts = 18) }
                true
            } else {
                false
            }
        }
        contentEntry.handler = handler
        onDispose { if (contentEntry.handler === handler) contentEntry.handler = null }
    }

    DisposableEffect(contentEntry) {
        val reset: () -> Unit = {
            LibraryFocusStore.resetLastFocused()
            focusTargetId = null
            pinJob?.cancel()
            coroutineScope.launch { listState.scrollToItem(0) }
        }
        contentEntry.resetToTop = reset
        onDispose { if (contentEntry.resetToTop === reset) contentEntry.resetToTop = null }
    }

    Box(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBringIntoViewSpec provides LibraryBringIntoViewSpec) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (showSourceSelector) {
                    item(key = "library_header") {
                        LibrarySourceSelectorRow(
                            sources = uiState.availableSources,
                            selectedSource = selectedSourceName,
                            rowEntryFocusRequester = sourceRowEntryFocusRequester,
                            isInteractive = true,
                            upFocusRequester = topBarFocusRequester,
                            downFocusRequester = firstFeedCardFocusRequester
                                .takeIf { sections.isNotEmpty() && !uiState.isLoading },
                            onSourceSelected = viewModel::switchSource,
                            modifier = Modifier.padding(
                                start = HomeContentHorizontalPadding,
                                top = TvDimens.LibrarySourceRowTopGap,
                            ),
                        )
                    }
                }

                if (uiState.isLoading) {
                    items(count = LIBRARY_PLACEHOLDER_COUNT) { index ->
                        LibraryLoadingSection(isFirst = index == 0)
                    }
                } else {
                    itemsIndexed(
                        items = sections,
                        key = { _, section -> section.id }
                    ) { index, section ->
                        val total = section.previewItems.size
                        FeedSection(
                            title = section.title,
                            sourceName = pluralStringResource(R.plurals.tv_library_titles_count, total, total),
                            pinnedTop = if (index == 0) TvDimens.LibraryFirstFeedPinnedTop else TvDimens.FeedPinnedTop,
                            showFocusedRating = true,
                            showMoreSubtitle = if (total > FEED_GRID_SIZE) {
                                stringResource(
                                    R.string.tv_library_more_count,
                                    total - FEED_VISIBLE_ITEMS_BEFORE_SHOW_MORE,
                                )
                            } else {
                                null
                            },
                            state = HomeFeedLoadState.Success(section.previewItems),
                            onMediaClick = ::openDetailsWithRestore,
                            onShowMoreClick = { openGridWithRestore(section) },
                            isInteractive = true,
                            modifier = Modifier.padding(horizontal = HomeContentHorizontalPadding),
                            firstItemFocusRequester = if (index == 0) firstFeedCardFocusRequester else null,
                            upFocusRequester = if (index == 0) {
                                if (showSourceSelector) sourceRowEntryFocusRequester else topBarFocusRequester
                            } else {
                                null
                            },
                            sectionFocusKey = "library:section:${section.id}",
                            pendingRestoreFocusTargetId = focusTargetId,
                            restoreFocusToken = focusToken,
                            onItemFocused = { item ->
                                LibraryFocusStore.onTargetFocused(LibraryFocusStore.feedPoster(section.id, item))
                                val slot = section.previewItems.indexOf(item).coerceAtLeast(0)
                                pinSectionToTop(index, slot / FEED_GRID_COLUMNS)
                            },
                            onShowMoreFocused = {
                                LibraryFocusStore.onTargetFocused(LibraryFocusStore.feedShowMore(section.id))
                                pinSectionToTop(index, row = 1)
                            },
                            onRestoreFocusConsumed = { targetId ->
                                if (focusTargetId == targetId) focusTargetId = null
                                LibraryFocusStore.clearPendingRestore(targetId)
                            },
                            onVerticalExit = { up, column -> navigateFromSection(index, up, column) },
                        )
                    }
                    if (sections.isNotEmpty()) {
                        // Lets the last list scroll up to the top edge.
                        item(key = "library_bottom") { Spacer(Modifier.height(bottomSpacer)) }
                    }
                }
            }
        }

        if (!uiState.isLoading && sections.isEmpty()) {
            val (title, message) = when {
                !uiState.isAnySyncApiAvailable ->
                    splitLibraryMessage(stringResource(R.string.empty_library_no_accounts_message))
                !uiState.errorMessage.isNullOrBlank() ->
                    stringResource(R.string.tv_library_load_error_title) to uiState.errorMessage
                else -> splitLibraryMessage(stringResource(R.string.empty_library_logged_in_message))
            }
            LibraryStateMessage(
                title = title,
                message = message,
                actionText = stringResource(R.string.category_account)
                    .takeIf { !uiState.isAnySyncApiAvailable },
                onAction = onOpenAccountsSettings,
                actionFocusRequester = accountsButtonFocusRequester,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

/** One list while loading: header skeleton (no title yet) + 2x6 poster skeletons. */
@Composable
private fun LibraryLoadingSection(isFirst: Boolean) {
    val pinnedTop = if (isFirst) TvDimens.LibraryFirstFeedPinnedTop else TvDimens.FeedPinnedTop
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(feedSectionHeight(rowCount = 2) - (TvDimens.FeedPinnedTop - pinnedTop))
            .padding(horizontal = HomeContentHorizontalPadding)
            .padding(top = pinnedTop)
    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(TvDimens.FeedHeaderHeight),
        ) {
            TvSkeletonBlock(
                width = TvDimens.LibraryHeaderSkeletonWidth,
                height = TvDimens.LibraryHeaderSkeletonHeight,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
            )
        }
        Spacer(modifier = Modifier.height(TvDimens.FeedHeaderGridGap))
        FeedMiniGridPlaceholder(modifier = Modifier.fillMaxWidth())
    }
}

/** Empty list / no accounts / error: centred text under the source row, optional action button. */
@Composable
private fun LibraryStateMessage(
    title: String,
    message: String?,
    actionText: String?,
    onAction: () -> Unit,
    actionFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(top = TvDimens.LibraryStateTop),
    ) {
        Text(
            text = title,
            style = TvTextStyles.StateTitle,
            color = TvColors.Text,
            textAlign = TextAlign.Center,
        )
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                style = TvTextStyles.StateMessage,
                color = TvColors.Text2,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = TvDimens.StateMessageGap)
                    .padding(horizontal = HomeContentHorizontalPadding),
            )
        }
        if (actionText != null) {
            TvButton(
                text = actionText,
                onClick = onAction,
                height = TvDimens.ButtonHeightLarge,
                modifier = Modifier
                    .padding(top = TvDimens.StateActionGap)
                    .focusRequester(actionFocusRequester),
            )
        }
    }
}

private val LibraryMessageLineSplit = Regex("\\s*\\n\\s*")
private val LibraryMessageSentenceSplit = Regex("^(.+?[.!?])\\s+(.+)$")

/** Splits "Title\nMessage" (or "Title. Message") into the bold title and the grey message. */
private fun splitLibraryMessage(text: String): Pair<String, String?> {
    val lines = text.trim().split(LibraryMessageLineSplit).filter { it.isNotBlank() }
    if (lines.size > 1) return lines.first() to lines.drop(1).joinToString(" ")
    val sentence = LibraryMessageSentenceSplit.find(text.trim())
    return if (sentence != null) {
        sentence.groupValues[1] to sentence.groupValues[2]
    } else {
        text.trim() to null
    }
}

private fun LazyListState.isScrolledToTop(): Boolean {
    return firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
}
