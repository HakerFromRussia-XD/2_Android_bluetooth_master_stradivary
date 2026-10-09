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
