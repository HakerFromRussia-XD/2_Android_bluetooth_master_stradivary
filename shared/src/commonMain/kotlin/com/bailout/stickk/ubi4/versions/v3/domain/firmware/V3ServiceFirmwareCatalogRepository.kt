package com.bailout.stickk.ubi4.versions.v3.domain.firmware

data class V3ServiceFirmwareFile(
    val family: String,
    val name: String,
    val path: String,
    val size: Long,
)

data class V3ServiceFirmwareLocalFile(val name: String, val path: String)

interface V3ServiceFirmwareCatalogRepository {
    suspend fun loadCatalog(): List<V3ServiceFirmwareFile>
    suspend fun downloadFiles(files: List<V3ServiceFirmwareFile>): List<V3ServiceFirmwareLocalFile>
}
