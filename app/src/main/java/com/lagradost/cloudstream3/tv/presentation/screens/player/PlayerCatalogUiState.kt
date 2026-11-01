package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

enum class TvPlayerSubtitleSelectionSource {
    None,
    Auto,
    User,
    PlaybackErrorRecovery,
}

@Immutable
data class PlayerCatalogUiState(
    val sourceCount: Int = 0,
    val sources: PersistentList<ExtractorLink> = persistentListOf(),
    val currentSourceIndex: Int = -1,
    val isCurrentSourceReady: Boolean = false,
    val subtitles: PersistentList<SubtitleData> = persistentListOf(),
    val selectedSubtitle: SubtitleData? = null,
    val selectedSubtitleId: String? = null,
    val selectedSubtitleIndex: Int = -1,
    val subtitleSelectionSource: TvPlayerSubtitleSelectionSource = TvPlayerSubtitleSelectionSource.None,
    val selectedAudioTrackIndex: Int = -1,
)
