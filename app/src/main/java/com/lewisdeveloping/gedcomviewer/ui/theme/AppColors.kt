package com.lewisdeveloping.gedcomviewer.ui.theme

import androidx.compose.ui.graphics.Color

enum class AppThemeOption {
    SILVER,
    EARTH
}

data class ExtendedColors(
    val background: Color,
    val secondaryBackground: Color,
    val surface: Color,
    val surfaceEmphasis: Color,
    val infoBackground: Color,
    val infoForeground: Color,
    val alertBackground: Color,
    val alertForeground: Color,
    val tabBackground: Color,
    val tabForeground: Color,
    val tabInactiveForeground: Color,
    val tabBackgroundEmphasis: Color,
    val navigationInteractive: Color,
    val border: Color,
    val accent: Color,
    val supportingText: Color
)

internal fun AppThemeOption.extendedColors(darkTheme: Boolean): ExtendedColors {
    val palette = themePalettes.getValue(this)
    return if (darkTheme) palette.dark else palette.light
}

private data class ThemePalette(
    val light: ExtendedColors,
    val dark: ExtendedColors
)

private val themePalettes: Map<AppThemeOption, ThemePalette> = mapOf(
    AppThemeOption.SILVER to ThemePalette(
        light = ExtendedColors(
            background = Color(0xFFE6E8F0),
            secondaryBackground = Color(0xFFF2F2F7),
            surface = Color(0xFFD1D6E0),
            surfaceEmphasis = Color(0xFFA8ADB8),
            infoBackground = Color(0xFFE0E6EB),
            infoForeground = Color(0xFF1F2430),
            alertBackground = Color(0xFFBDC2CC),
            alertForeground = Color(0xFF595C61),
            tabBackground = Color(0xFF525761),
            tabForeground = Color(0xFFF5F7FC),
            tabInactiveForeground = Color(0xFFB8BDC7),
            tabBackgroundEmphasis = Color(0xFF3D424A),
            navigationInteractive = Color(0xFF5770AD),
            border = Color(0xFF8C8F99),
            accent = Color(0xFF737885),
            supportingText = Color(0xFF545761)
        ),
        dark = ExtendedColors(
            background = Color(0xFF24262B),
            secondaryBackground = Color(0xFF2E3036),
            surface = Color(0xFF383B42),
            surfaceEmphasis = Color(0xFF757A85),
            infoBackground = Color(0xFF3D424A),
            infoForeground = Color(0xFFE6E8F0),
            alertBackground = Color(0xFF575C66),
            alertForeground = Color(0xFFE0E6F0),
            tabBackground = Color(0xFF333840),
            tabForeground = Color(0xFFE0E6F0),
            tabInactiveForeground = Color(0xFFA6ABB2),
            tabBackgroundEmphasis = Color(0xFF292B33),
            navigationInteractive = Color(0xFF9EB5D9),
            border = Color(0xFF666B75),
            accent = Color(0xFFB2B8C7),
            supportingText = Color(0xFFC7CCD6)
        )
    ),
    AppThemeOption.EARTH to ThemePalette(
        light = ExtendedColors(
            background = Color(0xFFDBD1A1),
            secondaryBackground = Color(0xFFDEE6B5),
            surface = Color(0xFFADC278),
            surfaceEmphasis = Color(0xFFA88566),
            infoBackground = Color(0xFFE8EDBF),
            infoForeground = Color(0xFF1E2414),
            alertBackground = Color(0xFFD1B899),
            alertForeground = Color(0xFF6B594C),
            tabBackground = Color(0xFFA88566),
            tabForeground = Color(0xFFF2F7EB),
            tabInactiveForeground = Color(0xFFDED1B8),
            tabBackgroundEmphasis = Color(0xFF85664F),
            navigationInteractive = Color(0xFF54784F),
            border = Color(0xFF6B574C),
            accent = Color(0xFF3D7545),
            supportingText = Color(0xFF304736)
        ),
        dark = ExtendedColors(
            background = Color(0xFF0F241A),
            secondaryBackground = Color(0xFF173626),
            surface = Color(0xFF214733),
            surfaceEmphasis = Color(0xFF528F70),
            infoBackground = Color(0xFF1F4C36),
            infoForeground = Color(0xFFE1EFDD),
            alertBackground = Color(0xFF296145),
            alertForeground = Color(0xFFD1F2D4),
            tabBackground = Color(0xFF143324),
            tabForeground = Color(0xFFD1F2D4),
            tabInactiveForeground = Color(0xFF87AB94),
            tabBackgroundEmphasis = Color(0xFF143324),
            navigationInteractive = Color(0xFF99C9A6),
            border = Color(0xFF407852),
            accent = Color(0xFF54A363),
            supportingText = Color(0xFFBDE3C4)
        )
    )
)

internal val DefaultExtendedColors = themePalettes.getValue(AppThemeOption.SILVER).light
