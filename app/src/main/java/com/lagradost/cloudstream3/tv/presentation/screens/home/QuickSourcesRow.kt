@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.compat.home.sourceId
import com.lagradost.cloudstream3.tv.presentation.common.TvChip
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

private val SeparatorColor = Color.White.copy(alpha = 0.08f)

/**
 * "Source" row under "Continue watching": up to five sources, then always "All sources (N)",
 * which opens the full list. A source is picked with OK only, never by focus.
 */
@Composable
fun QuickSourcesRow(
    quickSources: List<MainAPI>,
    allSourcesCount: Int,
    selectedSource: MainAPI?,
    rowEntryFocusRequester: FocusRequester,
    moreButtonFocusRequester: FocusRequester,
    isInteractive: Boolean,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    onSourceSelected: (MainAPI) -> Unit,
    onMoreClick: () -> Unit,
) {
    val selectedSourceId = selectedSource?.sourceId()
    val quickSourceRequesters = remember(quickSources) { quickSources.map { FocusRequester() } }
    val entryIndex = quickSources.indexOfFirst { it.sourceId() == selectedSourceId }.coerceAtLeast(0)

    Column(modifier = modifier.fillMaxWidth().testTag("sources_row")) {
        Box(
            Modifier
                .padding(end = TvDimens.ScreenHorizontalPadding)
                .fillMaxWidth()
                .height(1.dp)
                .background(SeparatorColor)
        )
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TvDimens.SourceChipGap),
        ) {
            Text(
                text = stringResource(R.string.home_source).uppercase(),
                style = TvTextStyles.HeroEyebrow,
                color = TvColors.Label,
                maxLines = 1,
                modifier = Modifier.padding(end = TvDimens.SourceLabelGap - TvDimens.SourceChipGap),
            )
            if (quickSources.isEmpty()) {
                Text(text = stringResource(R.string.loading), style = TvTextStyles.Body, color = TvColors.Text2)
            }
            quickSources.forEachIndexed { index, source ->
                val sourceId = source.sourceId()
                TvChip(
                    text = source.name,
                    selected = sourceId == selectedSourceId,
                    onClick = { onSourceSelected(source) },
                    accent = TvColors.Text,
                    selectedFill = Color.White.copy(alpha = 0.14f),
                    selectedBorderColor = TvColors.BorderChip,
                    selectedTrailing = { SelectedDot() },
                    modifier = Modifier
                        .widthIn(max = TvDimens.SourceChipMaxWidth)
                        .focusRequester(if (index == entryIndex) rowEntryFocusRequester else quickSourceRequesters[index])
                        .focusProperties {
                            canFocus = isInteractive
                            up = upFocusRequester ?: FocusRequester.Default
                            down = downFocusRequester ?: FocusRequester.Default
                        }
                        .testTag("source_chip_$sourceId"),
                )
            }
            if (allSourcesCount > 0) {
                TvChip(
                    text = stringResource(R.string.tv_home_all_sources, allSourcesCount),
                    selected = false,
                    onClick = onMoreClick,
                    accent = TvColors.Accent,
                    leadingIcon = Icons.Default.GridView,
                    modifier = Modifier
                        .focusRequester(moreButtonFocusRequester)
                        .focusProperties {
                            canFocus = isInteractive
                            // End of the row: nothing to the right.
                            right = FocusRequester.Cancel
                            up = upFocusRequester ?: FocusRequester.Default
                            down = downFocusRequester ?: FocusRequester.Default
                        }
                        .testTag("all_sources_chip"),
                )
            }
        }
    }
}

@Composable
private fun SelectedDot() {
    Box(
        Modifier
            .size(TvDimens.NavRailDotSize + 2.dp)
            .background(TvColors.Accent, CircleShape)
    )
}
