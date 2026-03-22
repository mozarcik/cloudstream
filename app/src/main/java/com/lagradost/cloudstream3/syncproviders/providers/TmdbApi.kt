package com.lagradost.cloudstream3.syncproviders.providers

import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.syncproviders.AccountManager.Companion.APP_STRING
import com.lagradost.cloudstream3.syncproviders.AuthData
import com.lagradost.cloudstream3.syncproviders.AuthLoginPage
import com.lagradost.cloudstream3.syncproviders.AuthPinData
import com.lagradost.cloudstream3.syncproviders.AuthToken
import com.lagradost.cloudstream3.syncproviders.AuthUser
import com.lagradost.cloudstream3.syncproviders.SyncAPI
import com.lagradost.cloudstream3.syncproviders.SyncIdName
import com.lagradost.cloudstream3.ui.library.ListSorting
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.txt
import java.text.SimpleDateFormat
import java.net.URI
import java.net.URLDecoder
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.runBlocking

internal const val TMDB_PROVIDER_NAME = "TMDB"
private const val TMDB_API_URL = "https://api.themoviedb.org"
private const val TMDB_REQUEST_TOKEN_EXPIRATION_SECONDS = 15 * 60
private const val TMDB_POLL_INTERVAL_SECONDS = 5
private const val TMDB_LOG_TAG = "TMDB"
private const val TMDB_CACHED_LIBRARY = "tmdb_cached_library"

internal val TMDB_SUPPORTED_LIST_SORTING = setOf(
    ListSorting.AlphabeticalA,
    ListSorting.AlphabeticalZ,
    ListSorting.ReleaseDateNew,
    ListSorting.ReleaseDateOld,
    ListSorting.RatingHigh,
    ListSorting.RatingLow,
)

class TmdbApi : SyncAPI() {
    override val name = TMDB_PROVIDER_NAME
    override val idPrefix = "tmdb"
    override val redirectUrlIdentifier = "tmdb"
    override val icon = R.drawable.tmdb_logo
    override val hasOAuth2 = true
    override val hasPin = true
    override val mainUrl = TMDB_API_URL
    override val createAccountUrl = "https://www.themoviedb.org/signup"
    override val syncIdName = SyncIdName.Tmdb
    override var requireLibraryRefresh = true

    override suspend fun updateStatus(
        auth: AuthData?,
        id: String,
        newStatus: AbstractSyncStatus
    ): Boolean = false

    override suspend fun status(auth: AuthData?, id: String): AbstractSyncStatus? = null

    override suspend fun load(auth: AuthData?, id: String): SyncResult? = null

    override suspend fun search(auth: AuthData?, query: String): List<SyncSearchResult>? = null

    override suspend fun library(auth: AuthData?): LibraryMetadata? {
        val authData = auth ?: return null
        val token = authData.token
        val payload = token.readPayload() ?: return null
        val accessToken = token.accessToken ?: return null

        val cachedLibrary = getTmdbLibrarySmart(
            auth = authData,
            payload = payload,
            accessToken = accessToken,
        ) ?: return null

        return buildTmdbLibraryMetadata(cachedLibrary)
    }

    override fun urlToId(url: String): String? {
        return Regex("""themoviedb\.org/(movie|tv)/(\d+)""")
            .find(url)
            ?.groupValues
            ?.getOrNull(2)
    }

    override fun loginRequest(): AuthLoginPage? {
        ensureAppAuthConfig()
        Log.i(TMDB_LOG_TAG, "loginRequest: creating browser login request token")

        val requestToken = runCatching {
            runBlocking {
                createRequestToken(
                    redirectTo = buildAppRedirectUrl()
                )
            }
        }.getOrElse { throwable ->
            Log.w(TMDB_LOG_TAG, "loginRequest: request token creation failed", throwable)
            throw ErrorLoadingException(
                throwable.message ?: "Nie udało się rozpocząć logowania TMDB."
            )
        } ?: throw ErrorLoadingException("Nie udało się rozpocząć logowania TMDB.")

        Log.i(
            TMDB_LOG_TAG,
            "loginRequest: browser login request token ready, redirect=${buildAppRedirectUrl()}"
        )
        return AuthLoginPage(
            url = buildVerificationUrl(requestToken),
            payload = requestToken,
        )
    }

