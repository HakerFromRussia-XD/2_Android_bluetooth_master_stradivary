package com.bailout.stickk.ubi4.versions.v3.data.firmware

import com.bailout.stickk.ubi4.data.network.YandexDiskFirmwareRepository
import com.bailout.stickk.ubi4.data.network.sharedFile
import com.bailout.stickk.ubi4.firmware.user.AssemblyModule
import com.bailout.stickk.ubi4.firmware.user.FirmwareAssembly
import com.bailout.stickk.ubi4.firmware.user.UserFirmwareArchive
import com.bailout.stickk.ubi4.firmware.user.UserFirmwareArchiveReader
import com.bailout.stickk.ubi4.firmware.user.UserFirmwareJournal
import com.bailout.stickk.ubi4.firmware.user.UserFirmwareTarget
import com.bailout.stickk.ubi4.firmware.user.firmwareSha256
import com.bailout.stickk.ubi4.firmware.user.writeFirmwareJournal
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3UserFirmwareSession
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3UserFirmwareBoard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume

class UserFirmwareRepository(
    private val directory: String,
    private val reader: UserFirmwareArchiveReader,
    private val disk: YandexDiskFirmwareRepository = YandexDiskFirmwareRepository()
) {
    suspend fun readJournal(path: String): V3UserFirmwareSession<UserFirmwareTarget>? {
        val text = sharedFile(path).let { if (it.exists()) it.readBytes().decodeToString() else null }
        return text?.takeIf { it.isNotBlank() && it != "null" }?.let {
            val journal = Json.decodeFromString<UserFirmwareJournal>(it)
            V3UserFirmwareSession(journal.deviceId, journal.targets, journal.completed, journal.attempted, journal.formatVersion)
        }
    }

    suspend fun writeJournal(path: String, session: V3UserFirmwareSession<UserFirmwareTarget>?) =
        writeFirmwareJournal(path, session?.let {
            Json.encodeToString(UserFirmwareJournal(it.deviceId, it.targets, it.completed, it.attempted, it.formatVersion))
        } ?: "null")

    suspend fun targets(boards: List<V3UserFirmwareBoard<*>>): List<UserFirmwareTarget> {
        val assembly = FirmwareAssembly.parse(disk.readPublicFile("assembly.json").decodeToString())
        val present = boards.map { it.address }.toSet()
        platformLog("USER_DFU", "assembly modules=${assembly.modules.joinToString { "${it.address}:${it.file}" }} present=$present")
        return assembly.modules.filter { it.address in present }.map { module ->
            val path = "$directory/${module.sha256}.zip"
            ensureArchive(module, path)
            val archive = read(path)
            UserFirmwareTarget(module, archive.version(), path)
        }
    }

    suspend fun validate(target: UserFirmwareTarget) {
        ensureArchive(target.module, target.path)
        require(read(target.path).version() == target.version) { "Firmware version changed" }
    }

    suspend fun read(path: String): UserFirmwareArchive = suspendCancellableCoroutine { continuation ->
        reader.read(path) { archive ->
            if (continuation.isActive) {
                if (archive == null) continuation.resumeWith(Result.failure(IllegalArgumentException("Invalid firmware ZIP")))
                else continuation.resumeWith(runCatching { archive.also { it.validateImage() } })
            }
        }
    }

    private suspend fun ensureArchive(module: AssemblyModule, path: String) {
        val file = sharedFile(path)
        if (file.exists()) {
            try {
                if (firmwareSha256(file.readBytes()) == module.sha256) return
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* Re-fetch corrupt/incomplete cache entries. */ }
        }
        val bytes = disk.readPublicFile(module.file)
        require(firmwareSha256(bytes) == module.sha256) { "Firmware SHA-256 mismatch" }
        file.writeBytes(bytes)
    }
}
