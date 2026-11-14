package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.Animatable as ColorAnimatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import com.lagradost.cloudstream3.tv.presentation.focus.TvScreenFocusEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import kotlinx.coroutines.launch

private object SettingsHostTokens {
    val DebugShape = RoundedCornerShape(TvDimens.SettingsDebugRadius)
    val SlideEasing: Easing = FastOutSlowInEasing
    val SettleEasing: Easing = CubicBezierEasing(0.18f, 1f, 0.22f, 1f)
}

/** Look of a panel in the strip: only alpha and background change when its role changes. */
private enum class PanelRole { Past, Active, Preview }

/**
 * Settings as a horizontal strip of flat 504 dp panels (Google TV two-panel model). The 960 dp
 * window shows the active panel and, to its right, the next level of the focused row as a real,
 * dimmed, non-focusable panel. Going deeper slides the strip by one panel (the preview becomes
 * the active panel, same instance); Left / Back slides it back.
 */
@Composable
fun SettingsHost(
    hostState: SettingsHostState,
    onExitSettings: () -> Unit,
    modifier: Modifier = Modifier,
    showDebugOverlay: Boolean = false
) {
    val onExitSettingsState by rememberUpdatedState(onExitSettings)
    val scope = rememberCoroutineScope()
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val activeInstance = hostState.activeInstance
    val depth = hostState.depth

    fun goBack() {
        scope.launch { if (!hostState.navigateBackQueued()) onExitSettingsState() }
    }

    // The menu was closed: focus returns to the row it left (the rows of a lazy list all look the same
    // to Compose's own focus restoring).
    // Entering: the Dashboard's own content focus does it here.
    TvScreenFocusEffect(
        enter = { false },
        restore = {
            hostState.focusRestoreRequests += 1
            true
        },
    )
    // Back goes up a level; from the first level it belongs to the Dashboard (it opens the menu).
    BackHandler(enabled = depth > 1) { goBack() }

    // The next level of the focused row, created as soon as focus lands on it (no debounce).
    val previewScreenId by remember(activeInstance.instanceId) {
        derivedStateOf {
            val entries = hostState.readyEntries(activeInstance.id).orEmpty()
            if (entries.isEmpty()) return@derivedStateOf null
            val focusedKey = activeInstance.focusKey ?: entries.firstOrNull { it.isFocusable() }?.stableKey
            entries.firstOrNull { it.stableKey == focusedKey }?.nextScreenId
        }
    }
    LaunchedEffect(activeInstance.instanceId, previewScreenId) {
        previewScreenId?.let { hostState.preparePreviewInstance(activeInstance.instanceId, it) }
    }
    val previewInstance = previewScreenId?.let { hostState.previewInstanceOrNull(activeInstance.instanceId, it) }

    val paneWidthPx = with(LocalDensity.current) { TvDimens.SettingsPaneWidth.toPx() }
    val offset = remember { Animatable((depth - 1) * paneWidthPx) }
    LaunchedEffect(depth, paneWidthPx) {
        offset.animateTo(
            (depth - 1) * paneWidthPx,
            tween(TvDimens.SettingsPanelSlideMs, easing = SettingsHostTokens.SlideEasing)
        )
    }

    val setInfoPanel = remember(hostState) { { content: (@Composable () -> Unit)? -> hostState.infoPanel = content } }
    CompositionLocalProvider(LocalSettingsInfoPanel provides setInfoPanel) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onPreviewKeyEvent { event ->
                // Left is Back on every level except where a row uses it (slider).
                if (
                    event.type == KeyEventType.KeyDown &&
                    event.key == Key.DirectionLeft &&
                    event.nativeKeyEvent.repeatCount == 0 &&
                    depth > 1 &&
                    hostState.focusedEntry(hostState.activeInstance)?.type != SettingsEntryType.Slider
                ) {
                    // Through the dispatcher, so a level's own Back handling (login flow, confirmation) wins.
                    backDispatcher?.onBackPressed()
                    backDispatcher != null
                } else {
                    false
                }
            }
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .fillMaxHeight()
                .graphicsLayer { translationX = -offset.value }
        ) {
            hostState.stack.forEachIndexed { index, instance ->
                key(instance.instanceId) {
                    SettingsPanel(
                        hostState = hostState,
                        instance = instance,
                        role = if (index == depth - 1) PanelRole.Active else PanelRole.Past,
                        hostDepth = depth,
                    )
                }
            }
            if (previewInstance != null) {
                key(previewInstance.instanceId) {
                    SettingsPanel(
                        hostState = hostState,
                        instance = previewInstance,
                        role = PanelRole.Preview,
                        hostDepth = depth,
                    )
                }
            } else {
                // Nothing to preview: an empty info panel (alpha 1, Preview background).
                Box(
                    Modifier
                        .width(TvDimens.SettingsPaneWidth)
                        .fillMaxHeight()
                        .background(TvColors.Preview)
                ) {
                    hostState.infoPanel?.invoke()
                }
            }
        }

        if (showDebugOverlay) {
            SettingsDebugOverlay(
                currentScreenId = activeInstance.id,
                previewScreenId = previewScreenId,
                focusedKey = activeInstance.focusKey,
                loadSource = previewScreenId?.let { hostState.lastLoadSourceByScreenId[it] },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(TvDimens.SettingsDebugPadding)
            )
        }
    }
    }
}

