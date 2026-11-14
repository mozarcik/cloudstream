package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.focus.requestFocusWithRetry
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import com.lagradost.cloudstream3.utils.DataStoreHelper

private val ScrimColor = Color(0xFF050609).copy(alpha = 0.86f)
private val MenuColor = Color(0xFF0B0D11)
private val MenuBrush = Brush.horizontalGradient(
    0f to MenuColor.copy(alpha = 0.99f),
    0.56f to MenuColor.copy(alpha = 0.97f),
    1f to Color.Transparent,
)
private val SeparatorColor = Color.White.copy(alpha = 0.10f)

/** Test tags of the menu rows (the menu is composed only while it is open). */
internal const val TvNavProfileTestTag = "tv_nav_profile"
internal fun tvNavScreenTestTag(target: TvNavTarget) = "tv_nav_screen_${target.name}"

/** 0 = collapsed rail (icons only), 1 = expanded menu; read in the draw phase only. */
private val LocalNavExpansion = compositionLocalOf<() -> Float> { { 1f } }

private const val ProfileKey = "profile"
private fun targetKey(target: TvNavTarget) = "screen:${target.name}"
private fun listKey(key: String) = "list:$key"

/**
 * Menu drawn over the content (not next to it, so opening it never re-lays the screen out).
 * [open] = the menu holds D-pad focus, collapsed to the icon rail; [expanded] (first Up / Down) adds
 * the labels and a scrim, animated in the draw phase only. It is composed only while it is open or
 * animating, so a closed menu can never receive D-pad focus.
 */
@Composable
internal fun TvNavDrawer(
    open: Boolean,
    expanded: Boolean,
    onExpand: () -> Unit,
    selection: TvNavSelection,
    listGroups: List<TvNavListGroup>,
    accountRefreshToken: Int,
    onOpenProfile: () -> Unit,
    onNavigate: (TvNavTarget) -> Unit,
    onOpenList: (providerName: String, listKey: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    val openState by rememberUpdatedState(open)
    val isComposed by remember { derivedStateOf { openState || progress.value > 0f } }
    LaunchedEffect(expanded) {
        progress.animateTo(
            if (expanded) 1f else 0f,
            if (expanded) tween(TvDimens.DrawerExpandMs, easing = FastOutSlowInEasing)
            else tween(TvDimens.DrawerCollapseMs, easing = FastOutLinearInEasing),
        )
    }
    if (!isComposed) return

    Box(modifier = modifier.fillMaxSize()) {
        // The rail's own scrim, so the collapsed menu looks exactly like the passive rail.
        Box(
            Modifier
                .fillMaxHeight()
                .width(TvDimens.NavRailScrimWidth)
                .background(RailScrimBrush)
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value }
                .background(ScrimColor)
        )
        Box(
            Modifier
                .fillMaxHeight()
                .width(TvDimens.NavMenuFadeWidth)
                .graphicsLayer { alpha = progress.value }
                .background(MenuBrush)
        )
        CompositionLocalProvider(LocalNavExpansion provides { progress.value }) {
        DrawerContent(
            open = open,
            onExpand = onExpand,
            selection = selection,
            listGroups = listGroups,
            accountRefreshToken = accountRefreshToken,
            onOpenProfile = onOpenProfile,
            onNavigate = onNavigate,
            onOpenList = onOpenList,
            onDismiss = onDismiss,
        )
        }
    }
}

