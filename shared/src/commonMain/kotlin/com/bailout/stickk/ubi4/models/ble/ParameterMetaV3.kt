package com.bailout.stickk.ubi4.models.ble

import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo

enum class ParameterCodecIdV3 {
    NONE,
    SPINNER,
    SLIDER,
    TOGGLE,
    EMG_GAINS,
    THRESHOLDS,
    CURRENT_GESTURE,
    ROTATION_GROUP,
    GESTURE_SETTINGS,
    SWITCHER,
    TEXT,
    UINT32
}

enum class WidgetKindV3 {
    UNKNOWN,
    PLOT,
    SLIDER,
    TOGGLE_SLIDER,
    SPINNER,
    GESTURES,
    SWITCHER,
    BUTTONS,
    TEXT_INPUT,
    COMMAND
}

data class ParameterMetaV3(
    val parameterInfo: ParameterInfo<Int, Int, Int, Int>,
    val codecId: ParameterCodecIdV3 = ParameterCodecIdV3.NONE,
    val widgetKind: WidgetKindV3 = WidgetKindV3.UNKNOWN,
    val valuePath: String = ""
)
