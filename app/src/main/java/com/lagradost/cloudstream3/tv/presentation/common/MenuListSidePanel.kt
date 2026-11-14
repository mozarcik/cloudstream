package com.lagradost.cloudstream3.tv.presentation.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.heightIn
import com.lagradost.cloudstream3.tv.presentation.focus.tvFocusFill
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.foundation.layout.size
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.focus.rememberFocusRequesterMap
import com.lagradost.cloudstream3.tv.presentation.focus.resolveAvailableFocusKey
import com.lagradost.cloudstream3.tv.presentation.screens.movies.DotSeparatedRow

enum class SidePanelTitleStyle {
    Default,
    SubtitleGroupHeader,
    SubtitleItem,
}

enum class SidePanelSupportingStyle {
    Default,
    SourceOption,
    SourceError,
}

enum class SidePanelContentNavigationDirection {
    Forward,
    Backward,
}

enum class SidePanelSelectionIndicatorStyle {
    Checkmark,
    RadioButton,
    /** Radio at the trailing edge (Player audio / video panels). */
    TrailingRadio,
}

data class SidePanelInlineTextField(
    val value: String,
    val placeholder: String = "",
    val valueChangeToken: Any? = null,
    val submitToken: Any? = null,
)

data class SidePanelMenuItem(
    val id: String,
    val title: String,
    val titleMaxLines: Int = 1,
    val titleStyle: SidePanelTitleStyle = SidePanelTitleStyle.Default,
    val titleTextStyle: TextStyle? = null,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val isSectionHeader: Boolean = false,
    val isVisible: Boolean = true,
    val animateVisibility: Boolean = false,
    val itemStartPadding: Dp = 0.dp,
    val itemBackgroundColor: Color? = null,
    val itemBackgroundShape: Shape? = null,
    val focusedItemShape: Shape? = null,
    val testTag: String? = null,
    val supportingTexts: List<String> = emptyList(),
    val supportingStyle: SidePanelSupportingStyle = SidePanelSupportingStyle.Default,
    val showChevron: Boolean = false,
    val chevronExpanded: Boolean = false,
    val expandableGroupKey: String? = null,
    val parentGroupKey: String? = null,
    val showTrailingRadio: Boolean = false,
    /** Radio before the label (language / encoding lists, "No subtitles"). */
    val showLeadingRadio: Boolean = false,
    /** Small muted text before the trailing icon (language group size "• 2"). */
    val trailingText: String? = null,
    /** Label color while not focused (selected language group). */
    val titleTint: Color? = null,
    /** Extra start padding inside the row fill. */
    val contentInsetStart: Dp = 0.dp,
    val actionToken: Any? = null,
    val onRightActionToken: Any? = null,
    val onLeftActionToken: Any? = null,
    val onClick: () -> Unit = {},
    val onMenuClick: (() -> Unit)? = null,
    /** Press and hold OK (the following release does not click). */
    val onLongClick: (() -> Unit)? = null,
    val onKeyUp: ((Int) -> Boolean)? = null,
    val inlineTextField: SidePanelInlineTextField? = null,
    val supportingContent: (@Composable () -> Unit)? = null,
    val leadingContent: (@Composable () -> Unit)? = null,
    /** Initials tile (44 px, r12) before the title, e.g. source list; turns light on focus. */
    val leadingTileText: String? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
)

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun MenuListSidePanel(
    visible: Boolean,
    onCloseRequested: () -> Unit,
    title: String,
    items: List<SidePanelMenuItem>,
    titleTrailing: String? = null,
    /** Accent line above the title (e.g. "Reacher • S3:E2"). */
    eyebrow: String? = null,
    /** Secondary line under the title (e.g. the item name). */
    subtitle: String? = null,
    /** Crumb with a back chevron above the title (second-level panels, e.g. "‹ Subtitles"). */
    backLabel: String? = null,
    titleStyle: TextStyle = TvTextStyles.PanelTitle,
    modifier: Modifier = Modifier,
    panelWidth: Dp = TvDimens.PanelWidth,
    panelTestTag: String? = null,
    headerContent: (@Composable ColumnScope.() -> Unit)? = null,
    emptyContent: (@Composable () -> Unit)? = null,
    closeOnLeftPress: Boolean = true,
    closeOnFocusExit: Boolean = true,
    initialFocusedItemId: String? = null,
    showSelectionRadio: Boolean = false,
    selectionIndicatorStyle: SidePanelSelectionIndicatorStyle = SidePanelSelectionIndicatorStyle.Checkmark,
    contentAnimationKey: Any? = null,
    contentNavigationDirection: SidePanelContentNavigationDirection = SidePanelContentNavigationDirection.Forward,
    enableContentAnimation: Boolean = true,
    enableItemAnimations: Boolean = true,
    onActionTokenClick: ((Any) -> Unit)? = null,
    onDirectionalActionToken: ((Any) -> Unit)? = null,
    onInlineTextFieldValueChanged: ((Any, String) -> Unit)? = null,
    onInlineTextFieldSubmit: ((Any, String) -> Unit)? = null,
    onExitUpRequested: (() -> Unit)? = null,
    rowHeight: Dp = TvDimens.PanelRowHeight,
    /** Rows keep exactly [rowHeight] (mockup 96 px sources); long text is cut with an ellipsis. */
    fixedRowHeight: Boolean = false,
    topPadding: Dp = TvDimens.PanelTopPadding,
    listBottomPadding: Dp = 0.dp,
    /** Group headers as small uppercase labels (details panels), else the generic meta style. */
    uppercaseSectionHeaders: Boolean = false,
) {
    val resolvedContentKey = contentAnimationKey ?: Unit
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(items) {
        val currentKeys = buildSet {
            items.forEach { menuItem ->
                menuItem.expandableGroupKey?.let { key ->
                    add(key)
                    if (expandedGroups.containsKey(key).not()) {
                        expandedGroups[key] = menuItem.chevronExpanded
                    }
                }
            }
        }

        expandedGroups.keys
            .toList()
            .filterNot(currentKeys::contains)
            .forEach(expandedGroups::remove)
    }

    fun isItemVisible(menuItem: SidePanelMenuItem): Boolean {
        if (!menuItem.isVisible) return false
        val parentGroupKey = menuItem.parentGroupKey ?: return true
        return expandedGroups[parentGroupKey] ?: true
    }

    val focusableItemIds = items.filter { menuItem ->
        !menuItem.isSectionHeader && menuItem.enabled && isItemVisible(menuItem)
    }
        .map { menuItem -> menuItem.id }
    val firstFocusableItemId = focusableItemIds.firstOrNull()
    val focusRequesters = rememberFocusRequesterMap(focusableItemIds)
    val listState = remember(resolvedContentKey) {
        LazyListState()
    }
    var focusedItemId by remember(resolvedContentKey) { mutableStateOf<String?>(null) }
    val requestFocusItemId = remember(
        visible,
        resolvedContentKey,
        initialFocusedItemId,
        focusableItemIds,
        focusedItemId
    ) {
        if (!visible || focusableItemIds.isEmpty()) {
            null
        } else if (focusedItemId != null && focusedItemId in focusableItemIds) {
            null
        } else {
            resolveAvailableFocusKey(focusableItemIds, initialFocusedItemId, focusedItemId)
        }
    }

    LaunchedEffect(visible, resolvedContentKey) {
        focusedItemId = null
    }

    LaunchedEffect(requestFocusItemId, items) {
        val targetItemId = requestFocusItemId ?: return@LaunchedEffect
        val focusItemListIndex = items.indexOfFirst { menuItem ->
            menuItem.id == targetItemId && isItemVisible(menuItem)
        }

        if (focusItemListIndex >= 0) {
            listState.scrollToItem(index = focusItemListIndex)
        }
    }

    FocusRequestEffect(
        requester = requestFocusItemId?.let(focusRequesters::get),
        requestKey = resolvedContentKey to requestFocusItemId,
        enabled = visible && requestFocusItemId != null,
        attempts = 40,
        retryDelayMs = 50,
        onFocused = { focusedItemId = requestFocusItemId }
    )

    SlidingSidePanel(
        visible = visible,
        onCloseRequested = onCloseRequested,
        panelWidth = panelWidth,
        panelTestTag = panelTestTag,
        panelPadding = PaddingValues(
            start = TvDimens.PanelHorizontalPadding,
            end = TvDimens.PanelHorizontalPadding,
            top = topPadding,
        ),
        modifier = modifier
    ) {
        val panelContent: @Composable () -> Unit = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                PanelHeader(
                    title = title,
                    titleTrailing = titleTrailing,
                    eyebrow = eyebrow,
                    subtitle = subtitle,
                    backLabel = backLabel,
                    titleStyle = titleStyle,
                )

                headerContent?.invoke(this)

                LazyColumn(
                    contentPadding = PaddingValues(bottom = listBottomPadding),
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                        .onPreviewKeyEvent { keyEvent ->
                            val nativeEvent = keyEvent.nativeKeyEvent
                            if (nativeEvent.keyCode != AndroidKeyEvent.KEYCODE_BACK &&
                                nativeEvent.keyCode != AndroidKeyEvent.KEYCODE_ESCAPE
                            ) {
                                return@onPreviewKeyEvent false
                            }

                            if (nativeEvent.action == AndroidKeyEvent.ACTION_UP) {
                                onCloseRequested()
                            }
                            true
                        }
                        .focusProperties {
                            onExit = {
                                if (closeOnFocusExit) {
                                    onCloseRequested()
                                    FocusRequester.Default
                                } else {
                                    FocusRequester.Cancel
                                }
                            }
                        },
                    state = listState,
                ) {
                    if (items.isEmpty()) {
                        item {
                            emptyContent?.invoke()
                        }
                    } else {
                        items(
                            items = items,
                            key = { menuItem -> menuItem.id }
                        ) { menuItem ->
                            val runtimeVisible = isItemVisible(menuItem)
                            if (!runtimeVisible && !menuItem.animateVisibility) {
                                return@items
                            }

                            if (menuItem.isSectionHeader) {
                                Text(
                                    text = if (uppercaseSectionHeaders) menuItem.title.uppercase() else menuItem.title,
                                    style = if (uppercaseSectionHeaders) TvTextStyles.PanelGroupHeader else TvTextStyles.Meta,
                                    color = TvColors.LabelPanel,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (uppercaseSectionHeaders) {
                                                Modifier.padding(
                                                    start = TvDimens.PanelTitleHorizontalPadding,
                                                    end = TvDimens.PanelTitleHorizontalPadding,
                                                    top = TvDimens.PanelGroupHeaderTop,
                                                    bottom = TvDimens.PanelGroupHeaderBottom,
                                                )
                                            } else {
                                                Modifier.padding(
                                                    horizontal = TvDimens.PanelTitleHorizontalPadding,
                                                    vertical = TvDimens.PanelSectionVerticalPadding,
                                                )
                                            },
                                        ),
                                )
                                return@items
                            }

                            var isFocused by remember(menuItem.id) { mutableStateOf(false) }
                            val longPressFired = remember(menuItem.id) { BooleanArray(1) }
                            val groupKey = menuItem.expandableGroupKey
                            val isGroupExpanded = groupKey?.let { key ->
                                expandedGroups[key] ?: menuItem.chevronExpanded
                            } ?: menuItem.chevronExpanded

                            fun toggleGroup(expanded: Boolean? = null): Boolean {
                                val key = groupKey ?: return false
                                val current = expandedGroups[key] ?: menuItem.chevronExpanded
                                val next = expanded ?: !current
                                if (next == current) return false
                                expandedGroups[key] = next
                                return true
                            }

                            val onRowFocusChanged: (FocusState) -> Unit = { focusState ->
                                isFocused = focusState.isFocused
                                if (focusState.isFocused) {
                                    focusedItemId = menuItem.id
                                } else if (focusedItemId == menuItem.id && !focusState.hasFocus) {
                                    focusedItemId = null
                                }
                            }
                            val canExitUp = menuItem.id == firstFocusableItemId && onExitUpRequested != null

                            val rowContent: @Composable () -> Unit = {
                                val inlineTextField = menuItem.inlineTextField
                                if (inlineTextField != null) {
                                    PanelTextFieldRow(
                                        menuItem = menuItem,
                                        inlineTextField = inlineTextField,
                                        isFocused = isFocused,
                                        onFocusChanged = onRowFocusChanged,
                                        focusRequester = focusRequesters[menuItem.id],
                                        canExitUp = canExitUp,
                                        rowHeight = rowHeight,
                                        onExitUpRequested = onExitUpRequested,
                                        onDirectionalActionToken = onDirectionalActionToken,
                                        onInlineTextFieldValueChanged = onInlineTextFieldValueChanged,
                                        onInlineTextFieldSubmit = onInlineTextFieldSubmit,
                                        closeOnLeftPress = closeOnLeftPress,
                                        onCloseRequested = onCloseRequested,
                                    )
                                } else {
                                    PanelRow(
                                        menuItem = menuItem,
                                        isFocused = isFocused,
                                        isGroupExpanded = isGroupExpanded,
                                        longPressFired = longPressFired,
                                        toggleGroup = { expanded -> toggleGroup(expanded) },
                                        onFocusChanged = onRowFocusChanged,
                                        focusRequester = focusRequesters[menuItem.id],
                                        canExitUp = canExitUp,
                                        rowHeight = rowHeight,
                                        fixedRowHeight = fixedRowHeight,
                                        showSelectionRadio = showSelectionRadio,
                                        selectionIndicatorStyle = selectionIndicatorStyle,
                                        onActionTokenClick = onActionTokenClick,
                                        onExitUpRequested = onExitUpRequested,
                                        onDirectionalActionToken = onDirectionalActionToken,
                                        closeOnLeftPress = closeOnLeftPress,
                                        onCloseRequested = onCloseRequested,
                                    )
                                }
                            }

                            if (menuItem.animateVisibility && enableItemAnimations) {
                                AnimatedVisibility(
                                    visible = runtimeVisible,
                                    enter = expandVertically(animationSpec = tween(durationMillis = TvDimens.MaxAnimationMs)),
                                    exit = shrinkVertically(animationSpec = tween(durationMillis = TvDimens.MaxAnimationMs)),
                                ) {
                                    Box(modifier = Modifier.animateContentSize()) {
                                        rowContent()
                                    }
                                }
                            } else if (runtimeVisible) {
                                rowContent()
                            }
                        }
                    }
                }
            }
        }

        if (contentAnimationKey == null || !enableContentAnimation) {
            panelContent()
        } else {
            AnimatedContent(
                targetState = resolvedContentKey,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val isForward = contentNavigationDirection == SidePanelContentNavigationDirection.Forward
                    if (isForward) {
                        (slideInHorizontally(
                            animationSpec = tween(durationMillis = TvDimens.MaxAnimationMs),
                            initialOffsetX = { fullWidth -> fullWidth / 3 },
                        ) + fadeIn(animationSpec = tween(durationMillis = TvDimens.MaxAnimationMs)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(durationMillis = TvDimens.MaxAnimationMs),
                                    targetOffsetX = { fullWidth -> -fullWidth / 4 },
                                ) + fadeOut(animationSpec = tween(durationMillis = TvDimens.MaxAnimationMs))
                            )
                    } else {
                        (slideInHorizontally(
                            animationSpec = tween(durationMillis = TvDimens.MaxAnimationMs),
                            initialOffsetX = { fullWidth -> -fullWidth / 3 },
                        ) + fadeIn(animationSpec = tween(durationMillis = TvDimens.MaxAnimationMs)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(durationMillis = TvDimens.MaxAnimationMs),
                                    targetOffsetX = { fullWidth -> fullWidth / 4 },
                                ) + fadeOut(animationSpec = tween(durationMillis = TvDimens.MaxAnimationMs))
                            )
                    }
                },
                label = "menu_side_panel_content",
            ) {
                panelContent()
            }
        }
    }
}

