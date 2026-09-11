package com.bailout.stickk.ubi4.versions.v3.domain.settings

import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_HAND_CONTROL_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_CHANGE_MODE

object V3SpinnerSettingsRules {
    fun allowedValues(parameterKey: String): IntRange = when (parameterKey) {
        // Normal, sport, smooth force, smooth speed, smooth force and speed.
        P_KEY_HAND_CONTROL_MODE -> 0..4
        // No action or move to open position.
        P_KEY_GESTURE_CHANGE_MODE -> 0..1
        else -> throw IllegalArgumentException("Unsupported Spinner parameter: $parameterKey")
    }
}
