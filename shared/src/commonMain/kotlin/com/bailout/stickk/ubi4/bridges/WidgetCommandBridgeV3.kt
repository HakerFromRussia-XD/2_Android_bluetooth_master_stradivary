package com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.parser.ParameterCodecActionV3
import com.bailout.stickk.ubi4.data.parser.ParameterCodecRegistryV3
import com.bailout.stickk.ubi4.data.parser.ParameterEncodedActionV3
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.models.ble.ParameterCodecIdV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.DEVICE_INFORMATION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.EMG_MASTER_CONTROL
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.GUI_CONTROL
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.PROSTHESIS_MODULE_CONTROL
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.EmgMasterControlEnum.EMCE_GET_EMG_GAIN_VALUE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.EmgMasterControlEnum.EMCE_GET_EMG_MAX_GAIN_VALUE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.EmgMasterControlEnum.EMCE_GET_EMG_MODE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.EmgMasterControlEnum.EMCE_SET_EMG_GAIN_VALUE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.EmgMasterControlEnum.EMCE_SET_EMG_MAX_GAIN_VALUE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.EmgMasterControlEnum.EMCE_SET_EMG_MODE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GuiModuleControlEnum.GMCE_GET_LEFT_RIGHT_HAND
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GuiModuleControlEnum.GMCE_GET_SCREEN_TIMEOUT
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GuiModuleControlEnum.GMCE_SET_LEFT_RIGHT_HAND
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GuiModuleControlEnum.GMCE_SET_SCREEN_TIMEOUT
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_CURRENT_GESTURE_NUM
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_EMG_CHANGE_GESTURE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_EMG_MOVEMENT_LOCK
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_GESTURE_GROUPE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_HAND_CONTROL_MODE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_THRESHOLD_VALUE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_CURRENT_GESTURE_NUM
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_EMG_CHANGE_GESTURE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_EMG_MOVEMENT_LOCK
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_GESTURE_GROUPE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_HAND_CONTROL_MODE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_THRESHOLD_VALUE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.DeviceInformationCommandV3.GET_DEVICE_NAME
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.DeviceInformationCommandV3.GET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.DeviceInformationCommandV3.SET_DEVICE_NAME
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.DeviceInformationCommandV3.SET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_GESTURE_CHANGE_MODE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_GESTURE_CHANGE_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SETTINGS_PROFILE
import com.bailout.stickk.ubi4.utility.logging.platformLog

/**
 * Shared V3 command encoder for iOS/Android UI.
 * UI sends semantic actions, bridge encodes and builds BLE payload bytes.
 */
object WidgetCommandBridgeV3 {

    fun buildSendSubcommand(subcommand: Int, parameter: Int = 0): ByteArray {
        return BLECommandsV3.sendSubcommand(subcommand = subcommand, parameter = parameter)
    }

    fun buildRequest(parameterID: Int, dataCode: Int): ByteArray {
        return BLECommandsV3.requestWithCommand(command = parameterID, subcommand = dataCode)
    }

