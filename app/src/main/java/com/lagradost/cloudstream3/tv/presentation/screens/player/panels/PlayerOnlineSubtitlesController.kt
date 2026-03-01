package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.subtitles.AbstractSubtitleEntities.SubtitleSearch
import com.lagradost.cloudstream3.syncproviders.AccountManager.Companion.subtitleProviders
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.ui.result.ResultEpisode
import com.lagradost.cloudstream3.ui.subtitles.SubtitlesFragment.Companion.getAutoSelectLanguageTagIETF
import kotlinx.coroutines.CoroutineScope

internal class PlayerOnlineSubtitlesController(
    private val coroutineScope: CoroutineScope,
    private val stringResolver: (Int, String) -> String,
    private val defaultQueryProvider: () -> String,
    private val createSearchRequest: (String, String?) -> SubtitleSearch,
    private val onVisibleUiRefreshRequested: () -> Unit,
    private val onSubtitlesDownloaded: (List<SubtitleData>) -> Unit,
) {
    private var navigationState = PlayerOnlineSubtitlesNavigationState()
    private val subtitleLanguageOptions = buildSubtitleLanguageOptions()

    var state: TvPlayerOnlineSubtitlesState = createDefaultOnlineSubtitlesState(
        query = "",
        options = subtitleLanguageOptions,
    )
        private set

    private val loader = PlayerOnlineSubtitlesLoader(
        coroutineScope = coroutineScope,
        stringResolver = stringResolver,
        createSearchRequest = createSearchRequest,
        stateProvider = { state },
        stateUpdater = { updatedState ->
            state = updatedState
        },
        onVisibleUiRefreshRequested = onVisibleUiRefreshRequested,
        onSubtitlesDownloaded = { downloadedSubtitles ->
            resetNavigation()
            onSubtitlesDownloaded(downloadedSubtitles)
        },
    )

    fun reset(query: String = "") {
        loader.reset()
        navigationState = PlayerOnlineSubtitlesNavigationState()
        state = createDefaultOnlineSubtitlesState(
            query = query,
            options = subtitleLanguageOptions,
        )
    }

    fun resetNavigation() {
        navigationState = PlayerOnlineSubtitlesNavigationState()
    }

    fun hasOnlineSubtitleProviders(): Boolean {
        return subtitleProviders.isNotEmpty()
    }

    fun canLoadFirstAvailableSubtitle(): Boolean {
        return hasOnlineSubtitleProviders() && defaultQueryProvider().isNotBlank()
    }

    fun openOnlineSubtitlesPanel(): Boolean {
        ensureQueryInitialized()
        navigationState = PlayerOnlineSubtitlesNavigationState(
            screen = TvPlayerSubtitlePanelScreen.OnlineSearch,
            direction = TvPlayerSubtitlePanelNavigationDirection.Forward,
        )
        if (state.query.isBlank()) {
            state = state.copy(
                status = TvPlayerOnlineSubtitlesStatus.Idle,
                results = kotlinx.collections.immutable.persistentListOf(),
                errorMessage = null,
            )
        } else {
            loader.scheduleSearch(immediate = true)
        }
        return true
    }

    fun openOnlineSubtitlesLanguagePanel(): Boolean {
        if (navigationState.screen != TvPlayerSubtitlePanelScreen.OnlineSearch) {
            return false
        }
        navigationState = PlayerOnlineSubtitlesNavigationState(
            screen = TvPlayerSubtitlePanelScreen.OnlineLanguageSelection,
            direction = TvPlayerSubtitlePanelNavigationDirection.Forward,
        )
        return true
    }

    fun navigateBack(): Boolean {
        return when (navigationState.screen) {
            TvPlayerSubtitlePanelScreen.Main -> false
            TvPlayerSubtitlePanelScreen.OnlineLanguageSelection -> {
                navigationState = PlayerOnlineSubtitlesNavigationState(
                    screen = TvPlayerSubtitlePanelScreen.OnlineSearch,
                    direction = TvPlayerSubtitlePanelNavigationDirection.Backward,
                    focusOnlineSearchItemId = SubtitleOnlineLanguageItemId,
                )
                true
            }
            TvPlayerSubtitlePanelScreen.OnlineSearch -> {
                navigationState = PlayerOnlineSubtitlesNavigationState(
                    screen = TvPlayerSubtitlePanelScreen.Main,
                    direction = TvPlayerSubtitlePanelNavigationDirection.Backward,
                    focusInternetEntryWhenMainVisible = true,
                )
                true
            }
        }
    }

    fun onQueryUpdated(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery == state.query) return

        loader.onQueryUpdated(
            query = normalizedQuery,
            shouldSearchImmediately = navigationState.screen == TvPlayerSubtitlePanelScreen.OnlineSearch,
        )
    }

    fun selectLanguageAndReturnToSearch(languageTag: String): Boolean {
        val normalizedTag = languageTag.trim().ifBlank { getAutoSelectLanguageTagIETF() }
        if (!normalizedTag.equals(state.selectedLanguageTag, ignoreCase = true)) {
            state = state.copy(
                selectedLanguageTag = normalizedTag,
                selectedLanguageLabel = subtitleLanguageLabel(
                    languageTag = normalizedTag,
                    options = subtitleLanguageOptions,
                ),
                status = when {
                    state.query.isBlank() -> TvPlayerOnlineSubtitlesStatus.Idle
                    else -> TvPlayerOnlineSubtitlesStatus.Loading
                },
                errorMessage = null,
                results = if (state.query.isBlank()) {
                    kotlinx.collections.immutable.persistentListOf()
                } else {
                    state.results
                },
            )
        }

        val shouldSearch = navigationState.screen == TvPlayerSubtitlePanelScreen.OnlineSearch ||
            navigationState.screen == TvPlayerSubtitlePanelScreen.OnlineLanguageSelection
        if (shouldSearch) {
            loader.scheduleSearch(immediate = true)
        }

        return if (navigationState.screen == TvPlayerSubtitlePanelScreen.OnlineLanguageSelection) {
            navigationState = PlayerOnlineSubtitlesNavigationState(
                screen = TvPlayerSubtitlePanelScreen.OnlineSearch,
                direction = TvPlayerSubtitlePanelNavigationDirection.Backward,
                focusOnlineSearchItemId = SubtitleOnlineLanguageItemId,
            )
            true
        } else {
            onVisibleUiRefreshRequested()
            false
        }
    }

    fun retrySearch() {
        loader.scheduleSearch(immediate = true)
    }

    fun selectOnlineSubtitleResult(resultId: String) {
        loader.selectOnlineSubtitleResult(resultId)
    }

    fun loadFirstAvailableSubtitle() {
        loader.loadFirstAvailableSubtitle(defaultQueryProvider())
    }

    fun buildPanelContent(): PlayerOnlineSubtitlesPanelContent {
        val content = buildOnlineSubtitlesPanelContent(
            state = state,
            navigationState = navigationState,
            subtitleLanguageOptions = subtitleLanguageOptions,
            stringResolver = stringResolver,
        )
        navigationState = consumeOnlineSubtitlesNavigationFocusState(navigationState)
        return content
    }

    private fun ensureQueryInitialized() {
        if (state.query.isNotBlank()) return
        state = state.copy(
            query = defaultQueryProvider(),
        )
    }

}
