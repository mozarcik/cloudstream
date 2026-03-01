package com.lagradost.cloudstream3.tv.presentation.screens.player

import com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem

internal const val SubtitleLoadFromInternetItemId = "subtitle_load_online"
internal const val SubtitleLoadFromFileItemId = "subtitle_load_file"
internal const val SubtitleLoadFirstAvailableItemId = "subtitle_load_first_available"
internal const val SubtitleOnlineQueryItemId = "subtitle_online_query"
internal const val SubtitleOnlineLanguageItemId = "subtitle_online_language"
internal const val SubtitleOnlineLanguageOptionItemPrefix = "subtitle_online_language_option_"
internal const val SubtitleOnlineLoadingItemId = "subtitle_online_loading"
internal const val SubtitleOnlineEmptyItemId = "subtitle_online_empty"
internal const val SubtitleOnlineErrorItemId = "subtitle_online_error"
internal const val SubtitleOnlineRetryItemId = "subtitle_online_retry"

enum class TvPlayerSidePanel {
    None,
    Sources,
    Subtitles,
    Tracks,
}

enum class TvPlayerSubtitlePanelScreen {
    Main,
    OnlineSearch,
    OnlineLanguageSelection,
}

enum class TvPlayerSubtitlePanelNavigationDirection {
    Forward,
    Backward,
}

enum class TvPlayerOnlineSubtitlesStatus {
    Idle,
    Loading,
    Empty,
    Error,
    Results,
}

enum class TvPlayerSourceStatus {
    Loading,
    Success,
    Error,
}

data class TvPlayerSourceState(
    val status: TvPlayerSourceStatus = TvPlayerSourceStatus.Loading,
    val httpCode: Int? = null,
)

data class TvPlayerSourceErrorDialog(
    val sourceIndex: Int,
    val sourceLabel: String,
    val message: String,
)

data class TvPlayerPlaybackErrorDetails(
    val exoErrorCode: Int,
    val exoErrorName: String,
    val httpCode: Int? = null,
)

data class TvPlayerSubtitleLanguageOption(
    val tag: String,
    val label: String,
)

data class TvPlayerOnlineSubtitleResult(
    val id: String,
    val providerIdPrefix: String,
    val subtitle: AbstractSubtitleEntities.SubtitleEntity,
    val title: String,
    val supportingTexts: List<String>,
)

data class TvPlayerOnlineSubtitlesState(
    val query: String = "",
    val selectedLanguageTag: String = "",
    val selectedLanguageLabel: String = "",
    val status: TvPlayerOnlineSubtitlesStatus = TvPlayerOnlineSubtitlesStatus.Idle,
    val results: List<TvPlayerOnlineSubtitleResult> = emptyList(),
    val errorMessage: String? = null,
)

sealed interface TvPlayerPanelEffect {
    data object OpenSubtitleFilePicker : TvPlayerPanelEffect

    data class OpenSourceErrorDialog(
        val dialog: TvPlayerSourceErrorDialog,
    ) : TvPlayerPanelEffect
}

sealed interface TvPlayerPanelItemAction {
    data object None : TvPlayerPanelItemAction

    data class SelectSource(val index: Int) : TvPlayerPanelItemAction
    data class InspectSourceError(val index: Int) : TvPlayerPanelItemAction
    data object DisableSubtitles : TvPlayerPanelItemAction
    data class SelectSubtitle(val index: Int) : TvPlayerPanelItemAction
    data object SelectDefaultTrack : TvPlayerPanelItemAction
    data class SelectTrack(val index: Int) : TvPlayerPanelItemAction
    data class ToggleSubtitleGroup(val groupKey: String) : TvPlayerPanelItemAction
    data class ExpandSubtitleGroup(val groupKey: String) : TvPlayerPanelItemAction
    data class CollapseSubtitleGroup(val groupKey: String) : TvPlayerPanelItemAction
    data object LoadSubtitleFromFile : TvPlayerPanelItemAction
    data object OpenOnlineSubtitles : TvPlayerPanelItemAction
    data object LoadFirstAvailableSubtitle : TvPlayerPanelItemAction
    data object BackFromOnlineSubtitles : TvPlayerPanelItemAction
    data object EditOnlineSubtitlesQuery : TvPlayerPanelItemAction
    data class UpdateOnlineSubtitlesQuery(val query: String) : TvPlayerPanelItemAction
    data object SelectOnlineSubtitlesLanguage : TvPlayerPanelItemAction
    data class SelectOnlineSubtitlesLanguageOption(val languageTag: String) : TvPlayerPanelItemAction
    data object RetryOnlineSubtitlesSearch : TvPlayerPanelItemAction
    data class SelectOnlineSubtitleResult(val resultId: String) : TvPlayerPanelItemAction
}

data class TvPlayerPanelsUiState(
    val activePanel: TvPlayerSidePanel = TvPlayerSidePanel.None,
    val sourceItems: List<SidePanelMenuItem> = emptyList(),
    val subtitleItems: List<SidePanelMenuItem> = emptyList(),
    val trackItems: List<SidePanelMenuItem> = emptyList(),
    val subtitlePanelScreen: TvPlayerSubtitlePanelScreen = TvPlayerSubtitlePanelScreen.Main,
    val subtitlePanelNavigationDirection: TvPlayerSubtitlePanelNavigationDirection = TvPlayerSubtitlePanelNavigationDirection.Forward,
    val subtitleOnlineItems: List<SidePanelMenuItem> = emptyList(),
    val sourceInitialFocusedItemId: String? = null,
    val subtitleInitialFocusedItemId: String? = null,
    val subtitleOnlineInitialFocusedItemId: String? = null,
    val trackInitialFocusedItemId: String? = null,
)
