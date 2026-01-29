package com.lagradost.cloudstream3.tv.presentation.screens.settings

/**
 * Manages navigation state through the settings hierarchy.
 * Tracks the current path using IDs instead of object references,
 * so it survives tree rebuilds when data changes.
 */
data class NavigationState(
    val pathIds: List<String> = emptyList(),
    val currentLevel: Int = 0
) {
    val isAtRoot: Boolean get() = pathIds.isEmpty()

    fun navigateTo(node: SettingsNode): NavigationState {
        return copy(
            pathIds = pathIds + node.id,
            currentLevel = currentLevel + 1
        )
    }

    fun navigateBack(): NavigationState {
        if (pathIds.isEmpty()) return this
        return copy(
            pathIds = pathIds.dropLast(1),
            currentLevel = (currentLevel - 1).coerceAtLeast(0)
        )
    }
    
    /**
     * Resolve the current node from the tree using the path of IDs
     */
    fun getCurrentNode(rootNode: SettingsNode): SettingsNode? {
        if (pathIds.isEmpty()) return null
        
        var current: SettingsNode? = rootNode
        for ((index, id) in pathIds.withIndex()) {
            val found = current?.children?.find { it.id == id }
            if (found == null) {
                return null
            }
            current = found
        }
        return current
    }
}