    // Android parity: map SET subcommands from widgets to corresponding GET requests.
    fun buildReadRequest(parameterID: Int, dataCode: Int): ByteArray? {
        val settingsProfileInfo = PreferenceKeysUbi4.ParameterInfoRegistry.require(P_KEY_SETTINGS_PROFILE)
        if (settingsProfileInfo.parameterID == parameterID && settingsProfileInfo.dataCode == dataCode) {
            return null
        }

        return when (parameterID) {
            PROSTHESIS_MODULE_CONTROL.number.toInt() -> {
                when (dataCode) {
                    PWCE_SET_THRESHOLD_VALUE.number.toInt() ->
                        BLECommandsV3.request(PWCE_GET_THRESHOLD_VALUE.number.toInt())
                    PWCE_SET_EMG_CHANGE_GESTURE.number.toInt() ->
                        BLECommandsV3.request(PWCE_GET_EMG_CHANGE_GESTURE.number.toInt())
                    PWCE_SET_EMG_MOVEMENT_LOCK.number.toInt() ->
                        BLECommandsV3.request(PWCE_GET_EMG_MOVEMENT_LOCK.number.toInt())
                    PWCE_SET_HAND_CONTROL_MODE.number.toInt() ->
                        BLECommandsV3.request(PWCE_GET_HAND_CONTROL_MODE.number.toInt())
                    PWCE_SET_CURRENT_GESTURE_NUM.number.toInt() ->
                        BLECommandsV3.request(PWCE_GET_CURRENT_GESTURE_NUM.number.toInt())
                    PWCE_SET_GESTURE_GROUPE.number.toInt() ->
                        BLECommandsV3.request(PWCE_GET_GESTURE_GROUPE.number.toInt())
                    PWCE_SET_GESTURE_CHANGE_MODE.number.toInt() ->
                        BLECommandsV3.request(PWCE_GET_GESTURE_CHANGE_MODE.number.toInt())
                    else ->
                        BLECommandsV3.requestWithCommand(command = parameterID, subcommand = dataCode)
                }
            }
            EMG_MASTER_CONTROL.number.toInt() -> {
                when (dataCode) {
                    EMCE_SET_EMG_GAIN_VALUE.number.toInt() ->
                        BLECommandsV3.requestWithCommand(
                            command = EMG_MASTER_CONTROL.number.toInt(),
                            subcommand = EMCE_GET_EMG_GAIN_VALUE.number.toInt()
                        )
                    EMCE_SET_EMG_MAX_GAIN_VALUE.number.toInt() ->
                        BLECommandsV3.requestWithCommand(
                            command = EMG_MASTER_CONTROL.number.toInt(),
                            subcommand = EMCE_GET_EMG_MAX_GAIN_VALUE.number.toInt()
                        )
                    EMCE_SET_EMG_MODE.number.toInt() ->
                        BLECommandsV3.requestWithCommand(
                            command = EMG_MASTER_CONTROL.number.toInt(),
                            subcommand = EMCE_GET_EMG_MODE.number.toInt()
                        )
                    else ->
                        BLECommandsV3.requestWithCommand(command = parameterID, subcommand = dataCode)
                }
            }
            GUI_CONTROL.number.toInt() -> {
                when (dataCode) {
                    GMCE_SET_SCREEN_TIMEOUT.number.toInt() ->
                        BLECommandsV3.requestWithCommand(
                            command = GUI_CONTROL.number.toInt(),
                            subcommand = GMCE_GET_SCREEN_TIMEOUT.number.toInt()
                        )
                    GMCE_SET_LEFT_RIGHT_HAND.number.toInt() ->
                        BLECommandsV3.requestWithCommand(
                            command = GUI_CONTROL.number.toInt(),
                            subcommand = GMCE_GET_LEFT_RIGHT_HAND.number.toInt()
                        )
                    else ->
                        BLECommandsV3.requestWithCommand(command = parameterID, subcommand = dataCode)
                }
            }
            DEVICE_INFORMATION.number.toInt() -> {
                when (dataCode) {
                    SET_DEVICE_NAME.number ->
                        BLECommandsV3.requestWithCommand(
                            command = DEVICE_INFORMATION.number.toInt(),
                            subcommand = GET_DEVICE_NAME.number
                        )
                    SET_SERIAL_NUMBER.number ->
                        BLECommandsV3.requestWithCommand(
                            command = DEVICE_INFORMATION.number.toInt(),
                            subcommand = GET_SERIAL_NUMBER.number
                        )
                    else ->
                        BLECommandsV3.requestWithCommand(command = parameterID, subcommand = dataCode)
                }
            }
            else -> BLECommandsV3.requestWithCommand(command = parameterID, subcommand = dataCode)
        }
    }

    fun buildSendThresholds(openThreshold: Int, closeThreshold: Int): ByteArray {
        return BLECommandsV3.sendThresholds(
            thresholdOpen = openThreshold,
            thresholdClose = closeThreshold
        )
    }

    fun buildSendEmgGains(openGain: Int, closeGain: Int): ByteArray {
        return BLECommandsV3.sendGaines(
            gainOpen = openGain,
            gainClose = closeGain
        )
    }

    fun buildSetInt(
        parameterID: Int,
        dataCode: Int,
        deviceAddress: Int,
        dataOffset: Int,
        value: Int
    ): ByteArray? {
        val parameterInfo = ParameterInfo(parameterID, dataCode, deviceAddress, dataOffset)
        return buildActionBytes(
            parameterInfo,
            ParameterCodecActionV3.SetInt(value = value, dataOffset = dataOffset)
        )
    }

    fun buildSetBoolean(
        parameterID: Int,
        dataCode: Int,
        deviceAddress: Int,
        checked: Boolean
    ): ByteArray? {
        val parameterInfo = ParameterInfo(parameterID, dataCode, deviceAddress, 0)
        return buildActionBytes(parameterInfo, ParameterCodecActionV3.SetBoolean(checked = checked))
    }

    fun buildSetText(
        parameterID: Int,
        dataCode: Int,
        deviceAddress: Int,
        text: String
    ): ByteArray? {
        val parameterInfo = ParameterInfo(parameterID, dataCode, deviceAddress, 0)
        return buildActionBytes(parameterInfo, ParameterCodecActionV3.SetText(text = text))
    }