@Composable
private fun PanelHeader(
    title: String,
    titleTrailing: String?,
    eyebrow: String?,
    subtitle: String?,
    backLabel: String?,
    titleStyle: TextStyle,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = TvDimens.PanelTitleHorizontalPadding,
                end = TvDimens.PanelTitleHorizontalPadding,
                bottom = TvDimens.PanelTitleBottomPadding,
            ),
    ) {
        if (backLabel != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerIcons.ChevronLeft,
                    contentDescription = null,
                    tint = TvColors.LabelPanel,
                    modifier = Modifier.size(11.dp),
                )
                Text(
                    text = backLabel,
                    style = TvTextStyles.PlayerPanelCrumb,
                    color = TvColors.LabelPanel,
                    maxLines = 1,
                )
            }
        }
        if (eyebrow != null) {
            Text(
                text = eyebrow,
                style = TvTextStyles.DetailsEyebrow,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = title,
                style = titleStyle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (titleTrailing != null) {
                Text(
                    text = titleTrailing,
                    style = TvTextStyles.Meta,
                    color = TvColors.LabelPanel,
                    modifier = Modifier.padding(start = TvDimens.PanelFieldGap),
                )
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = TvTextStyles.RowSubtitle,
                color = TvColors.Text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = TvDimens.PanelSupportingTopPadding),
            )
        }
    }
}