@Composable
private fun DrawerContent(
    open: Boolean,
    onExpand: () -> Unit,
    selection: TvNavSelection,
    listGroups: List<TvNavListGroup>,
    accountRefreshToken: Int,
    onOpenProfile: () -> Unit,
    onNavigate: (TvNavTarget) -> Unit,
    onOpenList: (providerName: String, listKey: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val requesters = remember { HashMap<String, FocusRequester>() }
    fun requesterFor(key: String) = requesters.getOrPut(key) { FocusRequester() }

    val account = remember(accountRefreshToken) { DataStoreHelper.getCurrentAccount() }
    val fallbackDrawable = remember(accountRefreshToken) {
        account?.let { DataStoreHelper.profileImages.getOrNull(it.defaultImageIndex) }
    }
    val listState = rememberLazyListState()
    val listItems = buildListItems(listGroups)
    val lastRow = listItems.lastOrNull { it is MenuListItem.Row }

    val selectedRowKey = when (selection) {
        is TvNavSelection.Screen -> targetKey(selection.target)
        is TvNavSelection.Library -> listKey(selection.listKey)
        TvNavSelection.None -> targetKey(TvNavTarget.Home)
    }
    val selectedKeyState by rememberUpdatedState(selectedRowKey)

    // Focus starts on the row that marks where the user is; a list row is scrolled into place first.
    // Also follows the selection while open (Back in the menu switches to Home and the focus goes with it).
    LaunchedEffect(open, selectedRowKey) {
        if (!open) return@LaunchedEffect
        val key = selectedKeyState
        val index = listItems.indexOfFirst { it is MenuListItem.Row && listKey(it.list.key) == key }
        if (index >= 0) listState.scrollToItem(index + 2)
        requesterFor(key).requestFocusWithRetry(attempts = 18)
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(TvDimens.NavMenuRowInset + TvDimens.NavMenuRowWidth)
            .focusGroup()
            // Left does nothing, Right closes the menu, the first Up / Down expands it (and still moves
            // focus); Up / Down stop at the profile and Settings rows, so the content is out of reach.
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp, Key.DirectionDown -> {
                        onExpand()
                        false
                    }

                    Key.DirectionLeft -> true
                    Key.DirectionRight -> {
                        onDismiss()
                        true
                    }

                    else -> false
                }
            }
            .padding(start = TvDimens.NavMenuRowInset),
    ) {
        Spacer(Modifier.height(18.dp))
        ProfileRow(
            name = account?.name.orEmpty(),
            customImage = account?.customImage,
            fallbackDrawable = fallbackDrawable,
            onClick = onOpenProfile,
            modifier = Modifier
                .testTag(TvNavProfileTestTag)
                .focusRequester(requesterFor(ProfileKey))
                .focusProperties { up = FocusRequester.Cancel },
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 30.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(TvDimens.NavMenuRowGap),
            modifier = Modifier
                .fillMaxSize()
                .drawScrollThumb(listState, LocalNavExpansion.current),
        ) {
            item(key = "screens") {
                Column(verticalArrangement = Arrangement.spacedBy(TvDimens.NavMenuRowGap)) {
                    ScreenRow(Icons.Outlined.Home, R.string.title_home, TvNavTarget.Home, selection, requesterFor(targetKey(TvNavTarget.Home)), onNavigate)
                    ScreenRow(Icons.Outlined.Search, R.string.search, TvNavTarget.Search, selection, requesterFor(targetKey(TvNavTarget.Search)), onNavigate)
                    ScreenRow(Icons.Outlined.FileDownload, R.string.title_downloads, TvNavTarget.Downloads, selection, requesterFor(targetKey(TvNavTarget.Downloads)), onNavigate)
                    ScreenRow(
                        Icons.AutoMirrored.Outlined.FormatListBulleted, R.string.library, TvNavTarget.Library, selection,
                        requesterFor(targetKey(TvNavTarget.Library)), onNavigate,
                        // The lazy list does not hand focus on to the pinned Settings row by itself.
                        extraModifier = if (listItems.isEmpty()) {
                            Modifier.focusProperties { down = requesterFor(targetKey(TvNavTarget.Settings)) }
                        } else {
                            Modifier
                        },
                    )
                }
            }
            // Nothing is pinned at first: the menu is just the screens and Settings.
            if (listItems.isNotEmpty()) item(key = "separator") { Separator(Modifier.padding(vertical = 6.dp)) }
            items(listItems, key = { it.key }) { item ->
                when (item) {
                    is MenuListItem.Header -> GroupHeader(item.title)
                    is MenuListItem.Row -> ListRow(
                        list = item.list,
                        selected = (selection as? TvNavSelection.Library)?.listKey == item.list.key,
                        onClick = { onOpenList(item.providerName, item.list.key) },
                        modifier = Modifier
                            .focusRequester(requesterFor(listKey(item.list.key)))
                            // The lazy list does not hand focus on to the pinned Settings row by itself.
                            .then(
                                if (item === lastRow) {
                                    Modifier.focusProperties { down = requesterFor(targetKey(TvNavTarget.Settings)) }
                                } else {
                                    Modifier
                                }
                            ),
                    )
                }
            }
        }
        ListFade(listState, top = true, modifier = Modifier.align(Alignment.TopCenter))
        ListFade(listState, top = false, modifier = Modifier.align(Alignment.BottomCenter))
        }
        Separator(Modifier.padding(top = 8.dp, bottom = 8.dp))
        ScreenRow(
            Icons.Outlined.Settings, R.string.title_settings, TvNavTarget.Settings, selection,
            requesterFor(targetKey(TvNavTarget.Settings)), onNavigate,
            extraModifier = Modifier.focusProperties { down = FocusRequester.Cancel },
        )
        Spacer(Modifier.height(12.dp))
    }
}