    override suspend fun pinRequest(): AuthPinData? {
        ensureAppAuthConfig()
        Log.i(TMDB_LOG_TAG, "pinRequest: creating TV request token")

        val requestToken = createRequestToken(
            redirectTo = "https://www.themoviedb.org/"
        ) ?: return null

        Log.i(TMDB_LOG_TAG, "pinRequest: request token created for TV flow")
        return AuthPinData(
            deviceCode = requestToken,
            userCode = "SCAN QR",
            verificationUrl = buildVerificationUrl(requestToken),
            expiresIn = TMDB_REQUEST_TOKEN_EXPIRATION_SECONDS,
            interval = TMDB_POLL_INTERVAL_SECONDS
        )
    }

    override suspend fun login(payload: AuthPinData): AuthToken? {
        ensureAppAuthConfig()
        Log.i(TMDB_LOG_TAG, "login(pin): polling approved request token")

        val requestToken = payload.deviceCode.trim()
        if (requestToken.isBlank()) {
            Log.w(TMDB_LOG_TAG, "login(pin): missing request token in payload")
            return null
        }

        return completeLogin(requestToken)
    }

    override suspend fun login(redirectUrl: String, payload: String?): AuthToken? {
        ensureAppAuthConfig()
        Log.i(TMDB_LOG_TAG, "login(callback): handling redirect=$redirectUrl")

        val requestToken = resolveRedirectRequestToken(
            redirectUrl = redirectUrl,
            payload = payload,
        ) ?: return null

        Log.i(TMDB_LOG_TAG, "login(callback): resolved request token from redirect or payload")
        return completeLogin(requestToken)
    }

    internal fun resolveRedirectRequestToken(
        redirectUrl: String,
        payload: String?,
    ): String? {
        val redirectPayload = parseRedirectPayload(redirectUrl)
        Log.i(
            TMDB_LOG_TAG,
            "resolveRedirectRequestToken: approved=${redirectPayload["approved"]} denied=${redirectPayload["denied"]} hasQueryToken=${!redirectPayload["request_token"].isNullOrBlank()} hasPayloadToken=${!payload.isNullOrBlank()}"
        )
        if (redirectPayload["denied"] == "true") {
            throw ErrorLoadingException("Logowanie TMDB zostało anulowane.")
        }

        return redirectPayload["request_token"]
            ?.takeIf { it.isNotBlank() }
            ?: payload?.takeIf { it.isNotBlank() }
    }

    private fun parseRedirectPayload(redirectUrl: String): Map<String, String> {
        val query = runCatching { URI(redirectUrl).rawQuery }.getOrNull().orEmpty()
        if (query.isBlank()) {
            return emptyMap()
        }

        return query.split("&")
            .mapNotNull { pair ->
                val idx = pair.indexOf("=")
                if (idx <= 0) {
                    return@mapNotNull null
                }

                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                key to value
            }
            .toMap()
    }

    private suspend fun completeLogin(requestToken: String): AuthToken? {
        Log.i(TMDB_LOG_TAG, "completeLogin: exchanging approved request token for user access token")
        val accessTokenResponse = runCatching {
            app.post(
                url = "$TMDB_API_URL/4/auth/access_token",
                headers = appAuthHeaders(),
                json = TmdbV4AccessTokenRequest(requestToken = requestToken)
            ).parsedSafe<TmdbV4AccessTokenResponse>()
        }.onFailure { throwable ->
            Log.w(TMDB_LOG_TAG, "completeLogin: access token exchange failed", throwable)
        }.getOrNull() ?: return null

        if (accessTokenResponse.success != true) {
            Log.w(TMDB_LOG_TAG, "completeLogin: access token exchange returned success=false")
            return null
        }

        val accessToken = accessTokenResponse.accessToken?.takeIf { it.isNotBlank() } ?: return null
        val accountObjectId = accessTokenResponse.accountIdOrObjectId?.takeIf { it.isNotBlank() } ?: return null
        Log.i(
            TMDB_LOG_TAG,
            "completeLogin: access token ready, accountObjectIdPresent=${accountObjectId.isNotBlank()}"
        )

        val sessionId = createV3Session(accessToken) ?: return null
        Log.i(TMDB_LOG_TAG, "completeLogin: v3 session created successfully")

        val account = loadAccountDetails(sessionId) ?: return null
        Log.i(
            TMDB_LOG_TAG,
            "completeLogin: account details loaded, username=${account.username}"
        )

        return AuthToken(
            accessToken = accessToken,
            payload = TmdbAuthPayload(
                sessionId = sessionId,
                accountId = account.id,
                accountObjectId = accountObjectId,
                displayName = account.displayName,
                username = account.username,
                profilePicture = account.profilePicture,
            ).toJson()
        )
    }

