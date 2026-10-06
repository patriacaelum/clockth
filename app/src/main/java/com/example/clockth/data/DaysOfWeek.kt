package com.example.clockth.data

import java.util.Calendar
import java.util.Locale

object DaysOfWeek {
    const val NONE = 0
    const val ALL = 0b1111111
    const val WEEKDAYS =
        (1 shl (Calendar.MONDAY - 1)) or
            (1 shl (Calendar.TUESDAY - 1)) or
            (1 shl (Calendar.WEDNESDAY - 1)) or
            (1 shl (Calendar.THURSDAY - 1)) or
            (1 shl (Calendar.FRIDAY - 1))
    const val WEEKENDS =
        (1 shl (Calendar.SATURDAY - 1)) or
            (1 shl (Calendar.SUNDAY - 1))

    val mondayFirstCalendarDays: IntArray = intArrayOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY,
    )

    fun bitFor(calendarDay: Int): Int = 1 shl (calendarDay - 1)

    fun contains(mask: Int, calendarDay: Int): Boolean =
        mask and bitFor(calendarDay) != 0

    fun toggle(mask: Int, calendarDay: Int): Int = mask xor bitFor(calendarDay)

    fun oneLetterLabel(calendarDay: Int, locale: Locale = Locale.getDefault()): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, calendarDay)
        val name = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, locale) ?: "?"
        return name.take(1).uppercase(locale)
    }

    fun summary(mask: Int): String {
        if (mask == NONE) return "Once"
        if (mask == ALL) return "Every day"
        if (mask == WEEKDAYS) return "Weekdays"
        if (mask == WEEKENDS) return "Weekends"
        val names = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        return names.filterIndexed { index, _ -> mask and (1 shl index) != 0 }.joinToString(" ")
    }
}
