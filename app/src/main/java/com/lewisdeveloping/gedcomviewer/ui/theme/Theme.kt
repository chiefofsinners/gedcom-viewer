package com.lewisdeveloping.gedcomviewer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = SlateLight,
    onPrimary = Color.White,
    primaryContainer = Slate,
    onPrimaryContainer = Color.White,
    secondary = Slate,
    onSecondary = Color.White,
    secondaryContainer = Silver,
    onSecondaryContainer = Slate,
    tertiary = Silver,
    onTertiary = Slate,
    background = SilverBright,
    surface = Color.White,
    onSurface = Slate,
    surfaceVariant = Silver,
    onSurfaceVariant = SlateLight,
    outline = SlateLight
)

private val DarkColors = darkColorScheme(
    primary = Silver,
    onPrimary = SlateDark,
    primaryContainer = SlateDark,
    onPrimaryContainer = Silver,
    secondary = Silver,
    onSecondary = SlateDark,
    secondaryContainer = Slate,
    onSecondaryContainer = Silver,
    tertiary = Slate,
    onTertiary = Silver,
    background = SlateDark,
    surface = Color(0xFF232830),
    onSurface = Silver,
    surfaceVariant = Color(0xFF2F3540),
    onSurfaceVariant = Silver,
    outline = Silver
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
