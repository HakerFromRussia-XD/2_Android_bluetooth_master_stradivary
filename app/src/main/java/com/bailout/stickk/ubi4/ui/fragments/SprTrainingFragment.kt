package com.bailout.stickk.ubi4.ui.fragments

import android.annotation.SuppressLint
import android.app.Dialog
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
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.navigator
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.models.FileItem
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.graphThreadFlag
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.updateFlow
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.jvm.internal.impl.incremental.components.Position


class SprTrainingFragment : Fragment() {
    private lateinit var binding: Ubi4FragmentSprTrainingBinding
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()
    private var onChangeState: ((state: Int) -> Unit)? = null
    private var onDestroyParent: (() -> Unit)? = null
    private var currentDialog: Dialog? = null
    private val progressFlow = MutableStateFlow(0)
    //private val loadedFiles = mutableSetOf<String>()


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
//        widgetListUpdater()
        //фейковые виджеты
//        adapterWidgets = initAdapter()
        adapterWidgets.swapData(mDataFactory.fakeData())


        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener { refreshWidgetsList() }



        binding.sprTrainingRv.layoutManager = LinearLayoutManager(context)
        binding.sprTrainingRv.adapter = adapterWidgets
        Log.d("SprTrainingFragment", "onViewCreated finished")
        return binding.root
    }


    private fun refreshWidgetsList() {
        graphThreadFlag = false
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


    // private fun initAdapter(): CompositeDelegateAdapter? {

    private var adapterWidgets: CompositeDelegateAdapter = CompositeDelegateAdapter(
        PlotDelegateAdapter(
            plotIsReadyToData = { num -> System.err.println("plotIsReadyToData $num") }
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
            }
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
            onShowFileClick = { showFilesDialog() },
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
    }

    @SuppressLint("MissingInflatedId")
    fun showConfirmTrainingDialog(confirmClick: () -> Unit) {
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
        val dialogFileBinding = layoutInflater.inflate(R.layout.ubi4_dialog_confirm_loading, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogFileBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        currentDialog = myDialog
        myDialog.show()

        val confirmBtn = dialogFileBinding.findViewById<View>(R.id.ubi4DialogConfirmLoadingBtn)
        confirmBtn.setOnClickListener {
            closeCurrentDialog()
            onConfirm()

            val cancelBtn = dialogFileBinding.findViewById<View>(R.id.ubi4DialogLoadingCancelBtn)
            cancelBtn.setOnClickListener {
                closeCurrentDialog()
            }

        }

    }

    private fun showProgressBarDialog(): Dialog {
        Log.d("Dialog", "Showing progress bar dialog")
        val dialogBinding = layoutInflater.inflate(R.layout.ubi4_dialog_progressbar, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        currentDialog = myDialog
        myDialog.show()
        return myDialog
    }

    private fun showFilesDialog() {
        val dialogFileBinding = layoutInflater.inflate(R.layout.ubi4_dialog_show_files, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogFileBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        val filesRecyclerView = dialogFileBinding.findViewById<RecyclerView>(R.id.dialogFileRv)
        val path = requireContext().getExternalFilesDir(null)
        val files = path?.listFiles()?.filter { it.name.contains("checkpoint") } ?: emptyList()
        //val progressBar = dialogFileBinding.findViewById<ProgressBar>(R.id.loadingProgressBar)

        if (files.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_saved_files), Toast.LENGTH_SHORT)
                .show()
            myDialog.dismiss()
            return
        }

        val fileItems = files.map { FileItem(it.name, it) }.toMutableList()

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
//                if (loadedFiles.contains(fileItem.name)) {
//                    Toast.makeText(requireContext(), "Этот файл уже загружен", Toast.LENGTH_SHORT).show()
//                    return
//                }
                showConfirmLoadingDialog {
                    val progressBarDialog = showProgressBarDialog()

                    val progressBar =
                        progressBarDialog.findViewById<ProgressBar>(R.id.loadingProgressBar)

                    lifecycleScope.launch {
                        progressFlow.collect { progress ->
                            withContext(Main) { // Убедитесь, что обновление происходит в UI-потоке
                                progressBar.progress = progress
                                Log.d("ProgressValue", "$progress")
                                if (progress == 100) {
                                    Toast.makeText(
                                        requireContext(),
                                        "Файл отправлен: ${fileItem.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    //loadedFiles.add(fileItem.name)
                                    closeCurrentDialog()
                                    progressFlow.value = 0

                                }
                            }
                        }
                    }

                    sendFileInChunks(fileItem.file.readBytes())
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


    private fun sendFileInChunks(byteArray: ByteArray) {
        val maxChunkSize = 249
        val totalChunks = (byteArray.size + maxChunkSize - 1) / maxChunkSize
        Log.d("TotalChunks", "totalChunks $totalChunks")
        //progressFlow.value = 0
        val chunksSent = AtomicInteger(0)

        lifecycleScope.launch(Dispatchers.IO) {
            byteArray.asList().chunked(maxChunkSize).forEachIndexed { index, chunk ->
                val chunkArray = chunk.toByteArray().toMutableList()
                chunkArray[0] = index.toByte()
                val modifiedChunkArray = chunkArray.toByteArray()

                // Отправка данных (выполняется в фоновом потоке)
                main?.runWriteDataTest(modifiedChunkArray, MAIN_CHANNEL, WRITE) {
                    val sent = chunksSent.incrementAndGet()
                    val progress = ((sent.toDouble() / totalChunks) * 100).toInt()
                    progressFlow.value = progress
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d("LagSpr", " started onDestroy")
        onDestroyParent?.invoke()
        //adapterWidgets = null
    }

}