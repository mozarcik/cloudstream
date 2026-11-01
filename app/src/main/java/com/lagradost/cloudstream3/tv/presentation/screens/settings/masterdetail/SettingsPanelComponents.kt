package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.TvListRow
import com.lagradost.cloudstream3.tv.presentation.common.TvRowLeading
import com.lagradost.cloudstream3.tv.presentation.common.TvRowTrailing
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.focus.TvInitialFocusEffect
import com.lagradost.cloudstream3.tv.presentation.focus.rememberFocusRequesterMap
import com.lagradost.cloudstream3.tv.presentation.focus.resolveAvailableFocusKey
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

fun settingsEntryTestTag(stableKey: String): String {
    return "settings_entry_$stableKey"
}

/** The focusable list of an active level: header (crumb, title, description) + rows. */
@Composable
internal fun ActiveSettingsPanelList(
    title: String,
    crumb: String,
    description: String?,
    entries: List<SettingsEntry>,
    listState: LazyListState,
    restoreFocusToken: Long,
    autoRequestFocus: Boolean = true,
    modifier: Modifier = Modifier,
    resolveFocusedKey: () -> String?,
    onEntryFocused: (SettingsEntry) -> Unit,
    onNavigateForward: (SettingsEntry) -> Unit,
    onRadioSelected: (SettingsEntry) -> Unit,
    onEntryUpdated: (SettingsEntry) -> Unit
) {
    val navigateForward by rememberUpdatedState(onNavigateForward)
    val radioSelected by rememberUpdatedState(onRadioSelected)
    val entryUpdated by rememberUpdatedState(onEntryUpdated)
    val focusedKeyProvider by rememberUpdatedState(resolveFocusedKey)
    val focusableEntries = remember(entries) {
        entries.filter { entry -> entry.isFocusable() }
    }
    val focusableKeys = remember(focusableEntries) {
        focusableEntries.map(SettingsEntry::stableKey)
    }
    val requestersByKey = rememberFocusRequesterMap(focusableKeys)
    val focusedKey = focusedKeyProvider()
    val restoreKey = remember(restoreFocusToken, autoRequestFocus, focusableKeys, focusedKey) {
        if (!autoRequestFocus) {
            null
        } else {
            // First entry on a level: the selected choice of a radio list, else the first row.
            resolveAvailableFocusKey(
                focusableKeys,
                focusedKey,
                focusableEntries.firstOrNull { it.type == SettingsEntryType.Radio && it.showCheckmark }?.stableKey
            )
        }
    }

    FocusRequestEffect(
        requester = restoreKey?.let(requestersByKey::get),
        requestKey = restoreFocusToken to restoreKey,
        enabled = autoRequestFocus && restoreKey != null,
        onFocused = {
            if (focusedKey == null) {
                focusableEntries
                    .firstOrNull { entry -> entry.stableKey == restoreKey }
                    ?.let(onEntryFocused)
            }
        }
    )

    Column(modifier = modifier.fillMaxSize()) {
        SettingsLevelHeader(title = title, crumb = crumb, description = description)
        if (entries.none { entry -> entry.type != SettingsEntryType.Message }) {
            SettingsMessageBlock(title = stringResource(R.string.no_data))
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = TvDimens.SettingsCardPaddingVertical),
        ) {
            items(items = entries, key = { item -> item.stableKey }) { entry ->
                SettingsEntryRow(
                    entry = entry,
                    interactive = entry.isFocusable(),
                    focusRequester = requestersByKey[entry.stableKey],
                    onFocused = { onEntryFocused(entry) },
                    onActivated = {
                        when {
                            entry.type == SettingsEntryType.Radio -> {
                                entry.action?.invoke()
                                radioSelected(entry)
                            }
                            entry.nextScreenId != null -> navigateForward(entry)
                            entry.action != null -> entry.action.invoke()
                        }
                    },
                    onEntryUpdated = { updatedEntry -> entryUpdated(updatedEntry) }
                )
            }
        }
    }
}

internal fun SettingsEntry.isFocusable(): Boolean {
    return type != SettingsEntryType.Header && type != SettingsEntryType.Message
}

