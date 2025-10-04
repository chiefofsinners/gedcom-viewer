package com.lewisdeveloping.gedcomviewer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = DeepPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFE0FF),
    onPrimaryContainer = Slate,
    secondary = Slate,
    onSecondary = Color.White,
    background = Color(0xFFF6F7FB),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Lavender,
    onSurfaceVariant = Slate
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC7C5FF),
    onPrimary = Slate,
    primaryContainer = Slate,
    onPrimaryContainer = Color(0xFFC7C5FF),
    secondary = Color(0xFFB9C6FF),
    onSecondary = Slate,
    background = Color(0xFF12121A),
    surface = Color(0xFF1D1D27),
    onSurface = Color(0xFFE7E7F0),
    surfaceVariant = Color(0xFF343545),
    onSurfaceVariant = Color(0xFFE7E7F0)
)

@Composable
fun GedcomViewerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
