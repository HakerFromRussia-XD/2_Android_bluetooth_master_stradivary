package com.bailout.stickk.ubi4.versions.v3.presentation.service

import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets.V3ServiceWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.SliderUiStateV3
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.SpinnerUiStateV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoField

data class V3ServiceUiState(
    val deviceProfile: V3DeviceProfile,
    val widgets: List<V3ServiceWidget>,
    val sliders: Map<String, SliderUiStateV3>,
    val animationsEnabled: Boolean = true,
    val spinners: Map<String, SpinnerUiStateV3> = emptyMap(),
    val role: V3ServiceRoleUiState? = null,
    val textInputs: Map<V3DeviceInfoField, V3ServiceTextInputUiState> = emptyMap(),
    val textInputFeedback: V3TextInputFeedback? = null,
    val calibration: V3ProsthesisCalibrationUiState? = null,
)