/** The dimmed, non-interactive card that shows the next level of the focused row. */
@Composable
internal fun PreviewSettingsPanelList(
    title: String,
    entries: List<SettingsEntry>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    Column(modifier = modifier.fillMaxSize()) {
        SettingsPreviewHeader(title = title)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            userScrollEnabled = false,
            contentPadding = PaddingValues(bottom = TvDimens.SettingsCardPaddingVertical),
        ) {
            items(items = entries, key = { item -> item.stableKey }) { entry ->
                when (entry.type) {
                    SettingsEntryType.Header -> SettingsPreviewSection(text = entry.title)
                    else -> SettingsPreviewRow(
                        title = entry.title,
                        subtitle = previewSubtitle(entry),
                        // Radio / multi-select: the chosen option gets a check mark (the mockup preview is empty).
                        leading = previewSelectionMark(entry),
                    )
                }
            }
        }
    }
}

@Composable
private fun previewSelectionMark(entry: SettingsEntry): (@Composable () -> Unit)? {
    val selected = when (entry.type) {
        SettingsEntryType.Radio -> entry.showCheckmark
        SettingsEntryType.MultiSelect -> entry.toggleValue
        else -> false
    }
    if (!selected) return null
    return {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = TvColors.SettingsPreviewText,
            modifier = Modifier.size(TvDimens.SettingsDp14),
        )
    }
}

@Composable
private fun previewSubtitle(entry: SettingsEntry): String? {
    return when (entry.type) {
        SettingsEntryType.Toggle -> stringResource(if (entry.toggleValue) R.string.tv_settings_on else R.string.tv_settings_off)
        SettingsEntryType.Slider -> entry.valueText ?: entry.sliderValue.toString()
        else -> entry.subtitle
    }
}

@Composable
internal fun SettingsEntryRow(
    entry: SettingsEntry,
    interactive: Boolean,
    onFocused: () -> Unit,
    onActivated: () -> Unit,
    onEntryUpdated: (SettingsEntry) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    if (entry.type == SettingsEntryType.Header) {
        SettingsSectionLabel(text = entry.title)
        return
    }
    if (entry.type == SettingsEntryType.Message) {
        SettingsMessageBlock(title = entry.title, message = entry.subtitle)
        return
    }

    fun setChecked(newValue: Boolean) {
        if (entry.toggleValue == newValue) return
        entry.onToggleChanged?.invoke(newValue)
        onEntryUpdated(entry.copy(toggleValue = newValue))
    }

    fun stepSlider(delta: Int) {
        val range = entry.sliderRange
        val step = entry.sliderStep.coerceAtLeast(1)
        val updated = (entry.sliderValue + delta * step).coerceIn(range.first, range.last)
        if (updated == entry.sliderValue) return
        entry.onSliderChanged?.invoke(updated)
        onEntryUpdated(entry.copy(sliderValue = updated, valueText = updated.toString()))
    }

    val rowModifier = modifier
        .testTag(settingsEntryTestTag(entry.stableKey))
        .onFocusChanged { state -> if (state.hasFocus) onFocused() }
        .onPreviewKeyEvent { event ->
            val isDown = event.type == KeyEventType.KeyDown
            when (event.key) {
                // The slider is the one row where Left / Right change the value instead of moving.
                Key.DirectionLeft -> if (entry.type == SettingsEntryType.Slider) {
                    if (isDown) stepSlider(-1)
                    true
                } else {
                    false
                }
                Key.DirectionRight -> when {
                    entry.type == SettingsEntryType.Slider -> {
                        if (isDown) stepSlider(1)
                        true
                    }
                    entry.type == SettingsEntryType.Toggle -> {
                        if (isDown) setChecked(!entry.toggleValue)
                        true
                    }
                    entry.type == SettingsEntryType.Item && entry.nextScreenId != null -> {
                        if (isDown) onActivated()
                        true
                    }
                    else -> false
                }
                else -> false
            }
        }
        .run { if (focusRequester != null) focusRequester(focusRequester) else this }

    val onClick: () -> Unit = {
        when (entry.type) {
            SettingsEntryType.Toggle,
            SettingsEntryType.MultiSelect -> setChecked(!entry.toggleValue)
            SettingsEntryType.Slider -> Unit
            else -> onActivated()
        }
    }

    val leading: (@Composable (Boolean) -> Unit)? =
        if (entry.hasLeadingVisual() && entry.type != SettingsEntryType.Radio && entry.type != SettingsEntryType.MultiSelect) {
            { _ -> SettingsEntryLeading(entry) }
        } else {
            null
        }

    when (entry.type) {
        SettingsEntryType.Radio -> TvListRow(
            title = entry.title,
            onClick = onClick,
            height = TvDimens.SettingsRadioRowHeight,
            horizontalPadding = TvDimens.SettingsRowHorizontalPadding,
            gap = TvDimens.SettingsRowGap,
            leadingIndicator = TvRowLeading.Radio(selected = entry.showCheckmark),
            trailing = when {
                entry.trailingColorArgb != null -> TvRowTrailing.Custom { _ -> SettingsColorSwatch(entry.trailingColorArgb) }
                !entry.subtitle.isNullOrBlank() -> TvRowTrailing.Custom { focused ->
                    Text(
                        text = entry.subtitle,
                        style = TvTextStyles.RowSubtitle,
                        color = if (focused) TvColors.OnFocusSecondary else TvColors.LabelPanel,
                        maxLines = 1,
                    )
                }
                else -> TvRowTrailing.None
            },
            modifier = rowModifier,
        )
        SettingsEntryType.MultiSelect -> TvListRow(
            title = entry.title,
            subtitle = entry.subtitle,
            onClick = onClick,
            height = TvDimens.SettingsMultiRowHeight,
            horizontalPadding = TvDimens.SettingsRowHorizontalPadding,
            gap = TvDimens.SettingsRowGap,
            leadingIndicator = TvRowLeading.Checkbox(checked = entry.toggleValue),
            modifier = rowModifier,
        )
        SettingsEntryType.Slider -> TvListRow(
            title = entry.title,
            subtitle = entry.subtitle,
            onClick = onClick,
            height = TvDimens.SettingsSliderRowHeight,
            verticalPadding = TvDimens.SettingsSliderRowVerticalPadding,
            verticalAlignment = Alignment.Top,
            horizontalPadding = TvDimens.SettingsRowHorizontalPadding,
            gap = TvDimens.SettingsRowGap,
            leading = leading,
            bodyContent = { focused ->
                SettingsSlider(
                    entry = entry,
                    focused = focused,
                )
            },
            modifier = rowModifier,
        )
        else -> TvListRow(
            title = entry.title,
            subtitle = entry.subtitle,
            onClick = onClick,
            height = if (entry.compactRow) TvDimens.SettingsAccountRowHeight else TvDimens.RowHeightLarge,
            horizontalPadding = TvDimens.SettingsRowHorizontalPadding,
            gap = TvDimens.SettingsRowGap,
            trailingIconSize = TvDimens.SettingsRowChevronSize,
            leading = leading,
            trailing = when {
                entry.type == SettingsEntryType.Toggle -> TvRowTrailing.Toggle(on = entry.toggleValue)
                entry.trailingIcon != null && entry.nextScreenId != null -> TvRowTrailing.Custom { _ ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(TvDimens.SettingsRowGap),
                    ) {
                        Icon(
                            imageVector = entry.trailingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(TvDimens.SettingsRowIconSize),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(TvDimens.SettingsRowChevronSize),
                        )
                    }
                }
                entry.nextScreenId != null -> TvRowTrailing.Chevron
                else -> TvRowTrailing.None
            },
            modifier = rowModifier,
        )
    }
}

