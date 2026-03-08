package com.lagradost.cloudstream3.tv.compat

import android.content.Context
import android.util.Log
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.ui.result.LinkLoadingResult

internal fun buildMovieDetailsDownloadMirrorSelectionRequest(
    target: MovieDetailsActionTarget,
    context: Context?,
    loaded: LinkLoadingResult,
    downloadLauncher: MovieDetailsDownloadLauncher,
): MovieDetailsCompatSelectionRequest {
    val options = buildMovieDetailsSourceSelectionPanelItems(
        links = loaded.links,
        itemKeyPrefix = "download_source",
    )
    val title = context?.getString(R.string.episode_action_download_mirror)
        ?: "Download mirror"

    return MovieDetailsCompatSelectionRequest(
        title = title,
        options = options,
        onOptionSelected = { selectedIndex ->
            Log.d(
                MovieDetailsDownloadSupport.DebugTag,
                "download mirror selected index=$selectedIndex episodeId=${target.episode.id}"
            )
            downloadLauncher.startMirrorDownload(
                target = target,
                loaded = loaded,
                selectedIndex = selectedIndex,
                context = context,
            )
            MovieDetailsCompatActionOutcome.Completed
        },
    )
}

internal fun buildMovieDetailsSubtitleSelectionRequest(
    target: MovieDetailsActionTarget,
    context: Context?,
    loaded: LinkLoadingResult,
    downloadLauncher: MovieDetailsDownloadLauncher,
): MovieDetailsCompatSelectionRequest {
    val options = loaded.subs.mapIndexed { index, subtitle ->
        MovieDetailsCompatPanelItem(
            id = index,
            label = subtitle.name,
            iconRes = R.drawable.ic_baseline_subtitles_24,
        )
    }
    val title = context?.getString(R.string.episode_action_download_subtitle)
        ?: "Download subtitles"

    return MovieDetailsCompatSelectionRequest(
        title = title,
        options = options,
        onOptionSelected = { selectedIndex ->
            val clampedIndex = selectedIndex.coerceIn(loaded.subs.indices)
            downloadLauncher.startSubtitleDownload(target, loaded.subs[clampedIndex])
            MovieDetailsCompatActionOutcome.Completed
        },
    )
}
