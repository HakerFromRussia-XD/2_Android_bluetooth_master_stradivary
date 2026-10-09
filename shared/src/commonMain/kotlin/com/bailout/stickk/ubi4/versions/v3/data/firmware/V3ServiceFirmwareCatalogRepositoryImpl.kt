package com.bailout.stickk.ubi4.versions.v3.data.firmware

import com.bailout.stickk.ubi4.data.network.YandexDiskFirmwareRepository
import com.bailout.stickk.ubi4.data.network.RemoteFirmwareFile
import com.bailout.stickk.ubi4.data.network.sharedFile
import com.bailout.stickk.ubi4.firmware.FirmwareBoardFamily
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3ServiceFirmwareCatalogRepository
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3ServiceFirmwareFile
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3ServiceFirmwareLocalFile

class V3ServiceFirmwareCatalogRepositoryImpl(
    private val cacheDirectory: () -> String,
    private val remote: YandexDiskFirmwareRepository = YandexDiskFirmwareRepository(),
) : V3ServiceFirmwareCatalogRepository {
    constructor(cacheDirectory: () -> String) : this(cacheDirectory, YandexDiskFirmwareRepository())

    override suspend fun loadCatalog() = remote.loadCatalog().map {
        V3ServiceFirmwareFile(it.family.name, it.name, it.path, it.size)
    }

    override suspend fun downloadFiles(files: List<V3ServiceFirmwareFile>): List<V3ServiceFirmwareLocalFile> {
        val directory = sharedFile(cacheDirectory())
        return files.map { file ->
            val downloaded = remote.download(
                RemoteFirmwareFile(FirmwareBoardFamily.valueOf(file.family), file.name, file.path, file.size),
                directory,
            )
            V3ServiceFirmwareLocalFile(file.name, downloaded.path)
        }
    }
}
