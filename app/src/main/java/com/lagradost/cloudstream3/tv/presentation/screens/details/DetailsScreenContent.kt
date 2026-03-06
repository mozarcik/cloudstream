package com.lagradost.cloudstream3.tv.presentation.screens.details

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionEffect
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionEvent
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionStateHolder
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatActionOutcome
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatPanelItem
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatSelectionRequest
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.data.util.StringConstants
import com.lagradost.cloudstream3.tv.presentation.common.PosterMoviesRow
import com.lagradost.cloudstream3.tv.presentation.focus.rememberFocusRequesters
import com.lagradost.cloudstream3.tv.presentation.screens.movies.BookmarkStatusSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.movies.CastAndCrewList
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieActionsSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetails
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsBackdrop
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsDownloadActionState
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsQuickAction
import com.lagradost.cloudstream3.tv.presentation.screens.movies.rememberChildPadding
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.AdditionalInfoSection
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.EpisodeCard
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.EpisodesSectionHeader
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.NoEpisodesRow
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.SeasonSelectorRow
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.SeasonsSectionHeader
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.resolveInitialSeasonId
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerScreenNavigation
import com.lagradost.cloudstream3.ui.WatchType
import com.lagradost.cloudstream3.ui.result.ACTION_MARK_AS_WATCHED
import com.lagradost.cloudstream3.ui.result.ACTION_MARK_WATCHED_UP_TO_THIS_EPISODE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.lagradost.cloudstream3.utils.VideoDownloadManager

private const val DebugTag = "TvDetailsUI"
private const val SkipDownloadLoadingActionId = -10_001
private const val DownloadLinksPrefetchDelayMs = 500L
private const val DownloadSelectionRefreshDelayMs = 900L

