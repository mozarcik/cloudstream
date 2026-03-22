package com.lagradost.cloudstream3.tv.presentation.screens.dashboard
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.data.util.StringConstants
import com.lagradost.cloudstream3.tv.presentation.screens.Screens
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamCardShape
import com.lagradost.cloudstream3.tv.presentation.theme.IconSize
import com.lagradost.cloudstream3.tv.presentation.utils.occupyScreenSize
import kotlinx.coroutines.delay
import java.util.Locale

val TopBarTabs = Screens.entries.toList().filter { it.isTabItem && it != Screens.Sources }
const val DashboardTopBarAvatarTestTag = "dashboard_top_bar_avatar"
const val DashboardTopBarTabsRowTestTag = "dashboard_top_bar_tabs"

private const val PROFILE_SCREEN_INDEX = -1
private const val TopBarFocusNavigationDelayMs = 300L

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
    screens: List<Screens> = TopBarTabs,
    focusRequesters: List<FocusRequester>,
    isFocusable: Boolean = true,
    isDownNavigationEnabled: Boolean = true,
    suppressDelayedNavigation: Boolean = false,
    focusRestorerResetToken: Int = 0,
    onFocusedTabIndexChanged: (Int?) -> Unit = {},
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
    val homeTabIndex = remember(screens) { screens.indexOf(Screens.Home).coerceAtLeast(0) }
    val isAccountFocusable = isFocusable && (isAccountFocused || isHomeTabFocused)
    val selectedScreen = screens.getOrNull(selectedTabIndex)
    val selectedTopBarItemIndex = if (selectedTabIndex >= 0) {
        (selectedTabIndex + 1).coerceAtMost(focusRequesters.lastIndex)
    } else {
        0
    }
    val restoreFocusRequester = focusRequesters[selectedTopBarItemIndex]

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
                    .background(MaterialTheme.colorScheme.surface)
                    .focusRestorer(restoreFocusRequester),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    modifier = Modifier
                        .size(32.dp)
                        .testTag(DashboardTopBarAvatarTestTag)
                        .focusRequester(focusRequesters[0])
                        .onFocusChanged { focusState ->
                            isAccountFocused = focusState.isFocused
                            if (focusState.isFocused) {
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
                    onClick = {
                        onScreenSelection(Screens.Profile)
                    }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(20.dp))
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
                                    shape = CloudStreamCardShape
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
                                        .focusRequester(focusRequesters[index + 1])
                                        .onFocusChanged { focusState ->
                                            if (index == homeTabIndex) {
                                                isHomeTabFocused = focusState.isFocused
                                            } else if (focusState.isFocused) {
                                                isHomeTabFocused = false
                                            }
                                            if (focusState.isFocused) {
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
                                    onFocus = { pendingFocusedScreen = screen },
                                    onClick = { focusManager.moveFocus(FocusDirection.Down) },
                                ) {
                                    if (screen.tabIcon != null) {
                                        Icon(
                                            screen.tabIcon,
                                            modifier = Modifier.padding(4.dp),
                                            contentDescription = StringConstants.Composable
                                                .ContentDescription.DashboardSearchButton,
                                            tint = LocalContentColor.current
                                        )
                                    } else {
                                        Text(
                                            modifier = Modifier
                                                .occupyScreenSize()
                                                .padding(horizontal = 16.dp),
                                            text = screen.title(),
                                            style = MaterialTheme.typography.titleSmall.copy(
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
            style = MaterialTheme.typography.titleSmall
        )
    }
}
