package com.bailout.stickk.ubi4.versions.v3.domain.firmware

sealed interface V3ServiceFirmwareUpdateResult {
    data object Success : V3ServiceFirmwareUpdateResult
    data object BootEntryVerified : V3ServiceFirmwareUpdateResult
    data class StartSystemUpdateRejected(val status: String) : V3ServiceFirmwareUpdateResult
    data class CheckNewFirmwareRejected(val status: String, val isBoardIncompatible: Boolean) : V3ServiceFirmwareUpdateResult
    data object PreloadFailed : V3ServiceFirmwareUpdateResult
    data object CrcMismatch : V3ServiceFirmwareUpdateResult
}

interface V3ServiceFirmwareUpdateRepository {
    fun requireDebugInstallationAllowed()
    suspend fun installForDebug(file: V3ServiceFirmwareLocalFile)

    suspend fun install(
        boardAddress: Int,
        file: V3ServiceFirmwareLocalFile,
        onProgress: (Int) -> Unit,
        onPhaseChanged: (String, Long) -> Unit,
    ): V3ServiceFirmwareUpdateResult
}
