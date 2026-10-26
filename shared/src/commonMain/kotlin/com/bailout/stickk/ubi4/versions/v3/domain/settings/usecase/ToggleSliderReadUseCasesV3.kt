package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsSnapshot
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SettingsResponsesRepository
import kotlinx.coroutines.Job

class GetToggleSliderSettingsUseCaseV3(private val repository: V3ToggleSliderSettingsRepository) {
    operator fun invoke(parameterKeys: Set<String>): V3ToggleSliderSettingsSnapshot =
        V3ToggleSliderSettingsSnapshot(parameterKeys.associateWith(repository::getToggleSliderValue), repository.toggleSliderInteractionEnabled.value)
}

class RequestToggleSliderValueUseCaseV3(private val repository: V3ToggleSliderSettingsRepository) {
    operator fun invoke(parameterKey: String) = repository.requestToggleSliderValue(parameterKey)
}

class ObserveToggleSliderResponsesUseCaseV3(private val repository: V3SettingsResponsesRepository) {
    operator fun invoke(deviceAddress: Int, parameterID: Int, dataCode: Int, onResponse: (Int?) -> Unit): Job =
        repository.observeToggleSliderResponses(deviceAddress, parameterID, dataCode, onResponse)
}
