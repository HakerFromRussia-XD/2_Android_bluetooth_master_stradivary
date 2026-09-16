package com.bailout.stickk.ubi4.testing

import android.os.Handler
import android.os.Looper
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.local.BindingGestureGroup
import com.bailout.stickk.ubi4.data.parser.BLEParserV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.PROSTHESIS_MODULE_CONTROL
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_BINDING_DATA
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_CURRENT_GESTURE_NUM
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_GESTURE_GROUPE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_PINCH_FINGER_POSITION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_PINCH_THUMB_POSITION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_BINDING_DATA
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_CURRENT_GESTURE_NUM
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_GESTURE_GROUPE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_PINCH_FINGER_POSITION
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_PINCH_THUMB_POSITION
import java.util.Collections

object V3BleEmulatorTestHooks {
    const val EXTRA_ENABLED = "com.bailout.stickk.ubi4.testing.V3_BLE_EMULATOR_ENABLED"
    const val EXTRA_OPEN_GESTURES = "com.bailout.stickk.ubi4.testing.V3_BLE_EMULATOR_OPEN_GESTURES"

    @Volatile
    private var enabled = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val outgoingPackets = Collections.synchronizedList(mutableListOf<ByteArray>())

    @Volatile
    private var activeGesture = 1

    @Volatile
    private var bindingGroup = defaultBindingGroup()

    @Volatile
    private var thumbClosedPosition = DEFAULT_THUMB_CLOSED_POSITION

    @Volatile
    private var indexMiddleClosedPosition = DEFAULT_INDEX_MIDDLE_CLOSED_POSITION

    fun enable() {
        enabled = true
    }

    fun disable() {
        enabled = false
        reset()
    }

    fun reset() {
        synchronized(outgoingPackets) {
            outgoingPackets.clear()
        }
        activeGesture = 1
        bindingGroup = defaultBindingGroup()
        thumbClosedPosition = DEFAULT_THUMB_CLOSED_POSITION
        indexMiddleClosedPosition = DEFAULT_INDEX_MIDDLE_CLOSED_POSITION
    }

    fun isEnabled(): Boolean = enabled

    fun outgoingPacketsSnapshot(): List<ByteArray> =
        synchronized(outgoingPackets) {
            outgoingPackets.map { it.copyOf() }
        }

    fun hasOutgoingSubcommand(subcommand: Int): Boolean =
        outgoingPacketsSnapshot().any { parsePacket(it)?.subcommand == subcommand }

    fun bindingPairsSnapshot(): List<Pair<Int, Int>> = bindingGroup.toGestureList()

    fun injectInitialResponses(parser: BLEParserV3?) {
        if (!enabled || parser == null) return
        mainHandler.post {
            responsePacketsFor(
                command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
                subcommand = PWCE_GET_CURRENT_GESTURE_NUM.number.toInt(),
                data = byteArrayOf(activeGesture.toByte())
            ).forEach(parser::parseReceivedData)
            responsePacketsFor(
                command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
                subcommand = PWCE_GET_GESTURE_GROUPE.number.toInt(),
                data = rotationGroupPayload()
            ).forEach(parser::parseReceivedData)
            responsePacketsFor(
                command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
                subcommand = PWCE_GET_BINDING_DATA.number.toInt(),
                data = bindingGroup.toPayloadBytes()
            ).forEach(parser::parseReceivedData)
            responsePacketsFor(
                command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
                subcommand = PWCE_GET_PINCH_THUMB_POSITION.number.toInt(),
                data = byteArrayOf(thumbClosedPosition.toByte())
            ).forEach(parser::parseReceivedData)
            responsePacketsFor(
                command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
                subcommand = PWCE_GET_PINCH_FINGER_POSITION.number.toInt(),
                data = byteArrayOf(indexMiddleClosedPosition.toByte())
            ).forEach(parser::parseReceivedData)
        }
    }

    fun tryHandleOutgoing(
        byteArray: ByteArray?,
        parser: BLEParserV3?,
        onChunkSent: () -> Unit
    ): Boolean {
        if (!enabled || byteArray == null) return false

        synchronized(outgoingPackets) {
            outgoingPackets.add(byteArray.copyOf())
        }

        val parsed = parsePacket(byteArray)
        val responses = parsed?.let { packet ->
            responsePacketsFor(packet.command, packet.subcommand, packet.data)
        }.orEmpty()

        mainHandler.postDelayed({
            responses.forEach { response -> parser?.parseReceivedData(response) }
            onChunkSent()
        }, 60L)
        return true
    }

