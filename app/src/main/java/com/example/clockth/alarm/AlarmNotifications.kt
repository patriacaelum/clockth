package com.example.clockth.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.clockth.R
import com.example.clockth.data.Alarm
import com.example.clockth.data.FireKind
import com.example.clockth.domain.AlarmTimeFormatter

object AlarmNotifications {
    const val CHANNEL_ID = "clockth_alarms"
    const val RINGING_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setBypassDnd(true)
            setSound(null, null)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun ringing(context: Context, alarm: Alarm, kind: FireKind): Notification {
        val title = when (kind) {
            FireKind.WAKE_CHECK -> context.getString(R.string.wake_check_title)
            FireKind.SNOOZE -> context.getString(R.string.snooze_ring_title)
            else -> alarm.label.ifBlank { context.getString(R.string.default_alarm_label) }
        }
        val time = AlarmTimeFormatter.formatClock(context, alarm.hour, alarm.minute)
        val text = when (kind) {
            FireKind.WAKE_CHECK -> context.getString(R.string.wake_check_notification, time)
            else -> context.getString(R.string.alarm_ringing_notification, time)
        }
        val fullScreen = AlarmIntents.ringActivity(context, alarm.id, kind)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(fullScreen)
            .setFullScreenIntent(fullScreen, true)
            .setSilent(true)
            .build()
    }
}
