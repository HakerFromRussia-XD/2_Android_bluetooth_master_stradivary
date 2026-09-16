package com.bailout.stickk.ubi4.models.device

import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_PINCH_FINGER_POSITION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_PINCH_THUMB_POSITION
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GlobalFingerPositionInitPolicyV3Test {
    @Test
    fun `only standard V3 requests both global finger positions during init`() {
        assertEquals(
            listOf(
                PWCE_GET_PINCH_THUMB_POSITION.number.toInt(),
                PWCE_GET_PINCH_FINGER_POSITION.number.toInt()
            ),
            GlobalFingerPositionInitPolicyV3.readSubcommands(V3DeviceProfile.STANDARD_V3)
        )
        assertTrue(GlobalFingerPositionInitPolicyV3.readSubcommands(V3DeviceProfile.INDY3).isEmpty())
        assertTrue(GlobalFingerPositionInitPolicyV3.readSubcommands(V3DeviceProfile.NOT_V3).isEmpty())
    }
}
