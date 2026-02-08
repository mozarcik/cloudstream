package com.lagradost.cloudstream3.tv.presentation.screens.movies

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.annotation.DrawableRes
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.data.entities.MovieDetails
import com.lagradost.cloudstream3.tv.data.util.StringConstants
import com.lagradost.cloudstream3.tv.presentation.utils.Padding
import kotlinx.coroutines.launch

val ParentPadding = PaddingValues(vertical = 16.dp, horizontal = 58.dp)
private val ActionItemShape = RoundedCornerShape(16.dp)
private val ActionPillHeight = 44.dp
private val ActionIconButtonSize = ActionPillHeight / 2
private val ActionsPillShape = RoundedCornerShape(ActionIconButtonSize)
private val ActionIconSlotWidth = ActionPillHeight
private val ActionFocusedCircleSize = ActionPillHeight
private val ActionLabelHeight = 16.dp
private val ActionItemsSpacing = 8.dp
private val ActionPillHorizontalPadding = 0.dp
private const val ActionFocusAnimationMs = 180
private const val ActionFocusedScale = 1f
private const val ActionUnfocusedAlpha = 0.82f

private data class ActionIconSpec(
    @DrawableRes val iconRes: Int,
    val label: String,
    val testTag: String,
)

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
) {
    val childPadding = rememberChildPadding()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val buttonModifier = Modifier.onFocusChanged {
        if (it.isFocused) {
            coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
    ) {
        Column(modifier = Modifier.fillMaxWidth(1.0f)) {
            Column(modifier = Modifier.fillMaxWidth(0.55f)) {
                Spacer(modifier = Modifier.height(108.dp))
                Column(
                    modifier = Modifier.padding(start = childPadding.start)
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

//                    DirectorScreenplayMusicRow(
//                        director = movieDetails.director,
//                        screenplay = movieDetails.screenplay,
//                        music = movieDetails.music
//                    )
                    }
                }
            }

            DetailsActionsRow(
                modifier = Modifier
                    .padding(start = childPadding.start)
                    .padding(top = 24.dp),
                playFocusRequester = focusRequester,
                itemFocusModifier = buttonModifier,
                onPlayClick = goToMoviePlayer,
                playButtonLabel = playButtonLabel
            )
        }
    }
}

