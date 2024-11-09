package com.bailout.stickk.ubi4.ui.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentSprTrainingBinding
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.GesturesOpticDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.OneButtonDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.PlotDelegateAdapter
import com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters.TrainingFragmentDelegateAdapter
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.contract.transmitter
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.graphThreadFlag
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.updateFlow
import com.bailout.stickk.ubi4.utility.Hyperparameters.BATCH_SIZE
import com.bailout.stickk.ubi4.utility.Hyperparameters.INDEX_TARGET_ID
import com.bailout.stickk.ubi4.utility.Hyperparameters.NUM_CLASSES
import com.bailout.stickk.ubi4.utility.Hyperparameters.NUM_EPOCHS
import com.bailout.stickk.ubi4.utility.Hyperparameters.NUM_FEATURES
import com.bailout.stickk.ubi4.utility.Hyperparameters.NUM_TIMESTEPS
import com.bailout.stickk.ubi4.utility.Hyperparameters.WIN_SHIFT
import com.livermor.delegateadapter.delegate.CompositeDelegateAdapter
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
//import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.channels.FileChannel
import java.util.Vector


class SprTrainingFragment : Fragment() {
    private lateinit var binding: Ubi4FragmentSprTrainingBinding
    private var main: MainActivityUBI4? = null
    private var mDataFactory: DataFactory = DataFactory()

//    private lateinit var tflite: Interpreter
    private lateinit var modelInfo: String
    private var thread: Thread = Thread()
    private val path: File by lazy {
        requireContext().getExternalFilesDir(null)
            ?: throw IllegalStateException("External files directory not available")
    }
    private lateinit var preprocessedX: Array<FloatArray>
    private lateinit var targetArray: Array<FloatArray>


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = Ubi4FragmentSprTrainingBinding.inflate(inflater, container, false)
        if (activity != null) {
            main = activity as MainActivityUBI4?
        }

        //настоящие виджеты
        widgetListUpdater()
        //фейковые виджеты
//        adapterWidgets.swapData(mDataFactory.fakeData2())


        binding.refreshLayout.setLottieAnimation("loader_3.json")
        binding.refreshLayout.setRepeatMode(SSPullToRefreshLayout.RepeatMode.REPEAT)
        binding.refreshLayout.setRepeatCount(SSPullToRefreshLayout.RepeatCount.INFINITE)
        binding.refreshLayout.setOnRefreshListener { refreshWidgetsList() }


        binding.sprTrainingRv.layoutManager = LinearLayoutManager(context)
        binding.sprTrainingRv.adapter = adapterWidgets
        return binding.root
    }

    private fun refreshWidgetsList() {
        graphThreadFlag = false
        transmitter().bleCommand(BLECommands.requestInicializeInformation(), MAIN_CHANNEL, WRITE)
    }
    private fun widgetListUpdater() {
        GlobalScope.launch(Main) {
            withContext(Default) {
                updateFlow.collect { value ->
                    main?.runOnUiThread {
                        Log.d("widgetListUpdater","${mDataFactory.prepareData(2)}")
                        adapterWidgets.swapData(mDataFactory.prepareData(2))
                        binding.refreshLayout.setRefreshing(false)
                    }
                }
            }
        }
    }

    private val adapterWidgets = CompositeDelegateAdapter(
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
                showConfirmTrainingDialog {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, MotionTrainingFragment()).commit()
                }
            }
        ),
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


//        path = requireContext().getExternalFilesDir(null)
//            ?: throw IllegalStateException("External files directory not available")
        val modelFile = File(path, "model.ckpt")

