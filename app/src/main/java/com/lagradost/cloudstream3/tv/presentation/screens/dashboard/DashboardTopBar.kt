package com.lagradost.cloudstream3.tv.presentation.screens.dashboard
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.data.util.StringConstants
import com.lagradost.cloudstream3.tv.presentation.screens.Screens
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamCardShape
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamTokens
import com.lagradost.cloudstream3.tv.presentation.theme.IconSize
import com.lagradost.cloudstream3.tv.presentation.theme.InterFontFamily
import com.lagradost.cloudstream3.tv.presentation.utils.occupyScreenSize
import com.lagradost.cloudstream3.utils.DataStoreHelper
import kotlinx.coroutines.delay
import java.util.Locale

val TopBarTabs = Screens.entries.toList().filter { it.isTabItem && it != Screens.Sources }
const val DashboardTopBarAvatarTestTag = "dashboard_top_bar_avatar"
const val DashboardTopBarTabsRowTestTag = "dashboard_top_bar_tabs"

private const val PROFILE_SCREEN_INDEX = -1
private const val TopBarFocusNavigationDelayMs = 250L
private const val FocusIntoContentRetryMs = 50L
private const val FocusIntoContentMaxAttempts = 100
private const val FocusIntoContentSettledChecks = 6

fun dashboardTopBarTabTestTag(screen: Screens): String {
    return "dashboard_top_bar_tab_${screen.name.lowercase(Locale.ROOT)}"
}

internal fun resolveDelayedTopBarNavigationTarget(
    pendingFocusedScreen: Screens?,
    selectedScreen: Screens?,
    isTabRowFocused: Boolean,
    suppressDelayedNavigation: Boolean,
): Screens? {
    if (suppressDelayedNavigation) return null
    if (!isTabRowFocused) return null
    val targetScreen = pendingFocusedScreen ?: return null
    if (targetScreen == selectedScreen) return null
    return targetScreen
}

