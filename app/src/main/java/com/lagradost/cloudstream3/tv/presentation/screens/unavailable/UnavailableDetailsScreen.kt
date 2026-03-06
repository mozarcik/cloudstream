package com.lagradost.cloudstream3.tv.presentation.screens.unavailable

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.isEpisodeBased
import com.lagradost.cloudstream3.tv.presentation.focus.FocusRequestEffect
import com.lagradost.cloudstream3.tv.presentation.screens.movies.DotSeparatedRow
import com.lagradost.cloudstream3.tv.presentation.screens.movies.MovieDetailsBackdrop
import com.lagradost.cloudstream3.tv.presentation.screens.movies.rememberChildPadding

private val UnavailableBottomPadding = 96.dp
private val UnavailableButtonsSpacing = 20.dp
private val UnavailableButtonsTopSpacing = 20.dp
private val UnavailableStatusTopSpacing = 24.dp
private val UnavailableStatusIconSpacing = 10.dp
private val UnavailableDescriptionTopSpacing = 10.dp
private val UnavailableDetailsTopSpacing = 12.dp
private val UnavailableStatusContainerPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
private val UnavailableActionsWidth = 520.dp
private val UnavailableActionButtonMinWidth = 168.dp
private val UnavailableActionIconSpacing = 8.dp
private val UnavailableBottomContentSpacing = 8.dp
private const val HeroSectionHeightRatio = 1f

@Immutable
data class UnavailableDetailsUiModel(
    val title: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val description: String? = null,
    val type: TvType? = null,
    val year: Int? = null,
    val providerName: String? = null,
)

@Composable
fun UnavailableDetailsScreen(
    state: UnavailableDetailsUiModel,
    showRemoveFromLibraryAction: Boolean,
    onRemoveFromLibrary: () -> Unit,
    onManualSearch: (String) -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val removeButtonFocusRequester = remember { FocusRequester() }
    val manualSearchFocusRequester = remember { FocusRequester() }
    val childPadding = rememberChildPadding()
    val listState = rememberLazyListState()
    val heroSectionHeight = LocalConfiguration.current.screenHeightDp.dp * HeroSectionHeightRatio
    val typeLabel = rememberTypeLabel(type = state.type)
    val metadata = remember(state.year, typeLabel) {
        listOfNotNull(
            state.year?.toString(),
            typeLabel
        )
    }
    val statusSubtitle = remember(state.providerName) {
        val providerName = state.providerName?.trim().orEmpty()
        if (providerName.isBlank()) {
            null
        } else {
            providerName
        }
    }
    val artworkUri = remember(state.posterUrl, state.backdropUrl) {
        state.backdropUrl?.takeIf { it.isNotBlank() } ?: state.posterUrl?.takeIf { it.isNotBlank() }
    }
    val safeTitle = state.title.ifBlank { stringResource(R.string.details) }
    val manualSearchQuery = remember(state.title) {
        state.title.trim()
    }
    val initialFocusRequester = if (showRemoveFromLibraryAction) {
        removeButtonFocusRequester
    } else {
        manualSearchFocusRequester
    }

    FocusRequestEffect(
        requester = initialFocusRequester,
        requestKey = state.title to showRemoveFromLibraryAction,
    )

    BackHandler(onBack = onBackPressed)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!artworkUri.isNullOrBlank()) {
            MovieDetailsBackdrop(
                posterUri = artworkUri,
                title = safeTitle,
                modifier = Modifier.fillMaxSize(),
                gradientColor = MaterialTheme.colorScheme.background
            )
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = UnavailableBottomPadding),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroSectionHeight),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.64f)
                            .padding(start = childPadding.start)
                    ) {
                        Text(
                            text = safeTitle,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (metadata.isNotEmpty()) {
                            DotSeparatedRow(
                                modifier = Modifier.padding(top = UnavailableDetailsTopSpacing),
                                texts = metadata
                            )
                        }

                        state.description
                            ?.takeIf { it.isNotBlank() }
                            ?.let { description ->
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = UnavailableDescriptionTopSpacing)
                                )
                            }

                        Surface(
                            modifier = Modifier
                                .padding(top = UnavailableStatusTopSpacing)
                                .fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = SurfaceDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.86f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(UnavailableStatusContainerPadding)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(UnavailableStatusIconSpacing)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.tv_unavailable_details_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = statusSubtitle?.let { provider ->
                                        stringResource(
                                            R.string.tv_unavailable_details_message_with_provider,
                                            provider
                                        )
                                    } ?: stringResource(R.string.tv_unavailable_details_message),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(UnavailableButtonsSpacing),
                            modifier = Modifier
                                .padding(top = UnavailableButtonsTopSpacing)
                                .widthIn(max = UnavailableActionsWidth)
                        ) {
                            if (showRemoveFromLibraryAction) {
                                Button(
                                    onClick = onRemoveFromLibrary,
                                    modifier = Modifier
                                        .widthIn(min = UnavailableActionButtonMinWidth)
                                        .focusRequester(removeButtonFocusRequester)
                                        .focusProperties {
                                            down = manualSearchFocusRequester
                                            right = manualSearchFocusRequester
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(UnavailableActionIconSpacing))
                                    Text(
                                        text = stringResource(R.string.tv_unavailable_details_remove_button),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    if (manualSearchQuery.isNotBlank()) {
                                        onManualSearch(manualSearchQuery)
                                    }
                                },
                                modifier = Modifier
                                    .widthIn(min = UnavailableActionButtonMinWidth)
                                    .focusRequester(manualSearchFocusRequester)
                                    .focusProperties {
                                        left = if (showRemoveFromLibraryAction) {
                                            removeButtonFocusRequester
                                        } else {
                                            FocusRequester.Default
                                        }
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(UnavailableActionIconSpacing))
                                Text(
                                    text = stringResource(R.string.tv_unavailable_details_search_button),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(UnavailableBottomContentSpacing))
                }
            }
        }
    }
}

@Composable
private fun rememberTypeLabel(type: TvType?): String? {
    val tvLabel = stringResource(R.string.tv_series_singular)
    val movieLabel = stringResource(R.string.movies_singular)
    val resolvedType = type ?: return null

    return if (resolvedType.isEpisodeBased()) {
        tvLabel
    } else {
        movieLabel
    }
}
