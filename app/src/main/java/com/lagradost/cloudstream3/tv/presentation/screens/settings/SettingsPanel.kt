package com.lagradost.cloudstream3.tv.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/**
 * Unified panel component used for both left and right panels.
 * The same rendering logic is used - only data and interactivity differ.
 */
@Composable
fun SettingsPanel(
    node: SettingsNode?,
    selectedIndex: Int,
    focusRequester: FocusRequester,
    isFocused: Boolean,
    isInteractive: Boolean,
    onItemSelected: (Int) -> Unit,
    onItemClick: (SettingsNode) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onBack: () -> Unit = {},
    level: Int,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isFocused) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(backgroundColor)
            .padding(vertical = 16.dp, horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Title
            node?.title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                )
            }

            // If node is a leaf (has content), render the content widget
            if (node?.isLeaf == true && node.content != null) {
                node.content.invoke(onBack)
                return@Column
            }

            // Items list
            val items = node?.children ?: emptyList()
            if (items.isEmpty()) return@Column

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isInteractive) {
                            Modifier
                                .focusGroup()
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    onFocusChanged(focusState.hasFocus)
                                }
                        } else {
                            Modifier
                        }
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(items) { index, item ->
                    val itemFocusRequester = remember { FocusRequester() }

                    SettingsRow(
                        node = item,
                        isSelected = index == selectedIndex && isInteractive,
                        focusRequester = itemFocusRequester,
                        isEnabled = isInteractive,
                        onFocusChanged = { isFocused ->
                            if (isFocused && isInteractive) onItemSelected(index)
                        },
                        onClick = { if (isInteractive) onItemClick(item) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Auto-focus selected item (only for interactive panels)
                    if (isInteractive && index == selectedIndex) {
                        LaunchedEffect(level) {
                            itemFocusRequester.requestFocus()
                        }
                    }
                }
            }
        }
    }
}
