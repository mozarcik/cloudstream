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
            resolveImmediateHomeSelection(
                availableSources = availableSources,
                savedSourceName = savedSourceName
            )?.let { selectedSource ->
                return HomeInitialSourceSelection(
                    source = selectedSource,
                )
            }

            if (availableSources.isNotEmpty()) {
                if (fallbackSource == null) {
                    fallbackSource = availableSources.firstOrNull()
                }
            }

            delay(pollDelayMs)
        }

        val finalAvailableSources = SourceRepository.getAvailableApis()
        val resolvedSource = resolveImmediateHomeSelection(
            availableSources = finalAvailableSources,
            savedSourceName = savedSourceName
        ) ?: fallbackSource ?: finalAvailableSources.firstOrNull()

        return HomeInitialSourceSelection(
            source = resolvedSource,
        )
    }
}

internal fun resolveImmediateHomeSelection(
    availableSources: List<MainAPI>,
    savedSourceName: String?,
): MainAPI? {
    if (availableSources.isEmpty()) return null
    if (availableSources.size == 1) return availableSources.first()
    if (savedSourceName.isNullOrBlank()) return availableSources.firstOrNull()

    return availableSources.firstOrNull { source ->
        source.name == savedSourceName
    }
}
