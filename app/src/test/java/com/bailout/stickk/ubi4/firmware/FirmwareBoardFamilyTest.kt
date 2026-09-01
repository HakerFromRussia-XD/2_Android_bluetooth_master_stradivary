package com.bailout.stickk.ubi4.firmware

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FirmwareBoardFamilyTest {

    @Test
    fun `family is resolved from V3 device address`() {
        assertEquals(FirmwareBoardFamily.FAM, FirmwareBoardFamily.fromDeviceAddress(0x00))
        assertEquals(FirmwareBoardFamily.GUI, FirmwareBoardFamily.fromDeviceAddress(0x09))
        assertEquals(FirmwareBoardFamily.EMG, FirmwareBoardFamily.fromDeviceAddress(0x11))
        assertEquals(FirmwareBoardFamily.EMG, FirmwareBoardFamily.fromDeviceAddress(0x12))
        (0x20..0x25).forEach {
            assertEquals(FirmwareBoardFamily.BLDC, FirmwareBoardFamily.fromDeviceAddress(it))
        }
        assertEquals(FirmwareBoardFamily.UNKNOWN, FirmwareBoardFamily.fromDeviceAddress(0x26))
    }

    @Test
    fun `BLDC suffix is a hexadecimal target address and not a version part`() {
        val metadata = FirmwareCompatibility.metadata(
            FirmwareBoardFamily.BLDC,
            "DRV_Module_v0.6.13_25.zip"
        )

        assertEquals("0.6.13", metadata?.version)
        assertEquals(0x25, metadata?.targetAddress)
        assertTrue(FirmwareCompatibility.isCompatible(0x25, "DRV_Module_v0.6.13_25.zip"))
        assertFalse(FirmwareCompatibility.isCompatible(0x24, "DRV_Module_v0.6.13_25.zip"))
    }

    @Test
    fun `BLDC dotted full name is supported`() {
        val metadata = FirmwareCompatibility.metadata(
            FirmwareBoardFamily.BLDC,
            "DRV_Module_v0.6.13.25.zip"
        )

        assertEquals("0.6.13", metadata?.version)
        assertEquals(0x25, metadata?.targetAddress)
    }

    @Test
    fun `only firmware for the concrete BLDC address participates in update check`() {
        val files = listOf(
            "DRV_Module_v9.9.9_24.zip",
            "DRV_Module_v0.6.13_25.zip"
        )

        assertEquals("0.6.13", FirmwareCompatibility.newestVersion(0x25, files))
        assertFalse(FirmwareCompatibility.isUpdateAvailable(0x25, "0.6.13", files))
        assertFalse(FirmwareCompatibility.isUpdateAvailable(0x25, "0.6.13.25", files))
        assertTrue(FirmwareCompatibility.isUpdateAvailable(0x25, "0.6.12", files))
        assertFalse(FirmwareCompatibility.isUpdateAvailable(0x24, "9.9.9", files))
    }

    @Test
    fun `non BLDC families accept zip files from their own folder`() {
        assertTrue(FirmwareCompatibility.isCompatible(0x00, "FH_FAM_v0.1.29_.zip"))
        assertTrue(FirmwareCompatibility.isCompatible(0x09, "UBI4_1_Interface_v0.4.6.zip"))
        assertTrue(FirmwareCompatibility.isCompatible(0x11, "UBI41_EMG_SENSE_v0.2.1_.zip"))
        assertFalse(FirmwareCompatibility.isCompatible(0x11, "notes.txt"))
        assertNull(FirmwareCompatibility.versionForDevice(0x26, "FW_v1.0.0.zip"))
    }

    @Test
    fun `zero firmware versions are recognized as unavailable boards`() {
        assertTrue(FirmwareVersionCatalog.isZeroVersion("0.0.0"))
        assertTrue(FirmwareVersionCatalog.isZeroVersion("0.0.0.0"))
        assertFalse(FirmwareVersionCatalog.isZeroVersion("0.0.1"))
        assertFalse(FirmwareVersionCatalog.isZeroVersion(null))
        assertFalse(FirmwareVersionCatalog.isZeroVersion("—"))
    }
}
