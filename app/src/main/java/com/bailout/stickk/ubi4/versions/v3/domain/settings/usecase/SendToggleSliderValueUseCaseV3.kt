package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsRules

class SendToggleSliderValueUseCaseV3(private val repository: V3ToggleSliderSettingsRepository) {
    operator fun invoke(parameterKey: String) {
        V3ToggleSliderSettingsRules.allowedTimeRange(parameterKey)
        if (!repository.toggleSliderInteractionEnabled.value) return
        val value = repository.getToggleSliderValue(parameterKey) ?: return
        repository.sendToggleSliderValue(parameterKey, value)
    }
}