//        //////////////////////////// [LOAD DATA] /////////////////////////////
//        val assetManager = requireContext().assets
//            val importData = mutableListOf<List<String>>()
//
//            assetManager.open("2024-10-28_12-43-48.emg8").bufferedReader().useLines { lines ->
//                // drop header
//                lines.drop(1).forEach { line ->
//                    val lineData = line.split(" ")
//                    val targetId = lineData[INDEX_TARGET_ID]
//                    // drop baseline rows
//                    if (targetId.toDoubleOrNull() != null && targetId.toDouble().toInt() != -1) {
//                        importData.add(lineData)
//                    }
//                }
//            }
//
//            // Create mapping from INDEX_TARGET_STATE to renumeration by order of appearance
//            val stateToId = importData.map { it[INDEX_TARGET_STATE] }
//                .distinct()
//                .mapIndexed { index, state -> state to index }
//                .toMap()
//
//            // Renumerate INDEX_TARGET_ID in importData according to stateToId
//            val renumeratedImportData = importData.map { row ->
//                row.toMutableList().apply {
//                    this[INDEX_TARGET_ID] =
//                        stateToId[this[INDEX_TARGET_STATE]]?.toString() ?: this[INDEX_TARGET_ID]
//                }
//            }
//
//            println("Max value of INDEX_TARGET_ID: ${renumeratedImportData.maxOf { it[INDEX_TARGET_ID].toInt() }}")
//
//            val initFeatures = Array(renumeratedImportData.size) { row ->
//                var rowData =
//                    renumeratedImportData[row].slice(INDEX_START_FEATURES until INDEX_START_FEATURES + N_OMG_CH)
//                if (USE_EMG) {
//                    rowData += renumeratedImportData[row].slice(INDEX_START_FEATURES + N_OMG_CH until INDEX_START_FEATURES + N_OMG_CH + N_EMG_CH)
//                }
//                rowData.map { it.toFloat() }.toFloatArray()
//            }
//
//            println()
//            // Log initFeatures to file
//            val logFileData = File(path, "log_2024-10-28_12-43-48.emg8")
//            logFileData.bufferedWriter().use { writer ->
//                for (row in renumeratedImportData) {
//                    writer.write(row.joinToString(" "))
//                    writer.newLine()
//                }
//            }
//            println("Results written to $logFileData")
//            //////////////////////////// [\LOAD DATA] ////////////////////////////
//
//            ////////////////////////// [PREPROCESS DATA] /////////////////////////
//            val omgStd = initFeatures.mapIndexed { index, row ->
//                if (index == 0) {
//                    0f // For the first row, we can't calculate the difference, so we use 0
//                } else {
//                    row.zip(initFeatures[index - 1]) { a, b -> Math.abs(a - b) }.sum()
//                }
//            }.toFloatArray()
//
//            val targetSubseq = regionProps1D(renumeratedImportData)
//            val rectCoo = mutableMapOf<Int, Triple<Int, Int, Int>>()
//
//            for ((index, triple) in targetSubseq.withIndex()) {
//                val (onset, length, targetId) = triple
//                var maxArea = 0f
//                var argmaxShift = 0
//                for (shift in 0 until (AUTO_SHIFT_RANGE * length).toInt()) {
//                    val area = omgStd.slice(onset + shift until onset + length + shift).let { slice ->
//                        slice.zipWithNext { a, b -> (a + b) / 2 }.sum()
//                    }
//                    if (area > maxArea) {
//                        maxArea = area
//                        argmaxShift = shift
//                    }
//                }
//                rectCoo[index] = Triple(onset + argmaxShift, length, targetId)
//            }
//
//            val targetArray1D = IntArray(initFeatures.size)
//            for ((_, triple) in rectCoo) {
//                val (onset, length, targetId) = triple
//                for (i in onset until onset + length) {
//                    if (i < targetArray1D.size) {
//                        targetArray1D[i] = targetId
//                    }
//                }
//            }
//
//            targetArray = Array(initFeatures.size) { FloatArray(NUM_CLASSES) }
//            for (i in 0 until initFeatures.size) {
//                targetArray[i][targetArray1D[i]] = 1.0f
//            }
//
//            preprocessedX = Array(initFeatures.size) { FloatArray(NUM_FEATURES) }
//
//            var n_lp_ch = N_OMG_CH
//            if (USE_EMG) {
//                n_lp_ch += N_EMG_CH
//            }
//            val x_lp = FloatArray(n_lp_ch * N_LP_ALPHAS)
//            val x_curr = FloatArray(NUM_FEATURES)
//            val x_features = FloatArray(n_lp_ch)
//
//            for (i in 0 until initFeatures.size) {
//                for (k in 0 until N_OMG_CH) {
//                    x_features[k] = initFeatures[i][k] / SCALE_OMG
//                }
//                if (USE_EMG) {
//                    for (k in 0 until N_EMG_CH) {
//                        x_features[N_OMG_CH + k] = initFeatures[i][N_OMG_CH + k] / SCALE_EMG
//                    }
//                }
//                // HP compute
//                for (j in 0 until N_LP_ALPHAS) {
//                    for (k in 0 until n_lp_ch) {
//                        x_lp[j * n_lp_ch + k] =
//                            LP_ALPHAS[j] * x_features[k] + (1 - LP_ALPHAS[j]) * x_lp[j * NUM_FEATURES + k]
//                        x_curr[j * n_lp_ch + k] = x_features[k] - x_lp[j * n_lp_ch + k]
//                    }
//                }
//                for (k in 0 until n_lp_ch) {
//                    x_curr[n_lp_ch * N_LP_ALPHAS + k] = x_features[k]
//                }
//                for (j in 0 until NUM_FEATURES) {
//                    preprocessedX[i][j] = x_curr[j]
//                }
//        }
//
//        // Log preprocessedX to file
//        val logFilePreprocessedX = File(path, "log_preprocessedX.txt")
//        logFilePreprocessedX.bufferedWriter().use { writer ->
//            for (row in preprocessedX) {
//                writer.write(row.joinToString(" "))
//                writer.newLine()
//            }
//        }
//        // Log targetArray to file
//        val logFileTargetArray = File(path, "log_targetArray.txt")
//        logFileTargetArray.bufferedWriter().use { writer ->
//            for (row in targetArray) {
//                writer.write(row.joinToString(" "))
//                writer.newLine()
//            }
//        }
//        println("Loaded data hase ${preprocessedX.size} rows and ${initFeatures[0].size} columns")
//        ///////////////////////// [\PREPROCESS DATA] /////////////////////////
//
//
//        //////////////////////////// [LOAD MODEL] ////////////////////////////
//        // Import prior weights from a checkpoint file.
//        // populate with preset ckpt in assets directory
//        modelFile.writeBytes(requireContext().assets.open("model.ckpt").readBytes())
//        tflite = loadModelFile("model.tflite")
//        modelInfo = getModelInfo(tflite)
//
//        // Restore the model from the checkpoint file
//        val ckpt = modelFile.absolutePath
//
//        val inputs_ckpt = HashMap<String, Any>()
//        inputs_ckpt.put("checkpoint_path", ckpt)
//        val outputs_ckpt = HashMap<String, Any>()
//        tflite.runSignature(inputs_ckpt, outputs_ckpt, "restore")
//
//        val data = parseFile("test_images.txt")
//        Log.i("parse", data.toString())
//
//        //////////////////////////// [WORK WITH MODEL] /////////////////////////////
//            //runModel()
//        //val btnTrain = view.findViewById<Button>(R.id.btnTrain)
//
//        //btnTrain.setOnClickListener { runModel() }
//        //////////////////////////// [\WORK WITH MODEL] ////////////////////////////
////        view.findViewById<Button>(R.id.btnValidate).setOnClickListener {
////            validate()
////        }
    }

    @SuppressLint("MissingInflatedId")
    fun showConfirmTrainingDialog(confirmClick: () -> Unit) {
        val dialogBinding = layoutInflater.inflate(R.layout.ubi4_dialog_confirm_training, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        val cancelBtn = dialogBinding.findViewById<View>(R.id.ubi4DialogTrainingCancelBtn)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
        }

        val confirmBtn = dialogBinding.findViewById<View>(R.id.ubi4DialogConfirmTrainingBtn)
        confirmBtn.setOnClickListener {
            myDialog.dismiss()
            confirmClick()
        }


    }

    private fun oneButtonPressed(addressDevice: Int, parameterID: Int, command: Int) {
        System.err.println("oneButtonPressed    parameterID: $parameterID   command: $command")
        transmitter().bleCommand(
            BLECommands.sendOneButtonCommand(addressDevice,parameterID, command),
            MAIN_CHANNEL,
            WRITE
        )
    }

    private fun oneButtonReleased(addressDevice: Int, parameterID: Int, command: Int) {
        System.err.println("oneButtonReleased    parameterID: $parameterID   command: $command")



        transmitter().bleCommand(
            BLECommands.sendOneButtonCommand(addressDevice,parameterID, command),
            MAIN_CHANNEL,
            WRITE
        )
    }