private fun SettingsEntry.hasLeadingVisual(): Boolean {
    return !iconUrl.isNullOrBlank() || icon != null || fallbackIconRes != null || initials != null
}

/** Leading visual of a row: remote icon (falls back to the drawable / initials), icon or initials tile. */
@Composable
private fun SettingsEntryLeading(entry: SettingsEntry) {
    val tint = LocalContentColor.current
    when {
        !entry.iconUrl.isNullOrBlank() -> {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(entry.iconUrl)
                    .crossfade(false)
                    .build()
            )
            val painterState by painter.state.collectAsState()
            if (painterState is AsyncImagePainter.State.Error) {
                SettingsEntryFallbackLeading(entry, tint)
            } else {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(TvDimens.SettingsPluginTileSize)
                        .clip(RoundedCornerShape(TvDimens.SettingsPluginTileRadius)),
                    contentScale = ContentScale.Fit
                )
            }
        }
        else -> SettingsEntryFallbackLeading(entry, tint)
    }
}

@Composable
private fun SettingsEntryFallbackLeading(entry: SettingsEntry, tint: Color) {
    val size = TvDimens.SettingsRowIconSize
    when {
        entry.initials != null -> SettingsInitialsTile(
            initials = entry.initials,
            size = if (entry.tileColorArgb != null) TvDimens.SettingsPluginTileSize else TvDimens.SettingsTileSize,
            radius = if (entry.tileColorArgb != null) TvDimens.SettingsPluginTileRadius else TvDimens.SettingsTileRadius,
            background = entry.tileColorArgb?.let { Color(it) } ?: TvColors.Tile,
            contentColor = if (entry.tileColorArgb != null) Color.White else TvColors.Meta,
        )
        entry.icon != null -> Icon(
            imageVector = entry.icon,
            contentDescription = null,
            modifier = Modifier.size(size),
            tint = tint,
        )
        entry.fallbackIconRes != null -> Icon(
            painter = painterResource(entry.fallbackIconRes),
            contentDescription = null,
            modifier = Modifier.size(size),
            tint = tint,
        )
    }
}


