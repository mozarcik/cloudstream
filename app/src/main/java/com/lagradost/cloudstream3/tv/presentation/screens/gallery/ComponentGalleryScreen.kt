package com.lagradost.cloudstream3.tv.presentation.screens.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.common.MenuListSidePanel
import com.lagradost.cloudstream3.tv.presentation.common.PosterCard
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelSelectionIndicatorStyle
import com.lagradost.cloudstream3.tv.presentation.common.TvButton
import com.lagradost.cloudstream3.tv.presentation.common.TvButtonVariant
import com.lagradost.cloudstream3.tv.presentation.common.TvChip
import com.lagradost.cloudstream3.tv.presentation.common.TvConfirmDialog
import com.lagradost.cloudstream3.tv.presentation.common.TvEmptyState
import com.lagradost.cloudstream3.tv.presentation.common.TvErrorState
import com.lagradost.cloudstream3.tv.presentation.common.TvListRow
import com.lagradost.cloudstream3.tv.presentation.common.TvPosterSkeletonRow
import com.lagradost.cloudstream3.tv.presentation.common.TvProgressBar
import com.lagradost.cloudstream3.tv.presentation.common.TvRowLeading
import com.lagradost.cloudstream3.tv.presentation.common.TvRowTrailing
import com.lagradost.cloudstream3.tv.presentation.focus.TvBackHandler
import com.lagradost.cloudstream3.tv.presentation.focus.rememberTvFocusMemory
import com.lagradost.cloudstream3.tv.presentation.screens.home.FeedMiniGrid
import com.lagradost.cloudstream3.tv.presentation.screens.home.ShowMoreCard
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

private const val GalleryPosterModel = "gallery://poster"

/**
 * Debug-only gallery of the foundation components and their states (mockup row "Komponenty").
 * Scroll with D-pad up/down; Back leaves the gallery (or closes the open dialog/panel).
 */
@Composable
fun ComponentGalleryScreen(onBack: () -> Unit) {
    var dialogOpen by remember { mutableStateOf(false) }
    var panelOpen by remember { mutableStateOf(false) }
    var panelChoice by remember { mutableStateOf("a") }
    var chipSelected by remember { mutableStateOf("Source A") }
    var radioChoice by remember { mutableStateOf(0) }
    var toggleOn by remember { mutableStateOf(true) }
    val focusMemory = rememberTvFocusMemory()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val sampleItems = remember { sampleMediaItems(count = 13) }

    TvBackHandler(enabled = !dialogOpen && !panelOpen, onBack = onBack)
    // First entry focuses the first button; after the dialog / panel closes focus returns to its opener.
    focusMemory.RestoreEffect(
        fallbackKey = "default",
        restoreToken = dialogOpen to panelOpen,
        enabled = !dialogOpen && !panelOpen,
    )

    Box(modifier = Modifier.fillMaxSize().background(TvColors.Ground)) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = TvDimens.ScreenHorizontalPadding,
                end = TvDimens.ScreenHorizontalPadding,
                top = 28.dp,
                bottom = 60.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(28.dp),
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            section("Colors") { ColorSwatches() }
            section("Type (Figtree)") { TypeSamples() }
            section("Buttons") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvButton(
                        text = "Default",
                        onClick = {},
                        icon = Icons.Default.PlayArrow,
                        modifier = with(focusMemory) { Modifier.track("default") }
                            // Colors / Type above are not focusable: Up from the first control scrolls back to the top.
                            .onPreviewKeyEvent { event ->
                                if (event.key == Key.DirectionUp && event.type == KeyEventType.KeyDown) {
                                    scope.launch { listState.animateScrollToItem(0) }
                                    true
                                } else {
                                    false
                                }
                            },
                    )
                    TvButton(text = "Primary", onClick = {}, variant = TvButtonVariant.Primary)
                    TvButton(text = "Delete", onClick = {}, icon = Icons.Default.Delete, variant = TvButtonVariant.Destructive)
                    TvButton(text = "Fixed width", onClick = {}, width = 140.dp)
                    TvButton(text = "Disabled", onClick = {}, enabled = false)
                }
            }
            section("Chips") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Source A", "Source B", "Source C").forEach { label ->
                        TvChip(
                            text = label,
                            selected = chipSelected == label,
                            onClick = { chipSelected = label },
                            accent = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            section("List rows") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(TvDimens.RowSpacing),
                    modifier = Modifier.width(300.dp),
                ) {
                    TvListRow(title = "Default", onClick = {}, trailing = TvRowTrailing.Chevron, titleStyle = TvTextStyles.RowPanel)
                    TvListRow(
                        titleStyle = TvTextStyles.RowPanel,
                        title = "With subtitle",
                        subtitle = "Supporting text",
                        onClick = {},
                        leadingIcon = Icons.Default.Search,
                        trailing = TvRowTrailing.Chevron,
                        height = TvDimens.RowHeightLarge,
                    )
                    listOf("Option one", "Option two").forEachIndexed { index, label ->
                        TvListRow(
                            titleStyle = TvTextStyles.RowPanel,
                            title = label,
                            onClick = { radioChoice = index },
                            leadingIndicator = TvRowLeading.Radio(selected = radioChoice == index),
                        )
                    }
                    TvListRow(title = "Selected, unfocused (season)", onClick = {}, selected = true, titleStyle = TvTextStyles.RowPanel)
                    TvListRow(
                        titleStyle = TvTextStyles.RowPanel,
                        title = "Toggle",
                        onClick = { toggleOn = !toggleOn },
                        trailing = TvRowTrailing.Toggle(on = toggleOn),
                    )
                    TvListRow(title = "Value", onClick = {}, trailing = TvRowTrailing.Value("English"), titleStyle = TvTextStyles.RowPanel)
                    TvListRow(title = "Checked", onClick = {}, trailing = TvRowTrailing.Check(), titleStyle = TvTextStyles.RowPanel)
                    TvListRow(title = "Disabled", onClick = {}, enabled = false, titleStyle = TvTextStyles.RowPanel)
                }
            }
            section("Cards (ring 2 dp + scale 1.05)") {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    repeat(3) { index ->
                        PosterCard(
                            model = GalleryPosterModel,
                            title = "Poster ${index + 1}",
                            ratingLabel = "7.8",
                            shape = TvShapes.Card,
                            onClick = {},
                            onFocusChange = {},
                            modifier = Modifier.width(110.dp),
                        )
                    }
                    ShowMoreCard(
                        onClick = {},
                        subtitle = "All of Latest",
                        modifier = Modifier.width(110.dp).size(width = 110.dp, height = 165.dp),
                    )
                }
            }
            section("Progress bar") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.width(260.dp)) {
                    TvProgressBar(progress = 0.4f)
                    TvProgressBar(progress = 0.85f, accent = TvColors.Selected)
                }
            }
            section("Mini-grid 2x6 (11 cards + Show more)") {
                FeedMiniGrid(
                    items = sampleItems,
                    onMediaClick = {},
                    onShowMoreClick = {},
                    isInteractive = true,
                    feedName = "Latest",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            section("States: loading / empty / error") {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TvPosterSkeletonRow(count = 4, posterWidth = 80.dp, posterHeight = 120.dp, gap = 10.dp)
                    Box(Modifier.size(width = 240.dp, height = 180.dp).background(TvColors.Preview, TvShapes.Card)) {
                        TvEmptyState(
                            icon = Icons.Default.Search,
                            title = "Nothing here",
                            message = "Empty state message",
                        )
                    }
                    Box(Modifier.size(width = 260.dp, height = 180.dp).background(TvColors.Preview, TvShapes.Card)) {
                        TvErrorState(
                            message = "Something went wrong",
                            retryText = "Try again",
                            onRetry = {},
                        )
                    }
                }
            }
            section("Dialog and side panel") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TvButton(
                        text = "Open dialog",
                        onClick = { dialogOpen = true },
                        modifier = with(focusMemory) { Modifier.track("dialog") },
                    )
                    TvButton(
                        text = "Open side panel",
                        onClick = { panelOpen = true },
                        modifier = with(focusMemory) { Modifier.track("panel") },
                    )
                }
            }
        }

        MenuListSidePanel(
            visible = panelOpen,
            onCloseRequested = { panelOpen = false },
            title = "Sources",
            items = listOf("a", "b", "c").map { id ->
                SidePanelMenuItem(
                    id = id,
                    title = "Option ${id.uppercase()}",
                    selected = panelChoice == id,
                    onClick = { panelChoice = id },
                )
            },
            showSelectionRadio = true,
            selectionIndicatorStyle = SidePanelSelectionIndicatorStyle.Checkmark,
        )

        if (dialogOpen) {
            TvConfirmDialog(
                title = "Delete File",
                description = "This will permanently delete the file.\nAre you sure?",
                primaryText = "Delete",
                secondaryText = "Cancel",
                onPrimary = { dialogOpen = false },
                onSecondary = { dialogOpen = false },
                onDismiss = { dialogOpen = false },
                destructive = true,
                focusSecondaryFirst = true,
            )
        }
    }
}

