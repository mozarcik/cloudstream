package com.lagradost.cloudstream3.tv.presentation.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Schedule
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import com.lagradost.cloudstream3.tv.presentation.focus.TvFocusMemory
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.lagradost.cloudstream3.NextAiring
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.data.entities.TvEpisode
import com.lagradost.cloudstream3.tv.data.entities.TvSeason
import com.lagradost.cloudstream3.tv.icons.CustomDownload
import com.lagradost.cloudstream3.tv.presentation.common.TvEmptyState
import com.lagradost.cloudstream3.tv.presentation.common.TvListRow
import com.lagradost.cloudstream3.tv.presentation.common.TvProgressBar
import com.lagradost.cloudstream3.tv.presentation.common.TvRowTrailing
import com.lagradost.cloudstream3.tv.presentation.focus.requestFocusWithRetry
import com.lagradost.cloudstream3.tv.presentation.focus.tvFocusFill
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsDownloadActionState
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsQuickAction
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvShapes
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

private const val FocusedDividerAlpha = 0.10f
private const val FocusedGroupFillAlpha = 0.10f
private const val UpcomingFillAlpha = 0.12f
private const val FocusRequestAttempts = 40
private const val FocusRequestRetryMs = 50L

internal fun resolveInitialSeasonId(
    seasons: List<TvSeason>,
    preferredSeasonNumber: Int?
): String? {
    if (seasons.isEmpty()) return null
    if (preferredSeasonNumber == null) return seasons.first().id

    return seasons.firstOrNull { season ->
        season.displaySeasonNumber == preferredSeasonNumber ||
            season.seasonNumber == preferredSeasonNumber
    }?.id ?: seasons.first().id
}

internal fun TvSeason.isExtras(): Boolean = (displaySeasonNumber ?: seasonNumber) == 0

/** Season 0 ("Extras") goes to the end of the list, the rest keeps the provider order. */
internal fun List<TvSeason>.withExtrasLast(): List<TvSeason> = sortedBy { season -> season.isExtras() }

/**
 * Focus plumbing of the Episodes tab: remembers the last focused episode per season, and lets the tab bar
 * (Down) and the season column (Right) ask for "the episode to watch" even when its row is not composed yet.
 */
@Stable
internal class DetailsEpisodesFocusController {
    private val requesters = HashMap<String, FocusRequester>()
    private val lastBySeason = HashMap<String, String>()
    private var pendingEpisodeId by mutableStateOf<String?>(null)
    private var pendingToken by mutableIntStateOf(0)
    var enterRequest by mutableStateOf<EpisodeEnterRequest?>(null)
        private set

    fun requesterFor(episodeId: String): FocusRequester = requesters.getOrPut(episodeId) { FocusRequester() }

    fun onEpisodeFocused(seasonId: String?, episodeId: String) {
        if (seasonId != null) lastBySeason[seasonId] = episodeId
    }

    fun lastFocused(seasonId: String?): String? = seasonId?.let(lastBySeason::get)

    fun requestEpisode(episodeId: String) {
        pendingEpisodeId = episodeId
        pendingToken += 1
        enterRequest = EpisodeEnterRequest(episodeId, pendingToken)
    }

    fun consume(request: EpisodeEnterRequest) {
        if (enterRequest == request) enterRequest = null
    }
}

internal fun episodeActionKey(episodeId: String, action: MovieDetailsQuickAction): String =
    "episode:$episodeId:${action.name}"

@Stable
internal data class EpisodeEnterRequest(val episodeId: String, val token: Int)

/**
 * The episode a user most likely wants: the last focused one in this season, the one the details point at
 * (resume target), the first not yet watched one, else the first.
 */
internal fun resolveEpisodeToFocus(
    episodes: List<TvEpisode>,
    lastFocusedId: String?,
    currentEpisodeNumber: Int?,
    isCurrentSeason: Boolean,
    isWatched: (TvEpisode) -> Boolean,
): TvEpisode? {
    if (episodes.isEmpty()) return null
    lastFocusedId?.let { id -> episodes.firstOrNull { it.id == id } }?.let { return it }
    if (isCurrentSeason && currentEpisodeNumber != null) {
        episodes.firstOrNull { it.episodeNumber == currentEpisodeNumber }?.let { return it }
    }
    return episodes.firstOrNull { !isWatched(it) } ?: episodes.first()
}

