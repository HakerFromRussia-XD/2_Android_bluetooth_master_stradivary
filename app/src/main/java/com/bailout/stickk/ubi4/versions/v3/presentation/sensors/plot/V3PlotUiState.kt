package com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot

import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3PlotThresholds

data class V3PlotFrame(val sequence: Long, val values: List<Int>)

data class V3PlotUiState(
    val thresholds: V3PlotThresholds = V3PlotThresholds(),
    val isEnabled: Boolean = false,
    val animateThresholdChanges: Boolean = false,
    val channelCount: Int = 2,
    val frame: V3PlotFrame? = null,
    val isPaused: Boolean = false,
)
