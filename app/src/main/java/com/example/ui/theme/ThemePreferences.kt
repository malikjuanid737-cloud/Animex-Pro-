package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ThemePreferences {
    private const val PREFS_NAME = "animex_theme_preferences"
    private const val KEY_DARK_MODE = "is_dark_mode_enabled"

    private lateinit var prefs: SharedPreferences
    private val _isDarkMode = MutableStateFlow(true) // Default to dark anime theme
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isDarkMode.value = prefs.getBoolean(KEY_DARK_MODE, true)
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun toggleTheme() {
        setDarkMode(!_isDarkMode.value)
    }
}
