package com.lagradost.cloudstream3.tv.data.entities

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.NextAiring
import com.lagradost.cloudstream3.ProviderType
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.ShowStatus
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.VPNStatus

@Immutable
data class MovieDetails(
    val id: String,
    val isFavorite: Boolean = false,
    val isBookmarked: Boolean = false,
    val bookmarkLabelRes: Int? = null,
    val name: String,
    val description: String,
    val posterUri: String,
    val backdropUri: String = "",
    val seasons: List<TvSeason> = emptyList(),
    val seasonCount: Int? = null,
    val episodeCount: Int? = null,
    val currentSeason: Int? = null,
    val currentEpisode: Int? = null,
    // Optional fields (may not be available from all providers)
    val cast: List<MovieCast> = emptyList(),
    val similarMovies: MovieList = emptyList(),
    val videoUri: String = "",
    val subtitleUri: String? = null,
    val providerName: String = "",
    val type: TvType? = null,
    val score: Score? = null,
    val showStatus: ShowStatus? = null,
    val nextAiring: NextAiring? = null,
    val comingSoon: Boolean = false,
    val logoUri: String? = null,
    val originalTitle: String? = null,
    val posterHeaders: Map<String, String> = emptyMap(),
    val providerType: ProviderType = ProviderType.DirectProvider,
    val vpnStatus: VPNStatus = VPNStatus.None,
    val pgRating: String = "",
    val releaseDate: String = "",
    val categories: List<String> = emptyList(),
    val duration: String = "",
    val director: String = "",
    val screenplay: String = "",
    val music: String = "",
    val status: String = "",
    val originalLanguage: String = "",
    val budget: String = "",
    val revenue: String = "",
)
