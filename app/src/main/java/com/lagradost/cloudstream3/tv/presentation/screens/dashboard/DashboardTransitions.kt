package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens

/** Screen change from the menu: the old screen fades out, the new one fades in. */
internal fun dashboardEnterTransition(): EnterTransition = fadeIn(tween(TvDimens.MaxAnimationMs))

internal fun dashboardExitTransition(): ExitTransition = fadeOut(tween(TvDimens.MaxAnimationMs))

internal fun NavGraphBuilder.dashboardComposable(
    route: String,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable(route) { entry ->
        val scope = this
        Box(Modifier.fillMaxSize()) {
            BackHandler(onBack = LocalDashboardBack.current)
            scope.content(entry)
        }
    }
}