@Composable
internal fun DetailsEpisodesTab(
    seasons: List<TvSeason>,
    selectedSeasonId: String?,
    episodes: List<TvEpisode>,
    nextAiring: NextAiring?,
    currentSeasonNumber: Int?,
    currentEpisodeNumber: Int?,
    listState: LazyListState,
    controller: DetailsEpisodesFocusController,
    upRequester: FocusRequester,
    resolveDownloadState: (TvEpisode) -> MovieDetailsDownloadActionState,
    resolveWatched: (TvEpisode) -> Boolean,
    onSeasonSelected: (TvSeason) -> Unit,
    onEpisodeClick: (TvEpisode) -> Unit,
    onEpisodeAction: (TvEpisode, MovieDetailsQuickAction) -> Unit,
    onEpisodeFocused: (seasonId: String?, episode: TvEpisode) -> Unit,
    memory: TvFocusMemory,
    modifier: Modifier = Modifier,
) {
    val seasonListState = rememberLazyListState()
    val seasonRequesters = remember(seasons) { seasons.associate { it.id to FocusRequester() } }
    val selectedSeason = seasons.firstOrNull { it.id == selectedSeasonId }
    val selectedIndex = seasons.indexOfFirst { it.id == selectedSeasonId }.coerceAtLeast(0)

    fun focusEpisodeToWatch() {
        val target = resolveEpisodeToFocus(
            episodes = episodes,
            lastFocusedId = controller.lastFocused(selectedSeasonId),
            currentEpisodeNumber = currentEpisodeNumber,
            isCurrentSeason = selectedSeason?.let { (it.displaySeasonNumber ?: it.seasonNumber) == currentSeasonNumber } ?: false,
            isWatched = resolveWatched,
        ) ?: return
        controller.requestEpisode(target.id)
    }

    // A request from the tab bar (Down) or the season column (Right): scroll to the row, then focus it.
    val request = controller.enterRequest
    LaunchedEffect(request) {
        val pending = request ?: return@LaunchedEffect
        val index = episodes.indexOfFirst { it.id == pending.episodeId }
        if (index >= 0) {
            listState.scrollToItem(index)
            controller.requesterFor(pending.episodeId)
                .requestFocusWithRetry(FocusRequestAttempts, FocusRequestRetryMs)
        }
        controller.consume(pending)
    }
    // Selecting another season starts its list from the top.
    LaunchedEffect(selectedSeasonId) { listState.scrollToItem(0) }

    Row(modifier = modifier.fillMaxSize()) {
        if (seasons.isNotEmpty()) {
            SeasonColumn(
                seasons = seasons,
                selectedSeasonId = selectedSeasonId,
                nextAiring = nextAiring,
                listState = seasonListState,
                requesters = seasonRequesters,
                upRequester = upRequester,
                onSeasonSelected = onSeasonSelected,
                onEnterEpisodes = { focusEpisodeToWatch() },
            )
            Box(
                modifier = Modifier
                    .padding(start = TvDimens.SeasonSeparatorStart - TvDimens.DetailsContentStart - TvDimens.SeasonColumnWidth)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(TvColors.DetailsSeparator),
            )
        }
        Column(
            modifier = Modifier
                .padding(
                    start = if (seasons.isNotEmpty()) {
                        TvDimens.EpisodeListStart - TvDimens.SeasonSeparatorStart - 1.dp
                    } else {
                        TvDimens.DetailsContentStart
                    },
                    end = TvDimens.EpisodeListEnd,
                )
                .weight(1f)
                .fillMaxHeight(),
        ) {
            val upcoming = rememberUpcomingLabel(nextAiring, selectedSeason, seasons)
            if (upcoming != null) {
                UpcomingBar(upcoming)
            }
            if (episodes.isEmpty()) {
                TvEmptyState(
                    icon = Icons.Outlined.PlayCircleOutline,
                    title = stringResource(R.string.no_episodes_found),
                    message = stringResource(R.string.tv_details_no_episodes_hint),
                )
            } else {
                val episodeSeasonId = selectedSeasonId
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(TvDimens.EpisodeRowGap),
                    contentPadding = PaddingValues(bottom = TvDimens.EpisodeRowGap),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(episodes, key = { episode -> episode.id }) { episode ->
                        val isFirst = episode.id == episodes.first().id
                        EpisodeRow(
                            episode = episode,
                            watched = resolveWatched(episode),
                            downloadState = resolveDownloadState(episode),
                            requester = controller.requesterFor(episode.id),
                            leftRequester = seasonRequesters[selectedSeasonId],
                            upRequester = if (isFirst) upRequester else null,
                            onClick = { onEpisodeClick(episode) },
                            onAction = { action -> onEpisodeAction(episode, action) },
                            memory = memory,
                            onFocused = {
                                controller.onEpisodeFocused(episodeSeasonId, episode.id)
                                onEpisodeFocused(episodeSeasonId, episode)
                            },
                        )
                    }
                }
            }
        }
    }
    // Keep the selected season visible when the list is long.
    LaunchedEffect(selectedIndex) { seasonListState.animateScrollToItem(selectedIndex) }
}