internal fun shouldClearPendingFocusedScreen(
    previousSelectedScreen: Screens?,
    selectedScreen: Screens?,
    pendingFocusedScreen: Screens?,
): Boolean {
    if (pendingFocusedScreen == null || selectedScreen == null) return false
    return selectedScreen != previousSelectedScreen
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DashboardTopBar(
    modifier: Modifier = Modifier,
    selectedTabIndex: Int,
    accountRefreshToken: Int = 0,
    screens: List<Screens> = TopBarTabs,
    focusRequesters: List<FocusRequester>,
    isFocusable: Boolean = true,
    isDownNavigationEnabled: Boolean = true,
    suppressDelayedNavigation: Boolean = false,
    focusRestorerResetToken: Int = 0,
    transparentBackground: Boolean = false,
    onFocusedTabIndexChanged: (Int?) -> Unit = {},
    onDownIntoContent: () -> Boolean = { false },
    onScreenSelection: (screen: Screens) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val onScreenSelectionState by rememberUpdatedState(onScreenSelection)
    val onFocusedTabIndexChangedState by rememberUpdatedState(onFocusedTabIndexChanged)
    var pendingFocusedScreen by remember { mutableStateOf<Screens?>(null) }
    var previousSelectedScreen by remember { mutableStateOf<Screens?>(null) }
    var isAccountFocused by remember { mutableStateOf(false) }
    var isHomeTabFocused by remember { mutableStateOf(false) }
    var isTabRowFocused by remember { mutableStateOf(false) }
    var focusedTabScreen by remember { mutableStateOf<Screens?>(null) }
    var focusDownAfterSelectionOf by remember { mutableStateOf<Screens?>(null) }
    val homeTabIndex = remember(screens) { screens.indexOf(Screens.Home).coerceAtLeast(0) }
    val isAccountFocusable = isFocusable && (
        selectedTabIndex == PROFILE_SCREEN_INDEX ||
            isAccountFocused ||
            isHomeTabFocused
        )
    val selectedScreen = screens.getOrNull(selectedTabIndex)
    val selectedTopBarItemIndex = if (selectedTabIndex >= 0) {
        (selectedTabIndex + 1).coerceAtMost(focusRequesters.lastIndex)
    } else {
        0
    }
    val restoreFocusRequester = focusRequesters[selectedTopBarItemIndex]
    val currentAccount = remember(accountRefreshToken) {
        DataStoreHelper.getCurrentAccount()
    }
    val avatarFallbackDrawableRes = remember(accountRefreshToken) {
        currentAccount?.let { account ->
            DataStoreHelper.profileImages.getOrNull(account.defaultImageIndex)
        }
    }

    LaunchedEffect(pendingFocusedScreen, selectedScreen, isTabRowFocused, suppressDelayedNavigation) {
        val targetScreen = resolveDelayedTopBarNavigationTarget(
            pendingFocusedScreen = pendingFocusedScreen,
            selectedScreen = selectedScreen,
            isTabRowFocused = isTabRowFocused,
            suppressDelayedNavigation = suppressDelayedNavigation,
        ) ?: return@LaunchedEffect

        delay(TopBarFocusNavigationDelayMs)
        if (
            pendingFocusedScreen == targetScreen &&
            isTabRowFocused &&
            !suppressDelayedNavigation &&
            targetScreen != selectedScreen
        ) {
            onScreenSelectionState(targetScreen)
        }
    }

    // DOWN / OK pressed before the debounce elapsed: commit the pending tab right away and move
    // focus into the freshly selected screen instead of the stale one that is still composed.
    fun commitPendingSelectionAndFocusContent(): Boolean {
        val target = pendingFocusedScreen
        if (target == null || target == selectedScreen || suppressDelayedNavigation) return false
        focusDownAfterSelectionOf = target
        onScreenSelectionState(target)
        return true
    }

    LaunchedEffect(selectedScreen, focusDownAfterSelectionOf) {
        val target = focusDownAfterSelectionOf ?: return@LaunchedEffect
        if (target != selectedScreen) return@LaunchedEffect
        // The destination composes (and may load) asynchronously, and the item that first took
        // focus can be swapped out when its data arrives. Retry until focus has stayed outside
        // the bar for a few consecutive checks.
        var checksOutsideBar = 0
        for (attempt in 0 until FocusIntoContentMaxAttempts) {
            delay(FocusIntoContentRetryMs)
            // The user moved on (another tab or out of the bar): stop stealing focus.
            if (focusedTabScreen != target && isTabRowFocused) break
            if (isTabRowFocused) {
                checksOutsideBar = 0
                focusManager.moveFocus(FocusDirection.Down)
            } else if (++checksOutsideBar >= FocusIntoContentSettledChecks) {
                break
            }
        }
        focusDownAfterSelectionOf = null
    }

    LaunchedEffect(suppressDelayedNavigation) {
        if (suppressDelayedNavigation) {
            pendingFocusedScreen = null
        }
    }

    LaunchedEffect(selectedScreen) {
        if (
            shouldClearPendingFocusedScreen(
                previousSelectedScreen = previousSelectedScreen,
                selectedScreen = selectedScreen,
                pendingFocusedScreen = pendingFocusedScreen,
            )
        ) {
            pendingFocusedScreen = null
        }
        previousSelectedScreen = selectedScreen
    }

    Box(modifier = modifier) {
        key(focusRestorerResetToken) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (transparentBackground) Color.Transparent else MaterialTheme.colorScheme.surface
                    )
                    .focusRestorer(restoreFocusRequester),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    modifier = Modifier
                        .size(TvDimens.TopBarLogoSize)
                        .testTag(DashboardTopBarAvatarTestTag)
                        .focusRequester(focusRequesters[0])
                        .onFocusChanged { focusState ->
                            isAccountFocused = focusState.isFocused
                            if (focusState.isFocused) {
                                onFocusedTabIndexChangedState(PROFILE_SCREEN_INDEX)
                            } else if (!focusState.hasFocus) {
                                onFocusedTabIndexChangedState(null)
                            }
                        }
                        .focusProperties {
                            canFocus = isAccountFocusable
                            right = focusRequesters[homeTabIndex + 1]
                            down = if (isDownNavigationEnabled) {
                                FocusRequester.Default
                            } else {
                                FocusRequester.Cancel
                            }
                        }
                        .semantics {
                            contentDescription =
                                StringConstants.Composable.ContentDescription.UserAvatar
                        },
                    selected = selectedTabIndex == PROFILE_SCREEN_INDEX,
                    customImageUrl = currentAccount?.customImage,
                    fallbackDrawableRes = avatarFallbackDrawableRes,
                    onClick = {
                        onScreenSelection(Screens.Profile)
                    }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(TvDimens.TopBarLogoGap))
                    TabRow(
                        modifier = Modifier
                            .testTag(DashboardTopBarTabsRowTestTag)
                            .onFocusChanged {
                                isTabRowFocused = it.isFocused || it.hasFocus
                                if (!isTabRowFocused) {
                                    pendingFocusedScreen = null
                                    onFocusedTabIndexChangedState(null)
                                }
                            },
                        selectedTabIndex = selectedTabIndex,
                        indicator = { tabPositions, _ ->
                            if (selectedTabIndex >= 0) {
                                DashboardTopBarItemIndicator(
                                    currentTabPosition = tabPositions[selectedTabIndex],
                                    anyTabFocused = isTabRowFocused,
                                    shape = CircleShape
                                )
                            }
                        },
                        separator = { Spacer(modifier = Modifier) }
                    ) {
                        screens.forEachIndexed { index, screen ->
                            key(index) {
                                Tab(
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag(dashboardTopBarTabTestTag(screen))
                                        .onPreviewKeyEvent { event ->
                                            isDownNavigationEnabled &&
                                                event.type == KeyEventType.KeyDown &&
                                                event.key == Key.DirectionDown &&
                                                (commitPendingSelectionAndFocusContent() || onDownIntoContent())
                                        }
                                        .focusRequester(focusRequesters[index + 1])
                                        .onFocusChanged { focusState ->
                                            if (index == homeTabIndex) {
                                                isHomeTabFocused = focusState.isFocused
                                            } else if (focusState.isFocused) {
                                                isHomeTabFocused = false
                                            }
                                            if (focusState.isFocused) {
                                                focusedTabScreen = screen
                                                onFocusedTabIndexChangedState(index)
                                            }
                                        }
                                        .focusProperties {
                                            canFocus = isFocusable
                                            left = when {
                                                index == homeTabIndex -> focusRequesters[0]
                                                index > 0 -> focusRequesters[index]
                                                else -> FocusRequester.Default
                                            }
                                            right = if (index < screens.lastIndex) {
                                                focusRequesters[index + 2]
                                            } else {
                                                FocusRequester.Default
                                            }
                                            down = if (isDownNavigationEnabled) {
                                                FocusRequester.Default
                                            } else {
                                                FocusRequester.Cancel
                                            }
                                        },
                                    selected = index == selectedTabIndex,
                                    colors = TabDefaults.pillIndicatorTabColors(
                                        contentColor = CloudStreamTokens.Text2,
                                        selectedContentColor = CloudStreamTokens.Text,
                                        focusedContentColor = CloudStreamTokens.OnFocus,
                                        focusedSelectedContentColor = CloudStreamTokens.OnFocus,
                                    ),
                                    onFocus = { pendingFocusedScreen = screen },
                                    onClick = {
                                        if (!commitPendingSelectionAndFocusContent()) {
                                            focusManager.moveFocus(FocusDirection.Down)
                                        }
                                    },
                                ) {
                                    if (screen.tabIcon != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(TvDimens.TopBarSearchGap),
                                            modifier = Modifier
                                                .occupyScreenSize()
                                                .padding(horizontal = TvDimens.TopBarTabPadding)
                                        ) {
                                            Icon(
                                                screen.tabIcon,
                                                modifier = Modifier.size(TvDimens.TopBarSearchIcon),
                                                contentDescription = StringConstants.Composable
                                                    .ContentDescription.DashboardSearchButton,
                                                tint = LocalContentColor.current
                                            )
                                            Text(
                                                text = screen.title(),
                                                style = TopBarTabTextStyle.copy(
                                                    color = LocalContentColor.current
                                                )
                                            )
                                        }
                                    } else {
                                        Text(
                                            modifier = Modifier
                                                .occupyScreenSize()
                                                .padding(horizontal = TvDimens.TopBarTabPadding),
                                            text = screen.title(),
                                            style = TopBarTabTextStyle.copy(
                                                color = LocalContentColor.current
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                CloudstreamLogo(
                    modifier = Modifier
                        .alpha(0.75f)
                        .padding(end = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun CloudstreamLogo(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = StringConstants.Composable
                .ContentDescription.BrandLogoImage,
            tint = Color.Unspecified,
            modifier = Modifier
                .padding(end = 4.dp)
                .size(IconSize)
        )
        Text(
            text = stringResource(R.string.brand_logo_text),
            style = TopBarBrandTextStyle,
            color = CloudStreamTokens.Text2.copy(alpha = 0.9f),
        )
    }
}

private val TopBarTabTextStyle = TvTextStyles.Tab.copy(fontSize = 13.sp)
private val TopBarBrandTextStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
)
