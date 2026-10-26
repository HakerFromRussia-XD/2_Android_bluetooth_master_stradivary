package com.bailout.stickk.ubi4.versions.v3.domain.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job

/** V3 device settings identified by the existing widget parameter keys. */
interface V3DeviceSettingsRepository {
    val sliderInteractionEnabled: StateFlow<Boolean>

    /** Local and incoming updates; observing never sends a device command. */
    fun observeSliderValue(parameterKey: String): Flow<Int?>

    /** Returns the local value, which may include a change not yet confirmed by the device. */
    fun getSliderValue(parameterKey: String): Int?

    /** Requests a device response without changing the local value or pending edits. */
    fun requestSliderValue(parameterKey: String)

    /** Updates the local value and schedules persistence and sending; does not acknowledge the device. */
    fun setSliderValue(parameterKey: String, value: Int)
}

/** Response events include repeated values and do not publish an initial cached value. */
interface V3SliderResponsesRepository {
    fun observeSliderResponses(parameterKey: String, onResponse: () -> Unit): Job
}

/** Received scalar values may be absent; repeated responses are still delivered. */
interface V3SettingsResponsesRepository {
    fun observeToggleSliderResponses(
        deviceAddress: Int, parameterID: Int, dataCode: Int, onResponse: (Int?) -> Unit,
    ): Job

    fun observeSpinnerResponses(
        deviceAddress: Int, parameterID: Int, dataCode: Int, onResponse: (Int?) -> Unit,
    ): Job

    fun observeSwitcherResponses(
        deviceAddress: Int, parameterID: Int, dataCode: Int, onResponse: (Boolean?) -> Unit,
    ): Job
}

interface V3SwitcherSettingsRepository {
    fun getSwitcherValue(deviceAddress: Int, parameterID: Int, dataCode: Int): Boolean?
    fun requestSwitcherValue(parameterID: Int, dataCode: Int)
    fun setSwitcherValue(deviceAddress: Int, parameterID: Int, dataCode: Int, value: Boolean)
}

/** Settings addressed by their device binding, without a canonical parameter key. */
interface V3SettingsParameterRepository {
    fun getToggleSliderParameterValue(deviceAddress: Int, parameterID: Int, dataCode: Int): Int?
    fun getSpinnerParameterValue(deviceAddress: Int, parameterID: Int, dataCode: Int): Int?
    fun requestParameterValue(parameterID: Int, dataCode: Int)
    fun sendParameterValue(deviceAddress: Int, parameterID: Int, dataCode: Int, dataOffset: Int, value: Int)
}
