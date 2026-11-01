package com.lagradost.cloudstream3.tv.presentation.screens.player.catalog

import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.player.SubtitleOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerCatalogStoreTest {
    @Test
    fun `replaceEmbeddedSubtitles only replaces embedded entries`() {
        val store = PlayerCatalogStore()
        val externalSubtitle = subtitle(
            originalName = "English",
            nameSuffix = "URL",
            url = "https://subs.test/en.vtt",
            origin = SubtitleOrigin.URL,
        )
        val downloadedSubtitle = subtitle(
            originalName = "Polish",
            nameSuffix = "File",
            url = "file:///storage/emulated/0/Download/pl.srt",
            origin = SubtitleOrigin.DOWNLOADED_FILE,
        )
        val oldEmbeddedSubtitle = subtitle(
            originalName = "English",
            nameSuffix = "Old forced",
            url = "embedded_old",
            origin = SubtitleOrigin.EMBEDDED_IN_VIDEO,
        )
        val newEmbeddedSubtitle = subtitle(
            originalName = "English",
            nameSuffix = "Forced",
            url = "embedded_forced",
            origin = SubtitleOrigin.EMBEDDED_IN_VIDEO,
        )

        store.insertSubtitle(externalSubtitle)
        store.insertSubtitle(downloadedSubtitle)
        store.insertSubtitle(oldEmbeddedSubtitle)

        val changed = store.replaceEmbeddedSubtitles(listOf(newEmbeddedSubtitle))
        store.refreshOrderedSubtitles()

        assertTrue(changed)
        assertEquals(
            setOf(
                externalSubtitle.getId(),
                downloadedSubtitle.getId(),
                newEmbeddedSubtitle.getId(),
            ),
            store.orderedSubtitles.map(SubtitleData::getId).toSet(),
        )
    }

    private fun subtitle(
        originalName: String,
        nameSuffix: String,
        url: String,
        origin: SubtitleOrigin,
    ): SubtitleData {
        return SubtitleData(
            originalName = originalName,
            nameSuffix = nameSuffix,
            url = url,
            origin = origin,
            mimeType = "text/vtt",
            headers = emptyMap(),
            languageCode = if (originalName == "Polish") "pl" else "en",
        )
    }
}
