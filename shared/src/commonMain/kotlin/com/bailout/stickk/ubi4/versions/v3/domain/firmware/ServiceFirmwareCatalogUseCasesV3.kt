package com.bailout.stickk.ubi4.versions.v3.domain.firmware

class LoadServiceFirmwareCatalogUseCaseV3(private val repository: V3ServiceFirmwareCatalogRepository) {
    suspend operator fun invoke() = repository.loadCatalog()
}

class DownloadServiceFirmwareFilesUseCaseV3(private val repository: V3ServiceFirmwareCatalogRepository) {
    suspend operator fun invoke(files: List<V3ServiceFirmwareFile>) = repository.downloadFiles(files)
}

/** Reuses the shared filename/version rules through pure functions supplied by DI. */
class GetServiceFirmwareForBoardUseCaseV3(
    private val familyForAddress: (Int) -> String,
    private val isCompatible: (Int, String) -> Boolean,
    private val versionForDevice: (Int, String) -> String?,
    private val isVersionNewer: (String?, String?) -> Boolean,
    private val isUpdateAvailable: (Int, String?, List<String>) -> Boolean,
) {
    operator fun invoke(
        catalog: List<V3ServiceFirmwareFile>,
        deviceAddress: Int,
    ): List<V3ServiceFirmwareFile> {
        val familyFiles = familyFiles(catalog, deviceAddress)
        return familyFiles
            .filter { isCompatible(deviceAddress, it.name) }
            .sortedWith { left, right ->
                val leftVersion = versionForDevice(deviceAddress, left.name)
                val rightVersion = versionForDevice(deviceAddress, right.name)
                when {
                    isVersionNewer(rightVersion, leftVersion) -> -1
                    isVersionNewer(leftVersion, rightVersion) -> 1
                    else -> left.name.compareTo(right.name, ignoreCase = true)
                }
            }
    }

    fun hasUpdate(catalog: List<V3ServiceFirmwareFile>, deviceAddress: Int, installedVersion: String?): Boolean =
        isUpdateAvailable(deviceAddress, installedVersion, familyFiles(catalog, deviceAddress).map { it.name })

    private fun familyFiles(catalog: List<V3ServiceFirmwareFile>, deviceAddress: Int): List<V3ServiceFirmwareFile> {
        val family = familyForAddress(deviceAddress)
        return catalog.filter { it.family == family }
    }
}
