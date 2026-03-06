package com.lagradost.cloudstream3.tv.presentation.screens.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.common.HaloHost
import com.lagradost.cloudstream3.tv.presentation.screens.home.FeedSection
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeedLoadState
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamCardShape

@Composable
fun SearchScreen(
    onMediaClick: (MediaItemCompat) -> Unit,
    onOpenFeedGrid: (SearchSectionUiState) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingPrefillQuery by SearchPrefillStore.pendingQuery.collectAsState()
    val listState = rememberLazyListState()
    val searchFieldFocusRequester = remember { FocusRequester() }
    val firstFeedCardFocusRequester = remember { FocusRequester() }
    val statusCardFocusRequester = remember { FocusRequester() }
    val searchErrorMessage = stringResource(id = R.string.tv_search_failed_to_load)
    val firstFocusableSectionIndex = remember(uiState.sections, uiState.hasSearched) {
        uiState.sections.indexOfFirst { section ->
            section.isFocusable(hasSearched = uiState.hasSearched)
        }
    }
    val shouldShowTopBar by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset < 100
        }
    }

    LaunchedEffect(shouldShowTopBar) {
        onScroll(shouldShowTopBar)
    }

    LaunchedEffect(pendingPrefillQuery) {
        val query = pendingPrefillQuery?.trim().orEmpty()
        if (query.isBlank()) return@LaunchedEffect

        viewModel.onQueryChanged(query)
        SearchPrefillStore.clearPendingQuery()
    }

    HaloHost(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    SearchInputField(
                        value = uiState.query,
                        onValueChange = viewModel::onQueryChanged,
                        onSearchAction = viewModel::onSearchSubmitted,
                        focusRequester = searchFieldFocusRequester,
                        downFocusRequester = when {
                            firstFocusableSectionIndex >= 0 -> firstFeedCardFocusRequester
                            uiState.hasSearched && uiState.sections.isEmpty() -> statusCardFocusRequester
                            else -> null
                        }
                    )
                }

                when {
                    uiState.sections.isNotEmpty() -> {
                        itemsIndexed(
                            items = uiState.sections,
                            key = { _, section -> section.id }
                        ) { index, section ->
                            FeedSection(
                                title = section.title,
                                state = section.state,
                                onMediaClick = onMediaClick,
                                onShowMoreClick = {
                                    onOpenFeedGrid(section)
                                },
                                isInteractive = section.isFocusable(hasSearched = uiState.hasSearched),
                                errorMessage = searchErrorMessage,
                                firstItemFocusRequester = if (index == firstFocusableSectionIndex) {
                                    firstFeedCardFocusRequester
                                } else {
                                    null
                                }
                            )
                        }
                    }

                    uiState.hasSearched -> {
                        item {
                            SearchStatusCard(
                                title = stringResource(id = R.string.title_search),
                                message = stringResource(id = R.string.tv_feed_empty),
                                focusRequester = statusCardFocusRequester,
                                isFocusable = true
                            )
                        }
                    }

                    else -> {
                        item {
                            SearchStatusCard(
                                title = stringResource(id = R.string.title_search),
                                message = stringResource(id = R.string.search_hint),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun SearchSectionUiState.isFocusable(hasSearched: Boolean): Boolean {
    if (isInteractive) return true
    if (!hasSearched) return false

    return when (state) {
        HomeFeedLoadState.Error -> true
        is HomeFeedLoadState.Success -> items.isEmpty()
        HomeFeedLoadState.Loading -> false
    }
}

@Composable
private fun SearchInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearchAction: () -> Unit,
    focusRequester: FocusRequester,
    downFocusRequester: FocusRequester?,
) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.border
        },
        label = "search-input-border-color"
    )

    Surface(
        onClick = { focusRequester.requestFocus() },
        shape = ClickableSurfaceDefaults.shape(shape = CloudStreamCardShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.inverseOnSurface,
            focusedContainerColor = MaterialTheme.colorScheme.inverseOnSurface,
            pressedContainerColor = MaterialTheme.colorScheme.inverseOnSurface,
            focusedContentColor = MaterialTheme.colorScheme.onSurface,
            pressedContentColor = MaterialTheme.colorScheme.onSurface
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(width = if (isFocused) 2.dp else 1.dp, color = borderColor),
                shape = CloudStreamCardShape
            )
        ),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            interactionSource = interactionSource,
            textStyle = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearchAction()
                    focusManager.moveFocus(FocusDirection.Down)
                }
            ),
            cursorBrush = Brush.verticalGradient(
                colors = listOf(LocalContentColor.current, LocalContentColor.current)
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .padding(start = 20.dp, end = 20.dp)
                ) {
                    innerTextField()
                    if (value.isBlank()) {
                        Text(
                            text = stringResource(id = R.string.tv_search_input_placeholder),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.graphicsLayer { alpha = 0.6f }
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .focusRequester(focusRequester)
                .focusProperties {
                    down = downFocusRequester ?: FocusRequester.Default
                }
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                    when (keyEvent.key) {
                        Key.Enter,
                        Key.NumPadEnter -> {
                            onSearchAction()
                            focusManager.moveFocus(FocusDirection.Down)
                            true
                        }

                        else -> false
                    }
                }
        )
    }
}

@Composable
private fun SearchStatusCard(
    title: String,
    message: String,
    focusRequester: FocusRequester? = null,
    isFocusable: Boolean = false,
) {
    Surface(
        onClick = { },
        enabled = isFocusable,
        shape = ClickableSurfaceDefaults.shape(shape = CloudStreamCardShape),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                ),
                shape = CloudStreamCardShape
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
            focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            pressedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContentColor = MaterialTheme.colorScheme.onSurface,
            pressedContentColor = MaterialTheme.colorScheme.onSurface
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )
            .focusProperties { canFocus = isFocusable }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), shape = CloudStreamCardShape)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                )
            }
        }
    }
}
