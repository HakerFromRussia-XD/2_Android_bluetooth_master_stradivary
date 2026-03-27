package com.bailout.stickk.ubi4.data

import com.bailout.stickk.ubi4.data.local.FirmwareInfoStruct
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SerializersUbi4ModelsTest {

    @Test
    fun `DeviceInfoStructs serializer should decode packed hex payload`() {
        val hex = buildString {
            append(asciiHexPadded("MOTOR", 32))
            append(hexByte(0x12))
            append(hexByte(0x34))
            append(asciiHexPadded("LABEL", 16))
            append(hexByte(0x07))
            append(hexByte(0x21))
            append(hexByte(0x03))
            append(hexByte(0x08))
            append(asciiHexPadded("PREFIX", 16))
            append(le32(0x00112233))
            append(hexByte(0xAA))
            append("1A2B3C4D")
        }

        val parsed = Json.decodeFromString<DeviceInfoStructs>("\"$hex\"")

        assertTrue(parsed.deviceName.startsWith("MOTOR"))
        assertTrue(parsed.deviceLabel.startsWith("LABEL"))
        assertEquals(0x12, parsed.deviceVersion)
        assertEquals(0x34, parsed.deviceSubVersion)
        assertEquals(0x07, parsed.deviceType)
        assertEquals(0x21, parsed.deviceCode)
        assertEquals(0x03, parsed.deviceRole)
        assertEquals(0x08, parsed.deviceAddress)
        assertEquals("PREFIX", parsed.deviceUUIDPrefix)
        assertEquals(0x00112233, parsed.deviceUUID)
        assertEquals(0xAA, parsed.deviceAdditionalInfoType)
        assertEquals(0x1A2B3C4D, parsed.deviceAdditionalInfo)
        assertEquals("00042", DeviceInfoStructs(deviceUUID = 42).formattedDeviceUUID)
    }

    @Test
    fun `FirmwareInfoStruct serializer should decode little endian fields`() {
        val hex = buildString {
            append(asciiHexPadded("FW_MAIN", 32))
            append(hexByte(1))
            append(hexByte(2))
            append(hexByte(3))
            append(hexByte(4))
            append(asciiHexPadded("STABLE", 16))
            append(hexByte(5))
            append(hexByte(6))
            append(le32(0x01020304))
            append(le32(0x11121314))
            append(le32(0x21222324))
            append(hexByte(7))
            append(hexByte(8))
            append(hexByte(9))
            append(hexByte(10))
            append(hexByte(11))
            append(le32(0x31323334))
        }

        val parsed = Json.decodeFromString<FirmwareInfoStruct>("\"$hex\"")

        assertEquals("FW_MAIN", parsed.fwName)
        assertEquals("STABLE", parsed.fwLabel)
        assertEquals(1, parsed.fwMajor)
        assertEquals(2, parsed.fwMinor)
        assertEquals(3, parsed.fwQuickFix)
        assertEquals("1.2.3", parsed.fwVersion)
        assertEquals(0x01020304, parsed.fwStartAddress)
        assertEquals(0x11121314, parsed.fwSize)
        assertEquals(0x21222324, parsed.fwCrc)
        assertEquals(11, parsed.fwAdditionalInfoType)
        assertEquals(0x31323334, parsed.fwAdditionalInfo)
    }

    @Test
    fun `FullInicializeConnection serializer should decode payload`() {
        val deviceLabelHex = "11223344556677889900AABBCCDDEEFF"
        val hex = buildString {
            append(asciiHexPadded("UBI_DEVICE", 32))
            append(hexByte(0x09))
            append(hexByte(0x0A))
            append(deviceLabelHex)
            append(hexByte(0x0B))
            append(hexByte(0x0C))
            append(hexByte(0x0D))
            append(asciiHexPadded("UUID_PREFIX", 16))
            append(le32(0x0A0B0C0D))
            append(hexByte(0x10))
            append(hexByte(0x11))
            append(hexByte(0x12))
            append(hexByte(0x13))
        }

        val parsed = Json.decodeFromString<FullInicializeConnectionStruct>("\"$hex\"")

        assertTrue(parsed.deviceName.startsWith("UBI_DEVICE"))
        assertEquals(0x09, parsed.deviceVersion)
        assertEquals(0x0A, parsed.deviceSubVersion)
        assertEquals(deviceLabelHex, parsed.deviceLabel)
        assertEquals(0x0B, parsed.deviceType)
        assertEquals(0x0C, parsed.deviceCode)
        assertEquals(0x0D, parsed.deviceAddress)
        assertTrue(parsed.deviceUUID_Prefix.startsWith("UUID_PREFIX"))
        assertEquals(0x0A0B0C0D, parsed.deviceUUID)
        assertEquals(0x10, parsed.parametersNum)
        assertEquals(0x11, parsed.subDeviceNum)
        assertEquals(0x12, parsed.programType)
        assertEquals(0x13, parsed.defaultPort)
    }

    @Test
    fun `FullInicializeConnection serializer should return defaults for short payload`() {
        val parsed = Json.decodeFromString<FullInicializeConnectionStruct>("\"AA\"")

        assertEquals("", parsed.deviceName)
        assertEquals(0, parsed.deviceVersion)
        assertEquals(0, parsed.deviceUUID)
        assertEquals(0, parsed.defaultPort)
    }

    private fun asciiHexPadded(text: String, bytesCount: Int): String {
        val bytes = ByteArray(bytesCount)
        text.encodeToByteArray().copyInto(bytes, endIndex = text.length.coerceAtMost(bytesCount))
        return EncodeByteToHex.bytesToHexString(bytes)
    }

    private fun hexByte(value: Int): String =
        (value and 0xFF).toString(16).padStart(2, '0')

    private fun le32(value: Int): String =
        buildString {
            repeat(4) { index ->
                append(hexByte((value ushr (index * 8)) and 0xFF))
            }
        }
}