private fun panelTitleTextStyle(menuItem: SidePanelMenuItem): TextStyle =
    menuItem.titleTextStyle ?: when (menuItem.titleStyle) {
        SidePanelTitleStyle.SubtitleGroupHeader -> TvTextStyles.RowPanel
        SidePanelTitleStyle.SubtitleItem -> TvTextStyles.RowPanel.copy(fontWeight = FontWeight.Normal)
        SidePanelTitleStyle.Default -> TvTextStyles.RowPanel
    }

private fun panelSupportingTextStyle(menuItem: SidePanelMenuItem): TextStyle =
    when (menuItem.supportingStyle) {
        SidePanelSupportingStyle.SourceOption -> TvTextStyles.RowSubtitle
        SidePanelSupportingStyle.SourceError -> TvTextStyles.RowSubtitle.copy(color = TvColors.ErrorText)
        SidePanelSupportingStyle.Default -> TvTextStyles.RowSubtitle
    }

private fun panelItemModifier(menuItem: SidePanelMenuItem, isFocused: Boolean): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(start = menuItem.itemStartPadding)
        .then(
            menuItem.itemBackgroundColor?.let { color ->
                menuItem.itemBackgroundShape?.let { shape ->
                    Modifier.background(color = color, shape = shape)
                } ?: Modifier.background(color)
            } ?: Modifier
        )
        .then(
            if (isFocused) {
                menuItem.focusedItemShape?.let { shape ->
                    Modifier.clip(shape)
                } ?: Modifier
            } else {
                Modifier
            }
        )
        .then(
            if (menuItem.testTag.isNullOrBlank()) {
                Modifier
            } else {
                Modifier.testTag(menuItem.testTag)
            }
        )

