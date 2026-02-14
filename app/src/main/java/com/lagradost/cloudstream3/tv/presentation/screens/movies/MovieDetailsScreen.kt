package com.lagradost.cloudstream3.tv.presentation.screens.movies

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.CommonActivity
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatActionOutcome
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatDownloadSnapshot
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatPanelItem
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatSelectionRequest
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.util.StringConstants
import com.lagradost.cloudstream3.tv.presentation.common.Error
import com.lagradost.cloudstream3.tv.presentation.common.ItemDirection
import com.lagradost.cloudstream3.tv.presentation.common.MenuListSidePanel
import com.lagradost.cloudstream3.tv.presentation.common.MoviesRow
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem
import com.lagradost.cloudstream3.ui.WatchType
import com.lagradost.cloudstream3.utils.VideoDownloadManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DebugTag = "TvMovieDetailsUI"
private const val SkipDownloadLoadingActionId = -10_001

object MovieDetailsScreen {
    const val UrlBundleKey = "url"
    const val ApiNameBundleKey = "apiName"
    const val LoadingTitleBundleKey = "movieLoadingTitle"
    const val LoadingPosterBundleKey = "movieLoadingPoster"
    const val LoadingBackdropBundleKey = "movieLoadingBackdrop"
}

@Composable
fun MovieDetailsScreen(
    goToMoviePlayer: () -> Unit,
    onBackPressed: () -> Unit,
    refreshScreenWithNewMovie: (Movie) -> Unit,
    movieDetailsScreenViewModel: MovieDetailsScreenViewModel
) {
    val uiState by movieDetailsScreenViewModel.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        is MovieDetailsScreenUiState.Loading -> {
            MovieDetailsLoadingPlaceholder(
                title = s.preview.title ?: stringResource(R.string.movies_singular),
                posterUri = s.preview.posterUri,
                backdropUri = s.preview.backdropUri,
                modifier = Modifier.fillMaxSize()
            )
        }

        is MovieDetailsScreenUiState.Error -> {
            Error(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        is MovieDetailsScreenUiState.Done -> {
            Details(
                movieDetails = s.movieDetails,
                sourceUrl = s.sourceUrl,
                apiName = s.apiName,
                goToMoviePlayer = goToMoviePlayer,
                onBackPressed = onBackPressed,
                refreshScreenWithNewMovie = refreshScreenWithNewMovie,
                onFavoriteClick = movieDetailsScreenViewModel::onFavoriteClick,
                onBookmarkClick = movieDetailsScreenViewModel::onBookmarkClick,
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
    movieDetails: MovieDetails,
    sourceUrl: String,
    apiName: String,
    goToMoviePlayer: () -> Unit,
    onBackPressed: () -> Unit,
    refreshScreenWithNewMovie: (Movie) -> Unit,
    onFavoriteClick: () -> Unit,
    onBookmarkClick: (WatchType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val childPadding = rememberChildPadding()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var isActionsPanelVisible by rememberSaveable(movieDetails.id) { mutableStateOf(false) }
    var isBookmarkPanelVisible by rememberSaveable(movieDetails.id) { mutableStateOf(false) }
    var isDownloadPanelVisible by rememberSaveable(movieDetails.id) { mutableStateOf(false) }
    var isActionInProgress by remember { mutableStateOf(false) }
    var isPanelLoading by remember(movieDetails.id) { mutableStateOf(false) }
    var isDownloadPanelLoading by remember(movieDetails.id) { mutableStateOf(false) }
    var panelItems by remember(movieDetails.id) { mutableStateOf<List<MovieDetailsCompatPanelItem>>(emptyList()) }
    var panelSelection by remember(movieDetails.id) { mutableStateOf<MovieDetailsCompatSelectionRequest?>(null) }
    var downloadPanelSelection by remember(movieDetails.id) {
        mutableStateOf<MovieDetailsCompatSelectionRequest?>(null)
    }
    var downloadEpisodeId by remember(movieDetails.id) { mutableStateOf<Int?>(null) }
    var downloadStatus by remember(movieDetails.id) {
        mutableStateOf<VideoDownloadManager.DownloadType?>(null)
    }
    var downloadProgressFraction by remember(movieDetails.id) { mutableStateOf(0f) }
    var downloadLoadedSourcesCount by remember(movieDetails.id) { mutableIntStateOf(0) }
    var skipDownloadSourcesLoading by remember(movieDetails.id) { mutableStateOf(false) }
    var downloadRequestVersion by remember(movieDetails.id) { mutableIntStateOf(0) }
    var lastLoggedProgressPercent by remember(movieDetails.id) { mutableIntStateOf(-1) }
    var hasLoggedPendingWarning by remember(movieDetails.id) { mutableStateOf(false) }

    val actionsCompat = remember(
        sourceUrl,
        apiName,
        movieDetails.currentSeason,
        movieDetails.currentEpisode
    ) {
        MovieDetailsEpisodeActionsCompat(
            sourceUrl = sourceUrl,
            apiName = apiName,
            preferredSeason = movieDetails.currentSeason,
            preferredEpisode = movieDetails.currentEpisode,
        )
    }

    suspend fun refreshDownloadSnapshot(reason: String) {
        val snapshot = actionsCompat.getDownloadSnapshot(context)
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

    fun closePanel() {
        panelSelection = null
        isActionsPanelVisible = false
    }

    fun closeBookmarkPanel() {
        isBookmarkPanelVisible = false
    }

    fun closeDownloadPanel() {
        Log.d(DebugTag, "close download panel")
        skipDownloadSourcesLoading = true
        downloadRequestVersion += 1
        downloadLoadedSourcesCount = 0
        downloadPanelSelection = null
        isDownloadPanelVisible = false
        isDownloadPanelLoading = false
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
        if (isActionInProgress || isPanelLoading) return

        coroutineScope.launch {
            isActionInProgress = true
            try {
                val selection = panelSelection
                val outcome = if (selection != null) {
                    selection.onOptionSelected(actionId)
                } else {
                    actionsCompat.execute(
                        actionId = actionId,
                        context = context,
                        onPlayInApp = goToMoviePlayer,
                    )
                }
                handleActionOutcome(outcome)
            } finally {
                isActionInProgress = false
            }
        }
    }

    fun openActionsPanel() {
        if (isPanelLoading || isActionInProgress) return

        closeBookmarkPanel()
        closeDownloadPanel()
        isActionsPanelVisible = true
        panelSelection = null
        panelItems = emptyList()

        coroutineScope.launch {
            isPanelLoading = true
            try {
                val loadedActions = actionsCompat.loadPanelActions(context)
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
        if (isPanelLoading || isActionInProgress) return
        closePanel()
        closeDownloadPanel()
        isBookmarkPanelVisible = true
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
                refreshDownloadSnapshot(reason = "after_source_selected")
                delay(900)
                refreshDownloadSnapshot(reason = "after_source_selected_delayed")
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

    fun openDownloadPanel() {
        if (isActionInProgress || isPanelLoading || isDownloadPanelLoading) return

        Log.d(DebugTag, "open download panel")
        closeBookmarkPanel()
        closePanel()
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
                val outcome = actionsCompat.requestDownloadMirrorSelection(
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

    LaunchedEffect(
        movieDetails.id,
        movieDetails.seasonCount,
        movieDetails.episodeCount,
        movieDetails.seasons.size
    ) {
        Log.d(
            DebugTag,
            "render id=${movieDetails.id} name=${movieDetails.name} seasonCount=${movieDetails.seasonCount} episodeCount=${movieDetails.episodeCount} seasons=${movieDetails.seasons.size} cast=${movieDetails.cast.size} similar=${movieDetails.similarMovies.size}"
        )
    }

    LaunchedEffect(actionsCompat, context) {
        refreshDownloadSnapshot(reason = "enter_details")
    }

    DisposableEffect(downloadEpisodeId) {
        val targetId = downloadEpisodeId
        if (targetId == null) {
            onDispose { }
        } else {
            val statusObserver: (Pair<Int, VideoDownloadManager.DownloadType>) -> Unit = { (id, status) ->
                if (id == targetId) {
                    coroutineScope.launch {
                        Log.d(
                            DebugTag,
                            "download status event: id=$id status=$status currentProgress=${(downloadProgressFraction * 100f).toInt()}%"
                        )
                        downloadStatus = status
                        if (status == VideoDownloadManager.DownloadType.IsDone) {
                            downloadProgressFraction = 1f
                        } else if (status == VideoDownloadManager.DownloadType.IsStopped ||
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
        enabled = !isActionsPanelVisible && !isBookmarkPanelVisible && !isDownloadPanelVisible,
        onBack = onBackPressed
    )

    Box(modifier = modifier) {
        MovieDetailsBackdrop(
            posterUri = movieDetails.posterUri,
            title = movieDetails.name,
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
                    movieDetails = movieDetails,
                    goToMoviePlayer = goToMoviePlayer,
                    playButtonLabel = movieDetailsPlayLabel(movieDetails),
                    titleMetadata = movieDetailsTitleMetadata(movieDetails),
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
                            else -> Unit
                        }
                    }
                )
            }

            item {
                CastAndCrewList(
                    castAndCrew = movieDetails.cast
                )
            }

            if (movieDetails.similarMovies.isNotEmpty()) {
                item {
                    MoviesRow(
                        title = StringConstants
                            .Composable
                            .movieDetailsScreenSimilarTo(movieDetails.name),
                        titleStyle = MaterialTheme.typography.titleMedium,
                        movieList = movieDetails.similarMovies,
                        itemDirection = ItemDirection.Horizontal,
                        onMovieSelected = refreshScreenWithNewMovie
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = childPadding.start)
                        .padding(BottomDividerPadding)
                        .fillMaxWidth()
                        .height(1.dp)
                        .alpha(0.15f)
                        .background(MaterialTheme.colorScheme.onSurface)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = childPadding.start),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val itemModifier = Modifier.width(192.dp)

                    TitleValueText(
                        modifier = itemModifier,
                        title = stringResource(R.string.status),
                        value = movieDetails.status
                    )
                    TitleValueText(
                        modifier = itemModifier,
                        title = stringResource(R.string.status),
                        value = movieDetails.originalLanguage
                    )
                    TitleValueText(
                        modifier = itemModifier,
                        title = stringResource(R.string.status),
                        value = movieDetails.budget
                    )
                    TitleValueText(
                        modifier = itemModifier,
                        title = stringResource(R.string.status),
                        value = movieDetails.revenue
                    )
                }
            }
        }

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
            panelTestTag = "movie_download_sources_side_panel",
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

        BookmarkStatusSidePanel(
            visible = isBookmarkPanelVisible,
            currentStatus = movieDetails.bookmarkLabelRes.toWatchType(),
            onCloseRequested = { closeBookmarkPanel() },
            onBookmarkSelected = { selectedStatus ->
                onBookmarkClick(selectedStatus)
                closeBookmarkPanel()
            },
            panelTestTag = "movie_bookmark_side_panel"
        )
    }
}

private val BottomDividerPadding = PaddingValues(vertical = 48.dp)

@Composable
private fun movieDetailsPlayLabel(movieDetails: MovieDetails): String {
    val seasonShort = stringResource(R.string.season_short)
    val episodeShort = stringResource(R.string.episode_short)
    val episodeLabel = stringResource(R.string.episode)
    val movieFallback = stringResource(R.string.movies_singular)
    val seriesFallback = stringResource(R.string.tv_series_singular)

    val currentEpisode = movieDetails.currentEpisode
    val currentSeason = movieDetails.currentSeason

    if (currentEpisode != null) {
        return if (currentSeason != null) {
            "$seasonShort$currentSeason:$episodeShort$currentEpisode"
        } else {
            "$episodeLabel $currentEpisode"
        }
    }

    val hasSeriesMetadata = movieDetails.seasonCount != null || movieDetails.episodeCount != null
    return if (hasSeriesMetadata) seriesFallback else movieFallback
}

@Composable
private fun movieDetailsTitleMetadata(movieDetails: MovieDetails): List<String> {
    val seasonLabel = stringResource(R.string.season)
    val episodesLabel = stringResource(R.string.episodes)

    return listOfNotNull(
        movieDetails.seasonCount?.let { "$seasonLabel $it" },
        movieDetails.episodeCount?.let { "$episodesLabel $it" }
    )
}

@Composable
fun MovieActionsSidePanel(
    visible: Boolean,
    loading: Boolean,
    inProgress: Boolean,
    title: String,
    items: List<MovieDetailsCompatPanelItem>,
    onCloseRequested: () -> Unit,
    onActionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    panelTestTag: String = "movie_actions_side_panel",
    showItemsWhileLoading: Boolean = false,
    headerContent: (@Composable ColumnScope.() -> Unit)? = null,
    emptyContent: (@Composable () -> Unit)? = null,
) {
    val menuItems = if (loading && !showItemsWhileLoading) {
        emptyList()
    } else {
        items.map { action ->
            SidePanelMenuItem(
                id = "movie_action_${action.id}",
                title = action.label,
                enabled = if (loading && showItemsWhileLoading) true else !inProgress,
                onClick = { onActionSelected(action.id) },
                leadingContent = {
                    Icon(
                        painter = painterResource(id = action.iconRes),
                        contentDescription = null
                    )
                }
            )
        }
    }

    MenuListSidePanel(
        visible = visible,
        onCloseRequested = onCloseRequested,
        title = title,
        items = menuItems,
        panelWidth = 340.dp,
        modifier = modifier,
        panelTestTag = panelTestTag,
        initialFocusedItemId = menuItems.firstOrNull()?.id,
        headerContent = headerContent,
        emptyContent = {
            if (emptyContent != null) {
                emptyContent()
            } else {
                val text = if (loading) {
                    stringResource(R.string.loading)
                } else {
                    stringResource(R.string.no_links_found_toast)
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    )
}

@Composable
fun BookmarkStatusSidePanel(
    visible: Boolean,
    currentStatus: WatchType,
    onCloseRequested: () -> Unit,
    onBookmarkSelected: (WatchType) -> Unit,
    modifier: Modifier = Modifier,
    panelTestTag: String = "bookmark_side_panel",
) {
    val menuItems = WatchType.entries.map { watchType ->
        SidePanelMenuItem(
            id = "watch_type_${watchType.internalId}",
            title = stringResource(watchType.stringRes),
            selected = watchType == currentStatus,
            onClick = { onBookmarkSelected(watchType) },
            leadingContent = {
                Icon(
                    painter = painterResource(id = watchType.iconRes),
                    contentDescription = null
                )
            }
        )
    }

    MenuListSidePanel(
        visible = visible,
        onCloseRequested = onCloseRequested,
        title = stringResource(R.string.action_add_to_bookmarks),
        items = menuItems,
        panelWidth = 340.dp,
        modifier = modifier,
        panelTestTag = panelTestTag,
        initialFocusedItemId = menuItems.firstOrNull { it.selected }?.id ?: menuItems.firstOrNull()?.id,
        showSelectionRadio = true
    )
}

private fun Int?.toWatchType(): WatchType {
    return WatchType.entries.firstOrNull { watchType -> watchType.stringRes == this } ?: WatchType.NONE
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
