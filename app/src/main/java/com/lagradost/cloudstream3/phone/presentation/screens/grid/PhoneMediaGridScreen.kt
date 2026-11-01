package com.lagradost.cloudstream3.phone.presentation.screens.grid

import com.lagradost.cloudstream3.phone.presentation.common.navBarBottom
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.lagradost.cloudstream3.designsystem.CsColors
import com.lagradost.cloudstream3.phone.presentation.common.CsButton
import com.lagradost.cloudstream3.phone.presentation.common.HideOnScrollScaffold
import com.lagradost.cloudstream3.phone.presentation.common.LocalPermanentMenu
import androidx.compose.material.icons.filled.Menu
import com.lagradost.cloudstream3.tv.presentation.screens.library.LibraryFeedGridSelectionStore
import com.lagradost.cloudstream3.tv.presentation.screens.library.buildLibrarySortPanelOptions
import com.lagradost.cloudstream3.tv.presentation.screens.library.sortLibraryGridItems
import com.lagradost.cloudstream3.ui.library.ListSorting
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.phone.presentation.common.CsIconButton
import com.lagradost.cloudstream3.phone.presentation.common.CsPosterCard
import com.lagradost.cloudstream3.phone.presentation.common.mediaMeta
import com.lagradost.cloudstream3.phone.presentation.screens.settings.LevelHeader
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneColors
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneShapes
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneText
import com.lagradost.cloudstream3.tv.compat.home.FeedRepositoryImpl
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.compat.home.SourceRepository
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeedGridSelectionStore
import com.lagradost.cloudstream3.tv.presentation.screens.home.MediaGridViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.home.MediaGridViewModelFactory
import com.lagradost.cloudstream3.tv.presentation.screens.search.SearchFeedGridSelectionStore

