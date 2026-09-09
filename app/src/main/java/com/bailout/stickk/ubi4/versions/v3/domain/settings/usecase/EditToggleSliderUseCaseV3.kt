package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsRules
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderValue

/** Edits one field while preserving the latest locally known value of the other. */
class EditToggleSliderUseCaseV3(private val repository: V3ToggleSliderSettingsRepository) {
    fun setTime(parameterKey: String, timeTenths: Int): V3ToggleSliderValue? {
        require(timeTenths in V3ToggleSliderSettingsRules.allowedTimeRange(parameterKey))
        if (!repository.toggleSliderInteractionEnabled.value) return null
        val current = repository.getToggleSliderValue(parameterKey) ?: V3ToggleSliderValue()
        if (!current.isEnabled) return null
        return current.copy(timeTenths = timeTenths).also { repository.saveToggleSliderValue(parameterKey, it) }
    }

    fun setEnabled(parameterKey: String, enabled: Boolean): V3ToggleSliderValue? {
        V3ToggleSliderSettingsRules.allowedTimeRange(parameterKey)
        if (!repository.toggleSliderInteractionEnabled.value) return null
        val current = repository.getToggleSliderValue(parameterKey) ?: V3ToggleSliderValue()
        return current.copy(isEnabled = enabled).also { repository.saveToggleSliderValue(parameterKey, it) }
    }
}
