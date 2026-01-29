package com.lagradost.cloudstream3.tv.presentation.screens.sources

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.tv.compat.home.SourceRepository

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SourceSelectionMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSourceSelected: (MainAPI) -> Unit,
    modifier: Modifier = Modifier
) {
    // Use State instead of remember - will update when APIs are loaded
    var availableApis by remember { mutableStateOf<List<MainAPI>>(emptyList()) }
    val currentApi by SourceRepository.selectedApi.collectAsState(initial = null)
    val firstItemFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    
    // Debug: Log when visible changes
    LaunchedEffect(visible) {
        android.util.Log.d("SourceMenu", "Visibility changed: $visible")
    }
    
    // Load APIs when menu becomes visible - with delay to ensure plugins are loaded
    LaunchedEffect(visible) {
        if (visible) {
            android.util.Log.d("SourceMenu", "Starting API load...")
            kotlinx.coroutines.delay(100) // Give plugins time to load
            
            val apis = SourceRepository.getAvailableApis()
            android.util.Log.d("SourceMenu", "Loaded ${apis.size} APIs from SourceRepository")
            
            availableApis = apis
            
            apis.take(5).forEach { 
                android.util.Log.d("SourceMenu", "  - ${it.name} (hasMainPage=${it.hasMainPage})") 
            }
            
            // Auto-focus first item when menu opens
            if (apis.isNotEmpty()) {
                kotlinx.coroutines.delay(100)
                firstItemFocusRequester.requestFocus()
            }
        }
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.4f)
                    .padding(32.dp),
                shape = MaterialTheme.shapes.medium,
                colors = SurfaceDefaults.colors(
                    containerColor = Color.Black.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Select Source",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (availableApis.isEmpty()) {
                        Text(
                            text = "No sources available with main page support",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                // Block all navigation that would escape the menu
                                .onPreviewKeyEvent { keyEvent ->
                                    val currentIndex = listState.firstVisibleItemIndex
                                    
                                    when (keyEvent.key) {
                                        Key.DirectionLeft, Key.DirectionRight -> {
                                            android.util.Log.d("SourceMenu", "Blocked ${keyEvent.key}")
                                            true // Always block left/right
                                        }
                                        Key.DirectionUp -> {
                                            // Block Up if we're at the first item
                                            if (currentIndex == 0) {
                                                android.util.Log.d("SourceMenu", "Blocked Up at first item")
                                                true
                                            } else {
                                                false // Allow Up within list
                                            }
                                        }
                                        else -> false // Allow Down and other keys
                                    }
                                }
                        ) {
                            items(availableApis) { api ->
                                val isFirst = availableApis.indexOf(api) == 0
                                SourceItem(
                                    api = api,
                                    isSelected = api == currentApi,
                                    onClick = {
                                        onSourceSelected(api)
                                    },
                                    focusRequester = if (isFirst) firstItemFocusRequester else null
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
                color = if (isSelected) Color.Green else Color.Gray,
                style = MaterialTheme.typography.titleLarge
            )
        },
        headlineContent = {
            Text(
                text = api.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isFocused) Color.Black else Color.White
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
