package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.tv.compat.home.FeedCategory
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

internal const val HOME_FEED_PRELOAD_SIZE = 20

@Immutable
sealed interface HomeFeedLoadState {
    data object Loading : HomeFeedLoadState
    data object Error : HomeFeedLoadState

    @Immutable
    data class Success(val items: PersistentList<MediaItemCompat>) : HomeFeedLoadState
}

@Immutable
data class HomeFeedSectionUiState(
    val feed: FeedCategory,
    val state: HomeFeedLoadState = HomeFeedLoadState.Loading,
)

@Immutable
data class HomeFeedsUiState(
    val feedSections: PersistentList<HomeFeedSectionUiState> = persistentListOf(),
    val isFeedListLoading: Boolean = true,
)

@Immutable
data class HomeContinueWatchingUiState(
    val state: HomeFeedLoadState = HomeFeedLoadState.Loading,
)
