package com.tinsic.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    secondary = NeonCyan,
    tertiary = NeonPink,
    background = Color(0xFF121212),  // Dark background
    surface = Color(0xFF1E1E1E),     // CardBackground
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun TinSicTheme(
    darkTheme: Boolean = true,  // Always dark theme for TinSic
    dynamicColor: Boolean = false,  // Disabled for consistent UI
    content: @Composable () -> Unit
) {
    // Always use dark color scheme
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}