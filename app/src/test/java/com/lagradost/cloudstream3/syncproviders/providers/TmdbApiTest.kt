package com.lagradost.cloudstream3.syncproviders.providers

import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.utils.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import kotlinx.coroutines.runBlocking

class TmdbApiTest {
    @Test
    fun `maps movie summary to library item`() {
        val item = tmdbMediaSummaryToLibraryItem(
            summary = TmdbMediaSummary(
                id = 550,
                title = "Fight Club",
                overview = "desc",
                posterPath = "/poster.jpg",
                backdropPath = "/backdrop.jpg",
                voteAverage = 8.4,
                releaseDate = "1999-10-15",
            ),
            fallbackType = TvType.Movie,
            apiName = TMDB_PROVIDER_NAME,
        )

        assertNotNull(item)
        assertEquals("Fight Club", item?.name)
        assertEquals(TvType.Movie, item?.type)
        assertEquals("https://www.themoviedb.org/movie/550", item?.url)
        assertEquals("movie:550", item?.syncId)
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", item?.posterUrl)
        assertEquals("https://image.tmdb.org/t/p/original/backdrop.jpg", item?.backgroundPosterUrl)
        assertEquals(1999, Calendar.getInstance().apply { time = item?.releaseDate!! }.get(Calendar.YEAR))
    }

    @Test
    fun `maps tv summary to library item`() {
        val item = tmdbMediaSummaryToLibraryItem(
            summary = TmdbMediaSummary(
                id = 1399,
                name = "Game of Thrones",
                firstAirDate = "2011-04-17",
                mediaType = "tv",
                numberOfEpisodes = 73,
            ),
            fallbackType = null,
            apiName = TMDB_PROVIDER_NAME,
        )

        assertNotNull(item)
        assertEquals(TvType.TvSeries, item?.type)
        assertEquals("tv:1399", item?.syncId)
        assertEquals(73, item?.episodesTotal)
        assertEquals("https://www.themoviedb.org/tv/1399", item?.url)
    }

    @Test
    fun `returns null when summary has no title`() {
        val item = tmdbMediaSummaryToLibraryItem(
            summary = TmdbMediaSummary(
                id = 1,
                mediaType = "movie",
            ),
            fallbackType = TvType.Movie,
            apiName = TMDB_PROVIDER_NAME,
        )

        assertNull(item)
    }

    @Test
    fun `builds tmdb url for tv title`() {
        assertEquals(
            "https://www.themoviedb.org/tv/1399",
            buildTmdbUrl(TvType.TvSeries, 1399)
        )
    }

    @Test
    fun `redirect request token prefers callback query token`() {
        val api = TmdbApi()

        val requestToken = api.run {
            resolveRedirectRequestToken(
                redirectUrl = "cloudstreamapp://tmdb?approved=true&request_token=callback-token",
                payload = "payload-token",
            )
        }

        assertEquals("callback-token", requestToken)
    }

    @Test
    fun `redirect request token falls back to payload`() {
        val api = TmdbApi()

        val requestToken = api.run {
            resolveRedirectRequestToken(
                redirectUrl = "cloudstreamapp://tmdb",
                payload = "payload-token",
            )
        }

        assertEquals("payload-token", requestToken)
    }

    @Test
    fun `builds library metadata from cached tmdb data`() {
        val favorite = createLibraryItem(
            name = "Fight Club",
            id = 550,
            type = TvType.Movie,
        )
        val watchlist = createLibraryItem(
            name = "Game of Thrones",
            id = 1399,
            type = TvType.TvSeries,
        )
        val custom = createLibraryItem(
            name = "The Bear",
            id = 100088,
            type = TvType.TvSeries,
        )

        val metadata = buildTmdbLibraryMetadata(
            TmdbCachedLibrary(
                favorites = listOf(favorite),
                watchlist = listOf(watchlist),
                customLists = listOf(
                    TmdbCachedLibraryList(
                        name = "Moja lista",
                        items = listOf(custom),
                    )
                )
            )
        )

        assertEquals(3, metadata.allLibraryLists.size)
        assertTrue(metadata.allLibraryLists[0].name is UiText.StringResource)
        assertTrue(metadata.allLibraryLists[1].name is UiText.StringResource)
        assertEquals(UiText.DynamicString("Moja lista"), metadata.allLibraryLists[2].name)
        assertEquals(listOf(favorite), metadata.allLibraryLists[0].items)
        assertEquals(listOf(watchlist), metadata.allLibraryLists[1].items)
        assertEquals(listOf(custom), metadata.allLibraryLists[2].items)
        assertEquals(TMDB_SUPPORTED_LIST_SORTING, metadata.supportedListSorting)
    }

    @Test
    fun `uses cached tmdb library when refresh is not required`() = runBlocking {
        val cachedLibrary = TmdbCachedLibrary(
            favorites = listOf(
                createLibraryItem(
                    name = "Fight Club",
                    id = 550,
                    type = TvType.Movie,
                )
            )
        )
        var freshLoadCount = 0
        var storedLibrary: TmdbCachedLibrary? = null

        val resolvedLibrary = resolveTmdbLibraryCache(
            requireLibraryRefresh = false,
            readCachedLibrary = { cachedLibrary },
            loadFreshLibrary = {
                freshLoadCount += 1
                null
            },
            storeLibrary = { storedLibrary = it }
        )

        assertSame(cachedLibrary, resolvedLibrary)
        assertEquals(0, freshLoadCount)
        assertNull(storedLibrary)
    }

    @Test
    fun `loads and stores fresh tmdb library when refresh is required`() = runBlocking {
        val freshLibrary = TmdbCachedLibrary(
            watchlist = listOf(
                createLibraryItem(
                    name = "The Bear",
                    id = 100088,
                    type = TvType.TvSeries,
                )
            )
        )
        var freshLoadCount = 0
        var storedLibrary: TmdbCachedLibrary? = null

        val resolvedLibrary = resolveTmdbLibraryCache(
            requireLibraryRefresh = true,
            readCachedLibrary = { null },
            loadFreshLibrary = {
                freshLoadCount += 1
                freshLibrary
            },
            storeLibrary = { storedLibrary = it }
        )

        assertSame(freshLibrary, resolvedLibrary)
        assertEquals(1, freshLoadCount)
        assertSame(freshLibrary, storedLibrary)
    }

    private fun createLibraryItem(
        name: String,
        id: Int,
        type: TvType,
    ): SyncAPI.LibraryItem {
        return SyncAPI.LibraryItem(
            name = name,
            url = buildTmdbUrl(type, id),
            syncId = "${typeToMediaType(type)}:$id",
            episodesCompleted = null,
            episodesTotal = null,
            personalRating = null,
            lastUpdatedUnixTime = null,
            apiName = TMDB_PROVIDER_NAME,
            type = type,
            posterUrl = null,
            posterHeaders = null,
            quality = null,
            releaseDate = null,
            id = id,
        )
    }

    private fun typeToMediaType(type: TvType): String {
        return when (type) {
            TvType.Movie,
            TvType.AnimeMovie -> "movie"
            else -> "tv"
        }
    }
}
