package com.doman.wordsteps.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = darkColorScheme(
    primary          = Color(0xFFFFC044),   // Amber
    onPrimary        = Color(0xFF0F1B2D),
    secondary        = Color(0xFF00C9A7),   // Teal
    onSecondary      = Color(0xFF0F1B2D),
    background       = Color(0xFF0F1B2D),   // Navy
    onBackground     = Color(0xFFF0F4FF),
    surface          = Color(0xFF1A2E4A),
    onSurface        = Color(0xFFF0F4FF),
    error            = Color(0xFFFF6B8A),
    onError          = Color(0xFF0F1B2D)
)

@Composable
fun WordStepsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content     = content
    )
}