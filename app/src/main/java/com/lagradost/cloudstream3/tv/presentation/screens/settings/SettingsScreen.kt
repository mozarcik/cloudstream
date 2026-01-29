package com.lagradost.cloudstream3.tv.presentation.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.ExtensionsUiState
import com.lagradost.cloudstream3.tv.presentation.screens.settings.extensions.ExtensionsViewModel

/**
 * Main Settings Screen for Google TV app.
 * Implements a Focus-Driven Master-Detail Two Pane Layout with unlimited depth navigation.
 * 
 * Both panels use the same rendering logic - only data and colors differ.
 * During navigation, the right panel visually becomes the left panel.
 */
@Composable
fun SettingsScreen(
    onExitSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onTopBarFocusableChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val extensionsViewModel = remember { ExtensionsViewModel() }
    
    // Setup SharedPreferences listener for auto-reload when plugins add repos
    LaunchedEffect(extensionsViewModel) {
        extensionsViewModel.setupPrefsListener(context)
    }
    
    // Collect extensions state to trigger updates
    val extensionsUiState by extensionsViewModel.uiState.collectAsState()
    val repoCount = if (extensionsUiState is ExtensionsUiState.Ready) {
        (extensionsUiState as ExtensionsUiState.Ready).repositories.size
    } else {
        0
    }
    
    // State MUST be declared before using it in logs
    var navigationState by remember { mutableStateOf(NavigationState()) }
    
    // Build tree on every recomposition - ID-based navigation preserves position
    val settingsTree = buildSettingsTree(extensionsViewModel)

    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }
    var isLeftPanelFocused by remember { mutableStateOf(true) }

    val leftPanelFocusRequester = remember { FocusRequester() }

    // Track navigation direction for animation
    var isNavigatingForward by remember { mutableStateOf(true) }
    
    // Track previous level to detect navigation vs selection change
    var previousLevel by remember { mutableIntStateOf(0) }
    
    // Control top bar focusability based on navigation level
    LaunchedEffect(navigationState.currentLevel) {
        onTopBarFocusableChanged(navigationState.currentLevel == 0)
    }

    // Create virtual node for root level (contains all categories)
    val rootNode = SettingsNode(
        id = "root",
        title = stringResource(R.string.title_settings),
        children = settingsTree
    )

    // Current node to display (root or deeper) - resolve from current tree using IDs
    val currentNode = navigationState.getCurrentNode(rootNode)

    // Determine what to show in left and right panels
    val leftNode = if (navigationState.isAtRoot) rootNode else (currentNode ?: rootNode)
    
    // Preview node - selected item from current, or null if we're on a leaf node
    val previewNode = if (leftNode.isLeaf) {
        null // Don't show preview for leaf nodes (they have content on the left)
    } else {
        leftNode.children.getOrNull(selectedItemIndex)
    }

    // Handle back navigation
    BackHandler(enabled = true) {
        when {
            // If we're deep in the navigation, go back one level
            !navigationState.isAtRoot -> {
                isNavigatingForward = false
                navigationState = navigationState.navigateBack()
                selectedItemIndex = 0
            }
            // Exit settings
            else -> {
                onExitSettings()
            }
        }
    }

    // Initialize focus on left panel
    LaunchedEffect(Unit) {
        leftPanelFocusRequester.requestFocus()
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 48.dp),
        colors = SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Panel - Current navigation level (interactive)
            // Animates with slide when navigation level changes
            // Use currentLevel as key to avoid re-triggering animation on state updates
            AnimatedContent(
                targetState = navigationState.currentLevel,
                transitionSpec = { settingsSlideTransition(isNavigatingForward) },
                label = "LeftPanelTransition",
                modifier = Modifier.width(480.dp)
            ) { animatedLevel ->
                // Calculate leftNode INSIDE AnimatedContent based on animatedLevel
                // This ensures the node is recalculated for each animation frame
                val animatedNavigationState = remember(animatedLevel) {
                    NavigationState(
                        pathIds = navigationState.pathIds.take(animatedLevel + 1)
                    )
                }
                val animatedCurrentNode = animatedNavigationState.getCurrentNode(rootNode)
                val animatedLeftNode = if (animatedNavigationState.isAtRoot) rootNode else (animatedCurrentNode ?: rootNode)
                
                SettingsPanel(
                    node = animatedLeftNode,
                    selectedIndex = selectedItemIndex,
                    focusRequester = leftPanelFocusRequester,
                    isFocused = isLeftPanelFocused,
                    isInteractive = true,
                    onItemSelected = { index -> selectedItemIndex = index },
                    onItemClick = { item ->
                        if (item.hasChildren || item.isLeaf) {
                            isNavigatingForward = true
                            navigationState = navigationState.navigateTo(item)
                            selectedItemIndex = 0
                        }
                    },
                    onFocusChanged = { hasFocus ->
                        if (hasFocus) isLeftPanelFocused = true
                    },
                    onBack = {
                        // Handle programmatic back (e.g., from form)
                        isNavigatingForward = false
                        navigationState = navigationState.navigateBack()
                    },
                    level = animatedLevel,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Right Panel - Preview of selected item (non-interactive)
            // Fades when selected item changes, slides when navigation level changes
            // Use level + node ID to avoid re-triggering on state updates
            AnimatedContent(
                targetState = Pair(navigationState.currentLevel, previewNode?.id),
                transitionSpec = {
                    val (oldLevel, _) = initialState
                    val (newLevel, _) = targetState
                    
                    // If level changed, use slide animation (same as left panel)
                    if (oldLevel != newLevel) {
                        settingsSlideTransition(isNavigatingForward)
                    } else {
                        // If only selected item changed, use fade
                        settingsFadeTransition()
                    }
                },
                label = "RightPanelTransition",
                modifier = Modifier.weight(1f)
            ) { (animatedLevel, animatedPreviewNodeId) ->
                // Calculate previewNode INSIDE AnimatedContent based on animatedLevel
                val animatedNavigationState = remember(animatedLevel) {
                    NavigationState(
                        pathIds = navigationState.pathIds.take(animatedLevel + 1)
                    )
                }
                val animatedCurrentNode = animatedNavigationState.getCurrentNode(rootNode)
                val animatedLeftNode = if (animatedNavigationState.isAtRoot) rootNode else (animatedCurrentNode ?: rootNode)
                val animatedPreviewNode = if (animatedLeftNode.isLeaf) {
                    null
                } else {
                    animatedLeftNode.children.find { it.id == animatedPreviewNodeId }
                }
                
                SettingsPanel(
                    node = animatedPreviewNode,
                    selectedIndex = -1,
                    focusRequester = remember { FocusRequester() },
                    isFocused = false,
                    isInteractive = false,
                    onItemSelected = {},
                    onItemClick = {},
                    onFocusChanged = {},
                    level = animatedLevel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