@Composable
private fun DetailsActionsRow(
    playFocusRequester: FocusRequester,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    itemFocusModifier: Modifier = Modifier,
    playButtonLabel: String? = null,
) {
    val firstActionFocusRequester = remember { FocusRequester() }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PrimaryPlayButton(
            modifier = itemFocusModifier
                .focusRequester(playFocusRequester)
                .focusProperties {
                    right = firstActionFocusRequester
                },
            goToMoviePlayer = onPlayClick,
            playButtonLabel = playButtonLabel
        )

        ActionIconsPill(
            playFocusRequester = playFocusRequester,
            firstActionFocusRequester = firstActionFocusRequester,
            itemFocusModifier = itemFocusModifier,
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
            painter = painterResource(id = R.drawable.ic_baseline_play_arrow_24),
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
private fun ActionIconsPill(
    playFocusRequester: FocusRequester,
    firstActionFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    itemFocusModifier: Modifier = Modifier,
) {
    val favoriteLabel = stringResource(R.string.favorite)
    val searchLabel = stringResource(R.string.search)
    val downloadLabel = stringResource(R.string.download)
    val actions = remember(favoriteLabel, searchLabel, downloadLabel) {
        listOf(
            ActionIconSpec(
                iconRes = R.drawable.ic_baseline_bookmark_border_24,
                label = "Bookmark",
                testTag = "action_bookmark"
            ),
            ActionIconSpec(
                iconRes = R.drawable.ic_baseline_favorite_border_24,
                label = favoriteLabel,
                testTag = "action_favorite"
            ),
            ActionIconSpec(
                iconRes = R.drawable.search_icon,
                label = searchLabel,
                testTag = "action_search"
            ),
            ActionIconSpec(
                iconRes = R.drawable.baseline_downloading_24,
                label = downloadLabel,
                testTag = "action_download"
            ),
            ActionIconSpec(
                iconRes = R.drawable.ic_baseline_more_vert_24,
                label = "More",
                testTag = "action_more"
            )
        )
    }
    val actionFocusRequesters = remember {
        List(actions.lastIndex) { FocusRequester() }
    }
    var focusedIndex by remember { mutableIntStateOf(-1) }

    fun requesterForIndex(index: Int): FocusRequester {
        return if (index == 0) {
            firstActionFocusRequester
        } else {
            actionFocusRequesters[index - 1]
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(ActionsPillShape)
                .background(Color.Black.copy(alpha = 0.08f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.36f),
                    shape = ActionsPillShape
                )
                .height(ActionPillHeight)
                .padding(horizontal = ActionPillHorizontalPadding)
                .testTag("action_icons_pill"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ActionItemsSpacing)
        ) {
            actions.forEachIndexed { index, action ->
                val leftRequester = if (index == 0) {
                    playFocusRequester
                } else {
                    requesterForIndex(index - 1)
                }
                val rightRequester = if (index == actions.lastIndex) {
                    FocusRequester.Cancel
                } else {
                    requesterForIndex(index + 1)
                }

                ActionIconItem(
                    action = action,
                    focusRequester = requesterForIndex(index),
                    leftFocusRequester = leftRequester,
                    rightFocusRequester = rightRequester,
                    modifier = itemFocusModifier,
                    onFocusedChanged = { isFocused ->
                        if (isFocused) {
                            focusedIndex = index
                        } else if (focusedIndex == index) {
                            focusedIndex = -1
                        }
                    }
                )
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = ActionPillHorizontalPadding),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(ActionItemsSpacing)
        ) {
            actions.forEachIndexed { index, action ->
                ActionIconLabel(
                    text = action.label,
                    visible = focusedIndex == index,
                    modifier = Modifier.width(ActionIconSlotWidth)
                )
            }
        }
    }
}

@Composable
private fun ActionIconItem(
    action: ActionIconSpec,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester,
    rightFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onFocusedChanged: (Boolean) -> Unit = {},
) {
    var isFocused by remember { mutableStateOf(false) }
    val iconScale by animateFloatAsState(
        targetValue = if (isFocused) ActionFocusedScale else 1f,
        animationSpec = tween(
            durationMillis = ActionFocusAnimationMs,
            easing = FastOutSlowInEasing
        ),
        label = "details_action_icon_scale"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else ActionUnfocusedAlpha,
        animationSpec = tween(
            durationMillis = ActionFocusAnimationMs,
            easing = FastOutSlowInEasing
        ),
        label = "details_action_icon_alpha"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isFocused) Color(0xFF1A1E27) else Color(0xFFE2E4EA),
        animationSpec = tween(
            durationMillis = ActionFocusAnimationMs,
            easing = FastOutSlowInEasing
        ),
        label = "details_action_icon_tint"
    )
    val iconContainerColor by animateColorAsState(
        targetValue = if (isFocused) Color.White.copy(alpha = 0.96f) else Color.Transparent,
        animationSpec = tween(
            durationMillis = ActionFocusAnimationMs,
            easing = FastOutSlowInEasing
        ),
        label = "details_action_icon_container"
    )

    Surface(
        onClick = {},
        shape = ClickableSurfaceDefaults.shape(shape = ActionItemShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(0.dp, Color.Transparent),
                shape = ActionItemShape
            )
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        ),
        modifier = modifier
            .focusRequester(focusRequester)
            .focusProperties {
                left = leftFocusRequester
                right = rightFocusRequester
            }
            .onFocusChanged {
                isFocused = it.isFocused
                onFocusedChanged(it.isFocused)
            }
            .testTag(action.testTag)
        ) {
            Box(
                modifier = Modifier
                    .width(ActionIconSlotWidth)
                    .size(ActionFocusedCircleSize)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                painter = painterResource(id = action.iconRes),
                contentDescription = action.label,
                    tint = iconTint.copy(alpha = iconAlpha),
                    modifier = Modifier
                        .size(ActionIconButtonSize)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
        }
    }
}

@Composable
private fun ActionIconLabel(
    text: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val labelAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = ActionFocusAnimationMs,
            easing = FastOutSlowInEasing
        ),
        label = "details_action_label_alpha"
    )
    val labelTranslationY by animateFloatAsState(
        targetValue = if (visible) 0f else with(density) { 8.dp.toPx() },
        animationSpec = tween(
            durationMillis = ActionFocusAnimationMs,
            easing = FastOutSlowInEasing
        ),
        label = "details_action_label_translation"
    )

    Box(
        modifier = modifier.height(ActionLabelHeight),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier
                .graphicsLayer {
                    alpha = labelAlpha
                    translationY = labelTranslationY
                }
                .wrapContentWidth(unbounded = true),
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DirectorScreenplayMusicRow(
    director: String,
    screenplay: String,
    music: String
) {
    Row(modifier = Modifier.padding(top = 32.dp)) {
        TitleValueText(
            modifier = Modifier
                .padding(end = 32.dp)
                .weight(1f),
            title = stringResource(R.string.status),
            value = director
        )

        TitleValueText(
            modifier = Modifier
                .padding(end = 32.dp)
                .weight(1f),
            title = stringResource(R.string.status),
            value = screenplay
        )

        TitleValueText(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.status),
            value = music
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
        maxLines = 1
    )
}

@Composable
fun MovieDetailsBackdrop(
    posterUri: String,
    title: String,
    modifier: Modifier = Modifier,
    gradientColor: Color = MaterialTheme.colorScheme.surface,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(posterUri)
            .crossfade(true).build(),
        contentDescription = StringConstants
            .Composable
            .ContentDescription
            .moviePoster(title),
        contentScale = ContentScale.Crop,
        modifier = modifier.drawWithContent {
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
