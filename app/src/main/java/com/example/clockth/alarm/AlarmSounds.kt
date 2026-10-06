package com.example.clockth.alarm

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import com.example.clockth.R

object AlarmSounds {
    fun defaultUri(): Uri? {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    fun resolve(context: Context, soundUri: String?): Uri {
        val fallback = defaultUri() ?: Uri.EMPTY
        if (soundUri.isNullOrBlank()) return fallback
        val uri = Uri.parse(soundUri)
        return try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { }
            uri
        } catch (_: Exception) {
            fallback
        }
    }

    fun title(context: Context, soundUri: String?): String {
        if (soundUri.isNullOrBlank()) {
            return context.getString(R.string.default_alarm_sound)
        }
        val uri = Uri.parse(soundUri)
        if (uri == RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)) {
            return context.getString(R.string.default_alarm_sound)
        }
        return runCatching {
            RingtoneManager.getRingtone(context, uri)?.getTitle(context)
        }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.custom_alarm_sound)
    }
}