private fun isExitUpKey(nativeEvent: AndroidKeyEvent, canExitUp: Boolean): Boolean =
    canExitUp && (nativeEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_UP ||
        nativeEvent.keyCode == AndroidKeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP)

/**
 * Key handling shared by every row, in this order: exit up (key down), then only key up:
 * exit up (consumed), the item's `onKeyUp`, group expand / collapse, left / right action
 * tokens, MENU, OK ([onConfirmKeyUp], row specific), Left closes the panel.
 * Returns whether the event was consumed.
 */
private fun handlePanelRowKey(
    nativeEvent: AndroidKeyEvent,
    menuItem: SidePanelMenuItem,
    canExitUp: Boolean,
    onExitUpRequested: (() -> Unit)?,
    onDirectionalActionToken: ((Any) -> Unit)?,
    closeOnLeftPress: Boolean,
    onCloseRequested: () -> Unit,
    toggleGroup: ((Boolean) -> Boolean)? = null,
    onConfirmKeyUp: (() -> Boolean)? = null,
): Boolean {
    val isExitUpEvent = isExitUpKey(nativeEvent, canExitUp)
    if (isExitUpEvent && nativeEvent.action == AndroidKeyEvent.ACTION_DOWN) {
        onExitUpRequested?.invoke()
        return true
    }

    if (nativeEvent.action != AndroidKeyEvent.ACTION_UP) {
        return false
    }

    if (isExitUpEvent) {
        return true
    }

    if (menuItem.onKeyUp?.invoke(nativeEvent.keyCode) == true) {
        return true
    }

    if (toggleGroup != null) {
        when (nativeEvent.keyCode) {
            AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
            AndroidKeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
                if (toggleGroup(true)) return true
            }

            AndroidKeyEvent.KEYCODE_DPAD_LEFT,
            AndroidKeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                if (toggleGroup(false)) return true
            }
        }
    }

    when (nativeEvent.keyCode) {
        AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
        AndroidKeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
            val token = menuItem.onRightActionToken
            if (token != null && onDirectionalActionToken != null) {
                onDirectionalActionToken(token)
                return true
            }
        }

        AndroidKeyEvent.KEYCODE_DPAD_LEFT,
        AndroidKeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
            val token = menuItem.onLeftActionToken
            if (token != null && onDirectionalActionToken != null) {
                onDirectionalActionToken(token)
                return true
            }
        }
    }

    return when (nativeEvent.keyCode) {
        AndroidKeyEvent.KEYCODE_MENU -> {
            val onMenuClick = menuItem.onMenuClick ?: return false
            onMenuClick()
            true
        }

        AndroidKeyEvent.KEYCODE_ENTER,
        AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
        AndroidKeyEvent.KEYCODE_DPAD_CENTER -> onConfirmKeyUp?.invoke() ?: false

        AndroidKeyEvent.KEYCODE_DPAD_LEFT,
        AndroidKeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
            if (!closeOnLeftPress) {
                return false
            }
            onCloseRequested()
            true
        }

        else -> false
    }
}

