package com.lewisdeveloping.gedcomviewer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalExtendedColors = staticCompositionLocalOf { DefaultExtendedColors }

object AppTheme {
    val colors: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
}

@Composable
fun GedcomViewerTheme(
    theme: AppThemeOption,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val extendedColors = remember(theme, darkTheme) { theme.extendedColors(darkTheme) }
    val materialColors = remember(extendedColors, darkTheme) {
        buildColorScheme(extendedColors, darkTheme)
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = Typography,
            content = content
        )
    }
}

private fun buildColorScheme(colors: ExtendedColors, darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.infoForeground,
            primaryContainer = colors.surfaceEmphasis,
            onPrimaryContainer = colors.background,
            secondary = colors.surface,
            onSecondary = colors.infoForeground,
            secondaryContainer = colors.secondaryBackground,
            onSecondaryContainer = colors.infoForeground,
            tertiary = colors.navigationInteractive,
            onTertiary = colors.background,
            background = colors.background,
            onBackground = colors.infoForeground,
            surface = colors.surface,
            onSurface = colors.infoForeground,
            surfaceVariant = colors.secondaryBackground,
            onSurfaceVariant = colors.supportingText,
            error = colors.alertBackground,
            onError = colors.alertForeground,
            outline = colors.border,
            outlineVariant = colors.border.copy(alpha = 0.6f)
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.infoForeground,
            primaryContainer = colors.surfaceEmphasis,
            onPrimaryContainer = colors.background,
            secondary = colors.surface,
            onSecondary = colors.supportingText,
            secondaryContainer = colors.secondaryBackground,
            onSecondaryContainer = colors.supportingText,
            tertiary = colors.navigationInteractive,
            onTertiary = colors.background,
            background = colors.background,
            onBackground = colors.infoForeground,
            surface = colors.surface,
            onSurface = colors.infoForeground,
            surfaceVariant = colors.secondaryBackground,
            onSurfaceVariant = colors.supportingText,
            error = colors.alertBackground,
            onError = colors.alertForeground,
            outline = colors.border,
            outlineVariant = colors.border.copy(alpha = 0.5f)
        )
    }
}
