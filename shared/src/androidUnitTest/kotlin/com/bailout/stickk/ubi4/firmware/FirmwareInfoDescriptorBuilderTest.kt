package com.bailout.stickk.ubi4.firmware

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class FirmwareInfoDescriptorBuilderTest {

    @Test
    fun `build should create 120 byte descriptor with little endian fields`() {
        val descriptor = FirmwareInfoDescriptorBuilder.build(
            mapOf(
                "BoardName" to "CPU",
                "BoardVersion" to "1",
                "BoardSubVersion" to "2",
                "BoardRevision" to "3",
                "BoardSubRevision" to "4",
                "BoardInstance" to "4660",
                "BoardType" to "5",
                "BoardCode" to "6",
                "BoardAdditionalInfoType" to "7",
                "BoardAdditionalInfo" to "16909060",
                "FwName" to "cpu_program",
                "FwMajorVersion" to "8",
                "FwMinorVersion" to "9",
                "FwQuickFix" to "10",
                "FWSinceLastTag" to "11",
                "FwLabel" to "stable",
                "FWType" to "12",
                "FWCode" to "13",
                "FWStartAddress" to "287454020",
                "FWsize" to "1432778632",
                "FWCRC" to "2578103244",
                "SDKMajorVersion" to "14",
                "SDKMinorVersion" to "15",
                "SDKQuickFix" to "16",
                "SDKSinceLastTag" to "17",
                "FWAdditionalInfoType" to "18",
                "FWAdditionalInfo" to "3723427584"
            )
        )

        assertEquals(120, descriptor.bytes.size)
        assertEquals(1432778632L, descriptor.firmwareSize)
        assertEquals(2578103244L, descriptor.firmwareCrc)
        assertEquals("8.9.10.11", descriptor.localVersionString)
        assertContentEquals("CPU".encodeToByteArray(), descriptor.bytes.copyOfRange(0, 3))
        assertContentEquals(byteArrayOf(0x34, 0x12), descriptor.bytes.copyOfRange(36, 38))
        assertContentEquals(byteArrayOf(0x04, 0x03, 0x02, 0x01), descriptor.bytes.copyOfRange(41, 45))
        assertContentEquals("cpu_program".encodeToByteArray(), descriptor.bytes.copyOfRange(45, 56))
        assertContentEquals(byteArrayOf(0x44, 0x33, 0x22, 0x11), descriptor.bytes.copyOfRange(99, 103))
        assertContentEquals(byteArrayOf(0x88.toByte(), 0x77, 0x66, 0x55), descriptor.bytes.copyOfRange(103, 107))
        assertContentEquals(byteArrayOf(0xCC.toByte(), 0xBB.toByte(), 0xAA.toByte(), 0x99.toByte()), descriptor.bytes.copyOfRange(107, 111))
    }

    @Test
    fun `patchFirmwareSizeInPlace should update size field only`() {
        val descriptor = ByteArray(120) { index -> index.toByte() }
        val before = descriptor.copyOf()

        FirmwareInfoDescriptorBuilder.patchFirmwareSizeInPlace(descriptor, 0x01020304)

        assertContentEquals(byteArrayOf(0x04, 0x03, 0x02, 0x01), descriptor.copyOfRange(103, 107))
        assertContentEquals(before.copyOfRange(0, 103), descriptor.copyOfRange(0, 103))
        assertContentEquals(before.copyOfRange(107, 120), descriptor.copyOfRange(107, 120))
    }
}
