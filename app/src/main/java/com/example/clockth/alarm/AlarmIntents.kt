package com.example.clockth.alarm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.clockth.MainActivity
import com.example.clockth.data.FireKind
import com.example.clockth.ui.ring.AlarmRingActivity

object AlarmIntents {
    const val ACTION_FIRE = "com.example.clockth.action.FIRE"
    const val EXTRA_ALARM_ID = "alarm_id"
    const val EXTRA_KIND = "fire_kind"

    const val FLAG_IMMUTABLE_UPDATE =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun fireBroadcast(context: Context, alarmId: Long, kind: FireKind): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_KIND, kind.name)
            data = android.net.Uri.parse("clockth://alarm/$alarmId/${kind.name}")
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(alarmId, kind),
            intent,
            FLAG_IMMUTABLE_UPDATE,
        )
    }

    fun showApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, 0, intent, FLAG_IMMUTABLE_UPDATE)
    }

    fun ringActivity(context: Context, alarmId: Long, kind: FireKind): PendingIntent {
        val intent = AlarmRingActivity.intent(context, alarmId, kind).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
        }
        return PendingIntent.getActivity(
            context,
            requestCode(alarmId, kind) + 10_000,
            intent,
            FLAG_IMMUTABLE_UPDATE,
        )
    }

    fun requestCode(alarmId: Long, kind: FireKind): Int {
        return (alarmId.toInt() and 0x00FFFFFF) * 10 + kind.ordinal
    }
}
