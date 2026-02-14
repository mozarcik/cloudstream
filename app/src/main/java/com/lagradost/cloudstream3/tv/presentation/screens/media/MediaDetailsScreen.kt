package com.lagradost.cloudstream3.tv.presentation.screens.media

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
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
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatActionOutcome
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatDownloadSnapshot
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatPanelItem
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatSelectionRequest
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.data.util.StringConstants
import com.lagradost.cloudstream3.tv.presentation.common.Error
import com.lagradost.cloudstream3.tv.presentation.common.ItemDirection
import com.lagradost.cloudstream3.tv.presentation.common.Loading
import com.lagradost.cloudstream3.tv.presentation.common.MoviesRow
import com.lagradost.cloudstream3.tv.presentation.screens.movies.CastAndCrewList
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetails
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsBackdrop
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsDownloadActionState
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsQuickAction
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieActionsSidePanel
import com.lagradost.cloudstream3.tv.presentation.screens.movies.rememberChildPadding
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.AdditionalInfoSection
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.EpisodeCard
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.EpisodesSectionHeader
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.NoEpisodesRow
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.SeasonSelectorRow
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.SeasonsSectionHeader
import com.lagradost.cloudstream3.tv.presentation.screens.tvseries.resolveInitialSeasonId
import com.lagradost.cloudstream3.utils.VideoDownloadManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DebugTag = "TvMediaDetailsUI"
private const val SkipDownloadLoadingActionId = -10_001

object MediaDetailsScreen {
    const val UrlBundleKey = "url"
    const val ApiNameBundleKey = "apiName"
}

