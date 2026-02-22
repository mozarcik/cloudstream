package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

private object SettingsHostTokens {
    const val LeftPanelWeight = 0.55f
    const val RightPanelWeight = 0.45f
    const val PreviewDebounceMs = 120L
    val LeftPanelStartPadding = 48.dp
    val LeftPanelEndPadding = 86.dp
    val RightPanelStartPadding = 14.dp
    val RightPanelEndPadding = 24.dp
    val PanelContentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp)
    val LeftPanelContentPadding = PanelContentPadding
    val RightPanelContentPadding = PanelContentPadding
    val PreviewTonalElevation = 2.dp
    val DebugPadding = 10.dp
    val DebugShape = RoundedCornerShape(10.dp)
}

@Composable
fun SettingsHost(
    hostState: SettingsHostState,
    onExitSettings: () -> Unit,
    onTopBarFocusableChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showDebugOverlay: Boolean = false
) {
    val onExitSettingsState by rememberUpdatedState(onExitSettings)
    val onTopBarFocusableChangedState by rememberUpdatedState(onTopBarFocusableChanged)
    val activeInstance = hostState.activeInstance
    val surfaceColor = MaterialTheme.colorScheme.surface
    val inactivePreviewColor = remember(surfaceColor) {
        Color.Black.copy(alpha = 0.30f).compositeOver(surfaceColor)
    }
    val inactivePreviewEmptyColor = remember(surfaceColor) {
        Color.Black.copy(alpha = 0.36f).compositeOver(surfaceColor)
    }

    BackHandler(enabled = true) {
        if (!hostState.navigateBack()) {
            onExitSettingsState()
        }
    }

    LaunchedEffect(hostState.depth) {
        onTopBarFocusableChangedState(hostState.depth == 1)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(SettingsHostTokens.LeftPanelWeight)
                    .padding(
                        start = SettingsHostTokens.LeftPanelStartPadding,
                        end = SettingsHostTokens.LeftPanelEndPadding
                    )
            ) {
                AnimatedContent(
                    targetState = activeInstance,
                    transitionSpec = { settingsPanelTransition(hostState.navigationDirection) },
                    label = "SettingsLeftPanelTransition",
                    modifier = Modifier.fillMaxSize()
                ) { panelInstance ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .onPreviewKeyEvent { event ->
                                if (
                                    event.type == KeyEventType.KeyUp &&
                                    event.key == Key.DirectionLeft &&
                                    hostState.depth > 1
                                ) {
                                    val focusedType = hostState.focusedEntry(panelInstance)?.type
                                    if (
                                        focusedType == SettingsEntryType.Slider ||
                                        focusedType == SettingsEntryType.Toggle
                                    ) {
                                        return@onPreviewKeyEvent false
                                    }
                                    hostState.navigateBack()
                                    true
                                } else {
                                    false
                                }
                            },
                        colors = SurfaceDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        ActivePanelContent(
                            hostState = hostState,
                            hostDepth = hostState.depth,
                            instance = panelInstance,
                            contentPadding = SettingsHostTokens.LeftPanelContentPadding,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(SettingsHostTokens.RightPanelWeight)
                    .padding(
                        start = SettingsHostTokens.RightPanelStartPadding,
                        end = SettingsHostTokens.RightPanelEndPadding
                    )
            ) {
                AnimatedContent(
                    targetState = activeInstance,
                    transitionSpec = { settingsPanelTransition(hostState.navigationDirection) },
                    label = "SettingsRightPanelTransition",
                    modifier = Modifier.fillMaxSize()
                ) { panelInstance ->
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        colors = SurfaceDefaults.colors(
                            containerColor = inactivePreviewColor
                        ),
                        tonalElevation = SettingsHostTokens.PreviewTonalElevation
                    ) {
                        PreviewPanelContent(
                            hostState = hostState,
                            activeInstance = panelInstance,
                            showDebugOverlay = showDebugOverlay,
                            emptyBackgroundColor = inactivePreviewEmptyColor,
                            contentPadding = SettingsHostTokens.RightPanelContentPadding,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivePanelContent(
    hostState: SettingsHostState,
    hostDepth: Int,
    instance: ScreenInstance,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val screen = hostState.screen(instance.id)
    if (screen.hasCustomContent) {
        screen.Content(
            modifier = modifier,
            contentPadding = contentPadding,
            isPreview = false,
            onBack = { hostState.navigateBack() },
            onDataChanged = { screenPrefix ->
                hostState.invalidateScreens { screenId ->
                    screenId.startsWith(screenPrefix)
                }
            }
        )
        return
    }

    val panelTitle = screen.title
    val screenData = hostState.dataState(instance.id)
    val refreshVersion = hostState.refreshVersion(instance.id)

    LaunchedEffect(instance.id, refreshVersion) {
        hostState.ensureLoaded(instance.id)
    }

    when (val currentData = screenData) {
        null,
        is SettingsScreenDataState.Loading -> SettingsLoadingPanel(
            title = panelTitle,
            contentPadding = contentPadding,
            modifier = modifier
        )
        is SettingsScreenDataState.Ready -> ActiveSettingsPanelList(
            title = panelTitle,
            entries = currentData.entries,
            listState = instance.listState,
            restoreFocusToken = instance.instanceId,
            autoRequestFocus = instance.focusKey != null || hostDepth > 1,
            contentPadding = contentPadding,
            modifier = modifier,
            resolveFocusedKey = { instance.focusKey },
            onEntryFocused = { entry ->
                if (instance.focusKey != entry.stableKey) {
                    instance.focusKey = entry.stableKey
                }
            },
            onNavigateForward = { entry ->
                val nextId = entry.nextScreenId ?: return@ActiveSettingsPanelList
                hostState.navigateForward(
                    parentInstanceId = instance.instanceId,
                    nextScreenId = nextId
                )
            },
            onEntryUpdated = { updatedEntry ->
                hostState.updateEntry(
                    screenId = instance.id,
                    stableKey = updatedEntry.stableKey
                ) {
                    updatedEntry
                }
            }
        )
        is SettingsScreenDataState.Error -> SettingsErrorPanel(
            title = panelTitle,
            message = currentData.message,
            contentPadding = contentPadding,
            modifier = modifier
        )
    }
}

@Composable
private fun PreviewPanelContent(
    hostState: SettingsHostState,
    activeInstance: ScreenInstance,
    showDebugOverlay: Boolean,
    emptyBackgroundColor: Color,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val activeEntries = (hostState.dataState(activeInstance.id) as? SettingsScreenDataState.Ready)
        ?.entries
        .orEmpty()
    val previewId by remember(activeInstance.instanceId, activeEntries) {
        derivedStateOf {
            if (activeEntries.isEmpty()) return@derivedStateOf null
            val focusedKey = activeInstance.focusKey ?: activeEntries.first().stableKey
            activeEntries.firstOrNull { it.stableKey == focusedKey }?.nextScreenId
        }
    }

    var debouncedPreviewId by remember(activeInstance.instanceId) { mutableStateOf<String?>(null) }
    val previewRefreshVersion = previewId?.let { screenId ->
        hostState.refreshVersion(screenId)
    }

    LaunchedEffect(activeInstance.instanceId, previewId, previewRefreshVersion) {
        val targetPreviewId = previewId
        if (targetPreviewId == null) {
            debouncedPreviewId = null
            return@LaunchedEffect
        }

        hostState.preparePreviewInstance(
            parentInstanceId = activeInstance.instanceId,
            screenId = targetPreviewId
        )

        delay(SettingsHostTokens.PreviewDebounceMs)
        debouncedPreviewId = targetPreviewId
        val previewScreen = hostState.screen(targetPreviewId)
        if (!previewScreen.hasCustomContent) {
            hostState.ensureLoaded(targetPreviewId)
        }
    }

    Box(modifier = modifier) {
        AnimatedContent(
            targetState = debouncedPreviewId,
            transitionSpec = { settingsPreviewFadeTransition() },
            label = "SettingsPreviewSwap",
            modifier = Modifier.fillMaxSize()
        ) { previewScreenId ->
            when {
                previewScreenId == null -> EmptyPreviewScreen(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = emptyBackgroundColor,
                    contentPadding = contentPadding
                )
                else -> {
                    val previewScreen = hostState.screen(previewScreenId)
                    if (previewScreen.hasCustomContent) {
                        previewScreen.Content(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = contentPadding,
                            isPreview = true,
                            onBack = {},
                            onDataChanged = { screenPrefix ->
                                hostState.invalidateScreens { screenId ->
                                    screenId.startsWith(screenPrefix)
                                }
                            }
                        )
                    } else {
                        val title = previewScreen.title
                        when (val previewData = hostState.dataState(previewScreenId)) {
                            null,
                            is SettingsScreenDataState.Loading -> SettingsLoadingPanel(
                                title = title,
                                contentPadding = contentPadding,
                                modifier = Modifier.fillMaxSize()
                            )
                            is SettingsScreenDataState.Ready -> PreviewSettingsPanelList(
                                title = title,
                                entries = previewData.entries,
                                contentPadding = contentPadding,
                                modifier = Modifier.fillMaxSize()
                            )
                            is SettingsScreenDataState.Error -> SettingsErrorPanel(
                                title = title,
                                message = previewData.message,
                                contentPadding = contentPadding,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        if (showDebugOverlay) {
            SettingsDebugOverlay(
                currentScreenId = activeInstance.id,
                previewScreenId = debouncedPreviewId,
                focusedKey = activeInstance.focusKey,
                loadSource = debouncedPreviewId?.let { hostState.lastLoadSourceByScreenId[it] },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(SettingsHostTokens.DebugPadding)
            )
        }
    }
}

@Composable
private fun SettingsDebugOverlay(
    currentScreenId: String,
    previewScreenId: String?,
    focusedKey: String?,
    loadSource: SettingsLoadSource?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                shape = SettingsHostTokens.DebugShape
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "screenId: $currentScreenId",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "previewId: ${previewScreenId ?: "none"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "focusedKey: ${focusedKey ?: "none"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "previewLoad: ${loadSource?.name ?: "n/a"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
