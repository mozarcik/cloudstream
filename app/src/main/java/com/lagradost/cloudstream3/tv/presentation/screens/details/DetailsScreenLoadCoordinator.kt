package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.repositories.MovieRepository

@Immutable
internal data class DetailsPrimaryStageResult(
    val details: MovieDetails,
    val loadResponse: LoadResponse,
    val actionsCompat: MovieDetailsEpisodeActionsCompat,
)

@Immutable
internal data class DetailsSecondaryStageResult(
    val details: MovieDetails,
    val actionsCompat: MovieDetailsEpisodeActionsCompat,
)

internal sealed interface DetailsScreenLoadOutcome {
    data class Success(
        val secondary: DetailsSecondaryStageResult,
    ) : DetailsScreenLoadOutcome

    data class SecondaryFailure(
        val primary: DetailsPrimaryStageResult,
        val error: Throwable,
    ) : DetailsScreenLoadOutcome
}

internal class DetailsScreenLoadCoordinator(
    private val sessionRepository: DetailsSessionRepository,
) {
    constructor(repository: MovieRepository) : this(
        sessionRepository = DefaultDetailsSessionRepository(repository)
    )

    suspend fun load(
        url: String,
        apiName: String,
        onPrimaryLoaded: (DetailsPrimaryStageResult) -> Unit,
    ): DetailsScreenLoadOutcome {
        val session = sessionRepository.openSession(url = url, apiName = apiName)
        val primary = session.loadPrimaryStage()
        onPrimaryLoaded(primary)

        return try {
            DetailsScreenLoadOutcome.Success(
                secondary = session.loadSecondaryStage(primary)
            )
        } catch (error: Throwable) {
            DetailsScreenLoadOutcome.SecondaryFailure(
                primary = primary,
                error = error,
            )
        }
    }

    suspend fun loadPrimary(
        url: String,
        apiName: String,
    ): DetailsPrimaryStageResult {
        return sessionRepository
            .openSession(url = url, apiName = apiName)
            .loadPrimaryStage()
    }

    suspend fun loadSecondary(
        url: String,
        apiName: String,
        primary: DetailsPrimaryStageResult,
    ): DetailsSecondaryStageResult {
        return sessionRepository
            .openSession(url = url, apiName = apiName)
            .loadSecondaryStage(primary)
    }
}

internal fun buildDetailsActionsCompat(
    loadResponse: LoadResponse,
    details: MovieDetails,
): MovieDetailsEpisodeActionsCompat {
    return MovieDetailsEpisodeActionsCompat(
        loadResponse = loadResponse,
        preferredSeason = details.currentSeason,
        preferredEpisode = details.currentEpisode,
    )
}