@Composable
private fun SeasonColumn(
    seasons: List<TvSeason>,
    selectedSeasonId: String?,
    nextAiring: NextAiring?,
    listState: LazyListState,
    requesters: Map<String, FocusRequester>,
    upRequester: FocusRequester,
    onSeasonSelected: (TvSeason) -> Unit,
    onEnterEpisodes: () -> Unit,
) {
    // Up/Down over the seasons switches the episode list right away (debounced), focus stays on the season.
    var focusedSeason by remember { mutableStateOf<TvSeason?>(null) }
    LaunchedEffect(focusedSeason) {
        val season = focusedSeason ?: return@LaunchedEffect
        if (season.id != selectedSeasonId) {
            delay(TvDimens.SeasonSelectDebounceMs)
            onSeasonSelected(season)
        }
    }
    val airingSeasonId = remember(seasons, nextAiring) { resolveAiringSeason(nextAiring, seasons)?.id }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(TvDimens.SeasonRowGap),
        contentPadding = PaddingValues(bottom = TvDimens.EpisodeRowGap),
        state = listState,
        modifier = Modifier
            .padding(start = TvDimens.DetailsContentStart)
            .width(TvDimens.SeasonColumnWidth)
            .fillMaxHeight(),
    ) {
        items(seasons.size, key = { index -> seasons[index].id }) { index ->
            val season = seasons[index]
            val episodeCount = season.episodes.size
            TvListRow(
                title = seasonLabel(season),
                subtitle = pluralStringResource(R.plurals.tv_details_episodes_count, episodeCount, episodeCount),
                onClick = { onEnterEpisodes() },
                selected = season.id == selectedSeasonId,
                height = TvDimens.SeasonRowHeight,
                titleStyle = TvTextStyles.SeasonTitle,
                accent = MaterialTheme.colorScheme.primary,
                trailing = if (season.id == airingSeasonId) {
                    TvRowTrailing.Custom { _ -> AiringDot() }
                } else {
                    TvRowTrailing.None
                },
                modifier = Modifier
                    .testTag("details_season_${season.id}")
                    .focusRequester(requesters.getValue(season.id))
                    .focusProperties {
                        left = FocusRequester.Cancel
                        if (index == 0) up = upRequester
                    }
                    .onFocusChanged { state -> if (state.isFocused) focusedSeason = season }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionRight) {
                            onEnterEpisodes()
                            true
                        } else {
                            false
                        }
                    },
            )
        }
    }
}

@Composable
private fun AiringDot() {
    Box(
        modifier = Modifier
            .size(TvDimens.EpisodeBadgeInset * 2)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
    )
}

