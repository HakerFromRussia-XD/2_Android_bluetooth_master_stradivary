package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings

import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.SliderUiStateV3
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.togglesliders.ToggleSliderUiStateV3
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.SpinnerUiStateV3
import com.bailout.stickk.ubi4.versions.v3.presentation.settingsprofiles.V3SettingsProfilesUiState

enum class V3SpecialSettingsSection { PROSTHESIS, APPLICATION }

data class V3SpecialSettingsUiState(
    val selectedSection: V3SpecialSettingsSection,
    val sliders: Map<String, SliderUiStateV3>,
    val deviceProfile: V3DeviceProfile,
    val widgets: List<V3SpecialSettingsWidget>,
    val toggleSliders: Map<String, ToggleSliderUiStateV3>,
    val spinners: Map<String, SpinnerUiStateV3>,
    val settingsProfiles: V3SettingsProfilesUiState? = null,
)
