package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.*

class GetToggleSliderSettingsUseCaseV3(private val repository: V3ToggleSliderSettingsRepository) {
    operator fun invoke(parameterKeys: Set<String>): V3ToggleSliderSettingsSnapshot =
        V3ToggleSliderSettingsSnapshot(parameterKeys.associateWith(repository::getToggleSliderValue), repository.toggleSliderInteractionEnabled.value)
}
