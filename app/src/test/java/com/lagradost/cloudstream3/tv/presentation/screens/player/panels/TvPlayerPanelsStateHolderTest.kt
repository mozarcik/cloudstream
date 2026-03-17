package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import com.lagradost.cloudstream3.tv.presentation.screens.player.TvPlayerSubtitleSelectionSource
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.player.SubtitleOrigin
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import org.junit.Assert.assertEquals
import org.junit.Test

class TvPlayerPanelsStateHolderTest {
    @Test
    fun `post-ready reconciliation prefers currently selected forced embedded subtitle`() {
        val stateHolder = TvPlayerPanelsStateHolder()
        val regularEnglish = embeddedSubtitle(
            url = "embedded_english",
            nameSuffix = "Regular",
        )
        val forcedEnglish = embeddedSubtitle(
            url = "embedded_forced",
            nameSuffix = "Forced",
        )
        val subtitles = listOf(regularEnglish, forcedEnglish)

        stateHolder.onPlaybackReady()
        stateHolder.onEmbeddedSubtitlesChanged(runtimeSelectedSubtitleId = forcedEnglish.getId())
        stateHolder.applyPreferredSubtitleAutoSelection(subtitles)

        val selection = stateHolder.selection(
            currentLink = testLink(),
            subtitles = subtitles,
        )

        assertEquals(forcedEnglish.getId(), selection.selectedSubtitleId)
        assertEquals(TvPlayerSubtitleSelectionSource.Auto, selection.subtitleSelectionSource)
    }

    private fun embeddedSubtitle(
        url: String,
        nameSuffix: String,
    ): SubtitleData {
        return SubtitleData(
            originalName = "English",
            nameSuffix = nameSuffix,
            url = url,
            origin = SubtitleOrigin.EMBEDDED_IN_VIDEO,
            mimeType = "text/vtt",
            headers = emptyMap(),
            languageCode = "en",
        )
    }

    private fun testLink(): ExtractorLink {
        return ExtractorLink(
            source = "Test",
            name = "Test source",
            url = "https://video.test/stream.m3u8",
            referer = "",
            quality = 0,
            headers = emptyMap(),
            extractorData = null,
            type = ExtractorLinkType.M3U8,
            audioTracks = emptyList(),
        )
    }
}
