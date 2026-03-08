package com.lagradost.cloudstream3.tv.compat

import android.content.Context
import android.widget.Toast
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.CommonActivity.getCastSession
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.ui.player.LOADTYPE_CHROMECAST
import com.lagradost.cloudstream3.ui.result.LinkLoadingResult
import com.lagradost.cloudstream3.ui.result.getRealPosition
import com.lagradost.cloudstream3.utils.CastHelper.startCast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class MovieDetailsCastActionSupport(
    private val linksLoader: MovieDetailsLinksLoader,
) {
    suspend fun castEpisode(target: MovieDetailsActionTarget): MovieDetailsCompatActionOutcome {
        val loaded = loadLinks(target)
        if (loaded.links.isEmpty()) {
            showToast(R.string.no_links_found_toast)
            return MovieDetailsCompatActionOutcome.Completed
        }
        startChromecast(target = target, loaded = loaded, startIndex = 0)
        return MovieDetailsCompatActionOutcome.Completed
    }

    suspend fun castMirror(
        target: MovieDetailsActionTarget,
        context: Context?,
    ): MovieDetailsCompatActionOutcome {
        val loaded = loadLinks(target)
        if (loaded.links.isEmpty()) {
            showToast(R.string.no_links_found_toast)
            return MovieDetailsCompatActionOutcome.Completed
        }

        if (loaded.links.size == 1) {
            startChromecast(target = target, loaded = loaded, startIndex = 0)
            return MovieDetailsCompatActionOutcome.Completed
        }

        val options = buildMovieDetailsSourceSelectionPanelItems(
            links = loaded.links,
            itemKeyPrefix = "cast_source",
        )
        val title = context?.getString(R.string.episode_action_chromecast_mirror)
            ?: "Chromecast mirror"

        return MovieDetailsCompatActionOutcome.OpenSelection(
            request = MovieDetailsCompatSelectionRequest(
                title = title,
                options = options,
                onOptionSelected = { selectedIndex ->
                    startChromecast(
                        target = target,
                        loaded = loaded,
                        startIndex = selectedIndex,
                    )
                    MovieDetailsCompatActionOutcome.Completed
                },
            )
        )
    }

    private suspend fun startChromecast(
        target: MovieDetailsActionTarget,
        loaded: LinkLoadingResult,
        startIndex: Int,
    ) {
        withContext(Dispatchers.Main) {
            val activity = CommonActivity.activity ?: return@withContext
            activity.getCastSession()?.startCast(
                apiName = target.loadResponse.apiName,
                isMovie = target.loadResponse.type in setOf(TvType.Movie, TvType.AnimeMovie),
                title = target.loadResponse.name,
                poster = target.loadResponse.posterUrl,
                currentEpisodeIndex = target.episode.index,
                episodes = listOf(target.episode),
                currentLinks = loaded.links,
                subtitles = loaded.subs,
                startIndex = startIndex.coerceIn(loaded.links.indices),
                startTime = target.episode.getRealPosition(),
            )
        }
    }

    private suspend fun loadLinks(
        target: MovieDetailsActionTarget,
    ): LinkLoadingResult {
        return linksLoader.load(
            target = target,
            sourceTypes = LOADTYPE_CHROMECAST,
            isCasting = true,
        )
    }

    private fun showToast(@androidx.annotation.StringRes textRes: Int) {
        CommonActivity.showToast(textRes, Toast.LENGTH_SHORT)
    }
}
