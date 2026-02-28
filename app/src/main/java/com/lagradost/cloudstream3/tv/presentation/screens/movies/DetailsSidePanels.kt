package com.lagradost.cloudstream3.tv.presentation.screens.movies

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatPanelItem
import com.lagradost.cloudstream3.tv.presentation.common.MenuListSidePanel
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem
import com.lagradost.cloudstream3.ui.WatchType

@Composable
fun MovieActionsSidePanel(
    visible: Boolean,
    loading: Boolean,
    inProgress: Boolean,
    title: String,
    items: List<MovieDetailsCompatPanelItem>,
    onCloseRequested: () -> Unit,
    onActionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    panelTestTag: String = "movie_actions_side_panel",
    showItemsWhileLoading: Boolean = false,
    headerContent: (@Composable ColumnScope.() -> Unit)? = null,
    emptyContent: (@Composable () -> Unit)? = null,
) {
    val menuItems = if (loading && !showItemsWhileLoading) {
        emptyList()
    } else {
        items.map { action ->
            SidePanelMenuItem(
                id = "movie_action_${action.id}",
                title = action.label,
                enabled = if (loading && showItemsWhileLoading) true else !inProgress,
                onClick = { onActionSelected(action.id) },
                leadingContent = {
                    Icon(
                        painter = painterResource(id = action.iconRes),
                        contentDescription = null
                    )
                }
            )
        }
    }

    MenuListSidePanel(
        visible = visible,
        onCloseRequested = onCloseRequested,
        title = title,
        items = menuItems,
        panelWidth = 340.dp,
        modifier = modifier,
        panelTestTag = panelTestTag,
        initialFocusedItemId = menuItems.firstOrNull()?.id,
        headerContent = headerContent,
        emptyContent = {
            if (emptyContent != null) {
                emptyContent()
            } else {
                val text = if (loading) {
                    stringResource(R.string.loading)
                } else {
                    stringResource(R.string.no_links_found_toast)
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    )
}

@Composable
fun BookmarkStatusSidePanel(
    visible: Boolean,
    currentStatus: WatchType,
    onCloseRequested: () -> Unit,
    onBookmarkSelected: (WatchType) -> Unit,
    modifier: Modifier = Modifier,
    panelTestTag: String = "bookmark_side_panel",
) {
    val menuItems = WatchType.entries.map { watchType ->
        SidePanelMenuItem(
            id = "watch_type_${watchType.internalId}",
            title = stringResource(watchType.stringRes),
            selected = watchType == currentStatus,
            onClick = { onBookmarkSelected(watchType) },
            leadingContent = {
                Icon(
                    painter = painterResource(id = watchType.iconRes),
                    contentDescription = null
                )
            }
        )
    }

    MenuListSidePanel(
        visible = visible,
        onCloseRequested = onCloseRequested,
        title = stringResource(R.string.action_add_to_bookmarks),
        items = menuItems,
        panelWidth = 340.dp,
        modifier = modifier,
        panelTestTag = panelTestTag,
        initialFocusedItemId = menuItems.firstOrNull { it.selected }?.id ?: menuItems.firstOrNull()?.id,
        showSelectionRadio = true
    )
}
