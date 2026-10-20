package com.bailout.stickk.ubi4.ui.theme

import android.content.SharedPreferences
import com.bailout.stickk.ubi4.data.theme.ThemeSettingsRepositoryImpl
import com.bailout.stickk.ubi4.domain.theme.AppThemeMode
import com.bailout.stickk.ubi4.domain.theme.GetAppThemeUseCase
import com.bailout.stickk.ubi4.domain.theme.SetAppThemeUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ThemeSettingsTest {
    private val preferences = mockk<SharedPreferences>()
    private val editor = mockk<SharedPreferences.Editor>()
    private val stored = mutableMapOf<String, String?>()
    private val repository = ThemeSettingsRepositoryImpl(preferences)

    init {
        every { preferences.getString(any(), any()) } answers { stored[firstArg()] ?: secondArg() }
        every { preferences.edit() } returns editor
        every { editor.putString(any(), any()) } answers { stored[firstArg()] = secondArg(); editor }
        every { editor.apply() } returns Unit
    }

    private fun viewModel() = ThemeSettingsViewModel(GetAppThemeUseCase(repository), SetAppThemeUseCase(repository))

    @Test fun `no saved choice follows the system without writing during initial rendering`() {
        assertEquals(AppThemeMode.SYSTEM, viewModel().uiState.value.selectedMode)
        verify(exactly = 0) { preferences.edit() }
    }

    @Test fun `all theme choices survive a new repository and screen instance`() {
        val screen = viewModel()
        listOf(AppThemeMode.LIGHT, AppThemeMode.SYSTEM, AppThemeMode.DARK).forEach { mode ->
            screen.onAction(ThemeSettingsAction.ThemeSelected(mode))
            assertEquals(mode, screen.uiState.value.selectedMode)
            val restoredRepository = ThemeSettingsRepositoryImpl(preferences)
            val restored = ThemeSettingsViewModel(GetAppThemeUseCase(restoredRepository), SetAppThemeUseCase(restoredRepository))
            assertEquals(mode, restored.uiState.value.selectedMode)
        }
    }

    @Test fun `selecting the current theme does not rewrite preferences`() {
        viewModel().onAction(ThemeSettingsAction.ThemeSelected(AppThemeMode.SYSTEM))
        verify(exactly = 0) { preferences.edit() }
    }

    @Test fun `unknown saved values follow the system`() {
        every { preferences.getString(any(), any()) } returns "unsupported_theme"
        assertEquals(AppThemeMode.SYSTEM, repository.getThemeMode())
    }

    @Test fun `explicit light and dark choices override the system default`() {
        for (mode in listOf(AppThemeMode.LIGHT, AppThemeMode.DARK)) {
            repository.setThemeMode(mode)
            assertEquals(mode, viewModel().uiState.value.selectedMode)
        }
    }
}
