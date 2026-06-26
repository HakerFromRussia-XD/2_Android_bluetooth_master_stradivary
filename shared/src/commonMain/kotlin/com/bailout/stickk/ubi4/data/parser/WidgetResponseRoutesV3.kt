package com.bailout.stickk.ubi4.data.parser

import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.DeviceInformationCommandV3.GET_DEVICE_ROLE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.DeviceInformationCommandV3.GET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.EmgMasterControlEnum.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GuiModuleControlEnum.*
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_CURRENT_GESTURE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_CHANGE_GESTURE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_CONTROL_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MAX_GAIN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_GAIN_OPEN_VALUE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MOVEMENT_LOCK
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_GROUPE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_CHANGE_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_SETTING
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_HAND_CONTROL_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_LEFT_RIGHT_HAND
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_OPEN_CLOSE_THRESHOLD
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SCREEN_TIMEOUT
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_DEVICE_ROLE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_FORCE_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_BINDING_DATA

data class WidgetResponseRouteV3(
    val command: Int,
    val responseSubcommand: Int,
    val parameterKey: String,
    val emitTarget: WidgetEmitTargetV3
)

enum class WidgetEmitTargetV3 {
    SPINNER_FLOW,
    SLIDER_FLOW,
    THRESHOLD_FLOW,
    CURRENT_GESTURE_FLOW,
    GESTURE_GROUP_FLOW,
    BINDING_GROUP_FLOW,
    GESTURE_SETTINGS_EVENT,
    NO_UI
}

