package com.bailout.stickk.ubi4.versions.v3.domain.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Ordinary device settings; profile management and service-role selection are separate scenarios. */
interface V3SpinnerSettingsRepository {
    val spinnerInteractionEnabled: StateFlow<Boolean>
    fun getSpinnerValue(parameterKey: String): Int?
    fun observeSpinnerValue(parameterKey: String): Flow<Int?>
    fun requestSpinnerValue(parameterKey: String)
    fun setSpinnerValue(parameterKey: String, value: Int)
}

/** Binding-based selection retains its platform persistence and notification order. */
interface V3SpinnerSelectionRepository {
    fun sendSelection(deviceAddress: Int, parameterID: Int, dataCode: Int, dataOffset: Int, index: Int)
    fun applyHandSideDeviceValue(index: Int)
}
