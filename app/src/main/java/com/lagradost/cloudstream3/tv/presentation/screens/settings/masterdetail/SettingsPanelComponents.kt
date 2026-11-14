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
    description: String?,
    entries: List<SettingsEntry>,
    listState: LazyListState,
    restoreFocusToken: Any,
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
        SettingsLevelHeader(title = title, description = description)
        if (entries.none { entry -> entry.type != SettingsEntryType.Message }) {
            SettingsMessageBlock(title = stringResource(R.string.no_data))
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                start = TvDimens.SettingsPaneStartInset,
                top = TvDimens.SettingsListTopPadding,
                bottom = TvDimens.SettingsListTopPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(TvDimens.SettingsItemGap),
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
    // Initials circles carry their own color (plugin palette); other circles use the neutral fill.
    val circleColor = entry.tileColorArgb?.let { Color(it) }

    val widget: SettingsRowWidget = when {
        entry.type == SettingsEntryType.Toggle -> SettingsRowWidget.Switch(on = entry.toggleValue)
        entry.type == SettingsEntryType.Radio -> when {
            entry.trailingColorArgb != null -> SettingsRowWidget.Custom { _ ->
                SettingsColorSwatch(entry.trailingColorArgb)
            }
            else -> SettingsRowWidget.Radio(selected = entry.showCheckmark)
        }
        entry.type == SettingsEntryType.MultiSelect -> SettingsRowWidget.Checkbox(checked = entry.toggleValue)
        entry.type == SettingsEntryType.Slider -> SettingsRowWidget.None
        entry.trailingIcon != null && entry.nextScreenId != null -> SettingsRowWidget.Custom { _ ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = entry.trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(TvDimens.SettingsItemGlyphSize),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(TvDimens.SettingsControlBox),
                )
            }
        }
        entry.nextScreenId != null -> SettingsRowWidget.Chevron
        else -> SettingsRowWidget.None
    }

    SettingsListRow(
        title = entry.title,
        summary = entry.subtitle,
        onClick = onClick,
        leading = leading,
        leadingCircleColor = circleColor,
        widget = widget,
        body = if (entry.type == SettingsEntryType.Slider) {
            { focused -> SettingsSlider(entry = entry, focused = focused) }
        } else {
            null
        },
        modifier = rowModifier,
    )
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
                        .size(TvDimens.SettingsItemIconSize)
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
            }
        }
        else -> SettingsEntryFallbackLeading(entry, tint)
    }
}

@Composable
private fun SettingsEntryFallbackLeading(entry: SettingsEntry, tint: Color) {
    val size = TvDimens.SettingsItemGlyphSize
    when {
        entry.initials != null -> Text(
            text = entry.initials,
            style = TvTextStyles.SettingsTile,
            color = if (entry.tileColorArgb != null) Color.White else TvColors.Text,
            maxLines = 1,
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
            .size(TvDimens.SettingsItemGlyphSize)
            .border(TvDimens.SettingsDp1, TvColors.RadioBorder, CircleShape)
            .padding(TvDimens.SettingsDp1)
            .clip(CircleShape)
            .background(Color(colorArgb))
    )
}

/** Track (2 dp) + thumb (12 dp) under the row text and the value in a 36 dp box; Left / Right change it. */
@Composable
private fun SettingsSlider(entry: SettingsEntry, focused: Boolean) {
    val min = entry.sliderRange.first
    val max = entry.sliderRange.last.coerceAtLeast(min + 1)
    val progress = ((entry.sliderValue - min).toFloat() / (max - min).toFloat()).coerceIn(0f, 1f)
    val trackColor = if (focused) TvColors.OnFocusSecondary else TvColors.ProgressTrack
    val fillColor = if (focused) TvColors.OnFocus else TvColors.Text

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TvDimens.SettingsDp8, end = TvDimens.SettingsDp22),
    ) {
        BoxWithTrack(
            progress = progress,
            trackColor = trackColor,
            fillColor = fillColor,
            trackHeight = TvDimens.SettingsDp2,
            thumb = TvDimens.SettingsSliderThumb,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = entry.valueText ?: entry.sliderValue.toString(),
            style = TvTextStyles.SettingsItemSummary,
            color = if (focused) TvColors.OnFocus else TvColors.Text,
            maxLines = 1,
            modifier = Modifier.width(TvDimens.SettingsDp36),
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

/** Loading state of a level: header + skeleton rows. */
@Composable
internal fun SettingsLoadingPanel(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        SettingsLevelHeader(title = title)
        SettingsSkeletonRows()
    }
}

/** Error state of a level: error paragraph + a focused "Try again" row + illustration. */
@Composable
internal fun SettingsErrorPanel(
    title: String,
    message: String,
    errorTitle: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    requestFocus: Boolean = true,
) {
    val retryRequester = remember { FocusRequester() }
    if (requestFocus) TvInitialFocusEffect(requester = retryRequester)
    Column(modifier = modifier.fillMaxSize()) {
        SettingsLevelHeader(title = title)
        Column(modifier = Modifier.padding(start = TvDimens.SettingsPaneStartInset)) {
            SettingsInfoParagraph(text = errorTitle ?: stringResource(R.string.error), color = TvColors.ErrorText)
            SettingsInfoParagraph(text = message, color = TvColors.ErrorText)
            SettingsActionRow(
                title = stringResource(R.string.tv_player_retry),
                icon = Icons.Default.Refresh,
                onClick = onRetry,
                modifier = Modifier.focusRequester(retryRequester),
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.tv_empty_error),
                contentDescription = null,
                modifier = Modifier.size(TvDimens.StateIllustrationSize),
            )
        }
    }
}
