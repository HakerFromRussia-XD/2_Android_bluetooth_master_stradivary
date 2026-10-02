package com.bailout.stickk.ubi4.versions.v3.domain.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Ordinary device settings; profile management and service-role selection are separate scenarios. */
interface V3SpinnerSettingsRepository {
    val spinnerInteractionEnabled: StateFlow<Boolean>
    fun getSpinnerValue(parameterKey: String): Int?
    fun observeSpinnerValue(parameterKey: String): Flow<Int?>
    fun setSpinnerValue(parameterKey: String, value: Int)
}
