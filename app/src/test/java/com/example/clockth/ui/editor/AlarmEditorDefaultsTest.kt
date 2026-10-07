package com.example.clockth.ui.editor

import com.example.clockth.data.Alarm
import com.example.clockth.data.DaysOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AlarmEditorDefaultsTest {
    @Test
    fun copiesLastCreatedExceptTime() {
        val last = Alarm(
            id = 3,
            hour = 5,
            minute = 15,
            label = "Gym",
            repeatDays = DaysOfWeek.WEEKDAYS,
            snoozeMinutes = 9,
            wakeCheckEnabled = false,
            wakeCheckMinutes = 12,
            vibrate = false,
            mathChallengeEnabled = false,
            soundUri = "content://tone",
            fadeInSeconds = 40,
        )
        val state = defaultNewEditorState(last, hour = 14, minute = 0)
        assertEquals(14, state.hour)
        assertEquals(0, state.minute)
        assertEquals("Gym", state.label)
        assertEquals(DaysOfWeek.WEEKDAYS, state.repeatDays)
        assertEquals(9, state.snoozeMinutes)
        assertEquals(false, state.wakeCheckEnabled)
        assertEquals(12, state.wakeCheckMinutes)
        assertEquals(false, state.vibrate)
        assertEquals(false, state.mathChallengeEnabled)
        assertEquals("content://tone", state.soundUri)
        assertEquals(40, state.fadeInSeconds)
        assertTrue(state.isNew)
        assertTrue(state.loaded)
    }

    @Test
    fun usesBuiltInDefaultsWhenNoPreviousAlarm() {
        val state = defaultNewEditorState(null, hour = 14, minute = 0)
        assertEquals(14, state.hour)
        assertEquals(0, state.minute)
        assertEquals("", state.label)
        assertEquals(DaysOfWeek.NONE, state.repeatDays)
        assertEquals(5, state.snoozeMinutes)
        assertEquals(true, state.wakeCheckEnabled)
        assertEquals(8, state.wakeCheckMinutes)
        assertEquals(true, state.vibrate)
        assertEquals(true, state.mathChallengeEnabled)
        assertEquals(null, state.soundUri)
        assertEquals(0, state.fadeInSeconds)
        assertTrue(state.isNew)
        assertTrue(state.loaded)
    }

    @Test
    fun clockTimeAfterAddsMinutes() {
        val now = millis(2026, Calendar.MARCH, 10, 14, 20)
        assertEquals(ClockTime(14, 50), clockTimeAfter(now, 30))
        assertEquals(ClockTime(15, 20), clockTimeAfter(now, 60))
        assertEquals(ClockTime(16, 20), clockTimeAfter(now, 120))
    }

    @Test
    fun clockTimeAfterWrapsMidnight() {
        val now = millis(2026, Calendar.MARCH, 10, 23, 50)
        assertEquals(ClockTime(0, 20), clockTimeAfter(now, 30))
        assertEquals(ClockTime(0, 50), clockTimeAfter(now, 60))
        assertEquals(ClockTime(1, 50), clockTimeAfter(now, 120))
    }
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
