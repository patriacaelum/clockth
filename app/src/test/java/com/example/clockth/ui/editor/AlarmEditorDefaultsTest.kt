package com.example.clockth.ui.editor

import com.example.clockth.data.Alarm
import com.example.clockth.data.DaysOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
