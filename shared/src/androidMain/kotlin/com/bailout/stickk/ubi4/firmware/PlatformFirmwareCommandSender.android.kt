package com.bailout.stickk.ubi4.firmware

import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL_CHARACTERISTIC
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.ble.BleEnvironment

actual object PlatformFirmwareCommandSender : FirmwareCommandSender {
    override suspend fun send(packet: ByteArray, channel: FirmwareTransportChannel) {
        val characteristic = when (channel) {
            FirmwareTransportChannel.UBI4_MAIN -> MAIN_CHANNEL_CHARACTERISTIC
            FirmwareTransportChannel.V3_SERIAL -> SERIALPORTCHAR_UUID
        }
        BleEnvironment.getBleCommandExecutor().bleCommandWithQueue(packet, characteristic, WRITE) {}
    }
}
