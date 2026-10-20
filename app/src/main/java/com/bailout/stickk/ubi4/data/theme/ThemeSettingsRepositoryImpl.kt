package com.bailout.stickk.ubi4.data.theme

import android.content.SharedPreferences
import com.bailout.stickk.ubi4.domain.theme.AppThemeMode
import com.bailout.stickk.ubi4.domain.theme.ThemeSettingsRepository

class ThemeSettingsRepositoryImpl(private val preferences: SharedPreferences) : ThemeSettingsRepository {
    override fun getThemeMode(): AppThemeMode {
        val savedMode = preferences.getString(THEME_MODE_KEY, null)
        return AppThemeMode.entries.firstOrNull { it.name == savedMode } ?: AppThemeMode.SYSTEM
    }

    override fun setThemeMode(mode: AppThemeMode) {
        preferences.edit().putString(THEME_MODE_KEY, mode.name).apply()
    }

    private companion object {
        const val THEME_MODE_KEY = "theme_mode"
    }
}
