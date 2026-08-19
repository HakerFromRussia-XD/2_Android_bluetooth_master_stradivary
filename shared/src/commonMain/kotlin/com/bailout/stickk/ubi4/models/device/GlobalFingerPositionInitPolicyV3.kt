package com.bailout.stickk.ubi4.models.device

import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_PINCH_FINGER_POSITION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_PINCH_THUMB_POSITION

/**
 * Shared platform policy for the two global closed-position parameters.
 * Firmware keeps the historical PINCH command names, while the app exposes
 * gesture-independent thumb and index/middle settings.
 */
object GlobalFingerPositionInitPolicyV3 {
    fun readSubcommands(profile: V3DeviceProfile): List<Int> =
        if (profile == V3DeviceProfile.STANDARD_V3) {
            listOf(
                PWCE_GET_PINCH_THUMB_POSITION.number.toInt(),
                PWCE_GET_PINCH_FINGER_POSITION.number.toInt()
            )
        } else {
            emptyList()
        }
}
