package com.lagradost.cloudstream3.tv.data.entities

import androidx.compose.runtime.Immutable

@Immutable
data class MovieCast(
    val id: String,
    val characterName: String,
    val realName: String,
    val avatarUrl: String
)

