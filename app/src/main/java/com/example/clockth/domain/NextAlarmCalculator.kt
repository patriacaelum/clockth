package com.example.clockth.domain

import com.example.clockth.data.DaysOfWeek
import java.util.Calendar

object NextAlarmCalculator {
    fun nextTriggerMillis(
        nowMillis: Long,
        hour: Int,
        minute: Int,
        repeatDays: Int,
    ): Long {
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val candidate = now.clone() as Calendar
        candidate.set(Calendar.HOUR_OF_DAY, hour)
        candidate.set(Calendar.MINUTE, minute)
        candidate.set(Calendar.SECOND, 0)
        candidate.set(Calendar.MILLISECOND, 0)
        if (candidate.timeInMillis <= nowMillis) {
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }
        if (repeatDays == DaysOfWeek.NONE) {
            return candidate.timeInMillis
        }
        repeat(8) {
            val day = candidate.get(Calendar.DAY_OF_WEEK)
            if (DaysOfWeek.contains(repeatDays, day)) {
                return candidate.timeInMillis
            }
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }
        error("repeatDays=$repeatDays did not match any weekday")
    }
}
