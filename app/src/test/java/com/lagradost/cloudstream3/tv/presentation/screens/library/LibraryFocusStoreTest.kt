package com.lagradost.cloudstream3.tv.presentation.screens.library

import org.junit.Assert.assertNull
import org.junit.Test

class LibraryFocusStoreTest {
    @Test
    fun `reset forgets the last focused item so a return starts at the top`() {
        LibraryFocusStore.onTargetFocused("library:section:a:show_more")
        LibraryFocusStore.resetLastFocused()
        LibraryFocusStore.scheduleRestoreToLastFocused()

        assertNull(LibraryFocusStore.pendingRestoreTargetId)
    }
}
