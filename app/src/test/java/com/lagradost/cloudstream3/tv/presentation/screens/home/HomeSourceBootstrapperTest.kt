package com.lagradost.cloudstream3.tv.presentation.screens.home

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.TvType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeSourceBootstrapperTest {
    @Test
    fun `resolveImmediateHomeSelection returns single available source even when saved source differs`() {
        val onlySource = testSource("Local")

        val selectedSource = resolveImmediateHomeSelection(
            availableSources = listOf(onlySource),
            savedSourceName = "Stare Zrodlo"
        )

        assertEquals(onlySource, selectedSource)
    }

    @Test
    fun `resolveImmediateHomeSelection returns saved source when multiple sources are available`() {
        val local = testSource("Local")
        val simkl = testSource("Simkl")

        val selectedSource = resolveImmediateHomeSelection(
            availableSources = listOf(local, simkl),
            savedSourceName = "Simkl"
        )

        assertEquals(simkl, selectedSource)
    }

    @Test
    fun `resolveImmediateHomeSelection returns null when saved source is unavailable among many sources`() {
        val local = testSource("Local")
        val simkl = testSource("Simkl")

        val selectedSource = resolveImmediateHomeSelection(
            availableSources = listOf(local, simkl),
            savedSourceName = "Trakt"
        )

        assertNull(selectedSource)
    }

    private fun testSource(name: String): MainAPI {
        return object : MainAPI() {
            override var name = name
            override val supportedTypes = emptySet<TvType>()
            override var lang = ""
            override val hasMainPage = true
        }
    }
}
