package com.bailout.stickk.ubi4.domain.theme

interface ThemeSettingsRepository {
    fun getThemeMode(): AppThemeMode
    fun setThemeMode(mode: AppThemeMode)
}

class GetAppThemeUseCase(private val repository: ThemeSettingsRepository) {
    operator fun invoke(): AppThemeMode = repository.getThemeMode()
}

class SetAppThemeUseCase(private val repository: ThemeSettingsRepository) {
    operator fun invoke(mode: AppThemeMode) = repository.setThemeMode(mode)
}
