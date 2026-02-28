package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
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
    val showCheckmark: Boolean = false,
    val trailingColorArgb: Int? = null,
    val stableKey: String,
    val nextScreenId: String? = null,
    val action: (() -> Unit)? = null,
    val onToggleChanged: ((Boolean) -> Unit)? = null,
    val onSliderChanged: ((Int) -> Unit)? = null
)

enum class SettingsEntryType {
    Item,
    Header,
    Toggle,
    Slider
}

interface SettingsScreen {
    val id: String
    val title: String
    val hasCustomContent: Boolean
        get() = false

    suspend fun load(): List<SettingsEntry> = emptyList()

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
    data class Error(val message: String) : SettingsScreenDataState
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
