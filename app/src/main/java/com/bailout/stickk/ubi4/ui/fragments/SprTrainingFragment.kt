package com.bailout.stickk.ubi4.ui.fragments
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context.MODE_PRIVATE
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentSprTrainingBinding
import com.bailout.stickk.ubi4.adapters.dialog.Emg8FilesCheckAdapter
import com.bailout.stickk.ubi4.adapters.dialog.FileCheckpointAdapter
import com.bailout.stickk.ubi4.adapters.dialog.OnCheckEmg8FileListener
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.BLEController
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.network.PassportRetrofitInstance
import com.bailout.stickk.ubi4.data.repository.Ubi4TrainingRepository
import com.bailout.stickk.ubi4.data.state.UiState.updateFlow
import com.bailout.stickk.ubi4.models.Emg8FileItem
import com.bailout.stickk.ubi4.models.widgets.FileItem
import com.bailout.stickk.ubi4.models.widgets.PlatformFile
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ARG_LAST_EMG8
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.data.state.FlagState.canSendNextChunkFlagFlow
import com.bailout.stickk.ubi4.ui.fragments.base.BaseWidgetsFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.utility.BaseUrlUtilsUBI4.API_KEY
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import com.bailout.stickk.ubi4.utility.TrainingUploadManager
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger


class SprTrainingFragment: BaseWidgetsFragment() {
    private lateinit var binding: Ubi4FragmentSprTrainingBinding
    private lateinit var bleController: BLEController
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()
    private var currentDialog: Dialog? = null
    private var loadingCurrentDialog: Dialog? = null
    private var chunksSend = AtomicInteger(0)
    private var warningDialog: Dialog? = null
    private var progressDialog: Dialog? = null

    private var canSendNextChunkFlag = true
    private var sendFileSuccessFlag = true
    private var autoDialogShown = false

    private val display = 3

    private val repo = Ubi4TrainingRepository(PassportRetrofitInstance.api)

