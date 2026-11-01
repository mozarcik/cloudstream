package com.lagradost.cloudstream3.phone.presentation.screens.home

import com.lagradost.cloudstream3.phone.presentation.common.navBarBottom
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.phone.presentation.common.CsButton
import com.lagradost.cloudstream3.phone.presentation.common.HideOnScrollScaffold
import com.lagradost.cloudstream3.phone.presentation.common.CsChip
import com.lagradost.cloudstream3.phone.presentation.common.CsIconButton
import com.lagradost.cloudstream3.phone.presentation.common.CsKicker
import com.lagradost.cloudstream3.phone.presentation.common.LocalPermanentMenu
import com.lagradost.cloudstream3.phone.presentation.common.CsPosterCard
import com.lagradost.cloudstream3.phone.presentation.common.CsProgress
import com.lagradost.cloudstream3.phone.presentation.common.CsSectionHeader
import com.lagradost.cloudstream3.phone.presentation.theme.LocalPhoneAccent
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneColors
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneDimens
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneShapes
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneText
import com.lagradost.cloudstream3.tv.compat.home.FeedCategory
import com.lagradost.cloudstream3.tv.compat.home.FeedRepositoryImpl
import com.lagradost.cloudstream3.tv.compat.home.FeaturedItemCompat
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.compat.home.ratingLabelOrNull
import com.lagradost.cloudstream3.tv.compat.home.preferredBackdropUriOrNull
import com.lagradost.cloudstream3.tv.compat.home.sourceId
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeContinueWatchingViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeContinueWatchingViewModelFactory
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeaturedViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeaturedViewModelFactory
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeaturedLoadState
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeedLoadState
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeedSectionUiState
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeedsViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeedsViewModelFactory
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeSourcesViewModel
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeSourcesViewModelFactory

