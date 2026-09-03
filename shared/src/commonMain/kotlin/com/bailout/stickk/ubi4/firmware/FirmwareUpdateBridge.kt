package com.bailout.stickk.ubi4.firmware

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

data class FirmwareUpdateBridgeEvent(
    val kind: String,
    val progress: Int,
    val message: String
) {
    val isFinished: Boolean get() = kind == KIND_SUCCESS || kind == KIND_ERROR
    val isSuccess: Boolean get() = kind == KIND_SUCCESS

    companion object {
        const val KIND_PROGRESS = "progress"
        const val KIND_SUCCESS = "success"
        const val KIND_ERROR = "error"
    }
}

object FirmwareUpdateBridge {
    private val coroutineScope: CoroutineScope = MainScope()

    fun runV3FirmwareUpdate(
        deviceAddress: Int,
        fileName: String,
        descriptorText: String,
        payload: ByteArray,
        callback: (FirmwareUpdateBridgeEvent) -> Unit
    ): Job = coroutineScope.launch {
        try {
            // The mobile update flow starts from a connected main application.
            // Recovery from an already-running bootloader remains a Dashboard
            // workflow, matching the Android client policy.
            DfuDiagnostics.requireMainStart = true
            val descriptor = FirmwareInfoDescriptorBuilder.build(
                FirmwareInfoDescriptorBuilder.parseIniProperties(descriptorText)
            )
            val firmware = FirmwareUpdatePackage(
                name = fileName,
                descriptor = descriptor.bytes,
                payload = payload,
                descriptorFirmwareSize = descriptor.firmwareSize,
                descriptorFirmwareCrc = descriptor.firmwareCrc,
                localVersionString = descriptor.localVersionString
            )
            val coordinator = FirmwareUpdateCoordinator(
                ubi4Updater = Ubi4FirmwareUpdater(
                    sender = PlatformFirmwareCommandSender,
                    logger = BridgeFirmwareUpdateLogger
                ),
                v3Updater = V3FirmwareUpdater(
                    sender = PlatformFirmwareCommandSender,
                    bulkTransport = PlatformFirmwareBulkTransport,
                    logger = BridgeFirmwareUpdateLogger
                ),
                legacyV3Updater = LegacyV3FirmwareUpdater(
                    sender = PlatformFirmwareCommandSender,
                    logger = BridgeFirmwareUpdateLogger
                ),
                logger = BridgeFirmwareUpdateLogger
            )
            val result = coordinator.runFirmwareUpdate(
                protocol = FirmwareUpdateProtocol.V3,
                addr = deviceAddress,
                firmware = firmware
            ) { offset, total ->
                val progress = if (total <= 0) 0 else (offset * 100 / total).coerceIn(0, 100)
                callback(FirmwareUpdateBridgeEvent(FirmwareUpdateBridgeEvent.KIND_PROGRESS, progress, ""))
            }

            when (result) {
                FirmwareUpdateResult.Success -> callback(
                    FirmwareUpdateBridgeEvent(
                        FirmwareUpdateBridgeEvent.KIND_SUCCESS,
                        100,
                        "Обновление успешно завершено!"
                    )
                )
                is FirmwareUpdateResult.StartSystemUpdateRejected -> callback(
                    FirmwareUpdateBridgeEvent(
                        FirmwareUpdateBridgeEvent.KIND_ERROR,
                        0,
                        "Не удалось начать обновление (status=${result.status})"
                    )
                )
                is FirmwareUpdateResult.CheckNewFirmwareRejected -> callback(
                    FirmwareUpdateBridgeEvent(
                        FirmwareUpdateBridgeEvent.KIND_ERROR,
                        0,
                        "Модуль не готов к записи (status=${result.status})"
                    )
                )
                FirmwareUpdateResult.PreloadFailed -> callback(
                    FirmwareUpdateBridgeEvent(
                        FirmwareUpdateBridgeEvent.KIND_ERROR,
                        0,
                        "Не удалось подготовить память для прошивки"
                    )
                )
                FirmwareUpdateResult.CrcMismatch -> callback(
                    FirmwareUpdateBridgeEvent(
                        FirmwareUpdateBridgeEvent.KIND_ERROR,
                        0,
                        "CRC mismatch! Обновление не удалось."
                    )
                )
            }
        } catch (error: Throwable) {
            callback(
                FirmwareUpdateBridgeEvent(
                    FirmwareUpdateBridgeEvent.KIND_ERROR,
                    0,
                    "Обновление не удалось: ${error.message.orEmpty()}"
                )
            )
        }
    }
}

private object BridgeFirmwareUpdateLogger : FirmwareUpdateLogger