    private val prefs by lazy { requireContext().getSharedPreferences(PreferenceKeysUBI4.NAME, MODE_PRIVATE) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("LagSpr", "onCreateView started")
        binding = Ubi4FragmentSprTrainingBinding.inflate(inflater, container, false)
        if (activity != null) {
            main = activity as MainActivityUBI4?

        }


        //настоящие виджеты
        widgetListUpdater()
        adapterWidgets.swapData(mDataFactory.prepareData(display))
        //фейковые виджеты
//        adapterWidgets.swapData(mDataFactory.fakeData())

        canSendNextChunkFlagUpdater()
        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener { refreshWidgetsList() }


        binding.sprTrainingRv.layoutManager = LinearLayoutManager(context)
        binding.sprTrainingRv.adapter = adapterWidgets
        bleController = (requireActivity() as MainActivityUBI4).getBLEController()
        // подписка на прогресс обучения модели
//        subscribeTrainingUpload()
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!autoDialogShown) {
            val lastEmg8 = arguments?.getString(ARG_LAST_EMG8)
            if (lastEmg8 != null) {                         // ← ключевое условие
                view.post {
                    showModelEmg8FilesDialog(lastEmg8) { selected ->
                        startUploadSelectedTrainingFiles(selected)
                    }
                }

                autoDialogShown = true
                arguments?.remove(ARG_LAST_EMG8)
            }
        }


    }

    private fun widgetListUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Main) {
            withContext(Default) {
                updateFlow.collect {
                    main?.runOnUiThread {
                        Log.d("widgetListUpdater", "${mDataFactory.prepareData(display)}")
                        adapterWidgets.swapData(mDataFactory.prepareData(display))
                        binding.refreshLayout.setRefreshing(false)
                    }
                }
            }
        }
    }

    private fun canSendNextChunkFlagUpdater() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            canSendNextChunkFlagFlow.collect { value ->
                Log.d("BLEFlowDebug", "Получено значение от оптики: $value, текущий chunksSend: ${chunksSend.get()}")
                if (value == chunksSend.toInt()) {
                    canSendNextChunkFlag = true
                    Log.d("BLEFlowDebug", "Соответствие пакетов: canSendNextChunkFlag установлен в true")
                } else {
                    Log.d("BLEFlowDebug", "Несоответствие пакетов: ожидается ${chunksSend.get()}, получено $value")
                }
            }
        }
    }


    @SuppressLint("MissingInflatedId")
    override fun showConfirmTrainingDialog(confirmClick: () -> Unit) {
        Log.d("StateCallBack11", "start ok")

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
            Log.d("showConfirmTrainingDialog", "cancelBtn ok")
            closeCurrentDialog()
        }


        val confirmBtn = dialogBinding.findViewById<View>(R.id.ubi4DialogConfirmTrainingBtn)
        confirmBtn.setOnClickListener {
            myDialog.dismiss()
            closeCurrentDialog()
            confirmClick()

        }
    }


    override fun showModelEmg8FilesDialog(
        preselectName: String?,
        onSendClick: (selectedFiles: List<File>) -> Unit
    ) {
        val dialogBinding = LayoutInflater.from(requireContext())
            .inflate(R.layout.ubi4_dialog_model_emg8_files, null)
        val dlg = Dialog(requireContext()).apply {
            setContentView(dialogBinding)
            setCancelable(false)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            show()
        }

        // Собираем список .emg8
        val dir   = requireContext().getExternalFilesDir(null)
        val files = dir?.listFiles()?.filter { it.extension == "emg8" } ?: emptyList()

        val items = files.map { f ->
            Emg8FileItem(f, f.name == preselectName)         // авто-галочка
        }.toMutableList()

        // RecyclerView + адаптер
        val rv = dialogBinding.findViewById<RecyclerView>(R.id.dialogModelEmg8Rv).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = Emg8FilesCheckAdapter(items, object : OnCheckEmg8FileListener {
                override fun onFileClicked(pos: Int, item: Emg8FileItem) {
                    items[pos] = item.copy(isChecked = !item.isChecked)
                    adapter?.notifyItemChanged(pos)
                }
            })
        }

        // ----- Кнопка Send -----
        dialogBinding.findViewById<View>(R.id.dialogModelEmg8SendBtn).setOnClickListener {
            val selected = items.filter { it.isChecked }.map { it.file }
            if (selected.isEmpty()) {
                Toast.makeText(context, "Выберите хотя бы один файл", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dlg.dismiss()
            onSendClick(selected)               // передаём выбранные .emg8 наверх
        }

        // ----- Cancel -----
        dialogBinding.findViewById<View>(R.id.dialogModelEmg8CancelBtn)
            .setOnClickListener { dlg.dismiss() }
    }


    fun startUploadSelectedTrainingFiles(selectedEmg8: List<File>) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            uploadWithAuthRetry(selectedEmg8)
        }
    }

    /** Отправка + единоразовый retry при 401 */
    private suspend fun uploadWithAuthRetry(selectedEmg8: List<File>) {

        val prefs  = requireContext()
            .getSharedPreferences(PreferenceKeysUBI4.NAME, MODE_PRIVATE)

        var token  = prefs.getString(PreferenceKeysUBI4.KEY_TOKEN, "") ?: ""
        var serial = prefs.getString(PreferenceKeysUBI4.KEY_SERIAL, "") ?: ""

        // если пусто после холодного старта → берём дефолты
        if (token.isBlank() || serial.isBlank()) {
            serial = SERIAL_DEFAULT
            token  = repo.fetchTokenBySerial(API_KEY, serial, PASSWORD_DEFAULT)

            prefs.edit()
                .putString(PreferenceKeysUBI4.KEY_TOKEN,  token)
                .putString(PreferenceKeysUBI4.KEY_SERIAL, serial)
                .apply()

            // паспорт качаем, как и раньше
            repo.fetchAndSavePassport(token, serial, requireContext().cacheDir)
        }

        fun doUpload(bearer: String) {
            TrainingUploadManager.launch(
                requireContext(), repo, bearer, serial, selectedEmg8
            )
        }

        try {
            doUpload(token)                 // 🔹 первая попытка
        } catch (e: retrofit2.HttpException) {
            if (e.code() != 401) throw e    // не 401 → дальше

            // 🔄 401 → берём новый токен теми же константами
            val fresh = repo.fetchTokenBySerial(API_KEY, serial, PASSWORD_DEFAULT)
            prefs.edit().putString(
                PreferenceKeysUBI4.KEY_TOKEN, fresh
            ).apply()

            doUpload(fresh)                 // 🔹 вторая (успешная) попытка
        }
    }

    override fun showFilesDialog(addressDevice: Int, parameterID: Int) {
        Log.d("showFilesDialog", "parameterID = $parameterID")
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

        val regex = Regex("^checkpoint_№(\\d+)_.*\\.ckpt$")

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

        val fileItems = files.mapNotNull { file ->
            val matchResult = regex.find(file.name)
            matchResult?.let {
                val number = it.groupValues[1].toIntOrNull()
                if (number != null) {
                    FileItem("checkpoint №$number", PlatformFile(file.path), number)
                } else {
                    null
                }
            }
        }.sortedBy { it.number }.toMutableList()

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

                val dateTimeRegex = Regex("_(\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2})")
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

                    lifecycleScope.launch {
                        //TODO РАЗОБРАТЬСЯ с parameterID, нам передается параметерID = 5!!!!
                        sendFileInChunks(fileItem.file.readBytes(), ConstantManagerUBI4.CHECKPOINT_NAME, addressDevice, 6)
                        sendFileInChunks(paramFile.readBytes(), ConstantManagerUBI4.PARAMS_BIN_NAME, addressDevice, 6)
                        showWarningLoadingDialog { closeWarningDialog() }
                    }
                }
            }

        })
        filesRecyclerView.adapter = adapter

        val cancelBtn = dialogFileBinding.findViewById<View>(R.id.dialogFileCancelBtn)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
        }
    }
    private fun showProgressBarDialog(): Dialog {
        closeProgressDialog()


        val dialogBinding = layoutInflater.inflate(R.layout.ubi4_dialog_progressbar, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        progressDialog = myDialog
        myDialog.show()
        bleController.setProgressDialog(myDialog)
        return myDialog
    }
    override fun showConfirmLoadingDialog(onConfirm: () -> Unit) {
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
        }
        val cancelBtn = dialogFileBinding.findViewById<View>(R.id.ubi4DialogLoadingCancelBtn)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
        }
    }
    suspend fun showWarningLoadingDialog(onConfirm: () -> Unit) {
        mutex.withLock {
            if (!sendFileSuccessFlag) {
                if (warningDialog != null && warningDialog?.isShowing == true) {
                    return
                }

                closeWarningDialog()
                val dialogFileBinding = layoutInflater.inflate(R.layout.ubi4_dialog_warning_load_checkpoint, null)
                val myDialog = Dialog(requireContext())
                myDialog.setContentView(dialogFileBinding)
                myDialog.setCancelable(false)
                myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                myDialog.show()

                warningDialog = myDialog

                val confirmBtn = dialogFileBinding.findViewById<View>(R.id.ubi4WarningLoadingTrainingBtn)
                confirmBtn.setOnClickListener {
                    closeWarningDialog()
//                    closeCurrentDialog()
                    onConfirm()
                }
                sendFileSuccessFlag = true
            }
        }
    }

    private val mutex = Mutex()
    override suspend fun sendFileInChunks(byteArray: ByteArray, name: String, addressDevice: Int, parameterID: Int) {
        mutex.withLock {
            try {
                if (!sendFileSuccessFlag) return
                val progressBarDialog = showProgressBarDialog()
                val progressBar = progressBarDialog.findViewById<ProgressBar>(R.id.loadingProgressBar)
                val maxChunkSize = 100 // max 249
                val totalChunks = (byteArray.size + maxChunkSize - 1) / maxChunkSize
                chunksSend = AtomicInteger(0)
                bleController.setUploadingState(true) // Устанавливаем флаг загрузки
                var indexPackage = 0

                // Создание файла (или открытие с очисткой)
                val openFileSuccess = waitForFlagWithRetry(
                    maxWaitTimeMs = 2000L, // Ожидание флага 2 секунды
                    retryCount = 3,         // Максимум 3 попытки
                    chunksSend = chunksSend.incrementAndGet(),
                    totalChunks = totalChunks + 2,
                    command = 1,
                    progressBar = progressBar
                ) {
                    main?.bleCommandWithQueue(
                        BLECommands.openCheckpointFileInSDCard(
                            name,
                            addressDevice,
                            parameterID,
                            1
                        ), MAIN_CHANNEL, WRITE
                    ) {}
                }

                if (!openFileSuccess) {
                    // Ошибка при открытии файла
                    Log.d("ChunkProcessing", "Ошибка передачи чанка №${chunksSend.get()} при открытии файла")
                    bleController.setUploadingState(false)
                    sendFileSuccessFlag = false
                    closeCurrentDialog()
                    return
                }

                // Отправка данных самого файла
                byteArray.asList().chunked(maxChunkSize).forEachIndexed { index, chunk ->
                    indexPackage = index
                    if (!bleController.isCurrentlyUploading()) {
                        Log.d("SprTrainingFragment", "Upload canceled due to BLE disconnection.")
                        return
                    }

                    val sendFileSuccess = waitForFlagWithRetry(
                        maxWaitTimeMs = 2000L, // Ожидание флага 2 секунды
                        retryCount = 3,         // Максимум 3 попытки
                        chunksSend = chunksSend.incrementAndGet(),
                        totalChunks = totalChunks + 2,
                        command = 2,
                        progressBar = progressBar
                    ) {
                        main?.bleCommandWithQueue(
                            BLECommands.writeDataInCheckpointFileInSDCard(
                                chunk.toByteArray(), addressDevice, parameterID, index + 2
                            ),
                            MAIN_CHANNEL,
                            WRITE
                        ) {}
                    }

                    if (!sendFileSuccess) {
                        // Ошибка при отправке файла
                        Log.d("ChunkProcessing", "Ошибка передачи чанка №${chunksSend.get()}")
                        bleController.setUploadingState(false)
                        sendFileSuccessFlag = false
                        closeProgressDialog()
                        return
                    }
                }

                // Закрытие файла
                val closeFileSuccess = waitForFlagWithRetry(
                    maxWaitTimeMs = 2000L, // Ожидание флага 2 секунды
                    retryCount = 3,         // Максимум 3 попытки
                    chunksSend = chunksSend.incrementAndGet(),
                    totalChunks = totalChunks + 2,
                    command = 3,
                    progressBar = progressBar
                ) {
                    main?.bleCommandWithQueue(
                        BLECommands.closeCheckpointFileInSDCard(
                            addressDevice,
                            parameterID,
                            indexPackage + 3
                        ),
                        MAIN_CHANNEL, WRITE
                    ) {}
                }

                if (!closeFileSuccess) {
                    // Ошибка при закрытии файла
                    Log.d("ChunkProcessing", "Ошибка закрытия файла после чанка №${chunksSend.get()}")
                    bleController.setUploadingState(false)
                    sendFileSuccessFlag = false
                    closeCurrentDialog()
                    return
                }

                // Закрытие диалога с прогрессом после успешной передачи
                closeCurrentDialog()
                Toast.makeText(requireContext(), "Файл отправлен!", Toast.LENGTH_SHORT).show()
                Log.d("ChunkProcessing", "Всего чанков отправлено: $totalChunks")
            } catch (e: Exception) {
                // Обработка непредвиденных исключений
                Log.e("ChunkProcessing", "Неожиданная ошибка: ${e.message}")
                closeCurrentDialog()
            } finally {
                // Сброс флага загрузки независимо от результата
                bleController.setUploadingState(false)
            }
        }
    }



    override fun closeCurrentDialog() {
        currentDialog?.dismiss()
        currentDialog = null
        loadingCurrentDialog?.dismiss()
        loadingCurrentDialog = null
        progressDialog?.dismiss()
        progressDialog = null
    }
    private fun closeWarningDialog() {
        warningDialog?.dismiss()
        warningDialog = null
    }
    private fun closeProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }
    private suspend fun waitForFlagWithRetry(maxWaitTimeMs: Long, retryCount: Int, chunksSend: Int, totalChunks: Int, command: Int, progressBar: ProgressBar, sendAction: () -> Unit): Boolean {
        repeat(retryCount) { attempt ->
            val startTime = System.currentTimeMillis()

            // Выполняем отправку текущего чанка
            sendAction()
            val progress = ((chunksSend.toDouble() / totalChunks) * 100).toInt()
            progressBar.progress = progress
            Log.d("ChunkProcessing", "Progress: $progress% ($chunksSend/$totalChunks chunks sent) command = $command")

            while (System.currentTimeMillis() - startTime < maxWaitTimeMs) {
                if (canSendNextChunkFlag) {
                    canSendNextChunkFlag = false
                    return true // Успешно дождались
                }
                delay(10) // Немного ждем перед повторной проверкой
            }
            Log.d("ChunkProcessing", "Retrying action, attempt ${attempt + 1} / $retryCount   command = $command")
        }
        return false // Не удалось дождаться флага после всех попыток
    }

