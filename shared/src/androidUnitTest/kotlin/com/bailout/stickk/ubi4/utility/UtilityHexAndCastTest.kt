package com.bailout.stickk.ubi4.utility

import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.utility.EncodeHexToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UtilityHexAndCastTest {

    @Test
    fun `bytesToHexString and decodeHex should work for ascii text`() {
        val hex = EncodeByteToHex.bytesToHexString("Hello".encodeToByteArray())

        assertEquals("48656c6c6f", hex)
        val decoded = with(EncodeByteToHex.Companion) { hex.decodeHex() }
        assertEquals("Hello", decoded)
    }

    @Test
    fun `decodeHexRussian should decode utf8 text`() {
        val original = "Привет"
        val hex = EncodeByteToHex.bytesToHexString(original.encodeToByteArray())

        val decoded = with(EncodeByteToHex.Companion) { hex.decodeHexRussian() }
        assertEquals(original, decoded)
    }

    @Test
    fun `decodeHex should throw on odd length`() {
        assertFailsWith<IllegalArgumentException> {
            with(EncodeByteToHex.Companion) { "ABC".decodeHex() }
        }
    }

    @Test
    fun `hexToBatteryPercent should parse and clamp values`() {
        val ok = with(EncodeHexToInt) { "64".hexToBatteryPercent() }
        val clamped = with(EncodeHexToInt) { "C8".hexToBatteryPercent() }
        val invalid = with(EncodeHexToInt) { "ZZ".hexToBatteryPercent() }

        assertEquals(100, ok)
        assertEquals(100, clamped)
        assertEquals(0, invalid)
    }

    @Test
    fun `castUnsignedCharToInt should convert signed byte to unsigned int`() {
        assertEquals(255, CastToUnsignedInt.Companion.castUnsignedCharToInt((-1).toByte()))
        assertEquals(128, CastToUnsignedInt.Companion.castUnsignedCharToInt((-128).toByte()))
        assertEquals(0, CastToUnsignedInt.Companion.castUnsignedCharToInt(0))
    }

    @Test
    fun `castBytesToFloatArray should decode little endian float values`() {
        val values = listOf(1.0f, -2.5f)
        val bytes = values.flatMap { toLittleEndianBytes(it).asList() }.toByteArray()

        val decoded = CastBytesToFloat.castBytesToFloatArray(bytes)
        assertEquals(values.size, decoded.size)
        assertEquals(1.0f, decoded[0], 0.00001f)
        assertEquals(-2.5f, decoded[1], 0.00001f)
    }

    @Test
    fun `crc32FromHexLE should parse first 4 bytes in little endian`() {
        val parsed = BleHexUtils.crc32FromHexLE("3412AABBCCDD")
        assertEquals(0xBBAA1234L, parsed)
    }

    @Test
    fun `crc32FromHexLE should reject too short input`() {
        assertFailsWith<IllegalArgumentException> {
            BleHexUtils.crc32FromHexLE("ABCD")
        }
    }

    private fun toLittleEndianBytes(value: Float): ByteArray {
        val bits = value.toBits()
        return byteArrayOf(
            (bits and 0xFF).toByte(),
            ((bits ushr 8) and 0xFF).toByte(),
            ((bits ushr 16) and 0xFF).toByte(),
            ((bits ushr 24) and 0xFF).toByte()
        )
    }
}
