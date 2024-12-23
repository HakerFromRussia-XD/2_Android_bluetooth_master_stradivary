package com.bailout.stickk.ubi4.ui.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentSprTrainingBinding
import com.bailout.stickk.ubi4.adapters.dialog.FileCheckpointAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.OneButtonDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.PlotDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.SliderDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.SwitcherDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.TrainingFragmentDelegateAdapter
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.BLEController
import com.bailout.stickk.ubi4.ble.BluetoothLeService
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.navigator
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.models.FileItem
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.canSendNextChunkFlagFlow
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.graphThreadFlag
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.listWidgets
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.updateFlow
import com.bailout.stickk.ubi4.utility.ConstantManager
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.jvm.internal.impl.incremental.components.Position


class SprTrainingFragment : Fragment() {
    private lateinit var binding: Ubi4FragmentSprTrainingBinding
    private lateinit var bleController: BLEController
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()
    private var onDestroyParent: (() -> Unit)? = null
    private var currentDialog: Dialog? = null
    private var loadingCurrentDialog: Dialog? = null


    private val progressFlow = MutableStateFlow(0)
    private var canSendNextChunkFlag = true

    //private val loadedFiles = mutableSetOf<String>()
    private var onDestroyParentCallbacks = mutableListOf<() -> Unit>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("LagSpr", "onCreateView started")
        binding = Ubi4FragmentSprTrainingBinding.inflate(inflater, container, false)
        if (activity != null) {
            main = activity as MainActivityUBI4?

        }


        //настоящие виджеты
        widgetListUpdater()
        //фейковые виджеты
//        adapterWidgets = initAdapter()
//        adapterWidgets.swapData(mDataFactory.fakeData())

        canSendNextChunkFlagUpdater()
        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener { refreshWidgetsList() }