private sealed class MenuListItem(val key: String) {
    class Header(val title: String) : MenuListItem("h:$title")
    class Row(val providerName: String, val list: TvNavList) : MenuListItem("l:$providerName:${list.key}")
}

@Composable
private fun buildListItems(groups: List<TvNavListGroup>): List<MenuListItem> {
    return remember(groups) {
        buildList {
            groups.forEach { group ->
                add(MenuListItem.Header(group.providerName))
                group.lists.forEach { add(MenuListItem.Row(group.providerName, it)) }
            }
        }
    }
}

@Composable
private fun Separator(modifier: Modifier = Modifier) {
    val expansion = LocalNavExpansion.current
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind {
                // Rail: 20 dp tick centred on the icons; menu: the full row width.
                val p = expansion()
                val left = lerp(18.dp.toPx(), 14.dp.toPx(), p)
                val right = lerp(38.dp.toPx(), size.width - 14.dp.toPx(), p)
                drawRect(SeparatorColor, Offset(left, 0f), Size(right - left, size.height))
            }
    )
}

@Composable
private fun GroupHeader(title: String) {
    val expansion = LocalNavExpansion.current
    Text(
        text = title.uppercase(),
        style = TvTextStyles.PanelGroupHeader,
        color = TvColors.Label,
        modifier = Modifier.graphicsLayer { alpha = expansion() }.padding(
            start = 14.dp,
            top = TvDimens.PanelGroupHeaderTop,
            bottom = TvDimens.PanelGroupHeaderBottom,
        ),
    )
}

@Composable
private fun ProfileRow(
    name: String,
    customImage: String?,
    fallbackDrawable: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val expansion = LocalNavExpansion.current
    Row(
        modifier = modifier
            .width(TvDimens.NavMenuRowWidth)
            .height(TvDimens.NavMenuProfileHeight)
            .clip(CircleShape)
            .drawPill(expansion, collapsedInset = 8.dp, collapsedWidth = TvDimens.NavMenuProfileHeight) {
                if (focused) TvColors.Focus else Color.Transparent
            }
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            // The avatar is smaller than the row, so the focus pill shows around it; the name stays where it was.
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(17.dp),
    ) {
        Box(Modifier.size(TvDimens.NavMenuAvatarSize).clip(CircleShape)) {
            AvatarImage(customImageUrl = customImage, fallbackDrawableRes = fallbackDrawable)
        }
        Column(Modifier.graphicsLayer { alpha = expansion() }) {
            Text(
                text = name,
                style = TvTextStyles.Row.copy(fontSize = TvTextStyles.Row.fontSize * 17f / 14f),
                color = if (focused) TvColors.OnFocus else TvColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.tv_menu_switch_profile),
                style = TvTextStyles.Meta,
                color = if (focused) TvColors.OnFocusSecondary else TvColors.Label,
            )
        }
    }
}

@Composable
private fun ScreenRow(
    icon: ImageVector,
    labelRes: Int,
    target: TvNavTarget,
    selection: TvNavSelection,
    focusRequester: FocusRequester,
    onNavigate: (TvNavTarget) -> Unit,
    extraModifier: Modifier = Modifier,
) {
    NavRow(
        icon = icon,
        label = stringResource(labelRes),
        height = TvDimens.NavMenuRowHeight,
        selected = (selection as? TvNavSelection.Screen)?.target == target,
        count = null,
        onClick = { onNavigate(target) },
        modifier = Modifier.testTag(tvNavScreenTestTag(target)).focusRequester(focusRequester).then(extraModifier),
    )
}