    private suspend fun createRequestToken(redirectTo: String): String? {
        Log.i(TMDB_LOG_TAG, "createRequestToken: requesting token for redirect=$redirectTo")
        val response = app.post(
            url = "$TMDB_API_URL/4/auth/request_token",
            headers = appAuthHeaders(),
            json = TmdbV4RequestTokenRequest(redirectTo = redirectTo)
        ).parsedSafe<TmdbV4RequestTokenResponse>() ?: return null

        if (!response.success || response.requestToken.isBlank()) {
            Log.w(TMDB_LOG_TAG, "createRequestToken: request token response invalid")
            return null
        }

        Log.i(TMDB_LOG_TAG, "createRequestToken: token created successfully")
        return response.requestToken
    }

    override suspend fun user(token: AuthToken?): AuthUser? {
        val payload = token?.readPayload() ?: return null
        return AuthUser(
            id = payload.accountId,
            name = payload.displayName ?: payload.username,
            profilePicture = payload.profilePicture,
        )
    }

    private suspend fun loadCombinedCollection(
        accountObjectId: String,
        accessToken: String,
        collectionType: TmdbCollectionType,
    ): List<LibraryItem> {
        val movieItems = loadMediaItems(
            url = "$TMDB_API_URL/4/account/$accountObjectId/movie/${collectionType.pathSegment}",
            accessToken = accessToken,
            fallbackType = TvType.Movie,
        )
        val tvItems = loadMediaItems(
            url = "$TMDB_API_URL/4/account/$accountObjectId/tv/${collectionType.pathSegment}",
            accessToken = accessToken,
            fallbackType = TvType.TvSeries,
        )
        return movieItems + tvItems
    }

    private suspend fun loadCustomLists(
        accountObjectId: String,
        accessToken: String,
    ): List<TmdbCachedLibraryList> {
        val summaries = loadPagedResults { page ->
            app.get(
                url = "$TMDB_API_URL/4/account/$accountObjectId/lists",
                headers = userAuthHeaders(accessToken),
                params = mapOf("page" to page.toString())
            ).parsedSafe<TmdbListPageResponse>()
        }

        return summaries.map { summary ->
            val items = loadMediaItems(
                url = "$TMDB_API_URL/4/list/${summary.id}",
                accessToken = accessToken,
                fallbackType = null,
            )
            TmdbCachedLibraryList(
                name = summary.name,
                items = items,
            )
        }
    }

    private suspend fun getTmdbLibrarySmart(
        auth: AuthData,
        payload: TmdbAuthPayload,
        accessToken: String,
    ): TmdbCachedLibrary? {
        return resolveTmdbLibraryCache(
            requireLibraryRefresh = requireLibraryRefresh,
            readCachedLibrary = {
                getKey<TmdbCachedLibrary>(TMDB_CACHED_LIBRARY, auth.user.id.toString())
            },
            loadFreshLibrary = {
                loadFreshTmdbLibrary(
                    accountObjectId = payload.accountObjectId,
                    accessToken = accessToken,
                )
            },
            storeLibrary = { library ->
                setKey(TMDB_CACHED_LIBRARY, auth.user.id.toString(), library)
            }
        )
    }

    private suspend fun loadFreshTmdbLibrary(
        accountObjectId: String,
        accessToken: String,
    ): TmdbCachedLibrary {
        val favorites = loadCombinedCollection(
            accountObjectId = accountObjectId,
            accessToken = accessToken,
            collectionType = TmdbCollectionType.Favorites
        )
        val watchlist = loadCombinedCollection(
            accountObjectId = accountObjectId,
            accessToken = accessToken,
            collectionType = TmdbCollectionType.Watchlist
        )
        val customLists = loadCustomLists(
            accountObjectId = accountObjectId,
            accessToken = accessToken
        )

        return TmdbCachedLibrary(
            favorites = favorites,
            watchlist = watchlist,
            customLists = customLists,
        )
    }

