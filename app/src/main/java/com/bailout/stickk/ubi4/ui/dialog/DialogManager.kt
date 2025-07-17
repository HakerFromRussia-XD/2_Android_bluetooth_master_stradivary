package com.bailout.stickk.ubi4.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.BleFirmwareUpdater
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.bootloaderStatusFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.preloadInfoFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.runProgramTypeFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.startSystemUpdateFlow
import com.bailout.stickk.ubi4.models.FirmwareFileItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.local.MaxChunkSizeInfo
import com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentUBI4.BootloaderBoardItemUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.firmware.FirmwareUpdateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

class DialogManager(
    private val context: Context,
    private val layoutInflater: LayoutInflater,
    private val viewLifecycleOwner: LifecycleOwner,
    private val onDisconnectConfirmed: () -> Unit,
    ) {
    private val updater = BleFirmwareUpdater()
    private var lastMaxChunkInfo: MaxChunkSizeInfo? = null
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

    @SuppressLint("LogNotTimber")
    fun showConfirmSendFirmwareFileDialog(
        board: BootloaderBoardItemUBI4,
        fileItem: FirmwareFileItem,
        onConfirm: (FirmwareFileItem) -> Unit
    ) {
        val view = layoutInflater.inflate(
            R.layout.ubi4_dialog_confirm_send_firmware_file, null
        )
        val dlg = Dialog(context).apply {
            setContentView(view)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            show()
        }

        view.findViewById<View>(R.id.ubi4DialogSendFirmwareCancelBtn)
            .setOnClickListener { dlg.dismiss() }

        view.findViewById<View>(R.id.ubi4DialogConfirmSendFirmwareBtn)
            .setOnClickListener {
                val addr = board.deviceAddress
                Log.d("FW_FLOW", "CONFIRM addr=$addr (${board.boardName})")

                viewLifecycleOwner.lifecycleScope.launch {
                    // 1) START_SYSTEM_UPDATE
                    val startStatus = updater.startSystemUpdate()
                    if (startStatus != PreferenceKeysUBI4.StartSystemUpdateStatus.NEW_FW_ACCEPT) {
                        main?.showToast("Не удалось начать обновление (status=$startStatus)")
                        dlg.dismiss()
                        return@launch
                    }

                    // 2) ENSURE BOOTLOADER
                    updater.ensureBootloader(addr)

                    // 3) GET_BOOTLOADER_INFO
                    val info = updater.getBootloaderInfo(addr)

                    // 4) CHECK_NEW_FW
                    val checkStatus = updater.checkNewFirmware(addr, fileItem)
                    if (checkStatus != PreferenceKeysUBI4.CheckNewFwStatus.NEW_FW_ACCEPT) {
                        main?.showToast("Модуль не готов к записи (status=$checkStatus")
                        return@launch
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
                        Log.d("FW_FLOW_DIALOG_MANAGER", "→ sendFirmware(addr=$addr, fwSize=${fileItem.file}, chunkSize=${info.chunkSize}, bytesInterval=${info.bytesInterval}, timeoutMs=${info.timeoutMs})")
                        updater.sendFirmware(addr, fileItem.file, info)
                    }
                    // 9) Проверка CRC и финализация
                    val crcOk = updater.checkFirmwareCrcAndCompleteUpdate(addr)
                    if (!crcOk) {
                        main?.showToast("CRC mismatch! Обновление не удалось.")
                        return@launch
                    }
                    //10 finish
                    updater.finishSystemUpdate(addr)
                    main?.showToast("Обновление успешно завершено!")
                    dlg.dismiss()
                    onConfirm(fileItem)
                }
            }
    }
}


