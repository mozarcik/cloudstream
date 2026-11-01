package com.lagradost.cloudstream3.tv.presentation.screens.player.catalog

import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerPreferredLinkTest {
    @Test
    fun `rank prefers same domain, then same source and quality, ignoring name`() {
        val preferred = link("Cinejoy", "Cinejoy - Nebula", 1080, url = "https://nebula.bright67.online/a.m3u8")
            .toPreferredLink()

        assertEquals(0, preferred.matchRank(link("Cinejoy", "other", 1080, url = "https://x.bright67.online/b.m3u8")))
        assertEquals(1, preferred.matchRank(link("Cinejoy", "Cinejoy - Secret", 1080, url = "https://lightspeed.st/c")))
        assertNull(preferred.matchRank(link("Cinejoy", "Cinejoy - Nebula", 720, url = "https://nebula.bright67.online/d")))
        assertNull(preferred.matchRank(link("Vidlink", "Cinejoy - Nebula", 1080, url = "https://nebula.bright67.online/e")))
    }

    @Test
    fun `playback starts from the same domain even when another mirror of the source is higher`() {
        val store = storeWith(
            preferred = link("Cinejoy", "Nebula", 1080, url = "https://nebula.bright67.online/old"),
            link("Top", "Top", 2160, url = "https://t.example/a"),
            link("Cinejoy", "Secret", 1080, url = "https://lightspeed.st/b"),
            link("Cinejoy", "Nebula", 1080, url = "https://nebula.bright67.online/c"),
        )

        assertEquals("https://nebula.bright67.online/c", store.markInitialLinkLoading()?.url)
    }

    @Test
    fun `order stays by quality and playback starts from the preferred link`() {
        val store = storeWith(
            preferred = link("Vidlink", "Vidlink", 720),
            link("DahmerMovies", "D", 2160, url = "a"),
            link("Vidlink", "Vidlink", 720, url = "b"),
            link("Vidlink", "Vidlink", 360, url = "c"),
        )

        assertEquals(listOf("a", "b", "c"), store.orderedLinks.map { it.url })
        assertEquals("b", store.markInitialLinkLoading()?.url)
        assertEquals(1, store.currentLinkIndex)
    }

    @Test
    fun `starts from the top when no link matches`() {
        val store = storeWith(
            preferred = link("Gone", "Gone", 1080),
            link("A", "A", 720, url = "a"),
            link("B", "B", 2160, url = "b"),
        )

        assertEquals("b", store.markInitialLinkLoading()?.url)
        assertEquals(0, store.currentLinkIndex)
    }

    @Test
    fun `failed preferred link falls back to the top and is not retried`() {
        val store = storeWith(
            preferred = link("C", "C", 1080),
            link("A", "A", 2160, url = "a"),
            link("B", "B", 1440, url = "b"),
            link("C", "C", 1080, url = "c"),
            link("D", "D", 720, url = "d"),
        )
        store.markInitialLinkLoading()
        assertEquals(2, store.currentLinkIndex)

        assertEquals(0, store.nextIndexAfterFailure("c"))
        store.setCurrentLinkIndex(1)
        // Sequential fallback from "b" would land on the already failed "c": skip it.
        assertEquals(3, store.nextIndexAfterFailure("b"))
    }

    private fun storeWith(preferred: ExtractorLink, vararg links: ExtractorLink): PlayerCatalogStore {
        val store = PlayerCatalogStore()
        store.preferredLink = preferred.toPreferredLink()
        links.forEach(store::insertLink)
        store.rebuildOrderedData(currentUrl = null)
        return store
    }

    private fun link(source: String, name: String, quality: Int, url: String = "$source-$quality") =
        ExtractorLink(
            source = source,
            name = name,
            url = url,
            referer = "",
            quality = quality,
            type = ExtractorLinkType.VIDEO,
        )
}
