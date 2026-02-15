package com.lagradost.cloudstream3.tv.presentation.screens.player

enum class TvPlayerSidePanel {
    None,
    Sources,
    Subtitles,
    Tracks,
}

enum class TvPlayerPanelItemStyle {
    Default,
    SourceOption,
    SubtitleGroupHeader,
    SubtitleItemSingle,
    SubtitleItemTop,
    SubtitleItemMiddle,
    SubtitleItemBottom,
}

sealed interface TvPlayerPanelItemAction {
    data object None : TvPlayerPanelItemAction

    data class SelectSource(val index: Int) : TvPlayerPanelItemAction
    data object DisableSubtitles : TvPlayerPanelItemAction
    data class SelectSubtitle(val index: Int) : TvPlayerPanelItemAction
    data object SelectDefaultTrack : TvPlayerPanelItemAction
    data class SelectTrack(val index: Int) : TvPlayerPanelItemAction
    data class ToggleSubtitleGroup(val groupKey: String) : TvPlayerPanelItemAction
    data class ExpandSubtitleGroup(val groupKey: String) : TvPlayerPanelItemAction
    data class CollapseSubtitleGroup(val groupKey: String) : TvPlayerPanelItemAction
}

data class TvPlayerPanelItemUi(
    val id: String,
    val title: String = "",
    val titleResId: Int? = null,
    val titleMaxLines: Int = 1,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val isSectionHeader: Boolean = false,
    val isVisible: Boolean = true,
    val detailTexts: List<String> = emptyList(),
    val style: TvPlayerPanelItemStyle = TvPlayerPanelItemStyle.Default,
    val showChevron: Boolean = false,
    val chevronExpanded: Boolean = false,
    val showTrailingRadio: Boolean = false,
    val action: TvPlayerPanelItemAction = TvPlayerPanelItemAction.None,
    val onRightAction: TvPlayerPanelItemAction? = null,
    val onLeftAction: TvPlayerPanelItemAction? = null,
)

data class TvPlayerPanelsUiState(
    val activePanel: TvPlayerSidePanel = TvPlayerSidePanel.None,
    val sourceItems: List<TvPlayerPanelItemUi> = emptyList(),
    val subtitleItems: List<TvPlayerPanelItemUi> = emptyList(),
    val trackItems: List<TvPlayerPanelItemUi> = emptyList(),
    val sourceInitialFocusedItemId: String? = null,
    val subtitleInitialFocusedItemId: String? = null,
    val trackInitialFocusedItemId: String? = null,
)