@Composable
private fun seasonLabel(season: TvSeason): String {
    val seasonLabel = stringResource(R.string.season)
    val extrasLabel = stringResource(R.string.extras)
    val number = season.displaySeasonNumber ?: season.seasonNumber
    val customTitle = season.title.orEmpty().trim()
    return when {
        number == 0 && customTitle.isNotBlank() -> customTitle
        number == 0 -> extrasLabel
        number != null && customTitle.isNotBlank() -> "$seasonLabel $number $customTitle"
        number != null -> "$seasonLabel $number"
        customTitle.isNotBlank() -> customTitle
        else -> seasonLabel
    }
}

/** The season the next airing episode belongs to: the matching number, else the last regular season. */
internal fun resolveAiringSeason(nextAiring: NextAiring?, seasons: List<TvSeason>): TvSeason? {
    if (nextAiring == null) return null
    val wanted = nextAiring.season
    return if (wanted != null) {
        seasons.firstOrNull { (it.displaySeasonNumber ?: it.seasonNumber) == wanted }
    } else {
        seasons.lastOrNull { !it.isExtras() }
    }
}

@Composable
private fun rememberUpcomingLabel(
    nextAiring: NextAiring?,
    selectedSeason: TvSeason?,
    seasons: List<TvSeason>,
): androidx.compose.ui.text.AnnotatedString? {
    val airing = resolveAiringSeason(nextAiring, seasons)
    if (nextAiring == null || airing == null || airing.id != selectedSeason?.id) return null
    val parts = detailsNextAiringParts(nextAiring) ?: return null
    return buildAnnotatedString {
        append(parts.first)
        append(' ')
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
            append(parts.second)
        }
    }
}

@Composable
private fun UpcomingBar(label: androidx.compose.ui.text.AnnotatedString) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TvDimens.DetailsNoticeIconGap),
        modifier = Modifier
            .padding(top = TvDimens.EpisodeRowGap, bottom = TvDimens.EpisodeUpcomingBottom)
            .fillMaxWidth()
            .height(TvDimens.EpisodeUpcomingHeight)
            .background(accent.copy(alpha = UpcomingFillAlpha), TvShapes.PanelRow)
            .padding(horizontal = TvDimens.DetailsNoticeHorizontalPadding),
    ) {
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(TvDimens.DetailsNoticeIconSize),
        )
        Text(
            text = label,
            style = TvTextStyles.DetailsNotice,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("details_next_airing"),
        )
    }
}

