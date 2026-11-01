package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.MenuListSidePanel
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelContentNavigationDirection
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelSelectionIndicatorStyle
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

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
                titleTrailing = stringResource(
                    R.string.tv_player_sources_found,
                    panels.sourceItems.count { !it.isSectionHeader },
                ),
                items = panels.sourceItems,
                rowHeight = TvDimens.PlayerSourceRowHeight,
                fixedRowHeight = true,
                uppercaseSectionHeaders = true,
                showSelectionRadio = false,
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
                titleStyle = TvTextStyles.PlayerPanelTitle,
                topPadding = TvDimens.PlayerPanelTopPadding,
                rowHeight = if (panels.subtitlePanelScreen == TvPlayerSubtitlePanelScreen.EncodingSelection) {
                    TvDimens.PlayerPanelOptionRowHeight
                } else {
                    TvDimens.PlayerPanelRowHeight
                },
                uppercaseSectionHeaders = true,
                backLabel = when (panels.subtitlePanelScreen) {
                    TvPlayerSubtitlePanelScreen.Main -> null
                    TvPlayerSubtitlePanelScreen.EncodingSelection,
                    TvPlayerSubtitlePanelScreen.OnlineSearch -> stringResource(R.string.player_subtitles_settings)
                    TvPlayerSubtitlePanelScreen.OnlineLanguageSelection -> stringResource(R.string.player_load_subtitles_online)
                },
                title = when (panels.subtitlePanelScreen) {
                    TvPlayerSubtitlePanelScreen.Main -> stringResource(R.string.player_subtitles_settings)
                    TvPlayerSubtitlePanelScreen.EncodingSelection -> stringResource(R.string.subtitles_encoding)
                    TvPlayerSubtitlePanelScreen.OnlineSearch -> stringResource(R.string.player_load_subtitles_online)
                    TvPlayerSubtitlePanelScreen.OnlineLanguageSelection -> stringResource(R.string.subs_subtitle_languages)
                },
                items = when (panels.subtitlePanelScreen) {
                    TvPlayerSubtitlePanelScreen.Main -> panels.subtitleItems
                    TvPlayerSubtitlePanelScreen.EncodingSelection -> panels.subtitleEncodingItems
                    TvPlayerSubtitlePanelScreen.OnlineSearch,
                    TvPlayerSubtitlePanelScreen.OnlineLanguageSelection -> panels.subtitleOnlineItems
                },
                showSelectionRadio = false,
                selectionIndicatorStyle = SidePanelSelectionIndicatorStyle.Checkmark,
                initialFocusedItemId = when (panels.subtitlePanelScreen) {
                    TvPlayerSubtitlePanelScreen.Main -> panels.subtitleInitialFocusedItemId
                    TvPlayerSubtitlePanelScreen.EncodingSelection -> panels.subtitleEncodingInitialFocusedItemId
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
                titleStyle = TvTextStyles.PlayerPanelTitle,
                topPadding = TvDimens.PlayerPanelTopPadding,
                rowHeight = TvDimens.PlayerPanelRowHeight,
                items = panels.trackItems,
                showSelectionRadio = true,
                selectionIndicatorStyle = SidePanelSelectionIndicatorStyle.TrailingRadio,
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
