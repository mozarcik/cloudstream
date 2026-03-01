package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class PlayerCatalogUiState(
    val sourceCount: Int = 0,
    val sources: PersistentList<ExtractorLink> = persistentListOf(),
    val currentSourceIndex: Int = -1,
    val subtitles: PersistentList<SubtitleData> = persistentListOf(),
    val selectedSubtitleIndex: Int = -1,
    val selectedAudioTrackIndex: Int = -1,
)
