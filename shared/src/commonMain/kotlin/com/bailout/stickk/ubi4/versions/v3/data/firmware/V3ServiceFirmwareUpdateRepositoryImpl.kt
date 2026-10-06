package com.bailout.stickk.ubi4.versions.v3.data.firmware

import com.bailout.stickk.ubi4.firmware.FirmwareUpdateLogger
import com.bailout.stickk.ubi4.firmware.FirmwareUpdatePackage
import com.bailout.stickk.ubi4.firmware.FirmwareUpdateResult
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3ServiceFirmwareLocalFile
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3ServiceFirmwareUpdateRepository
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3ServiceFirmwareUpdateResult
import kotlinx.coroutines.CancellationException

/** Uses the existing DFU engine and platform ZIP reader; does not implement another protocol. */
class V3ServiceFirmwareUpdateRepositoryImpl(
    private val readPackage: (String) -> FirmwareUpdatePackage,
    private val prepareTransfer: suspend () -> Boolean,
    private val setSessionActive: (Boolean) -> Unit,
    private val runUpdate: suspend (Int, FirmwareUpdatePackage, (Int, Int) -> Unit) -> FirmwareUpdateResult,
    private val ensureBootloader: suspend (Int) -> Unit,
    private val elapsedRealtime: () -> Long,
    private val probeOnly: Boolean,
    private val logger: FirmwareUpdateLogger,
    private val debugLogger: FirmwareUpdateLogger = logger,
) : V3ServiceFirmwareUpdateRepository {
    override fun requireDebugInstallationAllowed() {
        check(!probeOnly) { "Full-update autorun is not permitted in the boot-entry probe build" }
    }

    override suspend fun installForDebug(file: V3ServiceFirmwareLocalFile) {
        debugLogger.info("DFU_V2_TRACE", "debug_autorun start file=${file.name}")
        try {
            setSessionActive(true)
            val firmware = readPackage(file.path)
            val result = runUpdate(0, firmware) { offset, total ->
                val percent = if (total <= 0) 0 else (offset * 100 / total).coerceIn(0, 100)
                debugLogger.info("DFU_V2_TRACE", "debug_autorun progress=$percent offset=$offset total=$total")
            }
            debugLogger.info("DFU_V2_TRACE", "debug_autorun result=$result")
        } catch (error: Throwable) {
            // The diagnostic entry historically logged cancellation too, without rethrowing it.
            debugLogger.error("DFU_V2_TRACE", "debug_autorun failed", error)
        } finally {
            setSessionActive(false)
        }
    }

    override suspend fun install(
        boardAddress: Int,
        file: V3ServiceFirmwareLocalFile,
        onProgress: (Int) -> Unit,
        onPhaseChanged: (String, Long) -> Unit,
    ): V3ServiceFirmwareUpdateResult {
        val startedAt = elapsedRealtime()
        var phase = "prepare_notifications"
        var progressBucket = -1
        logger.info(DIAG_TAG, "attempt START id=$startedAt addr=$boardAddress file=${file.name} interface_v3=true")
        fun enterPhase(value: String) {
            phase = value
            onPhaseChanged(value, startedAt)
        }
        onPhaseChanged(phase, startedAt)
        return try {
            setSessionActive(true)
            check(prepareTransfer()) { "Не удалось включить уведомления канала прошивки" }
            enterPhase("read_package")
            val firmware = readPackage(file.path)
            logger.info(DIAG_TAG,
                "attempt PACKAGE id=$startedAt protocol=V3 bytes=${firmware.payload.size} declared_size=${firmware.descriptorFirmwareSize} crc=${firmware.descriptorFirmwareCrc.toString(16)} descriptor=" +
                    firmware.descriptor.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') })
            enterPhase("coordinator")
            if (probeOnly) {
                check(boardAddress == 0) { "Эта диагностическая сборка проверяет только вход FAM в boot" }
                enterPhase("boot_entry_probe")
                logger.info(DIAG_TAG, "entry_probe START id=$startedAt; coordinator/BEGIN/erase disabled for this bench build")
                ensureBootloader(boardAddress)
                logger.info(DIAG_TAG, "entry_probe VERIFIED id=$startedAt elapsed_ms=${elapsedRealtime() - startedAt}; no firmware transfer")
                return V3ServiceFirmwareUpdateResult.BootEntryVerified
            }
            val result = runUpdate(boardAddress, firmware) { offset, total ->
                val bucket = if (total <= 0) 0 else (offset.toLong() * 100 / total).toInt() / 5
                if (bucket != progressBucket) {
                    progressBucket = bucket
                    logger.info(DIAG_TAG, "attempt PROGRESS id=$startedAt offset=$offset total=$total elapsed_ms=${elapsedRealtime() - startedAt}")
                }
                if (total > 0) {
                    val percent = (offset * 100 / total).coerceIn(0, 100)
                    onProgress(percent)
                }
            }
            logger.info(DIAG_TAG, "attempt RESULT id=$startedAt result=$result")
            enterPhase("handle_result")
            if (result == FirmwareUpdateResult.Success) {
                enterPhase("success")
                logger.info(DIAG_TAG, "attempt SUCCESS id=$startedAt elapsed_ms=${elapsedRealtime() - startedAt}")
            }
            result.toDomain()
        } catch (error: CancellationException) {
            logger.warn(DIAG_TAG, "attempt CANCELLED id=$startedAt phase=$phase: $error")
            throw error
        } catch (error: Exception) {
            logger.error(DIAG_TAG, "attempt FAILED id=$startedAt phase=$phase elapsed_ms=${elapsedRealtime() - startedAt}", error)
            logger.error("FW_FLOW", "Firmware update failed", error)
            throw error
        } finally {
            logger.info(DIAG_TAG, "attempt END id=$startedAt phase=$phase")
            setSessionActive(false)
        }
    }

    private fun FirmwareUpdateResult.toDomain(): V3ServiceFirmwareUpdateResult = when (this) {
        FirmwareUpdateResult.Success -> V3ServiceFirmwareUpdateResult.Success
        is FirmwareUpdateResult.StartSystemUpdateRejected -> V3ServiceFirmwareUpdateResult.StartSystemUpdateRejected(status.toString())
        is FirmwareUpdateResult.CheckNewFirmwareRejected -> V3ServiceFirmwareUpdateResult.CheckNewFirmwareRejected(status.toString(), status.code == 0)
        FirmwareUpdateResult.PreloadFailed -> V3ServiceFirmwareUpdateResult.PreloadFailed
        FirmwareUpdateResult.CrcMismatch -> V3ServiceFirmwareUpdateResult.CrcMismatch
    }

    private companion object { const val DIAG_TAG = "DFU_V2_DIAG" }
}
