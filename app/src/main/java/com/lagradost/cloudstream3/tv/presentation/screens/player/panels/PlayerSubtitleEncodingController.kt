package com.lagradost.cloudstream3.tv.presentation.screens.player.panels

import com.lagradost.cloudstream3.tv.presentation.common.SidePanelMenuItem
import com.lagradost.cloudstream3.tv.presentation.screens.player.normalizeSubtitleEncodingValue

internal data class PlayerSubtitleEncodingPanelContent(
    val screen: TvPlayerSubtitlePanelScreen,
    val direction: TvPlayerSubtitlePanelNavigationDirection,
    val items: List<SidePanelMenuItem>,
    val initialFocusedItemId: String?,
    val overrideMainInitialFocusedItemId: String? = null,
)

private data class PlayerSubtitleEncodingNavigationState(
    val screen: TvPlayerSubtitlePanelScreen = TvPlayerSubtitlePanelScreen.Main,
    val direction: TvPlayerSubtitlePanelNavigationDirection = TvPlayerSubtitlePanelNavigationDirection.Forward,
    val focusMainItemId: String? = null,
)

internal class PlayerSubtitleEncodingController {
    private var navigationState = PlayerSubtitleEncodingNavigationState()

    fun resetNavigation() {
        navigationState = PlayerSubtitleEncodingNavigationState()
    }

    fun openSelection(): Boolean {
        if (navigationState.screen == TvPlayerSubtitlePanelScreen.EncodingSelection) {
            return false
        }

        navigationState = PlayerSubtitleEncodingNavigationState(
            screen = TvPlayerSubtitlePanelScreen.EncodingSelection,
            direction = TvPlayerSubtitlePanelNavigationDirection.Forward,
        )
        return true
    }

    fun navigateBack(): Boolean {
        return returnToMain()
    }

    fun returnToMainAfterSelection(): Boolean {
        return returnToMain()
    }

    fun buildPanelContent(
        options: List<TvPlayerSubtitleEncodingOption>,
        selectedValue: String?,
    ): PlayerSubtitleEncodingPanelContent {
        val content = buildSubtitleEncodingPanelContent(
            options = options,
            selectedValue = selectedValue,
            navigationState = navigationState,
        )
        navigationState = consumeSubtitleEncodingNavigationState(navigationState)
        return content
    }

    private fun returnToMain(): Boolean {
        if (navigationState.screen != TvPlayerSubtitlePanelScreen.EncodingSelection) {
            return false
        }

        navigationState = PlayerSubtitleEncodingNavigationState(
            screen = TvPlayerSubtitlePanelScreen.Main,
            direction = TvPlayerSubtitlePanelNavigationDirection.Backward,
            focusMainItemId = SubtitleEncodingItemId,
        )
        return true
    }
}

private fun buildSubtitleEncodingPanelContent(
    options: List<TvPlayerSubtitleEncodingOption>,
    selectedValue: String?,
    navigationState: PlayerSubtitleEncodingNavigationState,
): PlayerSubtitleEncodingPanelContent {
    if (navigationState.screen != TvPlayerSubtitlePanelScreen.EncodingSelection) {
        return PlayerSubtitleEncodingPanelContent(
            screen = navigationState.screen,
            direction = navigationState.direction,
            items = emptyList(),
            initialFocusedItemId = null,
            overrideMainInitialFocusedItemId = navigationState.focusMainItemId,
        )
    }

    val panelItems = buildSubtitleEncodingPanelItems(
        options = options,
        selectedValue = selectedValue,
    )
    return PlayerSubtitleEncodingPanelContent(
        screen = TvPlayerSubtitlePanelScreen.EncodingSelection,
        direction = navigationState.direction,
        items = panelItems.items,
        initialFocusedItemId = panelItems.initialFocusedItemId,
    )
}

private fun consumeSubtitleEncodingNavigationState(
    navigationState: PlayerSubtitleEncodingNavigationState,
): PlayerSubtitleEncodingNavigationState {
    return if (navigationState.screen == TvPlayerSubtitlePanelScreen.EncodingSelection) {
        navigationState
    } else {
        PlayerSubtitleEncodingNavigationState()
    }
}

internal fun buildSubtitleEncodingPanelItems(
    options: List<TvPlayerSubtitleEncodingOption>,
    selectedValue: String?,
): PlayerPanelItems {
    val normalizedSelectedValue = normalizeSubtitleEncodingValue(selectedValue)
    val items = options.map { option ->
        val normalizedOptionValue = normalizeSubtitleEncodingValue(option.value)
        SidePanelMenuItem(
            id = subtitleEncodingOptionItemId(normalizedOptionValue),
            title = option.label,
            selected = normalizedOptionValue == normalizedSelectedValue,
            showTrailingRadio = true,
            actionToken = TvPlayerPanelItemAction.SelectSubtitleEncodingOption(
                value = normalizedOptionValue,
            ),
        )
    }
    val initialFocusedItemId = items.firstOrNull { item ->
        item.selected
    }?.id ?: items.firstOrNull()?.id

    return PlayerPanelItems(
        items = items,
        initialFocusedItemId = initialFocusedItemId,
    )
}

private fun subtitleEncodingOptionItemId(value: String?): String {
    val normalizedValue = normalizeSubtitleEncodingValue(value)
    return if (normalizedValue == null) {
        "${SubtitleEncodingOptionItemPrefix}automatic"
    } else {
        SubtitleEncodingOptionItemPrefix + normalizedValue.replace(Regex("[^A-Za-z0-9_]"), "_")
    }
}
