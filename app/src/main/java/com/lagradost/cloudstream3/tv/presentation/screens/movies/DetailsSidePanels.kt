package com.lagradost.cloudstream3.tv.presentation.screens.movies

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatPanelItem
import com.lagradost.cloudstream3.tv.presentation.common.MenuListSidePanel
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelSupportingStyle
import com.lagradost.cloudstream3.ui.WatchType

private val DefaultMovieActionsPanelWidth = TvDimens.PanelWidth

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
    panelWidth: Dp? = null,
    panelTestTag: String = "movie_actions_side_panel",
    showItemsWhileLoading: Boolean = false,
    closeOnFocusExit: Boolean = true,
    headerContent: (@Composable ColumnScope.() -> Unit)? = null,
    emptyContent: (@Composable () -> Unit)? = null,
    eyebrow: String? = null,
    subtitle: String? = null,
    titleTrailing: String? = null,
    /** Icon at the end of every action row (e.g. download arrow on the mirror list). */
    itemTrailingIcon: ImageVector? = null,
    rowHeight: Dp = TvDimens.PanelRowHeight,
    fixedRowHeight: Boolean = false,
) {
    val menuItems = if (loading && !showItemsWhileLoading) {
        emptyList()
    } else {
        items.map { action ->
            SidePanelMenuItem(
                id = action.key,
                title = action.label,
                testTag = action.key,
                titleMaxLines = action.titleMaxLines,
                enabled = when {
                    action.isSectionHeader -> false
                    loading && showItemsWhileLoading -> true
                    else -> !inProgress
                },
                isSectionHeader = action.isSectionHeader,
                supportingTexts = action.supportingTexts,
                supportingStyle = if (action.supportingTexts.isNotEmpty()) {
                    SidePanelSupportingStyle.SourceOption
                } else {
                    SidePanelSupportingStyle.Default
                },
                onClick = { onActionSelected(action.id) },
                trailingContent = itemTrailingIcon?.takeIf { !action.isSectionHeader }?.let { icon ->
                    {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(TvDimens.RowIconSize),
                        )
                    }
                },
                leadingContent = action.iconRes?.takeIf { itemTrailingIcon == null }?.let { iconRes ->
                    {
                        Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    }
    val resolvedPanelWidth = panelWidth ?: DefaultMovieActionsPanelWidth

    MenuListSidePanel(
        visible = visible,
        onCloseRequested = onCloseRequested,
        title = title,
        items = menuItems,
        eyebrow = eyebrow,
        subtitle = subtitle,
        titleTrailing = titleTrailing,
        panelWidth = resolvedPanelWidth,
        modifier = modifier,
        panelTestTag = panelTestTag,
        initialFocusedItemId = menuItems.firstOrNull { !it.isSectionHeader }?.id,
        headerContent = headerContent,
        closeOnFocusExit = closeOnFocusExit,
        rowHeight = rowHeight,
        fixedRowHeight = fixedRowHeight,
        listBottomPadding = TvDimens.PanelListBottomPadding,
        uppercaseSectionHeaders = true,
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
    subtitle: String? = null,
    panelTestTag: String = "bookmark_side_panel",
) {
    val accent = MaterialTheme.colorScheme.primary
    val menuItems = WatchType.entries.map { watchType ->
        val selected = watchType == currentStatus
        SidePanelMenuItem(
            id = "watch_type_${watchType.internalId}",
            title = stringResource(watchType.stringRes),
            selected = selected,
            onClick = { onBookmarkSelected(watchType) },
            leadingContent = {
                Icon(
                    imageVector = when {
                        watchType == WatchType.NONE -> Icons.Filled.Add
                        selected -> Icons.Filled.Bookmark
                        else -> Icons.Filled.BookmarkBorder
                    },
                    contentDescription = null,
                    modifier = Modifier.size(TvDimens.PanelStatusIconSize),
                )
            },
            trailingContent = if (selected) {
                {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(TvDimens.PanelStatusIconSize),
                    )
                }
            } else {
                null
            },
        )
    }

    MenuListSidePanel(
        visible = visible,
        onCloseRequested = onCloseRequested,
        title = stringResource(R.string.action_add_to_bookmarks),
        subtitle = subtitle,
        items = menuItems,
        panelWidth = DefaultMovieActionsPanelWidth,
        modifier = modifier,
        panelTestTag = panelTestTag,
        rowHeight = TvDimens.PanelStatusRowHeight,
        topPadding = TvDimens.PanelStatusTopPadding,
        initialFocusedItemId = menuItems.firstOrNull { it.selected }?.id ?: menuItems.firstOrNull()?.id,
    )
}
