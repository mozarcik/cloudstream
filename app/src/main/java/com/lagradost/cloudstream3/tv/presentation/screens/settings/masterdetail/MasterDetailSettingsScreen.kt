package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester

@Composable
fun MasterDetailSettingsScreen(
    onExitSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onTopBarFocusableChanged: (Boolean) -> Unit = {},
    onTopBarDownNavigationEnabledChanged: (Boolean) -> Unit = {},
    onTopBarVisibleChanged: (Boolean) -> Unit = {},
    topBarFocusRequester: FocusRequester? = null,
    isTopBarFocused: Boolean = false,
    showDebugOverlay: Boolean = false
) {
    val registry = rememberMasterDetailSettingsRegistry()
    val hostState = rememberSettingsHostState(
        registry = registry,
        rootScreenId = MasterDetailSettingsDefaults.RootScreenId
    )

    SettingsHost(
        hostState = hostState,
        onExitSettings = onExitSettings,
        onTopBarFocusableChanged = onTopBarFocusableChanged,
        onTopBarDownNavigationEnabledChanged = onTopBarDownNavigationEnabledChanged,
        onTopBarVisibleChanged = onTopBarVisibleChanged,
        topBarFocusRequester = topBarFocusRequester,
        isTopBarFocused = isTopBarFocused,
        modifier = modifier,
        showDebugOverlay = showDebugOverlay
    )
}
