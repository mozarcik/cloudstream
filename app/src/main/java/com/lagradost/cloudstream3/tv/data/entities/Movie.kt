package com.lagradost.cloudstream3.tv.data.entities

data class Movie(
    val id: String,
    val posterUri: String,
    val name: String,
    val description: String
)

enum class ThumbnailType {
    Standard,
    Long
}
