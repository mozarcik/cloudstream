package com.lagradost.cloudstream3.tv.presentation.screens.home

import org.junit.Assert.assertNull
import org.junit.Test

class HomeFocusStoreTest {
    @Test
    fun `reset forgets the last focused item so Down from the top bar starts at the top`() {
        HomeFocusStore.onTargetFocused("home:feed:x:show_more")
        HomeFocusStore.resetLastFocused()

        assertNull(HomeFocusStore.lastTargetId)
        assertNull(HomeFocusStore.pendingRestoreTargetId)
    }
}
