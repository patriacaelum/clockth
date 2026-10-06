package com.example.clockth.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DaysOfWeekTest {
    @Test
    fun toggleAddsAndRemoves() {
        var mask = DaysOfWeek.NONE
        mask = DaysOfWeek.toggle(mask, Calendar.MONDAY)
        assertTrue(DaysOfWeek.contains(mask, Calendar.MONDAY))
        mask = DaysOfWeek.toggle(mask, Calendar.MONDAY)
        assertFalse(DaysOfWeek.contains(mask, Calendar.MONDAY))
    }

    @Test
    fun summaries() {
        assertEquals("Once", DaysOfWeek.summary(DaysOfWeek.NONE))
        assertEquals("Every day", DaysOfWeek.summary(DaysOfWeek.ALL))
        assertEquals("Weekdays", DaysOfWeek.summary(DaysOfWeek.WEEKDAYS))
        assertEquals("Weekends", DaysOfWeek.summary(DaysOfWeek.WEEKENDS))
    }
}
