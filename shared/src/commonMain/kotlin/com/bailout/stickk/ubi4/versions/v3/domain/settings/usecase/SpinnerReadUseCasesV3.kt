package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsSnapshot
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SettingsResponsesRepository
import kotlinx.coroutines.Job

class GetSpinnerSettingsUseCaseV3(private val repository: V3SpinnerSettingsRepository) {
    operator fun invoke(parameterKeys: Set<String>): V3SpinnerSettingsSnapshot =
        V3SpinnerSettingsSnapshot(parameterKeys.associateWith(repository::getSpinnerValue), repository.spinnerInteractionEnabled.value)
}

class RequestSpinnerValueUseCaseV3(private val repository: V3SpinnerSettingsRepository) {
    operator fun invoke(parameterKey: String) = repository.requestSpinnerValue(parameterKey)
}

class ObserveSpinnerResponsesUseCaseV3(private val repository: V3SettingsResponsesRepository) {
    operator fun invoke(deviceAddress: Int, parameterID: Int, dataCode: Int, onResponse: (Int?) -> Unit): Job =
        repository.observeSpinnerResponses(deviceAddress, parameterID, dataCode, onResponse)
}
