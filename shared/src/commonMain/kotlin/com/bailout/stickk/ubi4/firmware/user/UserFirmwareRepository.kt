package com.bailout.stickk.ubi4.firmware.user

import com.bailout.stickk.ubi4.data.network.YandexDiskFirmwareRepository
import com.bailout.stickk.ubi4.data.network.sharedFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class UserFirmwareRepository(
    private val directory: String,
    private val reader: UserFirmwareArchiveReader,
    private val disk: YandexDiskFirmwareRepository = YandexDiskFirmwareRepository()
) {
    suspend fun targets(boards: List<UserFirmwareBoard>): List<UserFirmwareTarget> {
        val assembly = FirmwareAssembly.parse(disk.readPublicFile("assembly.json").decodeToString())
        val present = boards.map { it.address }.toSet()
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
