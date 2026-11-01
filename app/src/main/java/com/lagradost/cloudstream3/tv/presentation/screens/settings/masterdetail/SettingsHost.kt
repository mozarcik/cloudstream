package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import kotlinx.coroutines.delay

private object SettingsHostTokens {
    const val PreviewDebounceMs = 120L
    val DebugShape = RoundedCornerShape(TvDimens.SettingsDebugRadius)
}

/**
 * Settings as two cards (Google TV model): the left one is the active level, the right one is a
 * dimmed, non-interactive preview of the next level for the focused row (empty when the row has
 * none). OK / Right on a row with a chevron slides the preview in as the new active level;
 * Left / Back goes up and puts focus back on the row we came from.
 */
@Composable
fun SettingsHost(
    hostState: SettingsHostState,
    onExitSettings: () -> Unit,
    onTopBarFocusableChanged: (Boolean) -> Unit,
    onTopBarDownNavigationEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onTopBarVisibleChanged: (Boolean) -> Unit = {},
    topBarFocusRequester: FocusRequester? = null,
    isTopBarFocused: Boolean = false,
    showDebugOverlay: Boolean = false
) {
    val onExitSettingsState by rememberUpdatedState(onExitSettings)
    val onTopBarFocusableChangedState by rememberUpdatedState(onTopBarFocusableChanged)
    val onTopBarVisibleChangedState by rememberUpdatedState(onTopBarVisibleChanged)
    val onTopBarDownNavigationEnabledChangedState by rememberUpdatedState(
        onTopBarDownNavigationEnabledChanged
    )
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val activeInstance = hostState.activeInstance
    val activeScreen = hostState.screen(activeInstance.id)
    val activeScreenData = hostState.dataState(activeInstance.id)

    // Level 1: Back focuses the Settings tab, the next Back leaves to Home (like the other tabs).
    val isTopBarFocusedState by rememberUpdatedState(isTopBarFocused)
    BackHandler(enabled = true) {
        when {
            hostState.navigateBack() -> Unit
            !isTopBarFocusedState && topBarFocusRequester != null -> topBarFocusRequester.requestFocus()
            else -> onExitSettingsState()
        }
    }

    LaunchedEffect(hostState.depth) {
        onTopBarFocusableChangedState(hostState.depth == 1)
        // Deeper levels use the whole screen (mockup: cards start 24 dp from the top).
        onTopBarVisibleChangedState(hostState.depth == 1)
    }
    DisposableEffect(Unit) {
        onDispose { onTopBarVisibleChangedState(true) }
    }

    val isTopBarDownNavigationEnabled = remember(activeInstance.id, activeScreen, activeScreenData) {
        if (activeScreen.hasCustomContent) {
            true
        } else {
            when (activeScreenData) {
                is SettingsScreenDataState.Ready ->
                    activeScreenData.entries.any { entry -> entry.isFocusable() }

                else -> false
            }
        }
    }

    LaunchedEffect(activeInstance.id, isTopBarDownNavigationEnabled) {
        onTopBarDownNavigationEnabledChangedState(isTopBarDownNavigationEnabled)
    }

    val topGap by animateDpAsState(
        targetValue = if (hostState.depth == 1) TvDimens.SettingsTopGap else TvDimens.SettingsTopGapDeep,
        animationSpec = tween(TvDimens.MaxAnimationMs),
        label = "SettingsTopGap"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(TvDimens.SettingsCardGap),
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = TvDimens.ScreenHorizontalPadding,
                    end = TvDimens.ScreenHorizontalPadding,
                    top = topGap,
                    bottom = TvDimens.SettingsBottomGap
                )
        ) {
            Box(
                modifier = Modifier
                    .width(TvDimens.SettingsCardWidth)
                    .fillMaxHeight()
                    .clip(SettingsCardShape)
                    .background(TvColors.Card)
                    .onPreviewKeyEvent { event ->
                        // Left is Back on every level except where a row uses it (slider).
                        if (
                            event.type == KeyEventType.KeyUp &&
                            event.key == Key.DirectionLeft &&
                            hostState.depth > 1 &&
                            hostState.focusedEntry(hostState.activeInstance)?.type != SettingsEntryType.Slider
                        ) {
                            backDispatcher?.onBackPressed()
                            backDispatcher != null
                        } else {
                            false
                        }
                    }
                    .padding(
                        start = TvDimens.SettingsCardPaddingHorizontal,
                        end = TvDimens.SettingsCardPaddingHorizontal,
                        top = TvDimens.SettingsCardPaddingVertical
                    )
            ) {
                AnimatedContent(
                    targetState = activeInstance,
                    transitionSpec = { settingsPanelTransition(hostState.navigationDirection) },
                    label = "SettingsLeftPanelTransition",
                    modifier = Modifier.fillMaxSize()
                ) { panelInstance ->
                    ActivePanelContent(
                        hostState = hostState,
                        hostDepth = hostState.depth,
                        instance = panelInstance,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(TvDimens.SettingsPreviewWidth)
                    .fillMaxHeight()
                    .clip(SettingsCardShape)
                    .background(TvColors.Preview)
                    .padding(
                        start = TvDimens.SettingsPreviewPaddingHorizontal,
                        end = TvDimens.SettingsPreviewPaddingHorizontal,
                        top = TvDimens.SettingsCardPaddingVertical
                    )
            ) {
                AnimatedContent(
                    targetState = activeInstance,
                    transitionSpec = {
                        fadeIn(tween(TvDimens.MaxAnimationMs)) togetherWith fadeOut(tween(TvDimens.MaxAnimationMs))
                    },
                    label = "SettingsRightPanelTransition",
                    modifier = Modifier.fillMaxSize()
                ) { panelInstance ->
                    PreviewPanelContent(
                        hostState = hostState,
                        activeInstance = panelInstance,
                        showDebugOverlay = showDebugOverlay,
                        modifier = Modifier.fillMaxSize()
                    )
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
    modifier: Modifier = Modifier
) {
    val screen = hostState.screen(instance.id)
    // Computed once per instance: while the level animates out it is no longer in the stack.
    val crumb = remember(instance.instanceId) { hostState.breadcrumbFor(instance) }

    if (screen.hasCustomContent) {
        CompositionLocalProvider(LocalSettingsBreadcrumb provides crumb) {
            screen.Content(
                modifier = modifier,
                contentPadding = PaddingValues(TvDimens.SettingsDp0),
                isPreview = false,
                onBack = { hostState.navigateBack() },
                onDataChanged = { screenPrefix ->
                    hostState.invalidateScreens { screenId ->
                        screenId.startsWith(screenPrefix)
                    }
                }
            )
        }
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
            crumb = crumb,
            modifier = modifier
        )
        is SettingsScreenDataState.Ready -> ActiveSettingsPanelList(
            title = panelTitle,
            crumb = crumb,
            description = screen.description,
            entries = currentData.entries,
            listState = instance.listState,
            restoreFocusToken = instance.instanceId,
            autoRequestFocus = instance.focusKey != null || hostDepth > 1,
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
            onRadioSelected = { entry ->
                hostState.selectRadio(screenId = instance.id, stableKey = entry.stableKey)
                hostState.refreshParents()
            },
            onEntryUpdated = { updatedEntry ->
                hostState.updateEntry(
                    screenId = instance.id,
                    stableKey = updatedEntry.stableKey
                ) {
                    updatedEntry
                }
                hostState.refreshParents()
            }
        )
        is SettingsScreenDataState.Error -> SettingsErrorPanel(
            title = panelTitle,
            crumb = crumb,
            message = currentData.message,
            errorTitle = currentData.title,
            onRetry = { hostState.retry(instance.id) },
            modifier = modifier
        )
    }
}

@Composable
private fun PreviewPanelContent(
    hostState: SettingsHostState,
    activeInstance: ScreenInstance,
    showDebugOverlay: Boolean,
    modifier: Modifier = Modifier
) {
    val activeEntries = (hostState.dataState(activeInstance.id) as? SettingsScreenDataState.Ready)
        ?.entries
        .orEmpty()
    val previewId by remember(activeInstance.instanceId, activeEntries) {
        derivedStateOf {
            if (activeEntries.isEmpty()) return@derivedStateOf null
            val focusedKey = activeInstance.focusKey ?: activeEntries.firstOrNull { it.isFocusable() }?.stableKey
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
            if (previewScreenId == null) {
                EmptyPreviewScreen(modifier = Modifier.fillMaxSize())
            } else {
                val previewScreen = hostState.screen(previewScreenId)
                if (previewScreen.hasCustomContent) {
                    previewScreen.Content(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(TvDimens.SettingsDp0),
                        isPreview = true,
                        onBack = {},
                        onDataChanged = { screenPrefix ->
                            hostState.invalidateScreens { screenId ->
                                screenId.startsWith(screenPrefix)
                            }
                        }
                    )
                } else {
                    when (val previewData = hostState.dataState(previewScreenId)) {
                        is SettingsScreenDataState.Ready -> PreviewSettingsPanelList(
                            title = previewScreen.title,
                            entries = previewData.entries,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Loading / error of the next level show up once it is entered.
                        else -> EmptyPreviewScreen(modifier = Modifier.fillMaxSize())
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
                    .padding(TvDimens.SettingsDebugPadding)
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
                color = MaterialTheme.colorScheme.surface.copy(alpha = TvDimens.SettingsDebugAlpha),
                shape = SettingsHostTokens.DebugShape
            )
            .padding(horizontal = TvDimens.SettingsDebugHorizontalPadding, vertical = TvDimens.SettingsDebugVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsDebugSpacing)
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
