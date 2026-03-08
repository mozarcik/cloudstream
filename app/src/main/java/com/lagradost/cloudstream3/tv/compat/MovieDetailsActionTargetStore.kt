package com.lagradost.cloudstream3.tv.compat

import com.lagradost.cloudstream3.tv.util.tvTraceSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class MovieDetailsActionTargetStore {
    private var cachedTarget: MovieDetailsActionTarget? = null
    private val targetMutex = Mutex()

    suspend fun resolve(
        targetResolver: MovieDetailsActionTargetResolver,
        preferredSeason: Int?,
        preferredEpisode: Int?,
    ): MovieDetailsActionTarget? {
        val baseTarget = resolveBaseTarget(targetResolver) ?: return null
        return targetResolver.selectPreferredTarget(
            baseTarget = baseTarget,
            preferredSeason = preferredSeason,
            preferredEpisode = preferredEpisode,
        )
    }

    private suspend fun resolveBaseTarget(
        targetResolver: MovieDetailsActionTargetResolver,
    ): MovieDetailsActionTarget? {
        cachedTarget?.let { return it }

        return targetMutex.withLock {
            cachedTarget?.let { return it }

            val target = tvTraceSection("details_action_target_build") {
                withContext(Dispatchers.IO) {
                    targetResolver.buildBaseTarget()
                }
            }

            cachedTarget = target
            target
        }
    }
}
