package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class ObserveToggleSliderSettingsUseCaseV3(private val repository: V3ToggleSliderSettingsRepository) {
    operator fun invoke(parameterKeys: Set<String>): Flow<V3ToggleSliderSettingsChange> = merge(
        repository.toggleSliderInteractionEnabled.map { V3ToggleSliderSettingsChange.InteractionChanged(it) },
        *parameterKeys.map { key ->
            repository.observeToggleSliderValue(key).map { V3ToggleSliderSettingsChange.ValueChanged(key, it) }
        }.toTypedArray(),
    )
}
