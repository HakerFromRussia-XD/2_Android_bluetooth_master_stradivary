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

                    Log.d("FW_FLOW", "TX START_SYSTEM_UPDATE")
                    main?.bleCommandWithQueue(
                        BLECommands.requestStartSystemUpdate(),
                        MAIN_CHANNEL, WRITE
                    ) {  }
                    val rawStatus = startSystemUpdateFlow.first()
                    val status = PreferenceKeysUBI4.StartSystemUpdateStatus.from(rawStatus)
                    Log.d("FW_FLOW", "RX START_SYSTEM_UPDATE status=$status")
                    if (status != PreferenceKeysUBI4.StartSystemUpdateStatus.NEW_FW_ACCEPT) {
                        Log.e("FW_FLOW", "Не удалось запустить системное обновление, статус=$status")
                        Toast.makeText(context,
                            "Не удалось начать обновление (status=$status)",
                            Toast.LENGTH_LONG
                        ).show()
                        dlg.dismiss()
                        return@launch
                    }
                    // функция, которая шлёт GET_RUN_PROGRAM_TYPE и читает первый пришедший статус
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

                    // шаг 1: прочитать начальный статус
                    val initial = readRunType()
                    Log.d("FW_FLOW", "RX initial status = $initial")

                    // если плата уже в bootloader — сразу выходим и вызываем onConfirm
                    if (initial == PreferenceKeysUBI4.RunProgramType.BOOTLOADER) {
                        main?.bleCommandWithQueue(BLECommands.getBootloaderInfo(addr.toByte()), MAIN_CHANNEL, WRITE) { }
                        Log.d("FW_FLOW", "Плата уже в bootloader — сразу запускаем прошивку")
                        dlg.dismiss()
                        onConfirm(fileItem)
                        return@launch
                    }

                    // иначе — переводим в bootloader
                    Log.d("FW_FLOW", "TX JUMP_TO_BOOTLOADER")
                    main?.bleCommandWithQueue(
                        BLECommands.jumpToBootloader(addr.toByte()),
                        MAIN_CHANNEL, WRITE
                    ) { }

                    // даём плате время перезагрузиться
                    delay(800)

                    // шаг 2: опрашиваем, пока не получим BOOTLOADER
                    Log.d("FW_FLOW", "TX GET_RUN_PROGRAM_TYPE (после delay)")
                    main?.bleCommandWithQueue(
                        BLECommands.requestRunProgramType(addr.toByte()),
                        MAIN_CHANNEL, WRITE
                    ) { }

                    Log.d("FW_FLOW", "Ждём BOOTLOADER...")
                    runProgramTypeFlow
                        .filter { it.first == addr && it.second == PreferenceKeysUBI4.RunProgramType.BOOTLOADER }
                        .first()
                    Log.d("FW_FLOW", "BOOTLOADER готов")
                    Log.d("FW_FLOW", "TX GET_BOOTLOADER_INFO")
                    main?.bleCommandWithQueue(BLECommands.getBootloaderInfo(addr.toByte()), MAIN_CHANNEL, WRITE) { }
                    // всё готово — закрываем окно и исполняем прошивку
                    dlg.dismiss()
                    onConfirm(fileItem)
                }

            }
    }
}