@Composable
private fun ListRow(
    list: TvNavList,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavRow(
        icon = Icons.Outlined.BookmarkBorder,
        label = list.title,
        height = TvDimens.NavMenuListRowHeight,
        selected = selected,
        count = list.count,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun NavRow(
    icon: ImageVector,
    label: String,
    height: androidx.compose.ui.unit.Dp,
    selected: Boolean,
    count: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val expansion = LocalNavExpansion.current
    val background = when {
        focused -> TvColors.Focus
        selected -> TvColors.AccentContainer
        else -> Color.Transparent
    }
    val contentColor = when {
        focused -> TvColors.OnFocus
        selected -> TvColors.OnAccentContainer
        else -> TvColors.RailIcon
    }
    Row(
        modifier = modifier
            .width(TvDimens.NavMenuRowWidth)
            .height(height)
            .clip(CircleShape)
            .drawPill(expansion, collapsedInset = 4.dp, collapsedWidth = TvDimens.NavRailTileSize) { background }
            .onFocusChanged { focused = it.isFocused }
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(start = 20.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(TvDimens.NavMenuIconSize),
        )
        Text(
            text = label,
            style = TvTextStyles.Row,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { alpha = expansion() }
                .padding(start = 16.dp),
        )
        if (count != null) {
            Text(
                text = count.toString(),
                style = TvTextStyles.Row,
                color = if (focused) TvColors.OnFocusSecondary else TvColors.Label,
                modifier = Modifier.graphicsLayer { alpha = expansion() }.padding(start = 8.dp),
            )
        }
    }
}

/** Row background: a [collapsedWidth] tile around the icon that grows to the full row; draw phase only. */
private fun Modifier.drawPill(
    expansion: () -> Float,
    collapsedInset: androidx.compose.ui.unit.Dp,
    collapsedWidth: androidx.compose.ui.unit.Dp,
    color: () -> Color,
): Modifier = drawBehind {
    val c = color()
    if (c == Color.Transparent) return@drawBehind
    val p = expansion()
    val left = lerp(collapsedInset.toPx(), 0f, p)
    val width = lerp(collapsedWidth.toPx(), size.width, p)
    drawRoundRect(c, Offset(left, 0f), Size(width, size.height), CornerRadius(size.height / 2))
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

private val FadeTopHeight = 24.dp
private val FadeBottomHeight = 36.dp
private val ThumbColor = Color.White.copy(alpha = 0.35f)

/** Edge fade over the scrolling list; only drawn while there is more to scroll in that direction. */
@Composable
private fun ListFade(state: androidx.compose.foundation.lazy.LazyListState, top: Boolean, modifier: Modifier = Modifier) {
    val visible by remember { derivedStateOf { if (top) state.canScrollBackward else state.canScrollForward } }
    if (!visible) return
    val expansion = LocalNavExpansion.current
    val brush = remember(top) {
        Brush.verticalGradient(
            if (top) listOf(MenuColor.copy(alpha = 0.97f), Color.Transparent)
            else listOf(Color.Transparent, MenuColor.copy(alpha = 0.97f))
        )
    }
    Box(
        modifier
            .graphicsLayer { alpha = expansion() }
            .fillMaxWidth()
            .height(if (top) FadeTopHeight else FadeBottomHeight)
            .background(brush)
    )
}

/** 3 dp scrollbar thumb, drawn in the draw phase from the lazy list's layout info. */
private fun Modifier.drawScrollThumb(
    state: androidx.compose.foundation.lazy.LazyListState,
    expansion: () -> Float,
): Modifier =
    drawWithContent {
        drawContent()
        if (expansion() < 0.5f) return@drawWithContent
        val info = state.layoutInfo
        val total = info.totalItemsCount
        val visible = info.visibleItemsInfo
        if (total == 0 || visible.isEmpty() || visible.size >= total) return@drawWithContent
        val avg = visible.sumOf { it.size }.toFloat() / visible.size
        val first = visible.first()
        val offsetPx = first.index * avg - first.offset
        val contentPx = total * avg
        val h = size.height
        val thumbH = (h * h / contentPx).coerceIn(24.dp.toPx(), h)
        val y = (offsetPx / (contentPx - h)).coerceIn(0f, 1f) * (h - thumbH)
        drawRoundRect(
            color = ThumbColor,
            topLeft = androidx.compose.ui.geometry.Offset(size.width - 3.dp.toPx() - 1.dp.toPx(), y),
            size = androidx.compose.ui.geometry.Size(3.dp.toPx(), thumbH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx()),
        )
    }
