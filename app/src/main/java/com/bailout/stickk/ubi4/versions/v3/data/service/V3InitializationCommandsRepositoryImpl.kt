package com.bailout.stickk.ubi4.versions.v3.data.service

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.DeviceInformationCommandV3.GET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.EmgMasterControlEnum.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GuiModuleControlEnum.*
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.*
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3InitializationCommand
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3InitializationCommand.*
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3InitializationCommandPacket
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3InitializationCommandsRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import java.util.Calendar

class V3InitializationCommandsRepositoryImpl(
    private val enqueuePacket: (ByteArray) -> Unit,
    private val currentDateTime: () -> Calendar = { Calendar.getInstance() },
) : V3InitializationCommandsRepository {
    override val availability = merge(BLEState.state.map { Unit }, UiState.updateFlow.map { Unit })
        .onStart { emit(Unit) }.map { isAvailable() }.distinctUntilChanged()

    override fun getCommands() = V3InitializationCommand.entries.map { describe(it, buildPacket(it)) }

    override fun enqueue(deviceAddress: String, command: V3InitializationCommand): V3InitializationCommandPacket? {
        if (!isAvailable() || deviceAddress != WidgetRepoProvider.mac()) return null
        val packet = buildPacket(command)
        enqueuePacket(packet)
        return describe(command, packet)
    }

    private fun isAvailable() = UiState.isInterfaceV3Activated && BLEState.state.value == BLEState.State.READY &&
        WidgetRepoProvider.mac().let { it.isNotBlank() && it != "null" }

    private fun describe(command: V3InitializationCommand, packet: ByteArray) =
        V3InitializationCommandPacket(command, packet.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) })

    private fun buildPacket(command: V3InitializationCommand): ByteArray = when (command) {
        DEVICE_DATA -> BLECommandsV3.requestDeviceData()
        PHONE_DATE_TIME -> currentDateTime().let { calendar ->
            BLECommandsV3.sendDateTime(
                year = calendar.get(Calendar.YEAR), month = calendar.get(Calendar.MONTH) + 1,
                day = calendar.get(Calendar.DAY_OF_MONTH), weekDay = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7,
                hour = calendar.get(Calendar.HOUR_OF_DAY), minute = calendar.get(Calendar.MINUTE), second = calendar.get(Calendar.SECOND),
            )
        }
        THRESHOLD -> BLECommandsV3.request(PWCE_GET_THRESHOLD_VALUE.number.toInt())
        EMG_GAIN -> BLECommandsV3.requestWithCommand(EMG_MASTER_CONTROL.number.toInt(), EMCE_GET_EMG_GAIN_VALUE.number.toInt())
        EMG_MODE -> BLECommandsV3.requestWithCommand(EMG_MASTER_CONTROL.number.toInt(), EMCE_GET_EMG_MODE.number.toInt())
        EMG_MAX_GAIN -> BLECommandsV3.requestWithCommand(EMG_MASTER_CONTROL.number.toInt(), EMCE_GET_EMG_MAX_GAIN_VALUE.number.toInt())
        EMG_CHANGE_GESTURE -> BLECommandsV3.request(PWCE_GET_EMG_CHANGE_GESTURE.number.toInt())
        SCREEN_TIMEOUT -> BLECommandsV3.requestWithCommand(GUI_CONTROL.number.toInt(), GMCE_GET_SCREEN_TIMEOUT.number.toInt())
        MOVEMENT_LOCK -> BLECommandsV3.request(PWCE_GET_EMG_MOVEMENT_LOCK.number.toInt())
        HAND_SIDE -> BLECommandsV3.requestWithCommand(GUI_CONTROL.number.toInt(), GMCE_GET_LEFT_RIGHT_HAND.number.toInt())
        BATTERY -> BLECommandsV3.requestWithCommand(GUI_CONTROL.number.toInt(), GMCE_GET_BATTERY.number.toInt())
        HAND_CONTROL_MODE -> BLECommandsV3.request(PWCE_GET_HAND_CONTROL_MODE.number.toInt())
        GESTURE_CHANGE_MODE -> BLECommandsV3.request(PWCE_GET_GESTURE_CHANGE_MODE.number.toInt())
        SPEED -> BLECommandsV3.request(PWCE_GET_SPEED_SETTINGS.number.toInt())
        FORCE -> BLECommandsV3.request(PWCE_GET_FORCE_SETTINGS.number.toInt())
        SERIAL_NUMBER -> BLECommandsV3.requestWithCommand(DEVICE_INFORMATION.number.toInt(), GET_SERIAL_NUMBER.number)
        THUMB_POSITION -> BLECommandsV3.request(PWCE_GET_PINCH_THUMB_POSITION.number.toInt())
        FINGERS_POSITION -> BLECommandsV3.request(PWCE_GET_PINCH_FINGER_POSITION.number.toInt())
    }
}