/** Feed rows hold 11 posters + the "Show more" cell (same 12 cells as the TV mini grid). */
private const val FeedPosterCount = 11

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneHomeScreen(
    onOpenMenu: () -> Unit,
    onOpenSearch: () -> Unit,
    onMediaClick: (MediaItemCompat) -> Unit,
    onContinueWatchingPlay: (MediaItemCompat) -> Unit,
    onOpenFeedGrid: (FeedCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sourcesVm: HomeSourcesViewModel = viewModel(factory = remember { HomeSourcesViewModelFactory() })
    val continueVm: HomeContinueWatchingViewModel =
        viewModel(factory = remember { HomeContinueWatchingViewModelFactory() })
    val featuredVm: HomeFeaturedViewModel = viewModel(factory = remember { HomeFeaturedViewModelFactory() })
    val feedsVm: HomeFeedsViewModel =
        viewModel(factory = remember { HomeFeedsViewModelFactory(FeedRepositoryImpl()) })

    val sources by sourcesVm.uiState.collectAsStateWithLifecycle()
    val continueState by continueVm.uiState.collectAsStateWithLifecycle()
    val featured by featuredVm.uiState.collectAsStateWithLifecycle()
    val feeds by feedsVm.uiState.collectAsStateWithLifecycle()

    // Same rule as TV: refresh Continue watching when coming back from the player.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, continueVm) {
        var wasPaused = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> wasPaused = true
                Lifecycle.Event.ON_RESUME -> if (wasPaused) {
                    wasPaused = false
                    continueVm.refresh()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showSourceSheet by remember { mutableStateOf(false) }
    val continueItems = (continueState.state as? HomeFeedLoadState.Success)?.items.orEmpty()
    val featuredItems = (featured.state as? HomeFeaturedLoadState.Success)?.items.orEmpty()

    HideOnScrollScaffold(
        modifier = modifier.background(PhoneColors.Ground),
        bar = { HomeHeader(onOpenMenu, onOpenSearch) },
    ) { padding ->
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 32.dp + navBarBottom()),
    ) {
        if (continueItems.isNotEmpty()) {
            item(key = "continue") {
                ContinueWatchingCarousel(
                    items = continueItems,
                    onResume = onContinueWatchingPlay,
                    onDetails = onMediaClick,
                    onRemove = { continueVm.removeItem(it.continueWatching?.parentId) },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        if (sources.allSources.isNotEmpty()) {
            item(key = "sources") {
                SourceChips(
                    quick = sources.quickSources,
                    selected = sources.selectedSource,
                    onSelect = sourcesVm::selectSource,
                    onMore = { sourcesVm.setMorePanelOpen(true); showSourceSheet = true },
                    modifier = Modifier.padding(top = PhoneDimens.SectionGap),
                )
            }
        } else if (sources.isBootstrapped) {
            item(key = "no-sources") {
                Text(
                    stringResource(R.string.phone_no_sources),
                    style = PhoneText.Body,
                    color = PhoneColors.Text2,
                    modifier = Modifier.padding(PhoneDimens.ScreenPadding),
                )
            }
        }

        if (featuredItems.isNotEmpty()) {
            item(key = "featured") {
                FeaturedPager(
                    items = featuredItems,
                    onClick = { onMediaClick(it.navigationTarget) },
                    modifier = Modifier.padding(top = 22.dp),
                )
            }
        }

        itemsIndexed(feeds.feedSections, key = { index, section -> "${section.feed.id}#$index" }) { _, section ->
            FeedRow(
                section = section,
                onMediaClick = onMediaClick,
                onShowMore = { onOpenFeedGrid(section.feed) },
                modifier = Modifier.padding(top = 22.dp),
            )
        }
    }
    }

    if (showSourceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSourceSheet = false; sourcesVm.setMorePanelOpen(false) },
            containerColor = PhoneColors.Sheet,
        ) {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                itemsIndexed(sources.morePanelSources, key = { index, source -> "${source.sourceId()}#$index" }) { _, source ->
                    SourceSheetRow(
                        name = source.name,
                        selected = source.sourceId() == sources.selectedSource?.sourceId(),
                        onClick = {
                            sourcesVm.selectSource(source)
                            showSourceSheet = false
                            sourcesVm.setMorePanelOpen(false)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceSheetRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(PhoneDimens.RowHeight)
            .background(if (selected) Color(0x14FFFFFF) else Color.Transparent)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = PhoneDimens.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = PhoneText.Row, color = PhoneColors.Text)
    }
}

@Composable
private fun HomeHeader(onOpenMenu: () -> Unit, onOpenSearch: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (!LocalPermanentMenu.current) {
            CsIconButton(
                icon = Icons.Default.Menu,
                contentDescription = stringResource(R.string.phone_open_menu),
                onClick = onOpenMenu,
                container = Color.Transparent,
            )
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Text("CloudStream", style = PhoneText.Level, color = PhoneColors.Text, modifier = Modifier.weight(1f))
        CsIconButton(Icons.Default.Search, stringResource(R.string.search), onOpenSearch)
    }
}

@Composable
private fun SourceChips(
    quick: List<MainAPI>,
    selected: MainAPI?,
    onSelect: (MainAPI) -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        CsKicker(stringResource(R.string.phone_source), Modifier.padding(horizontal = PhoneDimens.ScreenPadding, vertical = 0.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = PhoneDimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 10.dp),
        ) {
            itemsIndexed(quick, key = { index, source -> "${source.sourceId()}#$index" }) { _, source ->
                CsChip(source.name, selected = source.sourceId() == selected?.sourceId(), onClick = { onSelect(source) })
            }
            item(key = "more") { CsChip(stringResource(R.string.phone_more), selected = false, onClick = onMore) }
        }
    }
}

@Composable
private fun ContinueWatchingCarousel(
    items: List<MediaItemCompat>,
    onResume: (MediaItemCompat) -> Unit,
    onDetails: (MediaItemCompat) -> Unit,
    onRemove: (MediaItemCompat) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // The card that is mostly on screen, not just the first one still partly visible.
    val current by remember {
        derivedStateOf {
            val first = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            val passedHalf = first != null && listState.firstVisibleItemScrollOffset > first.size / 2
            (listState.firstVisibleItemIndex + if (passedHalf) 1 else 0).coerceIn(0, items.lastIndex)
        }
    }
    Column(modifier) {
        Row(Modifier.padding(horizontal = PhoneDimens.ScreenPadding).padding(bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
            CsKicker(stringResource(R.string.continue_watching), Modifier.weight(1f))
            Text(
                stringResource(R.string.phone_x_of_y, current + 1, items.size),
                style = PhoneText.Meta,
                color = PhoneColors.Text2,
            )
        }
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(horizontal = PhoneDimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(items, key = { index, item -> "${item.id}${item.apiName}#$index" }) { _, item ->
                ContinueWatchingCard(item, onResume, onDetails, onRemove)
            }
        }
        PagerDots(count = items.size, current = current, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp))
    }
}

@Composable
private fun ContinueWatchingCard(
    item: MediaItemCompat,
    onResume: (MediaItemCompat) -> Unit,
    onDetails: (MediaItemCompat) -> Unit,
    onRemove: (MediaItemCompat) -> Unit,
) {
    val state = item.continueWatching
    Box(
        modifier = Modifier
            .size(PhoneDimens.HeroCardWidth, PhoneDimens.HeroCardHeight)
            .clip(PhoneShapes.Hero)
            .background(PhoneColors.Card)
            .clickable(role = Role.Button) { onResume(item) },
    ) {
        item.preferredBackdropUriOrNull()?.let {
            AsyncImage(it, stringResource(R.string.phone_resume_item, item.name), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.3f to Color.Transparent, 1f to Color(0xF00E1015))))
        Box(
            Modifier.align(Alignment.Center).size(52.dp).clip(PhoneShapes.Pill).background(Color(0x8C0E1015)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.PlayArrow, null, tint = PhoneColors.Text, modifier = Modifier.size(26.dp)) }
        CsIconButton(
            icon = Icons.Default.Close,
            contentDescription = stringResource(R.string.phone_remove_continue_watching),
            onClick = { onRemove(item) },
            container = Color(0x990E1015),
            size = 40.dp,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    val meta = continueMeta(state?.season, state?.episode, state?.remainingMs)
                    if (meta.isNotEmpty()) Text(meta, style = PhoneText.Meta, color = LocalPhoneAccent.current, maxLines = 1)
                    Text(item.name, style = PhoneText.Section, color = PhoneColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                CsButton(stringResource(R.string.details), { onDetails(item) }, height = 36.dp)
            }
            state?.progress?.let { CsProgress(it, Modifier.padding(top = 10.dp)) }
        }
    }
}

@Composable
private fun continueMeta(season: Int?, episode: Int?, remainingMs: Long?): String {
    val ep = if (season != null && episode != null) "S$season:E$episode" else null
    val left = remainingMs?.takeIf { it > 0 }?.let {
        stringResource(R.string.phone_time_left, "${(it / 60_000L).coerceAtLeast(1)} min")
    }
    return listOfNotNull(ep, left).joinToString(" • ")
}

@Composable
private fun PagerDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    if (count < 2) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count.coerceAtMost(8)) { index ->
            val active = index == current
            Box(
                Modifier
                    .height(6.dp)
                    .width(if (active) 20.dp else 6.dp)
                    .clip(PhoneShapes.Pill)
                    .background(if (active) LocalPhoneAccent.current else Color(0xFF3A3F4B))
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeaturedPager(items: List<FeaturedItemCompat>, onClick: (FeaturedItemCompat) -> Unit, modifier: Modifier = Modifier) {
    val pager = rememberPagerState(pageCount = { items.size })
    Column(modifier) {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = PhoneDimens.ScreenPadding),
            pageSpacing = 10.dp,
            key = { "${items[it].id}#$it" },
        ) { page ->
            val item = items[page]
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(PhoneShapes.Card)
                    .background(PhoneColors.Card)
                    .clickable(role = Role.Button) { onClick(item) }
            ) {
                val image = item.backdropUri.ifBlank { item.posterUri }
                if (image.isNotBlank()) {
                    AsyncImage(image, item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.3f to Color.Transparent, 1f to Color(0xEB0E1015))))
                Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    CsKicker(stringResource(R.string.phone_featured), color = LocalPhoneAccent.current)
                    Text(item.name, style = PhoneText.Level.copy(fontSize = 26.sp), color = PhoneColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val meta = listOfNotNull(item.year?.toString(), item.scoreLabel?.let { "★ $it" }, item.supportingLabel).joinToString(" • ")
                    if (meta.isNotEmpty()) Text(meta, style = PhoneText.Meta, color = PhoneColors.Body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        PagerDots(items.size, pager.currentPage, Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp))
    }
}

@Composable
private fun FeedRow(
    section: HomeFeedSectionUiState,
    onMediaClick: (MediaItemCompat) -> Unit,
    onShowMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        CsSectionHeader(section.feed.name) {
            Row(
                Modifier.height(44.dp).clickable(role = Role.Button, onClick = onShowMore).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.phone_show_more), style = PhoneText.Button.copy(fontSize = 14.sp), color = LocalPhoneAccent.current)
                Icon(Icons.Default.ChevronRight, null, tint = LocalPhoneAccent.current, modifier = Modifier.size(18.dp))
            }
        }
        when (val state = section.state) {
            is HomeFeedLoadState.Success -> LazyRow(
                contentPadding = PaddingValues(horizontal = PhoneDimens.ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(PhoneDimens.PosterGap),
                modifier = Modifier.padding(top = 6.dp),
            ) {
                val shown = state.items.take(FeedPosterCount)
                itemsIndexed(shown, key = { index, media -> "${media.gridKeySeed}#$index" }) { _, media ->
                    CsPosterCard(media.name, media.posterUri, onClick = { onMediaClick(media) })
                }
                item(key = "show-more") { ShowMoreCell(extra = state.items.size - shown.size, onClick = onShowMore) }
            }
            HomeFeedLoadState.Loading -> Spacer(Modifier.height(PhoneDimens.PosterHeight + 30.dp))
            HomeFeedLoadState.Error -> Text(
                stringResource(R.string.phone_feed_error),
                style = PhoneText.Body,
                color = PhoneColors.Text2,
                modifier = Modifier.padding(PhoneDimens.ScreenPadding),
            )
        }
    }
}

@Composable
private fun ShowMoreCell(extra: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(PhoneDimens.PosterWidth, PhoneDimens.PosterHeight)
            .clip(PhoneShapes.Poster)
            .background(PhoneColors.Card)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(44.dp).clip(PhoneShapes.Pill).background(PhoneColors.FillButton), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.ChevronRight, null, tint = PhoneColors.Text)
        }
        Text(stringResource(R.string.phone_show_more), style = PhoneText.Button, color = PhoneColors.Text)
        if (extra > 0) Text(stringResource(R.string.phone_show_more_count, extra), style = PhoneText.Meta, color = PhoneColors.Text2)
    }
}
