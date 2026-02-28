package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade

private object SettingsPanelTokens {
    val ActivePanelContentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp)
    val PreviewPanelContentPadding = ActivePanelContentPadding
    val HeaderBottomPadding = 20.dp
    val RowVerticalPadding = 12.dp
    val RowHorizontalPadding = 16.dp
    val RowShape = RoundedCornerShape(12.dp)
    val ItemSpacing = 8.dp
    val IconSize = 20.dp
    val IconSpacing = 12.dp
    val HeaderHorizontalPadding = 4.dp
    val HeaderVerticalPadding = 6.dp
    val ToggleContainerWidth = 54.dp
    val ToggleContainerHeight = 28.dp
    val ToggleThumbSize = 20.dp
    val TrailingContentSpacing = 12.dp
    val ColorPreviewSize = 16.dp
    val ColorPreviewBorderWidth = 1.dp
    val SliderTopPadding = 8.dp
    val SliderBarHeight = 6.dp
    val SliderValueTopPadding = 6.dp
    val PlaceholderHeight = 56.dp
    val PlaceholderSpacing = 10.dp
    const val FocusScale = 1.03f
    const val FocusScaleAnimationMs = 120
}

@Composable
internal fun ActiveSettingsPanelList(
    title: String,
    entries: List<SettingsEntry>,
    listState: LazyListState,
    restoreFocusToken: Long,
    autoRequestFocus: Boolean = true,
    contentPadding: PaddingValues = SettingsPanelTokens.ActivePanelContentPadding,
    modifier: Modifier = Modifier,
    resolveFocusedKey: () -> String?,
    onEntryFocused: (SettingsEntry) -> Unit,
    onNavigateForward: (SettingsEntry) -> Unit,
    onEntryUpdated: (SettingsEntry) -> Unit
) {
    val navigateForward by rememberUpdatedState(onNavigateForward)
    val entryUpdated by rememberUpdatedState(onEntryUpdated)
    val focusedKeyProvider by rememberUpdatedState(resolveFocusedKey)
    val focusableEntries = remember(entries) {
        entries.filter { entry -> entry.type != SettingsEntryType.Header }
    }
    val requestersByKey = remember(focusableEntries) {
        focusableEntries.associate { entry ->
            entry.stableKey to FocusRequester()
        }
    }

    LaunchedEffect(restoreFocusToken, focusableEntries, autoRequestFocus) {
        if (!autoRequestFocus) return@LaunchedEffect
        if (focusableEntries.isEmpty()) return@LaunchedEffect
        val focusedKey = focusedKeyProvider()
        val restoreKey = focusedKey ?: focusableEntries.first().stableKey
        val restoreIndex = focusableEntries.indexOfFirst { it.stableKey == restoreKey }
            .takeIf { it >= 0 }
            ?: 0

        if (focusedKey == null) {
            onEntryFocused(focusableEntries[restoreIndex])
        }

        requestersByKey[focusableEntries[restoreIndex].stableKey]?.requestFocus()
    }

    SettingsPanelList(
        title = title,
        entries = entries,
        listState = listState,
        contentPadding = contentPadding,
        modifier = modifier
    ) { entry ->
        SettingsEntryRow(
            entry = entry,
            interactive = entry.type != SettingsEntryType.Header,
            focusRequester = requestersByKey[entry.stableKey],
            onFocused = { onEntryFocused(entry) },
            onActivated = {
                when {
                    entry.nextScreenId != null -> {
                        navigateForward(entry)
                    }
                    entry.action != null -> {
                        entry.action.invoke()
                    }
                }
            },
            onEntryUpdated = { updatedEntry ->
                entryUpdated(updatedEntry)
            }
        )
    }
}

@Composable
internal fun PreviewSettingsPanelList(
    title: String,
    entries: List<SettingsEntry>,
    contentPadding: PaddingValues = SettingsPanelTokens.PreviewPanelContentPadding,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    SettingsPanelList(
        title = title,
        entries = entries,
        listState = listState,
        contentPadding = contentPadding,
        modifier = modifier
    ) { entry ->
        SettingsEntryRow(
            entry = entry,
            interactive = false,
            onFocused = {},
            onActivated = {},
            onEntryUpdated = {}
        )
    }
}

@Composable
private fun SettingsPanelList(
    title: String,
    entries: List<SettingsEntry>,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    itemContent: @Composable (SettingsEntry) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        PanelHeader(title = title)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(SettingsPanelTokens.ItemSpacing)
        ) {
            items(items = entries, key = { item -> item.stableKey }) { entry ->
                itemContent(entry)
            }
        }
    }
}

@Composable
private fun PanelHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(bottom = SettingsPanelTokens.HeaderBottomPadding)
    )
}