@Composable
private fun PanelTextFieldRow(
    menuItem: SidePanelMenuItem,
    inlineTextField: SidePanelInlineTextField,
    isFocused: Boolean,
    onFocusChanged: (FocusState) -> Unit,
    focusRequester: FocusRequester?,
    canExitUp: Boolean,
    rowHeight: Dp,
    onExitUpRequested: (() -> Unit)?,
    onDirectionalActionToken: ((Any) -> Unit)?,
    onInlineTextFieldValueChanged: ((Any, String) -> Unit)?,
    onInlineTextFieldSubmit: ((Any, String) -> Unit)?,
    closeOnLeftPress: Boolean,
    onCloseRequested: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val confirmArmed = remember(menuItem.id) { mutableStateOf(false) }
    val titleTextStyle = panelTitleTextStyle(menuItem)
    val supportingTextStyle = panelSupportingTextStyle(menuItem)
    val focusModifier = focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
    val cursorColor = MaterialTheme.colorScheme.onSurface

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.RowGap),
        modifier = panelItemModifier(menuItem, isFocused)
            .heightIn(min = rowHeight)
            .tvFocusFill(focused = isFocused, shape = TvShapes.PanelRow, focusedContainer = MaterialTheme.colorScheme.inverseSurface)
            .padding(horizontal = TvDimens.RowHorizontalPadding, vertical = TvDimens.PanelFieldVerticalPadding / 2),
    ) {
        menuItem.leadingContent?.let { leading ->
            CompositionLocalProvider(
                LocalContentColor provides if (isFocused) TvColors.OnFocus else TvColors.Text,
            ) { leading() }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = menuItem.title,
                style = titleTextStyle,
                color = if (isFocused) TvColors.OnFocus else TvColors.Text,
                maxLines = menuItem.titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )

            BasicTextField(
                value = inlineTextField.value,
                onValueChange = { updatedValue ->
                    val token = inlineTextField.valueChangeToken
                    if (token != null && onInlineTextFieldValueChanged != null) {
                        onInlineTextFieldValueChanged(token, updatedValue)
                    }
                },
                singleLine = true,
                enabled = menuItem.enabled,
                textStyle = TvTextStyles.PlayerPanelMeta.copy(
                    color = if (isFocused) TvColors.OnFocusSecondary else TvColors.RowSubtitle,
                ),
                cursorBrush = SolidColor(if (isFocused) TvColors.OnFocus else cursorColor),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = TvDimens.RowSubtitleTopPadding),
                    ) {
                        if (inlineTextField.value.isBlank() &&
                            inlineTextField.placeholder.isNotBlank()
                        ) {
                            Text(
                                text = inlineTextField.placeholder,
                                style = TvTextStyles.PlayerPanelMeta,
                                color = if (isFocused) TvColors.OnFocusSecondary else TvColors.RowSubtitle,
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(focusModifier)
                    .onFocusChanged(onFocusChanged)
                    .semantics(mergeDescendants = true) { }
                    .onPreviewKeyEvent { keyEvent ->
                        val nativeEvent = keyEvent.nativeKeyEvent

                        // The OK key-up that completes the click on the previous row must not open the keyboard.
                        val isConfirm = nativeEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
                            nativeEvent.keyCode == AndroidKeyEvent.KEYCODE_ENTER
                        if (isConfirm) {
                            if (nativeEvent.action == AndroidKeyEvent.ACTION_DOWN) {
                                confirmArmed.value = true
                            } else if (nativeEvent.action == AndroidKeyEvent.ACTION_UP && !confirmArmed.value) {
                                return@onPreviewKeyEvent true
                            } else if (nativeEvent.action == AndroidKeyEvent.ACTION_UP) {
                                confirmArmed.value = false
                            }
                        }
                        // A focused text field swallows up/down on TV; move focus explicitly.
                        if (nativeEvent.action == AndroidKeyEvent.ACTION_DOWN && !isExitUpKey(nativeEvent, canExitUp)) {
                            val direction = when (nativeEvent.keyCode) {
                                AndroidKeyEvent.KEYCODE_DPAD_DOWN -> FocusDirection.Down
                                AndroidKeyEvent.KEYCODE_DPAD_UP -> FocusDirection.Up
                                else -> null
                            }
                            if (direction != null) {
                                focusManager.moveFocus(direction)
                                return@onPreviewKeyEvent true
                            }
                        }

                        handlePanelRowKey(
                            nativeEvent = nativeEvent,
                            menuItem = menuItem,
                            canExitUp = canExitUp,
                            onExitUpRequested = onExitUpRequested,
                            onDirectionalActionToken = onDirectionalActionToken,
                            closeOnLeftPress = closeOnLeftPress,
                            onCloseRequested = onCloseRequested,
                            onConfirmKeyUp = {
                                val token = inlineTextField.submitToken
                                if (token != null && onInlineTextFieldSubmit != null) {
                                    onInlineTextFieldSubmit(token, inlineTextField.value)
                                    true
                                } else {
                                    false
                                }
                            },
                        )
                    },
            )

            if (menuItem.supportingTexts.isNotEmpty()) {
                DotSeparatedRow(
                    texts = menuItem.supportingTexts,
                    textStyle = supportingTextStyle,
                    modifier = Modifier.padding(top = TvDimens.PanelSupportingTopPadding),
                )
            }
        }
    }
}

