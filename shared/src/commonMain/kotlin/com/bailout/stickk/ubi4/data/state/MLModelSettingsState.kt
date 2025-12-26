package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.data.local.MLModelSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object MLModelSettingsState {
    private val _mlModelSettingsFlow = MutableSharedFlow<MLModelSettings>(replay = 1)
    val mlModelSettingsFlow: SharedFlow<MLModelSettings> = _mlModelSettingsFlow.asSharedFlow()

    fun emitMLModelSettings(settings: MLModelSettings) {
        _mlModelSettingsFlow.tryEmit(settings)
    }
}