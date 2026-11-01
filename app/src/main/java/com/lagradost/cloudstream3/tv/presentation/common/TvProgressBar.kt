package com.lagradost.cloudstream3.tv.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.tv.material3.MaterialTheme
import com.lagradost.cloudstream3.designsystem.CsProgressBar
import com.lagradost.cloudstream3.tv.presentation.theme.TvColors
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens

/** 3 dp progress bar (mockup 6 px) in the title's accent color; draws in a single node. */
@Composable
fun TvProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    track: Color = TvColors.ProgressTrack,
    height: Dp = TvDimens.ProgressHeight,
) {
    CsProgressBar(progress = progress, accent = accent, track = track, height = height, modifier = modifier)
}
