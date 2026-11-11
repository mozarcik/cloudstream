package com.lagradost.cloudstream3.tv.presentation.screens.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.TvEmptyState
import com.lagradost.cloudstream3.tv.presentation.focus.rememberFocusRequesterMap
import com.lagradost.cloudstream3.tv.presentation.focus.requestFocusWithRetry
import com.lagradost.cloudstream3.tv.presentation.screens.dashboard.LocalDashboardContentEntry
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ListWidth = 380.dp
private val PinColumnGap = 19.dp
private val PinSize = 36.dp
private val RowHeight = 48.dp
private val RowRadius = 14.dp
private val HeaderHeight = 70.dp
private val PreviewWidth = 340.dp
private val PreviewTop = 70.dp
private val PreviewBottom = 30.dp
private val PreviewEnd = 60.dp
private val PosterWidth = 92.dp
private val PosterHeight = 138.dp
private val PosterGap = 12.dp
private val ToastStart = 88.dp
private val ToastBottom = 16.dp
private const val PreviewDebounceMs = 150L
private const val ToastMs = 2_000L
private const val EntryFocusAttempts = 18

private enum class FocusPart { Row, Pin }

private data class ListFocus(val key: String, val part: FocusPart)

private data class PinToast(val title: String, val pinned: Boolean)

private sealed interface ListItem {
    val key: String

    data class Header(val providerName: String) : ListItem {
        override val key = "h:$providerName"
    }

    data class Entry(val providerName: String, val row: LibraryListRow) : ListItem {
        override val key = "l:${row.key}"
    }
}

/**
 * All library lists of every account, each with a pin that adds it to the navigation menu.
 * D-pad: Right on a row reaches its pin, Left goes back (plain focus search); OK opens the list
 * on a row and toggles the pin on a pin. The preview on the right never takes focus.
 */
@Composable
internal fun LibraryListsScreen(
    state: LibraryListsUiState,
    lastFocusedKey: () -> String?,
    onFocusedKey: (String) -> Unit,
    onOpenList: (providerName: String, listKey: String) -> Unit,
    onTogglePin: (listKey: String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val items = remember(state.groups) {
        buildList {
            state.groups.forEach { group ->
                add(ListItem.Header(group.providerName))
                group.rows.forEach { add(ListItem.Entry(group.providerName, it)) }
            }
        }
    }
    val rowsByKey = remember(state.groups) {
        state.groups.flatMap { group -> group.rows.map { it.key to (group.providerName to it) } }.toMap()
    }
    val rowRequesters = rememberFocusRequesterMap(rowsByKey.keys.toList())

    val focus = remember { mutableStateOf<ListFocus?>(null) }
    val toast = remember { mutableStateOf<PinToast?>(null) }
    val listState = rememberLazyListState()

    // Entering the screen from the menu (or back from a list): the last focused row, else the first.
    val contentEntry = LocalDashboardContentEntry.current
    val scope = rememberCoroutineScope()
    DisposableEffect(contentEntry, rowRequesters) {
        val handler: () -> Boolean = {
            val requester = lastFocusedKey()?.let(rowRequesters::get) ?: rowRequesters.values.firstOrNull()
            requester?.let {
                scope.launch { it.requestFocusWithRetry(attempts = EntryFocusAttempts) }
                true
            } ?: false
        }
        contentEntry.handler = handler
        onDispose { if (contentEntry.handler === handler) contentEntry.handler = null }
    }

    val toastTemplatePinned = stringResource(R.string.tv_library_pinned_toast)
    val toastTemplateUnpinned = stringResource(R.string.tv_library_unpinned_toast)

    Box(modifier = modifier.fillMaxSize().background(TvColors.Ground)) {
        if (items.isEmpty()) {
            if (!state.isLoading) {
                TvEmptyState(
                    title = stringResource(R.string.tv_menu_no_lists),
                    illustration = R.drawable.tv_empty_no_accounts,
                    message = stringResource(R.string.tv_library_no_lists_message),
                )
            }
        } else {
            Column(modifier = Modifier.padding(start = TvDimens.ContentStartWithRail)) {
                Header(state)
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = TvDimens.SearchListBottomPadding),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.width(ListWidth + PinColumnGap + PinSize),
                ) {
                    items(items, key = { it.key }, contentType = { it::class }) { item ->
                        when (item) {
                            is ListItem.Header -> ProviderHeader(item.providerName)
                            is ListItem.Entry -> ListEntry(
                                row = item.row,
                                rowRequester = rowRequesters.getValue(item.row.key),
                                onFocus = { part ->
                                    focus.value = ListFocus(item.row.key, part)
                                    onFocusedKey(item.row.key)
                                },
                                onOpen = { onOpenList(item.providerName, item.row.key) },
                                onTogglePin = {
                                    val pinned = onTogglePin(item.row.key)
                                    toast.value = PinToast(item.row.title, pinned)
                                },
                            )
                        }
                    }
                }
            }
            PreviewPanel(
                focus = focus,
                rowsByKey = rowsByKey,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = PreviewTop, bottom = PreviewBottom, end = PreviewEnd),
            )
            PinBackHandler(focus = focus, rowRequesters = rowRequesters)
            ToastHost(
                toast = toast,
                pinnedTemplate = toastTemplatePinned,
                unpinnedTemplate = toastTemplateUnpinned,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = ToastStart, bottom = ToastBottom),
            )
        }
    }
}