//    fun handleTrainingFinished() {
//        val bearer = prefs.getString(PreferenceKeysUBI4.KEY_TOKEN, "")!!
//        val serial = prefs.getString(PreferenceKeysUBI4.KEY_SERIAL, "")!!
//        TrainingUploadManager.launch(requireContext(), repo, bearer, serial)
//    }


    fun startAuthAndDownloadPassport(onSuccess:() -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val serial = SERIAL_DEFAULT
                val pass   = PASSWORD_DEFAULT

                // авторизация
                val token  = repo.fetchTokenBySerial(API_KEY, serial, pass)
                prefs.edit()
                    .putString(PreferenceKeysUBI4.KEY_TOKEN, token.toString())
                    .putString(PreferenceKeysUBI4.KEY_SERIAL, serial)
                    .putString(PreferenceKeysUBI4.KEY_PASSWORD, pass)
                    .apply()

                // скачиваем паспорт
                val raw = repo.fetchAndSavePassport(token, serial, requireContext().cacheDir)
                val ts  = raw.name.removeSuffix(".emg8.data_passport")
                val dst = File(requireContext().getExternalFilesDir(null), "$ts.emg8.data_passport")
                raw.copyTo(dst, overwrite = true)
                File(dst.parentFile!!, "config.json").writeText(dst.readText())
                withContext(Main) { onSuccess() }

            } catch (e: IOException) {
                withContext(Main) {
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        fun newInstance(lastEmg8: String): SprTrainingFragment =
            SprTrainingFragment().apply {
                arguments = Bundle().apply { putString(PreferenceKeysUBI4.ARG_LAST_EMG8, lastEmg8) }
            }

        private const val SERIAL_DEFAULT = "CYBI-H-05007" //<- Макс Емец
//        private const val SERIAL_DEFAULT = "CYBI-F-05663" //<- Тест
        private const val PASSWORD_DEFAULT = "123фыв6"
    }
}



