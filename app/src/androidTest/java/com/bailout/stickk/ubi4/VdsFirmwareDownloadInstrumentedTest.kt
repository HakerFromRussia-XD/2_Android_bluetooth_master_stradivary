package com.bailout.stickk.ubi4

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bailout.stickk.ubi4.data.network.FirmwareRepositoryProvider
import com.bailout.stickk.ubi4.data.network.sharedFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Properties
import java.util.zip.ZipFile

@RunWith(AndroidJUnit4::class)
class VdsFirmwareDownloadInstrumentedTest {
    @Test
    fun catalogAndFirmwareArchivesDownloadWithoutBluetooth() = runBlocking {
        val repository = FirmwareRepositoryProvider.active()
        val assembly = repository.readPublicFile("assembly.json").decodeToString()
        assertTrue("assembly.json must describe an assembly", assembly.contains("\"kind\": \"assembly\""))

        val catalog = repository.loadCatalog()
        assertTrue("VDS catalog must contain firmware archives", catalog.isNotEmpty())

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val testDirectory = File(context.cacheDir, "vds_firmware_download_test").apply {
            deleteRecursively()
            check(mkdirs())
        }

        try {
            val cacheDirectory = sharedFile(testDirectory.absolutePath)
            catalog.forEach { firmware ->
                val downloaded = repository.download(firmware, cacheDirectory)
                val archive = File(downloaded.path)

                assertTrue("${firmware.path} was not downloaded", archive.isFile)
                assertEquals(
                    "${firmware.path} size differs from catalog.json",
                    firmware.size,
                    archive.length()
                )
                ZipFile(archive).use { zip ->
                    assertNotNull("${firmware.path} has no UserFW.bin", zip.getEntry("UserFW.bin"))
                    val descriptor = assertNotNull(
                        "${firmware.path} has no FW_ini.ini",
                        zip.getEntry("FW_ini.ini")
                    )
                    val properties = Properties().apply {
                        zip.getInputStream(descriptor).bufferedReader().useLines { lines ->
                            load(lines.filterNot { it.trimStart().startsWith("[") }.joinToString("\n").reader())
                        }
                    }
                    assertEquals(
                        "${firmware.path} version differs from FW_ini.ini",
                        listOf("FwMajorVersion", "FwMinorVersion", "FwQuickFix")
                            .joinToString(".") { properties.getProperty(it) },
                        firmware.version
                    )
                }
            }
        } finally {
            testDirectory.deleteRecursively()
        }
    }
}
