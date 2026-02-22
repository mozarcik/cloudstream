package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat

@Composable
fun FeedSection(
    title: String,
    state: HomeFeedLoadState,
    onMediaClick: (MediaItemCompat) -> Unit,
    onShowMoreClick: () -> Unit,
    isInteractive: Boolean,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
    firstItemFocusRequester: FocusRequester? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val cardWidth = (
            maxWidth -
                (FeedGridHorizontalPadding * 2) -
                (FeedGridColumnSpacing * (FEED_GRID_COLUMNS - 1))
            ) / FEED_GRID_COLUMNS
        val cardPosterHeight = cardWidth / FEED_POSTER_ASPECT_RATIO
        val cardHeight = cardPosterHeight + FeedCardTitleHeight
        val successGridConfig = (state as? HomeFeedLoadState.Success)?.let { success ->
            resolveFeedMiniGridConfig(success.items)
        }
        val rowCount = when (state) {
            HomeFeedLoadState.Error -> 1
            else -> successGridConfig?.rowCount ?: FEED_GRID_ROWS
        }
        val gridHeight = gridHeightForRows(cardHeight, rowCount)
        val sectionHeight = gridHeight + FeedSectionHeaderHeight
        val resolvedErrorMessage = errorMessage ?: stringResource(R.string.tv_home_failed_to_load)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(sectionHeight)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            when (state) {
                HomeFeedLoadState.Loading -> {
                    FeedMiniGridPlaceholder(
                        cardWidth = cardWidth,
                        cardHeight = cardPosterHeight,
                        gridHeight = gridHeight,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HomeFeedLoadState.Error -> {
                    FeedMiniGridEmptyOrError(
                        gridHeight = gridHeight,
                        isInteractive = isInteractive,
                        text = resolvedErrorMessage,
                        modifier = Modifier.fillMaxWidth(),
                        firstItemFocusRequester = firstItemFocusRequester,
                    )
                }

                is HomeFeedLoadState.Success -> {
                    FeedMiniGrid(
                        items = state.items,
                        onMediaClick = onMediaClick,
                        onShowMoreClick = onShowMoreClick,
                        isInteractive = isInteractive,
                        firstItemFocusRequester = firstItemFocusRequester,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        cardPosterHeight = cardPosterHeight,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
