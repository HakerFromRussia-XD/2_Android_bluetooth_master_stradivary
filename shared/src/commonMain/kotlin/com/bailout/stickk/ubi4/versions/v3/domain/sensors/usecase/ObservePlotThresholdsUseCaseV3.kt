package com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3PlotThresholds
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3SensorsPlotRepository

class ObservePlotThresholdsUseCaseV3(private val repository: V3SensorsPlotRepository) {
    private val callbackScope by lazy { MainScope() }

    operator fun invoke(): Flow<V3PlotThresholds?> = repository.observeThresholds()

    fun observe(callback: (V3PlotThresholds?) -> Unit): Job = callbackScope.launch {
        invoke().collect { callback(it) }
    }
}