internal fun settingsInitialsColor(seed: String): Color {
    return TvColors.SettingsTilePalette.let { it[(seed.hashCode() and Int.MAX_VALUE) % it.size] }
}

internal fun settingsInitials(name: String): String {
    val words = name.trim().split(' ', '-', '_').filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2).replaceFirstChar { it.uppercase() }
        else -> (words[0].take(1) + words[1].take(1)).uppercase()
    }
}

@Composable
private fun SettingsColorSwatch(colorArgb: Int) {
    Box(
        modifier = Modifier
            .size(TvDimens.SettingsRowIconSize)
            .border(TvDimens.SettingsDp1, TvColors.RadioBorder, CircleShape)
            .padding(TvDimens.SettingsDp1)
            .clip(CircleShape)
            .background(Color(colorArgb))
    )
}

/** Inline slider (mockup SettingsSlider): arrows + track + value; thumb and arrows only on focus. */
@Composable
private fun SettingsSlider(entry: SettingsEntry, focused: Boolean) {
    val min = entry.sliderRange.first
    val max = entry.sliderRange.last.coerceAtLeast(min + 1)
    val progress = ((entry.sliderValue - min).toFloat() / (max - min).toFloat()).coerceIn(0f, 1f)
    val trackColor = if (focused) Color(0x260E1015) else Color(0x24FFFFFF)
    val fillColor = if (focused) TvColors.ToggleOn else TvColors.RadioBorder
    val trackHeight = if (focused) TvDimens.SettingsSliderTrackFocused else TvDimens.SettingsSliderTrack
    val thumb = TvDimens.SettingsSliderThumb

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.SettingsSliderGap),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TvDimens.SettingsSliderTopGap),
    ) {
        if (focused) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                modifier = Modifier.size(TvDimens.SettingsSliderArrow),
            )
        }
        BoxWithTrack(
            progress = progress,
            trackColor = trackColor,
            fillColor = fillColor,
            trackHeight = trackHeight,
            thumb = if (focused) thumb else null,
            modifier = Modifier.weight(1f),
        )
        if (focused) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(TvDimens.SettingsSliderArrow),
            )
        }
        Text(
            text = entry.valueText ?: entry.sliderValue.toString(),
            style = if (focused) TvTextStyles.SettingsSliderValue else TvTextStyles.Row,
            color = if (focused) TvColors.OnFocus else TvColors.Meta,
            maxLines = 1,
            modifier = Modifier.width(TvDimens.SettingsSliderValueWidth),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun BoxWithTrack(
    progress: Float,
    trackColor: Color,
    fillColor: Color,
    trackHeight: androidx.compose.ui.unit.Dp,
    thumb: androidx.compose.ui.unit.Dp?,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier.height(thumb ?: trackHeight),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(trackHeight / 2))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(trackHeight)
                    .background(fillColor)
            )
        }
        if (thumb != null) {
            Box(
                modifier = Modifier
                    .offset(x = maxWidth * progress - thumb / 2)
                    .size(thumb)
                    .background(fillColor, CircleShape)
            )
        }
    }
}

/** Loading state of an active level: header + skeleton rows. */
@Composable
internal fun SettingsLoadingPanel(
    title: String,
    crumb: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        SettingsLevelHeader(title = title, crumb = crumb)
        SettingsSkeletonRows()
    }
}

/** Error state of an active level: message box + a focused "Try again" row. */
@Composable
internal fun SettingsErrorPanel(
    title: String,
    crumb: String,
    message: String,
    errorTitle: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val retryRequester = remember { FocusRequester() }
    TvInitialFocusEffect(requester = retryRequester)
    Column(modifier = modifier.fillMaxSize()) {
        SettingsLevelHeader(title = title, crumb = crumb)
        SettingsStateBox(
            title = errorTitle ?: stringResource(R.string.error),
            message = message,
        )
        Spacer(modifier = Modifier.height(TvDimens.SettingsDp4))
        SettingsActionRow(
            title = stringResource(R.string.tv_player_retry),
            icon = Icons.Default.Refresh,
            onClick = onRetry,
            modifier = Modifier.focusRequester(retryRequester),
        )
    }
}

@Composable
internal fun EmptyPreviewScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize())
}
