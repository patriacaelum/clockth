package com.example.clockth.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmRepositoryTest {
    @Test
    fun saveSchedulesNextOccurrence() = runTest {
        val store = InMemoryAlarmStore()
        val scheduler = RecordingScheduler()
        var now = 1_000_000L
        val repo = AlarmRepository(store, scheduler) { now }
        val saved = repo.save(
            Alarm(hour = 7, minute = 0, repeatDays = DaysOfWeek.WEEKDAYS),
        )
        assertEquals(FireKind.ALARM, saved.pendingKind)
        assertEquals(1, scheduler.scheduled.size)
        assertEquals(FireKind.ALARM, scheduler.scheduled.single().kind)
    }

    @Test
    fun saveKeepsSoundAndFadeIn() = runTest {
        val store = InMemoryAlarmStore()
        val scheduler = RecordingScheduler()
        val repo = AlarmRepository(store, scheduler) { 5_000L }
        val saved = repo.save(
            Alarm(
                hour = 6,
                minute = 30,
                soundUri = "content://media/internal/audio/media/12",
                fadeInSeconds = 45,
            ),
        )
        assertEquals("content://media/internal/audio/media/12", saved.soundUri)
        assertEquals(45, saved.fadeInSeconds)
    }

    @Test
    fun saveKeepsMathChallengeDisabled() = runTest {
        val store = InMemoryAlarmStore()
        val scheduler = RecordingScheduler()
        val repo = AlarmRepository(store, scheduler) { 5_000L }
        val saved = repo.save(
            Alarm(
                hour = 6,
                minute = 30,
                mathChallengeEnabled = false,
            ),
        )
        assertEquals(false, saved.mathChallengeEnabled)
    }

    @Test
    fun snoozeSchedulesFromNow() = runTest {
        val store = InMemoryAlarmStore()
        val scheduler = RecordingScheduler()
        var now = 5_000L
        val repo = AlarmRepository(store, scheduler) { now }
        val saved = repo.save(Alarm(hour = 7, minute = 0, snoozeMinutes = 5))
        repo.snooze(saved.id)
        val snoozed = repo.get(saved.id)!!
        assertEquals(FireKind.SNOOZE, snoozed.pendingKind)
        assertEquals(now + 5 * 60_000L, snoozed.pendingAtEpochMillis)
    }

    @Test
    fun dismissOneTimeWithWakeCheckKeepsAlarm() = runTest {
        val store = InMemoryAlarmStore()
        val scheduler = RecordingScheduler()
        var now = 5_000L
        val repo = AlarmRepository(store, scheduler) { now }
        val saved = repo.save(
            Alarm(
                hour = 7,
                minute = 0,
                wakeCheckEnabled = true,
                wakeCheckMinutes = 8,
            ),
        )
        repo.dismiss(saved.id, FireKind.ALARM)
        val kept = repo.get(saved.id)!!
        assertEquals(FireKind.WAKE_CHECK, kept.pendingKind)
        assertEquals(now + 8 * 60_000L, kept.pendingAtEpochMillis)
    }

    @Test
    fun dismissWakeCheckDeletesOneTime() = runTest {
        val store = InMemoryAlarmStore()
        val scheduler = RecordingScheduler()
        val repo = AlarmRepository(store, scheduler) { 5_000L }
        val saved = repo.save(
            Alarm(hour = 7, minute = 0, wakeCheckEnabled = true),
        )
        repo.dismiss(saved.id, FireKind.WAKE_CHECK)
        assertNull(repo.get(saved.id))
        assertTrue(scheduler.cancelledAll.contains(saved.id))
    }

    @Test
    fun dismissRecurringWithoutWakeCheckSchedulesNext() = runTest {
        val store = InMemoryAlarmStore()
        val scheduler = RecordingScheduler()
        val repo = AlarmRepository(store, scheduler) { 5_000L }
        val saved = repo.save(
            Alarm(
                hour = 7,
                minute = 0,
                repeatDays = DaysOfWeek.ALL,
                wakeCheckEnabled = false,
            ),
        )
        scheduler.scheduled.clear()
        repo.dismiss(saved.id, FireKind.ALARM)
        val updated = repo.get(saved.id)!!
        assertEquals(FireKind.ALARM, updated.pendingKind)
        assertEquals(1, scheduler.scheduled.size)
    }

    @Test
    fun cancelNextAlarmSkipsRecurringOccurrence() = runTest {
        val store = InMemoryAlarmStore()
        val scheduler = RecordingScheduler()
        val now = millis(2026, java.util.Calendar.SEPTEMBER, 30, 10, 0)
        val repo = AlarmRepository(store, scheduler) { now }
        val saved = repo.save(
            Alarm(hour = 7, minute = 0, repeatDays = DaysOfWeek.WEEKDAYS),
        )
        assertEquals(millis(2026, java.util.Calendar.OCTOBER, 1, 7, 0), saved.pendingAtEpochMillis)
        repo.cancelNextAlarm(saved.id)
        val skipped = repo.get(saved.id)!!
        assertEquals(FireKind.ALARM, skipped.pendingKind)
        assertEquals(millis(2026, java.util.Calendar.OCTOBER, 2, 7, 0), skipped.pendingAtEpochMillis)
    }

    @Test
    fun cancelNextAlarmIgnoresAlarmsBeyond24Hours() = runTest {
        val store = InMemoryAlarmStore()
        val scheduler = RecordingScheduler()
        val now = millis(2026, java.util.Calendar.OCTOBER, 2, 10, 0) // Friday
        val repo = AlarmRepository(store, scheduler) { now }
        val saved = repo.save(
            Alarm(hour = 7, minute = 0, repeatDays = DaysOfWeek.WEEKDAYS),
        )
        val original = saved.pendingAtEpochMillis
        assertEquals(millis(2026, java.util.Calendar.OCTOBER, 5, 7, 0), original)
        repo.cancelNextAlarm(saved.id)
        val unchanged = repo.get(saved.id)!!
        assertEquals(original, unchanged.pendingAtEpochMillis)
        assertEquals(FireKind.ALARM, unchanged.pendingKind)
    }

    @Test
    fun cancelNextAlarmDisablesOneTime() = runTest {
        val store = InMemoryAlarmStore()
        val scheduler = RecordingScheduler()
        val repo = AlarmRepository(store, scheduler) { 5_000L }
        val saved = repo.save(Alarm(hour = 7, minute = 0))
        repo.cancelNextAlarm(saved.id)
        val updated = repo.get(saved.id)!!
        assertEquals(false, updated.enabled)
        assertEquals(FireKind.NONE, updated.pendingKind)
    }

    @Test
    fun cancelWakeCheckDeletesOneTime() = runTest {
        val store = InMemoryAlarmStore()
        val scheduler = RecordingScheduler()
        val repo = AlarmRepository(store, scheduler) { 5_000L }
        val saved = repo.save(
            Alarm(hour = 7, minute = 0, wakeCheckEnabled = true, wakeCheckMinutes = 8),
        )
        repo.dismiss(saved.id, FireKind.ALARM)
        repo.cancelWakeCheck(saved.id)
        assertNull(repo.get(saved.id))
    }

    @Test
    fun cancelWakeCheckKeepsRecurringAlarm() = runTest {
        val store = InMemoryAlarmStore()
        val scheduler = RecordingScheduler()
        val now = millis(2026, java.util.Calendar.SEPTEMBER, 30, 10, 0)
        val repo = AlarmRepository(store, scheduler) { now }
        val saved = repo.save(
            Alarm(
                hour = 7,
                minute = 0,
                repeatDays = DaysOfWeek.WEEKDAYS,
                wakeCheckEnabled = true,
                wakeCheckMinutes = 8,
            ),
        )
        repo.dismiss(saved.id, FireKind.ALARM)
        assertEquals(FireKind.WAKE_CHECK, repo.get(saved.id)!!.pendingKind)
        repo.cancelWakeCheck(saved.id)
        val updated = repo.get(saved.id)!!
        assertEquals(FireKind.ALARM, updated.pendingKind)
        assertEquals(millis(2026, java.util.Calendar.OCTOBER, 1, 7, 0), updated.pendingAtEpochMillis)
    }

    @Test
    fun cancelNextAlarmDuringWakeCheckSkipsFollowingOccurrence() = runTest {
        val store = InMemoryAlarmStore()
        val scheduler = RecordingScheduler()
        val now = millis(2026, java.util.Calendar.SEPTEMBER, 30, 10, 0)
        val repo = AlarmRepository(store, scheduler) { now }
        val saved = repo.save(
            Alarm(
                hour = 7,
                minute = 0,
                repeatDays = DaysOfWeek.WEEKDAYS,
                wakeCheckEnabled = true,
            ),
        )
        repo.dismiss(saved.id, FireKind.ALARM)
        repo.cancelNextAlarm(saved.id)
        val updated = repo.get(saved.id)!!
        assertEquals(FireKind.WAKE_CHECK, updated.pendingKind)
        assertEquals(millis(2026, java.util.Calendar.OCTOBER, 2, 7, 0), updated.nextOccurrenceEpochMillis)
    }

    @Test
    fun cancelSnoozeRestoresRecurringAlarm() = runTest {
        val store = InMemoryAlarmStore()
        val scheduler = RecordingScheduler()
        val now = millis(2026, java.util.Calendar.SEPTEMBER, 30, 10, 0)
        val repo = AlarmRepository(store, scheduler) { now }
        val saved = repo.save(
            Alarm(hour = 7, minute = 0, repeatDays = DaysOfWeek.WEEKDAYS, snoozeMinutes = 5),
        )
        repo.snooze(saved.id)
        repo.cancelNextAlarm(saved.id)
        val updated = repo.get(saved.id)!!
        assertEquals(FireKind.ALARM, updated.pendingKind)
        assertEquals(millis(2026, java.util.Calendar.OCTOBER, 1, 7, 0), updated.pendingAtEpochMillis)
    }

    @Test
    fun latestCreatedReturnsHighestId() = runTest {
        val store = InMemoryAlarmStore()
        val scheduler = RecordingScheduler()
        val repo = AlarmRepository(store, scheduler) { 5_000L }
        assertNull(repo.latestCreated())
        val first = repo.save(Alarm(hour = 7, minute = 0, label = "first"))
        repo.save(
            Alarm(
                hour = 8,
                minute = 0,
                label = "second",
                snoozeMinutes = 10,
            ),
        )
        val latest = repo.latestCreated()
        assertEquals("second", latest?.label)
        assertEquals(10, latest?.snoozeMinutes)
        repo.delete(latest!!.id)
        assertEquals(first.id, repo.latestCreated()?.id)
    }

    @Test
    fun disableCancelsSchedules() = runTest {
        val store = InMemoryAlarmStore()
        val scheduler = RecordingScheduler()
        val repo = AlarmRepository(store, scheduler) { 5_000L }
        val saved = repo.save(Alarm(hour = 6, minute = 30))
        repo.setEnabled(saved.id, false)
        val updated = repo.get(saved.id)!!
        assertEquals(false, updated.enabled)
        assertEquals(FireKind.NONE, updated.pendingKind)
        assertTrue(scheduler.cancelledAll.contains(saved.id))
    }
}

