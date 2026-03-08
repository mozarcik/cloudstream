package com.lagradost.cloudstream3.tv.data.entities

import androidx.compose.runtime.Immutable

@Immutable
data class Movie(
    val id: String,
    val posterUri: String,
    val name: String,
    val description: String
)

@Immutable
enum class ThumbnailType {
    Standard,
    Long
}
