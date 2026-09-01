package com.bailout.stickk.ubi4.firmware

enum class FirmwareBoardFamily(val folderName: String) {
    FAM("FAM"),
    GUI("GUI"),
    EMG("EMG"),
    BLDC("BLDC_Driver"),
    UNKNOWN("");

    companion object {
        fun fromDeviceAddress(address: Int): FirmwareBoardFamily =
            when (address) {
                0x00 -> FAM
                0x09 -> GUI
                0x11, 0x12 -> EMG
                in 0x20..0x25 -> BLDC
                else -> UNKNOWN
            }
    }
}

data class FirmwareArtifactMetadata(
    val version: String,
    val targetAddress: Int? = null
)

object FirmwareCompatibility {
    private val bldcVersionPattern = Regex(
        pattern = "^(\\d+)\\.(\\d+)\\.(\\d+)(?:[._]([0-9a-fA-F]{2}))?(?:_|$)"
    )

    fun compatibleFileNames(deviceAddress: Int, fileNames: List<String>): List<String> =
        fileNames.filter { isCompatible(deviceAddress, it) }

    fun isCompatible(deviceAddress: Int, fileName: String): Boolean {
        val family = FirmwareBoardFamily.fromDeviceAddress(deviceAddress)
        if (family == FirmwareBoardFamily.UNKNOWN) return false
        if (!fileName.endsWith(".zip", ignoreCase = true)) return false

        return when (family) {
            FirmwareBoardFamily.BLDC ->
                metadata(family, fileName)?.targetAddress == deviceAddress
            else -> true
        }
    }

    fun metadata(family: FirmwareBoardFamily, fileName: String): FirmwareArtifactMetadata? =
        when (family) {
            FirmwareBoardFamily.BLDC -> parseBldcMetadata(fileName)
            FirmwareBoardFamily.UNKNOWN -> null
            else -> FirmwareVersionCatalog.parseVersionFromFileName(fileName)
                ?.let(::FirmwareArtifactMetadata)
        }

    fun versionForDevice(deviceAddress: Int, fileName: String): String? {
        val family = FirmwareBoardFamily.fromDeviceAddress(deviceAddress)
        if (!isCompatible(deviceAddress, fileName)) return null
        return metadata(family, fileName)?.version
    }

    fun newestVersion(deviceAddress: Int, fileNames: List<String>): String? =
        compatibleFileNames(deviceAddress, fileNames)
            .mapNotNull { versionForDevice(deviceAddress, it) }
            .fold(null as String?) { newest, candidate ->
                if (newest == null || FirmwareVersionCatalog.isLocalVersionNewer(newest, candidate)) {
                    candidate
                } else {
                    newest
                }
            }

    fun isUpdateAvailable(
        deviceAddress: Int,
        installedVersion: String?,
        fileNames: List<String>
    ): Boolean {
        val newest = newestVersion(deviceAddress, fileNames) ?: return false
        return FirmwareVersionCatalog.isLocalVersionNewer(
            deviceVersion = normalizeInstalledVersion(deviceAddress, installedVersion),
            localVersion = newest
        )
    }

    private fun parseBldcMetadata(fileName: String): FirmwareArtifactMetadata? {
        val baseName = fileName.substringAfterLast('/').removeZipExtension()
        val markerIndex = maxOf(
            baseName.lowercase().lastIndexOf("_v"),
            baseName.lowercase().lastIndexOf("-v")
        )
        if (markerIndex < 0) return null

        val match = bldcVersionPattern.find(baseName.substring(markerIndex + 2)) ?: return null
        val addressToken = match.groupValues.getOrNull(4).orEmpty()
        if (addressToken.isBlank()) return null

        return FirmwareArtifactMetadata(
            version = listOf(match.groupValues[1], match.groupValues[2], match.groupValues[3]).joinToString("."),
            targetAddress = addressToken.toIntOrNull(radix = 16)
        )
    }

    private fun normalizeInstalledVersion(deviceAddress: Int, installedVersion: String?): String? {
        if (FirmwareBoardFamily.fromDeviceAddress(deviceAddress) != FirmwareBoardFamily.BLDC) {
            return installedVersion
        }
        val parts = installedVersion?.split('.') ?: return null
        if (parts.size != 4 || parts.last().toIntOrNull(radix = 16) != deviceAddress) {
            return installedVersion
        }
        return parts.take(3).joinToString(".")
    }

    private fun String.removeZipExtension(): String =
        if (endsWith(".zip", ignoreCase = true)) dropLast(4) else this
}
