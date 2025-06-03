//package com.bailout.stickk.ubi4.utility
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.content.res.AssetManager
//import android.os.SystemClock
//import android.util.Log
//import android.widget.Chronometer
//import com.bailout.stickk.ubi4.data.state.WidgetState.stateOpticTrainingFlow
//import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
//import com.bailout.stickk.ubi4.utility.Hyperparameters.INDEX_START_FEATURES
//import com.bailout.stickk.ubi4.utility.Hyperparameters.INDEX_TARGET_ID
//import com.bailout.stickk.ubi4.utility.Hyperparameters.INDEX_TARGET_STATE
//import com.bailout.stickk.ubi4.utility.Hyperparameters.NUM_EPOCHS
//import com.google.gson.Gson
//import com.google.gson.annotations.SerializedName
//import com.google.gson.reflect.TypeToken
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.launch
////import org.tensorflow.lite.Interpreter
//import java.io.File
//import java.io.FileInputStream
//import java.io.FileOutputStream
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//import java.nio.FloatBuffer
//import java.nio.channels.FileChannel
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//import java.util.Vector
//import kotlin.math.pow
//import kotlin.math.sqrt
//
//data class Parameters(
//    @SerializedName("model_name") val model_name: String,
//    @SerializedName("timestamp") val timestamp: String,
//    @SerializedName("TARGET_CORRECTION") val TARGET_CORRECTION: Int,
//    @SerializedName("AUTO_SHIFT_RANGE") val AUTO_SHIFT_RANGE: Double,
//    @SerializedName("DATASET_SAMPLING_TOLERANCE_MS") val DATASET_SAMPLING_TOLERANCE_MS: Int,
//    @SerializedName("ALLOW_DATA_DUPLICATES") val ALLOW_DATA_DUPLICATES: Boolean,
//    @SerializedName("ADD_DC_COMPONENT_TO_X") val ADD_DC_COMPONENT_TO_X: Boolean,
//    @SerializedName("PATHS_DATA") val PATHS_DATA: List<List<Any>>,
//    @SerializedName("USE_OMG") val USE_OMG: Int,
//    @SerializedName("USE_EMG") val USE_EMG: Int,
//    @SerializedName("USE_BNO") val USE_BNO: Int,
//    @SerializedName("N_OMG_CH") val N_OMG_CH: Int,
//    @SerializedName("N_EMG_CH") val N_EMG_CH: Int,
//    @SerializedName("N_BNO_CH") val N_BNO_CH: Int,
//    @SerializedName("SAMPLE_CH") val SAMPLE_CH: List<String>,
//    @SerializedName("GESTURES") val GESTURES: List<String>,
//    @SerializedName("N_CLASSES") val N_CLASSES: Int,
//    @SerializedName("LP_ALPHAS") val LP_ALPHAS: List<Double>,
//    @SerializedName("N_LP_ALPHAS") val N_LP_ALPHAS: Int,
//    @SerializedName("X_SCALE") val X_SCALE: List<Double>,
//    @SerializedName("N_FEATURES") val N_FEATURES: Int,
//    @SerializedName("NUM_TIMESTEPS") val NUM_TIMESTEPS: Int,
//    @SerializedName("WIN_SHIFT") val WIN_SHIFT: Int,
//    @SerializedName("MOTORICA_AI_VERSION") val MOTORICA_AI_VERSION: String,
//    @SerializedName("GRU_SIZE") val GRU_SIZE: Int,
//    @SerializedName("GRU_SIZE_2") val GRU_SIZE_2: Int,
//    @SerializedName("BATCH_SIZE") val BATCH_SIZE: Int,
//    @SerializedName("HW_ALPHA") val HW_ALPHA: Double,
//    @SerializedName("NOGO_CLASS_IDX") val NOGO_CLASS_IDX: Int,
//    @SerializedName("DENOISE_WIN_SIZE") val DENOISE_WIN_SIZE: Int,
//    @SerializedName("DENOISE_SWITCH_SIZE") val DENOISE_SWITCH_SIZE: Int,
//    @SerializedName("N_LP_CH") val N_LP_CH: Int,
//    @SerializedName("USE_BIAS") val USE_BIAS: Int,
//    @SerializedName("RESET_AFTER") val RESET_AFTER: Int,
//    @SerializedName("WEIGHTS_LABELS") val WEIGHTS_LABELS: List<String>
//)
//
//class TrainingPipeline(private val context: Context) {
//    private var path: File? = null
//    private lateinit var modelFile: File
//    private lateinit var modelInfo: String
//    private lateinit var preprocessedX: Array<FloatArray>
//    private lateinit var targetArray: Array<FloatArray>
//    private lateinit var tflite: Interpreter
//    private lateinit var assetManager: AssetManager
//    private lateinit var epochsTimer: Chronometer
//    private lateinit var batchesTimer: Chronometer
//    private lateinit var hyperparameters: Parameters
//    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
//    private lateinit var learningTimer: Chronometer
//    private var percentProgressLearningModel:Int = 0
//
//    fun initialize() {
//        path = context.getExternalFilesDir(null)
//        modelFile = path?.let { File(it, "model.ckpt") }
//            ?: throw IllegalStateException("External files directory not available")
//        modelFile.writeBytes(context.assets.open("model.ckpt").readBytes())
//        assetManager = context.assets
//        tflite = loadModelFile("model.tflite")
//        epochsTimer = Chronometer(context)
//        batchesTimer = Chronometer(context)
//        getHyperParameters()
//        scope.launch { stateOpticTrainingFlow.emit(PreferenceKeysUBI4.TrainingModelState.BASE) }
//        Log.d("StateCallBack", "Initialized with state: 0")
//    }
//
//    fun getPercentProgressLearningModel(): Int{
//        return percentProgressLearningModel
//    }
//
//    private fun getHyperParameters() {
//        val phoneConfigFile = File(path, "passport.json")
//        try {
//            hyperparameters = Gson().fromJson(phoneConfigFile.reader(), object : TypeToken<Map<String, Any>>() {}.type)
//        }
//        catch (e: Exception) {
//            assetManager.open("passport.json").use { input ->
//                FileOutputStream(phoneConfigFile).use { output ->
//                    input.copyTo(output)
//                }
//            }
//            hyperparameters = Gson().fromJson(assetManager.open("passport.json").reader(), Parameters::class.java)
//        }
//    }
//
//    private fun handleState(state: PreferenceKeysUBI4.TrainingModelState) {
//
//        when (state) {
//            PreferenceKeysUBI4.TrainingModelState.EXPORT -> {
//                scope.launch {
//                    stateOpticTrainingFlow.emit(PreferenceKeysUBI4.TrainingModelState.EXPORT)
//                }
//                Log.d("StateCallBack", "handleState: export")
//            }
//
//            PreferenceKeysUBI4.TrainingModelState.RUN -> {
//                scope.launch {
//                    stateOpticTrainingFlow.emit(PreferenceKeysUBI4.TrainingModelState.RUN)
//                }
//                Log.d("StateCallBack", "handleState: runModel")
//            }
//
//            PreferenceKeysUBI4.TrainingModelState.BASE -> {
//                scope.launch {
//                    stateOpticTrainingFlow.emit(PreferenceKeysUBI4.TrainingModelState.BASE)
//                }
//                Log.d("StateCallBack", "handleState: BASE")
//            }
//        }
//    }
//
//    private fun loadModelFile(modelPath: String): Interpreter {
//        val assetFileDescriptor = context.assets.openFd(modelPath)
//        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
//        val fileChannel = fileInputStream.channel
//        val startOffset = assetFileDescriptor.startOffset
//        val declaredLength = assetFileDescriptor.declaredLength
//        val model = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
//        return Interpreter(model)
//    }
//
//    private fun standardDeviation(numbers: FloatArray): Float {
//        val mean = numbers.average()
//        val variance = numbers.map { (it - mean).pow(2) }.average()
//        return sqrt(variance).toFloat()
//    }
//
//    fun runModel() {
//        learningTimer = Chronometer(context)
//        learningTimer.base = SystemClock.elapsedRealtime()
//        learningTimer.start()
//
//        Log.d("StateCallBack", " Run runModel")
//        scope.launch {
////            if (stateOpticTrainingFlow.value != PreferenceKeysUBI4.TrainingModelState.RUN) {
////                stateOpticTrainingFlow.emit(PreferenceKeysUBI4.TrainingModelState.RUN)
////                Log.d("StateFlow", "State changed to RUN")
////            }
//            Log.d("StateCallBack", " Run runModel1")
//
//            handleState(PreferenceKeysUBI4.TrainingModelState.RUN)
//            val startTime = System.currentTimeMillis()
//            val endTime = System.currentTimeMillis()
//            try {
//                Log.d("StateCallBack", " Run runModel2")
//
//                //////////////////////////// [LOAD DATA] /////////////////////////////
////                val importData = mutableListOf<List<String>>()
//                Log.d("SprTraining", "assetManager: $assetManager")
//
//                val importData = mutableListOf<List<String>>()
//                val path = context.getExternalFilesDir(null)
//                val regex = Regex("""^serial_data(\d{10})$""")
//                val serialFile = path?.listFiles()
//                    ?.filter { regex.matches(it.name)
//                    } // оставляем только те, что полностью соответствуют шаблону
//                    ?.maxByOrNull { file ->
//                        val numberString = regex.find(file.name)?.groupValues?.get(1) ?: "0"
//                        numberString.toLong()
//                    }
//                Log.d("serialFile", " serialFile: ${serialFile?.name}")
//
//                if (serialFile != null) {
//                    if (!serialFile.exists()) {
//                        Log.e("StateCallBack", "File serial_data does not exist at: ${serialFile.absolutePath}")
//                        return@launch
//                    }
//                }
//
//                serialFile?.bufferedReader()?.useLines { lines ->
//                    // drop header
//                    lines.drop(1).forEach { line ->
//                        val lineData = line.split(" ")
//                        // drop baseline rows
//                        if (lineData[INDEX_TARGET_ID].toDouble().toInt() != -1) {
//                            importData.add(lineData)
//                        }
//                    }
//                }
//                Log.d("StateCallBack", "Start RunModel2 ${endTime - startTime} ms")
//
//                // Create mapping from INDEX_TARGET_STATE to renumeration by order of appearance
//                val stateToId = importData.map { it[INDEX_TARGET_STATE] }
//                    .distinct()
//                    .mapIndexed { index, state -> state to index }
//                    .toMap()
//
//                // Renumerate INDEX_TARGET_ID in importData according to stateToId
//                val renumeratedImportData = importData.map { row ->
//                    row.toMutableList().apply {
//                        this[INDEX_TARGET_ID] = stateToId[this[INDEX_TARGET_STATE]]?.toString() ?: this[INDEX_TARGET_ID]
//                    }
//                }
//
//                val initFeatures = Array(renumeratedImportData.size) { row ->
//                    val rowData = renumeratedImportData[row].slice(INDEX_START_FEATURES until INDEX_START_FEATURES + hyperparameters.N_OMG_CH)
//                        .toMutableList()
//                    if (hyperparameters.USE_EMG == 1) {
//                        rowData += renumeratedImportData[row].slice(INDEX_START_FEATURES + hyperparameters.N_OMG_CH until INDEX_START_FEATURES + hyperparameters.N_OMG_CH + hyperparameters.N_EMG_CH)
//                    }
//                    rowData.map { it.toFloat() }.toFloatArray()
//                }
//
//                Log.d("StateCallBack", "Start RunModel4 ${endTime - startTime} ms")
//                //////////////////////////// [\LOAD DATA] ////////////////////////////
//
//
//                ////////////////////////// [PREPROCESS DATA] /////////////////////////
//                // target shift heuristic
//                val sfreq = 25.0 // TODO: HARDCODED
//                val transition_win_samples = (sfreq * 0.5).toInt()
//
////                initFeatures
////                val omgStd = FloatArray(initFeatures.size){0f}
//               val omgStd = initFeatures.mapIndexed { index, row ->
//                    if (index == 0) {
//                        0f // For the first row, we can't calculate the difference, so we use 0
//                    } else {
//                        row.zip(initFeatures[index - 1]) { a, b -> Math.abs(a - b) }.sum()//a, b -> Math.abs(a - b)
//                    }
//                }.toFloatArray()
//                val targetSubseq = regionProps1D(renumeratedImportData)
//                val rectCoo = mutableMapOf<Int, Triple<Int, Int, Int>>()
//                for ((index, triple) in targetSubseq.withIndex()) {
//                    val (onset, _length, targetId) = triple
//                    var length = _length
//                    length = (1.1 * length).toInt()
//                    var maxArea = 0f
//                    var argmaxShift = 0
//                    for (shift in 0 until (hyperparameters.AUTO_SHIFT_RANGE * length).toInt()) {
//                        if (length > omgStd.size - onset - shift) {
//                            length = omgStd.size - onset - shift
//                        }
//                        val area = omgStd.slice(onset + shift until onset + length + shift).let { slice ->
//                            // analog of `np.trapz`
//                            slice.zipWithNext { a, b -> (a + b) / 2 }.sum() * 1 // Assuming time step is 1
//                        }
//                        if (area > maxArea) {
//                            maxArea = area
//                            argmaxShift = shift
//                        }
//                    }
//                    rectCoo[index] = Triple(onset + argmaxShift, length, targetId)
//                }
//                val targetArray1D = IntArray(initFeatures.size)
//                for ((_, triple) in rectCoo) {
//                    val (onset, length, targetId) = triple
//                    for (i in onset + transition_win_samples until onset + length - transition_win_samples) {
//                        if (i < targetArray1D.size) {
//                            targetArray1D[i] = targetId
//                        }
//                    }
//                }
//                val targetArray = Array(initFeatures.size) { FloatArray(hyperparameters.N_CLASSES) }
//                for (i in 0 until initFeatures.size) {
//                    targetArray[i][targetArray1D[i]] = 1.0f
//                }
//
//                val preprocessedX = Array(initFeatures.size) { FloatArray(hyperparameters.N_FEATURES) }
//                var n_lp_ch = hyperparameters.N_OMG_CH
//                if (hyperparameters.USE_EMG == 1) {
//                    n_lp_ch += hyperparameters.N_EMG_CH
//                }
//                val x_lp = FloatArray(n_lp_ch * hyperparameters.N_LP_ALPHAS)
//                val x_curr = FloatArray(hyperparameters.N_FEATURES)
//                val x_features = FloatArray(n_lp_ch)
//                val X_SCALE = FloatArray(hyperparameters.N_FEATURES) { 1f }
//                for (i in 0 until initFeatures.size) {
//                    // fill x_features
//                    for (k in 0 until hyperparameters.N_OMG_CH) {
//                        x_features[k] = initFeatures[i][k]
//                    }
//                    if (hyperparameters.USE_EMG == 1) {
//                        for (k in 0 until hyperparameters.N_EMG_CH) {
//                            x_features[hyperparameters.N_OMG_CH + k] = initFeatures[i][hyperparameters.N_OMG_CH + k]
//                        }
//                    }
//                    // HP compute
//                    for (j in 0 until hyperparameters.N_LP_ALPHAS) {
//                        for (k in 0 until n_lp_ch) {
//                            x_lp[j * n_lp_ch + k] = (hyperparameters.LP_ALPHAS[j] * x_features[k] + (1 - hyperparameters.LP_ALPHAS[j]) * x_lp[j * n_lp_ch + k]).toFloat()
//                            x_curr[j * n_lp_ch + k] = x_features[k] - x_lp[j * n_lp_ch + k]
//                        }
//                    }
//                    if (hyperparameters.ADD_DC_COMPONENT_TO_X) {
//                        // copy X to x_curr
//                        for (k in 0 until n_lp_ch) {
//                            x_curr[n_lp_ch * hyperparameters.N_LP_ALPHAS + k] = x_features[k]
//                        }
//                    }
//                    // copy x_curr to preprocessedX
//                    for (j in 0 until hyperparameters.N_FEATURES) {
//                        preprocessedX[i][j] = x_curr[j]
//                    }
//                }
//                // scale compute // TODO: повысить точность расчета
//                for (j in 0 until hyperparameters.N_FEATURES) {
//                    X_SCALE[j] = standardDeviation(preprocessedX.map { it[j] }.toFloatArray()).coerceAtLeast(1e-6f)
//                }
//                // scale apply
//                for (i in 0 until preprocessedX.size) {
//                    for (j in 0 until hyperparameters.N_FEATURES) {
//                        preprocessedX[i][j] /= X_SCALE[j]
//                    }
//                }
//                Log.d("StateCallBack", "Start RunModel7 ${endTime - startTime} ms")
//
//                val currentDateTime = getCurrentDateTime()
//
//                val binaryFileX_SCALE = File(path, "params_$currentDateTime.bin")
//                binaryFileX_SCALE.outputStream().use { output ->
//                    val byteBuffer = ByteBuffer.allocate(X_SCALE.size * 4) // 4 bytes for each float
//                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN) // Ensure little-endian order
//                    for (scale in X_SCALE) {
//                        byteBuffer.putFloat(scale)
//                    }
//                    output.write(byteBuffer.array())
//                }
//
//                ///////////////////////// [\PREPROCESS DATA] /////////////////////////
//
//
//                //////////////////////////// [LOAD MODEL] ////////////////////////////
//                // Import prior weights from a checkpoint file.
//                // populate with preset ckpt in assets directory
//                Log.d("TFLite", "Interpreter initialized: $tflite")
//                modelInfo = getModelInfo(tflite)
//
//                // Restore the model from the checkpoint file
//                val ckpt = modelFile.absolutePath
//                val inputs_ckpt = HashMap<String, Any>()
//                inputs_ckpt.put("checkpoint_path", ckpt)
//                val outputs_ckpt = HashMap<String, Any>()
//                tflite.runSignature(inputs_ckpt, outputs_ckpt, "restore")
//                train(preprocessedX, targetArray)
//                export(path, currentDateTime)
//                run(preprocessedX)
//                Log.d("StateCallBack", "Start RunModel8 ${endTime - startTime} ms")
//            } catch (e: Exception) {
//                Log.d("StateCallBack", " Runmodel ERROR ${e.message}")
//            }
//
//        }
//    }
//
//    @SuppressLint("LogNotTimber")
//    private fun train(preprocessedX: Array<FloatArray>, targetArray: Array<FloatArray>) {
//        //////////////////////////// [TRAIN MODEL] ////////////////////////////
//        // train the model
//        // https://ai.google.dev/edge/litert/models/ondevice_training
//        // Формирование индексов для формирования батчей
//        // TODO: need to be changed to random indexes
//        val indexes = IntRange(0, preprocessedX.size - hyperparameters.NUM_TIMESTEPS - 1).step(hyperparameters.WIN_SHIFT).toList()
//        // Shuffle the indexes
//        val num_batches = indexes.size / hyperparameters.BATCH_SIZE
//        // TODO: удалить в будующем, тк используется только для логирования
//        val indexesAllBatches = Array(num_batches) { IntArray(hyperparameters.BATCH_SIZE) }
//        for (i in 0 until num_batches) {
//            for (j in 0 until hyperparameters.BATCH_SIZE) {
//                indexesAllBatches[i][j] = indexes[i * hyperparameters.BATCH_SIZE + j]
//            }
//        }
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
//            epochsTimer = Chronometer(context)
//            epochsTimer.base = SystemClock.elapsedRealtime()
//            epochsTimer.start()
//            val shuffledIndexes = indexes.shuffled()
//            val shuffledIndexesAllBatches = Array(num_batches) { IntArray(hyperparameters.BATCH_SIZE) }
//            for (i in 0 until num_batches) {
//                for (j in 0 until hyperparameters.BATCH_SIZE) {
//                    shuffledIndexesAllBatches[i][j] = shuffledIndexes[i * hyperparameters.BATCH_SIZE + j]
//                }
//            }
//            val logFileShuffledIndexesAllBatches = File(path, "log_shuffled_indexes_batches_${epoch}.txt")
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
//                batchesTimer = Chronometer(context)
//                batchesTimer.base = SystemClock.elapsedRealtime()
//                batchesTimer.start()
//
//                // construct batch frame
//                val batchIndexes = shuffledIndexesAllBatches[batchIdx]
//                val batchX = FloatBuffer.allocate(hyperparameters.BATCH_SIZE * hyperparameters.NUM_TIMESTEPS * hyperparameters.N_FEATURES)
//                val batchLabels = FloatBuffer.allocate(hyperparameters.BATCH_SIZE * hyperparameters.NUM_TIMESTEPS * hyperparameters.N_CLASSES)
//                batchX.rewind()
//                batchLabels.rewind()
//
//                // Fill the data
//                for (bIdx in 0 until hyperparameters.BATCH_SIZE) {
//                    val batchIndex = batchIndexes[bIdx]
//                    for (tIdx in 0 until hyperparameters.NUM_TIMESTEPS) {
//                        for (wIdx in 0 until hyperparameters.N_FEATURES) {
//                            batchX.put(preprocessedX[batchIndex + tIdx][wIdx])
//                        }
//                        for (cIdx in 0 until hyperparameters.N_CLASSES) {
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
//                Log.i("timer_batches", (SystemClock.elapsedRealtime() - batchesTimer.base).toString())
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
//            percentProgressLearningModel = epoch +1
//            Log.i("epochs_counter", "Finished ${epoch + 1} epochs, current loss: ${lossCalc.get(0)}")
//        }
//
//        Log.d("StateCallBack", "Start RunModel10")
//    }
//
//    private fun getNextCheckpointNumber(): Int {
//        val sharedPreferences = context.getSharedPreferences("training_model_prefs", Context.MODE_PRIVATE)
//        val currentNumber = sharedPreferences.getInt("checkpoint_number", 1)
//        sharedPreferences.edit().putInt("checkpoint_number", currentNumber + 1).apply()
//        return currentNumber
//    }
//
//    private fun getCurrentDateTime(): String {
//        val dateFormat = SimpleDateFormat("MM-dd_HH-mm-ss", Locale.getDefault())
//        return dateFormat.format(Date())
//    }
//
//    private fun export(path: File?, currentDateTime: String) {
//
//        scope.launch {
//            Log.d("StateCallBack", "Start RunModel11")
//            //////////////////////////// [EXPORT MODEL] ////////////////////////////
//            // Export the trained weights as a checkpoint file.
//            handleState(PreferenceKeysUBI4.TrainingModelState.EXPORT)
//
//
//            val checkpointNumber = getNextCheckpointNumber()
//
//            val outputFile = File(path, "checkpoint_№${checkpointNumber}_$currentDateTime")
//
//            try {
//
//                val inputs = HashMap<String, Any>()
//                inputs.put("checkpoint_path", outputFile.absolutePath)
//                val outputs = HashMap<String, Any>()
//                tflite.runSignature(
//                    inputs,
//                    outputs,
//                    "save"
//                )
//            } finally {
//                Log.d("StateCallBack", "finaly: EXPORT")
//
//            }
//
//            // This file should be then transferred to the microcontroller
//            Log.d("StateCallBack", "Start RunModel12")
//            //////////////////////////// [\EXPORT MODEL] ////////////////////////////
//        }
//    }
//
//    private fun run(preprocessedX: Array<FloatArray>) {
//        Log.d("StateCallBack", "Start RunModel13")
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
//        val inputData = FloatBuffer.allocate(hyperparameters.BATCH_SIZE * hyperparameters.NUM_TIMESTEPS * hyperparameters.N_FEATURES)
//        inputData.rewind()
//        for (bIdx in 0 until hyperparameters.BATCH_SIZE) {
//            for (tIdx in 0 until hyperparameters.NUM_TIMESTEPS) {
//                for (wIdx in 0 until hyperparameters.N_FEATURES) {
//                    try {
//                        inputData.put(preprocessedX[bIdx * hyperparameters.NUM_TIMESTEPS + tIdx][wIdx])
//                    }
//                    catch (e: Exception) {
//                        Log.i("runModelErrors", e.message.toString())
//                    }
//                }
//            }
//        }
//        percentProgressLearningModel = 0
//        handleState(PreferenceKeysUBI4.TrainingModelState.BASE)
//        inputData.rewind()
//
//        // Prepare output buffer
//        val outputData = FloatBuffer.allocate(hyperparameters.BATCH_SIZE * hyperparameters.NUM_TIMESTEPS * hyperparameters.N_CLASSES)
//        outputsRun["output"] = outputData
//
//        inputsRun["x"] = inputData
//        tflite.runSignature(inputsRun, outputsRun, "infer") // MAX: why we need `outputsRun`?
//        // Write results
//        outputData.rewind()
//
//        Log.d("LagSpr", "Start RunModel14")
//        //////////////////////////// [\RUN MODEL] ////////////////////////////
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
//
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
//        Log.d("SprTrainingFun", "regionProps1D")
//
//        return targetSubseq
//    }
//
//
//}