        binding.sprTrainingRv.layoutManager = LinearLayoutManager(context)
        binding.sprTrainingRv.adapter = adapterWidgets
        bleController = (requireActivity() as MainActivityUBI4).getBLEController()
        return binding.root
    }


    private fun refreshWidgetsList() {
        graphThreadFlag = false
        listWidgets.clear()
        onDestroyParentCallbacks.forEach { it.invoke() }
        onDestroyParentCallbacks.clear()
        transmitter().bleCommand(BLECommands.requestInicializeInformation(), MAIN_CHANNEL, WRITE)
    }

    private fun widgetListUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            withContext(Default) {
                updateFlow.collect { value ->
                    main?.runOnUiThread {
                        Log.d("widgetListUpdater", "${mDataFactory.prepareData(2)}")
                        adapterWidgets?.swapData(mDataFactory.prepareData(2))
                        binding.refreshLayout.setRefreshing(false)
                    }
                }
            }
        }
    }

    private fun canSendNextChunkFlagUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            canSendNextChunkFlagFlow.collect { value ->
                canSendNextChunkFlag = value
            }
        }
    }


    // private fun initAdapter(): CompositeDelegateAdapter? {

    private var adapterWidgets: CompositeDelegateAdapter = CompositeDelegateAdapter(
        PlotDelegateAdapter(
            plotIsReadyToData = { num -> System.err.println("plotIsReadyToData $num") },
            onDestroyParent = { onDestroyParent -> onDestroyParentCallbacks.add(onDestroyParent) }
        ),
        OneButtonDelegateAdapter(
            onButtonPressed = { addressDevice, parameterID, command ->
                oneButtonPressed(
                    addressDevice,
                    parameterID,
                    command
                )
            },
            onButtonReleased = { addressDevice, parameterID, command ->
                oneButtonReleased(
                    addressDevice,
                    parameterID,
                    command
                )
            },
            onDestroyParent = { onDestroyParent -> onDestroyParentCallbacks.add(onDestroyParent) }
        ),

        TrainingFragmentDelegateAdapter(
            onConfirmClick = {
                if (isAdded) {
                    Log.d("StateCallBack", "onConfirmClick: Button clicked")
                    showConfirmTrainingDialog {
                        navigator().showMotionTrainingScreen {
                            main?.manageTrainingLifecycle()
                        }
                    }
                } else {
                    Log.e("StateCallBack", "Fragment is not attached to activity")
                }
            },
            onShowFileClick = { addressDevice -> showFilesDialog(addressDevice,6) },
            onDestroyParent = { onDestroyParent -> this.onDestroyParent = onDestroyParent },
        ),
        SwitcherDelegateAdapter(
            onSwitchClick = { addressDevice, parameterID, switchState ->
                sendSwitcherState(addressDevice, parameterID, switchState)
            },
            onDestroyParent = { onDestroyParent -> this.onDestroyParent = onDestroyParent }
        ),
        SliderDelegateAdapter(
            onSetProgress = { addressDevice, parameterID, progress ->
                sendSliderProgress(
                    addressDevice,
                    parameterID,
                    progress
                )
            },
            //TODO решение сильно под вопросом, потому что колбек будет перезаписываться и скорее всего вызовется только у одного виджета
            onDestroyParent = { onDestroyParent -> this.onDestroyParent = onDestroyParent }
        )
    )

    private fun closeCurrentDialog() {
        currentDialog?.dismiss()
        currentDialog = null
        loadingCurrentDialog?.dismiss()
        loadingCurrentDialog = null
    }


    @SuppressLint("MissingInflatedId")
    fun showConfirmTrainingDialog(confirmClick: () -> Unit) {
        if (currentDialog != null && currentDialog?.isShowing == true) {
            return
        }
        val dialogBinding = layoutInflater.inflate(R.layout.ubi4_dialog_confirm_training, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        currentDialog = myDialog
        myDialog.show()

        val cancelBtn = dialogBinding.findViewById<View>(R.id.ubi4DialogTrainingCancelBtn)
        cancelBtn.setOnClickListener {
            closeCurrentDialog()
        }

        val confirmBtn = dialogBinding.findViewById<View>(R.id.ubi4DialogConfirmTrainingBtn)
        confirmBtn.setOnClickListener {
            myDialog.dismiss()
            closeCurrentDialog()
            confirmClick()
        }

    }

    private fun showConfirmLoadingDialog(onConfirm: () -> Unit) {
        if (loadingCurrentDialog != null && loadingCurrentDialog?.isShowing == true) {
            return
        }
        closeCurrentDialog()
        val dialogFileBinding = layoutInflater.inflate(R.layout.ubi4_dialog_confirm_loading, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogFileBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        loadingCurrentDialog = myDialog
        myDialog.show()

        val confirmBtn = dialogFileBinding.findViewById<View>(R.id.ubi4DialogConfirmLoadingBtn)
        confirmBtn.setOnClickListener {
            closeCurrentDialog()
            onConfirm()
            Log.d("DialogManagement", "After onConfirm, current dialog: $currentDialog")

        }
        val cancelBtn = dialogFileBinding.findViewById<View>(R.id.ubi4DialogLoadingCancelBtn)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
        }

    }

    private fun showProgressBarDialog(): Dialog {
        closeCurrentDialog()
        val dialogBinding = layoutInflater.inflate(R.layout.ubi4_dialog_progressbar, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        currentDialog = myDialog
        myDialog.show()
        bleController.setProgressDialog(myDialog)
        return myDialog
    }

    private fun showFilesDialog(addressDevice: Int, parameterID: Int) {
        if (currentDialog != null && currentDialog?.isShowing == true) {
            return
        }
        val dialogFileBinding = layoutInflater.inflate(R.layout.ubi4_dialog_show_files, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogFileBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        currentDialog = myDialog
        myDialog.show()

        val filesRecyclerView = dialogFileBinding.findViewById<RecyclerView>(R.id.dialogFileRv)
        val path = requireContext().getExternalFilesDir(null)

        val regex = Regex("^checkpoint_№(\\d+)_\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}$")

        val files = path?.listFiles()?.filter {
            it.name.startsWith("checkpoint_№") &&
                    regex.matches(it.name)
        } ?: emptyList()

        if (files.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_saved_files), Toast.LENGTH_SHORT)
                .show()
            myDialog.dismiss()
            return
        }


//        val fileItems = files.map { FileItem(it.name, it) }.toMutableList()

        val fileItems = files.mapNotNull { file ->
            val matchResult = regex.find(file.name)
            matchResult?.let {
                val number = it.groupValues[1].toIntOrNull()
                if (number != null) {
                    FileItem("checkpoint №$number", file, number)
                } else {
                    null
                }
            }
        }.toMutableList()

        filesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        val adapter = FileCheckpointAdapter(fileItems, object :
            FileCheckpointAdapter.OnFileActionListener {
            override fun onDelete(position: Int, fileItem: FileItem) {
                if (fileItem.file.delete()) {
                    Toast.makeText(
                        requireContext(),
                        "Файл ${fileItem.name} удалён",
                        Toast.LENGTH_SHORT
                    ).show()
                    fileItems.remove(fileItem)
                    filesRecyclerView.adapter?.notifyDataSetChanged()
                    if (fileItems.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "Нет сохранённых файлов",
                            Toast.LENGTH_SHORT
                        ).show()
                        myDialog.dismiss()
                    }
                } else {
                    Toast.makeText(requireContext(), "Ошибка удаления файла", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onSelect(position: Int, fileItem: FileItem, onComplete: () -> Unit) {
                if (!bleController.getStatusConnected()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.there_is_no_bluetooth_connection_check_the_connection),
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val dateTimeRegex = Regex("_(\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2})$")
                val match = dateTimeRegex.find(fileItem.file.name)
                val dateTimeStr = match?.groupValues?.get(1) ?: run {
                    Toast.makeText(
                        requireContext(),
                        "Не удалось извлечь дату/время из имени файла",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                val paramFileName = "params_$dateTimeStr.bin"
                val paramFile = File(requireContext().getExternalFilesDir(null), paramFileName)
                if (!paramFile.exists()) {
                    Toast.makeText(
                        requireContext(),
                        "Соответствующий params файл не найден",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                showConfirmLoadingDialog {
                    if (bleController.isCurrentlyUploading()) {
                        Toast.makeText(
                            requireContext(),
                            "Загрузка уже выполняется",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@showConfirmLoadingDialog
                    }
                    Log.d("DialogManagement", "Loading confirmed. Opening progress bar dialog.")
                    val progressBarDialog = showProgressBarDialog()
                    val progressBar =
                        progressBarDialog.findViewById<ProgressBar>(R.id.loadingProgressBar)

                    var sendingCheckpoint = true
                    var sendingParams = false
                    var isCompleted = false

                    val job = lifecycleScope.launch {
                        progressFlow.collect { progress ->
                            withContext(Main) {
                                when {
                                    sendingCheckpoint -> {
                                        progressBar.progress = progress
                                        Log.d(
                                            "CheckpointSend",
                                            "Прогресс отправки чекпоинта: $progress%"
                                        )

                                        if (progress == 100) {
                                            closeCurrentDialog()
                                            progressFlow.value = 0
                                            sendingCheckpoint = false
                                            sendingParams = true
                                            progressFlow.value = 0
                                            Log.d(
                                                "ParamsSend",
                                                "Начинаем отправку файла params: $paramFileName"
                                            )
                                            sendFileInChunks(paramFile.readBytes(), ConstantManager.PARAMS_BIN_NAME, addressDevice, parameterID)


                                        }
                                    }

                                    sendingParams -> {
                                        Log.d("ParamsSend", "Прогресс отправки params: $progress%")
                                        if (progress == 100 && !isCompleted) {
                                            Log.d("ParamsSend", "Файл params отправлен успешно!")
                                            Toast.makeText(
                                                requireContext(),
                                                "Файлы отправлены: ${fileItem.name} и $paramFileName",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            isCompleted = true
                                            sendingParams = false
                                            this@launch.cancel()


                                        }

                                    }
                                }
                            }
                        }
                    }

                    sendFileInChunks(fileItem.file.readBytes(), ConstantManager.CHECKPOINT_NAME, addressDevice, parameterID )
                }
            }

        })
        filesRecyclerView.adapter = adapter

        val cancelBtn = dialogFileBinding.findViewById<View>(R.id.dialogFileCancelBtn)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
        }


    }


    private fun oneButtonPressed(addressDevice: Int, parameterID: Int, command: Int) {
        System.err.println("oneButtonPressed    parameterID: $parameterID   command: $command")
        transmitter().bleCommand(
            BLECommands.sendOneButtonCommand(addressDevice, parameterID, command),
            MAIN_CHANNEL,
            WRITE
        )
    }

    private fun oneButtonReleased(addressDevice: Int, parameterID: Int, command: Int) {
        System.err.println("oneButtonReleased    parameterID: $parameterID   command: $command")

        transmitter().bleCommand(
            BLECommands.sendOneButtonCommand(addressDevice, parameterID, command),
            MAIN_CHANNEL,
            WRITE
        )
    }

    private fun sendSliderProgress(addressDevice: Int, parameterID: Int, progress: Int) {
        Log.d(
            "sendSliderProgress",
            "addressDevice=$addressDevice  parameterID: $parameterID  progress = $progress"
        )
        transmitter().bleCommand(
            BLECommands.sendSliderCommand(
                addressDevice,
                parameterID,
                progress
            ), MAIN_CHANNEL, WRITE
        )
    }

    private fun sendSwitcherState(addressDevice: Int, parameterID: Int, switchState: Boolean) {
        Log.d(
            "sendSwitcherCommand",
            "addressDevice=$addressDevice  parameterID: $parameterID  command = $switchState"
        )
        transmitter().bleCommand(
            BLECommands.sendSwitcherCommand(
                addressDevice,
                parameterID,
                switchState
            ), MAIN_CHANNEL, WRITE
        )

    }


    private fun sendFileInChunks(byteArray: ByteArray, name: String, addressDevice: Int, parameterID: Int,) {
        val maxChunkSize = 100 // max 249
        val totalChunks = (byteArray.size + maxChunkSize - 1) / maxChunkSize
        val chunksSent = AtomicInteger(0)
        bleController.setUploadingState(true)
        lifecycleScope.launch(Dispatchers.IO) {
            var indexPackage = 0
            while (!canSendNextChunkFlag) {
                delay(10)
            }
            canSendNextChunkFlag = false
            main?.bleCommandWithQueue(BLECommands.openCheckpointFileInSDCard(name,addressDevice, parameterID, 1), MAIN_CHANNEL, WRITE){}

            byteArray.asList().chunked(maxChunkSize).forEachIndexed { index, chunk ->
                if (!bleController.isCurrentlyUploading()) {
                    Log.d("SprTrainingFragment", "Upload canceled due to BLE disconnection.")
                    progressFlow.value = 0
                    return@launch
                }
                indexPackage = index

                while (!canSendNextChunkFlag) {
                    delay(10)
                }
                canSendNextChunkFlag = false
                // Отправка данных
                main?.bleCommandWithQueue(
                    BLECommands.writeDataInCheckpointFileInSDCard(chunk.toByteArray(),addressDevice, parameterID, index +2),
                    MAIN_CHANNEL,
                    WRITE
                ) {
                    val sent = chunksSent.incrementAndGet()
                    val progress = ((sent.toDouble() / totalChunks) * 100).toInt()
                    progressFlow.value = progress
                    Log.d("ChunkProcessing", "Progress: $progress% ($sent/$totalChunks chunks sent)")
                }
//                sleep(50)
            }

            while (!canSendNextChunkFlag) {
                delay(10)
            }
            canSendNextChunkFlag = false
            main?.bleCommandWithQueue(BLECommands.closeCheckpointFileInSDCard(addressDevice, parameterID, indexPackage + 3 ),
                MAIN_CHANNEL, WRITE) {}
            bleController.setUploadingState(false)
            Log.d("ChunkProcessing", "Total chunks to send: $totalChunks")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        onDestroyParentCallbacks.forEach { it.invoke() }
    }

}