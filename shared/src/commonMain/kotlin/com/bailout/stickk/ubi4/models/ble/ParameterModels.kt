package com.bailout.stickk.ubi4.models.ble

import com.bailout.stickk.ubi4.data.local.RotationGroupSerializer
import kotlinx.serialization.Serializable


data class ParameterRef(
    val addressDevice: Int,
    val parameterID: Int,
    val dataCode: Int
)
data class ParameterRefV3(
    val parameterID: Int,
    val dataCode: Int
)

data class PlotParameterRef(
    val addressDevice: Int,
    val parameterID: Int,
    val dataPlots: ArrayList<Int>
)
@Serializable
data class ThresholdsV3(
    var openThreshold: Int = 0,
    var closeThreshold: Int = 0
)
@Serializable
data class EMGGainsV3(
    var openGain: Int = 0,
    var closeGain: Int = 0
)
@Serializable
data class SwitcherV3(
    var checked: Boolean = false,
)
@Serializable
data class CurrentGestureV3(
    var currentGesture: Int = 0
)
@Serializable
data class GestureV3(
    var gestureId: Int = 0,

    var openPosition1: Int = 0,
    var openPosition2: Int = 0,
    var openPosition3: Int = 0,
    var openPosition4: Int = 0,
    var openPosition5: Int = 0,
    var openPosition6: Int = 0,

    var closePosition1: Int = 0,
    var closePosition2: Int = 0,
    var closePosition3: Int = 0,
    var closePosition4: Int = 0,
    var closePosition5: Int = 0,
    var closePosition6: Int = 0,

    var openToCloseTimeShift1: Int = 0,
    var openToCloseTimeShift2: Int = 0,
    var openToCloseTimeShift3: Int = 0,
    var openToCloseTimeShift4: Int = 0,
    var openToCloseTimeShift5: Int = 0,
    var openToCloseTimeShift6: Int = 0,

    var closeToOpenTimeShift1: Int = 0,
    var closeToOpenTimeShift2: Int = 0,
    var closeToOpenTimeShift3: Int = 0,
    var closeToOpenTimeShift4: Int = 0,
    var closeToOpenTimeShift5: Int = 0,
    var closeToOpenTimeShift6: Int = 0
)

@Serializable
data class RotationGroupV3 (
    var gesture1Id: Int = 0,
    var gesture1ImageId: Int = 0,
    var gesture2Id: Int = 0,
    var gesture2ImageId: Int = 0,
    var gesture3Id: Int = 0,
    var gesture3ImageId: Int = 0,
    var gesture4Id: Int = 0,
    var gesture4ImageId: Int = 0,
    var gesture5Id: Int = 0,
    var gesture5ImageId: Int = 0,
    var gesture6Id: Int = 0,
    var gesture6ImageId: Int = 0,
    var gesture7Id: Int = 0,
    var gesture7ImageId: Int = 0,
    var gesture8Id: Int = 0,
    var gesture8ImageId: Int = 0,
) {
    fun toGestureList(): List<Pair<Int, Int>> {
        return listOf(
            gesture1Id to gesture1ImageId,
            gesture2Id to gesture2ImageId,
            gesture3Id to gesture3ImageId,
            gesture4Id to gesture4ImageId,
            gesture5Id to gesture5ImageId,
            gesture6Id to gesture6ImageId,
            gesture7Id to gesture7ImageId,
            gesture8Id to gesture8ImageId
        )
    }
}