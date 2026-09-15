package com.bailout.stickk.ubi4.versions.v3.presentation.service

import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.V3SliderAction
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.V3SpinnerAction
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRole
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoField

sealed interface V3ServiceAction {
    data object ViewAttached : V3ServiceAction
    data object ViewDetached : V3ServiceAction
    data object ViewDestroyed : V3ServiceAction
    data class SliderAction(val action: V3SliderAction) : V3ServiceAction
    data class SpinnerAction(val action: V3SpinnerAction) : V3ServiceAction
    data class RoleSelected(val role: V3DeviceRole) : V3ServiceAction
    data class RolePinSubmitted(val requestId: Long, val pin: String) : V3ServiceAction
    data class RolePinCancelled(val requestId: Long) : V3ServiceAction
    data class RolePinFeedbackShown(val requestId: Long) : V3ServiceAction
    data class TextInputPrefillRequested(val field: V3DeviceInfoField) : V3ServiceAction
    data class TextInputChanged(val field: V3DeviceInfoField, val text: String) : V3ServiceAction
    data class TextInputSendClicked(val field: V3DeviceInfoField) : V3ServiceAction
    data class TextInputFeedbackShown(val id: Long) : V3ServiceAction
}
