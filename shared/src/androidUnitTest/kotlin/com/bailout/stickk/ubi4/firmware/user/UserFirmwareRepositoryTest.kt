package com.bailout.stickk.ubi4.firmware.user

import com.bailout.stickk.ubi4.data.network.YandexDiskFirmwareRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.*

class UserFirmwareRepositoryTest {
    @Test fun wrongHashNeverReachesArchiveReader() = runTest {
        val directory = createTempDir(prefix = "user-firmware-test-")
        var archiveReads = 0
        val client = HttpClient(MockEngine { request ->
            if (request.url.encodedPath.endsWith("/download")) {
                assertEquals("/GUI/main.zip", request.url.parameters["path"])
                respond("""{"href":"https://test.local/image"}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            } else respond(byteArrayOf(1, 2, 3))
        }) { install(ContentNegotiation) { json() } }
        try {
            val repository = UserFirmwareRepository(directory.path, object : UserFirmwareArchiveReader {
                override fun read(path: String, callback: (UserFirmwareArchive?) -> Unit) { archiveReads++; callback(null) }
            }, YandexDiskFirmwareRepository(client, "https://test.local/public", "https://test.local/"))
            val target = UserFirmwareTarget(AssemblyModule(9, file = "GUI/main.zip", sha256 = "a".repeat(64)),
                UserFirmwareVersion(1, 2, 3), File(directory, "image.zip").path)
            assertFailsWith<IllegalArgumentException> { repository.validate(target) }
            assertEquals(0, archiveReads)
            assertFalse(File(target.path).exists())
        } finally { client.close(); directory.deleteRecursively() }
    }

    @Test fun validCacheNeedsNoNetworkAndVersionMustMatch() = runTest {
        val directory = createTempDir(prefix = "user-firmware-cache-")
        val file = File(directory, "image.zip").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val client = HttpClient(MockEngine { error("Network must not be used for a verified cache entry") })
        try {
            val repository = UserFirmwareRepository(directory.path, object : UserFirmwareArchiveReader {
                override fun read(path: String, callback: (UserFirmwareArchive?) -> Unit) {
                    callback(UserFirmwareArchive("FWType=0\nFwMajorVersion=1\nFwMinorVersion=2\nFwQuickFix=3\nFWCRC=${com.bailout.stickk.ubi4.firmware.MotoricaCrc32.calculate(byteArrayOf(9))}", byteArrayOf(9)))
                }
            }, YandexDiskFirmwareRepository(client))
            val target = UserFirmwareTarget(AssemblyModule(9, file = "GUI/main.zip", sha256 = firmwareSha256(file.readBytes())),
                UserFirmwareVersion(1, 2, 3), file.path)
            repository.validate(target)
            assertFailsWith<IllegalArgumentException> { repository.validate(target.copy(version = UserFirmwareVersion(1, 2, 4))) }
        } finally { client.close(); directory.deleteRecursively() }
    }

    @Test fun journalReplacementIsReadableAfterMultipleWrites() = runTest {
        val directory = createTempDir(prefix = "user-firmware-journal-")
        try {
            val file = File(directory, "session.json")
            writeFirmwareJournal(file.path, "first")
            writeFirmwareJournal(file.path, "second")
            assertEquals("second", file.readText())
            assertFalse(File(file.path + ".tmp").exists())
        } finally { directory.deleteRecursively() }
    }
}
