package com.example.clockth.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.clockth.ClockthApp
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val relevant = action == Intent.ACTION_BOOT_COMPLETED ||
                action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
                action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                action == Intent.ACTION_TIME_CHANGED ||
                action == Intent.ACTION_TIMEZONE_CHANGED ||
                action == Intent.ACTION_DATE_CHANGED ||
                (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                        action == android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
        if (!relevant) return
        val pending = goAsync()
        val app = context.applicationContext as ClockthApp
        app.applicationScope.launch {
            try {
                app.container.repository.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
