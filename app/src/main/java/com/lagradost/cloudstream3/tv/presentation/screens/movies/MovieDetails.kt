package com.lagradost.cloudstream3.tv.presentation.screens.movies

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.rememberAsyncImagePainter
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.lagradost.cloudstream3.APIHolder.unixTime
import com.lagradost.cloudstream3.ProviderType
import com.lagradost.cloudstream3.Score
import com.lagradost.cloudstream3.ShowStatus
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.util.StringConstants
import com.lagradost.cloudstream3.tv.icons.CustomDownload
import com.lagradost.cloudstream3.tv.presentation.common.ActionIconSpec
import com.lagradost.cloudstream3.tv.presentation.common.ActionIconsPill
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamBorderWidth
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamCardShape
import com.lagradost.cloudstream3.tv.presentation.theme.CloudStreamSurfaceDefaults
import com.lagradost.cloudstream3.tv.presentation.utils.bringIntoViewIfChildrenAreFocused
import com.lagradost.cloudstream3.tv.presentation.utils.Padding
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

val ParentPadding = PaddingValues(vertical = 16.dp, horizontal = 58.dp)

private const val DetailsDescriptionCollapsedState = "collapsed"
private const val DetailsDescriptionExpandedState = "expanded"
private const val DetailsDescriptionCollapsedMaxLines = 5
private const val DetailsTitleMaxWidthFraction = 0.55f
private val DetailsDescriptionFocusInset = 8.dp

enum class MovieDetailsQuickAction {
    Bookmark,
    Favorite,
    Search,
    Download,
    More,
    MarkAsWatched,
    MarkWatchedUpToThisEpisode,
    RemoveFromWatched,
    RemoveWatchedUpToThisEpisode,
}

sealed interface MovieDetailsDownloadActionState {
    data object Idle : MovieDetailsDownloadActionState
    data class Downloading(val progress: Float) : MovieDetailsDownloadActionState
    data object Downloaded : MovieDetailsDownloadActionState
    data object Failed : MovieDetailsDownloadActionState
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
    downloadActionState: MovieDetailsDownloadActionState = MovieDetailsDownloadActionState.Idle,
    requestInitialPlayButtonFocus: Boolean = true,
    onPrimaryActionsFocused: () -> Unit = {},
    onInitialPlayButtonFocused: () -> Unit = {},
    onQuickActionClick: (MovieDetailsQuickAction) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val childPadding = rememberChildPadding()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val playButtonFocusRequester = remember { FocusRequester() }
    val heroSectionHeight = LocalConfiguration.current.screenHeightDp.dp * 0.9f
    val ratingLabel = detailsRatingLabel(movieDetails.score)
    val statusLabel = detailsStatusLabel(movieDetails.showStatus)
    val nextAiringLabel = detailsNextAiringLabel(movieDetails.nextAiring)
    val providerNotice = if (movieDetails.providerType == ProviderType.MetaProvider) {
        stringResource(R.string.provider_info_meta)
    } else {
        null
    }
    val vpnNotice = when (movieDetails.vpnStatus) {
        com.lagradost.cloudstream3.VPNStatus.None -> null
        com.lagradost.cloudstream3.VPNStatus.MightBeNeeded -> stringResource(R.string.vpn_might_be_needed)
        com.lagradost.cloudstream3.VPNStatus.Torrent -> stringResource(R.string.vpn_torrent)
    }
    val descriptionText = movieDetails.description.ifBlank {
        stringResource(R.string.normal_no_plot)
    }
    val comingSoonLabel = if (movieDetails.comingSoon) {
        stringResource(R.string.coming_soon)
    } else {
        null
    }
    val castSummary = remember(movieDetails.cast) {
        if (movieDetails.cast.any { castMember -> castMember.avatarUrl.isNotBlank() }) {
            null
        } else {
            movieDetails.cast
                .asSequence()
                .map { castMember -> castMember.realName.trim() }
                .filter { castName -> castName.isNotBlank() }
                .distinct()
                .joinToString()
                .takeIf { castNames -> castNames.isNotBlank() }
        }
    }?.let { castNames ->
        stringResource(R.string.cast_format, castNames)
    }
    val primaryMetadataTexts = remember(
        movieDetails.providerName,
        ratingLabel,
        statusLabel,
        movieDetails.pgRating,
    ) {
        listOfNotNull(
            movieDetails.providerName.takeIf { providerName -> providerName.isNotBlank() },
            ratingLabel,
            statusLabel,
            movieDetails.pgRating.takeIf { rating -> rating.isNotBlank() },
        )
    }
    val detailsRowTexts = remember(
        movieDetails.releaseDate,
        movieDetails.duration,
        movieDetails.categories,
    ) {
        listOfNotNull(
            movieDetails.releaseDate.takeIf { releaseDate -> releaseDate.isNotBlank() },
            movieDetails.duration.takeIf { duration -> duration.isNotBlank() },
            movieDetails.categories
                .joinToString(separator = ", ")
                .takeIf { categoriesLabel -> categoriesLabel.isNotBlank() },
        )
    }
    val supportingMessages = remember(
        comingSoonLabel,
        castSummary,
        vpnNotice,
    ) {
        buildList {
            comingSoonLabel?.let(::add)
            castSummary?.let(::add)
            vpnNotice?.let(::add)
        }
    }

