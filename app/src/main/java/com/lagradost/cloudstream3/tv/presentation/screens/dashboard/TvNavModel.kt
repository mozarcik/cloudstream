package com.lagradost.cloudstream3.tv.presentation.screens.dashboard

import androidx.compose.runtime.Immutable
import com.lagradost.cloudstream3.tv.presentation.screens.Screens

/** Screens reachable from the navigation menu (the rail shows the same set). */
internal enum class TvNavTarget(val screen: Screens) {
    Home(Screens.Home),
    Search(Screens.Search),
    Downloads(Screens.Downloads),
    Settings(Screens.Settings),
}

/** Where the user currently is, as the menu and the rail mark it. */
@Immutable
internal sealed interface TvNavSelection {
    data class Screen(val target: TvNavTarget) : TvNavSelection

    /** A library list is open; [listKey] is null when none is picked yet. */
    data class Library(val listKey: String?) : TvNavSelection

    data object None : TvNavSelection
}

@Immutable
internal data class TvNavList(val key: String, val title: String, val count: Int)

/** One account's lists in the menu (TMDB, AniList, Simkl, Local, ...). */
@Immutable
internal data class TvNavListGroup(
    val providerName: String,
    val lists: List<TvNavList>,
    val isLoading: Boolean,
    val hasError: Boolean = false,
)

internal fun navSelectionForRoute(route: String?, openedListKey: String?): TvNavSelection {
    return when (route) {
        Screens.Home.name, Screens.HomeFeedGrid.name -> TvNavSelection.Screen(TvNavTarget.Home)
        Screens.Search.name, Screens.SearchFeedGrid.name -> TvNavSelection.Screen(TvNavTarget.Search)
        Screens.Downloads.name -> TvNavSelection.Screen(TvNavTarget.Downloads)
        Screens.Settings.name -> TvNavSelection.Screen(TvNavTarget.Settings)
        Screens.Library.name, Screens.LibraryFeedGrid.name -> TvNavSelection.Library(openedListKey)
        null -> TvNavSelection.Screen(TvNavTarget.Home)
        else -> TvNavSelection.None
    }
}
