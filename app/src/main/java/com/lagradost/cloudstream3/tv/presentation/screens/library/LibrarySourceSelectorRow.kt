package com.lagradost.cloudstream3.tv.presentation.screens.library

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import com.lagradost.cloudstream3.R
import androidx.tv.material3.Text
import com.lagradost.cloudstream3.tv.presentation.common.TvChip
import com.lagradost.cloudstream3.tv.presentation.screens.home.HomeContentHorizontalPadding
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens
import com.lagradost.cloudstream3.tv.presentation.theme.TvTextStyles

/**
 * "LISTS FROM" label + one chip per logged-in sync account (mockup Library). A plain row: the
 * number of accounts is small, so no lazy list is needed. Entering it lands on the selected chip.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LibrarySourceSelectorRow(
    sources: List<String>,
    selectedSource: String,
    rowEntryFocusRequester: FocusRequester,
    isInteractive: Boolean,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier,
    onSourceSelected: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    val refreshRequester = remember { FocusRequester() }
    val sourceRequesters = remember(sources) {
        sources.map { FocusRequester() }
    }
    val selectedIndex = sources.indexOf(selectedSource).let { index ->
        if (index < 0) 0 else index
    }

    fun requesterFor(index: Int): FocusRequester {
        return if (index == selectedIndex) {
            rowEntryFocusRequester
        } else {
            sourceRequesters[index]
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(TvDimens.LibrarySourceChipGap),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(TvDimens.LibrarySourceRowHeight)
            // Entering the row from above must land on the selected source, not the nearest chip.
            .focusRestorer(rowEntryFocusRequester)
            .focusGroup()
    ) {
        Text(
            text = stringResource(R.string.tv_library_lists_from).uppercase(),
            style = TvTextStyles.Eyebrow,
            color = TvColors.Label,
            maxLines = 1,
            modifier = Modifier.padding(end = TvDimens.LibrarySourceLabelGap - TvDimens.LibrarySourceChipGap),
        )
        sources.forEachIndexed { index, source ->
            TvChip(
                text = source,
                selected = source == selectedSource,
                onClick = { onSourceSelected(source) },
                accent = TvColors.Selected,
                height = TvDimens.LibraryChipHeight,
                modifier = Modifier
                    .widthIn(max = TvDimens.LibrarySourceChipMaxWidth)
                    .focusRequester(requesterFor(index))
                    .focusProperties {
                        canFocus = isInteractive
                        up = upFocusRequester ?: FocusRequester.Default
                        down = downFocusRequester ?: FocusRequester.Default
                        if (index == sources.lastIndex) right = refreshRequester
                    }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        TvChip(
            text = stringResource(R.string.tv_library_refresh),
            selected = false,
            onClick = onRefresh,
            accent = TvColors.Selected,
            height = TvDimens.LibraryChipHeight,
            modifier = Modifier
                .padding(end = HomeContentHorizontalPadding)
                .focusRequester(refreshRequester)
                .focusProperties {
                    canFocus = isInteractive
                    up = upFocusRequester ?: FocusRequester.Default
                    down = downFocusRequester ?: FocusRequester.Default
                    right = FocusRequester.Cancel
                }
        )
    }
}
