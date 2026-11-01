package com.lagradost.cloudstream3.phone.presentation.screens.search

import com.lagradost.cloudstream3.phone.presentation.common.navBarBottom
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.phone.presentation.common.CsButton
import com.lagradost.cloudstream3.phone.presentation.common.LocalPermanentMenu
import androidx.compose.material.icons.filled.Menu
import com.lagradost.cloudstream3.phone.presentation.common.CsIconButton
import com.lagradost.cloudstream3.phone.presentation.common.CsPosterCard
import com.lagradost.cloudstream3.phone.presentation.common.mediaMeta
import com.lagradost.cloudstream3.phone.presentation.theme.LocalPhoneAccent
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneColors
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneDimens
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneShapes
import com.lagradost.cloudstream3.phone.presentation.theme.PhoneText
import com.lagradost.cloudstream3.tv.compat.home.MediaItemCompat
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeFeedLoadState
import com.lagradost.cloudstream3.tv.presentation.screens.search.SearchFeedGridSelectionStore
import com.lagradost.cloudstream3.tv.presentation.screens.search.SearchSectionUiState
import com.lagradost.cloudstream3.tv.presentation.screens.search.SearchViewModel

private const val ResultRowCount = 11

/** Results are grouped per source; every source loads and fails on its own. */
@Composable
fun PhoneSearchScreen(
    onOpenMenu: () -> Unit,
    onMediaClick: (MediaItemCompat) -> Unit,
    onShowMore: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (!state.hasSearched) focus.requestFocus() }

    Column(modifier.fillMaxSize().background(PhoneColors.Ground).statusBarsPadding().imePadding()) {
        Row(Modifier.padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Search is a top-level destination of the menu: menu button, not Back (system Back still pops it).
            if (!LocalPermanentMenu.current) {
                CsIconButton(Icons.Default.Menu, stringResource(R.string.phone_open_menu), onOpenMenu, container = Color.Transparent)
            } else {
                androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))
            }
            Row(
                Modifier.weight(1f).height(52.dp).clip(PhoneShapes.Pill).background(PhoneColors.Card).padding(start = 18.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.Search, null, tint = PhoneColors.Label, modifier = Modifier.size(22.dp))
                Box(Modifier.weight(1f)) {
                    if (state.query.isEmpty()) Text(stringResource(R.string.phone_search_hint), style = PhoneText.Row, color = PhoneColors.Label)
                    BasicTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChanged,
                        singleLine = true,
                        textStyle = PhoneText.Row.copy(color = PhoneColors.Text),
                        cursorBrush = SolidColor(PhoneColors.Text),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.onSearchSubmitted() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
                if (state.query.isNotEmpty()) {
                    CsIconButton(Icons.Default.Close, stringResource(R.string.phone_clear), { viewModel.onQueryChanged(""); viewModel.onSearchSubmitted() }, container = Color.Transparent, size = 40.dp)
                }
            }
        }

        if (!state.hasSearched) {
            Text(stringResource(R.string.phone_search_empty), style = PhoneText.Body, color = PhoneColors.Text2, modifier = Modifier.padding(PhoneDimens.ScreenPadding))
        }
        LazyColumn(contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp + navBarBottom()), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            itemsIndexed(state.sections, key = { index, section -> "${section.id}#$index" }) { _, section ->
                SectionBlock(
                    section = section,
                    onMediaClick = onMediaClick,
                    onShowMore = {
                        SearchFeedGridSelectionStore.setQuery(state.submittedQuery)
                        SearchFeedGridSelectionStore.setSelectedSection(section)
                        onShowMore()
                    },
                    onRetry = viewModel::onSearchSubmitted,
                )
            }
        }
    }
}

@Composable
private fun SectionBlock(
    section: SearchSectionUiState,
    onMediaClick: (MediaItemCompat) -> Unit,
    onShowMore: () -> Unit,
    onRetry: () -> Unit,
) {
    val accent = LocalPhoneAccent.current
    when (val s = section.state) {
        HomeFeedLoadState.Loading -> Column {
            Text(section.title, style = PhoneText.Section, color = PhoneColors.Text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp))
            LazyRow(contentPadding = PaddingValues(16.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(PhoneDimens.PosterGap)) {
                items(4) { Box(Modifier.size(PhoneDimens.PosterWidth, PhoneDimens.PosterHeight).clip(PhoneShapes.Poster).background(PhoneColors.Card)) }
            }
        }
        HomeFeedLoadState.Error -> Column(Modifier.padding(horizontal = 16.dp)) {
            Text(section.title, style = PhoneText.Section, color = PhoneColors.Text, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShapeError).background(PhoneColors.Card).padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Default.Warning, null, tint = accent)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.phone_source_not_responding), style = PhoneText.Button, color = PhoneColors.Text)
                    Text(stringResource(R.string.phone_other_results_unaffected), style = PhoneText.Meta, color = PhoneColors.Text2)
                }
                CsButton(stringResource(R.string.phone_try_again), onRetry, height = 40.dp)
            }
        }
        is HomeFeedLoadState.Success -> if (s.items.isEmpty()) Unit else Column {
            Row(Modifier.padding(start = 16.dp, end = 12.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(section.title, style = PhoneText.Section, color = PhoneColors.Text)
                Text(stringResource(R.string.phone_results_count, s.items.size), style = PhoneText.Meta, color = PhoneColors.Text2, modifier = Modifier.weight(1f).padding(bottom = 2.dp))
                if (s.items.size > ResultRowCount) {
                    Row(Modifier.height(44.dp).clickable(role = Role.Button, onClick = onShowMore).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.phone_show_more), style = PhoneText.Button.copy(fontSize = 14.sp), color = accent)
                        Icon(Icons.Default.ChevronRight, null, tint = accent, modifier = Modifier.size(18.dp))
                    }
                }
            }
            LazyRow(contentPadding = PaddingValues(16.dp, 8.dp), horizontalArrangement = Arrangement.spacedBy(PhoneDimens.PosterGap)) {
                itemsIndexed(s.items.take(ResultRowCount), key = { index, item -> "${item.gridKeySeed}#$index" }) { _, item ->
                    CsPosterCard(item.name, item.posterUri, { onMediaClick(item) }, meta = mediaMeta(item))
                }
            }
        }
    }
}

private val RoundedCornerShapeError = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
