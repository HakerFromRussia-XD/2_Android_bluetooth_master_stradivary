package com.bailout.stickk.ubi4.ui.theme

import androidx.lifecycle.ViewModel
import com.bailout.stickk.ubi4.domain.theme.AppThemeMode
import com.bailout.stickk.ubi4.domain.theme.GetAppThemeUseCase
import com.bailout.stickk.ubi4.domain.theme.SetAppThemeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ThemeSettingsUiState(val selectedMode: AppThemeMode)

sealed interface ThemeSettingsAction {
    data class ThemeSelected(val mode: AppThemeMode) : ThemeSettingsAction
}

class ThemeSettingsViewModel(
    getAppTheme: GetAppThemeUseCase,
    private val setAppTheme: SetAppThemeUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ThemeSettingsUiState(getAppTheme()))
    val uiState = mutableState.asStateFlow()

    fun onAction(action: ThemeSettingsAction) {
        when (action) {
            is ThemeSettingsAction.ThemeSelected -> {
                if (action.mode == mutableState.value.selectedMode) return
                setAppTheme(action.mode)
                mutableState.value = ThemeSettingsUiState(action.mode)
            }
        }
    }
}
