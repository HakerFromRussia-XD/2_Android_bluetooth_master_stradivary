package com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3SensorsPlotRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ObservePlotSamplesUseCaseV3(private val repository: V3SensorsPlotRepository) {
    private val callbackScope by lazy { MainScope() }

    operator fun invoke(): Flow<List<Int>> = repository.observeSamples()

    fun observe(callback: (List<Int>) -> Unit): Job = callbackScope.launch {
        invoke().collect { callback(it) }
    }
}
