package com.lewisdeveloping.gedcomviewer.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(readTheme())
    val theme: StateFlow<AppThemeOption> = _theme.asStateFlow()

    fun select(option: AppThemeOption) {
        if (_theme.value == option) return
        _theme.value = option
        prefs.edit().putString(KEY_SELECTED_THEME, option.name).apply()
    }

    private fun readTheme(): AppThemeOption {
        val raw = prefs.getString(KEY_SELECTED_THEME, null)
        return raw?.runCatching { AppThemeOption.valueOf(this) }?.getOrNull() ?: AppThemeOption.SILVER
    }

    private companion object {
        const val PREFS_NAME = "gedcom_theme_preferences"
        const val KEY_SELECTED_THEME = "selected_theme"
    }
}
