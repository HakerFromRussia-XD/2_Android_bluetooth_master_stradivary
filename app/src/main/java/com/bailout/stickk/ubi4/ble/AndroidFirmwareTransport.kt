package com.bailout.stickk.ubi4.ble

import android.util.Log
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL_CHARACTERISTIC
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.firmware.FirmwareCommandSender
import com.bailout.stickk.ubi4.firmware.FirmwareTransportChannel
import com.bailout.stickk.ubi4.firmware.FirmwareUpdateLogger
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main

object AndroidFirmwareCommandSender : FirmwareCommandSender {
    override suspend fun send(packet: ByteArray, channel: FirmwareTransportChannel) {
        val characteristic = when (channel) {
            FirmwareTransportChannel.UBI4_MAIN -> MAIN_CHANNEL_CHARACTERISTIC
            FirmwareTransportChannel.V3_SERIAL -> SERIALPORTCHAR_UUID
        }
        main?.bleCommandWithQueue(packet, characteristic, WRITE) {}
    }

    override suspend fun sendBootloaderJump(
        packet: ByteArray,
        channel: FirmwareTransportChannel
    ) {
        check(channel == FirmwareTransportChannel.V3_SERIAL)
        main?.firmwareJumpToBootloader(packet)
            ?: error("MainActivityUBI4 is unavailable for bootloader handoff")
    }
}

object AndroidFirmwareUpdateLogger : FirmwareUpdateLogger {
    override fun debug(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun info(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun warn(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        if (throwable == null) {
            Log.e(tag, message)
        } else {
            Log.e(tag, message, throwable)
        }
    }
}