@Composable
internal fun DetailsScreenContent(
    mode: DetailsScreenMode,
    details: MovieDetails,
    sourceUrl: String,
    apiName: String,
    goToPlayer: (String?) -> Unit,
    onBackPressed: () -> Unit,
    refreshScreenWithNewItem: (Movie) -> Unit,
    onFavoriteClick: () -> Unit,
    onBookmarkClick: (WatchType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val childPadding = rememberChildPadding()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val downloadButtonViewModel: DetailsDownloadButtonViewModel = viewModel()
    val downloadButtonState by downloadButtonViewModel.uiState.collectAsStateWithLifecycle()
    val seasons = details.seasons
    val isSeriesContent = mode.isSeriesContent(details)
    val hasAdditionalInfo = listOf(
        details.status,
        details.originalLanguage,
        details.budget,
        details.revenue
    ).any { value -> value.isNotBlank() }

    var isActionsPanelVisible by rememberSaveable(details.id, mode) { mutableStateOf(false) }
    var isBookmarkPanelVisible by rememberSaveable(details.id, mode) { mutableStateOf(false) }
    var isActionInProgress by remember { mutableStateOf(false) }
    var isPanelLoading by remember(details.id, mode) { mutableStateOf(false) }
    var panelItems by remember(details.id, mode) { mutableStateOf<List<MovieDetailsCompatPanelItem>>(emptyList()) }
    var panelSelection by remember(details.id, mode) { mutableStateOf<MovieDetailsCompatSelectionRequest?>(null) }
    var selectedSeasonId by rememberSaveable(details.id) {
        mutableStateOf(resolveInitialSeasonId(seasons, details.currentSeason))
    }
    val seasonTabFocusRequesters = rememberFocusRequesters(count = seasons.size)

    val selectedSeason = seasons.firstOrNull { season -> season.id == selectedSeasonId } ?: seasons.firstOrNull()
    val selectedEpisodes = selectedSeason?.episodes.orEmpty()
    val selectedSeasonTabIndex = seasons.indexOfFirst { season -> season.id == selectedSeasonId }
        .takeIf { it >= 0 } ?: 0
    val selectedSeasonFocusRequester = seasonTabFocusRequesters.getOrNull(selectedSeasonTabIndex)
    val defaultActionsCompat = remember(
        sourceUrl,
        apiName,
        details.currentSeason,
        details.currentEpisode
    ) {
        MovieDetailsEpisodeActionsCompat(
            sourceUrl = sourceUrl,
            apiName = apiName,
            preferredSeason = details.currentSeason,
            preferredEpisode = details.currentEpisode,
        )
    }
    val downloadMirrorStateHolder = remember(details.id, coroutineScope) {
        DownloadMirrorSelectionStateHolder(scope = coroutineScope)
    }
    val downloadMirrorState by downloadMirrorStateHolder.uiState.collectAsStateWithLifecycle()
    var episodeDownloadStates by remember(details.id, selectedSeasonId) {
        mutableStateOf<Map<String, DetailsDownloadButtonUiState>>(emptyMap())
    }
    var episodeWatchedStates by remember(details.id, selectedSeasonId) {
        mutableStateOf<Map<String, Boolean>>(emptyMap())
    }

    fun closePanel() {
        panelSelection = null
        isActionsPanelVisible = false
    }

    fun closeBookmarkPanel() {
        isBookmarkPanelVisible = false
    }

    fun closeDownloadPanel() {
        Log.d(DebugTag, "close download panel mode=$mode")
        downloadMirrorStateHolder.onEvent(DownloadMirrorSelectionEvent.Close)
        downloadButtonViewModel.clearPendingCompat()
        isActionInProgress = false
    }

    fun resolveEpisodeSeason(episode: TvEpisode): Int? {
        return episode.seasonNumber
            ?: selectedSeason?.displaySeasonNumber
            ?: selectedSeason?.seasonNumber
    }

    fun toDownloadUiState(
        snapshotState: DetailsDownloadButtonUiState,
    ): MovieDetailsDownloadActionState {
        return resolveDownloadActionState(
            status = snapshotState.status,
            progressFraction = snapshotState.progressFraction
        )
    }

    fun resolveEpisodeDownloadState(episode: TvEpisode): DetailsDownloadButtonUiState {
        return episodeDownloadStates[episode.id] ?: DetailsDownloadButtonUiState()
    }

    fun resolveEpisodeWatchedState(episode: TvEpisode): Boolean {
        return episodeWatchedStates[episode.id] == true
    }

    suspend fun loadEpisodeWatchedStates(episodes: List<TvEpisode>): Map<String, Boolean> {
        if (episodes.isEmpty()) return emptyMap()

        val episodeTargets = episodes.map { episode ->
            episode to resolveEpisodeSeason(episode)
        }

        return withContext(Dispatchers.IO) {
            episodeTargets.associate { (episode, targetSeason) ->
                val isWatched = defaultActionsCompat.isEpisodeWatched(
                    preferredSeason = targetSeason,
                    preferredEpisode = episode.episodeNumber
                ) == true
                episode.id to isWatched
            }
        }
    }

    fun updateEpisodeDownloadStateByEpisodeId(
        episodeId: Int,
        transform: (DetailsDownloadButtonUiState) -> DetailsDownloadButtonUiState
    ) {
        episodeDownloadStates = episodeDownloadStates.mapValues { (_, state) ->
            if (state.episodeId == episodeId) {
                transform(state)
            } else {
                state
            }
        }
    }

    fun navigatePanelBack() {
        if (panelSelection != null) {
            panelSelection = null
        } else {
            closePanel()
        }
    }

    fun handleActionOutcome(outcome: MovieDetailsCompatActionOutcome) {
        when (outcome) {
            MovieDetailsCompatActionOutcome.Completed -> closePanel()
            is MovieDetailsCompatActionOutcome.OpenSelection -> {
                panelSelection = outcome.request
                isActionsPanelVisible = true
            }
        }
    }

    fun executeAction(actionId: Int) {
        if (!mode.allowsExtendedActions || isActionInProgress || isPanelLoading) return

        coroutineScope.launch {
            isActionInProgress = true
            try {
                val selection = panelSelection
                val outcome = if (selection != null) {
                    selection.onOptionSelected(actionId)
                } else {
                    defaultActionsCompat.execute(
                        actionId = actionId,
                        context = context,
                        onPlayInApp = { goToPlayer(resolveDefaultEpisodeData(details)) },
                    )
                }
                handleActionOutcome(outcome)
            } finally {
                isActionInProgress = false
            }
        }
    }

    fun openActionsPanel() {
        if (!mode.allowsExtendedActions || isPanelLoading || isActionInProgress) return

        closeBookmarkPanel()
        closeDownloadPanel()
        isActionsPanelVisible = true
        panelSelection = null
        panelItems = emptyList()

        coroutineScope.launch {
            isPanelLoading = true
            try {
                val loadedActions = defaultActionsCompat.loadPanelActions(context)
                panelItems = loadedActions

                if (loadedActions.isEmpty()) {
                    closePanel()
                    CommonActivity.showToast(R.string.no_links_found_toast, Toast.LENGTH_SHORT)
                }
            } finally {
                isPanelLoading = false
            }
        }
    }

    fun openBookmarkPanel() {
        if (!mode.allowsBookmark || isPanelLoading || isActionInProgress) return
        closePanel()
        closeDownloadPanel()
        isBookmarkPanelVisible = true
    }

    fun handleDownloadActionOutcome(outcome: MovieDetailsCompatActionOutcome) {
        when (outcome) {
            MovieDetailsCompatActionOutcome.Completed -> {
                Log.d(DebugTag, "download outcome: completed mode=$mode")
                closeDownloadPanel()
            }

            is MovieDetailsCompatActionOutcome.OpenSelection -> {
                Log.d(
                    DebugTag,
                    "download outcome: open selection mode=$mode options=${outcome.request.options.size}"
                )
                downloadMirrorStateHolder.updateSelectionRequest(outcome.request)
            }
        }
    }

    fun executeDownloadSelection(actionId: Int) {
        val selection = downloadMirrorState.selectionRequest ?: return
        if (isActionInProgress) return

        coroutineScope.launch {
            isActionInProgress = true
            downloadButtonViewModel.markPending()
            var shouldClearPendingCompat = false
            try {
                val outcome = withContext(Dispatchers.IO) {
                    selection.onOptionSelected(actionId)
                }
                when (outcome) {
                    MovieDetailsCompatActionOutcome.Completed -> {
                        Log.d(DebugTag, "download outcome: completed mode=$mode")
                        downloadMirrorStateHolder.onEvent(DownloadMirrorSelectionEvent.Close)
                        shouldClearPendingCompat = true
                        downloadButtonViewModel.refreshPendingOrDefaultSnapshot(
                            context = context,
                            reason = "after_source_selected"
                        )
                        delay(DownloadSelectionRefreshDelayMs)
                        downloadButtonViewModel.refreshPendingOrDefaultSnapshot(
                            context = context,
                            reason = "after_source_selected_delayed"
                        )
                    }

                    is MovieDetailsCompatActionOutcome.OpenSelection -> {
                        handleDownloadActionOutcome(outcome)
                    }
                }
            } catch (error: Throwable) {
                Log.e(DebugTag, "download selection failed mode=$mode actionId=$actionId", error)
                downloadButtonViewModel.markFailed()
                downloadMirrorStateHolder.onEvent(DownloadMirrorSelectionEvent.Close)
                shouldClearPendingCompat = true
            } finally {
                if (shouldClearPendingCompat) {
                    downloadButtonViewModel.clearPendingCompat()
                }
                isActionInProgress = false
            }
        }
    }

    fun skipDownloadLoading() {
        if (!downloadMirrorState.isLoading || downloadMirrorState.loadedSourcesCount <= 0) return
        downloadMirrorStateHolder.onEvent(DownloadMirrorSelectionEvent.SkipLoadingUi)
    }

    fun openDownloadPanel(
        downloadCompat: MovieDetailsEpisodeActionsCompat = defaultActionsCompat,
        preferredSeason: Int? = null,
        preferredEpisode: Int? = null,
    ) {
        if (isActionInProgress || isPanelLoading || downloadMirrorState.isLoading) return

        if (mode.allowsBookmark) {
            closeBookmarkPanel()
        }
        if (mode.allowsExtendedActions) {
            closePanel()
        }
        downloadButtonViewModel.setPendingCompat(downloadCompat)
        downloadMirrorStateHolder.onEvent(
            DownloadMirrorSelectionEvent.Open(
                compat = downloadCompat,
                context = context,
                preferredSeason = preferredSeason,
                preferredEpisode = preferredEpisode,
            )
        )
    }

    fun openEpisodeDownloadPanel(episode: TvEpisode) {
        openDownloadPanel(
            preferredSeason = resolveEpisodeSeason(episode),
            preferredEpisode = episode.episodeNumber
        )
    }

    fun executeEpisodeQuickAction(
        episode: TvEpisode,
        actionId: Int,
    ) {
        if (isActionInProgress || isPanelLoading || downloadMirrorState.isLoading) return

        val targetSeason = resolveEpisodeSeason(episode)
        val targetEpisode = episode.episodeNumber
        coroutineScope.launch {
            isActionInProgress = true
            try {
                defaultActionsCompat.executeForEpisode(
                    actionId = actionId,
                    context = context,
                    preferredSeason = targetSeason,
                    preferredEpisode = targetEpisode,
                    onPlayInApp = { episodeData ->
                        goToPlayer(episodeData)
                    }
                )
            } catch (error: Throwable) {
                Log.e(
                    DebugTag,
                    "episode quick action failed actionId=$actionId season=$targetSeason episode=$targetEpisode",
                    error
                )
            } finally {
                try {
                    episodeWatchedStates = loadEpisodeWatchedStates(selectedEpisodes)
                } catch (error: Throwable) {
                    Log.e(DebugTag, "episode watched states refresh failed", error)
                }
                isActionInProgress = false
            }
        }
    }

    fun playDownloadedByState(
        state: DetailsDownloadButtonUiState,
    ): Boolean {
        val episodeId = state.episodeId ?: run {
            Log.d(DebugTag, "playDownloadedByState skipped: state without episodeId")
            return false
        }
        goToPlayer(
            PlayerScreenNavigation.buildDownloadedEpisodeData(episodeId)
        )
        Log.d(
            DebugTag,
            "playDownloadedByState episodeId=$episodeId status=${state.status} played=true via compose player route"
        )
        return true
    }

    fun handleDownloadQuickAction(
        state: DetailsDownloadButtonUiState,
        preferredSeason: Int?,
        preferredEpisode: Int?,
    ) {
        when (toDownloadUiState(state)) {
            is MovieDetailsDownloadActionState.Downloading -> {
                Log.d(
                    DebugTag,
                    "handleDownloadQuickAction ignored (downloading) season=$preferredSeason episode=$preferredEpisode"
                )
                Unit
            }

            MovieDetailsDownloadActionState.Downloaded -> {
                Log.d(
                    DebugTag,
                    "handleDownloadQuickAction downloaded season=$preferredSeason episode=$preferredEpisode episodeId=${state.episodeId}"
                )
                val isPlayed = playDownloadedByState(state)
                if (!isPlayed) {
                    Log.d(
                        DebugTag,
                        "handleDownloadQuickAction fallback to source panel (play failed)"
                    )
                    openDownloadPanel(
                        preferredSeason = preferredSeason,
                        preferredEpisode = preferredEpisode
                    )
                }
            }

            MovieDetailsDownloadActionState.Idle,
            MovieDetailsDownloadActionState.Failed -> {
                Log.d(
                    DebugTag,
                    "handleDownloadQuickAction open panel state=${toDownloadUiState(state)} season=$preferredSeason episode=$preferredEpisode"
                )
                openDownloadPanel(
                    preferredSeason = preferredSeason,
                    preferredEpisode = preferredEpisode
                )
            }
        }
    }

    LaunchedEffect(seasons, details.currentSeason) {
        if (seasons.none { season -> season.id == selectedSeasonId }) {
            selectedSeasonId = resolveInitialSeasonId(seasons, details.currentSeason)
        }
    }

    LaunchedEffect(defaultActionsCompat, context) {
        downloadButtonViewModel.setDefaultCompat(defaultActionsCompat)
        downloadButtonViewModel.refreshDefaultSnapshot(
            context = context,
            reason = "enter_details"
        )
    }

    LaunchedEffect(selectedSeasonId, selectedEpisodes, defaultActionsCompat, context) {
        if (selectedEpisodes.isEmpty()) {
            episodeDownloadStates = emptyMap()
            return@LaunchedEffect
        }

        val loadedStates = withContext(Dispatchers.IO) {
            selectedEpisodes.associate { episode ->
                val snapshot = defaultActionsCompat.getDownloadSnapshotForEpisode(
                    context = context,
                    preferredSeason = resolveEpisodeSeason(episode),
                    preferredEpisode = episode.episodeNumber
                )
                val status = snapshot?.let { normalizeDownloadStatus(it) }
                val progressFraction = when {
                    snapshot == null -> 0f
                    status == VideoDownloadManager.DownloadType.IsDone -> 1f
                    else -> calculateProgressFraction(snapshot.downloadedBytes, snapshot.totalBytes)
                }

                episode.id to DetailsDownloadButtonUiState(
                    episodeId = snapshot?.episodeId,
                    status = status,
                    progressFraction = progressFraction
                )
            }
        }

        episodeDownloadStates = loadedStates
    }

    LaunchedEffect(selectedSeasonId, selectedEpisodes, defaultActionsCompat) {
        episodeWatchedStates = loadEpisodeWatchedStates(selectedEpisodes)
    }

    LaunchedEffect(defaultActionsCompat) {
        delay(DownloadLinksPrefetchDelayMs)
        withContext(Dispatchers.IO) {
            defaultActionsCompat.prefetchDownloadMirrorLinks()
        }
    }

    LaunchedEffect(downloadMirrorStateHolder) {
        downloadMirrorStateHolder.effects.collect { effect ->
            when (effect) {
                is DownloadMirrorSelectionEffect.LoadingFinished -> {
                    handleDownloadActionOutcome(effect.outcome)
                }
            }
        }
    }

    DisposableEffect(selectedSeasonId, coroutineScope) {
        val statusObserver: (Pair<Int, VideoDownloadManager.DownloadType>) -> Unit = { (episodeId, status) ->
            coroutineScope.launch {
                updateEpisodeDownloadStateByEpisodeId(episodeId) { current ->
                    val nextProgress = when (status) {
                        VideoDownloadManager.DownloadType.IsDone -> 1f
                        VideoDownloadManager.DownloadType.IsStopped,
                        VideoDownloadManager.DownloadType.IsFailed -> 0f
                        else -> current.progressFraction
                    }
                    current.copy(
                        status = status,
                        progressFraction = nextProgress
                    )
                }
            }
        }
        val progressObserver: (Triple<Int, Long, Long>) -> Unit = { (episodeId, downloadedBytes, totalBytes) ->
            coroutineScope.launch {
                val progress = calculateProgressFraction(downloadedBytes, totalBytes)
                updateEpisodeDownloadStateByEpisodeId(episodeId) { current ->
                    val nextProgress = when {
                        current.status == VideoDownloadManager.DownloadType.IsDone -> 1f
                        progress > 0f -> progress
                        else -> current.progressFraction
                    }
                    current.copy(progressFraction = nextProgress)
                }
            }
        }

        VideoDownloadManager.downloadStatusEvent += statusObserver
        VideoDownloadManager.downloadProgressEvent += progressObserver

        onDispose {
            VideoDownloadManager.downloadStatusEvent -= statusObserver
            VideoDownloadManager.downloadProgressEvent -= progressObserver
        }
    }

    DisposableEffect(downloadMirrorStateHolder) {
        onDispose {
            downloadMirrorStateHolder.onEvent(DownloadMirrorSelectionEvent.Close)
            downloadButtonViewModel.clearPendingCompat()
        }
    }

    val downloadActionState = resolveDownloadActionState(
        status = downloadButtonState.status,
        progressFraction = downloadButtonState.progressFraction
    )

    BackHandler(
        enabled = !isActionsPanelVisible &&
            !isBookmarkPanelVisible &&
            !downloadMirrorState.isVisible,
        onBack = onBackPressed
    )

    Box(modifier = modifier) {
        MovieDetailsBackdrop(
            posterUri = details.posterUri,
            title = details.name,
            modifier = Modifier.matchParentSize(),
            gradientColor = MaterialTheme.colorScheme.background
        )

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 135.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                MovieDetails(
                    movieDetails = details,
                    goToMoviePlayer = { goToPlayer(resolveDefaultEpisodeData(details)) },
                    playButtonLabel = detailsPlayLabel(
                        mode = mode,
                        details = details,
                        isSeriesContent = isSeriesContent
                    ),
                    titleMetadata = detailsTitleMetadata(details),
                    downloadActionState = downloadActionState,
                    downFocusRequester = selectedSeasonFocusRequester,
                    onPrimaryActionsFocused = {
                        if (listState.firstVisibleItemIndex == 0 &&
                            listState.firstVisibleItemScrollOffset == 0
                        ) {
                            return@MovieDetails
                        }
                        if (listState.isScrollInProgress) return@MovieDetails
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    onQuickActionClick = { quickAction ->
                        when (quickAction) {
                            MovieDetailsQuickAction.Bookmark -> openBookmarkPanel()
                            MovieDetailsQuickAction.Favorite -> onFavoriteClick()
                            MovieDetailsQuickAction.Download -> {
                                handleDownloadQuickAction(
                                    state = downloadButtonState,
                                    preferredSeason = null,
                                    preferredEpisode = null
                                )
                            }
                            MovieDetailsQuickAction.More -> openActionsPanel()
                            MovieDetailsQuickAction.Search -> Unit
                            MovieDetailsQuickAction.MarkAsWatched -> Unit
                            MovieDetailsQuickAction.MarkWatchedUpToThisEpisode -> Unit
                            MovieDetailsQuickAction.RemoveFromWatched -> Unit
                            MovieDetailsQuickAction.RemoveWatchedUpToThisEpisode -> Unit
                        }
                    },
                )
            }

            if (isSeriesContent) {
                if (mode == DetailsScreenMode.Media && seasons.isNotEmpty()) {
                    item {
                        SeasonsSectionHeader(
                            modifier = Modifier.padding(start = childPadding.start)
                        )
                    }
                }

                if (seasons.isNotEmpty()) {
                    item {
                        SeasonSelectorRow(
                            seasons = seasons,
                            selectedSeasonId = selectedSeasonId,
                            focusRequesters = seasonTabFocusRequesters,
                            onSeasonSelected = { season -> selectedSeasonId = season.id },
                            modifier = Modifier
                                .padding(start = childPadding.start, end = childPadding.end)
                                .padding(bottom = 8.dp, top = 8.dp)
                        )
                    }
                }

                if (mode == DetailsScreenMode.Media) {
                    item {
                        EpisodesSectionHeader(
                            modifier = Modifier.padding(start = childPadding.start)
                        )
                    }
                }

                if (selectedEpisodes.isEmpty()) {
                    item {
                        NoEpisodesRow(
                            modifier = Modifier
                                .padding(start = childPadding.start, end = childPadding.end)
                                .padding(bottom = 8.dp)
                        )
                    }
                } else {
                    items(
                        items = selectedEpisodes,
                        key = { episode -> episode.id }
                    ) { episode ->
                        val episodeDownloadState = resolveEpisodeDownloadState(episode)
                        EpisodeCard(
                            episode = episode,
                            fallbackDescription = details.description,
                            onEpisodeSelected = { selectedEpisode ->
                                goToPlayer(selectedEpisode.data)
                            },
                            isWatched = resolveEpisodeWatchedState(episode),
                            downloadActionState = toDownloadUiState(episodeDownloadState),
                            onEpisodeQuickActionClick = { selectedEpisode, quickAction ->
                                when (quickAction) {
                                    MovieDetailsQuickAction.MarkAsWatched,
                                    MovieDetailsQuickAction.RemoveFromWatched,
                                    MovieDetailsQuickAction.Bookmark -> executeEpisodeQuickAction(
                                        episode = selectedEpisode,
                                        actionId = ACTION_MARK_AS_WATCHED
                                    )

                                    MovieDetailsQuickAction.MarkWatchedUpToThisEpisode,
                                    MovieDetailsQuickAction.RemoveWatchedUpToThisEpisode,
                                    MovieDetailsQuickAction.Favorite -> executeEpisodeQuickAction(
                                        episode = selectedEpisode,
                                        actionId = ACTION_MARK_WATCHED_UP_TO_THIS_EPISODE
                                    )

                                    MovieDetailsQuickAction.Download -> {
                                        val selectedEpisodeDownloadState =
                                            resolveEpisodeDownloadState(selectedEpisode)
                                        val targetSeason = resolveEpisodeSeason(selectedEpisode)
                                        val targetEpisode = selectedEpisode.episodeNumber
                                        handleDownloadQuickAction(
                                            state = selectedEpisodeDownloadState,
                                            preferredSeason = targetSeason,
                                            preferredEpisode = targetEpisode
                                        )
                                    }

                                    MovieDetailsQuickAction.More ->
                                        openEpisodeDownloadPanel(selectedEpisode)

                                    MovieDetailsQuickAction.Search -> Unit
                                }
                            },
                            modifier = Modifier
                                .padding(start = childPadding.start, end = childPadding.end)
                                .padding(bottom = 12.dp)
                        )
                    }
                }
            }

            if (details.cast.isNotEmpty()) {
                item {
                    CastAndCrewList(
                        castAndCrew = details.cast
                    )
                }
            }

            if (details.similarMovies.isNotEmpty()) {
                item {
                    PosterMoviesRow(
                        title = StringConstants
                            .Composable
                            .movieDetailsScreenSimilarTo(details.name),
                        titleStyle = MaterialTheme.typography.titleMedium,
                        movieList = details.similarMovies,
                        onMovieSelected = refreshScreenWithNewItem
                    )
                }
            }

            if (hasAdditionalInfo) {
                item {
                    Box(
                        modifier = Modifier
                            .padding(start = childPadding.start, end = childPadding.end)
                            .padding(DetailsBottomDividerPadding)
                            .fillMaxWidth()
                            .height(1.dp)
                            .alpha(0.15f)
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                }

                item {
                    AdditionalInfoSection(
                        tvSeriesDetails = details
                    )
                }
            }
        }

        if (mode.allowsExtendedActions) {
            val currentTitle = panelSelection?.title ?: stringResource(R.string.episode_more_options_des)
            val currentItems = panelSelection?.options ?: panelItems

            MovieActionsSidePanel(
                visible = isActionsPanelVisible,
                loading = isPanelLoading,
                inProgress = isActionInProgress,
                title = currentTitle,
                items = currentItems,
                onCloseRequested = { navigatePanelBack() },
                onActionSelected = { actionId -> executeAction(actionId) }
            )
        }

        val downloadPanelTitle = downloadMirrorState.selectionRequest?.title
            ?: stringResource(R.string.episode_action_download_mirror)
        val downloadPanelItems = downloadMirrorState.selectionRequest?.options ?: emptyList()
        val skipLoadingItem = MovieDetailsCompatPanelItem(
            id = SkipDownloadLoadingActionId,
            label = stringResource(R.string.skip_loading),
            iconRes = R.drawable.ic_baseline_fast_forward_24
        )
        val showSkipLoadingAction = downloadMirrorState.isLoading &&
            !downloadMirrorState.isLoadingUiSkipped &&
            downloadMirrorState.loadedSourcesCount > 0
        val downloadPanelActionItems = when {
            showSkipLoadingAction -> listOf(skipLoadingItem)
            else -> downloadPanelItems
        }

        MovieActionsSidePanel(
            visible = downloadMirrorState.isVisible,
            loading = downloadMirrorState.isLoading,
            inProgress = isActionInProgress,
            title = downloadPanelTitle,
            items = downloadPanelActionItems,
            onCloseRequested = { closeDownloadPanel() },
            onActionSelected = { actionId ->
                if (actionId == SkipDownloadLoadingActionId) {
                    skipDownloadLoading()
                } else {
                    executeDownloadSelection(actionId)
                }
            },
            panelTestTag = mode.downloadPanelTestTag,
            showItemsWhileLoading = downloadMirrorState.loadedSourcesCount > 0,
            headerContent = {
                if (downloadMirrorState.isLoading) {
                    Text(
                        text = stringResource(
                            R.string.tv_player_loading_sources_progress,
                            downloadMirrorState.loadedSourcesCount
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            },
            emptyContent = {
                if (downloadMirrorState.isLoading) {
                    Text(
                        text = stringResource(R.string.loading),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.no_links_found_toast),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        )

        if (mode.allowsBookmark) {
            BookmarkStatusSidePanel(
                visible = isBookmarkPanelVisible,
                currentStatus = details.bookmarkLabelRes.toWatchType(),
                onCloseRequested = { closeBookmarkPanel() },
                onBookmarkSelected = { selectedStatus ->
                    onBookmarkClick(selectedStatus)
                    closeBookmarkPanel()
                },
                panelTestTag = mode.bookmarkPanelTestTag ?: "bookmark_side_panel"
            )
        }
    }
}
