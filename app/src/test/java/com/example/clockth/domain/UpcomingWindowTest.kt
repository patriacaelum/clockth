package com.example.clockth.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpcomingWindowTest {
    @Test
    fun insideWindow() {
        val now = 1_000_000L
        assertTrue(UpcomingWindow.contains(now + 1, now))
        assertTrue(UpcomingWindow.contains(now + UpcomingWindow.DURATION_MS, now))
        assertTrue(UpcomingWindow.contains(now + 12 * 60 * 60 * 1000L, now))
    }

    @Test
    fun outsideWindow() {
        val now = 1_000_000L
        assertFalse(UpcomingWindow.contains(null, now))
        assertFalse(UpcomingWindow.contains(now, now))
        assertFalse(UpcomingWindow.contains(now - 1, now))
        assertFalse(UpcomingWindow.contains(now + UpcomingWindow.DURATION_MS + 1, now))
    }
}
