package com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3PlotThresholds
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3SensorsPlotRepository

data class V3PlotSettings(
    val thresholds: V3PlotThresholds?,
    val channelCount: Int,
)

class GetPlotSettingsUseCaseV3(private val repository: V3SensorsPlotRepository) {
    operator fun invoke(): V3PlotSettings = V3PlotSettings(
        repository.getThresholds(), repository.getChannelCount(),
    )
}

class RequestPlotThresholdsUseCaseV3(private val repository: V3SensorsPlotRepository) {
    operator fun invoke(parameterID: Int, dataCode: Int) = repository.requestThresholds(parameterID, dataCode)
}