    private fun responsePacketsFor(command: Int, subcommand: Int, data: ByteArray): List<ByteArray> {
        if (command != PROSTHESIS_MODULE_CONTROL.number.toInt()) return emptyList()

        return when (subcommand) {
            PWCE_GET_CURRENT_GESTURE_NUM.number.toInt() ->
                listOf(BLECommandsV3.sendSubcommand(subcommand, activeGesture))

            PWCE_SET_CURRENT_GESTURE_NUM.number.toInt() -> {
                activeGesture = data.firstOrNull()?.toInt()?.and(0xFF) ?: activeGesture
                listOf(
                    BLECommandsV3.sendSubcommand(
                        PWCE_GET_CURRENT_GESTURE_NUM.number.toInt(),
                        activeGesture
                    )
                )
            }

            PWCE_GET_GESTURE_GROUPE.number.toInt() ->
                listOf(
                    BLECommandsV3.sendLongCommand(
                        PROSTHESIS_MODULE_CONTROL.number.toInt(),
                        subcommand,
                        rotationGroupPayload()
                    )
                )

            PWCE_SET_GESTURE_GROUPE.number.toInt() ->
                listOf(
                    BLECommandsV3.sendLongCommand(
                        PROSTHESIS_MODULE_CONTROL.number.toInt(),
                        PWCE_GET_GESTURE_GROUPE.number.toInt(),
                        rotationGroupPayload()
                    )
                )

            PWCE_GET_BINDING_DATA.number.toInt() ->
                listOf(
                    BLECommandsV3.sendLongCommand(
                        PROSTHESIS_MODULE_CONTROL.number.toInt(),
                        subcommand,
                        bindingGroup.toPayloadBytes()
                    )
                )

            PWCE_SET_BINDING_DATA.number.toInt() -> {
                bindingGroup = BindingGestureGroup.fromPayloadBytes(data)
                listOf(
                    BLECommandsV3.sendLongCommand(
                        PROSTHESIS_MODULE_CONTROL.number.toInt(),
                        PWCE_GET_BINDING_DATA.number.toInt(),
                        bindingGroup.toPayloadBytes()
                    )
                )
            }

            PWCE_GET_PINCH_THUMB_POSITION.number.toInt() ->
                listOf(BLECommandsV3.sendSubcommand(subcommand, thumbClosedPosition))

            PWCE_SET_PINCH_THUMB_POSITION.number.toInt() -> {
                thumbClosedPosition = data.scalarPositionOr(thumbClosedPosition)
                listOf(
                    BLECommandsV3.sendSubcommand(
                        PWCE_GET_PINCH_THUMB_POSITION.number.toInt(),
                        thumbClosedPosition
                    )
                )
            }

            PWCE_GET_PINCH_FINGER_POSITION.number.toInt() ->
                listOf(BLECommandsV3.sendSubcommand(subcommand, indexMiddleClosedPosition))

            PWCE_SET_PINCH_FINGER_POSITION.number.toInt() -> {
                indexMiddleClosedPosition = data.scalarPositionOr(indexMiddleClosedPosition)
                listOf(
                    BLECommandsV3.sendSubcommand(
                        PWCE_GET_PINCH_FINGER_POSITION.number.toInt(),
                        indexMiddleClosedPosition
                    )
                )
            }

            else -> emptyList()
        }
    }

    private fun parsePacket(packet: ByteArray): OutgoingPacket? {
        if (packet.size < 5) return null
        val command = packet[1].toInt() and 0xFF
        val isLong = (packet[0].toInt() and 0x80) != 0
        return if (isLong) {
            val payloadSize = (packet[2].toInt() and 0xFF) or ((packet[3].toInt() and 0xFF) shl 8)
            if (packet.size < 5 + payloadSize) return null
            val payload = packet.copyOfRange(5, 5 + payloadSize)
            if (payload.isEmpty()) return null
            OutgoingPacket(
                command = command,
                subcommand = payload[0].toInt() and 0xFF,
                data = payload.drop(1).toByteArray()
            )
        } else {
            OutgoingPacket(
                command = command,
                subcommand = packet[2].toInt() and 0xFF,
                data = byteArrayOf(packet[3])
            )
        }
    }

    private fun rotationGroupPayload(): ByteArray =
        byteArrayOf(
            1, 1,
            2, 2,
            3, 3,
            4, 4,
            5, 5,
            6, 6,
            7, 7,
            8, 8
        )

    private fun defaultBindingGroup(): BindingGestureGroup =
        BindingGestureGroup().apply {
            (1..BindingGestureGroup.PAIR_COUNT).forEach { index ->
                val gestureId = ((index - 1) % 8) + 1
                setGestureAt(index - 1, index to gestureId)
            }
        }

    private data class OutgoingPacket(
        val command: Int,
        val subcommand: Int,
        val data: ByteArray
    )

    private fun ByteArray.scalarPositionOr(fallback: Int): Int =
        firstOrNull()?.toInt()?.and(0xFF)?.coerceIn(0, 100) ?: fallback

    private const val DEFAULT_THUMB_CLOSED_POSITION = 50
    private const val DEFAULT_INDEX_MIDDLE_CLOSED_POSITION = 50
}