    private suspend fun loadMediaItems(
        url: String,
        accessToken: String,
        fallbackType: TvType?,
    ): List<LibraryItem> {
        return loadPagedResults { page ->
            app.get(
                url = url,
                headers = userAuthHeaders(accessToken),
                params = mapOf(
                    "language" to Locale.getDefault().toLanguageTag(),
                    "page" to page.toString(),
                )
            ).parsedSafe<TmdbMediaPageResponse>()
        }.mapNotNull { summary ->
            tmdbMediaSummaryToLibraryItem(
                summary = summary,
                fallbackType = fallbackType,
                apiName = name,
            )
        }
    }

    private suspend fun <T> loadPagedResults(
        loadPage: suspend (page: Int) -> TmdbPagedResponse<T>?,
    ): List<T> {
        val results = mutableListOf<T>()
        var page = 1
        var totalPages = 1

        while (page <= totalPages) {
            val response = loadPage(page) ?: throw ErrorLoadingException("Unable to load TMDB list data.")
            results += response.results
            totalPages = response.totalPages.coerceAtLeast(1)
            page += 1
        }

        return results
    }

    private suspend fun createV3Session(accessToken: String): String? {
        Log.i(TMDB_LOG_TAG, "createV3Session: converting v4 user token to v3 session")
        val response = runCatching {
            app.post(
                url = "$TMDB_API_URL/3/authentication/session/convert/4",
                headers = userAuthHeaders(accessToken),
                params = v3QueryParams(),
                json = TmdbV3SessionConvertRequest(accessToken = accessToken)
            ).parsedSafe<TmdbV3SessionConvertResponse>()
        }.onFailure { throwable ->
            Log.w(TMDB_LOG_TAG, "createV3Session: conversion request failed", throwable)
        }.getOrNull() ?: return null

        val sessionId = response
            .takeIf { it.success }
            ?.sessionId
            ?.takeIf { it.isNotBlank() }
        if (sessionId == null) {
            Log.w(TMDB_LOG_TAG, "createV3Session: conversion response missing session id or success=false")
        }
        return sessionId
    }

    private suspend fun loadAccountDetails(sessionId: String): TmdbAccountDetails? {
        Log.i(TMDB_LOG_TAG, "loadAccountDetails: fetching profile with v3 session")
        val response = runCatching {
            app.get(
                url = "$TMDB_API_URL/3/account",
                headers = appAuthHeaders(),
                params = v3QueryParams() + mapOf("session_id" to sessionId)
            ).parsedSafe<TmdbAccountDetailsResponse>()
        }.onFailure { throwable ->
            Log.w(TMDB_LOG_TAG, "loadAccountDetails: account details request failed", throwable)
        }.getOrNull() ?: return null

        return response.toDomain()
    }

    private fun ensureAppAuthConfig() {
        if (BuildConfig.TMDB_READ_ACCESS_TOKEN.isBlank()) {
            throw ErrorLoadingException("Brakuje TMDB_READ_ACCESS_TOKEN w local.properties albo w zmiennych środowiskowych.")
        }
        if (BuildConfig.TMDB_API_KEY.isBlank()) {
            throw ErrorLoadingException("Brakuje TMDB_API_KEY w local.properties albo w zmiennych środowiskowych.")
        }
    }

    private fun AuthToken.readPayload(): TmdbAuthPayload? {
        return tryParseJson(payload)
    }

    private fun appAuthHeaders(): Map<String, String> {
        return mapOf(
            "Accept" to "application/json",
            "Authorization" to "Bearer ${BuildConfig.TMDB_READ_ACCESS_TOKEN}"
        )
    }

    private fun userAuthHeaders(accessToken: String): Map<String, String> {
        return mapOf(
            "Accept" to "application/json",
            "Authorization" to "Bearer $accessToken"
        )
    }

    private fun v3QueryParams(): Map<String, String> {
        return mapOf("api_key" to BuildConfig.TMDB_API_KEY)
    }

    private fun buildVerificationUrl(requestToken: String): String {
        return "https://www.themoviedb.org/auth/access?request_token=$requestToken"
    }

