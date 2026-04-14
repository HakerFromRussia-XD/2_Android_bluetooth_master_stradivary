package com.bailout.stickk.ubi4.data.parser

import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.models.ble.CurrentGestureV3
import com.bailout.stickk.ubi4.models.ble.EMGGainsV3
import com.bailout.stickk.ubi4.models.ble.GestureV3
import com.bailout.stickk.ubi4.models.ble.ParameterCodecIdV3
import com.bailout.stickk.ubi4.models.ble.RotationGroupV3
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.models.ble.SpinnerV3
import com.bailout.stickk.ubi4.models.ble.SwitcherV3
import com.bailout.stickk.ubi4.models.ble.ThresholdsV3
import com.bailout.stickk.ubi4.models.ble.ToggleV3
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed interface ParameterCodecActionV3 {
    data class SetInt(
        val value: Int,
        val dataOffset: Int = 0
    ) : ParameterCodecActionV3

    data class SetBoolean(val checked: Boolean) : ParameterCodecActionV3

    data class SetText(val text: String) : ParameterCodecActionV3
}

sealed interface ParameterEncodedActionV3 {
    data class IntValue(val value: Int) : ParameterEncodedActionV3
    data class ByteArrayValue(val value: ByteArray) : ParameterEncodedActionV3
    data class EmgGainsValue(
        val openGain: Int,
        val closeGain: Int
    ) : ParameterEncodedActionV3
}

object ParameterCodecRegistryV3 {
    private val json = Json { ignoreUnknownKeys = true }

    fun decodeFromPayload(
        codecId: ParameterCodecIdV3,
        payload: ByteArrayView?
    ): ParameterTypedValueV3? {
        return when (codecId) {
            ParameterCodecIdV3.SPINNER -> {
                if (payload == null || payload.length < 2) return null
                ParameterTypedValueV3.Spinner(SpinnerV3(spinnerValue = payload.u8(1)))
            }
            ParameterCodecIdV3.SLIDER -> {
                if (payload == null || payload.length < 2) return null
                ParameterTypedValueV3.Slider(SliderV3(sliderValue = payload.u8(1)))
            }
            ParameterCodecIdV3.TOGGLE -> {
                if (payload == null || payload.length < 2) return null
                ParameterTypedValueV3.Toggle(ToggleV3(toggleValue = payload.u8(1)))
            }
            ParameterCodecIdV3.EMG_GAINS -> {
                if (payload == null || payload.length < 3) return null
                ParameterTypedValueV3.EmgGains(
                    EMGGainsV3(
                        openGain = payload.u8(1),
                        closeGain = payload.u8(2)
                    )
                )
            }
            ParameterCodecIdV3.THRESHOLDS -> {
                if (payload == null || payload.length < 3) return null
                ParameterTypedValueV3.Thresholds(
                    ThresholdsV3(
                        openThreshold = payload.u8(1),
                        closeThreshold = payload.u8(2)
                    )
                )
            }
            ParameterCodecIdV3.CURRENT_GESTURE -> {
                if (payload == null || payload.length < 2) return null
                ParameterTypedValueV3.CurrentGesture(
                    CurrentGestureV3(currentGesture = payload.u8(1))
                )
            }
            ParameterCodecIdV3.ROTATION_GROUP -> {
                if (payload == null || payload.length < 17) return null
                ParameterTypedValueV3.RotationGroup(
                    RotationGroupV3(
                        gesture1Id = payload.u8(1),
                        gesture1ImageId = payload.u8(2),
                        gesture2Id = payload.u8(3),
                        gesture2ImageId = payload.u8(4),
                        gesture3Id = payload.u8(5),
                        gesture3ImageId = payload.u8(6),
                        gesture4Id = payload.u8(7),
                        gesture4ImageId = payload.u8(8),
                        gesture5Id = payload.u8(9),
                        gesture5ImageId = payload.u8(10),
                        gesture6Id = payload.u8(11),
                        gesture6ImageId = payload.u8(12),
                        gesture7Id = payload.u8(13),
                        gesture7ImageId = payload.u8(14),
                        gesture8Id = payload.u8(15),
                        gesture8ImageId = payload.u8(16),
                    )
                )
            }
            ParameterCodecIdV3.GESTURE_SETTINGS -> {
                if (payload == null || payload.length < 26) return null
                ParameterTypedValueV3.GestureSettings(
                    GestureV3(
                        gestureId = payload.u8(1),
                        openPosition1 = payload.u8(2),
                        openPosition2 = payload.u8(3),
                        openPosition3 = payload.u8(4),
                        openPosition4 = payload.u8(5),
                        openPosition5 = payload.u8(6),
                        openPosition6 = payload.u8(7),
                        closePosition1 = payload.u8(8),
                        closePosition2 = payload.u8(9),
                        closePosition3 = payload.u8(10),
                        closePosition4 = payload.u8(11),
                        closePosition5 = payload.u8(12),
                        closePosition6 = payload.u8(13),
                        openToCloseTimeShift1 = payload.u8(14),
                        openToCloseTimeShift2 = payload.u8(15),
                        openToCloseTimeShift3 = payload.u8(16),
                        openToCloseTimeShift4 = payload.u8(17),
                        openToCloseTimeShift5 = payload.u8(18),
                        openToCloseTimeShift6 = payload.u8(19),
                        closeToOpenTimeShift1 = payload.u8(20),
                        closeToOpenTimeShift2 = payload.u8(21),
                        closeToOpenTimeShift3 = payload.u8(22),
                        closeToOpenTimeShift4 = payload.u8(23),
                        closeToOpenTimeShift5 = payload.u8(24),
                        closeToOpenTimeShift6 = payload.u8(25),
                    )
                )
            }
            ParameterCodecIdV3.SWITCHER -> {
                if (payload == null || payload.length < 2) return null
                ParameterTypedValueV3.Switcher(
                    SwitcherV3(checked = payload.u8(1) != 0)
                )
            }
            ParameterCodecIdV3.TEXT -> {
                if (payload == null || payload.length < 2) return null
                val bytes = payload.toByteArray().drop(1).takeWhile { it.toInt() != 0 }.toByteArray()
                ParameterTypedValueV3.Text(bytes.decodeToString())
            }
            ParameterCodecIdV3.NONE -> null
        }
    }

