package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRules

class SetSpinnerValueUseCaseV3(private val repository: V3SpinnerSettingsRepository) {
    operator fun invoke(parameterKey: String, value: Int) {
        require(value in V3SpinnerSettingsRules.allowedValues(parameterKey)) { "Spinner value is outside its allowed range" }
        if (!repository.spinnerInteractionEnabled.value) return
        repository.setSpinnerValue(parameterKey, value)
    }
}
