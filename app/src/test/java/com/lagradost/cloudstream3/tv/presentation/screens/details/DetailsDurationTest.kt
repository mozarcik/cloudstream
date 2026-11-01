package com.lagradost.cloudstream3.tv.presentation.screens.details

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetailsDurationTest {
    private fun format(minutes: Int) = formatDetailsDuration(
        minutes,
        minutesOnly = { "$it min" },
        hoursOnly = { "${it}h" },
        hoursAndMinutes = { h, m -> "${h}h ${m}min" },
    )

    @Test
    fun `165 minutes is shown as hours and minutes`() = assertEquals("2h 45min", format(165))

    @Test
    fun `whole hours have no minutes part`() = assertEquals("2h", format(120))

    @Test
    fun `under an hour stays in minutes`() = assertEquals("45 min", format(45))

    @Test
    fun `parses the mapper text`() {
        assertEquals(165, parseDetailsDurationMinutes("165min"))
        assertNull(parseDetailsDurationMinutes(""))
        assertNull(parseDetailsDurationMinutes("0min"))
    }
}