    private fun buildAppRedirectUrl(): String {
        return "$APP_STRING://$redirectUrlIdentifier"
    }
}

internal suspend fun resolveTmdbLibraryCache(
    requireLibraryRefresh: Boolean,
    readCachedLibrary: () -> TmdbCachedLibrary?,
    loadFreshLibrary: suspend () -> TmdbCachedLibrary?,
    storeLibrary: (TmdbCachedLibrary) -> Unit,
): TmdbCachedLibrary? {
    return if (requireLibraryRefresh) {
        val freshLibrary = loadFreshLibrary()
        if (freshLibrary != null) {
            storeLibrary(freshLibrary)
        }
        freshLibrary
    } else {
        readCachedLibrary()
    }
}

internal fun buildTmdbLibraryMetadata(cachedLibrary: TmdbCachedLibrary): SyncAPI.LibraryMetadata {
    return SyncAPI.LibraryMetadata(
        allLibraryLists = buildList {
            add(SyncAPI.LibraryList(txt(R.string.favorites_list_name), cachedLibrary.favorites))
            add(SyncAPI.LibraryList(txt(R.string.watchlist_list_name), cachedLibrary.watchlist))
            addAll(cachedLibrary.customLists.map { list ->
                SyncAPI.LibraryList(
                    name = txt(list.name),
                    items = list.items,
                )
            })
        },
        supportedListSorting = TMDB_SUPPORTED_LIST_SORTING,
    )
}

internal fun tmdbMediaSummaryToLibraryItem(
    summary: TmdbMediaSummary,
    fallbackType: TvType?,
    apiName: String,
): SyncAPI.LibraryItem? {
    val resolvedType = when (summary.mediaType ?: fallbackType?.toTmdbMediaType()) {
        "movie" -> TvType.Movie
        "tv" -> TvType.TvSeries
        else -> fallbackType ?: return null
    }
    val title = summary.title?.takeIf { it.isNotBlank() }
        ?: summary.name?.takeIf { it.isNotBlank() }
        ?: return null

    return SyncAPI.LibraryItem(
        name = title,
        url = buildTmdbUrl(resolvedType, summary.id),
        syncId = "${resolvedType.toTmdbMediaType()}:${summary.id}",
        episodesCompleted = null,
        episodesTotal = summary.numberOfEpisodes,
        personalRating = summary.rating?.let { Score.from10(it) },
        lastUpdatedUnixTime = null,
        apiName = apiName,
        type = resolvedType,
        posterUrl = tmdbImageUrl(summary.posterPath),
        posterHeaders = null,
        quality = null,
        releaseDate = parseTmdbReleaseDate(summary.releaseDate ?: summary.firstAirDate),
        id = summary.id,
        plot = summary.overview.takeIf { it.isNotBlank() },
        score = summary.voteAverage?.let { Score.from10(it) },
        backgroundPosterUrl = tmdbImageUrl(summary.backdropPath, size = "original"),
    )
}

internal fun buildTmdbUrl(type: TvType, id: Int): String {
    val pathSegment = when (type) {
        TvType.Movie,
        TvType.AnimeMovie -> "movie"
        else -> "tv"
    }
    return "https://www.themoviedb.org/$pathSegment/$id"
}

internal fun tmdbImageUrl(
    path: String?,
    size: String = "w500",
): String? {
    return path?.takeIf { it.isNotBlank() }?.let {
        "https://image.tmdb.org/t/p/$size$it"
    }
}

internal fun parseTmdbReleaseDate(value: String?): Date? {
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(value ?: return null)
    } catch (_: Throwable) {
        null
    }
}

private fun TvType.toTmdbMediaType(): String = when (this) {
    TvType.Movie,
    TvType.AnimeMovie -> "movie"
    else -> "tv"
}

private data class TmdbAuthPayload(
    @JsonProperty("sessionId") val sessionId: String,
    @JsonProperty("accountId") val accountId: Int,
    @JsonProperty("accountObjectId") val accountObjectId: String,
    @JsonProperty("displayName") val displayName: String?,
    @JsonProperty("username") val username: String,
    @JsonProperty("profilePicture") val profilePicture: String?,
)

private data class TmdbV4RequestTokenRequest(
    @JsonProperty("redirect_to") val redirectTo: String,
)

