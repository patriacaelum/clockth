package com.example.clockth.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeFadeTest {
    @Test
    fun noFadeIsAlwaysFull() {
        assertEquals(1f, VolumeFade.volumeAt(0, 0), 0f)
        assertEquals(1f, VolumeFade.volumeAt(5_000, 0), 0f)
    }

    @Test
    fun startsSilent() {
        assertEquals(0f, VolumeFade.volumeAt(0, 10_000), 0f)
        assertEquals(0f, VolumeFade.volumeAt(-1, 10_000), 0f)
    }

    @Test
    fun rampsLinearly() {
        assertEquals(0.25f, VolumeFade.volumeAt(2_500, 10_000), 0.0001f)
        assertEquals(0.5f, VolumeFade.volumeAt(5_000, 10_000), 0.0001f)
        assertEquals(0.75f, VolumeFade.volumeAt(7_500, 10_000), 0.0001f)
    }

    @Test
    fun reachesFullAndStaysThere() {
        assertEquals(1f, VolumeFade.volumeAt(10_000, 10_000), 0f)
        assertEquals(1f, VolumeFade.volumeAt(30_000, 10_000), 0f)
    }
}