    fun decodeFromSerialized(
        codecId: ParameterCodecIdV3,
        data: String
    ): ParameterTypedValueV3? {
        if (data.isBlank()) return null
        return runCatching {
            when (codecId) {
                ParameterCodecIdV3.SPINNER ->
                    ParameterTypedValueV3.Spinner(json.decodeFromString<SpinnerV3>(data))
                ParameterCodecIdV3.SLIDER ->
                    ParameterTypedValueV3.Slider(json.decodeFromString<SliderV3>(data))
                ParameterCodecIdV3.TOGGLE ->
                    ParameterTypedValueV3.Toggle(json.decodeFromString<ToggleV3>(data))
                ParameterCodecIdV3.EMG_GAINS ->
                    ParameterTypedValueV3.EmgGains(json.decodeFromString<EMGGainsV3>(data))
                ParameterCodecIdV3.THRESHOLDS ->
                    ParameterTypedValueV3.Thresholds(json.decodeFromString<ThresholdsV3>(data))
                ParameterCodecIdV3.CURRENT_GESTURE ->
                    ParameterTypedValueV3.CurrentGesture(json.decodeFromString<CurrentGestureV3>(data))
                ParameterCodecIdV3.ROTATION_GROUP ->
                    ParameterTypedValueV3.RotationGroup(json.decodeFromString<RotationGroupV3>(data))
                ParameterCodecIdV3.GESTURE_SETTINGS ->
                    ParameterTypedValueV3.GestureSettings(json.decodeFromString<GestureV3>(data))
                ParameterCodecIdV3.SWITCHER ->
                    ParameterTypedValueV3.Switcher(json.decodeFromString<SwitcherV3>(data))
                ParameterCodecIdV3.TEXT ->
                    ParameterTypedValueV3.Text(data)
                ParameterCodecIdV3.NONE -> null
            }
        }.onFailure {
            platformLog("ParameterCodecRegistryV3", "decodeFromSerialized failed: ${it.message}")
        }.getOrNull()
    }

