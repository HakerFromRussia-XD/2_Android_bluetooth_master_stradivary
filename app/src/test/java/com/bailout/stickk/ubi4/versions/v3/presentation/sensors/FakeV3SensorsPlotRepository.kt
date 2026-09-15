package com.bailout.stickk.ubi4.versions.v3.presentation.sensors

import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3PlotThresholds
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3SensorsPlotRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeV3SensorsPlotRepository : V3SensorsPlotRepository {
    override val interactionEnabled = MutableStateFlow(true)
    val thresholds = MutableStateFlow<V3PlotThresholds?>(V3PlotThresholds(158, 109))
    val samples = MutableSharedFlow<List<Int>>()
    var channels = 2
    var paused = false
    val writes = mutableListOf<V3PlotThresholds>()
    override fun getThresholds() = thresholds.value
    override fun observeThresholds() = thresholds
    override fun observeSamples() = samples
    override fun getChannelCount() = channels
    override fun arePlotPointsPaused() = paused
    override fun setThresholds(thresholds: V3PlotThresholds) {
        writes += thresholds
        this.thresholds.value = thresholds
    }
}