private data class TmdbV4RequestTokenResponse(
    @JsonProperty("success") val success: Boolean = false,
    @JsonProperty("request_token") val requestToken: String = "",
)

private data class TmdbV4AccessTokenRequest(
    @JsonProperty("request_token") val requestToken: String,
)

private data class TmdbV4AccessTokenResponse(
    @JsonProperty("success") val success: Boolean? = null,
    @JsonProperty("access_token") val accessToken: String? = null,
    @JsonProperty("account_id") val accountId: String? = null,
    @JsonProperty("account_object_id") val accountObjectId: String? = null,
) {
    val accountIdOrObjectId: String?
        get() = accountId ?: accountObjectId
}

private data class TmdbV3SessionConvertRequest(
    @JsonProperty("access_token") val accessToken: String,
)

private data class TmdbV3SessionConvertResponse(
    @JsonProperty("success") val success: Boolean = false,
    @JsonProperty("session_id") val sessionId: String = "",
)

private interface TmdbPagedResponse<T> {
    val totalPages: Int
    val results: List<T>
}

internal data class TmdbCachedLibrary(
    @JsonProperty("favorites") val favorites: List<SyncAPI.LibraryItem> = emptyList(),
    @JsonProperty("watchlist") val watchlist: List<SyncAPI.LibraryItem> = emptyList(),
    @JsonProperty("customLists") val customLists: List<TmdbCachedLibraryList> = emptyList(),
)

internal data class TmdbCachedLibraryList(
    @JsonProperty("name") val name: String,
    @JsonProperty("items") val items: List<SyncAPI.LibraryItem> = emptyList(),
)

internal data class TmdbMediaSummary(
    @JsonProperty("id") val id: Int,
    @JsonProperty("media_type") val mediaType: String? = null,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("overview") val overview: String = "",
    @JsonProperty("poster_path") val posterPath: String? = null,
    @JsonProperty("backdrop_path") val backdropPath: String? = null,
    @JsonProperty("vote_average") val voteAverage: Double? = null,
    @JsonProperty("release_date") val releaseDate: String? = null,
    @JsonProperty("first_air_date") val firstAirDate: String? = null,
    @JsonProperty("rating") val rating: Double? = null,
    @JsonProperty("number_of_episodes") val numberOfEpisodes: Int? = null,
)

private data class TmdbMediaPageResponse(
    @JsonProperty("page") val page: Int = 1,
    @JsonProperty("total_pages") override val totalPages: Int = 1,
    @JsonProperty("total_results") val totalResults: Int = 0,
    @JsonProperty("results") override val results: List<TmdbMediaSummary> = emptyList(),
) : TmdbPagedResponse<TmdbMediaSummary>

private data class TmdbListPageResponse(
    @JsonProperty("page") val page: Int = 1,
    @JsonProperty("total_pages") override val totalPages: Int = 1,
    @JsonProperty("total_results") val totalResults: Int = 0,
    @JsonProperty("results") override val results: List<TmdbListSummary> = emptyList(),
) : TmdbPagedResponse<TmdbListSummary>

private data class TmdbListSummary(
    @JsonProperty("id") val id: Int,
    @JsonProperty("name") val name: String,
)

private data class TmdbAccountDetailsResponse(
    @JsonProperty("id") val id: Int,
    @JsonProperty("username") val username: String,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("avatar") val avatar: TmdbAvatarContainer? = null,
) {
    fun toDomain(): TmdbAccountDetails {
        return TmdbAccountDetails(
            id = id,
            username = username,
            displayName = name?.takeIf { it.isNotBlank() } ?: username,
            profilePicture = avatar?.tmdb?.avatarPath?.let { path ->
                if (path.startsWith("http")) {
                    path
                } else {
                    "https://image.tmdb.org/t/p/w185$path"
                }
            }
        )
    }
}

private data class TmdbAvatarContainer(
    @JsonProperty("tmdb") val tmdb: TmdbAvatarPath? = null,
)

private data class TmdbAvatarPath(
    @JsonProperty("avatar_path") val avatarPath: String? = null,
)

private data class TmdbAccountDetails(
    val id: Int,
    val username: String,
    val displayName: String?,
    val profilePicture: String?,
)

private enum class TmdbCollectionType(val pathSegment: String) {
    Favorites("favorites"),
    Watchlist("watchlist"),
}
