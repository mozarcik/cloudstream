package com.lagradost.cloudstream3.tv.compat

import android.content.Context
import android.widget.Toast
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.actions.VideoClickActionHolder
import com.lagradost.cloudstream3.ui.result.LinkLoadingResult
import com.lagradost.cloudstream3.utils.DataStore.setKey

internal class MovieDetailsPlaybackActionExecutor(
    private val linksLoader: MovieDetailsLinksLoader,
) {
    private val castActionSupport = MovieDetailsCastActionSupport(
        linksLoader = linksLoader,
    )

    suspend fun reloadEpisodeLinks(target: MovieDetailsActionTarget): Boolean {
        val links = loadLinks(
            target = target,
            sourceTypes = com.lagradost.cloudstream3.ui.player.LOADTYPE_INAPP,
            clearCache = true,
        )
        return links.links.isNotEmpty()
    }

    suspend fun castEpisode(target: MovieDetailsActionTarget): MovieDetailsCompatActionOutcome {
        return castActionSupport.castEpisode(target)
    }

    suspend fun castMirror(
        target: MovieDetailsActionTarget,
        context: Context?,
    ): MovieDetailsCompatActionOutcome {
        return castActionSupport.castMirror(
            target = target,
            context = context,
        )
    }

    suspend fun executeExternalAction(
        actionId: Int,
        target: MovieDetailsActionTarget,
        context: Context?,
    ): MovieDetailsCompatActionOutcome {
        val action = VideoClickActionHolder.getActionById(actionId)
            ?: return MovieDetailsCompatActionOutcome.Completed

        CommonActivity.activity?.setKey("last_click_action", action.uniqueId())

        if (action.oneSource) {
            val loaded = loadLinks(
                target = target,
                sourceTypes = action.sourceTypes,
            )
            if (loaded.links.isEmpty()) {
                showToast(R.string.no_links_found_toast)
                return MovieDetailsCompatActionOutcome.Completed
            }

            if (loaded.links.size == 1) {
                action.runActionSafe(CommonActivity.activity, target.episode, loaded, 0)
                return MovieDetailsCompatActionOutcome.Completed
            }

            val options = buildMovieDetailsSourceSelectionPanelItems(
                links = loaded.links,
                itemKeyPrefix = "external_source_$actionId",
            )

            return MovieDetailsCompatActionOutcome.OpenSelection(
                request = MovieDetailsCompatSelectionRequest(
                    title = action.name.asStringNull(context) ?: action.name.toString(),
                    options = options,
                    onOptionSelected = { selectedIndex ->
                        action.runActionSafe(
                            CommonActivity.activity,
                            target.episode,
                            loaded,
                            selectedIndex.coerceIn(loaded.links.indices),
                        )
                        MovieDetailsCompatActionOutcome.Completed
                    },
                )
            )
        }

        val loaded = loadLinks(
            target = target,
            sourceTypes = action.sourceTypes,
        )
        if (loaded.links.isEmpty()) {
            showToast(R.string.no_links_found_toast)
            return MovieDetailsCompatActionOutcome.Completed
        }

        action.runActionSafe(CommonActivity.activity, target.episode, loaded, null)
        return MovieDetailsCompatActionOutcome.Completed
    }

    private suspend fun loadLinks(
        target: MovieDetailsActionTarget,
        sourceTypes: Set<com.lagradost.cloudstream3.utils.ExtractorLinkType>,
        clearCache: Boolean = false,
        isCasting: Boolean = false,
    ): LinkLoadingResult {
        return linksLoader.load(
            target = target,
            sourceTypes = sourceTypes,
            clearCache = clearCache,
            isCasting = isCasting,
        )
    }

    private fun showToast(@androidx.annotation.StringRes textRes: Int) {
        CommonActivity.showToast(textRes, Toast.LENGTH_SHORT)
    }
}