    fun encodeToSerialized(
        codecId: ParameterCodecIdV3,
        typedValue: ParameterTypedValueV3
    ): String? {
        return runCatching {
            when (codecId) {
                ParameterCodecIdV3.SPINNER -> json.encodeToString((typedValue as ParameterTypedValueV3.Spinner).value)
                ParameterCodecIdV3.SLIDER -> json.encodeToString((typedValue as ParameterTypedValueV3.Slider).value)
                ParameterCodecIdV3.TOGGLE -> json.encodeToString((typedValue as ParameterTypedValueV3.Toggle).value)
                ParameterCodecIdV3.EMG_GAINS -> json.encodeToString((typedValue as ParameterTypedValueV3.EmgGains).value)
                ParameterCodecIdV3.THRESHOLDS -> json.encodeToString((typedValue as ParameterTypedValueV3.Thresholds).value)
                ParameterCodecIdV3.CURRENT_GESTURE -> json.encodeToString((typedValue as ParameterTypedValueV3.CurrentGesture).value)
                ParameterCodecIdV3.ROTATION_GROUP -> json.encodeToString((typedValue as ParameterTypedValueV3.RotationGroup).value)
                ParameterCodecIdV3.GESTURE_SETTINGS -> json.encodeToString((typedValue as ParameterTypedValueV3.GestureSettings).value)
                ParameterCodecIdV3.SWITCHER -> json.encodeToString((typedValue as ParameterTypedValueV3.Switcher).value)
                ParameterCodecIdV3.TEXT -> (typedValue as ParameterTypedValueV3.Text).value
                ParameterCodecIdV3.NONE -> null
            }
        }.onFailure {
            platformLog("ParameterCodecRegistryV3", "encodeToSerialized failed: ${it.message}")
        }.getOrNull()
    }

    fun encodeAction(
        codecId: ParameterCodecIdV3,
        currentValue: ParameterTypedValueV3?,
        action: ParameterCodecActionV3
    ): ParameterEncodedActionV3? {
        return when (codecId) {
            ParameterCodecIdV3.SPINNER -> {
                val value = (action as? ParameterCodecActionV3.SetInt)?.value ?: return null
                ParameterEncodedActionV3.IntValue(value)
            }
            ParameterCodecIdV3.SLIDER -> {
                val value = (action as? ParameterCodecActionV3.SetInt)?.value ?: return null
                ParameterEncodedActionV3.IntValue(value)
            }
            ParameterCodecIdV3.TOGGLE -> {
                val value = (action as? ParameterCodecActionV3.SetInt)?.value ?: return null
                ParameterEncodedActionV3.IntValue(value)
            }
            ParameterCodecIdV3.EMG_GAINS -> {
                val set = action as? ParameterCodecActionV3.SetInt ?: return null
                val current = (currentValue as? ParameterTypedValueV3.EmgGains)?.value ?: EMGGainsV3()
                val open = if (set.dataOffset == 0) set.value else current.openGain
                val close = if (set.dataOffset == 1) set.value else current.closeGain
                ParameterEncodedActionV3.EmgGainsValue(openGain = open, closeGain = close)
            }
            ParameterCodecIdV3.SWITCHER -> {
                val checked = (action as? ParameterCodecActionV3.SetBoolean)?.checked ?: return null
                ParameterEncodedActionV3.IntValue(if (checked) 1 else 0)
            }
            ParameterCodecIdV3.TEXT -> {
                val text = (action as? ParameterCodecActionV3.SetText)?.text ?: return null
                ParameterEncodedActionV3.ByteArrayValue(text.encodeToByteArray())
            }
            else -> null
        }
    }
}

private fun ByteArrayView.u8(i: Int): Int = this[i].toInt() and 0xFF