//    private fun loadModelFile(modelPath: String): Interpreter {
//        val assetFileDescriptor = requireContext().assets.openFd(modelPath)
//        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
//        val fileChannel = fileInputStream.channel
//        val startOffset = assetFileDescriptor.startOffset
//        val declaredLength = assetFileDescriptor.declaredLength
//        val model = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
//        return Interpreter(model)
//    }
//
//    private fun getModelInfo(interpreter: Interpreter): String {
//        val inputTensorCount = interpreter.inputTensorCount
//        val outputTensorCount = interpreter.outputTensorCount
//
//        val inputDetails = (0 until inputTensorCount).joinToString("\n") { i ->
//            val tensor = interpreter.getInputTensor(i)
//            val shape = tensor.shape().joinToString(", ")
//            val type = tensor.dataType()
//            "Input Tensor $i: shape=[$shape], type=$type"
//        }
//
//        val outputDetails = (0 until outputTensorCount).joinToString("\n") { i ->
//            val tensor = interpreter.getOutputTensor(i)
//            val shape = tensor.shape().joinToString(", ")
//            val type = tensor.dataType()
//            "Output Tensor $i: shape=[$shape], type=$type"
//        }
//        val weightDetails = (0 until interpreter.inputTensorCount).joinToString("\n") { i ->
//            val tensor = interpreter.getInputTensor(i)
//            val buffer = tensor.asReadOnlyBuffer()
//            val shape = tensor.shape().joinToString(", ")
//            val type = tensor.dataType()
//            "Weight Tensor $i: shape=[$shape], type=$type, values=${bufferToString(buffer, type)}"
//
//
//        }
//        return "Model Info:\n$inputDetails\n$outputDetails\n\nWeight Details:\n$weightDetails"
//    }

