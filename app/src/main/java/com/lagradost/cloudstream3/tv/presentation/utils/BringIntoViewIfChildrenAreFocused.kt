package com.lagradost.cloudstream3.tv.presentation.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.relocation.BringIntoViewModifierNode

@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.bringIntoViewIfChildrenAreFocused(
    paddingValues: PaddingValues = PaddingValues()
): Modifier = this.then(
    BringIntoViewIfChildrenAreFocusedElement(paddingValues)
)

@OptIn(ExperimentalFoundationApi::class)
private data class BringIntoViewIfChildrenAreFocusedElement(
    val paddingValues: PaddingValues
) : ModifierNodeElement<BringIntoViewIfChildrenAreFocusedNode>() {
    override fun create() = BringIntoViewIfChildrenAreFocusedNode(paddingValues)
    
    override fun update(node: BringIntoViewIfChildrenAreFocusedNode) {
        node.paddingValues = paddingValues
    }
    
    override fun InspectorInfo.inspectableProperties() {
        name = "bringIntoViewIfChildrenAreFocused"
        properties["paddingValues"] = paddingValues
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class BringIntoViewIfChildrenAreFocusedNode(
    var paddingValues: PaddingValues
) : BringIntoViewModifierNode, Modifier.Node() {
    override suspend fun bringIntoView(
        childCoordinates: androidx.compose.ui.layout.LayoutCoordinates,
        boundsProvider: () -> Rect?
    ) {
        // The container is not expected to be scrollable. Hence the child is
        // already in view with respect to the container.
    }
}
