package com.lagradost.cloudstream3.tv.presentation.common

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.data.entities.Movie
import com.lagradost.cloudstream3.tv.data.entities.MovieList
import com.lagradost.cloudstream3.tv.presentation.screens.movies.rememberChildPadding
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamCardShape

private const val PosterMoviesRowVisibleItemCount = 4

private val PosterMoviesRowItemSpacing = 20.dp
private val PosterMoviesRowTitleTopPadding = 16.dp
private val PosterMoviesRowTitleBottomPadding = 16.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PosterMoviesRow(
    movieList: MovieList,
    modifier: Modifier = Modifier,
    startPadding: Dp = rememberChildPadding().start,
    endPadding: Dp = rememberChildPadding().end,
    title: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.headlineLarge.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp
    ),
    onMovieSelected: (movie: Movie) -> Unit = {},
) {
    val (lazyRow, firstItem) = remember { FocusRequester.createRefs() }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val interItemSpacing = PosterMoviesRowItemSpacing * (PosterMoviesRowVisibleItemCount - 1)
    val itemWidth = (screenWidth - startPadding - endPadding - interItemSpacing) /
        PosterMoviesRowVisibleItemCount

    Column(modifier = modifier.focusGroup()) {
        title?.let { rowTitle ->
            Text(
                text = rowTitle,
                style = titleStyle,
                modifier = Modifier.padding(
                    start = startPadding,
                    top = PosterMoviesRowTitleTopPadding,
                    bottom = PosterMoviesRowTitleBottomPadding
                )
            )
        }

        LazyRow(
            contentPadding = PaddingValues(
                start = startPadding,
                end = endPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(PosterMoviesRowItemSpacing),
            modifier = Modifier
                .focusRequester(lazyRow)
                .focusRestorer(firstItem)
        ) {
            itemsIndexed(
                items = movieList,
                key = { _, movie -> movie.id }
            ) { index, movie ->
                val itemModifier = if (index == 0) {
                    Modifier.focusRequester(firstItem)
                } else {
                    Modifier
                }

                PosterMoviesRowItem(
                    movie = movie,
                    index = index,
                    onMovieSelected = {
                        lazyRow.saveFocusedChild()
                        onMovieSelected(it)
                    },
                    modifier = itemModifier.width(itemWidth)
                )
            }
        }
    }
}

@Composable
private fun PosterMoviesRowItem(
    movie: Movie,
    index: Int,
    onMovieSelected: (Movie) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haloController = LocalHaloController.current
    val isHaloEnabled = LocalHaloEnabled.current
    val haloKey = remember(movie.id, movie.posterUri, movie.name) {
        Any()
    }
    val posterModel = remember(movie.posterUri, movie.id) {
        movie.posterUri.takeIf { posterUri -> posterUri.isNotBlank() } ?: movie.id
    }

    PosterCard(
        model = posterModel,
        title = movie.name,
        ratingLabel = null,
        shape = CloudStreamCardShape,
        onClick = { onMovieSelected(movie) },
        onFocus = { focusInfo ->
            if (!isHaloEnabled) {
                return@PosterCard
            }

            if (focusInfo.isFocused) {
                haloController.onItemFocused(
                    key = haloKey,
                    rectInRoot = focusInfo.boundsInRoot,
                    color = focusInfo.accentColor
                )
            } else {
                haloController.onItemFocusCleared(haloKey)
            }
        },
        modifier = modifier.focusProperties {
            left = if (index == 0) {
                FocusRequester.Cancel
            } else {
                FocusRequester.Default
            }
        }
    )
}
