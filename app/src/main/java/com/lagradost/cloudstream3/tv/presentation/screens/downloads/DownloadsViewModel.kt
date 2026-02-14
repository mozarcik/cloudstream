package com.lagradost.cloudstream3.tv.presentation.screens.downloads

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.isEpisodeBased
import com.lagradost.cloudstream3.utils.DOWNLOAD_EPISODE_CACHE
import com.lagradost.cloudstream3.utils.DOWNLOAD_HEADER_CACHE
import com.lagradost.cloudstream3.utils.DataStore.getFolderName
import com.lagradost.cloudstream3.utils.DataStore.getKey
import com.lagradost.cloudstream3.utils.DataStore.getKeys
import com.lagradost.cloudstream3.utils.VideoDownloadHelper
import com.lagradost.cloudstream3.utils.VideoDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class)
class DownloadsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var appContext: Context? = null
    private var periodicRefreshJob: Job? = null

    private data class ProgressSample(
        val downloadedBytes: Long,
        val timestampMs: Long,
    )

    private val speedBytesPerSecondById = mutableMapOf<Int, Long>()
    private val progressSamplesById = mutableMapOf<Int, ProgressSample>()
    private val lastUpdatedAtById = mutableMapOf<Int, Long>()

    private val statusObserver: (Pair<Int, VideoDownloadManager.DownloadType>) -> Unit =
        { (id, _) ->
            lastUpdatedAtById[id] = System.currentTimeMillis()
            refreshRequests.tryEmit(Unit)
        }

    private val progressObserver: (Triple<Int, Long, Long>) -> Unit = { (id, downloaded, _) ->
        val now = SystemClock.elapsedRealtime()
        val previous = progressSamplesById[id]
        if (previous != null) {
            val deltaBytes = (downloaded - previous.downloadedBytes).coerceAtLeast(0L)
            val deltaMs = (now - previous.timestampMs).coerceAtLeast(1L)
            val speed = ((deltaBytes * 1000L) / deltaMs).coerceAtLeast(0L)
            if (speed > 0L) {
                speedBytesPerSecondById[id] = speed
            }
        }

        progressSamplesById[id] = ProgressSample(
            downloadedBytes = downloaded,
            timestampMs = now
        )
        lastUpdatedAtById[id] = System.currentTimeMillis()
        refreshRequests.tryEmit(Unit)
    }

    private val deleteObserver: (Int) -> Unit = { id ->
        speedBytesPerSecondById.remove(id)
        progressSamplesById.remove(id)
        lastUpdatedAtById.remove(id)
        refreshRequests.tryEmit(Unit)
    }

    init {
        VideoDownloadManager.downloadStatusEvent += statusObserver
        VideoDownloadManager.downloadProgressEvent += progressObserver
        VideoDownloadManager.downloadDeleteEvent += deleteObserver

        viewModelScope.launch {
            refreshRequests
                .debounce(120)
                .collect {
                    refreshUiState()
                }
        }
    }

    fun bind(context: Context) {
        val newContext = context.applicationContext
        if (appContext !== newContext) {
            appContext = newContext
            startPeriodicRefresh()
        }
        refreshRequests.tryEmit(Unit)
    }

    private fun startPeriodicRefresh() {
        periodicRefreshJob?.cancel()
        periodicRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(2_500L)
                refreshRequests.tryEmit(Unit)
            }
        }
    }

    private suspend fun refreshUiState() {
        val context = appContext ?: return
        val state = withContext(Dispatchers.IO) {
            buildDownloadsState(context)
        }
        _uiState.value = state
    }

    private fun buildDownloadsState(
        context: Context
    ): DownloadsUiState {
        val headersById = context.getKeys(DOWNLOAD_HEADER_CACHE)
            .mapNotNull { key ->
                context.getKey<VideoDownloadHelper.DownloadHeaderCached>(key)
            }
            .associateBy { header -> header.id }

        val cachedEpisodes = context.getKeys(DOWNLOAD_EPISODE_CACHE)
            .mapNotNull { key ->
                context.getKey<VideoDownloadHelper.DownloadEpisodeCached>(key)
            }

        val movieSyntheticEpisodes = headersById.values
            .asSequence()
            .filter { header -> !header.type.isEpisodeBased() }
            .mapNotNull { header ->
                context.getKey<VideoDownloadHelper.DownloadEpisodeCached>(
                    DOWNLOAD_EPISODE_CACHE,
                    getFolderName(header.id.toString(), header.id.toString())
                )
            }
            .toList()

        val episodes = (cachedEpisodes + movieSyntheticEpisodes).distinctBy { episode -> episode.id }

        val downloadItems = episodes.mapNotNull { episode ->
            val header = headersById[episode.parentId] ?: return@mapNotNull null
            val fileInfo = VideoDownloadManager.getDownloadFileInfoAndUpdateSettings(context, episode.id)
            val downloadedBytes = fileInfo?.fileLength ?: 0L
            val totalBytes = fileInfo?.totalBytes ?: 0L
            val hasPendingRequest = hasPendingDownloadRequest(context, episode.id)
            val status = normalizeDownloadStatus(
                status = VideoDownloadManager.downloadStatus[episode.id],
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                hasPendingRequest = hasPendingRequest
            )
            val progressFraction = calculateProgressFraction(downloadedBytes, totalBytes)
            val speedBytesPerSec = speedBytesPerSecondById[episode.id]
            val etaSeconds = if (speedBytesPerSec != null && speedBytesPerSec > 0L && totalBytes > downloadedBytes) {
                ((totalBytes - downloadedBytes) / speedBytesPerSec).coerceAtLeast(1L)
            } else {
                null
            }

            val state = when {
                status == VideoDownloadManager.DownloadType.IsDone || progressFraction >= 0.999f -> {
                    DownloadState.Downloaded(fileSizeBytes = downloadedBytes)
                }

                status == VideoDownloadManager.DownloadType.IsPaused -> {
                    DownloadState.Paused(
                        progress = progressFraction,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes.takeIf { it > 0L },
                    )
                }

                status == VideoDownloadManager.DownloadType.IsFailed ||
                    status == VideoDownloadManager.DownloadType.IsStopped -> {
                    DownloadState.Failed(errorMessage = null)
                }

                status == VideoDownloadManager.DownloadType.IsDownloading ||
                    status == VideoDownloadManager.DownloadType.IsPending -> {
                    DownloadState.Downloading(
                        progress = progressFraction,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes.takeIf { it > 0L },
                        speedBytesPerSec = speedBytesPerSec,
                        etaSeconds = etaSeconds
                    )
                }

                else -> {
                    if (downloadedBytes > 0L) {
                        DownloadState.Downloaded(fileSizeBytes = downloadedBytes)
                    } else {
                        return@mapNotNull null
                    }
                }
            }

            val updatedAt = maxOf(
                episode.cacheTime,
                lastUpdatedAtById[episode.id] ?: 0L
            )

            DownloadItemUiModel(
                id = episode.id.toString(),
                episodeId = episode.id,
                parentId = episode.parentId,
                title = resolveDownloadItemTitle(header = header, episode = episode),
                episodeName = episode.name,
                episodeNumber = episode.episode.takeIf { it > 0 },
                seasonNumber = episode.season,
                description = episode.description,
                posterUrl = episode.poster ?: header.poster,
                backdropUrl = header.backdrop,
                mediaType = header.type.toDownloadMediaType(),
                sourceUrl = header.url,
                apiName = header.apiName,
                state = state,
                startedAtMillis = episode.cacheTime,
                updatedAtMillis = updatedAt,
                downloadedAtMillis = updatedAt.takeIf { state is DownloadState.Downloaded }
            )
        }

        return DownloadsUiState(
            downloadingItems = downloadItems
                .filter { item -> item.state !is DownloadState.Downloaded }
                .sortedByDescending { item -> item.startedAtMillis },
            downloadedItems = downloadItems
                .filter { item -> item.state is DownloadState.Downloaded }
                .sortedByDescending { item -> item.downloadedAtMillis ?: item.startedAtMillis }
        )
    }

    private fun hasPendingDownloadRequest(
        context: Context,
        episodeId: Int,
    ): Boolean {
        val key = episodeId.toString()
        val hasWorkerInfo = context.getKey<VideoDownloadManager.DownloadInfo>(
            VideoDownloadManager.WORK_KEY_INFO,
            key
        ) != null
        val hasWorkerPackage = context.getKey<VideoDownloadManager.DownloadResumePackage>(
            VideoDownloadManager.WORK_KEY_PACKAGE,
            key
        ) != null
        val hasResumePackage = VideoDownloadManager.getDownloadResumePackage(
            context,
            episodeId
        ) != null

        return hasWorkerInfo || hasWorkerPackage || hasResumePackage
    }

    private fun normalizeDownloadStatus(
        status: VideoDownloadManager.DownloadType?,
        downloadedBytes: Long,
        totalBytes: Long,
        hasPendingRequest: Boolean,
    ): VideoDownloadManager.DownloadType? {
        if (status != null) return status

        if (hasPendingRequest) {
            return if (downloadedBytes > 0L) {
                VideoDownloadManager.DownloadType.IsDownloading
            } else {
                VideoDownloadManager.DownloadType.IsPending
            }
        }

        if (downloadedBytes > 0L && totalBytes <= 0L) {
            return VideoDownloadManager.DownloadType.IsDownloading
        }

        if (totalBytes <= 0L) {
            return null
        }

        val isDone = downloadedBytes > 1024L &&
            downloadedBytes + 1024L >= totalBytes

        return if (isDone) {
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

    private fun resolveDownloadItemTitle(
        header: VideoDownloadHelper.DownloadHeaderCached,
        episode: VideoDownloadHelper.DownloadEpisodeCached,
    ): String {
        if (!header.type.isEpisodeBased()) {
            return header.name
        }

        val seasonNumber = episode.season
        val episodeNumber = episode.episode.takeIf { it > 0 }
        val episodeLabel = when {
            seasonNumber != null && episodeNumber != null -> "S${seasonNumber}E${episodeNumber}"
            episodeNumber != null -> "E${episodeNumber}"
            else -> null
        }

        val episodeTitle = episode.name?.takeIf { it.isNotBlank() }
        return listOfNotNull(
            header.name,
            episodeLabel,
            episodeTitle
        ).joinToString(" • ")
    }

    private fun TvType.toDownloadMediaType(): DownloadMediaType {
        return when {
            this == TvType.Movie || this == TvType.AnimeMovie -> DownloadMediaType.Movie
            this.isEpisodeBased() -> DownloadMediaType.Series
            else -> DownloadMediaType.Media
        }
    }

    override fun onCleared() {
        periodicRefreshJob?.cancel()
        VideoDownloadManager.downloadStatusEvent -= statusObserver
        VideoDownloadManager.downloadProgressEvent -= progressObserver
        VideoDownloadManager.downloadDeleteEvent -= deleteObserver
        super.onCleared()
    }
}