@Composable
private fun PanelRow(
    menuItem: SidePanelMenuItem,
    isFocused: Boolean,
    isGroupExpanded: Boolean,
    longPressFired: BooleanArray,
    toggleGroup: (Boolean?) -> Boolean,
    onFocusChanged: (FocusState) -> Unit,
    focusRequester: FocusRequester?,
    canExitUp: Boolean,
    rowHeight: Dp,
    fixedRowHeight: Boolean,
    showSelectionRadio: Boolean,
    selectionIndicatorStyle: SidePanelSelectionIndicatorStyle,
    onActionTokenClick: ((Any) -> Unit)?,
    onExitUpRequested: (() -> Unit)?,
    onDirectionalActionToken: ((Any) -> Unit)?,
    closeOnLeftPress: Boolean,
    onCloseRequested: () -> Unit,
) {
    val titleTextStyle = panelTitleTextStyle(menuItem)
    val supportingTextStyle = panelSupportingTextStyle(menuItem)
    val groupKey = menuItem.expandableGroupKey
    val showSelectionIndicator = menuItem.showTrailingRadio || showSelectionRadio
    TvListRow(
        title = menuItem.title,
        titleStyle = titleTextStyle,
        titleMaxLines = menuItem.titleMaxLines,
        height = rowHeight,
        fixedHeight = fixedRowHeight,
        startInset = menuItem.contentInsetStart,
        tint = menuItem.titleTint,
        shape = TvShapes.PanelRow,
        selected = menuItem.selected && !showSelectionIndicator && !menuItem.showLeadingRadio,
        enabled = menuItem.enabled,
        onClick = {
            if (groupKey != null) {
                toggleGroup(null)
                return@TvListRow
            }
            val token = menuItem.actionToken
            if (token != null && onActionTokenClick != null) {
                onActionTokenClick(token)
            } else {
                menuItem.onClick()
            }
        },
        modifier = panelItemModifier(menuItem, isFocused)
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .onFocusChanged(onFocusChanged)
            .semantics(mergeDescendants = true) { }
            .onPreviewKeyEvent { keyEvent ->
                val nativeEvent = keyEvent.nativeKeyEvent
                val longClick = menuItem.onLongClick
                val isConfirmKey = nativeEvent.keyCode == AndroidKeyEvent.KEYCODE_ENTER ||
                    nativeEvent.keyCode == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER ||
                    nativeEvent.keyCode == AndroidKeyEvent.KEYCODE_DPAD_CENTER
                if (longClick != null && isConfirmKey) {
                    when {
                        nativeEvent.action == AndroidKeyEvent.ACTION_DOWN &&
                            nativeEvent.repeatCount > 0 -> {
                            if (!longPressFired[0]) {
                                longPressFired[0] = true
                                longClick()
                            }
                            return@onPreviewKeyEvent true
                        }

                        nativeEvent.action == AndroidKeyEvent.ACTION_UP && longPressFired[0] -> {
                            longPressFired[0] = false
                            return@onPreviewKeyEvent true
                        }
                    }
                }

                handlePanelRowKey(
                    nativeEvent = nativeEvent,
                    menuItem = menuItem,
                    canExitUp = canExitUp,
                    onExitUpRequested = onExitUpRequested,
                    onDirectionalActionToken = onDirectionalActionToken,
                    closeOnLeftPress = closeOnLeftPress,
                    onCloseRequested = onCloseRequested,
                    toggleGroup = toggleGroup,
                )
            },
        supportingContent = when {
            menuItem.supportingContent != null -> menuItem.supportingContent
            menuItem.supportingTexts.isNotEmpty() -> {
                {
                  CompositionLocalProvider(
                    LocalContentColor provides if (isFocused) TvColors.OnFocusSecondary else TvColors.Text2,
                  ) {
                    DotSeparatedRow(
                        texts = menuItem.supportingTexts,
                        textStyle = if (isFocused && menuItem.supportingStyle != SidePanelSupportingStyle.SourceError) {
                            supportingTextStyle.copy(color = TvColors.OnFocusSecondary)
                        } else {
                            supportingTextStyle
                        },
                        modifier = Modifier.padding(top = TvDimens.RowSubtitleTopPadding),
                    )
                  }
                }
            }
            else -> null
        },
        leadingIndicator = if (menuItem.showLeadingRadio) {
            TvRowLeading.Radio(selected = menuItem.selected)
        } else if (showSelectionIndicator &&
            selectionIndicatorStyle == SidePanelSelectionIndicatorStyle.RadioButton
        ) {
            TvRowLeading.Radio(selected = menuItem.selected)
        } else {
            null
        },
        leading = when {
            menuItem.leadingTileText != null -> { focused ->
                TvInitialsTile(menuItem.leadingTileText, focused)
            }
            else -> menuItem.leadingContent?.let { leadingContent ->
                { _ -> leadingContent() }
            }
        },
        trailing = when {
            menuItem.trailingContent != null -> TvRowTrailing.Custom {
                menuItem.trailingContent.invoke()
            }
            menuItem.showChevron && menuItem.titleStyle == SidePanelTitleStyle.SubtitleGroupHeader ->
                TvRowTrailing.Custom { _ ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        menuItem.trailingText?.let { count ->
                            Text(
                                text = count,
                                style = TvTextStyles.PlayerPanelMeta,
                                color = LocalContentColor.current.copy(alpha = 0.6f),
                            )
                        }
                        Icon(
                            imageVector = if (isGroupExpanded) {
                                com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerIcons.ChevronUp
                            } else {
                                com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerIcons.ChevronDown
                            },
                            contentDescription = null,
                            modifier = Modifier.size(TvDimens.RowTrailingIconSize),
                        )
                    }
                }
            menuItem.showChevron && isGroupExpanded -> TvRowTrailing.Custom { _ ->
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(TvDimens.RowTrailingIconSize),
                )
            }
            menuItem.showChevron -> TvRowTrailing.Chevron
            menuItem.showTrailingRadio -> TvRowTrailing.Radio(selected = menuItem.selected)
            showSelectionIndicator -> when (selectionIndicatorStyle) {
                SidePanelSelectionIndicatorStyle.Checkmark ->
                    TvRowTrailing.Check(checked = menuItem.selected)
                SidePanelSelectionIndicatorStyle.RadioButton ->
                    TvRowTrailing.None
                SidePanelSelectionIndicatorStyle.TrailingRadio ->
                    TvRowTrailing.Radio(selected = menuItem.selected)
            }
            else -> TvRowTrailing.None
        },
    )
}

@Composable
private fun TvInitialsTile(text: String, focused: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(TvDimens.PanelTileSize)
            .background(
                if (focused) TvColors.TileFocused else TvColors.Tile,
                RoundedCornerShape(TvDimens.PanelTileRadius),
            ),
    ) {
        Text(
            text = text,
            style = TvTextStyles.Tile,
            color = if (focused) TvColors.OnFocus else TvColors.Meta,
        )
    }
}
