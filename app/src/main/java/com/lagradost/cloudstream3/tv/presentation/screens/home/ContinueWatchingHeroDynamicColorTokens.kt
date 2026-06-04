package com.lagradost.cloudstream3.tv.presentation.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.tv.material3.ColorScheme
import com.lagradost.cloudstream3.tv.util.ArtworkColorRoles
import com.lagradost.cloudstream3.tv.util.rememberArtworkColorRoles

private const val ContinueWatchingCardBorderAlpha = 0.82f
private const val ContinueWatchingOutlinedBorderAlpha = 0.72f

@Immutable
internal data class ContinueWatchingHeroColorTokens(
    val resumeContainerColor: Color,
    val resumeContentColor: Color,
    val resumeFocusedContainerColor: Color,
    val resumeFocusedContentColor: Color,
    val detailsContentColor: Color,
    val detailsFocusedContainerColor: Color,
    val detailsFocusedContentColor: Color,
    val detailsBorderColor: Color,
    val detailsFocusedBorderColor: Color,
    val removeContentColor: Color,
    val removeFocusedContainerColor: Color,
    val removeFocusedContentColor: Color,
    val removeBorderColor: Color,
    val removeFocusedBorderColor: Color,
    val cardFocusedBorderColor: Color,
    val progressIndicatorColor: Color,
)

@Composable
internal fun rememberContinueWatchingHeroColorTokens(
    artworkUrl: String?,
    baseColorScheme: ColorScheme,
): ContinueWatchingHeroColorTokens {
    val artworkColorRoles = rememberArtworkColorRoles(
        artworkUrl = artworkUrl,
        isDarkScheme = baseColorScheme.background.luminance() <= 0.5f,
    )

    return remember(baseColorScheme, artworkColorRoles) {
        artworkColorRoles?.toContinueWatchingHeroColorTokens() ?: baseColorScheme.toContinueWatchingHeroColorTokens()
    }
}

private fun ColorScheme.toContinueWatchingHeroColorTokens(): ContinueWatchingHeroColorTokens {
    return ContinueWatchingHeroColorTokens(
        resumeContainerColor = primary,
        resumeContentColor = onPrimary,
        resumeFocusedContainerColor = primary,
        resumeFocusedContentColor = onPrimary,
        detailsContentColor = onSurface,
        detailsFocusedContainerColor = onSurface,
        detailsFocusedContentColor = inverseOnSurface,
        detailsBorderColor = onSurfaceVariant.copy(alpha = 0.4f),
        detailsFocusedBorderColor = onSurfaceVariant,
        removeContentColor = onSurface,
        removeFocusedContainerColor = onSurface,
        removeFocusedContentColor = inverseOnSurface,
        removeBorderColor = onSurfaceVariant.copy(alpha = 0.4f),
        removeFocusedBorderColor = onSurfaceVariant,
        cardFocusedBorderColor = primary.copy(alpha = ContinueWatchingCardBorderAlpha),
        progressIndicatorColor = primary,
    )
}

private fun ArtworkColorRoles.toContinueWatchingHeroColorTokens(): ContinueWatchingHeroColorTokens {
    return ContinueWatchingHeroColorTokens(
        resumeContainerColor = primaryContainer,
        resumeContentColor = onPrimaryContainer,
        resumeFocusedContainerColor = primary,
        resumeFocusedContentColor = onPrimary,
        detailsContentColor = onSurface,
        detailsFocusedContainerColor = secondaryContainer,
        detailsFocusedContentColor = onSecondaryContainer,
        detailsBorderColor = secondary.copy(alpha = ContinueWatchingOutlinedBorderAlpha),
        detailsFocusedBorderColor = secondary,
        removeContentColor = onSurface,
        removeFocusedContainerColor = tertiaryContainer,
        removeFocusedContentColor = onTertiaryContainer,
        removeBorderColor = tertiary.copy(alpha = ContinueWatchingOutlinedBorderAlpha),
        removeFocusedBorderColor = tertiary,
        cardFocusedBorderColor = primary.copy(alpha = ContinueWatchingCardBorderAlpha),
        progressIndicatorColor = primary,
    )
}
