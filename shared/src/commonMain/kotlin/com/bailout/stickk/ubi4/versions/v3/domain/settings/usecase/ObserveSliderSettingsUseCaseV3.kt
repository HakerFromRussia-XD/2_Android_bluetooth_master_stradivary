package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SliderSettingsChange
import kotlinx.coroutines.Job

class ObserveSliderSettingsUseCaseV3(private val repository: V3DeviceSettingsRepository) {
    operator fun invoke(parameterKeys: Set<String>): Flow<V3SliderSettingsChange> = merge(
        repository.sliderInteractionEnabled.map { V3SliderSettingsChange.InteractionChanged(it) },
        *parameterKeys.map { key ->
            repository.observeSliderValue(key).map { V3SliderSettingsChange.ValueChanged(key, it) }
        }.toTypedArray(),
    )
}

/** Observes responses separately from state changes so an unchanged reply is still delivered. */
class ObserveSliderResponsesUseCaseV3(private val repository: V3SliderResponsesRepository) {
    operator fun invoke(parameterKey: String, onResponse: () -> Unit): Job =
        repository.observeSliderResponses(parameterKey, onResponse)
}
