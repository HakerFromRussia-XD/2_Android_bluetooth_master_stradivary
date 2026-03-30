package com.bailout.stickk.ubi4.data.local

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GestureAndGroupsTest {

    @Test
    fun `Gesture serializer should round-trip numeric fields`() {
        val source = Gesture(
            gestureId = 1,
            openPosition1 = 2, openPosition2 = 3, openPosition3 = 4,
            openPosition4 = 5, openPosition5 = 6, openPosition6 = 7,
            closePosition1 = 8, closePosition2 = 9, closePosition3 = 10,
            closePosition4 = 11, closePosition5 = 12, closePosition6 = 13,
            openToCloseTimeShift1 = 14, openToCloseTimeShift2 = 15,
            openToCloseTimeShift3 = 16, openToCloseTimeShift4 = 17,
            openToCloseTimeShift5 = 18, openToCloseTimeShift6 = 19,
            closeToOpenTimeShift1 = 20, closeToOpenTimeShift2 = 21,
            closeToOpenTimeShift3 = 22, closeToOpenTimeShift4 = 23,
            closeToOpenTimeShift5 = 24, closeToOpenTimeShift6 = 25,
            gestureName = "not_persisted",
            gestureImage = 99
        )

        val encoded = Json.encodeToString(source)
        val decoded = Json.decodeFromString<Gesture>(encoded)

        assertTrue(encoded.length >= 52) // 50 hex chars + quotes
        assertEquals(1, decoded.gestureId)
        assertEquals(7, decoded.openPosition6)
        assertEquals(13, decoded.closePosition6)
        assertEquals(19, decoded.openToCloseTimeShift6)
        assertEquals(25, decoded.closeToOpenTimeShift6)
        assertEquals("", decoded.gestureName)
        assertEquals(0, decoded.gestureImage)
    }

    @Test
    fun `RotationGroup serializer should parse eight gesture pairs`() {
        val hex = buildHex(
            1, 11, 2, 12, 3, 13, 4, 14,
            5, 15, 6, 16, 7, 17, 8, 18
        )

        val parsed = Json.decodeFromString<RotationGroup>("\"$hex\"")
        val list = parsed.toGestureList()

        assertEquals(8, list.size)
        assertEquals(1 to 11, list.first())
        assertEquals(8 to 18, list.last())
    }

    @Test
    fun `BindingGroup serializer should parse 12 pairs and set item by index`() {
        val hex = buildHex(
            1, 21, 2, 22, 3, 23, 4, 24, 5, 25, 6, 26,
            7, 27, 8, 28, 9, 29, 10, 30, 11, 31, 12, 32
        ) + "0000"

        val parsed = Json.decodeFromString<BindingGestureGroup>("\"$hex\"")
        val before = parsed.toGestureList()
        assertEquals(1 to 21, before.first())
        assertEquals(12 to 32, before.last())

        parsed.setGestureAt(5, 99 to 77)
        val after = parsed.toGestureList()
        assertEquals(99 to 77, after[5])
    }

    @Test
    fun `BindingGestureGroup setGestureAt should throw for invalid index`() {
        val group = BindingGestureGroup()
        assertFailsWith<IndexOutOfBoundsException> {
            group.setGestureAt(12, 1 to 1)
        }
    }

    private fun buildHex(vararg values: Int): String =
        values.joinToString("") { value ->
            (value and 0xFF).toString(16).padStart(2, '0')
        }
}
