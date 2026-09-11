package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings

import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.V3SliderAction
import com.bailout.stickk.ubi4.versions.v3.presentation.togglesliders.V3ToggleSliderAction
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.V3SpinnerAction

sealed interface V3SpecialSettingsAction {
    data object ViewAttached : V3SpecialSettingsAction
    data object ViewDetached : V3SpecialSettingsAction
    data class SettingsSectionSelected(val section: V3SpecialSettingsSection) : V3SpecialSettingsAction
    data class SliderAction(val action: V3SliderAction) : V3SpecialSettingsAction
    data class ToggleSliderAction(val action: V3ToggleSliderAction) : V3SpecialSettingsAction
    data class SpinnerAction(val action: V3SpinnerAction) : V3SpecialSettingsAction
    data class SettingsProfileSelected(val profileId: Int) : V3SpecialSettingsAction
    data object SettingsProfileCreateRequested : V3SpecialSettingsAction
    data class SettingsProfileRenameRequested(val profileId: Int) : V3SpecialSettingsAction
    data class SettingsProfileNameSubmitted(val requestId: Long, val name: String) : V3SpecialSettingsAction
    data class SettingsProfileNameDismissed(val requestId: Long) : V3SpecialSettingsAction
}
