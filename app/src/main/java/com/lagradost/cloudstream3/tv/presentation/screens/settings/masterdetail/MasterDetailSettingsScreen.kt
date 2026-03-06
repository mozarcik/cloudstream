package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MasterDetailSettingsScreen(
    onExitSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onTopBarFocusableChanged: (Boolean) -> Unit = {},
    onTopBarDownNavigationEnabledChanged: (Boolean) -> Unit = {},
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
        modifier = modifier,
        showDebugOverlay = showDebugOverlay
    )
}
