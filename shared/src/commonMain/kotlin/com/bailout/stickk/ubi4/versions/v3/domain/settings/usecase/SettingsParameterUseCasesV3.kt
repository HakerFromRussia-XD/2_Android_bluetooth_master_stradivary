package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SettingsParameterRepository

class GetSettingsParameterUseCaseV3(private val repository: V3SettingsParameterRepository) {
    fun toggleSliderValue(deviceAddress: Int, parameterID: Int, dataCode: Int): Int? =
        repository.getToggleSliderParameterValue(deviceAddress, parameterID, dataCode)

    fun spinnerValue(deviceAddress: Int, parameterID: Int, dataCode: Int): Int? =
        repository.getSpinnerParameterValue(deviceAddress, parameterID, dataCode)
}

class RequestSettingsParameterUseCaseV3(private val repository: V3SettingsParameterRepository) {
    operator fun invoke(parameterID: Int, dataCode: Int) = repository.requestParameterValue(parameterID, dataCode)
}

class SendSettingsParameterUseCaseV3(private val repository: V3SettingsParameterRepository) {
    operator fun invoke(deviceAddress: Int, parameterID: Int, dataCode: Int, dataOffset: Int, value: Int) =
        repository.sendParameterValue(deviceAddress, parameterID, dataCode, dataOffset, value)
}
