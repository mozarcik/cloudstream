package com.lagradost.cloudstream3.tv.compat.home

/**
 * Reprezentuje jedną kategorię feedu (np. "Trending", "Top 10").
 * Mapowane z HomePageList.
 */
data class FeedCategory(
    val id: String,                    // Unique ID dla kategorii
    val name: String,                  // Display name
    val mainPageRequest: MainPageRequest?, // Request data dla pagination
)

data class MainPageRequest(
    val data: String,
    val name: String,
    val horizontalImages: Boolean = false, // Default to vertical poster images
)
