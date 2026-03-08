package com.lagradost.cloudstream3.tv.compat

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.actions.VideoClickActionHolder
import com.lagradost.cloudstream3.ui.result.ACTION_CHROME_CAST_EPISODE
import com.lagradost.cloudstream3.ui.result.ACTION_CHROME_CAST_MIRROR
import com.lagradost.cloudstream3.ui.result.ACTION_DOWNLOAD_EPISODE
import com.lagradost.cloudstream3.ui.result.ACTION_DOWNLOAD_EPISODE_SUBTITLE_MIRROR
import com.lagradost.cloudstream3.ui.result.ACTION_DOWNLOAD_MIRROR
import com.lagradost.cloudstream3.ui.result.ACTION_MARK_AS_WATCHED
import com.lagradost.cloudstream3.ui.result.ACTION_MARK_WATCHED_UP_TO_THIS_EPISODE
import com.lagradost.cloudstream3.ui.result.ACTION_PLAY_EPISODE_IN_PLAYER
import com.lagradost.cloudstream3.ui.result.ACTION_RELOAD_EPISODE
import com.lagradost.cloudstream3.utils.AppContextUtils.isConnectedToChromecast

internal class MovieDetailsPanelActionsFactory(
    private val watchStateSupport: MovieDetailsWatchStateSupport,
) {
    fun build(
        context: Context?,
        target: MovieDetailsActionTarget,
    ): List<MovieDetailsCompatPanelItem> {
        val actions = mutableListOf<MovieDetailsCompatPanelItem>()

        if (context?.isConnectedToChromecast() == true) {
            actions += item(
                id = ACTION_CHROME_CAST_EPISODE,
                labelRes = R.string.episode_action_chromecast_episode,
                iconRes = R.drawable.ic_baseline_tv_24,
                context = context,
            )
            actions += item(
                id = ACTION_CHROME_CAST_MIRROR,
                labelRes = R.string.episode_action_chromecast_mirror,
                iconRes = R.drawable.ic_baseline_tv_24,
                context = context,
            )
        }

        actions += item(
            id = ACTION_PLAY_EPISODE_IN_PLAYER,
            labelRes = R.string.episode_action_play_in_app,
            iconRes = R.drawable.ic_baseline_play_arrow_24,
            context = context,
        )
        actions += item(
            id = ACTION_DOWNLOAD_EPISODE,
            labelRes = R.string.episode_action_auto_download,
            iconRes = R.drawable.baseline_downloading_24,
            context = context,
        )
        actions += item(
            id = ACTION_DOWNLOAD_MIRROR,
            labelRes = R.string.episode_action_download_mirror,
            iconRes = R.drawable.baseline_downloading_24,
            context = context,
        )
        actions += item(
            id = ACTION_DOWNLOAD_EPISODE_SUBTITLE_MIRROR,
            labelRes = R.string.episode_action_download_subtitle,
            iconRes = R.drawable.ic_baseline_subtitles_24,
            context = context,
        )
        actions += item(
            id = ACTION_RELOAD_EPISODE,
            labelRes = R.string.episode_action_reload_links,
            iconRes = R.drawable.ic_refresh,
            context = context,
        )

        val externalActions = VideoClickActionHolder.makeOptionMap(CommonActivity.activity, target.episode)
        actions += externalActions.map { (name, actionId) ->
            MovieDetailsCompatPanelItem(
                id = actionId,
                label = name.asStringNull(context) ?: name.toString(),
                iconRes = R.drawable.ic_baseline_open_in_new_24,
            )
        }

        if (target.episode.tvType !in setOf(TvType.Movie, TvType.AnimeMovie)) {
            val isWatched = watchStateSupport.isEpisodeWatched(target)
            actions += item(
                id = ACTION_MARK_AS_WATCHED,
                labelRes = if (isWatched) {
                    R.string.action_remove_from_watched
                } else {
                    R.string.action_mark_as_watched
                },
                iconRes = R.drawable.ic_baseline_check_24,
                context = context,
            )
            actions += item(
                id = ACTION_MARK_WATCHED_UP_TO_THIS_EPISODE,
                labelRes = if (isWatched) {
                    R.string.action_remove_mark_watched_up_to_this_episode
                } else {
                    R.string.action_mark_watched_up_to_this_episode
                },
                iconRes = R.drawable.baseline_list_alt_24,
                context = context,
            )
        }

        return actions
    }

    private fun item(
        id: Int,
        @StringRes labelRes: Int,
        @DrawableRes iconRes: Int,
        context: Context?,
    ): MovieDetailsCompatPanelItem {
        return MovieDetailsCompatPanelItem(
            id = id,
            label = context?.getString(labelRes) ?: labelRes.toString(),
            iconRes = iconRes,
        )
    }
}
