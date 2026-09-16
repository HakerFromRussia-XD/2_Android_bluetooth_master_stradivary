package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.*

class GetSpinnerSettingsUseCaseV3(private val repository: V3SpinnerSettingsRepository) {
    operator fun invoke(parameterKeys: Set<String>): V3SpinnerSettingsSnapshot =
        V3SpinnerSettingsSnapshot(parameterKeys.associateWith(repository::getSpinnerValue), repository.spinnerInteractionEnabled.value)
}
