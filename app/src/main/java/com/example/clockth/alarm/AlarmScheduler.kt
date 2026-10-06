package com.example.clockth.alarm

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import com.example.clockth.data.AlarmSchedulerPort
import com.example.clockth.data.FireKind

class AlarmScheduler(
    context: Context,
) : AlarmSchedulerPort {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    override fun schedule(alarmId: Long, triggerAtEpochMillis: Long, kind: FireKind) {
        val show = AlarmIntents.showApp(appContext)
        val fire = AlarmIntents.fireBroadcast(appContext, alarmId, kind)
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtEpochMillis, show),
            fire,
        )
    }

    override fun cancel(alarmId: Long, kind: FireKind) {
        alarmManager.cancel(AlarmIntents.fireBroadcast(appContext, alarmId, kind))
    }

    override fun cancelAll(alarmId: Long) {
        FireKind.entries.forEach { kind ->
            if (kind != FireKind.NONE) {
                cancel(alarmId, kind)
            }
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}
