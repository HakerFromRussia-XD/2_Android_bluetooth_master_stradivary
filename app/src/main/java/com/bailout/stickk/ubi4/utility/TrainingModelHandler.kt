package com.bailout.stickk.ubi4.utility

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetManager
import android.os.SystemClock
import android.util.Log
import android.widget.Chronometer
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.stateOpticTrainingFlow
import com.bailout.stickk.ubi4.utility.Hyperparameters.AUTO_SHIFT_RANGE
import com.bailout.stickk.ubi4.utility.Hyperparameters.BATCH_SIZE
import com.bailout.stickk.ubi4.utility.Hyperparameters.INDEX_START_FEATURES
import com.bailout.stickk.ubi4.utility.Hyperparameters.INDEX_TARGET_ID
import com.bailout.stickk.ubi4.utility.Hyperparameters.INDEX_TARGET_STATE
import com.bailout.stickk.ubi4.utility.Hyperparameters.LP_ALPHAS
import com.bailout.stickk.ubi4.utility.Hyperparameters.NUM_CLASSES
import com.bailout.stickk.ubi4.utility.Hyperparameters.NUM_EPOCHS
import com.bailout.stickk.ubi4.utility.Hyperparameters.NUM_FEATURES
import com.bailout.stickk.ubi4.utility.Hyperparameters.NUM_TIMESTEPS
import com.bailout.stickk.ubi4.utility.Hyperparameters.N_EMG_CH
import com.bailout.stickk.ubi4.utility.Hyperparameters.N_LP_ALPHAS
import com.bailout.stickk.ubi4.utility.Hyperparameters.N_OMG_CH
import com.bailout.stickk.ubi4.utility.Hyperparameters.SCALE_EMG
import com.bailout.stickk.ubi4.utility.Hyperparameters.SCALE_OMG
import com.bailout.stickk.ubi4.utility.Hyperparameters.USE_EMG
import com.bailout.stickk.ubi4.utility.Hyperparameters.WIN_SHIFT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Vector

class TrainingModelHandler(private val context: Context) {
    private var path: File? = null
    private lateinit var modelFile: File
    private lateinit var modelInfo: String
    private lateinit var preprocessedX: Array<FloatArray>
    private lateinit var targetArray: Array<FloatArray>
    private lateinit var tflite: Interpreter
    private lateinit var assetManager: AssetManager
    private lateinit var epochsTimer: Chronometer
    private lateinit var batchesTimer: Chronometer

    fun initialize() {
        path = context.getExternalFilesDir(null)
        modelFile = path?.let { File(it, "model.ckpt") }
            ?: throw IllegalStateException("External files directory not available")
        modelFile.writeBytes(context.assets.open("model.ckpt").readBytes())
        assetManager = context.assets
        tflite = loadModelFile("model.tflite")
        epochsTimer = Chronometer(context)
        batchesTimer = Chronometer(context)
        CoroutineScope(Default).launch { stateOpticTrainingFlow.emit(PreferenceKeysUBI4.TrainingModelState.BASE) }
        Log.d("StateCallBack", "Initialized with state: 0")
    }

    //TODO прописать сохранение в шаред преференс
    private fun handleState(state: PreferenceKeysUBI4.TrainingModelState) {
//        saveStateToPreference(state)
        when (state) {
            PreferenceKeysUBI4.TrainingModelState.EXPORT -> {
                CoroutineScope(Default).launch {
                    stateOpticTrainingFlow.emit(PreferenceKeysUBI4.TrainingModelState.EXPORT)
                }
                Log.d("StateCallBack", "handleState: export")
            }

            PreferenceKeysUBI4.TrainingModelState.RUN -> {
                CoroutineScope(Default).launch {
                    stateOpticTrainingFlow.emit(PreferenceKeysUBI4.TrainingModelState.RUN)
                }
                Log.d("StateCallBack", "handleState: runModel")
            }

            PreferenceKeysUBI4.TrainingModelState.BASE -> {
                CoroutineScope(Default).launch {
                    stateOpticTrainingFlow.emit(PreferenceKeysUBI4.TrainingModelState.BASE)
                }
                Log.d("StateCallBack", "handleState: BASE")
            }
        }
    }

