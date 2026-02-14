package com.lagradost.cloudstream3.tv.presentation.common

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

data class SidePanelMenuItem(
    val id: String,
    val title: String,
    val titleMaxLines: Int = 1,
    val titleTextStyle: TextStyle? = null,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val isSectionHeader: Boolean = false,
    val isVisible: Boolean = true,
    val animateVisibility: Boolean = false,
    val itemStartPadding: Dp = 0.dp,
    val itemBackgroundColor: Color? = null,
    val itemBackgroundShape: Shape? = null,
    val focusedItemShape: Shape? = null,
    val testTag: String? = null,
    val onClick: () -> Unit,
    val onMenuClick: (() -> Unit)? = null,
    val onKeyUp: ((Int) -> Boolean)? = null,
    val supportingContent: (@Composable () -> Unit)? = null,
    val leadingContent: (@Composable () -> Unit)? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
)

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun MenuListSidePanel(
    visible: Boolean,
    onCloseRequested: () -> Unit,
    title: String,
    items: List<SidePanelMenuItem>,
    modifier: Modifier = Modifier,
    panelWidth: Dp = 340.dp,
    panelTestTag: String? = null,
    headerContent: (@Composable ColumnScope.() -> Unit)? = null,
    emptyContent: (@Composable () -> Unit)? = null,
    closeOnLeftPress: Boolean = true,
    initialFocusedItemId: String? = null,
    showSelectionRadio: Boolean = false,
) {
    val focusableItemIds = remember(items) {
        items.filter { menuItem ->
            !menuItem.isSectionHeader && menuItem.enabled && menuItem.isVisible
        }
            .map { menuItem -> menuItem.id }
    }
    val focusRequesters = remember(focusableItemIds) {
        focusableItemIds.associateWith { FocusRequester() }
    }

    LaunchedEffect(visible, initialFocusedItemId, focusableItemIds) {
        if (!visible || focusableItemIds.isEmpty()) return@LaunchedEffect
        delay(100)
        val focusItemId = initialFocusedItemId?.takeIf { focusRequesters.containsKey(it) }
            ?: focusableItemIds.first()
        focusRequesters[focusItemId]?.requestFocus()
    }

    SlidingSidePanel(
        visible = visible,
        onCloseRequested = onCloseRequested,
        panelWidth = panelWidth,
        panelTestTag = panelTestTag,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                modifier = Modifier.padding(20.dp),
                fontSize = 20.sp,
            )

            headerContent?.invoke(this)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .focusProperties {
                        onExit = {
                            onCloseRequested()
                            FocusRequester.Default
                        }
                    }
            ) {
                if (items.isEmpty()) {
                    item {
                        emptyContent?.invoke()
                    }
                } else {
                    items(
                        items = items,
                        key = { menuItem -> menuItem.id }
                    ) { menuItem ->
                        if (!menuItem.isVisible && !menuItem.animateVisibility) {
                            return@items
                        }

                        if (menuItem.isSectionHeader) {
                            Text(
                                text = menuItem.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                            )
                        } else {
                            var isFocused by remember(menuItem.id) { mutableStateOf(false) }

                            val rowContent: @Composable () -> Unit = {
                                ListItem(
                                    selected = menuItem.selected,
                                    onClick = menuItem.onClick,
                                    enabled = menuItem.enabled,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = menuItem.itemStartPadding)
                                        .then(
                                            menuItem.itemBackgroundColor?.let { color ->
                                                menuItem.itemBackgroundShape?.let { shape ->
                                                    Modifier.background(color = color, shape = shape)
                                                } ?: Modifier.background(color)
                                            } ?: Modifier
                                        )
                                        .then(
                                            if (isFocused) {
                                                menuItem.focusedItemShape?.let { shape ->
                                                    Modifier.clip(shape)
                                                } ?: Modifier
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .then(
                                            focusRequesters[menuItem.id]?.let { focusRequester ->
                                                Modifier.focusRequester(focusRequester)
                                            } ?: Modifier
                                        )
                                        .onFocusChanged { focusState ->
                                            isFocused = focusState.isFocused
                                        }
                                        .semantics(mergeDescendants = true) { }
                                        .then(
                                            if (menuItem.testTag.isNullOrBlank()) {
                                                Modifier
                                            } else {
                                                Modifier.testTag(menuItem.testTag)
                                            }
                                        )
                                        .onPreviewKeyEvent { keyEvent ->
                                            val nativeEvent = keyEvent.nativeKeyEvent
                                            if (nativeEvent.action != AndroidKeyEvent.ACTION_UP) {
                                                return@onPreviewKeyEvent false
                                            }

                                            if (menuItem.onKeyUp?.invoke(nativeEvent.keyCode) == true) {
                                                return@onPreviewKeyEvent true
                                            }

                                            when (nativeEvent.keyCode) {
                                                AndroidKeyEvent.KEYCODE_MENU -> {
                                                    val onMenuClick = menuItem.onMenuClick ?: return@onPreviewKeyEvent false
                                                    onMenuClick()
                                                    true
                                                }

                                                AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                                                AndroidKeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                                                    if (!closeOnLeftPress) {
                                                        return@onPreviewKeyEvent false
                                                    }
                                                    onCloseRequested()
                                                    true
                                                }

                                                else -> false
                                            }
                                        },
                                    headlineContent = {
                                        Column {
                                            Text(
                                                text = menuItem.title,
                                                style = menuItem.titleTextStyle ?: MaterialTheme.typography.titleMedium,
                                                maxLines = menuItem.titleMaxLines,
                                            )
                                            menuItem.supportingContent?.invoke()
                                        }
                                    },
                                    leadingContent = menuItem.leadingContent?.let { leading ->
                                        {
                                            leading()
                                        }
                                    },
                                    trailingContent = {
                                        when {
                                            menuItem.trailingContent != null -> menuItem.trailingContent.invoke()
                                            showSelectionRadio -> RadioButton(
                                                selected = menuItem.selected,
                                                onClick = { }
                                            )
                                        }
                                    }
                                )
                            }

                            if (menuItem.animateVisibility) {
                                AnimatedVisibility(
                                    visible = menuItem.isVisible,
                                    enter = expandVertically(animationSpec = tween(durationMillis = 220)),
                                    exit = shrinkVertically(animationSpec = tween(durationMillis = 220)),
                                ) {
                                    Box(modifier = Modifier.animateContentSize()) {
                                        rowContent()
                                    }
                                }
                            } else if (menuItem.isVisible) {
                                rowContent()
                            }
                        }
                    }
                }
            }
        }
    }
}