    FocusRequestEffect(
        requester = playButtonFocusRequester,
        requestKey = movieDetails.id,
        enabled = requestInitialPlayButtonFocus,
        onFocused = onInitialPlayButtonFocused,
    )

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
                    .fillMaxWidth(DetailsTitleMaxWidthFraction)
                    .padding(start = childPadding.start)
            ) {
                MovieHeroTitle(movieDetails = movieDetails)

                movieDetails.originalTitle
                    ?.takeIf { originalTitle -> originalTitle.isNotBlank() }
                    ?.let { originalTitle ->
                        Text(
                            text = stringResource(R.string.details_original_title_format, originalTitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }

                if (primaryMetadataTexts.isNotEmpty()) {
                    DotSeparatedRow(
                        modifier = Modifier.padding(top = 10.dp),
                        texts = primaryMetadataTexts
                    )
                }

                if (detailsRowTexts.isNotEmpty()) {
                    DotSeparatedRow(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .alpha(0.75f),
                        texts = detailsRowTexts
                    )
                }

                nextAiringLabel?.let { label ->
                    MovieSupportingText(
                        text = label,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                MovieDescription(
                    detailsId = movieDetails.id,
                    description = descriptionText,
                    modifier = Modifier.padding(top = 12.dp)
                )

                supportingMessages.forEach { message ->
                    MovieSupportingText(
                        text = message,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                providerNotice?.let { notice ->
                    MovieSupportingText(
                        text = notice,
                        modifier = Modifier.padding(top = 8.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DetailsActionsRow(
                modifier = Modifier
                    .padding(start = childPadding.start)
                    .padding(bottom = 24.dp),
                playButtonModifier = Modifier.focusRequester(playButtonFocusRequester),
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
    playButtonModifier: Modifier = Modifier,
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
            playButtonLabel = playButtonLabel,
            modifier = playButtonModifier
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
            MovieDetailsDownloadActionState.Failed -> 0f
        }

        val downloadLabel = when (downloadActionState) {
            MovieDetailsDownloadActionState.Idle -> downloadIdleLabel
            is MovieDetailsDownloadActionState.Downloading ->
                "$downloadingLabel (${(downloadProgressFraction * 100f).roundToInt()}%)"

            MovieDetailsDownloadActionState.Downloaded -> downloadedLabel
            MovieDetailsDownloadActionState.Failed -> stringResource(R.string.download_failed)
        }

        val downloadIcon = when (downloadActionState) {
            MovieDetailsDownloadActionState.Idle -> Icons.Filled.CustomDownload
            is MovieDetailsDownloadActionState.Downloading -> Icons.Default.Downloading
            MovieDetailsDownloadActionState.Downloaded -> Icons.Default.DownloadDone
            MovieDetailsDownloadActionState.Failed -> Icons.Outlined.ErrorOutline
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
        modifier = modifier.testTag("details_play_button"),
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        colors = ButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            focusedContainerColor = MaterialTheme.colorScheme.primary,
            focusedContentColor = MaterialTheme.colorScheme.onPrimary,
            pressedContainerColor = MaterialTheme.colorScheme.primary,
            pressedContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
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
private fun MovieHeroTitle(
    movieDetails: MovieDetails,
) {
    MovieLargeTitle(
        movieTitle = movieDetails.name,
        modifier = Modifier.testTag("details_title_text")
    )
}

@Composable
private fun MovieDescription(
    detailsId: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable(detailsId) {
        mutableStateOf(false)
    }
    var hasFocus by remember(detailsId) {
        mutableStateOf(false)
    }
    val focusInset = DetailsDescriptionFocusInset
    val focusBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    val focusBorderColor = MaterialTheme.colorScheme.primary
    val focusBorderWidth = CloudStreamBorderWidth

    Box(
        modifier = modifier
            .drawBehind {
                if (!hasFocus) {
                    return@drawBehind
                }

                drawExpandedFocusDecoration(
                    shape = CloudStreamCardShape,
                    insetPx = focusInset.toPx(),
                    backgroundColor = focusBackgroundColor,
                    borderColor = focusBorderColor,
                    borderWidthPx = focusBorderWidth.toPx(),
                )
            }
    ) {
        Surface(
            onClick = { isExpanded = !isExpanded },
            shape = ClickableSurfaceDefaults.shape(shape = CloudStreamCardShape),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border.None,
                pressedBorder = Border.None,
            ),
            glow = ClickableSurfaceDefaults.glow(
                glow = Glow.None,
                focusedGlow = Glow.None,
                pressedGlow = Glow.None,
            ),
            colors = CloudStreamSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            modifier = Modifier
                .testTag("details_description")
                .onFocusChanged { focusState ->
                    hasFocus = focusState.hasFocus
                }
                .semantics {
                    stateDescription = if (isExpanded) {
                        DetailsDescriptionExpandedState
                    } else {
                        DetailsDescriptionCollapsedState
                    }
                }
        ) {
            Text(
                text = description,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                maxLines = if (isExpanded) Int.MAX_VALUE else DetailsDescriptionCollapsedMaxLines,
                overflow = if (isExpanded) TextOverflow.Clip else TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MovieSupportingText(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text,
        style = textStyle,
        color = color,
        modifier = modifier,
    )
}

@Composable
private fun MovieLargeTitle(
    movieTitle: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = movieTitle,
        style = MaterialTheme.typography.displayMedium.copy(
            fontWeight = FontWeight.Bold
        ),
        modifier = modifier,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawExpandedFocusDecoration(
    shape: Shape,
    insetPx: Float,
    backgroundColor: Color,
    borderColor: Color,
    borderWidthPx: Float,
) {
    val expandedSize = Size(
        width = size.width + (insetPx * 2f),
        height = size.height + (insetPx * 2f),
    )
    val outline = shape.createOutline(
        size = expandedSize,
        layoutDirection = layoutDirection,
        density = this,
    )

    withTransform({
        translate(
            left = -insetPx,
            top = -insetPx,
        )
    }) {
        when (outline) {
            is Outline.Generic -> {
                drawPath(
                    path = outline.path,
                    color = backgroundColor,
                )
                drawPath(
                    path = outline.path,
                    color = borderColor,
                    style = Stroke(width = borderWidthPx),
                )
            }

            is Outline.Rectangle -> {
                drawRect(
                    color = backgroundColor,
                    topLeft = outline.rect.topLeft,
                    size = outline.rect.size,
                )
                drawRect(
                    color = borderColor,
                    topLeft = outline.rect.topLeft,
                    size = outline.rect.size,
                    style = Stroke(width = borderWidthPx),
                )
            }

            is Outline.Rounded -> {
                val outlinePath = Path().apply {
                    addRoundRect(outline.roundRect)
                }
                drawPath(
                    path = outlinePath,
                    color = backgroundColor,
                )
                drawPath(
                    path = outlinePath,
                    color = borderColor,
                    style = Stroke(width = borderWidthPx),
                )
            }
        }
    }
}

@Composable
fun MovieDetailsBackdrop(
    posterUri: String,
    title: String,
    headers: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier,
    gradientColor: Color = MaterialTheme.colorScheme.surface,
    applyBlur: Boolean = false,
) {
    val context = LocalContext.current
    val imageRequest = remember(context, posterUri, headers) {
        ImageRequest.Builder(context)
            .data(posterUri)
            .crossfade(true)
            .apply {
                if (headers.isNotEmpty()) {
                    httpHeaders(
                        NetworkHeaders.Builder().apply {
                            this["User-Agent"] = USER_AGENT
                            headers.forEach { (key, value) ->
                                this[key] = value
                            }
                        }.build()
                    )
                }
            }
            .build()
    }
    val painter = rememberAsyncImagePainter(model = imageRequest)
    val backdropModifier = if (applyBlur) {
        modifier.blur(22.dp)
    } else {
        modifier
    }

    Image(
        painter = painter,
        contentDescription = StringConstants
            .Composable
            .ContentDescription
            .moviePoster(title),
        contentScale = ContentScale.Crop,
        modifier = backdropModifier.drawWithCache {
            val verticalGradient = Brush.verticalGradient(
                colors = listOf(Color.Transparent, gradientColor),
                startY = size.height * 0.6f
            )
            val horizontalGradient = Brush.horizontalGradient(
                colors = listOf(gradientColor, Color.Transparent),
                endX = size.width * 0.5f,
                startX = 0f
            )
            val diagonalGradient = Brush.linearGradient(
                colors = listOf(gradientColor, Color.Transparent),
                start = Offset(x = 0f, y = size.height),
                end = Offset(x = size.width * 0.7f, y = size.height * 0.3f)
            )

            onDrawWithContent {
                drawContent()
                drawRect(verticalGradient)
                drawRect(horizontalGradient)
                drawRect(diagonalGradient)
            }
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
    val placeholderBlock = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val loadingBackdropUri = backdropUri?.takeIf { it.isNotBlank() }
    val loadingPosterUri = posterUri?.takeIf { it.isNotBlank() }
    val artworkUri = loadingBackdropUri ?: loadingPosterUri
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundColor = MaterialTheme.colorScheme.background
    val placeholderGradient = remember(surfaceVariantColor, surfaceColor, backgroundColor) {
        listOf(
            surfaceVariantColor,
            surfaceColor,
            backgroundColor,
        )
    }

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
                            colors = placeholderGradient
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
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        )
                )

                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .height(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun detailsRatingLabel(score: Score?): String? {
    val scoreLabel = score?.toStringNull(
        minScore = 0.1,
        maxScore = 10,
        decimals = 1,
        removeTrailingZeros = false,
        decimalChar = '.',
    ) ?: return null

    return stringResource(R.string.rating_format, scoreLabel)
}

@Composable
private fun detailsStatusLabel(showStatus: ShowStatus?): String? {
    return when (showStatus) {
        ShowStatus.Completed -> stringResource(R.string.status_completed)
        ShowStatus.Ongoing -> stringResource(R.string.status_ongoing)
        null -> null
    }
}

@Composable
private fun detailsNextAiringLabel(nextAiring: com.lagradost.cloudstream3.NextAiring?): String? {
    if (nextAiring == null || nextAiring.unixTime <= unixTime) {
        return null
    }

    val seconds = nextAiring.unixTime - unixTime
    val days = TimeUnit.SECONDS.toDays(seconds)
    val hours = TimeUnit.SECONDS.toHours(seconds) - days * 24
    val minutes = TimeUnit.SECONDS.toMinutes(seconds) - TimeUnit.SECONDS.toHours(seconds) * 60
    val timeLabel = when {
        days > 0 -> stringResource(R.string.next_episode_time_day_format, days, hours, minutes)
        hours > 0 -> stringResource(R.string.next_episode_time_hour_format, hours, minutes)
        minutes > 0 -> stringResource(R.string.next_episode_time_min_format, minutes)
        else -> return null
    }
    val episodeLabel = when (val season = nextAiring.season) {
        null -> stringResource(R.string.next_episode_format, nextAiring.episode)
        else -> stringResource(R.string.next_season_episode_format, season, nextAiring.episode)
    }

    return "$episodeLabel $timeLabel"
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
