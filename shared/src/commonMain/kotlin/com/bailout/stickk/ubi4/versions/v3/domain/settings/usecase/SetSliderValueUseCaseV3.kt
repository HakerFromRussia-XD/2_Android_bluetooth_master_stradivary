package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SliderSettingsRules

class SetSliderValueUseCaseV3(private val repository: V3DeviceSettingsRepository) {
    operator fun invoke(parameterKey: String, value: Int) {
        require(value in V3SliderSettingsRules.allowedRange(parameterKey)) {
            "Slider value is outside its allowed range"
        }
        if (!repository.sliderInteractionEnabled.value) return
        repository.setSliderValue(parameterKey, value)
    }
}
