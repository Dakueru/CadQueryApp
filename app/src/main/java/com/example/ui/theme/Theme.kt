package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CadDarkColorScheme = darkColorScheme(
    primary = CadCyanPrimary,
    onPrimary = Color.Black,
    primaryContainer = CadCyanSecondary,
    onPrimaryContainer = Color.White,
    secondary = CadGreen,
    onSecondary = Color.Black,
    tertiary = CadAmber,
    background = CadDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = CadSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = CadSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = CadRed,
    onError = Color.White
)

@Composable
fun CadQueryTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CadDarkColorScheme,
        typography = Typography,
        content = content
    )
}