@Composable
private fun EpisodeRow(
    episode: TvEpisode,
    watched: Boolean,
    downloadState: MovieDetailsDownloadActionState,
    requester: FocusRequester,
    leftRequester: FocusRequester?,
    upRequester: FocusRequester?,
    onClick: () -> Unit,
    onAction: (MovieDetailsQuickAction) -> Unit,
    memory: TvFocusMemory,
    onFocused: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val accent = colors.primary
    val mainInteraction = remember { MutableInteractionSource() }
    val mainFocused by mainInteraction.collectIsFocusedAsState()
    var groupFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val stillRequest = remember(context, episode.posterUri) {
        ImageRequest.Builder(context).data(episode.posterUri.takeIf { it.isNotBlank() }).crossfade(false).build()
    }
    val watchedAction = if (watched) MovieDetailsQuickAction.RemoveFromWatched else MovieDetailsQuickAction.MarkAsWatched
    val titleColor = if (mainFocused) colors.inverseOnSurface else colors.onSurface
    val metaColor = if (mainFocused) TvColors.OnFocusSecondary else colors.onSurfaceVariant

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(TvDimens.EpisodeRowHeight)
            .testTag("episode_card_${episode.id}")
            .focusGroup()
            // Inherited by the action buttons too, so Up from them reaches the tab header instead of a neighbour.
            .focusProperties { if (upRequester != null) up = upRequester }
            .onFocusChanged { state -> groupFocused = state.hasFocus }
            .drawBehind {
                // Fill read in draw phase so focus changes don't recompose the row for the background.
                val fill = when {
                    mainFocused -> colors.inverseSurface
                    groupFocused -> Color.White.copy(alpha = FocusedGroupFillAlpha)
                    else -> return@drawBehind
                }
                drawRoundRect(
                    color = fill,
                    cornerRadius = CornerRadius(TvDimens.EpisodeRowRadius.toPx()),
                )
            }
            .padding(horizontal = TvDimens.EpisodeRowPadding),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TvDimens.EpisodeRowContentGap),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .focusRequester(requester)
                .focusProperties {
                    if (leftRequester != null) left = leftRequester
                    if (upRequester != null) up = upRequester
                }
                .onFocusChanged { state -> if (state.isFocused) onFocused() }
                .clickable(interactionSource = mainInteraction, indication = null, role = Role.Button, onClick = onClick),
        ) {
            Box(
                modifier = Modifier
                    .size(TvDimens.EpisodeStillWidth, TvDimens.EpisodeStillHeight)
                    .clip(RoundedCornerShapeOf(TvDimens.EpisodeStillRadius))
                    .background(TvColors.PosterPlaceholder),
            ) {
                if (episode.posterUri.isNotBlank()) {
                    AsyncImage(
                        model = stillRequest,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.PlayCircleOutline,
                        contentDescription = null,
                        tint = TvColors.Disabled,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(TvDimens.StateIconSize / 2),
                    )
                }
                if (watched) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(TvDimens.EpisodeBadgeInset)
                            .size(TvDimens.EpisodeBadgeSize)
                            .background(TvColors.EpisodeBadge, CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(TvDimens.EpisodeBadgeIcon),
                        )
                    }
                    TvProgressBar(
                        progress = 1f,
                        accent = accent,
                        modifier = Modifier.align(Alignment.BottomStart),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episodeTitleText(episode),
                    style = TvTextStyles.EpisodeTitle,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = remember(episode, watched, configuration) {
                    episodeMetaText(context, episode, watched)
                }
                if (meta != null) {
                    Text(
                        text = meta,
                        style = TvTextStyles.EpisodeMeta,
                        color = metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = TvDimens.EpisodeMetaTop),
                    )
                }
                Text(
                    text = episode.description.ifBlank { stringResource(R.string.tv_no_episode_description) },
                    style = TvTextStyles.EpisodeDescription,
                    color = if (mainFocused) TvColors.OnFocusSecondary else colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = TvDimens.EpisodeDescriptionTop),
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(TvDimens.EpisodeActionGap, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = TvDimens.EpisodeActionGap)
                .width(TvDimens.EpisodeActionsWidth),
        ) {
            val showActions = groupFocused
            EpisodeIconButton(
                icon = Icons.Filled.Check,
                description = stringResource(
                    if (watched) R.string.action_remove_from_watched else R.string.action_mark_as_watched
                ),
                onClick = { onAction(watchedAction) },
                rowFocused = mainFocused,
                tint = if (watched) accent else null,
                modifier = Modifier
                    .alpha(if (showActions) 1f else 0f)
                    .testTag("episode_${episode.id}_action_watched"),
            )
            EpisodeDownloadButton(
                state = downloadState,
                onClick = { onAction(MovieDetailsQuickAction.Download) },
                rowFocused = mainFocused,
                modifier = Modifier
                    .alpha(if (showActions || downloadState != MovieDetailsDownloadActionState.Idle) 1f else 0f)
                    .then(with(memory) { Modifier.track(episodeActionKey(episode.id, MovieDetailsQuickAction.Download)) })
                    .testTag("episode_${episode.id}_action_download"),
            )
            EpisodeIconButton(
                icon = Icons.Filled.MoreVert,
                description = stringResource(R.string.episode_more_options_des),
                onClick = { onAction(MovieDetailsQuickAction.More) },
                rowFocused = mainFocused,
                modifier = Modifier
                    .alpha(if (showActions) 1f else 0f)
                    .then(with(memory) { Modifier.track(episodeActionKey(episode.id, MovieDetailsQuickAction.More)) })
                    .testTag("episode_${episode.id}_action_more"),
            )
        }
    }
}

private fun RoundedCornerShapeOf(radius: androidx.compose.ui.unit.Dp) =
    androidx.compose.foundation.shape.RoundedCornerShape(radius)

