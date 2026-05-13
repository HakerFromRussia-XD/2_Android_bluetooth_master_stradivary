package com.bailout.stickk.ubi4.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.ble.BleFirmwareUpdater
import com.bailout.stickk.ubi4.ble.BleFirmwareUpdaterV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.FirmwareFileItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.local.MaxChunkSizeInfo
import com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentUBI4.BootloaderBoardItemUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DialogManager(
    private val context: Context,
    private val layoutInflater: LayoutInflater,
    private val viewLifecycleOwner: LifecycleOwner,
    private val onDisconnectConfirmed: () -> Unit,
    ) {
    private val updater = BleFirmwareUpdater()
    private val updaterV3 = BleFirmwareUpdaterV3()
    private var lastMaxChunkInfo: MaxChunkSizeInfo? = null
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
                    val timeoutJob = launch {
                        var last = progressBar.progress
                        while (isActive) {
                            delay(30_000)
                            if (progressBar.progress == last && progressBar.progress < 100) {
                                showWarningLoadingDialog()
                                break
                            }
                            last = progressBar.progress
                        }
                    }
                    try {
                        val success = if (UiState.isInterfaceV3Activated) {
                            runFirmwareUpdateV3(addr, fileItem, progressBar)
                        } else {
                            runFirmwareUpdateUbi4(addr, fileItem, progressBar)
                        }
                        if (!success) {
                            return@launch
                        }

                        progressDialog?.dismiss()
                        main?.showToast("Обновление успешно завершено!")
                        currentDialog?.dismiss()
                        onConfirm(fileItem)
                    } catch (e: Exception) {
                        Log.e("FW_FLOW", "Firmware update failed", e)
                        progressDialog?.dismiss()
                        main?.showToast("Обновление не удалось: ${e.message}")
                    } finally {
                        timeoutJob.cancel()
                    }
                }
            }
    }

    private suspend fun runFirmwareUpdateUbi4(
        addr: Int,
        fileItem: FirmwareFileItem,
        progressBar: ProgressBar
    ): Boolean {
        // 1) START_SYSTEM_UPDATE
        val startStatus = updater.startSystemUpdate()
        if (startStatus != PreferenceKeysUbi4.StartSystemUpdateStatus.NEW_FW_ACCEPT) {
            main?.showToast("Не удалось начать обновление (status=$startStatus)")
            progressDialog?.dismiss()
            currentDialog?.dismiss()
            return false
        }

        // 2) ENSURE BOOTLOADER
        updater.ensureBootloader(addr)

        // 3) GET_BOOTLOADER_INFO
        updater.getBootloaderInfo(addr)

        // 4) CHECK_NEW_FW
        val checkStatus = updater.checkNewFirmware(addr, fileItem)
        if (checkStatus != PreferenceKeysUbi4.CheckNewFwStatus.NEW_FW_ACCEPT) {
            progressDialog?.dismiss()
            main?.showToast("Модуль не готов к записи (status=$checkStatus)")
            return false
        }

        // 5) GET_MAX_CHANK_SIZE
        lastMaxChunkInfo = updater.getMaxChunkSize(addr)

        // 6) PRELOAD_INFO
        val preloadStatus = updater.preloadFlash(addr)
        Log.d("FW_FLOW", "RX PRELOAD_INFO → $preloadStatus")

        // 6.1) Ждём flashClearDelayMs мс для завершения очистки флеша
        val delayMs = lastMaxChunkInfo?.flashClearDelayMs?.toLong() ?: 0L
        Log.d("FW_FLOW", "Waiting $delayMs ms for flash clear")
        delay(delayMs)

        // 7) GET_BOOTLOADER_STATUS (ожидание DONE_CLEAR)
        val doneClear = updater.waitForDoneClear(addr)
        Log.i("FW_FLOW", "Прошивка готова, статус = $doneClear")

        // 8) Всё готово — отправляем файл чанками
        lastMaxChunkInfo?.let { info ->
            updater.sendFirmwareWithProgress(addr, fileItem.file, info) { offset, total ->
                updateProgress(progressBar, offset, total)
            }
        }

        // 9) Проверка CRC и финализация
        val crcOk = updater.checkFirmwareCrcAndCompleteUpdate(addr)
        if (!crcOk) {
            progressDialog?.dismiss()
            main?.showToast("CRC mismatch! Обновление не удалось.")
            return false
        }

        // 10) FINISH_SYSTEM_UPDATE
        updater.finishSystemUpdate(addr)
        return true
    }

    private suspend fun runFirmwareUpdateV3(
        addr: Int,
        fileItem: FirmwareFileItem,
        progressBar: ProgressBar
    ): Boolean {
        // 1) GET_RUN_PROGRAM_TYPE + JUMP_TO_BOOTLOADER при необходимости
        updaterV3.ensureBootloader(addr)

        // 2) GET_UP_LOAD_ATRIBUTE
        lastMaxChunkInfo = updaterV3.getUploadAttribute(addr)

        // 3) CHECK_NEW_FW
        val checkStatus = updaterV3.checkNewFirmware(addr, fileItem)
        if (checkStatus != PreferenceKeysUbi4.CheckNewFwStatus.NEW_FW_ACCEPT) {
            progressDialog?.dismiss()
            main?.showToast("Модуль не готов к записи (status=$checkStatus)")
            return false
        }

        // 4) PRELOAD_INFO с размером прошивки
        val fwSize = updaterV3.getFirmwarePayloadSize(fileItem.file)
        val preloadOk = updaterV3.preloadFlash(addr, fwSize)
        if (!preloadOk) {
            progressDialog?.dismiss()
            main?.showToast("Не удалось подготовить память для прошивки")
            return false
        }

        val delayMs = lastMaxChunkInfo?.flashClearDelayMs?.toLong() ?: 0L
        Log.d("FW_FLOW_V3", "Waiting $delayMs ms for flash clear")
        delay(delayMs)

        // 5) LOAD_NEW_FW чанками
        lastMaxChunkInfo?.let { info ->
            updaterV3.sendFirmwareWithProgress(addr, fileItem.file, info) { offset, total ->
                updateProgress(progressBar, offset, total)
            }
        }

        // 6) CALCULATE_CRC + COMPLITE_CRC
        val crcOk = updaterV3.checkFirmwareCrcAndCompleteUpdate(addr)
        if (!crcOk) {
            progressDialog?.dismiss()
            main?.showToast("CRC mismatch! Обновление не удалось.")
            return false
        }

        return true
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
