package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository

class SetSliderValueUseCaseV3(private val repository: V3DeviceSettingsRepository) {
    operator fun invoke(parameterKey: String, value: Int, allowedRange: IntRange) {
        require(value in allowedRange) { "Slider value is outside its allowed range" }
        if (!repository.sliderInteractionEnabled.value) return
        repository.setSliderValue(parameterKey, value)
    }
}
