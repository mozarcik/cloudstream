package com.lagradost.cloudstream3.tv.presentation.screens.settings.masterdetail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Stable
class SettingsHostState(
    private val registry: SettingsRegistry,
    private val coroutineScope: CoroutineScope,
    rootScreenId: String,
) {
    private var nextInstanceId by mutableLongStateOf(1L)
    private val loadingJobs: MutableMap<String, Job> = mutableMapOf()
    private val preparedPreviewInstances = mutableStateMapOf<PreviewCacheKey, ScreenInstance>()
    private val refreshVersionByScreenId = mutableStateMapOf<String, Int>()

    val stack: SnapshotStateList<ScreenInstance> = listOf(createInstance(rootScreenId))
        .toMutableStateList()
    val screenData = mutableStateMapOf<String, SettingsScreenDataState>()
    val lastLoadSourceByScreenId = mutableStateMapOf<String, SettingsLoadSource>()

    var navigationDirection by mutableStateOf(SettingsNavigationDirection.Forward)
        private set

    val activeInstance: ScreenInstance
        get() = stack.last()

    val depth: Int
        get() = stack.size

    fun screen(screenId: String): SettingsScreen = registry.get(screenId)

    fun screenTitle(screenId: String): String = screen(screenId).title

    fun refreshVersion(screenId: String): Int = refreshVersionByScreenId[screenId] ?: 0

    fun dataState(screenId: String): SettingsScreenDataState? = screenData[screenId]

    fun readyEntries(screenId: String): List<SettingsEntry>? {
        return (screenData[screenId] as? SettingsScreenDataState.Ready)?.entries
    }

    fun updateEntry(
        screenId: String,
        stableKey: String,
        update: (SettingsEntry) -> SettingsEntry
    ) {
        val currentState = screenData[screenId] as? SettingsScreenDataState.Ready ?: return
        val index = currentState.entries.indexOfFirst { entry -> entry.stableKey == stableKey }
        if (index < 0) return

        val currentEntry = currentState.entries[index]
        val updatedEntry = update(currentEntry)
        if (updatedEntry == currentEntry) return

        val updatedEntries = currentState.entries.toMutableList().apply {
            this[index] = updatedEntry
        }
        screenData[screenId] = SettingsScreenDataState.Ready(updatedEntries)
    }

    fun focusedEntry(instance: ScreenInstance): SettingsEntry? {
        val entries = readyEntries(instance.id) ?: return null
        val selectedKey = instance.focusKey ?: entries.firstOrNull()?.stableKey ?: return null
        return entries.firstOrNull { it.stableKey == selectedKey }
    }

    fun ensureLoaded(screenId: String) {
        when (screenData[screenId]) {
            is SettingsScreenDataState.Ready -> {
                lastLoadSourceByScreenId[screenId] = SettingsLoadSource.Cache
                return
            }
            is SettingsScreenDataState.Loading -> return
            else -> Unit
        }

        if (loadingJobs[screenId]?.isActive == true) return

        screenData[screenId] = SettingsScreenDataState.Loading
        loadingJobs[screenId] = coroutineScope.launch {
            val result = runCatching {
                val screen = registry.require(screenId)
                withContext(Dispatchers.IO) {
                    screen.load()
                }
            }

            result.onSuccess { entries ->
                screenData[screenId] = SettingsScreenDataState.Ready(entries)
                lastLoadSourceByScreenId[screenId] = SettingsLoadSource.Fetch
            }.onFailure { throwable ->
                screenData[screenId] = SettingsScreenDataState.Error(
                    message = throwable.message ?: "Unknown loading error"
                )
            }
            loadingJobs.remove(screenId)
        }
    }

    fun invalidateScreens(predicate: (String) -> Boolean) {
        val knownIds = buildSet {
            addAll(screenData.keys)
            addAll(stack.map { instance -> instance.id })
            addAll(preparedPreviewInstances.keys.map { key -> key.screenId })
        }.filter(predicate)

        knownIds.forEach { screenId ->
            loadingJobs[screenId]?.cancel()
            loadingJobs.remove(screenId)
            screenData.remove(screenId)
            lastLoadSourceByScreenId.remove(screenId)
            refreshVersionByScreenId[screenId] = (refreshVersionByScreenId[screenId] ?: 0) + 1
        }
    }

    fun preparePreviewInstance(parentInstanceId: Long, screenId: String): ScreenInstance {
        val key = PreviewCacheKey(parentInstanceId = parentInstanceId, screenId = screenId)
        return preparedPreviewInstances.getOrPut(key) { createInstance(screenId) }
    }

    fun navigateForward(parentInstanceId: Long, nextScreenId: String) {
        val key = PreviewCacheKey(parentInstanceId = parentInstanceId, screenId = nextScreenId)
        val prepared = preparedPreviewInstances.remove(key)
        val targetInstance = prepared ?: createInstance(nextScreenId)

        navigationDirection = SettingsNavigationDirection.Forward
        stack.add(targetInstance)
        cleanupPreparedPreviewFor(parentInstanceId)
    }

    fun navigateBack(): Boolean {
        if (stack.size <= 1) return false

        navigationDirection = SettingsNavigationDirection.Backward
        val removed = stack.removeAt(stack.lastIndex)
        cleanupPreparedPreviewFor(removed.instanceId)
        return true
    }

    private fun cleanupPreparedPreviewFor(parentInstanceId: Long) {
        val keysToRemove = preparedPreviewInstances.keys
            .filter { key -> key.parentInstanceId == parentInstanceId }
        keysToRemove.forEach { key ->
            preparedPreviewInstances.remove(key)
        }
    }

    private fun createInstance(screenId: String): ScreenInstance {
        val instance = ScreenInstance(
            id = screenId,
            instanceId = nextInstanceId
        )
        nextInstanceId += 1L
        return instance
    }
}

@Composable
fun rememberSettingsHostState(
    registry: SettingsRegistry,
    rootScreenId: String
): SettingsHostState {
    val coroutineScope = rememberCoroutineScope()
    return remember(registry, rootScreenId, coroutineScope) {
        SettingsHostState(
            registry = registry,
            coroutineScope = coroutineScope,
            rootScreenId = rootScreenId
        )
    }
}
