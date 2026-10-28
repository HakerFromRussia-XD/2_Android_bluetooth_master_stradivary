package com.bailout.stickk.ubi4.versions.v3.domain.service

import kotlinx.coroutines.flow.Flow

/** Unique outgoing commands, in the order of the captured STANDARD_V3 initialization. */
enum class V3InitializationCommand {
    DEVICE_DATA, PHONE_DATE_TIME, THRESHOLD, EMG_GAIN, EMG_MODE, EMG_MAX_GAIN,
    EMG_CHANGE_GESTURE, SCREEN_TIMEOUT, MOVEMENT_LOCK, HAND_SIDE, BATTERY,
    HAND_CONTROL_MODE, GESTURE_CHANGE_MODE, SPEED, FORCE, SERIAL_NUMBER,
    THUMB_POSITION, FINGERS_POSITION,
}

data class V3InitializationCommandPacket(val command: V3InitializationCommand, val hex: String)

interface V3InitializationCommandsRepository {
    val availability: Flow<Boolean>
    fun getCommands(): List<V3InitializationCommandPacket>
    /** Enqueues exactly once. Returns the actual packet, or null if the device is unavailable. */
    fun enqueue(deviceAddress: String, command: V3InitializationCommand): V3InitializationCommandPacket?
}
