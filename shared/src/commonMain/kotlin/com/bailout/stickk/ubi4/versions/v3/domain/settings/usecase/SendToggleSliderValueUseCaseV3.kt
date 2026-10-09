package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsRules
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderValue

class SendToggleSliderValueUseCaseV3(
    private val repository: V3ToggleSliderSettingsRepository,
    private val requireInteractionEnabled: Boolean = true,
) {
    private val isInteractionAllowed: Boolean
        get() = !requireInteractionEnabled || repository.toggleSliderInteractionEnabled.value

    operator fun invoke(parameterKey: String) {
        V3ToggleSliderSettingsRules.allowedTimeRange(parameterKey)
        if (!isInteractionAllowed) return
        val value = repository.getToggleSliderValue(parameterKey) ?: return
        repository.sendToggleSliderValue(parameterKey, value)
    }

    /** Sends the committed UI draft without reading or optimistically saving another value. */
    operator fun invoke(parameterKey: String, value: V3ToggleSliderValue) {
        require(value.timeTenths in V3ToggleSliderSettingsRules.allowedTimeRange(parameterKey))
        if (!isInteractionAllowed) return
        repository.sendToggleSliderValue(parameterKey, value)
    }
}
