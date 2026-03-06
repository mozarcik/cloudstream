package com.lagradost.cloudstream3.tv.presentation.screens.sources

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.tv.compat.home.SourceRepository
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SourceSelectionMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSourceSelected: (MainAPI) -> Unit,
    modifier: Modifier = Modifier
) {
    var availableApis by remember { mutableStateOf<List<MainAPI>>(emptyList()) }
    val currentApi by SourceRepository.selectedApi.collectAsState(initial = null)
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (visible) {
            kotlinx.coroutines.delay(100)
            availableApis = SourceRepository.getAvailableApis()
        }
    }

    BackHandler(enabled = visible, onBack = onDismiss)

    FocusRequestEffect(
        requester = firstItemFocusRequester,
        requestKey = visible to availableApis.firstOrNull()?.name,
        enabled = visible && availableApis.isNotEmpty()
    )

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.4f)
                    .padding(32.dp),
                shape = MaterialTheme.shapes.medium,
                colors = SurfaceDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Select Source",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (availableApis.isEmpty()) {
                        Text(
                            text = "No sources available with main page support",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusProperties {
                                    onExit = { FocusRequester.Cancel }
                                }
                        ) {
                            itemsIndexed(
                                items = availableApis,
                                key = { index, api -> "${api.name}_$index" }
                            ) { index, api ->
                                SourceItem(
                                    api = api,
                                    isSelected = api == currentApi,
                                    onClick = {
                                        onSourceSelected(api)
                                    },
                                    focusRequester = if (index == 0) firstItemFocusRequester else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceItem(
    api: MainAPI,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    
    ListItem(
        selected = isFocused,
        onClick = onClick,
        leadingContent = {
            Text(
                text = if (isSelected) "●" else "○",
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.titleLarge
            )
        },
        headlineContent = {
            Text(
                text = api.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isFocused) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            )
            .onFocusChanged { isFocused = it.isFocused }
    )
}
