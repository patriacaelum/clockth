package com.example.clockth.data

import com.example.clockth.domain.UpcomingWindow

data class Alarm(
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true,
    val repeatDays: Int = DaysOfWeek.NONE,
    val snoozeMinutes: Int = 5,
    val wakeCheckEnabled: Boolean = true,
    val wakeCheckMinutes: Int = 8,
    val vibrate: Boolean = true,
    val soundUri: String? = null,
    val fadeInSeconds: Int = 0,
    val pendingKind: FireKind = FireKind.NONE,
    val pendingAtEpochMillis: Long? = null,
    val nextOccurrenceEpochMillis: Long? = null,
) {
    val isOneTime: Boolean get() = repeatDays == DaysOfWeek.NONE

    val upcomingAlarmMillis: Long?
        get() = when (pendingKind) {
            FireKind.ALARM, FireKind.SNOOZE -> pendingAtEpochMillis
            else -> nextOccurrenceEpochMillis
        }

    val upcomingWakeCheckMillis: Long?
        get() = pendingAtEpochMillis.takeIf { pendingKind == FireKind.WAKE_CHECK }

    fun canCancelNextAlarm(nowMillis: Long): Boolean {
        return enabled && UpcomingWindow.contains(upcomingAlarmMillis, nowMillis)
    }
}
