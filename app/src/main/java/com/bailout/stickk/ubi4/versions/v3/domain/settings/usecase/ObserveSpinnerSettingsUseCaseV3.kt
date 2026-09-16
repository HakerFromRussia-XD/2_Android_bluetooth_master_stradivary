package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class ObserveSpinnerSettingsUseCaseV3(private val repository: V3SpinnerSettingsRepository) {
    operator fun invoke(parameterKeys: Set<String>): Flow<V3SpinnerSettingsChange> = merge(
        repository.spinnerInteractionEnabled.map { V3SpinnerSettingsChange.InteractionChanged(it) },
        *parameterKeys.map { key ->
            repository.observeSpinnerValue(key).map { V3SpinnerSettingsChange.ValueChanged(key, it) }
        }.toTypedArray(),
    )
}
