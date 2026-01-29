package com.lagradost.cloudstream3.tv.presentation.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Universal node in the settings hierarchy.
 * Supports unlimited depth - each node can have children OR content (leaf widget).
 * Navigation works the same at every level.
 */
data class SettingsNode(
    val id: String,
    val title: String,
    val description: String? = null,
    val icon: ImageVector? = null,
    val iconUrl: String? = null,
    val fallbackIconRes: Int? = null,
    val children: List<SettingsNode> = emptyList(),
    val content: (@Composable (onBack: () -> Unit) -> Unit)? = null
) {
    val isLeaf: Boolean get() = content != null
    val hasChildren: Boolean get() = children.isNotEmpty()
}