@Composable
private fun SettingsEntryRow(
    entry: SettingsEntry,
    interactive: Boolean,
    onFocused: () -> Unit,
    onActivated: () -> Unit,
    onEntryUpdated: (SettingsEntry) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    var toggleValue by remember(entry.stableKey) { mutableStateOf(entry.toggleValue) }
    var sliderValue by remember(entry.stableKey) { mutableIntStateOf(entry.sliderValue) }

    LaunchedEffect(entry.toggleValue) {
        toggleValue = entry.toggleValue
    }
    LaunchedEffect(entry.sliderValue) {
        sliderValue = entry.sliderValue
    }

    val scale by animateFloatAsState(
        targetValue = if (interactive && isFocused) SettingsPanelTokens.FocusScale else 1f,
        animationSpec = tween(SettingsPanelTokens.FocusScaleAnimationMs),
        label = "SettingsEntryScale"
    )

    fun updateToggle(newValue: Boolean) {
        if (entry.type != SettingsEntryType.Toggle || toggleValue == newValue) return
        toggleValue = newValue
        entry.onToggleChanged?.invoke(newValue)
        onEntryUpdated(entry.copy(toggleValue = newValue))
    }

    fun updateSlider(delta: Int) {
        if (entry.type != SettingsEntryType.Slider) return
        val range = entry.sliderRange
        val step = entry.sliderStep.coerceAtLeast(1)
        val updatedValue = (sliderValue + delta * step).coerceIn(range.first, range.last)
        if (updatedValue == sliderValue) return

        sliderValue = updatedValue
        entry.onSliderChanged?.invoke(updatedValue)
        onEntryUpdated(
            entry.copy(
                sliderValue = updatedValue,
                valueText = updatedValue.toString()
            )
        )
    }

    val rowModifier = modifier
        .fillMaxWidth()
        .run {
            if (interactive) {
                onFocusChanged { state ->
                    if (state.isFocused) {
                        isFocused = true
                        onFocused()
                    } else if (!state.hasFocus) {
                        isFocused = false
                    }
                }
            } else {
                this
            }
        }
        .run {
            if (interactive) {
                onPreviewKeyEvent { event ->
                    when (event.key) {
                        Key.DirectionLeft -> {
                            when (entry.type) {
                                SettingsEntryType.Toggle -> {
                                    if (event.type == KeyEventType.KeyDown) {
                                        updateToggle(false)
                                    }
                                    true
                                }
                                SettingsEntryType.Slider -> {
                                    if (event.type == KeyEventType.KeyDown) {
                                        updateSlider(delta = -1)
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                        Key.DirectionRight -> {
                            when (entry.type) {
                                SettingsEntryType.Toggle -> {
                                    if (event.type == KeyEventType.KeyDown) {
                                        updateToggle(true)
                                    }
                                    true
                                }
                                SettingsEntryType.Slider -> {
                                    if (event.type == KeyEventType.KeyDown) {
                                        updateSlider(delta = 1)
                                    }
                                    true
                                }
                                else -> {
                                    if (event.type == KeyEventType.KeyDown) {
                                        if (entry.nextScreenId != null || entry.action != null) {
                                            onActivated()
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                }
                            }
                        }
                        else -> false
                    }
                }
            } else {
                this
            }
        }
        .run {
            if (interactive && focusRequester != null) {
                focusRequester(focusRequester)
            } else {
                this
            }
        }
        .graphicsLayer {
            if (interactive) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0.5f)
            }
        }

    if (interactive) {
        Surface(
            onClick = {
                when (entry.type) {
                    SettingsEntryType.Toggle -> updateToggle(!toggleValue)
                    SettingsEntryType.Item -> {
                        if (entry.nextScreenId != null || entry.action != null) {
                            onActivated()
                        }
                    }
                    else -> Unit
                }
            },
            modifier = rowModifier,
            shape = ClickableSurfaceDefaults.shape(shape = SettingsPanelTokens.RowShape),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                pressedContainerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            SettingsEntryRowContent(
                entry = entry,
                isFocused = isFocused,
                interactive = true,
                toggleValue = toggleValue,
                sliderValue = sliderValue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = SettingsPanelTokens.RowHorizontalPadding,
                        vertical = SettingsPanelTokens.RowVerticalPadding
                    )
            )
        }
    } else {
        Box(
            modifier = rowModifier
                .clip(SettingsPanelTokens.RowShape)
                .background(Color.Transparent)
                .padding(
                    horizontal = SettingsPanelTokens.RowHorizontalPadding,
                    vertical = SettingsPanelTokens.RowVerticalPadding
                )
        ) {
            SettingsEntryRowContent(
                entry = entry,
                isFocused = false,
                interactive = false,
                toggleValue = toggleValue,
                sliderValue = sliderValue,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SettingsEntryRowContent(
    entry: SettingsEntry,
    isFocused: Boolean,
    interactive: Boolean,
    toggleValue: Boolean,
    sliderValue: Int,
    modifier: Modifier = Modifier
) {
    if (entry.type == SettingsEntryType.Header) {
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(
                horizontal = SettingsPanelTokens.HeaderHorizontalPadding,
                vertical = SettingsPanelTokens.HeaderVerticalPadding
            )
        )
        return
    }

    val titleColor = if (interactive && isFocused) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = if (interactive && isFocused) {
        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        entry.trailingColorArgb?.let { colorArgb ->
            SettingsEntryColorPreview(
                colorArgb = colorArgb,
                borderColor = if (interactive && isFocused) {
                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.28f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                }
            )
            Spacer(modifier = Modifier.width(SettingsPanelTokens.IconSpacing))
        }

        when {
            !entry.iconUrl.isNullOrBlank() -> {
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(entry.iconUrl)
                        .crossfade(true)
                        .build()
                )

                if (painter.state is AsyncImagePainter.State.Error) {
                    when {
                        entry.icon != null -> {
                            Icon(
                                imageVector = entry.icon,
                                contentDescription = null,
                                modifier = Modifier.size(SettingsPanelTokens.IconSize),
                                tint = subtitleColor
                            )
                        }
                        entry.fallbackIconRes != null -> {
                            Icon(
                                painter = painterResource(entry.fallbackIconRes),
                                contentDescription = null,
                                modifier = Modifier.size(SettingsPanelTokens.IconSize),
                                tint = subtitleColor
                            )
                        }
                    }
                } else {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.size(SettingsPanelTokens.IconSize),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.width(SettingsPanelTokens.IconSpacing))
            }
            entry.icon != null -> {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = null,
                    modifier = Modifier.size(SettingsPanelTokens.IconSize),
                    tint = subtitleColor
                )
                Spacer(modifier = Modifier.width(SettingsPanelTokens.IconSpacing))
            }
            entry.fallbackIconRes != null -> {
                Icon(
                    painter = painterResource(entry.fallbackIconRes),
                    contentDescription = null,
                    modifier = Modifier.size(SettingsPanelTokens.IconSize),
                    tint = subtitleColor
                )
                Spacer(modifier = Modifier.width(SettingsPanelTokens.IconSpacing))
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = titleColor
            )
            entry.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = subtitleColor
                )
            }

            if (entry.type == SettingsEntryType.Slider) {
                val min = entry.sliderRange.first
                val max = entry.sliderRange.last.coerceAtLeast(min + 1)
                val progress = ((sliderValue - min).toFloat() / (max - min).toFloat())
                    .coerceIn(0f, 1f)

                Column(
                    modifier = Modifier.padding(top = SettingsPanelTokens.SliderTopPadding)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(SettingsPanelTokens.SliderBarHeight)
                            .clip(RoundedCornerShape(SettingsPanelTokens.SliderBarHeight))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(
                                    if (interactive && isFocused) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                        )
                    }
                    Text(
                        text = entry.valueText ?: sliderValue.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = subtitleColor,
                        modifier = Modifier.padding(top = SettingsPanelTokens.SliderValueTopPadding)
                    )
                }
            }
        }

        if (entry.type == SettingsEntryType.Toggle || entry.showCheckmark) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SettingsPanelTokens.TrailingContentSpacing)
            ) {
                if (entry.type == SettingsEntryType.Toggle) {
                    ToggleIndicator(
                        enabled = toggleValue,
                        isFocused = interactive && isFocused
                    )
                }

                if (entry.showCheckmark) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SettingsPanelTokens.IconSize),
                        tint = if (interactive && isFocused) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsEntryColorPreview(
    colorArgb: Int,
    borderColor: Color,
) {
    Box(
        modifier = Modifier
            .size(SettingsPanelTokens.ColorPreviewSize)
            .border(
                width = SettingsPanelTokens.ColorPreviewBorderWidth,
                color = borderColor,
                shape = CircleShape
            )
            .padding(SettingsPanelTokens.ColorPreviewBorderWidth)
            .clip(CircleShape)
            .background(Color(colorArgb))
    )
}

@Composable
private fun ToggleIndicator(
    enabled: Boolean,
    isFocused: Boolean
) {
    val trackColor = when {
        enabled && isFocused -> MaterialTheme.colorScheme.onSecondaryContainer
        enabled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val thumbColor = when {
        enabled && isFocused -> MaterialTheme.colorScheme.secondaryContainer
        enabled -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .width(SettingsPanelTokens.ToggleContainerWidth)
            .height(SettingsPanelTokens.ToggleContainerHeight)
            .clip(RoundedCornerShape(SettingsPanelTokens.ToggleContainerHeight))
            .background(trackColor)
            .padding(horizontal = 4.dp),
        contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(SettingsPanelTokens.ToggleThumbSize)
                .clip(RoundedCornerShape(SettingsPanelTokens.ToggleThumbSize))
                .background(thumbColor)
        )
    }
}

@Composable
internal fun SettingsLoadingPanel(
    title: String,
    contentPadding: PaddingValues = SettingsPanelTokens.ActivePanelContentPadding,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        PanelHeader(title = title)
        repeat(6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SettingsPanelTokens.PlaceholderHeight)
                    .clip(SettingsPanelTokens.RowShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            )
            Spacer(modifier = Modifier.height(SettingsPanelTokens.PlaceholderSpacing))
        }
    }
}

@Composable
internal fun SettingsErrorPanel(
    title: String,
    message: String,
    contentPadding: PaddingValues = SettingsPanelTokens.ActivePanelContentPadding,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        PanelHeader(title = title)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
internal fun EmptyPreviewScreen(
    backgroundColor: Color,
    contentPadding: PaddingValues = SettingsPanelTokens.PreviewPanelContentPadding,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(contentPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No preview available",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Select an item with nested settings",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}
