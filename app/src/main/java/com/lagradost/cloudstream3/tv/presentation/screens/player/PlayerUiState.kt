package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.utils.ExtractorLink

@Immutable
data class TvPlayerMetadata(
    val title: String,
    val subtitle: String,
    val backdropUri: String?,
    val year: Int? = null,
    val apiName: String = "",
    val season: Int? = null,
    val episode: Int? = null,
    val episodeTitle: String? = null,
    val isEpisodeBased: Boolean = false,
) {
    companion object {
        val Empty = TvPlayerMetadata(
            title = "",
            subtitle = "",
            backdropUri = null,
        )
    }
}

sealed interface TvPlayerUiState {
    @Immutable
    data class LoadingSources(
        val metadata: TvPlayerMetadata,
        val loadedSources: Int,
        val canSkip: Boolean,
    ) : TvPlayerUiState

    data class Ready(
        val metadata: TvPlayerMetadata,
        val link: ExtractorLink,
        val episodeId: Int = -1,
        val resumePositionMs: Long = 0L,
    ) : TvPlayerUiState

    @Immutable
    data class Error(
        val metadata: TvPlayerMetadata,
        val messageResId: Int,
    ) : TvPlayerUiState
}
