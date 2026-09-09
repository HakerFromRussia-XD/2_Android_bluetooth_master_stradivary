package com.bailout.stickk.ubi4.versions.v3.domain.settings

import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MOVEMENT_LOCK
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_CHANGE_GESTURE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SCREEN_TIMEOUT

object V3ToggleSliderSettingsRules {
    fun allowedTimeRange(parameterKey: String): IntRange {
        require(parameterKey in setOf(P_KEY_EMG_CHANGE_GESTURE, P_KEY_EMG_MOVEMENT_LOCK, P_KEY_SCREEN_TIMEOUT)) {
            "Unsupported ToggleSlider parameter: $parameterKey"
        }
        return 10..100
    }
}
