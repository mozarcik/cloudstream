package com.lagradost.cloudstream3.tv.presentation.common

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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.tv.material3.ListItem
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.screens.movies.DotSeparatedRow
import kotlinx.coroutines.delay

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
    val actionToken: Any? = null,
    val onRightActionToken: Any? = null,
    val onLeftActionToken: Any? = null,
    val onClick: () -> Unit = {},
    val onMenuClick: (() -> Unit)? = null,
    val onKeyUp: ((Int) -> Boolean)? = null,
    val inlineTextField: SidePanelInlineTextField? = null,
    val supportingContent: (@Composable () -> Unit)? = null,
    val leadingContent: (@Composable () -> Unit)? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
)

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun MenuListSidePanel(
    visible: Boolean,
    onCloseRequested: () -> Unit,
    title: String,
    items: List<SidePanelMenuItem>,
    modifier: Modifier = Modifier,
    panelWidth: Dp = 340.dp,
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
    val focusRequesters = remember(focusableItemIds) {
        focusableItemIds.associateWith { FocusRequester() }
    }
    val listState = remember(resolvedContentKey) {
        LazyListState()
    }
    var hasRequestedInitialFocus by remember { mutableStateOf(false) }
    var focusedItemId by remember(resolvedContentKey) { mutableStateOf<String?>(null) }

    LaunchedEffect(visible, resolvedContentKey) {
        hasRequestedInitialFocus = false
        focusedItemId = null
    }

    suspend fun requestItemFocus(targetItemId: String): Boolean {
        val focusRequester = focusRequesters[targetItemId] ?: return false
        val focusItemListIndex = items.indexOfFirst { menuItem ->
            menuItem.id == targetItemId && isItemVisible(menuItem)
        }

        if (focusItemListIndex >= 0) {
            listState.scrollToItem(index = focusItemListIndex)
        }

        repeat(2) {
            if (focusRequester.requestFocus()) {
                return true
            }
            delay(16)
        }
        return false
    }

    LaunchedEffect(visible, resolvedContentKey, initialFocusedItemId, focusableItemIds, focusedItemId) {
        if (!visible || focusableItemIds.isEmpty()) {
            return@LaunchedEffect
        }

        if (focusedItemId?.let(focusableItemIds::contains) == true) {
            return@LaunchedEffect
        }

        val focusItemId = initialFocusedItemId?.takeIf(focusRequesters::containsKey)
            ?: focusableItemIds.first()
        repeat(40) {
            if (!visible) return@LaunchedEffect
            if (focusedItemId?.let(focusableItemIds::contains) == true) {
                return@LaunchedEffect
            }
            if (requestItemFocus(focusItemId)) {
                hasRequestedInitialFocus = true
                focusedItemId = focusItemId
                return@LaunchedEffect
            }
            delay(50)
        }
    }

    SlidingSidePanel(
        visible = visible,
        onCloseRequested = onCloseRequested,
        panelWidth = panelWidth,
        panelTestTag = panelTestTag,
        modifier = modifier
    ) {
        val panelContent: @Composable () -> Unit = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(20.dp),
                    fontSize = 20.sp,
                )

                headerContent?.invoke(this)

                LazyColumn(
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
                                    text = menuItem.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                )
                            } else {
                                var isFocused by remember(menuItem.id) { mutableStateOf(false) }
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

                                val titleTextStyle = menuItem.titleTextStyle ?: when (menuItem.titleStyle) {
                                    SidePanelTitleStyle.SubtitleGroupHeader -> MaterialTheme.typography.titleMedium
                                    SidePanelTitleStyle.SubtitleItem -> MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Normal
                                    )
                                    SidePanelTitleStyle.Default -> MaterialTheme.typography.titleMedium
                                }
                                val supportingTextStyle = when (menuItem.supportingStyle) {
                                    SidePanelSupportingStyle.SourceOption -> MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Normal
                                    )
                                    SidePanelSupportingStyle.SourceError -> MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.88f),
                                    )
                                    SidePanelSupportingStyle.Default -> MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                                val baseItemModifier = Modifier
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

                                val rowContent: @Composable () -> Unit = {
                                    val inlineTextField = menuItem.inlineTextField
                                    if (inlineTextField != null) {
                                        val focusModifier = focusRequesters[menuItem.id]?.let { focusRequester ->
                                            Modifier.focusRequester(focusRequester)
                                        } ?: Modifier
                                        val cursorColor = MaterialTheme.colorScheme.onSurface
                                        val containerColor = if (isFocused) {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
                                        } else {
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
                                        }

                                        Column(
                                            modifier = baseItemModifier
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                        ) {
                                            Text(
                                                text = menuItem.title,
                                                style = titleTextStyle,
                                                maxLines = menuItem.titleMaxLines,
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
                                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                ),
                                                cursorBrush = SolidColor(cursorColor),
                                                decorationBox = { innerTextField ->
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(top = 6.dp)
                                                            .background(
                                                                color = containerColor,
                                                                shape = MaterialTheme.shapes.small,
                                                            )
                                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    ) {
                                                        if (inlineTextField.value.isBlank() &&
                                                            inlineTextField.placeholder.isNotBlank()
                                                        ) {
                                                            Text(
                                                                text = inlineTextField.placeholder,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                                maxLines = 1,
                                                            )
                                                        }
                                                        innerTextField()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .then(focusModifier)
                                                    .onFocusChanged { focusState ->
                                                        isFocused = focusState.isFocused
                                                        if (focusState.isFocused) {
                                                            focusedItemId = menuItem.id
                                                        } else if (focusedItemId == menuItem.id && !focusState.hasFocus) {
                                                            focusedItemId = null
                                                        }
                                                    }
                                                    .semantics(mergeDescendants = true) { }
                                                    .onPreviewKeyEvent { keyEvent ->
                                                        val nativeEvent = keyEvent.nativeKeyEvent
                                                        if (nativeEvent.action != AndroidKeyEvent.ACTION_UP) {
                                                            return@onPreviewKeyEvent false
                                                        }

                                                        if (menuItem.onKeyUp?.invoke(nativeEvent.keyCode) == true) {
                                                            return@onPreviewKeyEvent true
                                                        }

                                                        when (nativeEvent.keyCode) {
                                                            AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
                                                            AndroidKeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
                                                                val token = menuItem.onRightActionToken
                                                                if (token != null && onDirectionalActionToken != null) {
                                                                    onDirectionalActionToken(token)
                                                                    return@onPreviewKeyEvent true
                                                                }
                                                            }

                                                            AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                                                            AndroidKeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                                                                val token = menuItem.onLeftActionToken
                                                                if (token != null && onDirectionalActionToken != null) {
                                                                    onDirectionalActionToken(token)
                                                                    return@onPreviewKeyEvent true
                                                                }
                                                            }
                                                        }

                                                        when (nativeEvent.keyCode) {
                                                            AndroidKeyEvent.KEYCODE_MENU -> {
                                                                val onMenuClick = menuItem.onMenuClick
                                                                    ?: return@onPreviewKeyEvent false
                                                                onMenuClick()
                                                                true
                                                            }

                                                            AndroidKeyEvent.KEYCODE_ENTER,
                                                            AndroidKeyEvent.KEYCODE_NUMPAD_ENTER,
                                                            AndroidKeyEvent.KEYCODE_DPAD_CENTER -> {
                                                                val token = inlineTextField.submitToken
                                                                if (token != null && onInlineTextFieldSubmit != null) {
                                                                    onInlineTextFieldSubmit(token, inlineTextField.value)
                                                                    return@onPreviewKeyEvent true
                                                                }
                                                                false
                                                            }

                                                            AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                                                            AndroidKeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                                                                if (!closeOnLeftPress) {
                                                                    return@onPreviewKeyEvent false
                                                                }
                                                                onCloseRequested()
                                                                true
                                                            }

                                                            else -> false
                                                        }
                                                    },
                                            )

                                            if (menuItem.supportingTexts.isNotEmpty()) {
                                                DotSeparatedRow(
                                                    texts = menuItem.supportingTexts,
                                                    textStyle = supportingTextStyle,
                                                    modifier = Modifier.padding(top = 4.dp),
                                                )
                                            }
                                        }
                                    } else {
                                        ListItem(
                                            selected = menuItem.selected,
                                            onClick = {
                                                if (groupKey != null) {
                                                    toggleGroup()
                                                    return@ListItem
                                                }
                                                val token = menuItem.actionToken
                                                if (token != null && onActionTokenClick != null) {
                                                    onActionTokenClick(token)
                                                } else {
                                                    menuItem.onClick()
                                                }
                                            },
                                            enabled = menuItem.enabled,
                                            modifier = baseItemModifier
                                                .then(
                                                    focusRequesters[menuItem.id]?.let { focusRequester ->
                                                        Modifier.focusRequester(focusRequester)
                                                    } ?: Modifier
                                                )
                                                .onFocusChanged { focusState ->
                                                    isFocused = focusState.isFocused
                                                    if (focusState.isFocused) {
                                                        focusedItemId = menuItem.id
                                                    } else if (focusedItemId == menuItem.id && !focusState.hasFocus) {
                                                        focusedItemId = null
                                                    }
                                                }
                                                .semantics(mergeDescendants = true) { }
                                                .onPreviewKeyEvent { keyEvent ->
                                                    val nativeEvent = keyEvent.nativeKeyEvent
                                                    if (nativeEvent.action != AndroidKeyEvent.ACTION_UP) {
                                                        return@onPreviewKeyEvent false
                                                    }

                                                    if (menuItem.onKeyUp?.invoke(nativeEvent.keyCode) == true) {
                                                        return@onPreviewKeyEvent true
                                                    }

                                                    when (nativeEvent.keyCode) {
                                                        AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
                                                        AndroidKeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
                                                            if (toggleGroup(expanded = true)) {
                                                                return@onPreviewKeyEvent true
                                                            }
                                                        }

                                                        AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                                                        AndroidKeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                                                            if (toggleGroup(expanded = false)) {
                                                                return@onPreviewKeyEvent true
                                                            }
                                                        }
                                                    }

                                                    when (nativeEvent.keyCode) {
                                                        AndroidKeyEvent.KEYCODE_DPAD_RIGHT,
                                                        AndroidKeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
                                                            val token = menuItem.onRightActionToken
                                                            if (token != null && onDirectionalActionToken != null) {
                                                                onDirectionalActionToken(token)
                                                                return@onPreviewKeyEvent true
                                                            }
                                                        }

                                                        AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                                                        AndroidKeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                                                            val token = menuItem.onLeftActionToken
                                                            if (token != null && onDirectionalActionToken != null) {
                                                                onDirectionalActionToken(token)
                                                                return@onPreviewKeyEvent true
                                                            }
                                                        }
                                                    }

                                                    when (nativeEvent.keyCode) {
                                                        AndroidKeyEvent.KEYCODE_MENU -> {
                                                            val onMenuClick = menuItem.onMenuClick
                                                                ?: return@onPreviewKeyEvent false
                                                            onMenuClick()
                                                            true
                                                        }

                                                        AndroidKeyEvent.KEYCODE_DPAD_LEFT,
                                                        AndroidKeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                                                            if (!closeOnLeftPress) {
                                                                return@onPreviewKeyEvent false
                                                            }
                                                            onCloseRequested()
                                                            true
                                                        }

                                                        else -> false
                                                    }
                                                },
                                            headlineContent = {
                                                Column {
                                                    Text(
                                                        text = menuItem.title,
                                                        style = titleTextStyle,
                                                        maxLines = menuItem.titleMaxLines,
                                                    )
                                                    if (menuItem.supportingContent != null) {
                                                        menuItem.supportingContent.invoke()
                                                    } else if (menuItem.supportingTexts.isNotEmpty()) {
                                                        DotSeparatedRow(
                                                            texts = menuItem.supportingTexts,
                                                            textStyle = supportingTextStyle,
                                                            modifier = Modifier.padding(top = 2.dp),
                                                        )
                                                    }
                                                }
                                            },
                                            leadingContent = menuItem.leadingContent?.let { leading ->
                                                {
                                                    leading()
                                                }
                                            },
                                            trailingContent = {
                                                when {
                                                    menuItem.trailingContent != null -> menuItem.trailingContent.invoke()
                                                    menuItem.showChevron -> {
                                                        Icon(
                                                            imageVector = if (isGroupExpanded) {
                                                                Icons.Default.KeyboardArrowDown
                                                            } else {
                                                                Icons.AutoMirrored.Filled.KeyboardArrowRight
                                                            },
                                                            contentDescription = null,
                                                        )
                                                    }
                                                    menuItem.showTrailingRadio || showSelectionRadio -> {
                                                        when (selectionIndicatorStyle) {
                                                            SidePanelSelectionIndicatorStyle.Checkmark -> {
                                                                if (menuItem.selected) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Check,
                                                                        contentDescription = null,
                                                                    )
                                                                }
                                                            }

                                                            SidePanelSelectionIndicatorStyle.RadioButton -> {
                                                                RadioButton(
                                                                    selected = menuItem.selected,
                                                                    onClick = { }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }

                                if (menuItem.animateVisibility && enableItemAnimations) {
                                    AnimatedVisibility(
                                        visible = runtimeVisible,
                                        enter = expandVertically(animationSpec = tween(durationMillis = 220)),
                                        exit = shrinkVertically(animationSpec = tween(durationMillis = 220)),
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
                            animationSpec = tween(durationMillis = 220),
                            initialOffsetX = { fullWidth -> fullWidth / 3 },
                        ) + fadeIn(animationSpec = tween(durationMillis = 220)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(durationMillis = 220),
                                    targetOffsetX = { fullWidth -> -fullWidth / 4 },
                                ) + fadeOut(animationSpec = tween(durationMillis = 220))
                            )
                    } else {
                        (slideInHorizontally(
                            animationSpec = tween(durationMillis = 220),
                            initialOffsetX = { fullWidth -> -fullWidth / 3 },
                        ) + fadeIn(animationSpec = tween(durationMillis = 220)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(durationMillis = 220),
                                    targetOffsetX = { fullWidth -> fullWidth / 4 },
                                ) + fadeOut(animationSpec = tween(durationMillis = 220))
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
