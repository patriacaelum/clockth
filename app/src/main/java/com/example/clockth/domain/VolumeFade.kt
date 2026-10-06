package com.example.clockth.domain

object VolumeFade {
    fun volumeAt(elapsedMs: Long, fadeDurationMs: Long): Float {
        if (fadeDurationMs <= 0L) return 1f
        if (elapsedMs <= 0L) return 0f
        if (elapsedMs >= fadeDurationMs) return 1f
        return elapsedMs.toFloat() / fadeDurationMs.toFloat()
    }
}