    private fun buildActionBytes(
        parameterInfo: ParameterInfo<Int, Int, Int, Int>,
        action: ParameterCodecActionV3
    ): ByteArray? {
        val normalizedInfo = normalizeSetParameterInfo(parameterInfo)
        val parameterMeta = PreferenceKeysUbi4.ParameterInfoRegistry.getMeta(normalizedInfo)
            ?: run {
                platformLog(
                    "WidgetCommandBridgeV3",
                    "No parameterMeta for parameterInfo=$parameterInfo normalized=$normalizedInfo"
                )
                return null
            }

        val currentTypedValue = ParameterStoreV3.get(normalizedInfo)
            ?: ParameterStoreV3.get(parameterInfo)
        val encodedAction = ParameterCodecRegistryV3.encodeAction(
            codecId = parameterMeta.codecId,
            currentValue = currentTypedValue,
            action = action
        ) ?: run {
            platformLog(
                "WidgetCommandBridgeV3",
                "encodeAction failed for codec=${parameterMeta.codecId} parameterInfo=$parameterInfo"
            )
            return null
        }

        return encodedToPayload(
            parameterInfo = normalizedInfo,
            codecId = parameterMeta.codecId,
            encodedAction = encodedAction
        )
    }

    private fun normalizeSetParameterInfo(
        parameterInfo: ParameterInfo<Int, Int, Int, Int>
    ): ParameterInfo<Int, Int, Int, Int> {
        val normalizedDataCode = when (parameterInfo.parameterID) {
            PROSTHESIS_MODULE_CONTROL.number.toInt() -> when (parameterInfo.dataCode) {
                PWCE_GET_THRESHOLD_VALUE.number.toInt() -> PWCE_SET_THRESHOLD_VALUE.number.toInt()
                PWCE_GET_EMG_CHANGE_GESTURE.number.toInt() -> PWCE_SET_EMG_CHANGE_GESTURE.number.toInt()
                PWCE_GET_EMG_MOVEMENT_LOCK.number.toInt() -> PWCE_SET_EMG_MOVEMENT_LOCK.number.toInt()
                PWCE_GET_HAND_CONTROL_MODE.number.toInt() -> PWCE_SET_HAND_CONTROL_MODE.number.toInt()
                PWCE_GET_CURRENT_GESTURE_NUM.number.toInt() -> PWCE_SET_CURRENT_GESTURE_NUM.number.toInt()
                PWCE_GET_GESTURE_GROUPE.number.toInt() -> PWCE_SET_GESTURE_GROUPE.number.toInt()
                PWCE_GET_GESTURE_CHANGE_MODE.number.toInt() -> PWCE_SET_GESTURE_CHANGE_MODE.number.toInt()
                else -> parameterInfo.dataCode
            }
            EMG_MASTER_CONTROL.number.toInt() -> when (parameterInfo.dataCode) {
                EMCE_GET_EMG_GAIN_VALUE.number.toInt() -> EMCE_SET_EMG_GAIN_VALUE.number.toInt()
                EMCE_GET_EMG_MAX_GAIN_VALUE.number.toInt() -> EMCE_SET_EMG_MAX_GAIN_VALUE.number.toInt()
                EMCE_GET_EMG_MODE.number.toInt() -> EMCE_SET_EMG_MODE.number.toInt()
                else -> parameterInfo.dataCode
            }
            GUI_CONTROL.number.toInt() -> when (parameterInfo.dataCode) {
                GMCE_GET_SCREEN_TIMEOUT.number.toInt() -> GMCE_SET_SCREEN_TIMEOUT.number.toInt()
                GMCE_GET_LEFT_RIGHT_HAND.number.toInt() -> GMCE_SET_LEFT_RIGHT_HAND.number.toInt()
                else -> parameterInfo.dataCode
            }
            DEVICE_INFORMATION.number.toInt() -> when (parameterInfo.dataCode) {
                GET_DEVICE_NAME.number -> SET_DEVICE_NAME.number
                GET_SERIAL_NUMBER.number -> SET_SERIAL_NUMBER.number
                else -> parameterInfo.dataCode
            }
            else -> parameterInfo.dataCode
        }

        return if (normalizedDataCode == parameterInfo.dataCode) {
            parameterInfo
        } else {
            ParameterInfo(
                parameterInfo.parameterID,
                normalizedDataCode,
                parameterInfo.deviceAddress,
                parameterInfo.dataOffsets
            )
        }
    }

    private fun encodedToPayload(
        parameterInfo: ParameterInfo<Int, Int, Int, Int>,
        codecId: ParameterCodecIdV3,
        encodedAction: ParameterEncodedActionV3
    ): ByteArray {
        return when (encodedAction) {
            is ParameterEncodedActionV3.IntValue -> {
                BLECommandsV3.sendCommand(
                    command = parameterInfo.parameterID,
                    subcommand = parameterInfo.dataCode,
                    parameter = encodedAction.value
                )
            }
            is ParameterEncodedActionV3.ByteArrayValue -> {
                val data = if (codecId == ParameterCodecIdV3.TEXT) {
                    encodedAction.value + byteArrayOf(0x00)
                } else {
                    encodedAction.value
                }
                BLECommandsV3.sendLongCommand(
                    command = parameterInfo.parameterID,
                    subcommand = parameterInfo.dataCode,
                    data = data
                )
            }
            is ParameterEncodedActionV3.EmgGainsValue -> {
                BLECommandsV3.sendGaines(
                    gainOpen = encodedAction.openGain,
                    gainClose = encodedAction.closeGain
                )
            }
        }
    }
}
