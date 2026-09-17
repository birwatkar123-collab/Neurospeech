package com.neurospeech.app.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E6FB7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E4F7),
    onPrimaryContainer = Color(0xFF0B2B52),
    secondary = Color(0xFF00696F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB9EFF2),
    onSecondaryContainer = Color(0xFF003F44),
    background = Color(0xFFF8F9FB),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFF8F9FB),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E5EB),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

@Composable
fun NeuroSpeechTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}