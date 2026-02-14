package com.lagradost.cloudstream3.tv.presentation.screens.movies

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.util.StringConstants
import com.lagradost.cloudstream3.tv.icons.CustomDownload
import com.lagradost.cloudstream3.tv.presentation.common.ActionIconSpec
import com.lagradost.cloudstream3.tv.presentation.common.ActionIconsPill
import com.lagradost.cloudstream3.tv.presentation.utils.bringIntoViewIfChildrenAreFocused
import com.lagradost.cloudstream3.tv.presentation.utils.Padding
import kotlin.math.roundToInt

val ParentPadding = PaddingValues(vertical = 16.dp, horizontal = 58.dp)

enum class MovieDetailsQuickAction {
    Bookmark,
    Favorite,
    Search,
    Download,
    More,
}

sealed interface MovieDetailsDownloadActionState {
    data object Idle : MovieDetailsDownloadActionState
    data class Downloading(val progress: Float) : MovieDetailsDownloadActionState
    data object Downloaded : MovieDetailsDownloadActionState
}

@Composable
fun rememberChildPadding(direction: LayoutDirection = LocalLayoutDirection.current): Padding {
    return remember {
        Padding(
            start = ParentPadding.calculateStartPadding(direction) + 8.dp,
            top = ParentPadding.calculateTopPadding(),
            end = ParentPadding.calculateEndPadding(direction) + 8.dp,
            bottom = ParentPadding.calculateBottomPadding()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MovieDetails(
    movieDetails: MovieDetails,
    goToMoviePlayer: () -> Unit,
    playButtonLabel: String? = null,
    titleMetadata: List<String> = emptyList(),
    downloadActionState: MovieDetailsDownloadActionState = MovieDetailsDownloadActionState.Idle,
    onPrimaryActionsFocused: () -> Unit = {},
    onQuickActionClick: (MovieDetailsQuickAction) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val childPadding = rememberChildPadding()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val focusRequester = remember { FocusRequester() }
    val heroSectionHeight = LocalConfiguration.current.screenHeightDp.dp * 0.9f

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroSectionHeight)
        ) {
            Spacer(modifier = Modifier.weight(1f, fill = true))

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .padding(start = childPadding.start)
            ) {
                MovieLargeTitle(movieTitle = movieDetails.name)

                if (titleMetadata.isNotEmpty()) {
                    DotSeparatedRow(
                        modifier = Modifier.padding(top = 8.dp),
                        texts = titleMetadata
                    )
                }

                Column(
                    modifier = Modifier.alpha(0.75f)
                ) {
                    val detailsRowTexts = listOf(
                        movieDetails.releaseDate,
                        movieDetails.duration,
                        movieDetails.categories.joinToString(", ")
                    ).filter { it.isNotBlank() }

                    if (detailsRowTexts.isNotEmpty()) {
                        DotSeparatedRow(
                            modifier = Modifier.padding(top = 20.dp),
                            texts = detailsRowTexts
                        )
                    }
                    MovieDescription(description = movieDetails.description)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailsActionsRow(
                modifier = Modifier
                    .padding(start = childPadding.start)
                    .padding(bottom = 24.dp),
                onPlayClick = goToMoviePlayer,
                playButtonLabel = playButtonLabel,
                isFavorite = movieDetails.isFavorite,
                isBookmarked = movieDetails.isBookmarked,
                bookmarkLabelRes = movieDetails.bookmarkLabelRes,
                downloadActionState = downloadActionState,
                onFocused = onPrimaryActionsFocused,
                onQuickActionClick = onQuickActionClick
            )
        }
    }
}

@Composable
private fun DetailsActionsRow(
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    playButtonLabel: String? = null,
    isFavorite: Boolean = false,
    isBookmarked: Boolean = false,
    bookmarkLabelRes: Int? = null,
    downloadActionState: MovieDetailsDownloadActionState = MovieDetailsDownloadActionState.Idle,
    onFocused: () -> Unit = {},
    onQuickActionClick: (MovieDetailsQuickAction) -> Unit = {},
) {
    Row(
        modifier = modifier
            .bringIntoViewIfChildrenAreFocused()
            .onFocusChanged { focusState ->
                if (focusState.hasFocus) {
                    onFocused()
                }
            },
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PrimaryPlayButton(
            goToMoviePlayer = onPlayClick,
            playButtonLabel = playButtonLabel
        )

        val favoriteLabel = stringResource(R.string.favorite)
        val searchLabel = stringResource(R.string.search)
        val downloadIdleLabel = stringResource(R.string.download)
        val downloadingLabel = stringResource(R.string.downloading)
        val downloadedLabel = stringResource(R.string.downloaded)
        val bookmarkLabel = bookmarkLabelRes?.let { stringResource(it) } ?: "Bookmark"
        val downloadProgressFraction = when (downloadActionState) {
            MovieDetailsDownloadActionState.Idle -> 0f
            is MovieDetailsDownloadActionState.Downloading ->
                downloadActionState.progress.coerceIn(0f, 1f)

            MovieDetailsDownloadActionState.Downloaded -> 1f
        }

        val downloadLabel = when (downloadActionState) {
            MovieDetailsDownloadActionState.Idle -> downloadIdleLabel
            is MovieDetailsDownloadActionState.Downloading ->
                "$downloadingLabel (${(downloadProgressFraction * 100f).roundToInt()}%)"

            MovieDetailsDownloadActionState.Downloaded -> downloadedLabel
        }

        val downloadIcon = when (downloadActionState) {
            MovieDetailsDownloadActionState.Idle -> Icons.Filled.CustomDownload
            is MovieDetailsDownloadActionState.Downloading -> Icons.Default.Downloading
            MovieDetailsDownloadActionState.Downloaded -> Icons.Default.DownloadDone
        }
        val actions = remember(
            bookmarkLabel,
            favoriteLabel,
            searchLabel,
            downloadLabel,
            downloadIcon,
            downloadProgressFraction,
            isBookmarked,
            isFavorite,
        ) {
            listOf(
                ActionIconSpec(
                    icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    label = bookmarkLabel,
                    testTag = "action_bookmark",
                    action = MovieDetailsQuickAction.Bookmark
                ),
                ActionIconSpec(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    label = favoriteLabel,
                    testTag = "action_favorite",
                    action = MovieDetailsQuickAction.Favorite
                ),
                ActionIconSpec(
                    icon = Icons.Default.Search,
                    label = searchLabel,
                    testTag = "action_search",
                    action = MovieDetailsQuickAction.Search
                ),
                ActionIconSpec(
                    icon = downloadIcon,
                    label = downloadLabel,
                    testTag = "action_download",
                    action = MovieDetailsQuickAction.Download,
                    progressFraction = downloadProgressFraction
                ),
                ActionIconSpec(
                    icon = Icons.Default.MoreVert,
                    label = "More",
                    testTag = "action_more",
                    action = MovieDetailsQuickAction.More
                )
            )
        }

        ActionIconsPill(
            actions = actions,
            onActionClick = onQuickActionClick
        )
    }
}

@Composable
private fun PrimaryPlayButton(
    goToMoviePlayer: () -> Unit,
    modifier: Modifier = Modifier,
    playButtonLabel: String? = null,
) {
    val label = playButtonLabel ?: stringResource(R.string.movies_singular)

    Button(
        onClick = goToMoviePlayer,
        modifier = modifier,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
private fun MovieDescription(description: String) {
    Text(
        text = description,
        style = MaterialTheme.typography.titleSmall.copy(
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal
        ),
        modifier = Modifier.padding(top = 8.dp),
        maxLines = 10
    )
}

@Composable
private fun MovieLargeTitle(movieTitle: String) {
    Text(
        text = movieTitle,
        style = MaterialTheme.typography.displayMedium.copy(
            fontWeight = FontWeight.Bold
        ),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun MovieDetailsBackdrop(
    posterUri: String,
    title: String,
    modifier: Modifier = Modifier,
    gradientColor: Color = MaterialTheme.colorScheme.surface,
    applyBlur: Boolean = false,
) {
    val backdropModifier = if (applyBlur) {
        modifier.blur(22.dp)
    } else {
        modifier
    }
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(posterUri)
            .crossfade(true).build(),
        contentDescription = StringConstants
            .Composable
            .ContentDescription
            .moviePoster(title),
        contentScale = ContentScale.Crop,
        modifier = backdropModifier.drawWithContent {
            drawContent()
            drawRect(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, gradientColor),
                    startY = size.height * 0.6f
                )
            )
            drawRect(
                Brush.horizontalGradient(
                    colors = listOf(gradientColor, Color.Transparent),
                    endX = size.width * 0.5f,
                    startX = 0f
                )
            )
            drawRect(
                Brush.linearGradient(
                    colors = listOf(gradientColor, Color.Transparent),
                    start = Offset(x = 0f, y = size.height),
                    end = Offset(x = size.width * 0.7f, y = size.height * 0.3f)
                )
            )
        }
    )
}

@Composable
fun MovieDetailsLoadingPlaceholder(
    title: String,
    posterUri: String? = null,
    backdropUri: String? = null,
    modifier: Modifier = Modifier,
) {
    val childPadding = rememberChildPadding()
    val heroSectionHeight = LocalConfiguration.current.screenHeightDp.dp * 0.9f
    val placeholderBlock = Color.White.copy(alpha = 0.14f)
    val loadingBackdropUri = backdropUri?.takeIf { it.isNotBlank() }
    val loadingPosterUri = posterUri?.takeIf { it.isNotBlank() }
    val artworkUri = loadingBackdropUri ?: loadingPosterUri

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (artworkUri.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1A2332),
                                Color(0xFF121924),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        } else {
            MovieDetailsBackdrop(
                posterUri = artworkUri,
                title = title,
                modifier = Modifier.fillMaxSize(),
                gradientColor = MaterialTheme.colorScheme.background,
                applyBlur = loadingBackdropUri.isNullOrBlank()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroSectionHeight)
        ) {
            Spacer(modifier = Modifier.weight(1f, fill = true))

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .padding(start = childPadding.start)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))
                PlaceholderLine(widthFraction = 0.9f, color = placeholderBlock)
                Spacer(modifier = Modifier.height(6.dp))
                PlaceholderLine(widthFraction = 0.82f, color = placeholderBlock)
                Spacer(modifier = Modifier.height(6.dp))
                PlaceholderLine(widthFraction = 0.68f, color = placeholderBlock)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(start = childPadding.start)
                    .padding(bottom = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(164.dp)
                        .height(48.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        )
                )

                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .height(48.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.12f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun PlaceholderLine(
    widthFraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(18.dp)
            .background(
                color = color,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
    )
}
