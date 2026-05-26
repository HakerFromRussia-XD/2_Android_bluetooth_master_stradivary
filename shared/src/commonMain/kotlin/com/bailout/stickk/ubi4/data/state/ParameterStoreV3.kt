package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.models.ble.CurrentGestureV3
import com.bailout.stickk.ubi4.models.ble.EMGGainsV3
import com.bailout.stickk.ubi4.models.ble.GestureV3
import com.bailout.stickk.ubi4.models.ble.RotationGroupV3
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.models.ble.SpinnerV3
import com.bailout.stickk.ubi4.models.ble.SwitcherV3
import com.bailout.stickk.ubi4.models.ble.ThresholdsV3
import com.bailout.stickk.ubi4.models.ble.ToggleV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class ParameterStoreKeyV3(
    val parameterID: Int,
    val dataCode: Int,
    val deviceAddress: Int
)

sealed interface ParameterTypedValueV3 {
    data class Spinner(val value: SpinnerV3) : ParameterTypedValueV3
    data class Slider(val value: SliderV3) : ParameterTypedValueV3
    data class Toggle(val value: ToggleV3) : ParameterTypedValueV3
    data class EmgGains(val value: EMGGainsV3) : ParameterTypedValueV3
    data class Thresholds(val value: ThresholdsV3) : ParameterTypedValueV3
    data class CurrentGesture(val value: CurrentGestureV3) : ParameterTypedValueV3
    data class RotationGroup(val value: RotationGroupV3) : ParameterTypedValueV3
    data class GestureSettings(val value: GestureV3) : ParameterTypedValueV3
    data class Switcher(val value: SwitcherV3) : ParameterTypedValueV3
    data class Text(val value: String) : ParameterTypedValueV3
    data class UInt32(val value: Long) : ParameterTypedValueV3
}

object ParameterStoreV3 {
    private val _values =
        MutableStateFlow<Map<ParameterStoreKeyV3, ParameterTypedValueV3>>(emptyMap())
    private val _updates = MutableSharedFlow<ParameterStoreKeyV3>(
        replay = 0,
        extraBufferCapacity = 128
    )

    val values = _values.asStateFlow()
    val updates = _updates.asSharedFlow()

    fun toKey(parameterInfo: ParameterInfo<Int, Int, Int, Int>): ParameterStoreKeyV3 {
        return ParameterStoreKeyV3(
            parameterID = parameterInfo.parameterID,
            dataCode = parameterInfo.dataCode,
            deviceAddress = parameterInfo.deviceAddress
        )
    }

    fun put(
        parameterInfo: ParameterInfo<Int, Int, Int, Int>,
        typedValue: ParameterTypedValueV3
    ) {
        val key = toKey(parameterInfo)
        _values.value += (key to typedValue)
        _updates.tryEmit(key)
    }

    fun get(parameterInfo: ParameterInfo<Int, Int, Int, Int>): ParameterTypedValueV3? {
        return _values.value[toKey(parameterInfo)]
    }

    fun observe(parameterInfo: ParameterInfo<Int, Int, Int, Int>) =
        values.map { map -> map[toKey(parameterInfo)] }.distinctUntilChanged()

    fun clear() {
        _values.value = emptyMap()
    }
}
