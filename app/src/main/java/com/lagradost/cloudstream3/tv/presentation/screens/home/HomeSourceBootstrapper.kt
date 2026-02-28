package com.lagradost.cloudstream3.tv.presentation.screens.home

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.tv.compat.home.HomeSourceSelectionRepository
import com.lagradost.cloudstream3.tv.compat.home.SourceRepository
import kotlinx.coroutines.delay

data class HomeInitialSourceSelection(
    val source: MainAPI?,
)

class HomeSourceBootstrapper(
    private val sourceSelectionRepository: HomeSourceSelectionRepository,
    private val pollAttempts: Int = 50,
    private val pollDelayMs: Long = 100L,
) {
    suspend fun awaitInitialSelection(): HomeInitialSourceSelection {
        val savedSourceName = sourceSelectionRepository.getSelectedSourceName()
        var fallbackSource: MainAPI? = null

        repeat(pollAttempts) {
            val availableSources = SourceRepository.getAvailableApis()
            if (availableSources.isNotEmpty()) {
                if (savedSourceName.isNullOrBlank()) {
                    return HomeInitialSourceSelection(
                        source = availableSources.firstOrNull(),
                    )
                }

                availableSources.firstOrNull { source ->
                    source.name == savedSourceName
                }?.let { matchedSource ->
                    return HomeInitialSourceSelection(
                        source = matchedSource,
                    )
                }

                if (fallbackSource == null) {
                    fallbackSource = availableSources.firstOrNull()
                }
            }

            delay(pollDelayMs)
        }

        val finalAvailableSources = SourceRepository.getAvailableApis()
        val resolvedSource = when {
            savedSourceName.isNullOrBlank() -> {
                finalAvailableSources.firstOrNull()
            }

            else -> {
                finalAvailableSources.firstOrNull { source ->
                    source.name == savedSourceName
                } ?: fallbackSource ?: finalAvailableSources.firstOrNull()
            }
        }

        return HomeInitialSourceSelection(
            source = resolvedSource,
        )
    }
}
