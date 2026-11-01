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
import com.lagradost.cloudstream3.actions.VideoClickAction
import com.lagradost.cloudstream3.actions.temp.CopyClipboardAction
import com.lagradost.cloudstream3.actions.temp.PlayMirrorAction

internal class MovieDetailsPanelActionsFactory(
    private val watchStateSupport: MovieDetailsWatchStateSupport,
) {
    fun build(
        context: Context?,
        target: MovieDetailsActionTarget,
    ): List<MovieDetailsCompatPanelItem> {
        val play = mutableListOf<MovieDetailsCompatPanelItem>()
        if (context?.isConnectedToChromecast() == true) {
            play += item(ACTION_CHROME_CAST_EPISODE, R.string.episode_action_chromecast_episode, R.drawable.ic_baseline_tv_24, context)
            play += item(ACTION_CHROME_CAST_MIRROR, R.string.episode_action_chromecast_mirror, R.drawable.ic_baseline_tv_24, context)
        }
        play += item(ACTION_PLAY_EPISODE_IN_PLAYER, R.string.episode_action_play_in_app, R.drawable.ic_baseline_play_arrow_24, context)
        play += externalPlayActions(context, target)

        val progress = mutableListOf<MovieDetailsCompatPanelItem>()
        if (target.episode.tvType !in setOf(TvType.Movie, TvType.AnimeMovie)) {
            val isWatched = watchStateSupport.isEpisodeWatched(target)
            progress += item(
                id = ACTION_MARK_AS_WATCHED,
                labelRes = if (isWatched) R.string.action_remove_from_watched else R.string.action_mark_as_watched,
                iconRes = R.drawable.ic_baseline_check_24,
                context = context,
            )
            progress += item(
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

        val download = listOf(
            item(ACTION_DOWNLOAD_EPISODE, R.string.episode_action_auto_download, R.drawable.baseline_downloading_24, context),
            item(ACTION_DOWNLOAD_MIRROR, R.string.episode_action_download_mirror, R.drawable.baseline_downloading_24, context),
            item(ACTION_DOWNLOAD_EPISODE_SUBTITLE_MIRROR, R.string.episode_action_download_subtitle, R.drawable.ic_baseline_subtitles_24, context),
        )
        val other = listOf(
            item(ACTION_RELOAD_EPISODE, R.string.episode_action_reload_links, R.drawable.ic_refresh, context),
        )

        // Same groups as the mockup: Play / Progress / Download / Other.
        return buildList {
            addGroup(context, R.string.tv_details_menu_play, "play", play)
            addGroup(context, R.string.tv_details_menu_progress, "progress", progress)
            addGroup(context, R.string.download, "download", download)
            addGroup(context, R.string.tv_details_menu_other, "other", other)
        }
    }

    /** Providers' "Play in ..." / "Copy link" actions; they follow the in-app play action. */
    private fun externalPlayActions(
        context: Context?,
        target: MovieDetailsActionTarget,
    ): List<MovieDetailsCompatPanelItem> {
        val externalActions = VideoClickActionHolder.makeOptionMap(CommonActivity.activity, target.episode)
        return externalActions.map { (name, actionId) ->
            MovieDetailsCompatPanelItem(
                id = actionId,
                label = name.asStringNull(context) ?: name.toString(),
                iconRes = externalActionIcon(VideoClickActionHolder.getActionById(actionId)),
            )
        }
    }

    /** Mockup: Browser and m3u8 use the external-link icon, Copy link a copy icon, mirror a server icon. */
    @DrawableRes
    private fun externalActionIcon(action: VideoClickAction?): Int = when (action) {
        is CopyClipboardAction -> R.drawable.copy_all_24px
        is PlayMirrorAction -> R.drawable.ic_baseline_dns_24
        else -> R.drawable.ic_baseline_open_in_new_24
    }

    private fun MutableList<MovieDetailsCompatPanelItem>.addGroup(
        context: Context?,
        @StringRes headerRes: Int,
        keySuffix: String,
        items: List<MovieDetailsCompatPanelItem>,
    ) {
        if (items.isEmpty()) return
        add(
            MovieDetailsCompatPanelItem(
                id = NonInteractiveHeaderId,
                label = context?.getString(headerRes) ?: headerRes.toString(),
                key = "movie_action_group_$keySuffix",
                isSectionHeader = true,
            )
        )
        addAll(items)
    }

    private companion object {
        const val NonInteractiveHeaderId = Int.MIN_VALUE
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