object WidgetResponseRoutesV3 {
    // [new widgets V3] тут добавляем маршрут нового ответа: command/getSubcommand -> parameterKey/emitTarget
    private val routes: List<WidgetResponseRouteV3> = listOf(
        WidgetResponseRouteV3(
            command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
            responseSubcommand = PWCE_GET_HAND_CONTROL_MODE.number.toInt(),
            parameterKey = P_KEY_HAND_CONTROL_MODE,
            emitTarget = WidgetEmitTargetV3.SPINNER_FLOW
        ),
        WidgetResponseRouteV3(
            command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
            responseSubcommand = PWCE_GET_GESTURE_CHANGE_MODE.number.toInt(),
            parameterKey = P_KEY_GESTURE_CHANGE_MODE,
            emitTarget = WidgetEmitTargetV3.SPINNER_FLOW
        ),
        WidgetResponseRouteV3(
            command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
            responseSubcommand = PWCE_GET_EMG_MOVEMENT_LOCK.number.toInt(),
            parameterKey = P_KEY_EMG_MOVEMENT_LOCK,
            emitTarget = WidgetEmitTargetV3.SLIDER_FLOW
        ),
        WidgetResponseRouteV3(
            command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
            responseSubcommand = PWCE_GET_EMG_CHANGE_GESTURE.number.toInt(),
            parameterKey = P_KEY_EMG_CHANGE_GESTURE,
            emitTarget = WidgetEmitTargetV3.SLIDER_FLOW
        ),
        WidgetResponseRouteV3(
            command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
            responseSubcommand = PWCE_GET_GESTURE_GROUPE.number.toInt(),
            parameterKey = P_KEY_GESTURE_GROUPE,
            emitTarget = WidgetEmitTargetV3.GESTURE_GROUP_FLOW
        ),
        WidgetResponseRouteV3(
            command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
            responseSubcommand = PWCE_GET_BINDING_DATA.number.toInt(),
            parameterKey = P_KEY_BINDING_DATA,
            emitTarget = WidgetEmitTargetV3.BINDING_GROUP_FLOW
        ),
        WidgetResponseRouteV3(
            command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
            responseSubcommand = PWCE_GET_CURRENT_GESTURE_NUM.number.toInt(),
            parameterKey = P_KEY_CURRENT_GESTURE,
            emitTarget = WidgetEmitTargetV3.CURRENT_GESTURE_FLOW
        ),

        WidgetResponseRouteV3(
            command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
            responseSubcommand = PWCE_GET_GESTURE_SETTING.number.toInt(),
            parameterKey = P_KEY_GESTURE_SETTING,
            emitTarget = WidgetEmitTargetV3.GESTURE_SETTINGS_EVENT
        ),
        WidgetResponseRouteV3(
            command = EMG_MASTER_CONTROL.number.toInt(),
            responseSubcommand = EMCE_GET_EMG_GAIN_VALUE.number.toInt(),
            parameterKey = P_KEY_EMG_GAIN_OPEN_VALUE,
            emitTarget = WidgetEmitTargetV3.SLIDER_FLOW
        ),
        WidgetResponseRouteV3(
            command = EMG_MASTER_CONTROL.number.toInt(),
            responseSubcommand = EMCE_GET_EMG_MAX_GAIN_VALUE.number.toInt(),
            parameterKey = P_KEY_EMG_MAX_GAIN_VALUE,
            emitTarget = WidgetEmitTargetV3.SLIDER_FLOW
        ),
        WidgetResponseRouteV3(
            command = EMG_MASTER_CONTROL.number.toInt(),
            responseSubcommand = EMCE_GET_EMG_MODE.number.toInt(),
            parameterKey = P_KEY_EMG_CONTROL_MODE,
            emitTarget = WidgetEmitTargetV3.SPINNER_FLOW
        ),
        WidgetResponseRouteV3(
            command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
            responseSubcommand = PWCE_GET_THRESHOLD_VALUE.number.toInt(),
            parameterKey = P_KEY_OPEN_CLOSE_THRESHOLD,
            emitTarget = WidgetEmitTargetV3.THRESHOLD_FLOW
        ),
        WidgetResponseRouteV3(
            command = GUI_CONTROL.number.toInt(),
            responseSubcommand = GMCE_GET_SCREEN_TIMEOUT.number.toInt(),
            parameterKey = P_KEY_SCREEN_TIMEOUT,
            emitTarget = WidgetEmitTargetV3.SLIDER_FLOW
        ),
        WidgetResponseRouteV3(
            command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
            responseSubcommand = PWCE_GET_SPEED_SETTINGS.number.toInt(),
            parameterKey = P_KEY_SPEED_SETTINGS,
            emitTarget = WidgetEmitTargetV3.SLIDER_FLOW
        ),
        WidgetResponseRouteV3(
            command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
            responseSubcommand = PWCE_GET_FORCE_SETTINGS.number.toInt(),
            parameterKey = P_KEY_FORCE_SETTINGS,
            emitTarget = WidgetEmitTargetV3.SLIDER_FLOW
        ),
        WidgetResponseRouteV3(
            command = GUI_CONTROL.number.toInt(),
            responseSubcommand = GMCE_GET_LEFT_RIGHT_HAND.number.toInt(),
            parameterKey = P_KEY_LEFT_RIGHT_HAND,
            emitTarget = WidgetEmitTargetV3.SPINNER_FLOW
        ),
        WidgetResponseRouteV3(
            command = DEVICE_INFORMATION.number.toInt(),
            responseSubcommand = GET_SERIAL_NUMBER.number,
            parameterKey = P_KEY_SET_SERIAL_NUMBER,
            emitTarget = WidgetEmitTargetV3.NO_UI
        ),
        WidgetResponseRouteV3(
            command = DEVICE_INFORMATION.number.toInt(),
            responseSubcommand = GET_DEVICE_ROLE.number,
            parameterKey = P_KEY_DEVICE_ROLE,
            emitTarget = WidgetEmitTargetV3.SPINNER_FLOW
        ),
    )

    fun find(command: Int, responseSubcommand: Int): WidgetResponseRouteV3? {
        return routes.firstOrNull {
            it.command == command && it.responseSubcommand == responseSubcommand
        }
    }
}
