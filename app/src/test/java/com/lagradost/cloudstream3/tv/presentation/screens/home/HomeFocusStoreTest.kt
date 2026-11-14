package com.lagradost.cloudstream3.tv.presentation.screens.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class HomeFocusStoreTest {
    @Test
    fun `reset forgets the last focused item so Down from the top bar starts at the top`() {
        HomeFocusStore.onTargetFocused("home:feed:x:show_more")
        HomeFocusStore.resetLastFocused()

        assertNull(HomeFocusStore.lastTargetId)
    }

    @Test
    fun `a feed row remembers its focused poster across screen recreation`() {
        val group = HomeFocusStore.rowGroup("source|feed")
        group.onItemFocused(3)

        assertSame(group, HomeFocusStore.rowGroup("source|feed"))
        assertEquals(3, HomeFocusStore.rowGroup("source|feed").lastIndex)
        // Another feed (or the same feed of another source) has its own memory.
        assertEquals(0, HomeFocusStore.rowGroup("source|other").lastIndex)
        assertEquals(0, HomeFocusStore.rowGroup("other|feed").lastIndex)
    }

    @Test
    fun `reset of the last target keeps the remembered posters of the rows`() {
        HomeFocusStore.rowGroup("source|kept").onItemFocused(2)
        HomeFocusStore.resetLastFocused()

        assertEquals(2, HomeFocusStore.rowGroup("source|kept").lastIndex)
    }

    @Test
    fun `feed id is read from poster and show all targets`() {
        assertEquals("trending", HomeFocusStore.feedIdFromTarget("home:feed:trending:item:api|1|url"))
        assertEquals("trending", HomeFocusStore.feedIdFromTarget(HomeFocusStore.feedShowMore("trending")))
        assertNull(HomeFocusStore.feedIdFromTarget(HomeFocusStore.ContinueWatchingResume))
    }
}
