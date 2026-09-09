package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings

import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.SliderUiStateV3
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.togglesliders.ToggleSliderUiStateV3

enum class V3SpecialSettingsSection { PROSTHESIS, APPLICATION }

data class V3SpecialSettingsUiState(
    val selectedSection: V3SpecialSettingsSection,
    val sliders: Map<String, SliderUiStateV3>,
    val deviceProfile: V3DeviceProfile,
    val widgets: List<V3SpecialSettingsWidget>,
    val toggleSliders: Map<String, ToggleSliderUiStateV3>,
)
