package com.bailout.stickk.ubi4.firmware

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FirmwareVersionCatalogTest {

    @Test
    fun `parseVersionFromFileName should support android firmware filename styles`() {
        val cases = mapOf(
            "cpu_program_v1.2.3.zip" to "1.2.3",
            "omg-program-v2.0.1.zip" to "2.0.1",
            "/storage/Firmware/bms_program_v3.4.zip" to "3.4",
            "fh_fam_v10.11.12.bin" to "10.11.12",
            "emg_sense_v1.0.0.7.zip" to "1.0.0.7"
        )

        cases.forEach { (fileName, version) ->
            assertEquals(version, FirmwareVersionCatalog.parseVersionFromFileName(fileName))
        }
    }

    @Test
    fun `latestVersions should choose highest version for every firmware key`() {
        val catalog = FirmwareVersionCatalog.latestVersions(
            listOf(
                "cpu_program_v1.0.0.zip",
                "cpu_program_v1.0.2.zip",
                "cpu_program_v1.0.1.zip",
                "bms_v2.1.0.zip",
                "bms-v2.2.0.zip"
            )
        )

        assertEquals("1.0.2", catalog["cpu_program"])
        assertEquals("2.2.0", catalog["bms"])
    }

    @Test
    fun `shouldHighlightUpdate should use android board aliases`() {
        val files = listOf(
            "omg_program_v1.0.1.zip",
            "cpu_program_v2.0.0.zip",
            "bms_v3.0.0.zip",
            "emg_sense_v4.0.0.zip",
            "fh_fam_v5.0.0.zip",
            "gui_v6.0.0.zip"
        )

        assertTrue(FirmwareVersionCatalog.shouldHighlightUpdate("OMG Module", "1.0.0", files))
        assertTrue(FirmwareVersionCatalog.shouldHighlightUpdate("CPU Module", "1.9.9", files))
        assertTrue(FirmwareVersionCatalog.shouldHighlightUpdate("BMS", "2.9.9", files))
        assertTrue(FirmwareVersionCatalog.shouldHighlightUpdate("EMG Sense", "3.9.9", files))
        assertTrue(FirmwareVersionCatalog.shouldHighlightUpdate("FEST H and F", "4.9.9", files))
        assertTrue(FirmwareVersionCatalog.shouldHighlightUpdate("FH-FAM", "4.9.9", files))
        assertTrue(FirmwareVersionCatalog.shouldHighlightUpdate("GUI", "5.9.9", files))
    }

    @Test
    fun `shouldHighlightUpdate should be false when local version is absent same or older`() {
        val files = listOf("cpu_program_v1.0.0.zip")

        assertFalse(FirmwareVersionCatalog.shouldHighlightUpdate("CPU Module", "1.0.0", files))
        assertFalse(FirmwareVersionCatalog.shouldHighlightUpdate("CPU Module", "1.0.1", files))
        assertFalse(FirmwareVersionCatalog.shouldHighlightUpdate("Unknown Board", "0.0.1", files))
    }

    @Test
    fun `empty device version should highlight when firmware exists`() {
        val files = listOf("cpu_program_v1.0.0.zip")

        assertTrue(FirmwareVersionCatalog.shouldHighlightUpdate("CPU Module", null, files))
        assertTrue(FirmwareVersionCatalog.shouldHighlightUpdate("CPU Module", "-", files))
        assertTrue(FirmwareVersionCatalog.shouldHighlightUpdate("CPU Module", "unknown", files))
    }
}