@Composable
fun MediaDetailsScreen(
    goToPlayer: (String?) -> Unit,
    onBackPressed: () -> Unit,
    refreshScreenWithNewItem: (Movie) -> Unit,
    mediaDetailsScreenViewModel: MediaDetailsScreenViewModel
) {
    val uiState by mediaDetailsScreenViewModel.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        is MediaDetailsScreenUiState.Loading -> {
            Loading(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        is MediaDetailsScreenUiState.Error -> {
            Error(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        is MediaDetailsScreenUiState.Done -> {
            Details(
                mediaDetails = s.mediaDetails,
                sourceUrl = s.sourceUrl,
                apiName = s.apiName,
                goToPlayer = goToPlayer,
                onBackPressed = onBackPressed,
                refreshScreenWithNewItem = refreshScreenWithNewItem,
                onFavoriteClick = mediaDetailsScreenViewModel::onFavoriteClick,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .animateContentSize()
            )
        }
    }
}

@Composable
private fun Details(
    mediaDetails: MovieDetails,
    sourceUrl: String,
    apiName: String,
    goToPlayer: (String?) -> Unit,
    onBackPressed: () -> Unit,
    refreshScreenWithNewItem: (Movie) -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val childPadding = rememberChildPadding()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val seasons = mediaDetails.seasons
    val isSeriesContent = mediaDetails.seasonCount != null || mediaDetails.episodeCount != null || seasons.isNotEmpty()
    var selectedSeasonId by rememberSaveable(mediaDetails.id) {
        mutableStateOf(resolveInitialSeasonId(seasons, mediaDetails.currentSeason))
    }
    val selectedSeason = seasons.firstOrNull { season -> season.id == selectedSeasonId } ?: seasons.firstOrNull()
    val selectedEpisodes = selectedSeason?.episodes.orEmpty()
    val hasAdditionalInfo = listOf(
        mediaDetails.status,
        mediaDetails.originalLanguage,
        mediaDetails.budget,
        mediaDetails.revenue
    ).any { value -> value.isNotBlank() }
    var isDownloadPanelVisible by rememberSaveable(mediaDetails.id) { mutableStateOf(false) }
    var isActionInProgress by remember { mutableStateOf(false) }
    var isDownloadPanelLoading by remember(mediaDetails.id) { mutableStateOf(false) }
    var downloadPanelSelection by remember(mediaDetails.id) {
        mutableStateOf<MovieDetailsCompatSelectionRequest?>(null)
    }
    var downloadEpisodeId by remember(mediaDetails.id) { mutableStateOf<Int?>(null) }
    var downloadStatus by remember(mediaDetails.id) {
        mutableStateOf<VideoDownloadManager.DownloadType?>(null)
    }
    var downloadProgressFraction by remember(mediaDetails.id) { mutableStateOf(0f) }
    var downloadLoadedSourcesCount by remember(mediaDetails.id) { mutableIntStateOf(0) }
    var skipDownloadSourcesLoading by remember(mediaDetails.id) { mutableStateOf(false) }
    var downloadRequestVersion by remember(mediaDetails.id) { mutableIntStateOf(0) }
    var lastLoggedProgressPercent by remember(mediaDetails.id) { mutableIntStateOf(-1) }
    var hasLoggedPendingWarning by remember(mediaDetails.id) { mutableStateOf(false) }

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
        mediaDetails.currentSeason,
        mediaDetails.currentEpisode
    ) {
        createEpisodeActionsCompat(
            mediaDetails.currentSeason,
            mediaDetails.currentEpisode
        )
    }
    var activeDownloadActionsCompat by remember(mediaDetails.id) {
        mutableStateOf<MovieDetailsEpisodeActionsCompat?>(null)
    }

    fun closeDownloadPanel() {
        Log.d(DebugTag, "close download panel")
        skipDownloadSourcesLoading = true
        downloadRequestVersion += 1
        downloadLoadedSourcesCount = 0
        downloadPanelSelection = null
        activeDownloadActionsCompat = null
        isDownloadPanelVisible = false
        isDownloadPanelLoading = false
        isActionInProgress = false
    }

    suspend fun refreshDownloadSnapshot(
        reason: String,
        compat: MovieDetailsEpisodeActionsCompat = defaultActionsCompat
    ) {
        val snapshot = compat.getDownloadSnapshot(context)
        if (snapshot == null) {
            Log.d(DebugTag, "download snapshot[$reason]: null")
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
            "download snapshot[$reason]: id=${snapshot.episodeId} status=${snapshot.status} normalizedStatus=$normalizedStatus downloaded=${snapshot.downloadedBytes} total=${snapshot.totalBytes} pending=${snapshot.hasPendingRequest} progress=${(normalizedProgress * 100f).toInt()}%"
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
                Log.d(DebugTag, "download outcome: completed")
                closeDownloadPanel()
            }

            is MovieDetailsCompatActionOutcome.OpenSelection -> {
                Log.d(
                    DebugTag,
                    "download outcome: open selection options=${outcome.request.options.size}"
                )
                downloadPanelSelection = outcome.request
                isDownloadPanelVisible = true
            }
        }
    }

    fun executeDownloadSelection(actionId: Int) {
        val selection = downloadPanelSelection ?: return
        if (isActionInProgress || isDownloadPanelLoading) return

        coroutineScope.launch {
            isActionInProgress = true
            downloadStatus = VideoDownloadManager.DownloadType.IsPending
            downloadProgressFraction = 0f
            lastLoggedProgressPercent = 0
            hasLoggedPendingWarning = false
            Log.d(DebugTag, "download source selected actionId=$actionId")
            try {
                val outcome = selection.onOptionSelected(actionId)
                handleDownloadActionOutcome(outcome)
                refreshDownloadSnapshot(
                    reason = "after_source_selected",
                    compat = activeDownloadActionsCompat ?: defaultActionsCompat
                )
                delay(900)
                refreshDownloadSnapshot(
                    reason = "after_source_selected_delayed",
                    compat = activeDownloadActionsCompat ?: defaultActionsCompat
                )
            } finally {
                isActionInProgress = false
            }
        }
    }

    fun skipDownloadLoading() {
        if (!isDownloadPanelLoading || downloadLoadedSourcesCount <= 0) return
        Log.d(
            DebugTag,
            "skip download source loading requested loadedSources=$downloadLoadedSourcesCount"
        )
        skipDownloadSourcesLoading = true
    }

    fun openDownloadPanel(downloadCompat: MovieDetailsEpisodeActionsCompat) {
        if (isActionInProgress || isDownloadPanelLoading) return

        Log.d(DebugTag, "open download panel")
        activeDownloadActionsCompat = downloadCompat
        val requestVersion = downloadRequestVersion + 1
        downloadRequestVersion = requestVersion
        skipDownloadSourcesLoading = false
        downloadLoadedSourcesCount = 0
        downloadPanelSelection = null
        isDownloadPanelVisible = true
        isDownloadPanelLoading = true

        coroutineScope.launch {
            isActionInProgress = true
            try {
                val outcome = downloadCompat.requestDownloadMirrorSelection(
                    context = context,
                    onSourcesProgress = { loadedSources ->
                        coroutineScope.launch {
                            if (downloadRequestVersion == requestVersion) {
                                downloadLoadedSourcesCount = loadedSources
                                Log.d(
                                    DebugTag,
                                    "download sources loading progress: loadedSources=$loadedSources requestVersion=$requestVersion"
                                )
                            }
                        }
                    },
                    shouldSkipLoading = {
                        skipDownloadSourcesLoading || downloadRequestVersion != requestVersion
                    }
                )
                if (downloadRequestVersion != requestVersion) return@launch
                handleDownloadActionOutcome(outcome)
            } finally {
                if (downloadRequestVersion == requestVersion) {
                    isActionInProgress = false
                    isDownloadPanelLoading = false
                    skipDownloadSourcesLoading = false
                }
            }
        }
    }

    fun openDefaultDownloadPanel() {
        openDownloadPanel(defaultActionsCompat)
    }

    fun openEpisodeDownloadPanel(episode: TvEpisode) {
        val targetSeason = episode.seasonNumber ?: selectedSeason?.displaySeasonNumber ?: selectedSeason?.seasonNumber
        val targetEpisode = episode.episodeNumber
        val episodeActionsCompat = createEpisodeActionsCompat(targetSeason, targetEpisode)
        openDownloadPanel(episodeActionsCompat)
    }

    LaunchedEffect(seasons, mediaDetails.currentSeason) {
        if (seasons.none { season -> season.id == selectedSeasonId }) {
            selectedSeasonId = resolveInitialSeasonId(seasons, mediaDetails.currentSeason)
        }
    }
    LaunchedEffect(
        mediaDetails.id,
        isSeriesContent,
        mediaDetails.seasonCount,
        mediaDetails.episodeCount,
        seasons.size,
        selectedSeasonId,
        selectedEpisodes.size
    ) {
        Log.d(
            DebugTag,
            "render id=${mediaDetails.id} name=${mediaDetails.name} isSeries=$isSeriesContent seasonCount=${mediaDetails.seasonCount} episodeCount=${mediaDetails.episodeCount} seasons=${seasons.size} selectedSeason=$selectedSeasonId selectedEpisodes=${selectedEpisodes.size} cast=${mediaDetails.cast.size} similar=${mediaDetails.similarMovies.size}"
        )
        if (isSeriesContent && seasons.isEmpty()) {
            Log.d(
                DebugTag,
                "render:series metadata exists but seasons list is empty"
            )
        }
    }

    LaunchedEffect(defaultActionsCompat, context) {
        refreshDownloadSnapshot(reason = "enter_details")
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
                            Log.d(
                                DebugTag,
                                "download status event: id=$id status=$status currentProgress=${(downloadProgressFraction * 100f).toInt()}%"
                            )
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
                                    "download pending with 0% progress. If it persists, check battery optimization/background restrictions for CloudStream."
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
                                "download progress event: id=$id progress=$progressPercent% bytes=$downloaded/$total"
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
        enabled = !isDownloadPanelVisible,
        onBack = onBackPressed
    )
    Box(modifier = modifier) {
        MovieDetailsBackdrop(
            posterUri = mediaDetails.posterUri,
            title = mediaDetails.name,
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
                    movieDetails = mediaDetails,
                    goToMoviePlayer = { goToPlayer(resolveDefaultEpisodeData(mediaDetails)) },
                    playButtonLabel = mediaDetailsPlayLabel(mediaDetails),
                    titleMetadata = mediaDetailsTitleMetadata(mediaDetails),
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
                            MovieDetailsQuickAction.Favorite -> onFavoriteClick()
                            MovieDetailsQuickAction.Download -> openDefaultDownloadPanel()
                            else -> Unit
                        }
                    },
                )
            }

            if (isSeriesContent) {
                if (seasons.isNotEmpty()) {
                    item {
                        SeasonsSectionHeader(
                            modifier = Modifier.padding(start = childPadding.start)
                        )
                        SeasonSelectorRow(
                            seasons = seasons,
                            selectedSeasonId = selectedSeasonId,
                            onSeasonSelected = { season -> selectedSeasonId = season.id }
                        )
                    }
                }

                item {
                    EpisodesSectionHeader(
                        modifier = Modifier.padding(start = childPadding.start)
                    )
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
                            fallbackDescription = mediaDetails.description,
                            onEpisodeSelected = { selectedEpisode ->
                                goToPlayer(selectedEpisode.data)
                            },
                            onEpisodeQuickActionClick = { selectedEpisode, quickAction ->
                                when (quickAction) {
                                    MovieDetailsQuickAction.Download -> openEpisodeDownloadPanel(selectedEpisode)
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

            if (mediaDetails.cast.isNotEmpty()) {
                item {
                    CastAndCrewList(
                        castAndCrew = mediaDetails.cast
                    )
                }
            }

            if (mediaDetails.similarMovies.isNotEmpty()) {
                item {
                    MoviesRow(
                        title = StringConstants
                            .Composable
                            .movieDetailsScreenSimilarTo(mediaDetails.name),
                        titleStyle = MaterialTheme.typography.titleMedium,
                        movieList = mediaDetails.similarMovies,
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
                            .padding(BottomDividerPadding)
                            .fillMaxWidth()
                            .height(1.dp)
                            .alpha(0.15f)
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                }

                item {
                    AdditionalInfoSection(
                        tvSeriesDetails = mediaDetails
                    )
                }
            }
        }

        val downloadPanelTitle = downloadPanelSelection?.title
            ?: stringResource(R.string.episode_action_download_mirror)
        val downloadPanelItems = downloadPanelSelection?.options ?: emptyList()
        val skipLoadingItem = MovieDetailsCompatPanelItem(
            id = SkipDownloadLoadingActionId,
            label = stringResource(R.string.skip_loading),
            iconRes = R.drawable.ic_baseline_fast_forward_24
        )
        val downloadPanelActionItems = when {
            isDownloadPanelLoading && downloadLoadedSourcesCount > 0 -> listOf(skipLoadingItem)
            else -> downloadPanelItems
        }

        MovieActionsSidePanel(
            visible = isDownloadPanelVisible,
            loading = isDownloadPanelLoading,
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
            panelTestTag = "media_download_sources_side_panel",
            showItemsWhileLoading = downloadLoadedSourcesCount > 0,
            headerContent = {
                if (isDownloadPanelLoading) {
                    Text(
                        text = stringResource(
                            R.string.tv_player_loading_sources_progress,
                            downloadLoadedSourcesCount
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            },
            emptyContent = {
                if (isDownloadPanelLoading) {
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
    }
}

private val BottomDividerPadding = PaddingValues(vertical = 48.dp)

private fun resolveDefaultEpisodeData(details: MovieDetails): String? {
    if (details.seasons.isEmpty()) return null

    val selectedSeason = details.currentSeason?.let { currentSeason ->
        details.seasons.firstOrNull { season ->
            season.displaySeasonNumber == currentSeason || season.seasonNumber == currentSeason
        }
    } ?: details.seasons.firstOrNull()

    val selectedEpisode = details.currentEpisode?.let { currentEpisode ->
        selectedSeason?.episodes?.firstOrNull { episode ->
            episode.episodeNumber == currentEpisode
        }
    } ?: selectedSeason?.episodes?.firstOrNull()

    return selectedEpisode?.data
}

@Composable
private fun mediaDetailsPlayLabel(mediaDetails: MovieDetails): String {
    val seasonShort = stringResource(R.string.season_short)
    val episodeShort = stringResource(R.string.episode_short)
    val episodeLabel = stringResource(R.string.episode)
    val movieFallback = stringResource(R.string.movies_singular)
    val seriesFallback = stringResource(R.string.tv_series_singular)

    val currentEpisode = mediaDetails.currentEpisode
    val currentSeason = mediaDetails.currentSeason

    if (currentEpisode != null) {
        return if (currentSeason != null) {
            "$seasonShort$currentSeason:$episodeShort$currentEpisode"
        } else {
            "$episodeLabel $currentEpisode"
        }
    }

    val hasSeriesMetadata = mediaDetails.seasonCount != null || mediaDetails.episodeCount != null
    return if (hasSeriesMetadata) seriesFallback else movieFallback
}

@Composable
private fun mediaDetailsTitleMetadata(mediaDetails: MovieDetails): List<String> {
    val seasonLabel = stringResource(R.string.season)
    val episodesLabel = stringResource(R.string.episodes)

    return listOfNotNull(
        mediaDetails.seasonCount?.let { "$seasonLabel $it" },
        mediaDetails.episodeCount?.let { "$episodesLabel $it" }
    )
}

private val ActiveDownloadStates = setOf(
    VideoDownloadManager.DownloadType.IsDownloading,
    VideoDownloadManager.DownloadType.IsPending,
    VideoDownloadManager.DownloadType.IsPaused
)

private fun resolveDownloadActionState(
    status: VideoDownloadManager.DownloadType?,
    progressFraction: Float,
): MovieDetailsDownloadActionState {
    val normalizedProgress = progressFraction.coerceIn(0f, 1f)

    return when {
        status == VideoDownloadManager.DownloadType.IsDone || normalizedProgress >= 0.999f ->
            MovieDetailsDownloadActionState.Downloaded

        status in ActiveDownloadStates ->
            MovieDetailsDownloadActionState.Downloading(progress = normalizedProgress)

        else -> MovieDetailsDownloadActionState.Idle
    }
}

private fun normalizeDownloadStatus(
    snapshot: MovieDetailsCompatDownloadSnapshot,
): VideoDownloadManager.DownloadType? {
    val status = snapshot.status
    if (status != null) {
        return status
    }

    if (snapshot.hasPendingRequest) {
        return if (snapshot.downloadedBytes > 0L) {
            VideoDownloadManager.DownloadType.IsDownloading
        } else {
            VideoDownloadManager.DownloadType.IsPending
        }
    }

    if (snapshot.downloadedBytes > 0L && snapshot.totalBytes <= 0L) {
        return VideoDownloadManager.DownloadType.IsDownloading
    }

    if (snapshot.totalBytes <= 0L) {
        return null
    }

    val hasFinishedDownload = snapshot.totalBytes > 0L &&
        snapshot.downloadedBytes > 1024L &&
        snapshot.downloadedBytes + 1024L >= snapshot.totalBytes

    return if (hasFinishedDownload) {
        VideoDownloadManager.DownloadType.IsDone
    } else {
        VideoDownloadManager.DownloadType.IsDownloading
    }
}

private fun calculateProgressFraction(
    downloadedBytes: Long,
    totalBytes: Long,
): Float {
    if (downloadedBytes <= 0L || totalBytes <= 0L) {
        return 0f
    }

    return (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
}
