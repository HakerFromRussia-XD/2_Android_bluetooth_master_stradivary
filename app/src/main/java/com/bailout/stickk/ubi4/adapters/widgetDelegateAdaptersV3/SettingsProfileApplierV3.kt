package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileApplyValue
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.data.local.RotationGroup
import com.bailout.stickk.ubi4.data.parser.ParameterCodecRegistryV3
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.models.ble.GestureV3
import com.bailout.stickk.ubi4.models.ble.RotationGroupV3
import com.bailout.stickk.ubi4.models.gestures.GestureWithAddress
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.MobileSettingsKey
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.logging.platformLog

object SettingsProfileApplierV3 {
    fun apply(values: List<SettingsProfileApplyValue>) {
        values.forEach { value ->
            when (value.target) {
                "BLE" -> applyBle(value)
                "MOBILE" -> applyMobile(value)
            }
        }
    }

    private fun applyBle(value: SettingsProfileApplyValue) {
        val parameterInfo = value.parameterInfo ?: return
        val codecId = value.codecId ?: return
        val typedValue = value.typedValue ?: return

        ParameterStoreV3.put(parameterInfo, typedValue)
        ParameterCodecRegistryV3.encodeToSerialized(codecId, typedValue)?.let { encoded ->
            ParameterProvider.getParameterV3(parameterInfo).data = encoded
        }

        val command = when (typedValue) {
            is ParameterTypedValueV3.Spinner -> BLECommandsV3.sendCommand(
                parameterInfo.parameterID,
                parameterInfo.dataCode,
                typedValue.value.spinnerValue
            )
            is ParameterTypedValueV3.Slider -> BLECommandsV3.sendCommand(
                parameterInfo.parameterID,
                parameterInfo.dataCode,
                typedValue.value.sliderValue
            )
            is ParameterTypedValueV3.Toggle -> BLECommandsV3.sendCommand(
                parameterInfo.parameterID,
                parameterInfo.dataCode,
                typedValue.value.toggleValue
            )
            is ParameterTypedValueV3.EmgGains -> BLECommandsV3.sendGaines(
                typedValue.value.openGain,
                typedValue.value.closeGain
            )
            is ParameterTypedValueV3.Thresholds -> BLECommandsV3.sendThresholds(
                typedValue.value.openThreshold,
                typedValue.value.closeThreshold
            )
            is ParameterTypedValueV3.Switcher -> BLECommandsV3.sendSwitcher(
                parameterInfo.dataCode,
                typedValue.value.checked
            )
            is ParameterTypedValueV3.CurrentGesture -> BLECommandsV3.sendCommand(
                parameterInfo.parameterID,
                parameterInfo.dataCode,
                typedValue.value.currentGesture
            )
            is ParameterTypedValueV3.RotationGroup -> BLECommandsV3.sendRotationGroup(
                typedValue.value.toLegacyRotationGroup()
            )
            is ParameterTypedValueV3.GestureSettings -> BLECommandsV3.sendGestureInfo(
                GestureWithAddress(
                    addressDevice = parameterInfo.deviceAddress,
                    parameterID = parameterInfo.parameterID,
                    gesture = typedValue.value.toLegacyGesture(),
                    gestureState = 0
                )
            )
            else -> {
                platformLog(
                    "SettingsProfileApplierV3",
                    "Skip unsupported profile value: ${typedValue::class.simpleName}"
                )
                null
            }
        } ?: return

        main.bleCommandWithQueue(command, SERIALPORTCHAR_UUID, WRITE) {}
    }

    private fun applyMobile(value: SettingsProfileApplyValue) {
        when (value.mobileKey) {
            MobileSettingsKey.AUTO_LOGIN.key -> {
                main.saveBoolean(
                    PreferenceKeysUbi4.SET_MODE_SMART_CONNECTION,
                    value.mobileBoolean == true
                )
            }
        }
    }
}

private fun RotationGroupV3.toLegacyRotationGroup(): RotationGroup =
    RotationGroup(
        gesture1Id = gesture1Id,
        gesture1ImageId = gesture1ImageId,
        gesture2Id = gesture2Id,
        gesture2ImageId = gesture2ImageId,
        gesture3Id = gesture3Id,
        gesture3ImageId = gesture3ImageId,
        gesture4Id = gesture4Id,
        gesture4ImageId = gesture4ImageId,
        gesture5Id = gesture5Id,
        gesture5ImageId = gesture5ImageId,
        gesture6Id = gesture6Id,
        gesture6ImageId = gesture6ImageId,
        gesture7Id = gesture7Id,
        gesture7ImageId = gesture7ImageId,
        gesture8Id = gesture8Id,
        gesture8ImageId = gesture8ImageId
    )

private fun GestureV3.toLegacyGesture(): Gesture =
    Gesture(
        gestureId = gestureId,
        openPosition1 = openPosition1,
        openPosition2 = openPosition2,
        openPosition3 = openPosition3,
        openPosition4 = openPosition4,
        openPosition5 = openPosition5,
        openPosition6 = openPosition6,
        closePosition1 = closePosition1,
        closePosition2 = closePosition2,
        closePosition3 = closePosition3,
        closePosition4 = closePosition4,
        closePosition5 = closePosition5,
        closePosition6 = closePosition6,
        openToCloseTimeShift1 = openToCloseTimeShift1,
        openToCloseTimeShift2 = openToCloseTimeShift2,
        openToCloseTimeShift3 = openToCloseTimeShift3,
        openToCloseTimeShift4 = openToCloseTimeShift4,
        openToCloseTimeShift5 = openToCloseTimeShift5,
        openToCloseTimeShift6 = openToCloseTimeShift6,
        closeToOpenTimeShift1 = closeToOpenTimeShift1,
        closeToOpenTimeShift2 = closeToOpenTimeShift2,
        closeToOpenTimeShift3 = closeToOpenTimeShift3,
        closeToOpenTimeShift4 = closeToOpenTimeShift4,
        closeToOpenTimeShift5 = closeToOpenTimeShift5,
        closeToOpenTimeShift6 = closeToOpenTimeShift6
    )
