package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.MenuListSidePanel
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelContentNavigationDirection
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelSelectionIndicatorStyle

@Composable
internal fun PlayerSidePanels(
    panels: TvPlayerPanelsUiState,
    onCloseRequested: () -> Unit,
    onSubtitlesBackRequested: () -> Boolean,
    onItemAction: (TvPlayerPanelItemAction) -> Unit,
) {
    when (panels.activePanel) {
        TvPlayerSidePanel.None -> Unit
        TvPlayerSidePanel.Sources -> {
            MenuListSidePanel(
                visible = true,
                onCloseRequested = onCloseRequested,
                title = stringResource(R.string.sources),
                items = panels.sourceItems,
                showSelectionRadio = true,
                selectionIndicatorStyle = SidePanelSelectionIndicatorStyle.Checkmark,
                initialFocusedItemId = panels.sourceInitialFocusedItemId,
                closeOnLeftPress = false,
                closeOnFocusExit = false,
                enableContentAnimation = false,
                enableItemAnimations = false,
                onActionTokenClick = { token ->
                    (token as? TvPlayerPanelItemAction)?.let(onItemAction)
                },
                onDirectionalActionToken = { token ->
                    (token as? TvPlayerPanelItemAction)?.let(onItemAction)
                },
            )
        }

        TvPlayerSidePanel.Subtitles -> {
            MenuListSidePanel(
                visible = true,
                onCloseRequested = {
                    if (!onSubtitlesBackRequested()) {
                        onCloseRequested()
                    }
                },
                title = when (panels.subtitlePanelScreen) {
                    TvPlayerSubtitlePanelScreen.Main -> stringResource(R.string.player_subtitles_settings)
                    TvPlayerSubtitlePanelScreen.OnlineSearch -> stringResource(R.string.player_load_subtitles_online)
                    TvPlayerSubtitlePanelScreen.OnlineLanguageSelection -> stringResource(R.string.subs_subtitle_languages)
                },
                items = when (panels.subtitlePanelScreen) {
                    TvPlayerSubtitlePanelScreen.Main -> panels.subtitleItems
                    TvPlayerSubtitlePanelScreen.OnlineSearch,
                    TvPlayerSubtitlePanelScreen.OnlineLanguageSelection -> panels.subtitleOnlineItems
                },
                showSelectionRadio = false,
                selectionIndicatorStyle = SidePanelSelectionIndicatorStyle.Checkmark,
                initialFocusedItemId = when (panels.subtitlePanelScreen) {
                    TvPlayerSubtitlePanelScreen.Main -> panels.subtitleInitialFocusedItemId
                    TvPlayerSubtitlePanelScreen.OnlineSearch,
                    TvPlayerSubtitlePanelScreen.OnlineLanguageSelection -> panels.subtitleOnlineInitialFocusedItemId
                },
                contentNavigationDirection = when (panels.subtitlePanelNavigationDirection) {
                    TvPlayerSubtitlePanelNavigationDirection.Forward -> SidePanelContentNavigationDirection.Forward
                    TvPlayerSubtitlePanelNavigationDirection.Backward -> SidePanelContentNavigationDirection.Backward
                },
                closeOnLeftPress = false,
                closeOnFocusExit = false,
                enableContentAnimation = false,
                enableItemAnimations = false,
                onActionTokenClick = { token ->
                    (token as? TvPlayerPanelItemAction)?.let(onItemAction)
                },
                onDirectionalActionToken = { token ->
                    (token as? TvPlayerPanelItemAction)?.let(onItemAction)
                },
                onInlineTextFieldValueChanged = { token, value ->
                    if (token == TvPlayerPanelItemAction.EditOnlineSubtitlesQuery) {
                        onItemAction(
                            TvPlayerPanelItemAction.UpdateOnlineSubtitlesQuery(
                                query = value,
                            )
                        )
                    }
                },
            )
        }

        TvPlayerSidePanel.Tracks -> {
            MenuListSidePanel(
                visible = true,
                onCloseRequested = onCloseRequested,
                title = stringResource(R.string.audio_tracks),
                items = panels.trackItems,
                showSelectionRadio = true,
                selectionIndicatorStyle = SidePanelSelectionIndicatorStyle.Checkmark,
                initialFocusedItemId = panels.trackInitialFocusedItemId,
                closeOnLeftPress = false,
                closeOnFocusExit = false,
                enableContentAnimation = false,
                enableItemAnimations = false,
                onActionTokenClick = { token ->
                    (token as? TvPlayerPanelItemAction)?.let(onItemAction)
                },
                onDirectionalActionToken = { token ->
                    (token as? TvPlayerPanelItemAction)?.let(onItemAction)
                },
            )
        }
    }
}

internal fun TvPlayerPanelItemAction.requiresPlaybackRestore(): Boolean {
    return when (this) {
        is TvPlayerPanelItemAction.SelectSource,
        TvPlayerPanelItemAction.DisableSubtitles,
        is TvPlayerPanelItemAction.SelectSubtitle,
        is TvPlayerPanelItemAction.SelectOnlineSubtitleResult,
        TvPlayerPanelItemAction.LoadFirstAvailableSubtitle,
        TvPlayerPanelItemAction.SelectDefaultTrack,
        is TvPlayerPanelItemAction.SelectTrack -> true
        else -> false
    }
}
