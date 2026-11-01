package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.lagradost.cloudstream3.tv.presentation.screens.Screens
import com.lagradost.cloudstream3.tv.presentation.theme.TvDimens

/** Tab change: the old tab leaves toward its side and fades, the new one comes from the other side. */
private const val TabSlideDivisor = 6

/** Space below the top bar; read by each route itself so a leaving tab keeps its own inset. */
internal val LocalDashboardTopInset = compositionLocalOf<() -> Dp> { { 0.dp } }

/** Position of a route's tab in the top bar (sub-screens belong to their tab); null for non-tab routes. */
private fun tabGroup(route: String?): Screens? = when (route) {
    Screens.Home.name, Screens.HomeFeedGrid.name -> Screens.Home
    Screens.Library.name, Screens.LibraryFeedGrid.name -> Screens.Library
    Screens.Search.name, Screens.SearchFeedGrid.name -> Screens.Search
    Screens.Downloads.name -> Screens.Downloads
    Screens.Settings.name -> Screens.Settings
    else -> null
}

/** +1 when [target] is to the right of [initial] in the top bar, -1 when to the left, 0 for no sideways move. */
internal fun tabDirection(initial: String?, target: String?): Int {
    val from = tabGroup(initial) ?: return 0
    val to = tabGroup(target) ?: return 0
    if (from == to) return 0
    return if (TopBarTabs.indexOf(to) > TopBarTabs.indexOf(from)) 1 else -1
}

internal fun dashboardEnterTransition(initial: String?, target: String?): EnterTransition {
    val direction = tabDirection(initial, target)
    val fade = fadeIn(tween(TvDimens.MaxAnimationMs))
    if (direction == 0) return fade
    return slideInHorizontally(tween(TvDimens.MaxAnimationMs)) { it * direction / TabSlideDivisor } + fade
}

internal fun dashboardExitTransition(initial: String?, target: String?): ExitTransition {
    val direction = tabDirection(initial, target)
    val fade = fadeOut(tween(TvDimens.MaxAnimationMs))
    if (direction == 0) return fade
    return slideOutHorizontally(tween(TvDimens.MaxAnimationMs)) { -it * direction / TabSlideDivisor } + fade
}

internal fun NavGraphBuilder.dashboardComposable(
    route: String,
    applyTopInset: Boolean = true,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    composable(route) { entry ->
        val scope = this
        val inset = LocalDashboardTopInset.current
        Box(
            Modifier.fillMaxSize().layout { measurable, constraints ->
                val top = if (applyTopInset) inset().roundToPx().coerceAtMost(constraints.maxHeight) else 0
                val placeable = measurable.measure(constraints.offset(vertical = -top))
                layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, top) }
            }
        ) {
            scope.content(entry)
        }
    }
}
