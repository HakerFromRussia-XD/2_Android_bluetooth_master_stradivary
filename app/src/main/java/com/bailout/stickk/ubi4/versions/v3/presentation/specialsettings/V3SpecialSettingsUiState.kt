package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings

import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3SpecialSettingsSection

import com.bailout.stickk.ubi4.versions.v3.presentation.autologin.V3AutoLoginUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.SliderUiStateV3
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.togglesliders.ToggleSliderUiStateV3
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.SpinnerUiStateV3
import com.bailout.stickk.ubi4.versions.v3.presentation.settingsprofiles.V3SettingsProfilesUiState

data class V3SpecialSettingsUiState(
    val selectedSection: V3SpecialSettingsSection,
    val settingsSectionReadFailed: Boolean = false,
    val settingsSectionSaveFailed: Boolean = false,
    val sliders: Map<String, SliderUiStateV3>,
    val deviceProfile: V3DeviceProfile,
    val widgets: List<V3SpecialSettingsWidget>,
    val toggleSliders: Map<String, ToggleSliderUiStateV3>,
    val spinners: Map<String, SpinnerUiStateV3>,
    val autoLogin: V3AutoLoginUiState = V3AutoLoginUiState(),
    val settingsProfiles: V3SettingsProfilesUiState? = null,
    val animationsEnabled: Boolean = true,
)
