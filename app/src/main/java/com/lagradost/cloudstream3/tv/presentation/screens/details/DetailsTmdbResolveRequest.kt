package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.TvType

@Immutable
internal data class DetailsTmdbResolveRequest(
    val title: String,
    val tmdbId: Int,
    val preferredApiName: String? = null,
    val expectedType: TvType? = null,
)
