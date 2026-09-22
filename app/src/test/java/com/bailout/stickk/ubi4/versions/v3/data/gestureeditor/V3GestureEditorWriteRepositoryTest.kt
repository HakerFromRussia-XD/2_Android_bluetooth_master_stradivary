package com.bailout.stickk.ubi4.versions.v3.data.gestureeditor

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.models.ble.GestureV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.gestures.GestureWithAddress
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_SETTING
import com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor.*
import io.mockk.mockk
import io.reactivex.schedulers.Schedulers
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

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
        val repository = V3GestureEditorRepositoryImpl(mockk(), { packet ->
            assertEquals(listOf("profile"), order)
            packets.add(packet)
            order.add("enqueue")
        }, Schedulers.trampoline()) { key, value ->
            assertEquals(info, key)
            assertEquals(expectedValue, value)
            assertEquals(value, ParameterStoreV3.get(key))
            order.add("profile")
        }
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
        val repository = V3GestureEditorRepositoryImpl(mockk(), { sent++ }, Schedulers.trampoline()) { _, _ ->
            throw IllegalStateException("profile failure")
        }
        assertThrows(IllegalStateException::class.java) {
            WriteGestureSettingsUseCaseV3(repository)(settings, V3GestureCommand.SAVE, "Name")
        }
        assertEquals(expectedValue, ParameterStoreV3.get(info))
        assertEquals(0, sent)
    }
}
