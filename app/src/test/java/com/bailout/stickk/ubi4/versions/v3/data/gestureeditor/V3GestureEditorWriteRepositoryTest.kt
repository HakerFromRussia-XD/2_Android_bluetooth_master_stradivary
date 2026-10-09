package com.bailout.stickk.ubi4.versions.v3.data.gestureeditor

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.models.ble.GestureV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.gestures.GestureWithAddress
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_SETTING
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.*
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.V3GestureSettings
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.V3GestureCommand
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.WriteGestureSettingsUseCaseV3

class V3GestureEditorWriteRepositoryTest {
    private val snapshot = ParameterStoreV3.values.value
    private val base = ParameterInfoRegistry.require(P_KEY_GESTURE_SETTING)
    private val info = ParameterInfo(base.parameterID, base.dataCode, base.deviceAddress, 7)
    private val settings = V3GestureSettings(7, listOf(-1,2,3,4,5,120), listOf(11,12,13,14,15,16),
        listOf(21,22,23,24,25,26), listOf(31,32,33,34,35,36))
    private val expectedValue = ParameterTypedValueV3.GestureSettings(GestureV3(7,
        0,2,3,4,5,100, 11,12,13,14,15,16, 21,22,23,24,25,26, 31,32,33,34,35,36))

    @AfterEach fun restore() {
        ParameterStoreV3.clear()
        snapshot.forEach { (key, value) ->
            ParameterStoreV3.put(ParameterInfo(key.parameterID, key.dataCode, key.deviceAddress, 0), value)
        }
    }

    @ParameterizedTest @EnumSource(V3GestureCommand::class)
    fun `store then profile then original BLE packet for every command`(command: V3GestureCommand) {
        val order = mutableListOf<String>()
        val packets = mutableListOf<ByteArray>()
        val repository = V3GestureEditorRepositoryImpl(
            readSavedHandSide = { error("Writing must not read preferences") },
            subscribeSettingsUpdates = { error("Writing must not subscribe") },
            enqueuePacket = { packet ->
                assertEquals(listOf("profile"), order)
                packets.add(packet)
                order.add("enqueue")
            },
            saveProfile = { key, value ->
                assertEquals(info, key)
                assertEquals(expectedValue, value)
                assertEquals(value, ParameterStoreV3.get(key))
                order.add("profile")
            },
        )
        WriteGestureSettingsUseCaseV3(repository)(settings, command, "Original name")
        val expectedCommand = when (command) {
            V3GestureCommand.OPEN -> 0
            V3GestureCommand.CLOSE -> 1
            V3GestureCommand.OPEN_WITH_DELAY -> 128
            V3GestureCommand.CLOSE_WITH_DELAY -> 129
            V3GestureCommand.SAVE -> 255
        }
        val oldGesture = Gesture(7, 0,2,3,4,5,100, 11,12,13,14,15,16,
            21,22,23,24,25,26, 31,32,33,34,35,36, "Original name", 0)
        assertArrayEquals(BLECommandsV3.sendGestureInfo(GestureWithAddress(0, base.dataCode, oldGesture, expectedCommand)), packets.single())
        assertEquals(listOf("profile", "enqueue"), order)
    }

    @Test fun `profile failure retains store update and does not enqueue or retry`() {
        var sent = 0
        val repository = V3GestureEditorRepositoryImpl(
            readSavedHandSide = { error("Writing must not read preferences") },
            subscribeSettingsUpdates = { error("Writing must not subscribe") },
            enqueuePacket = { sent++ },
            saveProfile = { _, _ -> throw IllegalStateException("profile failure") },
        )
        assertThrows(IllegalStateException::class.java) {
            WriteGestureSettingsUseCaseV3(repository)(settings, V3GestureCommand.SAVE, "Name")
        }
        assertEquals(expectedValue, ParameterStoreV3.get(info))
        assertEquals(0, sent)
    }

    @ParameterizedTest @EnumSource(V3GestureCommand::class)
    fun `native raw write repeats original packet offline for every command without metadata store or profile`(command: V3GestureCommand) {
        val previousState = BLEState.state.value
        val raw = V3GestureSettings(Int.MIN_VALUE,
            listOf(-1, 0, 101, 255, 256, Int.MAX_VALUE), listOf(Int.MIN_VALUE, -257, 17, 99, 100, 400),
            listOf(-1, 255, 256, Int.MIN_VALUE, Int.MAX_VALUE, 21), listOf(31, -258, 0, 257, 1000, -500))
        val oldGesture = Gesture(raw.gestureId,
            -1, 0, 101, 255, 256, Int.MAX_VALUE, Int.MIN_VALUE, -257, 17, 99, 100, 400,
            -1, 255, 256, Int.MIN_VALUE, Int.MAX_VALUE, 21, 31, -258, 0, 257, 1000, -500, "Name", 0)
        val oldPacket = BLECommandsV3.sendGestureInfo(GestureWithAddress(91, 92, oldGesture, command.code))
        ParameterStoreV3.put(info, expectedValue)
        val beforeValues = ParameterStoreV3.values.value
        val packets = mutableListOf<ByteArray>()
        val repository = V3GestureEditorRepositoryImpl(
            readSavedHandSide = { error("Writing must not read preferences") },
            subscribeSettingsUpdates = { error("Writing must not subscribe") },
            enqueuePacket = { assertEquals(beforeValues, ParameterStoreV3.values.value); packets += it },
            saveProfile = { _, _ -> error("Native writing must not save a profile") },
            saveSettingsBeforeSending = false,
        )
        mockkObject(ParameterInfoRegistry)
        try {
            every { ParameterInfoRegistry.require(any()) } throws IllegalStateException("Native writing must not require metadata")
            BLEState.publishDisconnect()
            val write = WriteGestureSettingsUseCaseV3(repository, clampPositions = false)
            repeat(2) { write(raw, command, "Name") }
            assertEquals(2, packets.size)
            packets.forEach { assertArrayEquals(oldPacket, it) }
            val payload = listOf(39, 0, 255, 0, 101, 255, 0, 255, 0, 255, 17, 99, 100, 144,
                255, 255, 0, 0, 255, 21, 31, 254, 0, 1, 232, 12, command.code)
            assertEquals(33, packets.first().size)
            assertBytesWithCrc(packets.first().copyOfRange(0, 5), listOf(128, 15, 27, 0))
            assertBytesWithCrc(packets.first().copyOfRange(5, 33), payload)
            assertEquals(raw.gestureId.toByte(), packets.first()[6])
            assertEquals(beforeValues, ParameterStoreV3.values.value)
            verify(exactly = 0) { ParameterInfoRegistry.require(any()) }
        } finally {
            unmockkObject(ParameterInfoRegistry)
            when (previousState) {
                BLEState.State.DISCONNECTED -> BLEState.publishDisconnect()
                BLEState.State.CONNECTING -> BLEState.publishConnecting()
                BLEState.State.READY -> BLEState.publishReady()
                BLEState.State.ERROR -> BLEState.publishError()
            }
        }
    }

    private fun assertBytesWithCrc(packet: ByteArray, body: List<Int>) {
        var crc = 0
        body.forEach { byte ->
            crc = crc xor byte
            repeat(8) { crc = if (crc and 1 != 0) (crc ushr 1) xor 0x8C else crc ushr 1 }
        }
        assertArrayEquals((body + crc).map(Int::toByte).toByteArray(), packet)
    }
}
