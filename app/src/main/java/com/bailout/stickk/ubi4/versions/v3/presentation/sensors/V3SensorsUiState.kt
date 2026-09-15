package com.bailout.stickk.ubi4.versions.v3.presentation.sensors

import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets.V3SensorsWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.SliderUiStateV3

import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot.V3PlotUiState

import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.buttons.V3SensorsButtonsUiState

data class V3SensorsUiState(
    val deviceProfile: V3DeviceProfile,
    val widgets: List<V3SensorsWidget>,
    val sliders: Map<String, SliderUiStateV3>,
    val animationsEnabled: Boolean = true,
    // Only the existing pull-to-refresh animation; this is not device synchronization readiness.
    val isRefreshIndicatorVisible: Boolean = false,
    val plot: V3PlotUiState? = null,
    val buttons: V3SensorsButtonsUiState = V3SensorsButtonsUiState(),
)
