package com.lagradost.cloudstream3.phone.presentation.screens.details

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.phone.presentation.theme.LocalPhoneAccent
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneColors
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneText
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionEvent
import com.lagradost.cloudstream3.tv.compat.DownloadMirrorSelectionStateHolder
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatActionOutcome
import com.lagradost.cloudstream3.tv.compat.MovieDetailsCompatDownloadSnapshot
import com.lagradost.cloudstream3.tv.compat.MovieDetailsEpisodeActionsCompat
import com.lagradost.cloudstream3.tv.presentation.screens.player.PlayerStartTarget
import com.lagradost.cloudstream3.utils.downloader.VideoDownloadManager.DownloadType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Download / "More" actions of the details screen. Uses the same compat layer and
 * [DownloadMirrorSelectionStateHolder] as TV; only the container (bottom sheet) differs.
 */
class PhoneDetailsActions(
    private val scope: CoroutineScope,
    private val context: Context,
    private val compat: () -> MovieDetailsEpisodeActionsCompat?,
    private val onPlay: (PlayerStartTarget) -> Unit,
) {
    val mirror = DownloadMirrorSelectionStateHolder(scope)

    /** Bumped after every action so episode rows re-read watched / download state. */
    var refreshKey by mutableIntStateOf(0)
        private set
    var isBusy by mutableStateOf(false)
        private set

    fun openDownload(season: Int? = null, episode: Int? = null) {
        val actions = compat() ?: return
        mirror.onEvent(DownloadMirrorSelectionEvent.Open(actions, context, season, episode))
    }

    fun openMore(season: Int? = null, episode: Int? = null, title: String? = null) {
        val actions = compat() ?: return
        scope.launch {
            val request = withContext(Dispatchers.IO) {
                actions.buildActionMenuRequest(context, season, episode, title) { data ->
                    scope.launch { onPlay(data?.let(PlayerStartTarget::DirectEpisodeData) ?: PlayerStartTarget.Default) }
                }
            }
            mirror.updateSelectionRequest(request)
        }
    }

    fun select(actionId: Int) {
        val request = mirror.uiState.value.selectionRequest ?: return
        if (isBusy) return
        scope.launch {
            isBusy = true
            try {
                when (val outcome = withContext(Dispatchers.IO) { request.onOptionSelected(actionId) }) {
                    MovieDetailsCompatActionOutcome.Completed -> mirror.onEvent(DownloadMirrorSelectionEvent.Close)
                    is MovieDetailsCompatActionOutcome.OpenSelection -> mirror.updateSelectionRequest(outcome.request)
                }
            } finally {
                isBusy = false
                refreshKey++
            }
        }
    }

    fun close() = mirror.onEvent(DownloadMirrorSelectionEvent.Close)
}

@Composable
fun rememberPhoneDetailsActions(
    compat: () -> MovieDetailsEpisodeActionsCompat?,
    onPlay: (PlayerStartTarget) -> Unit,
    context: Context,
): PhoneDetailsActions {
    val scope = rememberCoroutineScope()
    return remember(scope, context) { PhoneDetailsActions(scope, context, compat, onPlay) }
}

/** Download progress of one episode (null season/episode = the movie itself) or null while unknown. */
@Composable
fun rememberDownloadSnapshot(
    actions: PhoneDetailsActions,
    compat: MovieDetailsEpisodeActionsCompat?,
    context: Context,
    season: Int?,
    episode: Int?,
): MovieDetailsCompatDownloadSnapshot? {
    val state = produceState<MovieDetailsCompatDownloadSnapshot?>(null, compat, actions.refreshKey, season, episode) {
        if (compat == null) return@produceState
        while (true) {
            val snapshot = withContext(Dispatchers.IO) { compat.getDownloadSnapshotForEpisode(context, season, episode) }
            value = snapshot
            // Keep polling only while something is moving.
            val active = snapshot?.hasPendingRequest == true || snapshot?.status == DownloadType.IsDownloading || snapshot?.status == DownloadType.IsPending
            if (!active) break
            delay(2_000)
        }
    }
    return state.value
}

@Composable
fun rememberWatched(actions: PhoneDetailsActions, compat: MovieDetailsEpisodeActionsCompat?, season: Int?, episode: Int?): Boolean {
    val state = produceState(false, compat, actions.refreshKey, season, episode) {
        value = compat?.let { withContext(Dispatchers.IO) { it.isEpisodeWatched(season, episode) } } == true
    }
    return state.value
}

fun MovieDetailsCompatDownloadSnapshot?.percent(): Int? = this?.takeIf { totalBytes > 0 }?.let { ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) }

/** Panel shared by "Download" (mirror picker) and "More" (actions list). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsActionSheet(actions: PhoneDetailsActions) {
    val state by actions.mirror.uiState.collectAsStateWithLifecycle()
    if (!state.isVisible && state.selectionRequest == null) return
    val request = state.selectionRequest
    ModalBottomSheet(onDismissRequest = actions::close, containerColor = PhoneColors.Sheet) {
        Text(
            request?.title ?: stringResource(R.string.phone_choose_mirror),
            style = PhoneText.Section, color = PhoneColors.Text,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
        )
        if (state.isLoading && !state.isLoadingUiSkipped) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalPhoneAccent.current)
                Text(stringResource(R.string.phone_loading_sources, state.loadedSourcesCount), style = PhoneText.Meta, color = PhoneColors.Text2, modifier = Modifier.weight(1f))
                // Stop waiting for the remaining providers once some links are in.
                if (state.loadedSourcesCount > 0) {
                    TextButton({ actions.mirror.onEvent(DownloadMirrorSelectionEvent.SkipLoadingUi) }) {
                        Text(stringResource(R.string.phone_skip_loading), style = PhoneText.Button, color = LocalPhoneAccent.current)
                    }
                }
            }
        }
        if (request != null) {
            LazyColumn(Modifier.heightIn(max = 480.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
                itemsIndexed(request.options, key = { index, item -> "${item.key}#$index" }) { _, item ->
                    if (item.isSectionHeader) {
                        Text(item.label.uppercase(), style = PhoneText.Kicker, color = PhoneColors.Label, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp))
                    } else {
                        Column(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = !actions.isBusy, role = Role.Button) { actions.select(item.id) }.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(item.label, style = PhoneText.Row, color = PhoneColors.Text, maxLines = item.titleMaxLines.coerceAtLeast(1))
                            val meta = item.supportingTexts.filter { it.isNotBlank() }.joinToString(" • ")
                            if (meta.isNotEmpty()) Text(meta, style = PhoneText.Meta.copy(fontSize = 12.sp), color = PhoneColors.Text2, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
