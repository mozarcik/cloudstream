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
import androidx.compose.runtime.mutableIntStateOf
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
import com.lagradost.cloudstream3.tv.presentation.common.ItemDirection
import com.lagradost.cloudstream3.tv.presentation.common.MoviesRow
import com.lagradost.cloudstream3.tv.presentation.screens.movies.BookmarkStatusSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.movies.CastAndCrewList
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieActionsSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetails
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsBackdrop
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsQuickAction
import com.lagradost.cloudstream3.tv.presentation.screens.movies.rememberChildPadding
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.AdditionalInfoSection
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.EpisodeCard
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.EpisodesSectionHeader
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.NoEpisodesRow
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.SeasonSelectorRow
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.SeasonsSectionHeader
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.resolveInitialSeasonId
import com.lagradost.cloudstream3.ui.WatchType
import com.lagradost.cloudstream3.utils.VideoDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DebugTag = "TvDetailsUI"
private const val SkipDownloadLoadingActionId = -10_001
private const val DownloadLinksPrefetchDelayMs = 500L

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
    var downloadEpisodeId by remember(details.id) { mutableStateOf<Int?>(null) }
    var downloadStatus by remember(details.id) {
        mutableStateOf<VideoDownloadManager.DownloadType?>(null)
    }
    var downloadProgressFraction by remember(details.id) { mutableStateOf(0f) }
    var lastLoggedProgressPercent by remember(details.id) { mutableIntStateOf(-1) }
    var hasLoggedPendingWarning by remember(details.id) { mutableStateOf(false) }
    var selectedSeasonId by rememberSaveable(details.id) {
        mutableStateOf(resolveInitialSeasonId(seasons, details.currentSeason))
    }

    val selectedSeason = seasons.firstOrNull { season -> season.id == selectedSeasonId } ?: seasons.firstOrNull()
    val selectedEpisodes = selectedSeason?.episodes.orEmpty()
    val createEpisodeActionsCompat = remember(sourceUrl, apiName) {
        { season: Int?, episode: Int? ->
            MovieDetailsEpisodeActionsCompat(
                sourceUrl = sourceUrl,
                apiName = apiName,
                preferredSeason = season,
                preferredEpisode = episode,
            )
        }
    }
    val defaultActionsCompat = remember(
        sourceUrl,
        apiName,
        details.currentSeason,
        details.currentEpisode
    ) {
        createEpisodeActionsCompat(
            details.currentSeason,
            details.currentEpisode
        )
    }
    var activeDownloadActionsCompat by remember(details.id) {
        mutableStateOf<MovieDetailsEpisodeActionsCompat?>(null)
    }
    val downloadMirrorStateHolder = remember(details.id, coroutineScope) {
        DownloadMirrorSelectionStateHolder(scope = coroutineScope)
    }
    val downloadMirrorState by downloadMirrorStateHolder.uiState.collectAsStateWithLifecycle()

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
        activeDownloadActionsCompat = null
        isActionInProgress = false
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

    suspend fun refreshDownloadSnapshot(
        reason: String,
        compat: MovieDetailsEpisodeActionsCompat = activeDownloadActionsCompat ?: defaultActionsCompat,
    ) {
        val snapshot = withContext(Dispatchers.IO) {
            compat.getDownloadSnapshot(context)
        }
        if (snapshot == null) {
            Log.d(DebugTag, "download snapshot[$reason]: null mode=$mode")
            downloadEpisodeId = null
            downloadStatus = null
            downloadProgressFraction = 0f
            lastLoggedProgressPercent = -1
            hasLoggedPendingWarning = false
            return
        }

        val normalizedStatus = normalizeDownloadStatus(snapshot)
        val normalizedProgress = when (normalizedStatus) {
            VideoDownloadManager.DownloadType.IsDone -> 1f
            else -> calculateProgressFraction(snapshot.downloadedBytes, snapshot.totalBytes)
        }

        Log.d(
            DebugTag,
            "download snapshot[$reason]: mode=$mode id=${snapshot.episodeId} status=${snapshot.status} normalizedStatus=$normalizedStatus downloaded=${snapshot.downloadedBytes} total=${snapshot.totalBytes} pending=${snapshot.hasPendingRequest} progress=${(normalizedProgress * 100f).toInt()}%"
        )

        downloadEpisodeId = snapshot.episodeId
        downloadStatus = normalizedStatus
        downloadProgressFraction = normalizedProgress
        lastLoggedProgressPercent = (normalizedProgress * 100f).toInt()
        hasLoggedPendingWarning = false
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
            downloadStatus = VideoDownloadManager.DownloadType.IsPending
            downloadProgressFraction = 0f
            lastLoggedProgressPercent = 0
            hasLoggedPendingWarning = false
            try {
                val outcome = selection.onOptionSelected(actionId)
                handleDownloadActionOutcome(outcome)
                refreshDownloadSnapshot(reason = "after_source_selected")
                delay(900)
                refreshDownloadSnapshot(reason = "after_source_selected_delayed")
            } finally {
                isActionInProgress = false
            }
        }
    }

    fun skipDownloadLoading() {
        if (!downloadMirrorState.isLoading || downloadMirrorState.loadedSourcesCount <= 0) return
        downloadMirrorStateHolder.onEvent(DownloadMirrorSelectionEvent.SkipLoadingUi)
    }

    fun openDownloadPanel(downloadCompat: MovieDetailsEpisodeActionsCompat = defaultActionsCompat) {
        if (isActionInProgress || isPanelLoading || downloadMirrorState.isLoading) return

        if (mode.allowsBookmark) {
            closeBookmarkPanel()
        }
        if (mode.allowsExtendedActions) {
            closePanel()
        }
        activeDownloadActionsCompat = downloadCompat
        downloadMirrorStateHolder.onEvent(
            DownloadMirrorSelectionEvent.Open(
                compat = downloadCompat,
                context = context
            )
        )
    }

    fun openEpisodeDownloadPanel(episode: TvEpisode) {
        val targetSeason =
            episode.seasonNumber ?: selectedSeason?.displaySeasonNumber ?: selectedSeason?.seasonNumber
        val targetEpisode = episode.episodeNumber
        val episodeActionsCompat = createEpisodeActionsCompat(targetSeason, targetEpisode)
        openDownloadPanel(episodeActionsCompat)
    }

    LaunchedEffect(seasons, details.currentSeason) {
        if (seasons.none { season -> season.id == selectedSeasonId }) {
            selectedSeasonId = resolveInitialSeasonId(seasons, details.currentSeason)
        }
    }

    LaunchedEffect(defaultActionsCompat, context) {
        refreshDownloadSnapshot(reason = "enter_details")
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

    DisposableEffect(downloadMirrorStateHolder) {
        onDispose {
            downloadMirrorStateHolder.onEvent(DownloadMirrorSelectionEvent.Close)
        }
    }

    DisposableEffect(downloadEpisodeId) {
        val targetId = downloadEpisodeId
        if (targetId == null) {
            onDispose { }
        } else {
            val statusObserver: (Pair<Int, VideoDownloadManager.DownloadType>) -> Unit =
                { (id, status) ->
                    if (id == targetId) {
                        coroutineScope.launch {
                            downloadStatus = status
                            if (status == VideoDownloadManager.DownloadType.IsDone) {
                                downloadProgressFraction = 1f
                            } else if (
                                status == VideoDownloadManager.DownloadType.IsStopped ||
                                status == VideoDownloadManager.DownloadType.IsFailed
                            ) {
                                downloadProgressFraction = 0f
                            }

                            if (status == VideoDownloadManager.DownloadType.IsPending &&
                                downloadProgressFraction <= 0f &&
                                !hasLoggedPendingWarning
                            ) {
                                hasLoggedPendingWarning = true
                                Log.w(
                                    DebugTag,
                                    "download pending with 0% progress mode=$mode. If it persists, check battery optimization/background restrictions for CloudStream."
                                )
                            }
                        }
                    }
                }

            val progressObserver: (Triple<Int, Long, Long>) -> Unit = { (id, downloaded, total) ->
                if (id == targetId) {
                    coroutineScope.launch {
                        val progress = calculateProgressFraction(downloaded, total)
                        downloadProgressFraction = when {
                            downloadStatus == VideoDownloadManager.DownloadType.IsDone -> 1f
                            progress > 0f -> progress
                            else -> downloadProgressFraction
                        }
                        val progressPercent = (downloadProgressFraction * 100f).toInt()
                        val shouldLogProgress = progressPercent != lastLoggedProgressPercent &&
                            (progressPercent <= 5 || progressPercent % 5 == 0 || progressPercent == 100)

                        if (shouldLogProgress) {
                            lastLoggedProgressPercent = progressPercent
                            Log.d(
                                DebugTag,
                                "download progress event: mode=$mode id=$id progress=$progressPercent% bytes=$downloaded/$total"
                            )
                        }
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
    }

    val downloadActionState = resolveDownloadActionState(
        status = downloadStatus,
        progressFraction = downloadProgressFraction
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
                            MovieDetailsQuickAction.Download -> openDownloadPanel()
                            MovieDetailsQuickAction.More -> openActionsPanel()
                            MovieDetailsQuickAction.Search -> Unit
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
                        EpisodeCard(
                            episode = episode,
                            fallbackDescription = details.description,
                            onEpisodeSelected = { selectedEpisode ->
                                goToPlayer(selectedEpisode.data)
                            },
                            onEpisodeQuickActionClick = { selectedEpisode, quickAction ->
                                when (quickAction) {
                                    MovieDetailsQuickAction.Download ->
                                        openEpisodeDownloadPanel(selectedEpisode)

                                    else -> Unit
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
                    MoviesRow(
                        title = StringConstants
                            .Composable
                            .movieDetailsScreenSimilarTo(details.name),
                        titleStyle = MaterialTheme.typography.titleMedium,
                        movieList = details.similarMovies,
                        itemDirection = ItemDirection.Horizontal,
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
