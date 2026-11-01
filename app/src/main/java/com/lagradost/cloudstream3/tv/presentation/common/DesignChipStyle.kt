@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.lagradost.cloudstream3.tv.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.Border
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens

/** Pill filter chip from the redesign: outlined, accent when selected, white fill on focus (no scale). */
object DesignChipStyle {
    val shape get() = FilterChipDefaults.shape(shape = CircleShape)

    val scale get() = FilterChipDefaults.scale(focusedScale = 1f)

    @Composable
    fun colors(accent: Color) = FilterChipDefaults.colors(
        containerColor = TvColors.FillChip,
        contentColor = TvColors.Body,
        focusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
        focusedContentColor = MaterialTheme.colorScheme.inverseOnSurface,
        pressedContainerColor = MaterialTheme.colorScheme.inverseSurface,
        pressedContentColor = MaterialTheme.colorScheme.inverseOnSurface,
        selectedContainerColor = accent.copy(alpha = 0.10f),
        selectedContentColor = accent,
        focusedSelectedContainerColor = MaterialTheme.colorScheme.inverseSurface,
        focusedSelectedContentColor = MaterialTheme.colorScheme.inverseOnSurface,
        pressedSelectedContainerColor = MaterialTheme.colorScheme.inverseSurface,
        pressedSelectedContentColor = MaterialTheme.colorScheme.inverseOnSurface,
    )

    @Composable
    fun border(accent: Color) = TvDimens.ChipBorderWidth.let { width ->
        FilterChipDefaults.border(
            border = Border(BorderStroke(width, TvColors.BorderChip), shape = CircleShape),
            focusedBorder = Border(BorderStroke(width, MaterialTheme.colorScheme.inverseSurface), shape = CircleShape),
            pressedBorder = Border(BorderStroke(width, MaterialTheme.colorScheme.inverseSurface), shape = CircleShape),
            selectedBorder = Border(BorderStroke(width, accent), shape = CircleShape),
            focusedSelectedBorder = Border(BorderStroke(width, MaterialTheme.colorScheme.inverseSurface), shape = CircleShape),
            pressedSelectedBorder = Border(BorderStroke(width, MaterialTheme.colorScheme.inverseSurface), shape = CircleShape),
        )
    }
}
