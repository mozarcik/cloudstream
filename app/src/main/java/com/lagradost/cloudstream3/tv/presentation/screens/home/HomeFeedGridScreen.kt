package com.lagradost.cloudstream3.tv.presentation.screens.home

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.compat.home.FeedRepositoryImpl
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.common.HaloHost
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect

private const val HomeGridFocusDebugTag = "TvHomeFocus"

@Composable
fun HomeFeedGridScreen(
    onMediaClick: (MediaItemCompat) -> Unit,
    onBack: () -> Unit,
    onScroll: (Boolean) -> Unit,
    restoreFocusToken: Int = 0,
    mediaGridViewModel: MediaGridViewModel = viewModel(
        factory = MediaGridViewModelFactory(FeedRepositoryImpl())
    ),
) {
    val selectedFeed by HomeFeedGridSelectionStore.selectedFeed.collectAsState()
    val pagingItems = mediaGridViewModel.pagingData.collectAsLazyPagingItems()
    val gridState = rememberLazyGridState()
    val firstItemFocusRequester = remember { FocusRequester() }
    val pendingRestoreTargetId = HomeFeedGridSelectionStore.pendingRestoreTargetId
    val pendingRestoreIndex = HomeFeedGridSelectionStore.pendingRestoreIndex
    var armedRestoreToken by rememberSaveable(selectedFeed?.id) { mutableStateOf(0) }
    var armedRestoreTargetId by rememberSaveable(selectedFeed?.id) { mutableStateOf<String?>(null) }
    var armedRestoreIndex by rememberSaveable(selectedFeed?.id) { mutableStateOf<Int?>(null) }
    var hasInitialFocusBeenHandled by rememberSaveable(selectedFeed?.id) { mutableStateOf(false) }

    if (selectedFeed == null) {
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    LaunchedEffect(Unit) {
        onScroll(true)
    }

    LaunchedEffect(restoreFocusToken) {
        if (restoreFocusToken <= 0 || restoreFocusToken == armedRestoreToken) {
            return@LaunchedEffect
        }

        armedRestoreToken = restoreFocusToken
        armedRestoreTargetId = HomeFeedGridSelectionStore.pendingRestoreTargetId
        armedRestoreIndex = HomeFeedGridSelectionStore.pendingRestoreIndex
        Log.d(
            HomeGridFocusDebugTag,
            "HomeFeedGridScreen arm restore token=$armedRestoreToken target=$armedRestoreTargetId index=$armedRestoreIndex"
        )
    }

    LaunchedEffect(selectedFeed?.id) {
        selectedFeed?.let { feed ->
            Log.d(
                HomeGridFocusDebugTag,
                "HomeFeedGridScreen selectFeed id=${feed.id} pendingTarget=$pendingRestoreTargetId pendingIndex=$pendingRestoreIndex restoreToken=$restoreFocusToken"
            )
            mediaGridViewModel.selectFeed(feed)
        }
    }

    FocusRequestEffect(
        requester = firstItemFocusRequester,
        requestKey = selectedFeed?.id,
        enabled = selectedFeed != null &&
            pagingItems.itemCount > 0 &&
            pendingRestoreTargetId == null &&
            armedRestoreTargetId == null &&
            !hasInitialFocusBeenHandled,
        onFocused = {
            hasInitialFocusBeenHandled = true
            Log.d(
                HomeGridFocusDebugTag,
                "HomeFeedGridScreen initial focus first item feed=${selectedFeed?.id} itemCount=${pagingItems.itemCount}"
            )
        }
    )

    HaloHost(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = selectedFeed?.name.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 12.dp, end = 24.dp)
            )

            MediaGrid(
                pagingItems = pagingItems,
                onMediaClick = { item ->
                    HomeFeedGridSelectionStore.scheduleRestoreToLastFocused()
                    onMediaClick(item)
                },
                gridState = gridState,
                firstItemFocusRequester = firstItemFocusRequester,
                focusKeyPrefix = "home_feed_grid",
                pendingRestoreFocusTargetId = armedRestoreTargetId,
                pendingRestoreFocusIndex = armedRestoreIndex,
                restoreFocusToken = armedRestoreToken,
                onItemFocused = HomeFeedGridSelectionStore::onTargetFocused,
                onRestoreFocusConsumed = { targetId ->
                    hasInitialFocusBeenHandled = true
                    HomeFeedGridSelectionStore.clearPendingRestore(targetId)
                    armedRestoreTargetId = null
                    armedRestoreIndex = null
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 56.dp)
            )
        }
    }
}
