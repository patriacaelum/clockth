package com.example.clockth.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Amber = Color(0xFFE8B86D)
val Cream = Color(0xFFF4EFE4)
val Navy = Color(0xFF070B16)
val NavySurface = Color(0xFF12182A)
val NavyCard = Color(0xFF1A2238)
val Muted = Color(0xFF8B93A7)
val Danger = Color(0xFFE07070)

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Navy,
    secondary = Color(0xFF7EC8C4),
    onSecondary = Navy,
    background = Navy,
    onBackground = Cream,
    surface = NavySurface,
    onSurface = Cream,
    surfaceVariant = NavyCard,
    onSurfaceVariant = Muted,
    error = Danger,
    onError = Cream,
    outline = Color(0xFF3A4563),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A5A12),
    onPrimary = Color.White,
    secondary = Color(0xFF1F6B68),
    onSecondary = Color.White,
    background = Color(0xFFF6F1E8),
    onBackground = Navy,
    surface = Color(0xFFFFFBF4),
    onSurface = Navy,
    surfaceVariant = Color(0xFFE8DFD0),
    onSurfaceVariant = Color(0xFF5B5348),
    error = Color(0xFFB42318),
    onError = Color.White,
    outline = Color(0xFFD0C4B0),
)

@Composable
fun ClockthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    forceDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme || forceDark) DarkColors else LightColors,
        content = content,
    )
}
