package com.example.clockth.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.clockth.data.FireKind

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmIntents.ACTION_FIRE) return
        val alarmId = intent.getLongExtra(AlarmIntents.EXTRA_ALARM_ID, -1L)
        if (alarmId < 0) return
        val kind = runCatching {
            FireKind.valueOf(intent.getStringExtra(AlarmIntents.EXTRA_KIND) ?: FireKind.ALARM.name)
        }.getOrDefault(FireKind.ALARM)
        AlarmRingingService.start(context, alarmId, kind)
    }
}