//    private fun bufferToString(buffer: ByteBuffer, type: org.tensorflow.lite.DataType): String {
//        return when (type) {
//            org.tensorflow.lite.DataType.FLOAT32 -> {
//                val floatBuffer = buffer.asFloatBuffer()
//                val array = FloatArray(floatBuffer.remaining())
//                floatBuffer.get(array)
//                array.joinToString(", ")
//            }
//
//            org.tensorflow.lite.DataType.INT32 -> {
//                val intBuffer = buffer.asIntBuffer()
//                val array = IntArray(intBuffer.remaining())
//                intBuffer.get(array)
//                array.joinToString(", ")
//            }
//
//            org.tensorflow.lite.DataType.UINT8 -> {
//                val byteArray = ByteArray(buffer.remaining())
//                buffer.get(byteArray)
//                byteArray.joinToString(", ")
//            }
//
//            else -> "Unsupported data type"
//        }
//    }
//
//    private fun parseFile(filename: String): MutableList<MutableList<Double>> {
//        val data = mutableListOf<MutableList<Double>>()
//        requireContext().assets.open(filename).bufferedReader().useLines { lines ->
//            lines.forEach { str ->
//                data.add(str.split(" ").map { it.toDouble() }.toMutableList())
//            }
//        }
//        return data
//    }
//
//    private fun runModel() {
//        if (!thread.isAlive) {
//            thread = Thread {
//                train(path, preprocessedX, targetArray)
//                export(path)
//                run(path, preprocessedX)
//            }
//            thread.start()
//        }
//    }
//
//    @SuppressLint("LogNotTimber")
//    private fun train(
//        path: File?,
//        preprocessedX: Array<FloatArray>,
//        targetArray: Array<FloatArray>
//    ) {
//        //////////////////////////// [TRAIN MODEL] ////////////////////////////
//        // train the model
//        // https://ai.google.dev/edge/litert/models/ondevice_training
//        // Формирование индексов для формирования батчей
//        // TODO: need to be changed to random indexes
//        val indexes = IntRange(0, preprocessedX.size - NUM_TIMESTEPS - 1).step(WIN_SHIFT).toList()
//        // Shuffle the indexes
//        val num_batches = indexes.size / BATCH_SIZE
//        // TODO: удалить в будующем, тк используется только для логирования
//        val indexesAllBatches = Array(num_batches) { IntArray(BATCH_SIZE) }
//        for (i in 0 until num_batches) {
//            for (j in 0 until BATCH_SIZE) {
//                indexesAllBatches[i][j] = indexes[i * BATCH_SIZE + j]
//            }
//        }
//        // Log indexesAllBatches
//        val logFileIndexesAllBatches = File(path, "log_indexes_batches.txt")
//        logFileIndexesAllBatches.bufferedWriter().use { writer ->
//            for (row in indexesAllBatches) {
//                writer.write(row.joinToString(" "))
//                writer.newLine()
//            }
//        }
//        println("Results written to $logFileIndexesAllBatches")
//
//        // Run training for a few steps.
//        val losses = FloatArray(NUM_EPOCHS) // TODO: remove potentialy, useless val
//        val lossesEpoch = FloatArray(num_batches)
//        val lossesAll = FloatArray(num_batches * NUM_EPOCHS)
//
//        val lossCalc = FloatBuffer.allocate(1)
//        val inputsCalc = HashMap<String, Any>()
//        val outputsCalc = HashMap<String, Any>()
//        // холостая имитация действий в цикле обучения
//        // без нее самый первый lossCalc будет нулевым
//        // хотя в python он не нулевой
//        // возможно это из-за неверной инициализации lossCalc
//        outputsCalc["loss"] = lossCalc
//        lossCalc.rewind()
//        // конец имитации
//
//        var epochsTimerSum = 0
//        val epochsTimerArr = Vector<Number>()
//
//        for (epoch in 0 until NUM_EPOCHS) {
//            val epochsTimer = Chronometer(context)
//            epochsTimer.base = SystemClock.elapsedRealtime()
//            epochsTimer.start()
//            val shuffledIndexes = indexes.shuffled()
//            val shuffledIndexesAllBatches = Array(num_batches) { IntArray(BATCH_SIZE) }
//            for (i in 0 until num_batches) {
//                for (j in 0 until BATCH_SIZE) {
//                    shuffledIndexesAllBatches[i][j] = shuffledIndexes[i * BATCH_SIZE + j]
//                }
//            }
//            val logFileShuffledIndexesAllBatches =
//                File(path, "log_shuffled_indexes_batches_${epoch}.txt")
//            logFileShuffledIndexesAllBatches.bufferedWriter().use { writer ->
//                for (row in shuffledIndexesAllBatches) {
//                    writer.write(row.joinToString(" "))
//                    writer.newLine()
//                }
//            }
//            // flush lossesEpoch
//            for (i in 0 until num_batches) {
//                lossesEpoch[i] = 0.0f
//            }
//
//            for (batchIdx in 0 until num_batches) {
//                val batchesTimer = Chronometer(context)
//                batchesTimer.base = SystemClock.elapsedRealtime()
//                batchesTimer.start()
//
//                // construct batch frame
//                val batchIndexes = shuffledIndexesAllBatches[batchIdx]
//                val batchX = FloatBuffer.allocate(BATCH_SIZE * NUM_TIMESTEPS * NUM_FEATURES)
//                val batchLabels = FloatBuffer.allocate(BATCH_SIZE * NUM_TIMESTEPS * NUM_CLASSES)
//                batchX.rewind()
//                batchLabels.rewind()
//
//                // Fill the data
//                for (bIdx in 0 until BATCH_SIZE) {
//                    val batchIndex = batchIndexes[bIdx]
//                    for (tIdx in 0 until NUM_TIMESTEPS) {
//                        for (wIdx in 0 until NUM_FEATURES) {
//                            batchX.put(preprocessedX[batchIndex + tIdx][wIdx])
//                        }
//                        for (cIdx in 0 until NUM_CLASSES) {
//                            batchLabels.put(targetArray[batchIndex + tIdx][cIdx])
//                        }
//                    }
//                }
//
//                batchX.rewind()
//                batchLabels.rewind()
//
//                inputsCalc["x"] = batchX
//                inputsCalc["y"] = batchLabels
//
//                tflite.runSignature(inputsCalc, outputsCalc, "train")
//
//                outputsCalc["loss"] = lossCalc
//                lossesEpoch[batchIdx] = lossCalc.get(0)
//                lossesAll[epoch * num_batches + batchIdx] = lossCalc.get(0)
//                lossCalc.rewind()
//                batchesTimer.stop()
//                Log.i(
//                    "timer_batches",
//                    (SystemClock.elapsedRealtime() - batchesTimer.base).toString()
//                )
//            }
//
//            // Record the last loss.
//            losses[epoch] = lossCalc.get(0)
//
//            epochsTimer.stop()
//            epochsTimerArr.add(SystemClock.elapsedRealtime() - epochsTimer.base)
//            epochsTimerSum += (SystemClock.elapsedRealtime() - epochsTimer.base).toInt()
//            Log.i("timer_epochs", (SystemClock.elapsedRealtime() - epochsTimer.base).toString())
//
//            // Print the loss output for every 10 epochs.
//            if ((epoch + 1) % 1 == 0) { // DEBUG
//                // if ((epoch + 1) % 10 == 0) { // PROD
//                println("Finished ${epoch + 1} epochs, current loss: ${lossCalc.get(0)}")
//            }
//        }
//
//        // Log lossesAll to file
//        val logFileLosses = File(path, "log_losses.txt")
//        logFileLosses.bufferedWriter().use { writer ->
//            for (loss in lossesAll) {
//                writer.write(loss.toString())
//                writer.newLine()
//            }
//        }
//        println("Results written to $logFileLosses")
//        Log.i(
//            "timer_epochs_data",
//            epochsTimerSum.toString() + ' ' + (epochsTimerSum / epochsTimerArr.size.toDouble()).toString()
//        )
//
//    }
//
//    private fun export(path: File?) {
//        //////////////////////////// [EXPORT MODEL] ////////////////////////////
//        val outputFile = File(path, "checkpoint.ckpt")
//        val inputs = HashMap<String, Any>()
//        inputs["checkpoint_path"] = outputFile.absolutePath
//        val outputs = HashMap<String, Any>()
//        tflite.runSignature(inputs, outputs, "save")
//        //////////////////////////// [\EXPORT MODEL] ////////////////////////////
//    }
//
//    private fun run(path: File?, preprocessedX: Array<FloatArray>) {
//        // Unnecessary code block because we dont need to run model on android device
//        // we need only to export it
//        // Проблема с методом "infer" в том, что мы используем statefull модели,
//        // т.е. модель сохраняет внутреннее состояние между шагами инференса
//        // вопрос: как внутри экспортированной модели сохранить состояние?
//        //////////////////////////// [RUN MODEL] ////////////////////////////
//        // initialize inputs and outputs
//        val inputsRun = HashMap<String, Any>()
//        val outputsRun = HashMap<String, Any>()
//
//        // fill inputsRun
//        val inputData = FloatBuffer.allocate(BATCH_SIZE * NUM_TIMESTEPS * NUM_FEATURES)
//        inputData.rewind()
//        for (bIdx in 0 until BATCH_SIZE) {
//            for (tIdx in 0 until NUM_TIMESTEPS) {
//                for (wIdx in 0 until NUM_FEATURES) {
//                    inputData.put(preprocessedX[bIdx * NUM_TIMESTEPS + tIdx][wIdx])
//                }
//            }
//        }
//        inputData.rewind()
//
//        // Prepare output buffer
//        val outputData = FloatBuffer.allocate(BATCH_SIZE * NUM_TIMESTEPS * NUM_CLASSES)
//        outputsRun["output"] = outputData
//
//        inputsRun["x"] = inputData
//        tflite.runSignature(inputsRun, outputsRun, "infer") // MAX: why we need `outputsRun`?
//        // Write results
//        outputData.rewind()
//
//        // TODO: Broken
//        // val outputArray = Array(BATCH_SIZE) { Array(NUM_TIMESTEPS) { FloatArray(NUM_CLASSES) } }
//        // for (bIdx in 0 until BATCH_SIZE) {
//        //     for (tIdx in 0 until NUM_TIMESTEPS) {
//        //         for (cIdx in 0 until NUM_CLASSES) {
//        //             outputArray[bIdx][tIdx][cIdx] = outputData[bIdx * NUM_TIMESTEPS + tIdx * NUM_CLASSES + cIdx]
//        //         }
//        //     }
//        // }
//        // outputData.rewind()
//
//        // Log results to file
//        val logFileInputInfer = File(path, "log_input_infer.txt")
//        logFileInputInfer.bufferedWriter().use { writer ->
//            inputData.rewind()
//            while (inputData.hasRemaining()) {
//                writer.write(inputData.get().toString())
//                writer.newLine()
//            }
//        }
//        val logFileOutputInfer = File(path, "log_output_infer.txt")
//        logFileOutputInfer.bufferedWriter().use { writer ->
//            outputData.rewind()
//            while (outputData.hasRemaining()) {
//                writer.write(outputData.get().toString())
//                writer.newLine()
//            }
//        }
//        // TODO: Broken
//        // val logFileOutputInferArray = File(path, "log_output_infer_array.txt")
//        // logFileOutputInferArray.bufferedWriter().use { writer ->
//        //     for (bIdx in 0 until BATCH_SIZE) {
//        //         for (tIdx in 0 until NUM_TIMESTEPS) {
//        //             writer.write(outputArray[bIdx][tIdx].joinToString(" "))
//        //             writer.newLine()
//        //         }
//        //     }
//        // }
//        println("Results written to $logFileInputInfer, $logFileOutputInfer")
//        //////////////////////////// [\RUN MODEL] ////////////////////////////
//    }
//
//
//    private fun regionProps1D(renumeratedImportData: List<List<String>>): List<Triple<Int, Int, Int>> {
//        val targetSubseq = mutableListOf<Triple<Int, Int, Int>>()
//        var onset = -1
//        var length = 0
//        var currentId = -1
//        for (i in renumeratedImportData.indices) {
//            val id = renumeratedImportData[i][INDEX_TARGET_ID].toInt()
//            if (id != currentId) {
//                if (currentId > 0) {
//                    targetSubseq.add(Triple(onset, length, currentId))
//                }
//                onset = i
//                length = 1
//                currentId = id
//            } else {
//                length++
//            }
//        }
//        if (currentId > 0) {
//            targetSubseq.add(Triple(onset, length, currentId))
//        }
//        return targetSubseq
//    }
}