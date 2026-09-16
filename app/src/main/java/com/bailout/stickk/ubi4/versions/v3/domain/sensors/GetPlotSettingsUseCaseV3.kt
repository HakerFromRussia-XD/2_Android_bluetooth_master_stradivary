package com.bailout.stickk.ubi4.versions.v3.domain.sensors

data class V3PlotSettings(
    val thresholds: V3PlotThresholds?,
    val channelCount: Int,
)

class GetPlotSettingsUseCaseV3(private val repository: V3SensorsPlotRepository) {
    operator fun invoke(): V3PlotSettings = V3PlotSettings(
        repository.getThresholds(), repository.getChannelCount(),
    )
}
