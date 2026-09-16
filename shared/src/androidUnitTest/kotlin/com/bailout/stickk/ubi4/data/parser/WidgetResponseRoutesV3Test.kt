package com.bailout.stickk.ubi4.data.parser

import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.DEVICE_INFORMATION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.PROSTHESIS_MODULE_CONTROL
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.DeviceInformationCommandV3.GET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_GESTURE_SETTING
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_PINCH_FINGER_POSITION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_PINCH_THUMB_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GLOBAL_THUMB_CLOSED_POSITION
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_SETTING
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_SERIAL_NUMBER
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class WidgetResponseRoutesV3Test {

    @Test
    fun `global closed positions should map to independent slider routes`() {
        val thumbRoute = WidgetResponseRoutesV3.find(
            command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
            responseSubcommand = PWCE_GET_PINCH_THUMB_POSITION.number.toInt()
        )
        val fingersRoute = WidgetResponseRoutesV3.find(
            command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
            responseSubcommand = PWCE_GET_PINCH_FINGER_POSITION.number.toInt()
        )

        assertNotNull(thumbRoute)
        assertNotNull(fingersRoute)
        assertEquals(P_KEY_GLOBAL_THUMB_CLOSED_POSITION, thumbRoute.parameterKey)
        assertEquals(P_KEY_GLOBAL_INDEX_MIDDLE_CLOSED_POSITION, fingersRoute.parameterKey)
        assertEquals(WidgetEmitTargetV3.SLIDER_FLOW, thumbRoute.emitTarget)
        assertEquals(WidgetEmitTargetV3.SLIDER_FLOW, fingersRoute.emitTarget)
    }

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

    @Test
    fun `serial number response should be stored without ui emit`() {
        val route = WidgetResponseRoutesV3.find(
            command = DEVICE_INFORMATION.number.toInt(),
            responseSubcommand = GET_SERIAL_NUMBER.number
        )

        assertNotNull(route)
        assertEquals(P_KEY_SET_SERIAL_NUMBER, route.parameterKey)
        assertEquals(WidgetEmitTargetV3.NO_UI, route.emitTarget)
    }
}