/**
 * One 504 dp panel. It is composed once at its fixed position in the strip; its role only
 * changes the background color (500 ms) and the content alpha (200 ms), both read in the draw phase.
 */
@Composable
private fun SettingsPanel(
    hostState: SettingsHostState,
    instance: ScreenInstance,
    role: PanelRole,
    hostDepth: Int,
) {
    val isPreview = role == PanelRole.Preview
    // A new preview fades in; panels that are created active (root) start fully visible.
    val alpha = remember(instance.instanceId) { Animatable(if (isPreview) 0f else 1f) }
    val background = remember(instance.instanceId) {
        ColorAnimatable(if (isPreview) TvColors.Preview else TvColors.Card)
    }
    LaunchedEffect(isPreview) {
        if (isPreview) {
            alpha.animateTo(
                TvDimens.SettingsPreviewInactiveAlpha,
                tween(TvDimens.SettingsPreviewFadeMs, easing = SettingsHostTokens.SettleEasing)
            )
        } else {
            alpha.animateTo(1f, tween(TvDimens.SettingsPanelAlphaMs, easing = SettingsHostTokens.SettleEasing))
        }
    }
    LaunchedEffect(isPreview) {
        background.animateTo(
            if (isPreview) TvColors.Preview else TvColors.Card,
            tween(TvDimens.SettingsPanelColorMs, easing = SettingsHostTokens.SettleEasing)
        )
    }

    Box(
        modifier = Modifier
            .width(TvDimens.SettingsPaneWidth)
            .fillMaxHeight()
            .drawBehind { drawRect(background.value) }
            .graphicsLayer { this.alpha = alpha.value }
    ) {
        // The preview never takes focus (its rows read this); it turns focusable as the active panel.
        CompositionLocalProvider(LocalSettingsPanelFocusable provides !isPreview) {
            PanelContent(
            hostState = hostState,
            hostDepth = hostDepth,
            instance = instance,
            isActive = role == PanelRole.Active,
            isPreview = isPreview,
            modifier = Modifier.fillMaxSize()
        )
        }
    }
}

@Composable
private fun PanelContent(
    hostState: SettingsHostState,
    hostDepth: Int,
    instance: ScreenInstance,
    isActive: Boolean,
    isPreview: Boolean,
    modifier: Modifier = Modifier
) {
    val screen = hostState.screen(instance.id)

    if (screen.hasCustomContent) {
        screen.Content(
            modifier = modifier,
            contentPadding = PaddingValues(TvDimens.SettingsDp0),
            isPreview = isPreview,
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
            modifier = modifier
        )
        is SettingsScreenDataState.Ready -> ActiveSettingsPanelList(
            title = panelTitle,
            description = screen.description,
            entries = currentData.entries,
            listState = instance.listState,
            restoreFocusToken = instance.instanceId to hostState.focusRestoreRequests,
            autoRequestFocus = isActive && (instance.focusKey != null || hostDepth > 1),
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
            message = currentData.message,
            errorTitle = currentData.title,
            onRetry = { hostState.retry(instance.id) },
            requestFocus = isActive,
            modifier = modifier
        )
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
