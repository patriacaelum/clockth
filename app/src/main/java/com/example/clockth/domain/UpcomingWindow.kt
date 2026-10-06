package com.example.clockth.domain

object UpcomingWindow {
    const val DURATION_MS: Long = 24L * 60 * 60 * 1000L

    fun contains(triggerAt: Long?, nowMillis: Long): Boolean {
        if (triggerAt == null) return false
        val delta = triggerAt - nowMillis
        return delta > 0L && delta <= DURATION_MS
    }
}
