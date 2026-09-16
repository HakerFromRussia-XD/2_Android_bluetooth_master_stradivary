package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class ObserveSliderSettingsUseCaseV3(private val repository: V3DeviceSettingsRepository) {
    operator fun invoke(parameterKeys: Set<String>): Flow<V3SliderSettingsChange> = merge(
        repository.sliderInteractionEnabled.map { V3SliderSettingsChange.InteractionChanged(it) },
        *parameterKeys.map { key ->
            repository.observeSliderValue(key).map { V3SliderSettingsChange.ValueChanged(key, it) }
        }.toTypedArray(),
    )
}
