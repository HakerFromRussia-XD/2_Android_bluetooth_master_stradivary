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
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.runProgramTypeFlow
import com.bailout.stickk.ubi4.data.state.FirmwareInfoState.startSystemUpdateFlow
import com.bailout.stickk.ubi4.models.FirmwareFileItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentUBI4.BootloaderBoardItemUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DialogManager(
    private val context: Context,
    private val layoutInflater: LayoutInflater,
    private val viewLifecycleOwner: LifecycleOwner,
    private val onDisconnectConfirmed: () -> Unit,
    ) {

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
            window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            show()
        }

        view.findViewById<View>(R.id.ubi4DialogSendFirmwareCancelBtn)
            .setOnClickListener { dlg.dismiss() }

        view.findViewById<View>(R.id.ubi4DialogConfirmSendFirmwareBtn)
            .setOnClickListener {
                val addr = board.deviceAddress
                Log.d("FW_FLOW", "CONFIRM  addr=$addr (${board.boardName})")

                viewLifecycleOwner.lifecycleScope.launch {
// 1) Запускаем системное обновление
                    Log.d("FW_FLOW", "TX START_SYSTEM_UPDATE")
                    main?.bleCommandWithQueue(
                        BLECommands.requestStartSystemUpdate(),
                        MAIN_CHANNEL, WRITE
                    ) { }
                    val rawStatus = startSystemUpdateFlow.first()
                    val startStatus = PreferenceKeysUBI4.StartSystemUpdateStatus.from(rawStatus)
                    Log.d("FW_FLOW", "RX START_SYSTEM_UPDATE status=$startStatus")
                    if (startStatus != PreferenceKeysUBI4.StartSystemUpdateStatus.NEW_FW_ACCEPT) {
                        Toast.makeText(context,
                            "Не удалось начать обновление (status=$startStatus)",
                            Toast.LENGTH_LONG
                        ).show()
                        dlg.dismiss()
                        return@launch
                    }

                    // 2) Читаем RUN_PROGRAM_TYPE
                    suspend fun readRunType(): PreferenceKeysUBI4.RunProgramType {
                        Log.d("FW_FLOW", "TX GET_RUN_PROGRAM_TYPE")
                        main?.bleCommandWithQueue(
                            BLECommands.requestRunProgramType(addr.toByte()),
                            MAIN_CHANNEL, WRITE
                        ) { }
                        return runProgramTypeFlow
                            .filter { it.first == addr }
                            .map    { it.second }
                            .first()
                    }

                    val initial = readRunType()
                    Log.d("FW_FLOW", "RX initial status = $initial")

                    // 3) Если уже в загрузчике — сразу получаем инфо
                    if (initial == PreferenceKeysUBI4.RunProgramType.BOOTLOADER) {
                        Log.d("FW_FLOW", "Плата уже в bootloader — получаем инфо")
                    } else {
                        // иначе прыгаем в загрузчик
                        Log.d("FW_FLOW", "TX JUMP_TO_BOOTLOADER")
                        main?.bleCommandWithQueue(
                            BLECommands.jumpToBootloader(addr.toByte()),
                            MAIN_CHANNEL, WRITE
                        ) { }
                        delay(800)
                        Log.d("FW_FLOW", "TX GET_RUN_PROGRAM_TYPE (после delay)")
                        main?.bleCommandWithQueue(
                            BLECommands.requestRunProgramType(addr.toByte()),
                            MAIN_CHANNEL, WRITE
                        ) { }
                        runProgramTypeFlow
                            .filter { it.first == addr && it.second == PreferenceKeysUBI4.RunProgramType.BOOTLOADER }
                            .first()
                        Log.d("FW_FLOW", "BOOTLOADER готов")
                    }

                    // 4) Запрашиваем информацию о загрузчике
                    Log.d("FW_FLOW", "TX GET_BOOTLOADER_INFO")
                    main?.bleCommandWithQueue(
                        BLECommands.getBootloaderInfo(addr.toByte()),
                        MAIN_CHANNEL, WRITE
                    ) { }

                    // 5) Ждём и обрабатываем ответ GET_BOOTLOADER_INFO
                    val infoPayload = FirmwareInfoState.bootloaderInfoFlow.first()
                    Log.d("FW_FLOW", "RX GET_BOOTLOADER_INFO payload=$infoPayload")
                    // здесь можно распарсить байты из infoPayload по вашему enum’у

                    // 6) И сразу после этого – CHECK_NEW_FW
                    Log.d("FW_FLOW", "TX CHECK_NEW_FW")
                    main?.bleCommandWithQueue(
                        BLECommands.requestCheckNewFw(addr.toByte()),
                        MAIN_CHANNEL, WRITE
                    ) { }
                    val raw = FirmwareInfoState.checkNewFwFlow.first()
                    val status = PreferenceKeysUBI4.CheckNewFwStatus.from(raw)
                    Log.e("FW_FLOW", "CHECK_NEW_FW ← raw=$raw mapped=$status")
                    if (status != PreferenceKeysUBI4.CheckNewFwStatus.NEW_FW_ACCEPT) {
                        Toast.makeText(context,
                            "Модуль не готов к записи (status=$status)",
                            Toast.LENGTH_LONG
                        ).show()
                        dlg.dismiss()
                        return@launch
                    }

                    // 7) Все проверки пройдены — шлём файл
                    dlg.dismiss()
                    onConfirm(fileItem)
                }

            }
    }
}
