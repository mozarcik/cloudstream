package com.lagradost.cloudstream3.tv.compat.server

import android.net.Uri
import com.lagradost.cloudstream3.homeserver.HomeServerApi
import com.lagradost.cloudstream3.homeserver.ServerLibraryItem
import com.lagradost.cloudstream3.ui.player.ExtractorUri
import com.lagradost.cloudstream3.ui.player.PlayerSubtitleHelper.Companion.toSubtitleMimeType
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.player.SubtitleOrigin
import com.lagradost.cloudstream3.ui.player.VideoGenerator
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType

/**
 * Plays library items of the home server. Presigned URLs are requested right before playback
 * (they carry their own authorization, so the player needs no headers) and handed to the player
 * through the same [ExtractorUri] path as downloaded files.
 */
class ServerPlaybackGenerator(
    private val items: List<ServerLibraryItem>,
    episodes: List<ExtractorUri>,
    private val api: HomeServerApi,
) : VideoGenerator<ExtractorUri>(episodes) {
    override val hasCache = false
    override val canSkipLoading = false

    override fun getId(index: Int): Int? = videos.getOrNull(index)?.id

    override suspend fun generateLinks(
        clearCache: Boolean,
        sourceTypes: Set<ExtractorLinkType>,
        callback: (Pair<ExtractorLink?, ExtractorUri?>) -> Unit,
        subtitleCallback: (SubtitleData) -> Unit,
        offset: Int,
        isCasting: Boolean,
    ): Boolean {
        val meta = videos.getOrNull(offset) ?: return false
        val item = items.getOrNull(offset) ?: return false
        val playable = runCatching { api.playable(item.id) }.getOrNull() ?: return false
        callback(null to meta.copy(uri = Uri.parse(playable.videoUrl)))
        playable.subtitles.forEach { sub ->
            subtitleCallback(
                SubtitleData(
                    sub.lang,
                    "",
                    sub.url,
                    SubtitleOrigin.URL,
                    sub.url.substringBefore('?').toSubtitleMimeType(),
                    emptyMap(),
                    sub.lang,
                )
            )
        }
        return true
    }
}
