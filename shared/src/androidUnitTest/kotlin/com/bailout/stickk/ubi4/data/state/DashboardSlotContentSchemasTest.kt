package com.bailout.stickk.ubi4.data.state

import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DashboardSlotContentSchemasTest {
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
}
