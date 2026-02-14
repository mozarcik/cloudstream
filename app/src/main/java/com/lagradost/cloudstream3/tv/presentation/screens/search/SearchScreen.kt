package com.lagradost.cloudstream3.tv.presentation.screens.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
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
import com.lagradost.cloudstream3.tv.presentation.screens.home.FeedSection
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeedLoadState
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamCardShape

private const val SEARCH_PLACEHOLDER_COUNT = 3

@Composable
fun SearchScreen(
    onMediaClick: (MediaItemCompat) -> Unit,
    onOpenFeedGrid: (SearchSectionUiState) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val searchFieldFocusRequester = remember { FocusRequester() }
    val firstFeedCardFocusRequester = remember { FocusRequester() }
    val shouldShowTopBar by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset < 100
        }
    }

    LaunchedEffect(shouldShowTopBar) {
        onScroll(shouldShowTopBar)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C1016))
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
                    focusRequester = searchFieldFocusRequester,
                    downFocusRequester = when {
                        uiState.sections.isNotEmpty() -> firstFeedCardFocusRequester
                        else -> null
                    }
                )
            }

            when {
                uiState.isLoading -> {
                    items(SEARCH_PLACEHOLDER_COUNT) {
                        FeedSection(
                            title = stringResource(id = R.string.loading),
                            state = HomeFeedLoadState.Loading,
                            onMediaClick = {},
                            onShowMoreClick = {},
                            isInteractive = false
                        )
                    }
                }

                uiState.sections.isNotEmpty() -> {
                    itemsIndexed(
                        items = uiState.sections,
                        key = { _, section -> section.id }
                    ) { index, section ->
                        FeedSection(
                            title = section.title,
                            state = HomeFeedLoadState.Success(section.items),
                            onMediaClick = onMediaClick,
                            onShowMoreClick = {
                                onOpenFeedGrid(section)
                            },
                            isInteractive = true,
                            firstItemFocusRequester = if (index == 0) {
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

@Composable
private fun SearchInputField(
    value: String,
    onValueChange: (String) -> Unit,
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
                onSearch = { focusManager.moveFocus(FocusDirection.Down) }
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
                        Key.DirectionDown -> {
                            focusManager.moveFocus(FocusDirection.Down)
                            true
                        }

                        Key.DirectionUp -> {
                            focusManager.moveFocus(FocusDirection.Up)
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
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Color.White.copy(alpha = 0.05f), shape = CloudStreamCardShape)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = CloudStreamCardShape
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.82f),
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.66f)
            )
        }
    }
}
