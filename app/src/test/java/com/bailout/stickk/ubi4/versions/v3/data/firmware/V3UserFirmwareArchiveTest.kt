package com.bailout.stickk.ubi4.versions.v3.data.firmware

import com.bailout.stickk.ubi4.firmware.MotoricaCrc32
import com.bailout.stickk.ubi4.firmware.user.UserFirmwareArchive
import com.bailout.stickk.ubi4.firmware.user.UserFirmwareVersion
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** Consumer checks for common archive validation and the bytes passed to the existing uploader. */
class V3UserFirmwareArchiveTest {
    private val payload = byteArrayOf(1, 2, 3)
    private val descriptor = "FWType=0\nFwMajorVersion=0\nFwMinorVersion=6\nFwQuickFix=10"
    private fun archive() = UserFirmwareArchive(
        "$descriptor\nFWsize=3\nFWCRC=${MotoricaCrc32.calculate(payload)}", payload,
    )

    @Test fun `comments separators and duplicate fields keep the same version and descriptor bytes`() {
        val archive = UserFirmwareArchive("""
            # ignored comment
            ; ignored comment
            ! ignored comment
            BoardName: CPU
            FWType=1
            FWType: 0
            FwMajorVersion=0
            FwMinorVersion=6
            FwQuickFix=9
            FwQuickFix: 10
            FWSinceLastTag=4
            FWsize: 3
            FWCRC: ${MotoricaCrc32.calculate(payload)}
        """.trimIndent(), payload)
        assertEquals(UserFirmwareVersion(0, 6, 10), archive.version())
        archive.validateImage()
        val packet = archive.packageFor("CPU/main.zip")
        assertEquals("CPU/main.zip", packet.name)
        assertEquals("0.6.10.4", packet.localVersionString)
        assertEquals(3L, packet.descriptorFirmwareSize)
        assertEquals(MotoricaCrc32.calculate(payload), packet.descriptorFirmwareCrc)
        assertSame(payload, packet.payload)
        assertEquals(120, packet.descriptor.size)
        assertArrayEquals("CPU".encodeToByteArray(), packet.descriptor.copyOfRange(0, 3))
        assertArrayEquals(byteArrayOf(0, 6, 10, 4), packet.descriptor.copyOfRange(77, 81))
        assertArrayEquals(byteArrayOf(3, 0, 0, 0), packet.descriptor.copyOfRange(103, 107))
        assertArrayEquals(packet.descriptor, archive.packageFor("CPU/main.zip").descriptor)
    }

    @Test fun `all archive entry points preserve validation order and existing error types`() {
        val cases = listOf(
            Triple("FwMajorVersion=0", byteArrayOf(), IllegalStateException("Missing firmware field: FWType")),
            Triple("FWType=1", byteArrayOf(), IllegalArgumentException("Only main firmware is allowed")),
            Triple("FWType=0", byteArrayOf(), IllegalArgumentException("Empty firmware image")),
            Triple("FWType=0\nFwMajorVersion=x", payload, IllegalStateException("Missing firmware field: FwMajorVersion")),
            Triple("FWType=0\nFwMajorVersion=0\nFwMinorVersion=6\nFwQuickFix=256", payload,
                IllegalArgumentException("Failed requirement.")),
        )
        val actions = listOf<(UserFirmwareArchive) -> Any>(
            { it.version() }, { it.validateImage() }, { it.packageFor("main.zip") },
        )
        for ((text, bytes, expected) in cases) {
            val archive = UserFirmwareArchive(text, bytes)
            for (action in actions) {
                val thrown = runCatching { action(archive) }.exceptionOrNull()
                assertNotNull(thrown)
                assertEquals(expected.javaClass, thrown!!.javaClass)
                assertEquals(expected.message, thrown.message)
            }
        }
    }

    @Test fun `CRC remains required numeric and equal to the current image`() {
        for (text in listOf(descriptor, "$descriptor\nFWCRC=x")) {
            val error = assertThrows(IllegalStateException::class.java) { UserFirmwareArchive(text, payload).validateImage() }
            assertEquals("Missing firmware CRC", error.message)
        }
        val error = assertThrows(IllegalArgumentException::class.java) {
            UserFirmwareArchive("$descriptor\nFWCRC=0", payload).validateImage()
        }
        assertEquals("Firmware image CRC mismatch", error.message)
    }

    @Test fun `successful validation does not cache validity of mutable payload bytes`() {
        val archive = archive()
        archive.validateImage()
        payload[0] = 9
        val error = assertThrows(IllegalArgumentException::class.java) { archive.validateImage() }
        assertEquals("Firmware image CRC mismatch", error.message)
        assertEquals(UserFirmwareVersion(0, 6, 10), archive.version())
        assertSame(payload, archive.packageFor("main.zip").payload)
    }

    @Test fun `copied archives use their own descriptor and still validate their payload`() {
        val archive = archive()
        archive.version()
        val changed = archive.copy(descriptorText = archive.descriptorText.replace("FwQuickFix=10", "FwQuickFix=11"))
        assertEquals(UserFirmwareVersion(0, 6, 11), changed.version())
        assertEquals(UserFirmwareVersion(0, 6, 10), archive.version())
        val error = assertThrows(IllegalArgumentException::class.java) { archive.copy(payload = byteArrayOf()).validateImage() }
        assertEquals("Empty firmware image", error.message)
    }

    @Test fun `package preparation preserves size metadata and does not add CRC validation`() {
        for (crcField in listOf("", "\nFWCRC=0", "\nFWCRC=x")) {
            val archive = UserFirmwareArchive("$descriptor\nFWsize=999$crcField", payload)
            val packet = archive.packageFor("main.zip")
            assertEquals(999L, packet.descriptorFirmwareSize)
            assertEquals(0L, packet.descriptorFirmwareCrc)
            assertSame(payload, packet.payload)
        }
    }
}