@Composable
private fun Header(state: LibraryListsUiState) {
    Row(
        modifier = Modifier
            .width(ListWidth + PinColumnGap + PinSize)
            .height(HeaderHeight)
            .padding(top = 30.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.tv_library_title),
                style = TvTextStyles.PanelTitle,
                color = TvColors.Text,
                maxLines = 1,
            )
            Text(
                text = stringResource(
                    R.string.tv_library_summary,
                    pluralStringResource(R.plurals.tv_library_lists_count, state.listCount, state.listCount),
                    state.pinnedCount,
                ),
                style = TvTextStyles.Meta,
                color = TvColors.Label,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        Box(Modifier.width(PinSize), contentAlignment = Alignment.TopCenter) {
            Text(
                text = stringResource(R.string.tv_library_in_menu_column).uppercase(),
                style = TvTextStyles.Eyebrow,
                color = TvColors.Label,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.wrapContentWidth(align = Alignment.CenterHorizontally, unbounded = true).padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun ProviderHeader(providerName: String) {
    Text(
        text = providerName.uppercase(),
        style = TvTextStyles.Eyebrow,
        color = TvColors.Label,
        maxLines = 1,
        modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun ListEntry(
    row: LibraryListRow,
    rowRequester: FocusRequester,
    onFocus: (FocusPart) -> Unit,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
) {
    var rowFocused by remember { mutableStateOf(false) }
    var pinFocused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().height(RowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(RowRadius))
                // Focused = white; while the pin is focused the row stays softly marked (draw phase only).
                .drawBehind {
                    val color = when {
                        rowFocused -> TvColors.Focus
                        pinFocused -> TvColors.FillRow
                        else -> Color.Transparent
                    }
                    if (color != Color.Transparent) drawRect(color)
                }
                .focusRequester(rowRequester)
                .onFocusChanged {
                    rowFocused = it.isFocused
                    if (it.isFocused) onFocus(FocusPart.Row)
                }
                // An empty list stays focusable (so focus does not skip it); OK just does nothing.
                .clickable(interactionSource = null, indication = null, onClick = if (row.count > 0) onOpen else ({}))
                .padding(horizontal = 18.dp),
        ) {
            Text(
                text = row.title,
                style = TvTextStyles.Row,
                color = if (rowFocused) TvColors.OnFocus else TvColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(R.plurals.tv_library_items_count, row.count, row.count),
                style = TvTextStyles.Meta,
                color = if (rowFocused) TvColors.OnFocusSecondary else TvColors.RowSubtitle,
                maxLines = 1,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(start = PinColumnGap)
                .size(PinSize)
                .clip(CircleShape)
                .drawBehind { if (pinFocused) drawRect(TvColors.Focus) }
                .onFocusChanged {
                    pinFocused = it.isFocused
                    if (it.isFocused) onFocus(FocusPart.Pin)
                }
                .clickable(interactionSource = null, indication = null, onClick = onTogglePin),
        ) {
            Icon(
                imageVector = if (row.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = null,
                tint = when {
                    row.pinned -> TvColors.Accent
                    pinFocused -> TvColors.OnFocus
                    else -> TvColors.Text
                },
                modifier = Modifier.size(TvDimens.RowIconSize),
            )
        }
    }
}

/** Back on a pin goes back to its row; anywhere else Back is the Dashboard's. */
@Composable
private fun PinBackHandler(focus: MutableState<ListFocus?>, rowRequesters: Map<String, FocusRequester>) {
    val current = focus.value
    BackHandler(enabled = current?.part == FocusPart.Pin) {
        current?.let { rowRequesters[it.key]?.requestFocus() }
    }
}

@Composable
private fun PreviewPanel(
    focus: MutableState<ListFocus?>,
    rowsByKey: Map<String, Pair<String, LibraryListRow>>,
    modifier: Modifier = Modifier,
) {
    val current = focus.value
    // Follows the focus after a short pause, so moving through the list does not reload posters.
    var shownKey by remember { mutableStateOf(current?.key) }
    LaunchedEffect(current?.key) {
        if (shownKey != null) delay(PreviewDebounceMs)
        shownKey = current?.key
    }
    val (providerName, row) = shownKey?.let(rowsByKey::get) ?: return

    Column(
        modifier = modifier
            .width(PreviewWidth)
            .fillMaxHeight()
            .clip(TvShapes.Dialog)
            .background(TvColors.Panel)
            .padding(20.dp),
    ) {
        Text(
            text = providerName.uppercase(),
            style = TvTextStyles.Eyebrow,
            color = TvColors.Label,
            maxLines = 1,
        )
        Text(
            text = row.title,
            style = TvTextStyles.PanelTitle,
            color = TvColors.Text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = stringResource(
                R.string.tv_library_preview_meta,
                pluralStringResource(R.plurals.tv_library_items_count, row.count, row.count),
                stringResource(if (row.pinned) R.string.tv_library_state_pinned else R.string.tv_library_state_unpinned),
            ),
            style = TvTextStyles.Meta,
            color = TvColors.RowSubtitle,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp),
        )
        Box(Modifier.weight(1f).fillMaxWidth().padding(top = 16.dp)) {
            if (row.previewItems.isEmpty()) {
                EmptyPreview()
            } else {
                PosterGrid(row)
            }
        }
        Text(
            text = stringResource(hintRes(row, current?.takeIf { it.key == row.key }?.part)),
            style = TvTextStyles.Meta,
            color = TvColors.RowSubtitle,
            maxLines = 1,
        )
    }
}

private fun hintRes(row: LibraryListRow, part: FocusPart?): Int = when {
    part == FocusPart.Pin ->
        if (row.pinned) R.string.tv_library_hint_pin_pinned else R.string.tv_library_hint_pin_unpinned
    row.count == 0 ->
        if (row.pinned) R.string.tv_library_hint_row_empty_pinned else R.string.tv_library_hint_row_empty_unpinned
    else ->
        if (row.pinned) R.string.tv_library_hint_row_pinned else R.string.tv_library_hint_row_unpinned
}

@Composable
private fun PosterGrid(row: LibraryListRow) {
    Column(verticalArrangement = Arrangement.spacedBy(PosterGap)) {
        row.previewItems.chunked(3).forEach { posters ->
            Row(horizontalArrangement = Arrangement.spacedBy(PosterGap)) {
                posters.forEach { item ->
                    AsyncImage(
                        model = item.posterUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(PosterWidth, PosterHeight)
                            .clip(TvShapes.Card)
                            .background(TvColors.PosterPlaceholder),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPreview() {
    TvEmptyState(
        title = stringResource(R.string.tv_library_empty_list_title),
        illustration = R.drawable.tv_empty_empty_list,
        message = stringResource(R.string.tv_library_empty_list_message),
    )
}

@Composable
private fun ToastHost(
    toast: MutableState<PinToast?>,
    pinnedTemplate: String,
    unpinnedTemplate: String,
    modifier: Modifier = Modifier,
) {
    val current = toast.value ?: return
    // A new toast restarts the timer.
    LaunchedEffect(current) {
        delay(ToastMs)
        if (toast.value === current) toast.value = null
    }
    Row(
        modifier = modifier
            .width(ListWidth)
            .height(44.dp)
            .clip(TvShapes.Dialog)
            .background(TvColors.Focus)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = if (current.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
            contentDescription = null,
            tint = TvColors.Accent,
            modifier = Modifier.size(TvDimens.RowIconSize),
        )
        Text(
            text = (if (current.pinned) pinnedTemplate else unpinnedTemplate).format(current.title),
            style = TvTextStyles.Row,
            color = TvColors.OnFocus,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
