package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
data class SettingsEntry(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector? = null,
    val iconUrl: String? = null,
    val fallbackIconRes: Int? = null,
    val type: SettingsEntryType = SettingsEntryType.Item,
    val toggleValue: Boolean = false,
    val sliderValue: Int = 0,
    val sliderRange: IntRange = 0..0,
    val sliderStep: Int = 1,
    val valueText: String? = null,
    /** Selected state of [SettingsEntryType.Radio] rows. */
    val showCheckmark: Boolean = false,
    val trailingColorArgb: Int? = null,
    /** Initials tile used instead of an icon (accounts, plugins without an icon). */
    val initials: String? = null,
    /** 46 dp row (account list) instead of 48 dp. */
    val compactRow: Boolean = false,
    /** When set, [initials] is drawn on a plugin-sized tile of this color (otherwise a small neutral tile). */
    val tileColorArgb: Int? = null,
    /** Small status icon before the chevron (e.g. "downloaded" plugin). */
    val trailingIcon: ImageVector? = null,
    val stableKey: String,
    val nextScreenId: String? = null,
    val action: (() -> Unit)? = null,
    val onToggleChanged: ((Boolean) -> Unit)? = null,
    val onSliderChanged: ((Int) -> Unit)? = null
)

enum class SettingsEntryType {
    /** Row with an optional chevron (opens the next level or runs an action). */
    Item,
    /** Section label, not focusable. */
    Header,
    Toggle,
    Slider,
    /** Single choice (leading radio); OK saves and stays on the level. */
    Radio,
    /** Multiple choice (leading checkbox); [SettingsEntry.toggleValue] is the checked state. */
    MultiSelect,
    /** Plain text block (title + optional subtitle), not focusable. */
    Message,
}

/** Multi-select row: same data as a toggle, drawn with a checkbox. */
internal fun multiSelectEntry(
    stableKey: String,
    title: String,
    subtitle: String?,
    fallbackIconRes: Int? = null,
    value: Boolean,
    onValueChanged: (Boolean) -> Unit
): SettingsEntry = SettingsEntry(
    title = title,
    subtitle = subtitle,
    fallbackIconRes = fallbackIconRes,
    stableKey = stableKey,
    type = SettingsEntryType.MultiSelect,
    toggleValue = value,
    onToggleChanged = onValueChanged
)

/** Thrown from [SettingsScreen.load] to show the Error state with a title and a message. */
class SettingsLoadException(val errorTitle: String?, message: String) : Exception(message)

/**
 * Lets the active level fill the info panel on its right (alpha 1, `Preview` background) while it
 * is shown; pass null to clear it.
 */
internal val LocalSettingsInfoPanel = compositionLocalOf<(( @Composable () -> Unit)?) -> Unit> { {} }

/** False inside the preview panel: its rows are drawn but can never take focus. */
internal val LocalSettingsPanelFocusable = compositionLocalOf { true }

interface SettingsScreen {
    val id: String
    val title: String
    val hasCustomContent: Boolean
        get() = false

    /** Optional sentence under the title (multi-select lists). */
    val description: String?
        get() = null

    suspend fun load(): List<SettingsEntry> = emptyList()

    /** Called before the level is reloaded from the Error state ("Try Again"). */
    suspend fun onRetry() = Unit

    @Composable
    fun Content(
        modifier: Modifier,
        contentPadding: PaddingValues,
        isPreview: Boolean,
        onBack: () -> Unit,
        onDataChanged: (String) -> Unit
    ) = Unit
}

@Stable
class ScreenInstance(
    val id: String,
    val instanceId: Long,
    initialFocusKey: String? = null,
    firstVisibleItemIndex: Int = 0,
    firstVisibleItemOffset: Int = 0,
) {
    var focusKey: String? by mutableStateOf(initialFocusKey)
    val listState: LazyListState = LazyListState(
        firstVisibleItemIndex = firstVisibleItemIndex,
        firstVisibleItemScrollOffset = firstVisibleItemOffset
    )
}

class SettingsRegistry(
    screens: List<SettingsScreen>,
    private val dynamicScreenResolver: ((String) -> SettingsScreen?)? = null
) {
    private val screenById: Map<String, SettingsScreen> = screens.associateBy { it.id }

    fun get(screenId: String): SettingsScreen =
        requireNotNull(screenById[screenId] ?: dynamicScreenResolver?.invoke(screenId)) {
            "Missing SettingsScreen for id=$screenId"
        }

    fun require(screenId: String): SettingsScreen =
        get(screenId)
}

sealed interface SettingsScreenDataState {
    data object Loading : SettingsScreenDataState

    @Immutable
    data class Ready(val entries: List<SettingsEntry>) : SettingsScreenDataState

    @Immutable
    data class Error(val message: String, val title: String? = null) : SettingsScreenDataState
}

enum class SettingsNavigationDirection {
    Forward,
    Backward
}

enum class SettingsLoadSource {
    Cache,
    Fetch
}

@Immutable
data class PreviewCacheKey(
    val parentInstanceId: Long,
    val screenId: String
)