/** Home "Show more": 3-column grid fed by the same Paging3 source as TV (infinite scroll). */
@Composable
fun PhoneHomeFeedGridScreen(
    onMediaClick: (MediaItemCompat) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MediaGridViewModel = viewModel(factory = MediaGridViewModelFactory(FeedRepositoryImpl())),
) {
    val feed by HomeFeedGridSelectionStore.selectedFeed.collectAsState()
    val selected = feed
    if (selected == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val source by SourceRepository.selectedApi.collectAsState(initial = null)
    val items = viewModel.pagingData.collectAsLazyPagingItems()
    LaunchedEffect(selected.id) { viewModel.selectFeed(selected) }

    GridScaffold(title = selected.name, subtitle = source?.name, onBack = onBack, modifier = modifier) {
        pagedItems(items, onMediaClick)
    }
}

/** Search "Show more": all results one source returned for the submitted query. */
@Composable
fun PhoneSearchGridScreen(
    onMediaClick: (MediaItemCompat) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val section by SearchFeedGridSelectionStore.selectedSection.collectAsState()
    val selected = section
    if (selected == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    GridScaffold(title = selected.title, subtitle = SearchFeedGridSelectionStore.query, onBack = onBack, modifier = modifier) {
        itemsIndexed(selected.items, key = { index, item -> "${item.gridKeySeed}#$index" }) { _, item ->
            CsPosterCard(item.name, item.posterUri, { onMediaClick(item) }, height = null, meta = mediaMeta(item), modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Sort options offered by the list behind the grid; only methods the list supports are passed in. */
data class PhoneGridSort(val options: List<Pair<String, String>>, val selectedId: String, val onSelected: (String) -> Unit)

/** Library list ("Show more" of a provider list), sortable with the methods its provider supports. */
@Composable
fun PhoneLibraryGridScreen(
    onMediaClick: (MediaItemCompat) -> Unit,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val section by LibraryFeedGridSelectionStore.selectedSection.collectAsState()
    val selected = section
    if (selected == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    var sortOrdinal by rememberSaveable(selected.id) { mutableStateOf<Int?>(null) }
    val method = sortOrdinal?.let { ListSorting.entries.getOrNull(it) }
    val sortOptions = buildLibrarySortPanelOptions(selected.supportedSortingMethods, method)
    val sort = if (selected.supportedSortingMethods.isEmpty()) null else PhoneGridSort(
        options = sortOptions.map { it.id to stringResource(it.method?.stringRes ?: R.string.phone_default_order) },
        selectedId = sortOptions.first { it.selected }.id,
        onSelected = { id -> sortOrdinal = sortOptions.firstOrNull { it.id == id }?.method?.ordinal },
    )
    val sorted = remember(selected.gridItems, method) { sortLibraryGridItems(selected.gridItems, method).map { it.mediaItem } }
    // A list is a top-level destination of the menu, so it shows the menu button, not Back.
    GridScaffold(title = selected.title, subtitle = LibraryFeedGridSelectionStore.accountName, onBack = onBack, onMenu = onOpenMenu, modifier = modifier, sort = sort) {
        itemsIndexed(sorted, key = { index, item -> "${item.gridKeySeed}#$index" }) { _, item ->
            CsPosterCard(item.name, item.posterUri, { onMediaClick(item) }, height = null, meta = mediaMeta(item), modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun GridScaffold(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    modifier: Modifier,
    onMenu: (() -> Unit)? = null,
    sort: PhoneGridSort? = null,
    content: LazyGridScope.() -> Unit,
) {
    var showSort by remember { mutableStateOf(false) }
    HideOnScrollScaffold(modifier = modifier.background(PhoneColors.Ground), bar = {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onMenu == null) {
                CsIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.phone_back), onBack, container = androidx.compose.ui.graphics.Color.Transparent)
            } else if (!LocalPermanentMenu.current) {
                CsIconButton(Icons.Default.Menu, stringResource(R.string.phone_open_menu), onMenu, container = androidx.compose.ui.graphics.Color.Transparent)
            } else {
                androidx.compose.foundation.layout.Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = PhoneText.Level.copy(fontSize = 22.sp), color = PhoneColors.Text, maxLines = 1)
                if (!subtitle.isNullOrBlank()) Text(subtitle, style = PhoneText.Meta, color = PhoneColors.Text2, maxLines = 1)
            }
            // Sort lives in the bar (current method as the description), only when the list supports sorting.
            if (sort != null) {
                val label = sort.options.firstOrNull { it.first == sort.selectedId }?.second.orEmpty()
                CsIconButton(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.phone_sort_by, label), { showSort = true }, container = androidx.compose.ui.graphics.Color.Transparent)
            }
        }
    }) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(104.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp + navBarBottom()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
            content = content,
        )
    }

    if (showSort && sort != null) {
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { showSort = false }, containerColor = PhoneColors.Sheet) {
            Text(stringResource(R.string.sort), style = PhoneText.Section, color = PhoneColors.Text, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp))
            sort.options.forEach { (id, label) ->
                val selected = id == sort.selectedId
                Row(
                    Modifier.fillMaxWidth().height(48.dp).background(if (selected) Color(0x14FFFFFF) else Color.Transparent)
                        .clickable(role = Role.RadioButton) { sort.onSelected(id); showSort = false }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { Text(label, style = PhoneText.Row, color = if (selected) CsColors.Info else PhoneColors.Text) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}


private fun LazyGridScope.pagedItems(items: LazyPagingItems<MediaItemCompat>, onMediaClick: (MediaItemCompat) -> Unit) {
    items(count = items.itemCount, key = { index -> "${items.peek(index)?.gridKeySeed}#$index" }) { index ->
        items[index]?.let { item ->
            CsPosterCard(item.name, item.posterUri, { onMediaClick(item) }, height = null, meta = mediaMeta(item), modifier = Modifier.fillMaxWidth())
        }
    }
    when {
        items.loadState.append is LoadState.Loading || items.loadState.refresh is LoadState.Loading -> {
            items(count = 3, span = { GridItemSpan(1) }) {
                Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(PhoneShapes.Poster).background(PhoneColors.Card))
            }
        }
        items.loadState.refresh is LoadState.Error && items.itemCount == 0 -> {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.phone_grid_load_error), style = PhoneText.Body, color = PhoneColors.Text2)
            }
        }
        items.itemCount == 0 && items.loadState.refresh is LoadState.NotLoading -> {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.phone_grid_empty), style = PhoneText.Body, color = PhoneColors.Text2)
            }
        }
    }
}
