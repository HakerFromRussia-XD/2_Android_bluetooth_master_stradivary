package com.bailout.stickk.ubi4.versions.v3.domain.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface V3ToggleSliderSettingsRepository {
    val toggleSliderInteractionEnabled: StateFlow<Boolean>
    fun observeToggleSliderValue(parameterKey: String): Flow<V3ToggleSliderValue?>
    fun getToggleSliderValue(parameterKey: String): V3ToggleSliderValue?

    /** Requests a device response without saving or changing the displayed draft. */
    fun requestToggleSliderValue(parameterKey: String)

    /** Optimistic local update and profile persistence, without a BLE command. */
    fun saveToggleSliderValue(parameterKey: String, value: V3ToggleSliderValue)
    fun sendToggleSliderValue(parameterKey: String, value: V3ToggleSliderValue)
}
