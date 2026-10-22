package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ArchDarkColorScheme = darkColorScheme(
    primary = ArchCyan,
    onPrimary = Color.Black,
    primaryContainer = ArchSurfaceHighlight,
    onPrimaryContainer = ArchCyanBright,
    secondary = TermGreen,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1E3A2F),
    onSecondaryContainer = TermGreen,
    tertiary = TermYellow,
    onTertiary = Color.Black,
    background = ArchDarkNavy,
    onBackground = TermBrightWhite,
    surface = ArchTerminalBg,
    onSurface = TermBrightWhite,
    surfaceVariant = ArchSurface,
    onSurfaceVariant = TermWhite,
    outline = ArchBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ArchDarkColorScheme,
        typography = Typography,
        content = content
    )
}
