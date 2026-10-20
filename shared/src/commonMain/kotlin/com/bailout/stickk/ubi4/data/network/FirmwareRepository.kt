package com.bailout.stickk.ubi4.data.network

import com.bailout.stickk.ubi4.firmware.FirmwareBoardFamily

data class RemoteFirmwareFile(
    val family: FirmwareBoardFamily,
    val name: String,
    val path: String,
    val size: Long,
    val version: String
)

interface FirmwareRepository {
    suspend fun readPublicFile(path: String): ByteArray
    suspend fun loadCatalog(): List<RemoteFirmwareFile>
    suspend fun download(file: RemoteFirmwareFile, cacheDirectory: SharedFile): SharedFile
}

object FirmwareRepositoryProvider {
    const val ACTIVE_BASE_URL = "https://firmware-ms.157.22.203.6.nip.io/"

    fun active(): FirmwareRepository = HttpFirmwareRepository(baseUrl = ACTIVE_BASE_URL)

    /** Preserved as an inactive rollback source; production code does not use it. */
    fun inactiveYandex(): FirmwareRepository = YandexDiskFirmwareRepository()
}
