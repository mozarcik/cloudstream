package com.lagradost.cloudstream3.tv.presentation.screens.player

import androidx.compose.runtime.Composable

@Composable
fun TvPlayerScreen(
    onBackPressed: () -> Unit,
    tvPlayerScreenViewModel: TvPlayerScreenViewModel,
) {
    PlayerRoute(
        onBackPressed = onBackPressed,
        tvPlayerScreenViewModel = tvPlayerScreenViewModel,
    )
}
