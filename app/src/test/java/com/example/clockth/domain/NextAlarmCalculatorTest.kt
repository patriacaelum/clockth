package com.example.clockth.domain

import com.example.clockth.data.DaysOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class NextAlarmCalculatorTest {
    @Test
    fun oneTimeLaterToday() {
        val now = millis(2026, Calendar.SEPTEMBER, 30, 6, 0)
        val next = NextAlarmCalculator.nextTriggerMillis(now, 7, 0, DaysOfWeek.NONE)
        assertEquals(millis(2026, Calendar.SEPTEMBER, 30, 7, 0), next)
    }

    @Test
    fun oneTimeTomorrowWhenTimePassed() {
        val now = millis(2026, Calendar.SEPTEMBER, 30, 10, 0)
        val next = NextAlarmCalculator.nextTriggerMillis(now, 7, 0, DaysOfWeek.NONE)
        assertEquals(millis(2026, Calendar.OCTOBER, 1, 7, 0), next)
    }

    @Test
    fun weekdaysSkipsToTomorrow() {
        val now = millis(2026, Calendar.SEPTEMBER, 30, 10, 0) // Wednesday
        val next = NextAlarmCalculator.nextTriggerMillis(now, 7, 0, DaysOfWeek.WEEKDAYS)
        assertEquals(millis(2026, Calendar.OCTOBER, 1, 7, 0), next)
    }

    @Test
    fun weekdaysSkipsWeekend() {
        val now = millis(2026, Calendar.OCTOBER, 2, 10, 0) // Friday
        val next = NextAlarmCalculator.nextTriggerMillis(now, 7, 0, DaysOfWeek.WEEKDAYS)
        assertEquals(millis(2026, Calendar.OCTOBER, 5, 7, 0), next)
    }

    @Test
    fun weekendsFromSaturdayGoesSunday() {
        val now = millis(2026, Calendar.OCTOBER, 3, 10, 0) // Saturday
        val next = NextAlarmCalculator.nextTriggerMillis(now, 8, 30, DaysOfWeek.WEEKENDS)
        assertEquals(millis(2026, Calendar.OCTOBER, 4, 8, 30), next)
    }

    @Test
    fun sameMinuteDoesNotFireImmediately() {
        val now = millis(2026, Calendar.SEPTEMBER, 30, 7, 0)
        val next = NextAlarmCalculator.nextTriggerMillis(now, 7, 0, DaysOfWeek.NONE)
        assertEquals(millis(2026, Calendar.OCTOBER, 1, 7, 0), next)
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
