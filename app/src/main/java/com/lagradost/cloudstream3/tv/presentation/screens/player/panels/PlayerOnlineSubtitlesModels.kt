package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem
import kotlinx.collections.immutable.PersistentList

internal data class PlayerOnlineSubtitlesPanelContent(
    val screen: TvPlayerSubtitlePanelScreen,
    val direction: TvPlayerSubtitlePanelNavigationDirection,
    val items: List<SidePanelMenuItem>,
    val initialFocusedItemId: String?,
    val overrideMainInitialFocusedItemId: String? = null,
)

internal data class PlayerOnlineSubtitlesNavigationState(
    val screen: TvPlayerSubtitlePanelScreen = TvPlayerSubtitlePanelScreen.Main,
    val direction: TvPlayerSubtitlePanelNavigationDirection = TvPlayerSubtitlePanelNavigationDirection.Forward,
    val focusInternetEntryWhenMainVisible: Boolean = false,
    val focusOnlineSearchItemId: String? = null,
)

internal data class PlayerOnlineSubtitleResultPayload(
    val idPrefix: String,
    val name: String,
    val lang: String,
    val data: String,
    val type: TvType,
    val source: String,
    val epNumber: Int?,
    val seasonNumber: Int?,
    val year: Int?,
    val isHearingImpaired: Boolean,
    val headers: Map<String, String>,
) {
    fun toSubtitleEntity(): AbstractSubtitleEntities.SubtitleEntity {
        return AbstractSubtitleEntities.SubtitleEntity(
            idPrefix = idPrefix,
            name = name,
            lang = lang,
            data = data,
            type = type,
            source = source,
            epNumber = epNumber,
            seasonNumber = seasonNumber,
            year = year,
            isHearingImpaired = isHearingImpaired,
            headers = headers,
        )
    }
}

internal data class PlayerOnlineSubtitleSearchSnapshot(
    val results: PersistentList<TvPlayerOnlineSubtitleResult>,
    val payloadsByResultId: Map<String, PlayerOnlineSubtitleResultPayload>,
)
