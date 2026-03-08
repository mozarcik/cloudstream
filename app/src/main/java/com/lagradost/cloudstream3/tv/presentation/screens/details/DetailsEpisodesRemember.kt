package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun rememberDetailsEpisodesStateHolder(
    detailsId: String,
    context: Context,
    actionsCompat: MovieDetailsEpisodeActionsCompat,
    initialSeasonId: String?,
    scope: CoroutineScope,
): DetailsEpisodesStateHolder {
    val appContext = remember(context) { context.applicationContext }
    return remember(detailsId, appContext, actionsCompat) {
        DetailsEpisodesStateHolder(
            scope = scope,
            hydrator = DetailsEpisodesWindowHydrator(
                context = appContext,
                actionsCompat = actionsCompat,
            ),
            initialSeasonId = initialSeasonId,
        )
    }
}
