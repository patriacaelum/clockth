package com.example.clockth.domain

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import java.util.Calendar
import java.util.Date

object AlarmTimeFormatter {
    fun formatClock(context: Context, hour: Int, minute: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return DateFormat.getTimeFormat(context).format(cal.time)
    }

    fun formatInstant(context: Context, epochMillis: Long): String {
        return DateFormat.getTimeFormat(context).format(Date(epochMillis))
    }

    fun formatDateTime(context: Context, epochMillis: Long): String {
        return DateUtils.formatDateTime(
            context,
            epochMillis,
            DateUtils.FORMAT_SHOW_DATE or
                    DateUtils.FORMAT_SHOW_TIME or
                    DateUtils.FORMAT_SHOW_WEEKDAY or
                    DateUtils.FORMAT_ABBREV_WEEKDAY or
                    DateUtils.FORMAT_ABBREV_MONTH,
        )
    }

    fun formatLiveClock(context: Context, epochMillis: Long): String {
        return DateFormat.getTimeFormat(context).format(Date(epochMillis))
    }

    fun formatLiveDate(context: Context, epochMillis: Long): String {
        return DateFormat.getMediumDateFormat(context).format(Date(epochMillis))
    }
}
