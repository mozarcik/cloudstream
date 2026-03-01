package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelInlineTextField
import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem

internal fun buildOnlineSubtitlesPanelContent(
    state: TvPlayerOnlineSubtitlesState,
    navigationState: PlayerOnlineSubtitlesNavigationState,
    subtitleLanguageOptions: List<TvPlayerSubtitleLanguageOption>,
    stringResolver: (Int, String) -> String,
): PlayerOnlineSubtitlesPanelContent {
    val (items, initialFocusedItemId) = when (navigationState.screen) {
        TvPlayerSubtitlePanelScreen.OnlineLanguageSelection -> buildLanguagePanelItems(
            state = state,
            subtitleLanguageOptions = subtitleLanguageOptions,
        )
        TvPlayerSubtitlePanelScreen.OnlineSearch,
        TvPlayerSubtitlePanelScreen.Main -> buildOnlinePanelItems(
            state = state,
            stringResolver = stringResolver,
        )
    }

    val resolvedInitialFocusedItemId = navigationState.focusOnlineSearchItemId
        ?.takeIf { requestedId ->
            navigationState.screen == TvPlayerSubtitlePanelScreen.OnlineSearch &&
                items.any { item -> item.id == requestedId && item.enabled }
        } ?: initialFocusedItemId

    val overrideMainFocus = if (
        navigationState.screen == TvPlayerSubtitlePanelScreen.Main &&
        navigationState.focusInternetEntryWhenMainVisible
    ) {
        SubtitleLoadFromInternetItemId
    } else {
        null
    }

    return PlayerOnlineSubtitlesPanelContent(
        screen = navigationState.screen,
        direction = navigationState.direction,
        items = items,
        initialFocusedItemId = resolvedInitialFocusedItemId,
        overrideMainInitialFocusedItemId = overrideMainFocus,
    )
}

internal fun consumeOnlineSubtitlesNavigationFocusState(
    navigationState: PlayerOnlineSubtitlesNavigationState,
): PlayerOnlineSubtitlesNavigationState {
    if (!navigationState.focusInternetEntryWhenMainVisible &&
        navigationState.focusOnlineSearchItemId == null
    ) {
        return navigationState
    }
    return navigationState.copy(
        focusInternetEntryWhenMainVisible = false,
        focusOnlineSearchItemId = null,
    )
}

private fun buildOnlinePanelItems(
    state: TvPlayerOnlineSubtitlesState,
    stringResolver: (Int, String) -> String,
): Pair<List<SidePanelMenuItem>, String?> {
    val queryPlaceholder = stringResolver(
        R.string.search_hint,
        "Search…",
    )

    val items = buildList {
        add(
            SidePanelMenuItem(
                id = SubtitleOnlineQueryItemId,
                title = stringResolver(
                    R.string.search,
                    "Search",
                ),
                inlineTextField = SidePanelInlineTextField(
                    value = state.query,
                    placeholder = queryPlaceholder,
                    valueChangeToken = TvPlayerPanelItemAction.EditOnlineSubtitlesQuery,
                ),
            )
        )
        add(
            SidePanelMenuItem(
                id = SubtitleOnlineLanguageItemId,
                title = stringResolver(
                    R.string.subs_subtitle_languages,
                    "Subtitle language",
                ),
                supportingTexts = listOf(state.selectedLanguageLabel),
                actionToken = TvPlayerPanelItemAction.SelectOnlineSubtitlesLanguage,
            )
        )

        when (state.status) {
            TvPlayerOnlineSubtitlesStatus.Idle -> Unit
            TvPlayerOnlineSubtitlesStatus.Loading -> {
                add(
                    SidePanelMenuItem(
                        id = SubtitleOnlineLoadingItemId,
                        title = stringResolver(
                            R.string.loading,
                            "Loading…",
                        ),
                        enabled = true,
                    )
                )
            }

            TvPlayerOnlineSubtitlesStatus.Empty -> {
                add(
                    SidePanelMenuItem(
                        id = SubtitleOnlineEmptyItemId,
                        title = stringResolver(
                            R.string.tv_feed_empty,
                            "No items in this list",
                        ),
                        enabled = false,
                    )
                )
            }

            TvPlayerOnlineSubtitlesStatus.Error -> {
                add(
                    SidePanelMenuItem(
                        id = SubtitleOnlineErrorItemId,
                        title = state.errorMessage?.takeIf { message ->
                            message.isNotBlank()
                        } ?: onlineSubtitlesSearchFailedMessage(stringResolver),
                        enabled = false,
                    )
                )
                add(
                    SidePanelMenuItem(
                        id = SubtitleOnlineRetryItemId,
                        title = stringResolver(
                            R.string.tv_player_retry,
                            "Try again",
                        ),
                        actionToken = TvPlayerPanelItemAction.RetryOnlineSubtitlesSearch,
                    )
                )
            }

            TvPlayerOnlineSubtitlesStatus.Results -> {
                state.results.forEach { result ->
                    add(
                        SidePanelMenuItem(
                            id = result.id,
                            title = result.title,
                            titleMaxLines = 1,
                            supportingTexts = result.supportingTexts,
                            actionToken = TvPlayerPanelItemAction.SelectOnlineSubtitleResult(
                                resultId = result.id,
                            ),
                        )
                    )
                }
            }
        }
    }

    val initialFocus = when {
        state.query.isBlank() -> SubtitleOnlineQueryItemId
        state.status == TvPlayerOnlineSubtitlesStatus.Results &&
            state.results.isNotEmpty() -> state.results.first().id
        state.status == TvPlayerOnlineSubtitlesStatus.Loading -> SubtitleOnlineLoadingItemId
        state.status == TvPlayerOnlineSubtitlesStatus.Error -> SubtitleOnlineRetryItemId
        else -> SubtitleOnlineQueryItemId
    }

    return items to initialFocus
}

private fun buildLanguagePanelItems(
    state: TvPlayerOnlineSubtitlesState,
    subtitleLanguageOptions: List<TvPlayerSubtitleLanguageOption>,
): Pair<List<SidePanelMenuItem>, String?> {
    val languageItems = subtitleLanguageOptions.map { option ->
        val selected = option.tag.equals(state.selectedLanguageTag, ignoreCase = true)
        SidePanelMenuItem(
            id = "$SubtitleOnlineLanguageOptionItemPrefix${option.tag}",
            title = option.label,
            selected = selected,
            showTrailingRadio = true,
            actionToken = TvPlayerPanelItemAction.SelectOnlineSubtitlesLanguageOption(
                languageTag = option.tag,
            ),
        )
    }
    val initialFocus = languageItems.firstOrNull { item ->
        item.selected
    }?.id ?: languageItems.firstOrNull()?.id
    return languageItems to initialFocus
}

internal fun onlineSubtitlesSearchFailedMessage(
    stringResolver: (Int, String) -> String,
): String {
    return stringResolver(
        R.string.tv_search_failed_to_load,
        "Couldn't load results from this source",
    )
}

internal fun onlineSubtitlesNoSubtitlesLoadedMessage(
    stringResolver: (Int, String) -> String,
): String {
    return stringResolver(
        R.string.no_subtitles_loaded,
        "No subtitles loaded yet",
    )
}