private fun LazyListScope.section(title: String, content: @Composable () -> Unit) {
    item(key = title) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = title.uppercase(), style = TvTextStyles.Eyebrow, color = TvColors.LabelPanel)
            content()
        }
    }
}

@Composable
private fun ColorSwatches() {
    val swatches = listOf(
        "Ground" to TvColors.Ground,
        "Card" to TvColors.Card,
        "Panel" to TvColors.Panel,
        "Preview" to TvColors.Preview,
        "Focus" to TvColors.Focus,
        "Text 2" to TvColors.Text2,
        "Label" to TvColors.Label,
        "Selected" to TvColors.Selected,
        "Accent" to MaterialTheme.colorScheme.primary,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        swatches.forEach { (name, color) ->
            Column {
                Spacer(
                    Modifier
                        .size(width = 60.dp, height = 40.dp)
                        .background(color, RoundedCornerShape(7.dp))
                )
                Text(text = name, style = TvTextStyles.Caption, color = TvColors.LabelPanel)
            }
        }
    }
}

@Composable
private fun TypeSamples() {
    Column {
        Text("Title 36", style = TvTextStyles.Title)
        Text("Level 24", style = TvTextStyles.Level)
        Text("Section 18", style = TvTextStyles.Section)
        Text("Row / tab 14", style = TvTextStyles.Row)
        Text("Body 13, descriptions, max 3 lines", style = TvTextStyles.Body, color = TvColors.Body)
        Text("Meta 11  2026 • 7.8", style = TvTextStyles.Meta, color = TvColors.Text2)
    }
}

private fun sampleMediaItems(count: Int): List<MediaItemCompat> = List(count) { index ->
    MediaItemCompat.Movie(
        id = "gallery-$index",
        url = "gallery://item/$index",
        apiName = "Gallery",
        name = "Sample title ${index + 1}",
        posterUri = GalleryPosterModel,
        type = TvType.Movie,
        score = null,
    )
}