    private fun saveStateToPreference(state: PreferenceKeysUBI4.TrainingModelState) {
        val sharedPreferences = context.getSharedPreferences("training_state", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("current_state",state.name)
        editor.apply()
    }

//    private fun loadStateToSharedPreference(): PreferenceKeysUBI4.TrainingModelState {
//        val sharedPreferences = context.getSharedPreferences("training_state", Context.MODE_PRIVATE)
//        val stateName = sharedPreferences.getString("current_state", PreferenceKeysUBI4.TrainingModelState.BASE.name)
//        return PreferenceKeysUBI4.TrainingModelState.valueOf(stateName!!) // Преобразуем строку обратно в enum
//    }

//    fun initializeState() {
//        val initializeState = loadStateToSharedPreference()
//            // handleState(initializeState)
//    }




    private fun loadModelFile(modelPath: String): Interpreter {
        val assetFileDescriptor = context.assets.openFd(modelPath)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val model = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        return Interpreter(model)
    }

    fun runModel() {
        Log.d("StateCallBack", "runModel")
        CoroutineScope(Dispatchers.IO).launch {
//            if (stateOpticTrainingFlow.value != PreferenceKeysUBI4.TrainingModelState.RUN) {
//                stateOpticTrainingFlow.emit(PreferenceKeysUBI4.TrainingModelState.RUN)
//                Log.d("StateFlow", "State changed to RUN")
//            }
            handleState(PreferenceKeysUBI4.TrainingModelState.RUN)
            val startTime = System.currentTimeMillis()
            val endTime = System.currentTimeMillis()
            try {
                //////////////////////////// [LOAD DATA] /////////////////////////////
                //val assetManager = requireContext().assets
                val importData = mutableListOf<List<String>>()
                Log.d("SprTraining", "assetManager: $assetManager")

                assetManager.open("2024-10-28_12-43-48.emg8").bufferedReader()
                    .useLines { lines ->
                        // drop header
                        lines.drop(1).forEach { line ->
                            val lineData = line.split(" ")
                            // drop baseline rows
                            if (lineData[INDEX_TARGET_ID].toInt() != -1) {
                                importData.add(lineData)
                            }
                        }
                    }
                Log.d("LagSpr", "Start RunModel2 ${endTime - startTime} ms")

                // Create mapping from INDEX_TARGET_STATE to renumeration by order of appearance
                val stateToId = importData.map { it[INDEX_TARGET_STATE] }
                    .distinct()
                    .mapIndexed { index, state -> state to index }
                    .toMap()

                // Renumerate INDEX_TARGET_ID in importData according to stateToId
                val renumeratedImportData = importData.map { row ->
                    row.toMutableList().apply {
                        this[INDEX_TARGET_ID] =
                            stateToId[this[INDEX_TARGET_STATE]]?.toString()
                                ?: this[INDEX_TARGET_ID]
                    }
                }
                Log.d("LagSpr", "Start RunModel3 ${endTime - startTime} ms")

                val initFeatures = Array(renumeratedImportData.size) { row ->
                    var rowData =
                        renumeratedImportData[row].slice(INDEX_START_FEATURES until INDEX_START_FEATURES + N_OMG_CH)
                    if (USE_EMG) {
                        rowData += renumeratedImportData[row].slice(INDEX_START_FEATURES + N_OMG_CH until INDEX_START_FEATURES + N_OMG_CH + N_EMG_CH)
                    }
                    rowData.map { it.toFloat() }.toFloatArray()
                }

                Log.d("LagSpr", "Start RunModel4 ${endTime - startTime} ms")
                //////////////////////////// [\LOAD DATA] ////////////////////////////


                ////////////////////////// [PREPROCESS DATA] /////////////////////////
                // target shift heuristic
                val omgStd = initFeatures.mapIndexed { index, row ->
                    if (index == 0) {
                        0f // For the first row, we can't calculate the difference, so we use 0
                    } else {
                        row.zip(initFeatures[index - 1]) { a, b -> Math.abs(a - b) }.sum()
                    }
                }.toFloatArray()
                val targetSubseq = regionProps1D(renumeratedImportData)
                Log.d("targetSubseq", "$targetSubseq")
                val rectCoo = mutableMapOf<Int, Triple<Int, Int, Int>>()
                for ((index, triple) in targetSubseq.withIndex()) {
                    val (onset, length, targetId) = triple
                    var maxArea = 0f
                    var argmaxShift = 0
                    for (shift in 0 until (AUTO_SHIFT_RANGE * length).toInt()) {
                        val area =
                            omgStd.slice(onset + shift until onset + length + shift)
                                .let { slice ->
                                    // analog of `np.trapz`
                                    slice.zipWithNext { a, b -> (a + b) / 2 }
                                        .sum() * 1 // Assuming time step is 1
                                }
                        if (area > maxArea) {
                            maxArea = area
                            argmaxShift = shift
                        }
                    }
                    rectCoo[index] = Triple(onset + argmaxShift, length, targetId)
                }
                Log.d("LagSpr", "Start RunModel5 ${endTime - startTime} ms")

                val targetArray1D = IntArray(initFeatures.size)
                for ((_, triple) in rectCoo) {
                    val (onset, length, targetId) = triple
                    for (i in onset until onset + length) {
                        if (i < targetArray1D.size) {
                            targetArray1D[i] = targetId
                        }
                    }
                }
                targetArray = Array(initFeatures.size) { FloatArray(NUM_CLASSES) }
                for (i in 0 until initFeatures.size) {
                    targetArray[i][targetArray1D[i]] = 1.0f
                }

                Log.d("LagSpr", "Start RunModel6 ${endTime - startTime} ms")

                preprocessedX = Array(initFeatures.size) { FloatArray(NUM_FEATURES) }

                var n_lp_ch = N_OMG_CH
                if (USE_EMG) {
                    n_lp_ch += N_EMG_CH
                }
                val x_lp = FloatArray(n_lp_ch * N_LP_ALPHAS)
                val x_curr = FloatArray(NUM_FEATURES)
                val x_features = FloatArray(n_lp_ch)
                for (i in 0 until initFeatures.size) {
                    for (k in 0 until N_OMG_CH) {
                        x_features[k] = initFeatures[i][k] / SCALE_OMG
                    }
                    if (USE_EMG) {
                        for (k in 0 until N_EMG_CH) {
                            x_features[N_OMG_CH + k] = initFeatures[i][N_OMG_CH + k] / SCALE_EMG
                        }
                    }
                    for (j in 0 until N_LP_ALPHAS) {
                        for (k in 0 until n_lp_ch) {
                            x_lp[j * n_lp_ch + k] =
                                LP_ALPHAS[j] * x_features[k] + (1 - LP_ALPHAS[j]) * x_lp[j * NUM_FEATURES + k]
                            x_curr[j * n_lp_ch + k] = x_features[k] - x_lp[j * n_lp_ch + k]
                        }
                    }
                    // copy X to x_curr
                    for (k in 0 until n_lp_ch) {
                        x_curr[n_lp_ch * N_LP_ALPHAS + k] = x_features[k]
                    }
                    for (j in 0 until NUM_FEATURES) {
                        preprocessedX[i][j] = x_curr[j]
                    }
                }
                Log.d("LagSpr", "Start RunModel7 ${endTime - startTime} ms")
                ///////////////////////// [\PREPROCESS DATA] /////////////////////////


                //////////////////////////// [LOAD MODEL] ////////////////////////////
                // Import prior weights from a checkpoint file.
                // populate with preset ckpt in assets directory
                Log.d("TFLite", "Interpreter initialized: $tflite")
                modelInfo = getModelInfo(tflite)

                // Restore the model from the checkpoint file
                val ckpt = modelFile.absolutePath
                val inputs_ckpt = HashMap<String, Any>()
                inputs_ckpt.put("checkpoint_path", ckpt)
                val outputs_ckpt = HashMap<String, Any>()
                tflite.runSignature(inputs_ckpt, outputs_ckpt, "restore")
                train(preprocessedX, targetArray)
                export(path)
                run(preprocessedX)
                Log.d("LagSpr", "Start RunModel8 ${endTime - startTime} ms")
            } catch (e: Exception) {
                Log.e("LagSpr", "Error in runModel: ${e.message}", e)
            }
        }
    }

    @SuppressLint("LogNotTimber")
    private fun train(
        preprocessedX: Array<FloatArray>,
        targetArray: Array<FloatArray>
    ) {
        Log.d("LagSpr", "Start RunModel9")
        //////////////////////////// [TRAIN MODEL] ////////////////////////////
        // train the model
        // https://ai.google.dev/edge/litert/models/ondevice_training
        // Формирование индексов для формирования батчей
        // TODO: need to be changed to random indexes
        val indexes = IntRange(0, preprocessedX.size - NUM_TIMESTEPS - 1).step(WIN_SHIFT).toList()
        // Shuffle the indexes
        val num_batches = indexes.size / BATCH_SIZE
        // TODO: удалить в будующем, тк используется только для логирования
        val indexesAllBatches = Array(num_batches) { IntArray(BATCH_SIZE) }
        for (i in 0 until num_batches) {
            for (j in 0 until BATCH_SIZE) {
                indexesAllBatches[i][j] = indexes[i * BATCH_SIZE + j]
            }
        }

        // Run training for a few steps.
        val losses = FloatArray(NUM_EPOCHS) // TODO: remove potentialy, useless val
        val lossesEpoch = FloatArray(num_batches)
        val lossesAll = FloatArray(num_batches * NUM_EPOCHS)

        val lossCalc = FloatBuffer.allocate(1)
        val inputsCalc = HashMap<String, Any>()
        val outputsCalc = HashMap<String, Any>()
        // холостая имитация действий в цикле обучения
        // без нее самый первый lossCalc будет нулевым
        // хотя в python он не нулевой
        // возможно это из-за неверной инициализации lossCalc
        outputsCalc["loss"] = lossCalc
        lossCalc.rewind()
        // конец имитации

        var epochsTimerSum = 0
        val epochsTimerArr = Vector<Number>()

        for (epoch in 0 until NUM_EPOCHS) {
            //val epochsTimer = Chronometer(requireContext())
            epochsTimer.base = SystemClock.elapsedRealtime()
            epochsTimer.start()
            val shuffledIndexes = indexes.shuffled()
            val shuffledIndexesAllBatches = Array(num_batches) { IntArray(BATCH_SIZE) }
            for (i in 0 until num_batches) {
                for (j in 0 until BATCH_SIZE) {
                    shuffledIndexesAllBatches[i][j] = shuffledIndexes[i * BATCH_SIZE + j]
                }
            }
            // flush lossesEpoch
            for (i in 0 until num_batches) {
                lossesEpoch[i] = 0.0f
            }

            for (batchIdx in 0 until num_batches) {
                // val batchesTimer = Chronometer(requireContext())
                batchesTimer.base = SystemClock.elapsedRealtime()
                batchesTimer.start()

                // construct batch frame
                val batchIndexes = shuffledIndexesAllBatches[batchIdx]
                val batchX = FloatBuffer.allocate(BATCH_SIZE * NUM_TIMESTEPS * NUM_FEATURES)
                val batchLabels = FloatBuffer.allocate(BATCH_SIZE * NUM_TIMESTEPS * NUM_CLASSES)
                batchX.rewind()
                batchLabels.rewind()

                // Fill the data
                for (bIdx in 0 until BATCH_SIZE) {
                    val batchIndex = batchIndexes[bIdx]
                    for (tIdx in 0 until NUM_TIMESTEPS) {
                        for (wIdx in 0 until NUM_FEATURES) {
                            batchX.put(preprocessedX[batchIndex + tIdx][wIdx])
                        }
                        for (cIdx in 0 until NUM_CLASSES) {
                            batchLabels.put(targetArray[batchIndex + tIdx][cIdx])
                        }
                    }
                }

                batchX.rewind()
                batchLabels.rewind()

                inputsCalc["x"] = batchX
                inputsCalc["y"] = batchLabels

                tflite.runSignature(inputsCalc, outputsCalc, "train")

                outputsCalc["loss"] = lossCalc
                lossesEpoch[batchIdx] = lossCalc.get(0)
                lossesAll[epoch * num_batches + batchIdx] = lossCalc.get(0)
                lossCalc.rewind()
                batchesTimer.stop()
                Log.i(
                    "timer_batches",
                    (SystemClock.elapsedRealtime() - batchesTimer.base).toString()
                )
            }

            // Record the last loss.
            losses[epoch] = lossCalc.get(0)

            epochsTimer.stop()
            epochsTimerArr.add(SystemClock.elapsedRealtime() - epochsTimer.base)
            epochsTimerSum += (SystemClock.elapsedRealtime() - epochsTimer.base).toInt()
            Log.i("timer_epochs", (SystemClock.elapsedRealtime() - epochsTimer.base).toString())

        }

        Log.d("LagSpr", "Start RunModel10")
    }


    fun export(path: File?) {
        CoroutineScope(Default).launch {
            Log.d("LagSpr", "Start RunModel11")
            //////////////////////////// [EXPORT MODEL] ////////////////////////////
            // Export the trained weights as a checkpoint file.
            try {
                val dateFormat = SimpleDateFormat("MM-dd_HH-mm-ss", Locale.getDefault())
                val currentDateTime = dateFormat.format(Date())

                val outputFile = File(path, "checkpoint_$currentDateTime")
                val inputs = HashMap<String, Any>()
                inputs.put("checkpoint_path", outputFile.absolutePath)
                val outputs = HashMap<String, Any>()
                tflite.runSignature(
                    inputs,
                    outputs,
                    "save"
                )
            } finally {
                handleState(PreferenceKeysUBI4.TrainingModelState.EXPORT)
                Log.d("StateCallBack", "finaly: EXPORT")

            }

            // This file should be then transferred to the microcontroller
            Log.d("LagSpr", "Start RunModel12")
            //////////////////////////// [\EXPORT MODEL] ////////////////////////////
        }
    }

    private fun run(preprocessedX: Array<FloatArray>) {
        Log.d("LagSpr", "Start RunModel13")
        // Unnecessary code block because we dont need to run model on android device
        // we need only to export it
        // Проблема с методом "infer" в том, что мы используем statefull модели,
        // т.е. модель сохраняет внутреннее состояние между шагами инференса
        // вопрос: как внутри экспортированной модели сохранить состояние?
        //////////////////////////// [RUN MODEL] ////////////////////////////
        // initialize inputs and outputs
        val inputsRun = HashMap<String, Any>()
        val outputsRun = HashMap<String, Any>()

        // fill inputsRun
        val inputData = FloatBuffer.allocate(BATCH_SIZE * NUM_TIMESTEPS * NUM_FEATURES)
        inputData.rewind()
        for (bIdx in 0 until BATCH_SIZE) {
            for (tIdx in 0 until NUM_TIMESTEPS) {
                for (wIdx in 0 until NUM_FEATURES) {
                    inputData.put(preprocessedX[bIdx * NUM_TIMESTEPS + tIdx][wIdx])
                }
            }
        }
        inputData.rewind()

        // Prepare output buffer
        val outputData = FloatBuffer.allocate(BATCH_SIZE * NUM_TIMESTEPS * NUM_CLASSES)
        outputsRun["output"] = outputData

        inputsRun["x"] = inputData
        tflite.runSignature(inputsRun, outputsRun, "infer") // MAX: why we need `outputsRun`?
        // Write results
        outputData.rewind()

        // TODO: Broken
        // val outputArray = Array(BATCH_SIZE) { Array(NUM_TIMESTEPS) { FloatArray(NUM_CLASSES) } }
        // for (bIdx in 0 until BATCH_SIZE) {
        //     for (tIdx in 0 until NUM_TIMESTEPS) {
        //         for (cIdx in 0 until NUM_CLASSES) {
        //             outputArray[bIdx][tIdx][cIdx] = outputData[bIdx * NUM_TIMESTEPS + tIdx * NUM_CLASSES + cIdx]
        //         }
        //     }
        // }
        // outputData.rewind()

        // Log results to file
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
        // TODO: Broken
        // val logFileOutputInferArray = File(path, "log_output_infer_array.txt")
        // logFileOutputInferArray.bufferedWriter().use { writer ->
        //     for (bIdx in 0 until BATCH_SIZE) {
        //         for (tIdx in 0 until NUM_TIMESTEPS) {
        //             writer.write(outputArray[bIdx][tIdx].joinToString(" "))
        //             writer.newLine()
        //         }
        //     }
        // }
        //    println("Results written to $logFileInputInfer, $logFileOutputInfer")
        Log.d("LagSpr", "Start RunModel14")

        //////////////////////////// [\RUN MODEL] ////////////////////////////
    }

    private fun getModelInfo(interpreter: Interpreter): String {
        val inputTensorCount = interpreter.inputTensorCount
        val outputTensorCount = interpreter.outputTensorCount

        val inputDetails = (0 until inputTensorCount).joinToString("\n") { i ->
            val tensor = interpreter.getInputTensor(i)
            val shape = tensor.shape().joinToString(", ")
            val type = tensor.dataType()
            "Input Tensor $i: shape=[$shape], type=$type"
        }

        val outputDetails = (0 until outputTensorCount).joinToString("\n") { i ->
            val tensor = interpreter.getOutputTensor(i)
            val shape = tensor.shape().joinToString(", ")
            val type = tensor.dataType()
            "Output Tensor $i: shape=[$shape], type=$type"
        }
        val weightDetails = (0 until interpreter.inputTensorCount).joinToString("\n") { i ->
            val tensor = interpreter.getInputTensor(i)
            val buffer = tensor.asReadOnlyBuffer()
            val shape = tensor.shape().joinToString(", ")
            val type = tensor.dataType()
            "Weight Tensor $i: shape=[$shape], type=$type, values=${bufferToString(buffer, type)}"


        }
        return "Model Info:\n$inputDetails\n$outputDetails\n\nWeight Details:\n$weightDetails"
    }

    private fun bufferToString(buffer: ByteBuffer, type: org.tensorflow.lite.DataType): String {
        return when (type) {
            org.tensorflow.lite.DataType.FLOAT32 -> {
                val floatBuffer = buffer.asFloatBuffer()
                val array = FloatArray(floatBuffer.remaining())
                floatBuffer.get(array)
                array.joinToString(", ")
            }

            org.tensorflow.lite.DataType.INT32 -> {
                val intBuffer = buffer.asIntBuffer()
                val array = IntArray(intBuffer.remaining())
                intBuffer.get(array)
                array.joinToString(", ")
            }

            org.tensorflow.lite.DataType.UINT8 -> {
                val byteArray = ByteArray(buffer.remaining())
                buffer.get(byteArray)
                byteArray.joinToString(", ")
            }

            else -> "Unsupported data type"
        }
    }

    private fun regionProps1D(renumeratedImportData: List<List<String>>): List<Triple<Int, Int, Int>> {
        val targetSubseq = mutableListOf<Triple<Int, Int, Int>>()
        var onset = -1
        var length = 0
        var currentId = -1
        for (i in renumeratedImportData.indices) {
            val id = renumeratedImportData[i][INDEX_TARGET_ID].toInt()
            if (id != currentId) {
                if (currentId > 0) {
                    targetSubseq.add(Triple(onset, length, currentId))
                }
                onset = i
                length = 1
                currentId = id
            } else {
                length++
            }
        }
        if (currentId > 0) {
            targetSubseq.add(Triple(onset, length, currentId))
        }
        Log.d("SprTrainingFun", "regionProps1D")

        return targetSubseq
    }


}