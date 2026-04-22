package com.bailout.stickk.ubi4.data.parser

import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.PROSTHESIS_MODULE_CONTROL
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_GESTURE_SETTING
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_SETTING
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class WidgetResponseRoutesV3Test {

    @Test
    fun `gesture settings response should map to gesture settings event`() {
        val route = WidgetResponseRoutesV3.find(
            command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
            responseSubcommand = PWCE_GET_GESTURE_SETTING.number.toInt()
        )

        assertNotNull(route)
        assertEquals(P_KEY_GESTURE_SETTING, route.parameterKey)
        assertEquals(WidgetEmitTargetV3.GESTURE_SETTINGS_EVENT, route.emitTarget)
    }
}
