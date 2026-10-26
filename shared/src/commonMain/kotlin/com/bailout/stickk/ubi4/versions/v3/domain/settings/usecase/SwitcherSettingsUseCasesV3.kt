package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SettingsResponsesRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SwitcherSettingsRepository
import kotlinx.coroutines.Job

class ObserveSwitcherResponsesUseCaseV3(private val repository: V3SettingsResponsesRepository) {
    operator fun invoke(deviceAddress: Int, parameterID: Int, dataCode: Int, onResponse: (Boolean?) -> Unit): Job =
        repository.observeSwitcherResponses(deviceAddress, parameterID, dataCode, onResponse)
}

class GetSwitcherValueUseCaseV3(private val repository: V3SwitcherSettingsRepository) {
    operator fun invoke(deviceAddress: Int, parameterID: Int, dataCode: Int): Boolean? =
        repository.getSwitcherValue(deviceAddress, parameterID, dataCode)
}

class RequestSwitcherValueUseCaseV3(private val repository: V3SwitcherSettingsRepository) {
    operator fun invoke(parameterID: Int, dataCode: Int) = repository.requestSwitcherValue(parameterID, dataCode)
}

class SetSwitcherValueUseCaseV3(private val repository: V3SwitcherSettingsRepository) {
    operator fun invoke(deviceAddress: Int, parameterID: Int, dataCode: Int, value: Boolean) =
        repository.setSwitcherValue(deviceAddress, parameterID, dataCode, value)
}
