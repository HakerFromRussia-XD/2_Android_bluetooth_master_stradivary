package com.bailout.stickk.ubi4.versions.v3.domain.settings

import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_GAIN_CLOSE_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_GAIN_OPEN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MAX_GAIN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_FORCE_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_THUMB_CLOSED_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS

/** Device parameter limits shared by input presentation and write validation. */
object V3SliderSettingsRules {
    fun allowedRange(parameterKey: String): IntRange = when (parameterKey) {
        P_KEY_EMG_MAX_GAIN_VALUE -> 0..250
        P_KEY_SPEED_SETTINGS,
        P_KEY_FORCE_SETTINGS,
        P_KEY_EMG_GAIN_OPEN_VALUE,
        P_KEY_EMG_GAIN_CLOSE_VALUE,
        P_KEY_GLOBAL_THUMB_CLOSED_POSITION,
        P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION -> 0..100
        else -> throw IllegalArgumentException("Unsupported Slider parameter: $parameterKey")
    }
}
