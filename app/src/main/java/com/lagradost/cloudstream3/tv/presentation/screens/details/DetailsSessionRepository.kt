package com.lagradost.cloudstream3.tv.presentation.screens.details

import com.lagradost.cloudstream3.tv.data.repositories.mergeSecondary
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepository

internal interface DetailsSession {
    suspend fun loadPrimaryStage(): DetailsPrimaryStageResult
    suspend fun loadSecondaryStage(primary: DetailsPrimaryStageResult): DetailsSecondaryStageResult
}

internal interface DetailsSessionRepository {
    fun openSession(
        url: String,
        apiName: String,
    ): DetailsSession
}

internal class DefaultDetailsSessionRepository(
    private val repository: MovieRepository,
) : DetailsSessionRepository {
    override fun openSession(
        url: String,
        apiName: String,
    ): DetailsSession {
        return DefaultDetailsSession(
            url = url,
            apiName = apiName,
            repository = repository,
        )
    }
}

private class DefaultDetailsSession(
    private val url: String,
    private val apiName: String,
    private val repository: MovieRepository,
) : DetailsSession {
    private var cachedPrimary: DetailsPrimaryStageResult? = null
    private var cachedSecondary: DetailsSecondaryStageResult? = null

    override suspend fun loadPrimaryStage(): DetailsPrimaryStageResult {
        cachedPrimary?.let { return it }

        val primary = repository.getPrimaryDetails(url = url, apiName = apiName)
        return DetailsPrimaryStageResult(
            details = primary.details,
            loadResponse = primary.loadResponse,
            actionsCompat = buildDetailsActionsCompat(
                loadResponse = primary.loadResponse,
                details = primary.details,
            ),
        ).also { result ->
            cachedPrimary = result
        }
    }

    override suspend fun loadSecondaryStage(primary: DetailsPrimaryStageResult): DetailsSecondaryStageResult {
        cachedSecondary?.let { return it }

        val secondary = repository.getSecondaryDetails(url = url, apiName = apiName)
        val mergedDetails = primary.details.mergeSecondary(secondary)
        return DetailsSecondaryStageResult(
            details = mergedDetails,
            actionsCompat = buildDetailsActionsCompat(
                loadResponse = primary.loadResponse,
                details = mergedDetails,
            ),
        ).also { result ->
            cachedSecondary = result
        }
    }
}
