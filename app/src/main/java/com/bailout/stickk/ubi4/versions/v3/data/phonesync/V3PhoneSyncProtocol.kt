package com.bailout.stickk.ubi4.versions.v3.data.phonesync

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3
import com.bailout.stickk.ubi4.versions.v3.domain.phonesync.V3PhoneSyncCommand
import com.bailout.stickk.ubi4.versions.v3.domain.phonesync.V3PhoneSyncReply

object V3PhoneSyncProtocol {
    private val echo = BaseCommandsV3.ECHO_COMMAND.number.toInt()
    private val access = BaseCommandsV3.DEVICE_ACCESS_COMMAND.number.toInt()

    fun packet(command: V3PhoneSyncCommand): ByteArray = when (command) {
        V3PhoneSyncCommand.Echo -> BLECommandsV3.sendCommand(echo, 0, 0)
        V3PhoneSyncCommand.Check -> BLECommandsV3.sendCommand(access, 1, 0)
        V3PhoneSyncCommand.Start -> BLECommandsV3.sendCommand(access, 2, 0)
    }

    fun isControlPacket(bytes: ByteArray): Boolean = bytes.size >= 2 &&
        (bytes[1].toInt() and 0xff) in setOf(echo, access)

    fun decode(bytes: ByteArray): V3PhoneSyncReply? {
        if (bytes.size != 5 || bytes[0] != 0.toByte()) return null
        if (BLECommandsV3.calculationCRCRange(bytes, 0, 4) != (bytes[4].toInt() and 0xff)) return null
        val command = bytes[1].toInt() and 0xff
        val subcommand = bytes[2].toInt() and 0xff
        val value = bytes[3].toInt() and 0xff
        return when {
            command == echo && subcommand == 0 && value == 0 -> V3PhoneSyncReply.Echo
            command == access && subcommand == 1 && value == 0 -> V3PhoneSyncReply.NotBound
            command == access && subcommand == 1 && value == 1 -> V3PhoneSyncReply.Bound
            command == access && subcommand == 2 && value == 0 -> V3PhoneSyncReply.Failure
            command == access && subcommand == 2 && value == 1 -> V3PhoneSyncReply.Success
            command == access && subcommand == 2 && value == 2 -> V3PhoneSyncReply.Started
            else -> null
        }
    }
}
