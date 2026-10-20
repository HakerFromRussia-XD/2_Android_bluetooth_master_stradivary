package com.bailout.stickk.ubi4.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bailout.stickk.ubi4.data.theme.ThemeSettingsRepositoryImpl
import com.bailout.stickk.ubi4.domain.theme.GetAppThemeUseCase
import com.bailout.stickk.ubi4.domain.theme.SetAppThemeUseCase
import com.bailout.stickk.ubi4.ui.theme.AppThemeModeApplier
import com.bailout.stickk.ubi4.ui.theme.ThemeSettingsViewModel

object ThemeDependencies {
    private fun repository(context: Context) = ThemeSettingsRepositoryImpl(
        context.applicationContext.getSharedPreferences("ubi4_appearance", Context.MODE_PRIVATE),
    )

    fun applySavedTheme(context: Context) {
        AppThemeModeApplier.apply(GetAppThemeUseCase(repository(context))())
    }

    fun viewModelFactory(context: Context): ViewModelProvider.Factory {
        val repository = repository(context)
        return object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ThemeSettingsViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return ThemeSettingsViewModel(GetAppThemeUseCase(repository), SetAppThemeUseCase(repository)) as T
            }
        }
    }
}
