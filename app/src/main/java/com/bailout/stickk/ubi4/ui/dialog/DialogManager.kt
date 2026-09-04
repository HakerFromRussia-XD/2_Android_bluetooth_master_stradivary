package com.bailout.stickk.ubi4.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.ble.AndroidFirmwareCommandSender
import com.bailout.stickk.ubi4.ble.AndroidFirmwareUpdateLogger
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.firmware.FirmwareUpdateCoordinator
import com.bailout.stickk.ubi4.firmware.FirmwareUpdateProtocol
import com.bailout.stickk.ubi4.firmware.FirmwareUpdateResult
import com.bailout.stickk.ubi4.firmware.LegacyV3FirmwareUpdater
import com.bailout.stickk.ubi4.firmware.PlatformFirmwareBulkTransport
import com.bailout.stickk.ubi4.firmware.Ubi4FirmwareUpdater
import com.bailout.stickk.ubi4.firmware.V3FirmwareUpdater
import com.bailout.stickk.ubi4.models.FirmwareFileItem
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentUBI4.BootloaderBoardItemUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.firmware.FirmwareUpdateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class DialogManager(
    private val context: Context,
    private val layoutInflater: LayoutInflater,
    private val viewLifecycleOwner: LifecycleOwner,
    private val onDisconnectConfirmed: () -> Unit,
    ) {
    private val firmwareUpdateCoordinator = FirmwareUpdateCoordinator(
        ubi4Updater = Ubi4FirmwareUpdater(
            sender = AndroidFirmwareCommandSender,
            logger = AndroidFirmwareUpdateLogger
        ),
        v3Updater = V3FirmwareUpdater(
            sender = AndroidFirmwareCommandSender,
            bulkTransport = PlatformFirmwareBulkTransport,
            logger = AndroidFirmwareUpdateLogger
        ),
        legacyV3Updater = LegacyV3FirmwareUpdater(
            sender = AndroidFirmwareCommandSender,
            logger = AndroidFirmwareUpdateLogger
        ),
        logger = AndroidFirmwareUpdateLogger
    )
    private var currentDialog: Dialog? = null
    private var progressDialog: Dialog? = null


    @SuppressLint("InflateParams")
    fun showDisconnectDialog() {
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.ubi4_dialog_disconnection, null)
        val myDialog = Dialog(context)
        myDialog.setContentView(dialogView)
        myDialog.setCancelable(false)
        myDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        val yesBtn = dialogView.findViewById<View>(R.id.ubi4DialogConfirmDisconnectionBtn)
        yesBtn.setOnClickListener {
            onDisconnectConfirmed()
            myDialog.dismiss()
        }
        val noBtn = dialogView.findViewById<View>(R.id.ubi4DialogCancelDisconnectionBtn)
        noBtn.setOnClickListener {
            myDialog.dismiss()
        }
    }
    private fun closeAllDialogs() {
        currentDialog?.dismiss()
        currentDialog = null
        progressDialog?.dismiss()
        progressDialog = null
    }

    fun onDestroy() {
        closeAllDialogs()
    }

    @SuppressLint("LogNotTimber")
    fun showConfirmSendFirmwareFileDialog(
        board: BootloaderBoardItemUBI4,
        fileItem: FirmwareFileItem,
        onConfirm: (FirmwareFileItem) -> Unit
    ) {
        val view = layoutInflater.inflate(
            R.layout.ubi4_dialog_confirm_send_firmware_file, null
        )
        currentDialog = Dialog(context).apply {
            setContentView(view)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            show()
        }

        view.findViewById<View>(R.id.ubi4DialogSendFirmwareCancelBtn)
            .setOnClickListener { currentDialog?.dismiss() }

        view.findViewById<View>(R.id.ubi4DialogConfirmSendFirmwareBtn)
            .setOnClickListener {
                val addr = board.deviceAddress
                Log.d("FW_FLOW", "CONFIRM addr=$addr (${board.boardName})")
                closeAllDialogs()
                val progressBar = showProgressBarDialog()

                viewLifecycleOwner.lifecycleScope.launch {
                    val startedAt = SystemClock.elapsedRealtime()
                    var phase = "prepare_notifications"
                    var progressBucket = -1
                    Log.i(AndroidFirmwareUpdateLogger.DIAG_TAG,
                        "attempt START id=$startedAt addr=$addr file=${fileItem.file.name} interface_v3=${UiState.isInterfaceV3Activated}")
                    val timeoutJob = launch {
                        var last = progressBar.progress
                        while (isActive) {
                            delay(30_000)
                            if (progressBar.progress == last && progressBar.progress < 100) {
                                Log.w(AndroidFirmwareUpdateLogger.DIAG_TAG,
                                    "attempt STALLED id=$startedAt phase=$phase progress=$last elapsed_ms=${SystemClock.elapsedRealtime() - startedAt}")
                                showWarningLoadingDialog()
                                break
                            }
                            last = progressBar.progress
                        }
                    }
                    try {
                        val bleController = main?.getBLEController()
                        bleController?.setFirmwareUpdateSessionActive(true)
                        val protocol = if (UiState.isInterfaceV3Activated) {
                            FirmwareUpdateProtocol.V3
                        } else {
                            FirmwareUpdateProtocol.UBI4
                        }
                        if (protocol == FirmwareUpdateProtocol.V3) {
                            check(bleController?.prepareFirmwareSessionNotifications() == true) {
                                "Не удалось включить уведомления канала прошивки"
                            }
                        }
                        phase = "read_package"
                        val firmwarePackage = FirmwareUpdateUtils.readFirmwarePackage(fileItem.file)
                        Log.i(AndroidFirmwareUpdateLogger.DIAG_TAG,
                            "attempt PACKAGE id=$startedAt protocol=$protocol bytes=${firmwarePackage.payload.size} declared_size=${firmwarePackage.descriptorFirmwareSize} crc=${firmwarePackage.descriptorFirmwareCrc.toString(16)} descriptor=" +
                                firmwarePackage.descriptor.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') })
                        phase = "coordinator"
                        val result = firmwareUpdateCoordinator.runFirmwareUpdate(
                            protocol = protocol,
                            addr = addr,
                            firmware = firmwarePackage
                        ) { offset, total ->
                            val bucket = if (total <= 0) 0 else (offset.toLong() * 100 / total).toInt() / 5
                            if (bucket != progressBucket) {
                                progressBucket = bucket
                                Log.i(AndroidFirmwareUpdateLogger.DIAG_TAG,
                                    "attempt PROGRESS id=$startedAt offset=$offset total=$total elapsed_ms=${SystemClock.elapsedRealtime() - startedAt}")
                            }
                            updateProgress(progressBar, offset, total)
                        }
                        Log.i(AndroidFirmwareUpdateLogger.DIAG_TAG, "attempt RESULT id=$startedAt result=$result")
                        phase = "handle_result"
                        if (!handleFirmwareUpdateResult(result)) {
                            return@launch
                        }

                        phase = "success"
                        Log.i(AndroidFirmwareUpdateLogger.DIAG_TAG,
                            "attempt SUCCESS id=$startedAt elapsed_ms=${SystemClock.elapsedRealtime() - startedAt}")
                        progressDialog?.dismiss()
                        main?.showToast(context.getString(SharedRes.strings.firmware_update_success.resourceId))
                        currentDialog?.dismiss()
                        onConfirm(fileItem)
                    } catch (e: CancellationException) {
                        Log.w(AndroidFirmwareUpdateLogger.DIAG_TAG, "attempt CANCELLED id=$startedAt phase=$phase", e)
                        throw e
                    } catch (e: Exception) {
                        Log.e(AndroidFirmwareUpdateLogger.DIAG_TAG,
                            "attempt FAILED id=$startedAt phase=$phase elapsed_ms=${SystemClock.elapsedRealtime() - startedAt}", e)
                        Log.e("FW_FLOW", "Firmware update failed", e)
                        progressDialog?.dismiss()
                        main?.showToast(
                            context.getString(
                                SharedRes.strings.firmware_update_failed_with_message.resourceId,
                                e.localizedMessage ?: context.getString(SharedRes.strings.error.resourceId)
                            )
                        )
                    } finally {
                        Log.i(AndroidFirmwareUpdateLogger.DIAG_TAG, "attempt END id=$startedAt phase=$phase")
                        main?.getBLEController()?.setFirmwareUpdateSessionActive(false)
                        timeoutJob.cancel()
                    }
                }
            }
    }

    fun runV3FirmwareUpdateForDebug(file: File) {
        viewLifecycleOwner.lifecycleScope.launch {
            Log.i("DFU_V2_TRACE", "debug_autorun start file=${file.name}")
            try {
                main?.getBLEController()?.setFirmwareUpdateSessionActive(true)
                val firmwarePackage = FirmwareUpdateUtils.readFirmwarePackage(file)
                val result = firmwareUpdateCoordinator.runFirmwareUpdate(
                    protocol = FirmwareUpdateProtocol.V3,
                    addr = 0,
                    firmware = firmwarePackage
                ) { offset, total ->
                    val percent = if (total <= 0) 0 else (offset * 100 / total).coerceIn(0, 100)
                    Log.i(
                        "DFU_V2_TRACE",
                        "debug_autorun progress=$percent offset=$offset total=$total"
                    )
                }
                Log.i("DFU_V2_TRACE", "debug_autorun result=$result")
            } catch (error: Throwable) {
                Log.e("DFU_V2_TRACE", "debug_autorun failed", error)
            } finally {
                main?.getBLEController()?.setFirmwareUpdateSessionActive(false)
            }
        }
    }

    private fun handleFirmwareUpdateResult(result: FirmwareUpdateResult): Boolean =
        when (result) {
            FirmwareUpdateResult.Success -> true
            is FirmwareUpdateResult.StartSystemUpdateRejected -> {
                progressDialog?.dismiss()
                currentDialog?.dismiss()
                main?.showToast(context.getString(SharedRes.strings.failed_to_start_update_status.resourceId, result.status))
                false
            }
            is FirmwareUpdateResult.CheckNewFirmwareRejected -> {
                progressDialog?.dismiss()
                main?.showToast(context.getString(SharedRes.strings.module_not_ready_for_writing_status.resourceId, result.status))
                false
            }
            FirmwareUpdateResult.PreloadFailed -> {
                progressDialog?.dismiss()
                main?.showToast(context.getString(SharedRes.strings.failed_to_prepare_memory_for_firmware.resourceId))
                false
            }
            FirmwareUpdateResult.CrcMismatch -> {
                progressDialog?.dismiss()
                main?.showToast(context.getString(SharedRes.strings.crc_mismatch_update_failed.resourceId))
                false
            }
        }

    private fun updateProgress(progressBar: ProgressBar, offset: Int, total: Int) {
        if (total <= 0) return
        val percent = (offset * 100 / total).coerceIn(0, 100)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            progressBar.progress = percent
        }
    }

    @SuppressLint("InflateParams", "MissingInflatedId")
    private fun showProgressBarDialog(): ProgressBar {
        // Гарантированно закрываем всё перед новым диалогом
        closeAllDialogs()

        // 1) Inflate правильный layout
        val dialogView = layoutInflater.inflate(R.layout.ubi4_dialog_progressbar_firmware, null)

        // 2) Создаём диалог
        progressDialog = Dialog(context).apply {
            setContentView(dialogView)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            show()
        }

        // 3) Находим ProgressBar внутри dialogView
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.loadingFirmwareProgressBar)
            ?: throw IllegalStateException("В ubi4_dialog_progressbar.xml нет View с id loadingFirmwareProgressBar")
        return progressBar
    }

    @SuppressLint("InflateParams")
    private fun showWarningLoadingDialog() {
        closeAllDialogs()
        val dialogView = layoutInflater.inflate(
            R.layout.ubi4_dialog_warning_load_firmware, null
        )
        Dialog(context).apply {
            setContentView(dialogView)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            show()
        }.also { dlg ->
            dialogView.findViewById<View>(R.id.ubi4WarningLoadingFirmwareBtn)
                .setOnClickListener {
                    dlg.dismiss()
                }
        }
    }
}