private class InMemoryAlarmStore : AlarmStorePort {
    private var nextId = 1L
    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())
    override val alarms: StateFlow<List<Alarm>> = _alarms.asStateFlow()

    override suspend fun getAll(): List<Alarm> = _alarms.value

    override suspend fun get(id: Long): Alarm? = _alarms.value.firstOrNull { it.id == id }

    override suspend fun upsert(alarm: Alarm): Alarm {
        val saved = if (alarm.id == 0L) alarm.copy(id = nextId++) else alarm
        val next = _alarms.value.toMutableList()
        val index = next.indexOfFirst { it.id == saved.id }
        if (index >= 0) next[index] = saved else next.add(saved)
        _alarms.value = next
        return saved
    }

    override suspend fun delete(id: Long) {
        _alarms.value = _alarms.value.filterNot { it.id == id }
    }
}

private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
    return java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.YEAR, year)
        set(java.util.Calendar.MONTH, month)
        set(java.util.Calendar.DAY_OF_MONTH, day)
        set(java.util.Calendar.HOUR_OF_DAY, hour)
        set(java.util.Calendar.MINUTE, minute)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private data class Scheduled(val id: Long, val at: Long, val kind: FireKind)

private class RecordingScheduler : AlarmSchedulerPort {
    val scheduled = mutableListOf<Scheduled>()
    val cancelledAll = mutableListOf<Long>()

    override fun schedule(alarmId: Long, triggerAtEpochMillis: Long, kind: FireKind) {
        scheduled.add(Scheduled(alarmId, triggerAtEpochMillis, kind))
    }

    override fun cancel(alarmId: Long, kind: FireKind) = Unit

    override fun cancelAll(alarmId: Long) {
        cancelledAll.add(alarmId)
    }
}
