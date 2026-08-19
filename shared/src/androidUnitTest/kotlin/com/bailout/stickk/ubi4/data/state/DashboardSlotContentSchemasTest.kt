package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DashboardSlotContentSchemasTest {
    @Test
    fun parseDeviceInfoV14UsesOffsetsFromSchema() {
        val data = MutableList(104) { 0 }
        data.writeString(0, "FEST-")
        data.writeString(10, "INDY-3")
        data[42] = 3
        data[43] = 1
        data.writeString(44, "INDY3")
        data[60] = 7
        data[61] = 9
        data[62] = 2
        data[63] = 0x2A
        data.writeString(64, "FEST-INDY3-00000000000000000001")
        data[96] = 4
        data.writeUInt32(97, 0x12345678)
        data[101] = 1
        data[102] = 0x10
        data[103] = 0x1F

        val parsed = DashboardSlotContentSchemas.parse(
            state = DashboardSlotContentUiState(
                dataCode = 3,
                version = 1,
                subVersion = 4,
                data = data
            )
        )

        assertEquals("FEST-", parsed.input("DevicePrefix").value)
        assertEquals("INDY-3", parsed.input("DeviceName").value)
        assertEquals("3", parsed.input("DeviceVersion").value)
        assertEquals("1", parsed.input("DeviceSubVersion").value)
        assertEquals("INDY3", parsed.input("DeviceLabel").value)
        assertEquals("7", parsed.input("DeviceType").value)
        assertEquals("9", parsed.input("DeviceCode").value)
        assertEquals("2", parsed.input("DeviceRole").value)
        assertEquals("0x2A", parsed.input("DeviceAddress").value)
        assertEquals("FEST-INDY3-00000000000000000001", parsed.input("DeviceUUID").value)
        assertEquals("4", parsed.input("DeviceAdditionalInfoType").value)
        assertEquals("0x12345678", parsed.input("DeviceAdditionalInfo").value)
        assertEquals("1", parsed.input("DeviceIsCopyable").value)
        assertEquals("0x10", parsed.input("min_device_address").value)
        assertEquals("0x1F", parsed.input("max_device_address").value)
    }

    @Test
    fun updateDeviceInfoV14FieldsPreservesUnrelatedBytes() {
        val original = MutableList(104) { index -> (index + 1) and 0xFF }
        val state = DashboardSlotContentUiState(
            dataCode = 3,
            version = 1,
            subVersion = 4,
            data = original
        )
        val parsed = DashboardSlotContentSchemas.parse(state)

        val nameUpdated = DashboardSlotContentSchemas.updateValue(
            state = state,
            path = parsed.input("DeviceName").path,
            value = "NEW"
        )

        assertEquals(original.subList(0, 10), nameUpdated.subList(0, 10))
        assertEquals(listOf('N'.code, 'E'.code, 'W'.code), nameUpdated.subList(10, 13))
        assertEquals(List(29) { 0 }, nameUpdated.subList(13, 42))
        assertEquals(original.subList(42, 104), nameUpdated.subList(42, 104))

        val nameUpdatedState = state.copy(data = nameUpdated)
        val nameUpdatedParsed = DashboardSlotContentSchemas.parse(nameUpdatedState)
        val additionalInfoUpdated = DashboardSlotContentSchemas.updateValue(
            state = nameUpdatedState,
            path = nameUpdatedParsed.input("DeviceAdditionalInfo").path,
            value = "0x12345678"
        )

        assertEquals(nameUpdated.subList(0, 97), additionalInfoUpdated.subList(0, 97))
        assertEquals(listOf(0x78, 0x56, 0x34, 0x12), additionalInfoUpdated.subList(97, 101))
        assertEquals(nameUpdated.subList(101, 104), additionalInfoUpdated.subList(101, 104))
    }

    @Test
    fun unknownSlotSubVersionUsesRawByteInputs() {
        val parsed = DashboardSlotContentSchemas.parse(
            state = DashboardSlotContentUiState(
                dataCode = 3,
                version = 1,
                subVersion = 99,
                data = listOf(0x00, 0x01, 0xFE, 0xFF)
            )
        )

        assertEquals(listOf("byte[0]", "byte[1]", "byte[2]", "byte[3]"), parsed.inputs.map { it.name })
        assertEquals(listOf("0x00", "0x01", "0xFE", "0xFF"), parsed.inputs.map { it.value })
        assertTrue(parsed.structureGroups.isEmpty())
    }

    @Test
    fun parseTelemetryDataUsesArrayFieldsFromSchema() {
        val data = MutableList(158) { 0 }
        "FEST-H-0001".forEachIndexed { index, char ->
            data[2 + index] = char.code
        }
        data.writeUInt32(34, 25)
        data.writeUInt32(34 + 15 * 4, 40)
        data.writeUInt32(98, 5)
        data.writeUInt32(98 + 14 * 4, 9)

        val parsed = DashboardSlotContentSchemas.parse(
            state = DashboardSlotContentUiState(
                dataCode = 30,
                version = 1,
                subVersion = 0,
                data = data
            )
        )

        assertEquals("FEST-H-0001", parsed.input("DeviceUUID").value)
        assertEquals(
            (listOf(25) + List(14) { 0 } + 40).joinToString(prefix = "[", postfix = "]", separator = ", "),
            parsed.input("gesture_movement_count").value
        )
        assertEquals(
            (listOf(5) + List(13) { 0 } + 9).joinToString(prefix = "[", postfix = "]", separator = ", "),
            parsed.input("user_gesture_movement_count").value
        )
    }

    @Test
    fun parseMotorPidSettingsUsesExactSubVersion() {
        val data = MutableList(64) { 0 }
        data.writeFloat(24, 0.02f)
        data.writeFloat(60, 0.5f)

        val parsed = DashboardSlotContentSchemas.parse(
            state = DashboardSlotContentUiState(
                dataCode = 31,
                version = 1,
                subVersion = 2,
                data = data
            )
        )

        assertNotNull(parsed.inputs.firstOrNull { it.name == "position.dt" })
        assertEquals("0.02", parsed.input("position.dt").value)
        assertEquals("0.5", parsed.input("speed.deadzone").value)
        assertTrue(parsed.inputs.none { it.name == "dt" })
    }

    @Test
    fun parseMotorSettingsUsesLatestOffsets() {
        val data = MutableList(37) { 0 }
        data[0] = 3
        data[24] = 77
        data.writeUInt32(28, 1024)

        val parsed = DashboardSlotContentSchemas.parse(
            state = DashboardSlotContentUiState(
                dataCode = 16,
                version = 1,
                subVersion = 6,
                data = data
            )
        )

        assertEquals("3", parsed.input("motor_channel").value)
        assertEquals("77", parsed.input("speed_limit").value)
        assertEquals("1024", parsed.input("min_speed_limit").value)
    }

    @Test
    fun dataSlotEnumContainsCurrentSchemaCodes() {
        assertEquals(30, PreferenceKeysUbi4.DataTableSlotsCode.DTCE_TELEMETRY_DATA.number.toInt())
        assertEquals(31, PreferenceKeysUbi4.DataTableSlotsCode.DTCE_MOTOR_PID_SETTINGS.number.toInt())
        assertEquals(20, PreferenceKeysUbi4.DataTableSlotsCode.DTCE_GESTURES_KEY_DESCRIPTION.number.toInt())
    }

    private fun DashboardSlotContentParsed.input(name: String): DashboardSlotContentInput =
        inputs.firstOrNull { it.name == name } ?: error("Input $name not found")

    private fun MutableList<Int>.writeUInt32(offset: Int, value: Long) {
        repeat(4) { index ->
            this[offset + index] = ((value shr (index * 8)) and 0xFF).toInt()
        }
    }

    private fun MutableList<Int>.writeFloat(offset: Int, value: Float) {
        writeUInt32(offset, value.toRawBits().toLong())
    }

    private fun MutableList<Int>.writeString(offset: Int, value: String) {
        value.forEachIndexed { index, char ->
            this[offset + index] = char.code
        }
    }
}
