package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.*

class GetSliderSettingsUseCaseV3(private val repository: V3DeviceSettingsRepository) {
    operator fun invoke(parameterKeys: Set<String>): V3SliderSettingsSnapshot =
        V3SliderSettingsSnapshot(parameterKeys.associateWith(repository::getSliderValue), repository.sliderInteractionEnabled.value)
}