@Composable
private fun EpisodeIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    rowFocused: Boolean,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    container: Color? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    val contentColor = when {
        focused -> colors.inverseOnSurface
        tint != null -> tint
        rowFocused -> colors.inverseOnSurface
        else -> colors.onSurface
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(TvDimens.EpisodeActionSize)
            .tvFocusFill(
                focused = focused,
                shape = CircleShape,
                container = container ?: if (rowFocused) Color.Black.copy(alpha = FocusedDividerAlpha) else TvColors.FillIcon,
                focusedContainer = colors.inverseSurface,
            )
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = contentColor,
            modifier = Modifier.size(TvDimens.EpisodeActionIcon),
        )
    }
}

/** Idle: download icon. Downloading: ring + percent. Downloaded: green. Failed: red. */
@Composable
private fun EpisodeDownloadButton(
    state: MovieDetailsDownloadActionState,
    onClick: () -> Unit,
    rowFocused: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        MovieDetailsDownloadActionState.Idle -> EpisodeIconButton(
            icon = Icons.Filled.CustomDownload,
            description = stringResource(R.string.download),
            onClick = onClick,
            rowFocused = rowFocused,
            modifier = modifier,
        )

        MovieDetailsDownloadActionState.Downloaded -> EpisodeIconButton(
            icon = Icons.Outlined.CheckCircle,
            description = stringResource(R.string.downloaded),
            onClick = onClick,
            rowFocused = rowFocused,
            tint = TvColors.Success,
            container = TvColors.SuccessFill,
            modifier = modifier,
        )

        MovieDetailsDownloadActionState.Failed -> EpisodeIconButton(
            icon = Icons.Outlined.ErrorOutline,
            description = stringResource(R.string.download_failed),
            onClick = onClick,
            rowFocused = rowFocused,
            tint = TvColors.ErrorText,
            container = TvColors.ErrorFill,
            modifier = modifier,
        )

        is MovieDetailsDownloadActionState.Downloading -> {
            val interactionSource = remember { MutableInteractionSource() }
            val focused by interactionSource.collectIsFocusedAsState()
            val colors = MaterialTheme.colorScheme
            val content = if (focused || rowFocused) colors.inverseOnSurface else colors.onSurface
            val percent = (state.progress.coerceIn(0f, 1f) * 100f).roundToInt()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TvDimens.EpisodeProgressLabelGap),
                modifier = modifier
                    .height(TvDimens.EpisodeActionSize)
                    .tvFocusFill(
                        focused = focused,
                        shape = CircleShape,
                        container = if (rowFocused) Color.Black.copy(alpha = FocusedDividerAlpha) else TvColors.FillIcon,
                        focusedContainer = colors.inverseSurface,
                    )
                    .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
                    .padding(horizontal = TvDimens.EpisodeActionGap),
            ) {
                DownloadProgressRing(progress = state.progress, accent = colors.primary, track = content)
                Text(text = "$percent%", style = TvTextStyles.Caption, color = content, maxLines = 1)
            }
        }
    }
}

@Composable
private fun episodeTitleText(episode: TvEpisode): String {
    val title = episode.title.ifBlank { stringResource(R.string.tv_episode_untitled) }
    val number = episode.episodeNumber ?: return title
    return if (title.trimStart().startsWith("$number.") || title.trimStart().startsWith("$number ")) title else "$number. $title"
}

private fun episodeMetaText(context: Context, episode: TvEpisode, watched: Boolean): String? {
    val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
    val watchedLabel = context.getString(R.string.tv_details_watched)
    val duration = episode.durationMinutes?.let { minutes ->
        if (minutes < 60) {
            context.getString(R.string.duration_format, minutes)
        } else {
            context.getString(R.string.tv_episode_duration_hour_min_format, minutes / 60, minutes % 60)
        }
    }
    val rating = episode.ratingText?.let { context.getString(R.string.tv_episode_rating_format, it) }
    val parts = listOfNotNull(
        episode.releaseDateMillis?.let { dateFormat.format(Date(it)) },
        duration,
        rating,
        watchedLabel.takeIf { watched },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
}
