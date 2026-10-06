package com.bailout.stickk.ubi4.versions.v3.domain.firmware

class InstallServiceFirmwareUseCaseV3(private val repository: V3ServiceFirmwareUpdateRepository) {
    suspend operator fun invoke(
        boardAddress: Int,
        file: V3ServiceFirmwareLocalFile,
        onProgress: (Int) -> Unit,
        onPhaseChanged: (String, Long) -> Unit,
    ) = repository.install(boardAddress, file, onProgress, onPhaseChanged)
}

class InstallServiceFirmwareForDebugUseCaseV3(private val repository: V3ServiceFirmwareUpdateRepository) {
    fun requireAllowed() = repository.requireDebugInstallationAllowed()
    suspend operator fun invoke(file: V3ServiceFirmwareLocalFile) = repository.installForDebug(file)
}
