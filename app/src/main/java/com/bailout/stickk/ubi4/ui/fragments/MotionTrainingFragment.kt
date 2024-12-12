package com.bailout.stickk.ubi4.ui.fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Chronometer
import androidx.fragment.app.Fragment
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4FragmentMotionTrainingBinding
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.local.OpticTrainingStruct
import com.bailout.stickk.ubi4.data.parser.BLEParser
import com.bailout.stickk.ubi4.models.ConfigOMGDataCollection
import com.bailout.stickk.ubi4.models.GestureConfig
import com.bailout.stickk.ubi4.models.GesturePhase
import com.bailout.stickk.ubi4.models.GesturesId
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.SprGestureItemsProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.roundToInt

class MotionTrainingFragment(
    val onFinishTraining: () -> Unit,
) : Fragment() {

    private var _bindig: Ubi4FragmentMotionTrainingBinding? = null
    private val binding get() = _bindig!!

    private var timer: CountDownTimer? = null
    private var preparationTimer: CountDownTimer? = null
    private var isCountingDown = false
    private lateinit var learningTimer: Chronometer
    private lateinit var learningStepTimer: Chronometer
    private var loggingFilename = "serial_data"
    private val disposables = CompositeDisposable()
    private val rxUpdateMainEvent = RxUpdateMainEventUbi4.getInstance()
    private val gestures: MutableList<Map<String, String>> = mutableListOf()
    private var prot = 0
    private var startLineInLearningTable = 0
    private var dataCollection: Map<String, Any> = mapOf()
    private lateinit var gestureConfig: GestureConfig
    private lateinit var sprGestureItemsProvider: SprGestureItemsProvider

    private var nCycles: Int = 0
    private var gestureSequence: List<String> = emptyList()
    private var gestureNumber: Int = 0
    private var preGestDuration: Double = 0.0
    private var atGestDuration: Double = 0.0
    private var postGestDuration: Double = 0.0
    private var gestureDuration: Int = 0
    private var gesturesId: GesturesId? = null
    private var baselineDuration: Double = 0.0
    private var generalTime: Long = 0L

    private var lineData: MutableList<GesturePhase> = mutableListOf()
    private var currentPhaseIndex: Int = 0


    @SuppressLint("CheckResult", "DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LagSpr", "Motion onCreate")
        //loadGestureConfig()
//        val result = trainingDataProcessing()
//        result.forEach { Log.d("trainingDataProcessing", "trainingDataProcessing ${it}") }
        deleteSerialDataFile()

        val mBLEParser = main.let { BLEParser(it) }
        //фейковые данные принимаемого потока
//        android.os.Handler().postDelayed({
//            mBLEParser?.parseReceivedData(BLECommands.testDataTransfer())
//        }, 1000)
//        Handler().postDelayed({
//            mBLEParser?.parseReceivedData(BLECommands.testDataTransfer())


        val parameter = ParameterProvider.getParameter(6, 15)
        Log.d("TestOptic", "OpticTrainingStruct = ${parameter.parameterDataSize}")
        val opticStreamDisposable = rxUpdateMainEvent.uiOpticTrainingObservable
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe { dataCode ->
                Log.d("WriteFileDebugCheck", "Received dataCode: $dataCode")

                try {
                    val dataStringRaw = parameter.data
                    if (dataStringRaw.isBlank() || dataStringRaw == "None") {
                        Log.e("TestFileContain", "Data is empty or invalid")
                        return@subscribe
                    }

                    val opticTrainingStruct =
                        Json.decodeFromString<OpticTrainingStruct>("\"${parameter.data}\"")
                    val dataString = opticTrainingStruct.data.joinToString(separator = " ") {
                        String.format("%.1f", it)
                    }
                    Log.d("WriteFileDebugCheck", "OpticTrainingStruct = $dataString")
                    Log.d("WriteFileDebugCheck", "Number Frame = ${opticTrainingStruct.numberOfFrame}")
                    writeToFile(dataString)
                } catch (e: Exception) {
                    Log.e("TestOptic", "Error decoding data: ${e.message}")
                }
            }
        disposables.add(opticStreamDisposable)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("LagSpr", "Motion onCreateView")

        _bindig = Ubi4FragmentMotionTrainingBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("LagSpr", "Motion onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        binding.stopTrainingBtn.setOnClickListener {
            showConfirmCancelTrainingDialog {
                parentFragmentManager.beginTransaction().replace(
                    R.id.fragmentContainer, SprTrainingFragment()
                ).commit()
            }
        }
        learningTimer = Chronometer(requireContext())
        sprGestureItemsProvider = SprGestureItemsProvider(requireContext())
        lineData = trainingDataProcessing()
        //learningPreprocessingParse()

        // Проверяем и объединяем BaseLine и Neutral
        if (lineData.size > 1 &&
            lineData[0].gestureName == "BaseLine" &&
            lineData[1].gestureName == "Neutral"
        ) {
            val combinedPhase = lineData[0].copy(
                timeGesture = lineData[0].timeGesture + lineData[1].timeGesture
            )
            lineData[0] = combinedPhase
            lineData.removeAt(1)

            switchAnimationSmoothly(lineData[0].animation)
        }


        startPhase(currentPhaseIndex)

    }


    @SuppressLint("MissingInflatedId")
    fun showConfirmCompletedTrainingDialog(confirmClick: () -> Unit) {
        stopTimers()
        preparationTimer?.cancel()
        val dialogBinding =
            layoutInflater.inflate(R.layout.ubi4_dialog_confirm_finish_training, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        val confirmBtn = dialogBinding.findViewById<View>(R.id.ubi4CompletedTrainingBtn)
        confirmBtn.setOnClickListener {
            myDialog.dismiss()
            confirmClick()
            onFinishTraining()
        }
    }

    @SuppressLint("MissingInflatedId")
    fun showConfirmCancelTrainingDialog(confirmClick: () -> Unit) {
        stopTimers()
        preparationTimer?.cancel()
        val dialogBinding =
            layoutInflater.inflate(R.layout.ubi4_dialog_cancel_training, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        val confirmBtn = dialogBinding.findViewById<View>(R.id.ubi4DialogConfirmCancelTrainingBtn)
        confirmBtn.setOnClickListener {
            stopTimers()
            myDialog.dismiss()
            confirmClick()
        }
        val cancelBtn = dialogBinding.findViewById<View>(R.id.ubi4DialogCancelTrainingCancelBtn)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
            resumeTimers()
        }

    }

    private fun stopTimers() {
        timer?.cancel()
        preparationTimer?.cancel()
    }

    private fun resumeTimers() {
        if (isCountingDown) {
            //startCountdown()
        } else {
            //startPreparationCountDown()
        }
    }


    private fun startPhase(phaseIndex: Int) {
        currentPhaseIndex = phaseIndex
        if (phaseIndex >= lineData.size) {
            Log.d("DebugCheck", "All phases completed. Showing finish dialog.")
            // Все фазы завершены
            showConfirmCompletedTrainingDialog {
                parentFragmentManager.beginTransaction().replace(
                    R.id.fragmentContainer, SprTrainingFragment()
                ).commitNow()
            }
            return
        }

        val currentPhase = lineData[phaseIndex]
        Log.d("DebugCheck", "Current phase: ${currentPhase.gestureName}, animationId: ${currentPhase.animation}")

        if (currentPhase.gestureName == "Neutral") {
            switchAnimationSmoothly(currentPhase.animation)
        }
        when {
            currentPhase.gestureName == "Neutral" || currentPhase.gestureName == "BaseLine" || currentPhase.gestureName == "Finish" -> {
                binding.motionHandIv.playAnimation()
                startPreparationCountDown(currentPhase, phaseIndex)
            }

            else -> {
                // Активная фаза
                binding.motionHandIv.setAnimation(currentPhase.animation)
                binding.motionHandIv.playAnimation()
                startCountdown(currentPhase, phaseIndex)
            }
        }
    }

    private fun startCountdown(phase: GesturePhase, phaseIndex: Int) {
        stopTimers()
        val countDownTime = (phase.timeGesture * 1000).toLong()
        val interval = 30L
        // Обновление названия текущего жеста
        binding.motionNameOfGesturesTv.text = phase.headerText
        binding.prepareForPerformTv.visibility = View.VISIBLE
        binding.prepareForPerformTv.text = phase.description
        binding.motionProgressBar.max = (countDownTime / interval).toInt()
        binding.motionProgressBar.progress = 0

        binding.countdownTextView.visibility = View.VISIBLE
        binding.motionProgressBar.visibility = View.VISIBLE

        timer = object : CountDownTimer(countDownTime, interval) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                binding.countdownTextView.text = secondsRemaining.toString()
                val progress = ((countDownTime - millisUntilFinished) / interval).toInt()
                binding.motionProgressBar.progress = progress
            }

            override fun onFinish() {
                binding.countdownTextView.text = "0"
                binding.countdownTextView.visibility = View.GONE
                binding.motionProgressBar.progress = 0
                // Переход к следующей фазе
                startPhase(phaseIndex = phaseIndex + 1)
                Log.d("DebugCheck", "Countdown finished for phase index: $currentPhaseIndex")
            }
        }.start()
    }

    private fun startPreparationCountDown(phase: GesturePhase, phaseIndex: Int) {
        stopTimers()
        val pauseBeforeStart = (phase.timeGesture * 1000).toLong()

        // Определение следующего жеста
        val nextGestureName = getNextGestureName(phaseIndex)
        Log.d("GestureName", "gestureName: $nextGestureName")



        if (nextGestureName.isEmpty()) {
            // Следующего жеста нет, это последний этап
            binding.motionNameOfGesturesTv.text = "Можете расслабить руку"
            binding.prepareForPerformTv.text = "Дождитесь окончания"
            binding.prepareForPerformTv.visibility = View.VISIBLE

            // Можно установить дефолтную картинку или скрыть ImageView
            switchAnimationSmoothly(R.raw.clock)
            binding.motionProgressBar.visibility = View.INVISIBLE
            binding.countdownTextView.visibility = View.VISIBLE
            binding.countdownTextView.text = (pauseBeforeStart / 1000).toString()

            preparationTimer = object : CountDownTimer(pauseBeforeStart, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val secondsRemaining = (millisUntilFinished / 1000).toInt()
                    binding.countdownTextView.text = secondsRemaining.toString()
                }

                override fun onFinish() {
                    binding.prepareForPerformTv.visibility = View.GONE
                    binding.countdownTextView.visibility = View.GONE
                    binding.motionProgressBar.visibility = View.INVISIBLE
                    Log.d("DebugCheck", "Preparation countdown finished. Ending training.")
                    // Так как это последний жест, можно завершить тренировку
                    showConfirmCompletedTrainingDialog {
                        parentFragmentManager.beginTransaction().replace(
                            R.id.fragmentContainer, SprTrainingFragment()
                        ).commitNow()
                    }
                }
            }.start()

        } else {

            if (phase.gestureName == "Neutral") {
                switchAnimationSmoothly(phase.animation)
            }
            binding.motionNameOfGesturesTv.text = "Следующий жест $nextGestureName"
            binding.prepareForPerformTv.text = "Подготовьтесь к выполнению жеста"
            binding.prepareForPerformTv.visibility = View.VISIBLE
            binding.motionProgressBar.visibility = View.INVISIBLE
            binding.countdownTextView.visibility = View.VISIBLE
            binding.countdownTextView.text = (pauseBeforeStart / 1000).toString()

            preparationTimer = object : CountDownTimer(pauseBeforeStart, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val secondsRemaining = (millisUntilFinished / 1000).toInt()
                    binding.countdownTextView.text = secondsRemaining.toString()
                }

                override fun onFinish() {
                    binding.prepareForPerformTv.visibility = View.GONE
                    binding.countdownTextView.visibility = View.GONE
                    binding.motionProgressBar.visibility = View.VISIBLE
                    Log.d("DebugCheck", "Preparation countdown finished for phase index: $currentPhaseIndex")
                    startPhase(phaseIndex = phaseIndex + 1)
                }
            }.start()
        }
    }

    private fun switchAnimationSmoothly(newAnimation: Int) {
        binding.motionHandIv.animate()
            .setDuration(150)
            .scaleX(0f)
            .scaleY(0f)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                binding.motionHandIv.setAnimation(newAnimation)
                binding.motionHandIv.playAnimation()
                binding.motionHandIv.addAnimatorUpdateListener { animation ->
                    if (animation.animatedFraction >= 0.5f) {
                        binding.motionHandIv.pauseAnimation()
                    }
                }
                binding.motionHandIv.animate()
                    .setDuration(150)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
            }.start()
    }

    private fun getNextGestureName(currentIndex: Int): String {
        var nextIndex = currentIndex + 1

        while (nextIndex < lineData.size) {
            val nextPhase = lineData[nextIndex]
            if (nextPhase.gestureName != "BaseLine" && nextPhase.gestureName != "Neutral" && nextPhase.gestureName != "Finish") {
                val gestureName =
                    sprGestureItemsProvider.getNameGestureByKeyName(nextPhase.gestureName)
                return gestureName ?: ""
            }
            nextIndex++
        }

        return ""
    }


    private val fileLock = Any()
    private fun writeToFile(data: String, isAppend: Boolean = true) {
//        Log.i("WriteFileDebugCheck", "currentPhaseIndex: $currentPhaseIndex")
        synchronized(fileLock) {

            try {
                learningTimer.base = SystemClock.elapsedRealtime()
                learningTimer.start()
                val path = requireContext().getExternalFilesDir(null)
                val file = File(path, loggingFilename)

                if (lineData.isEmpty()) {
                    Log.e("WriteFileDebugCheck", "lineData $lineData")

                    Log.e("WriteFileDebugCheck", "No phases available in lineData")
                    return
                }
                val currentPhase = lineData.getOrNull(currentPhaseIndex) ?: run {
                    return
                }
//            Log.d("WriteFileDebugCheck", "Current phase: ${currentPhase.gestureName}, gestureId: ${currentPhase.gestureId}")
                val gestureId = currentPhase.gestureId
                if (!file.exists()) {
                    file.writeText(
                        "ts td omg0 omg1 omg2 omg3 omg4 omg5 omg6 omg7 omg8 omg9 omg10 omg11 omg12 omg13 omg14 omg15 " +
                                "emg0 emg1 emg2 emg3 emg4 emg5 emg6 emg7 bno0 bno1 bno2 prb0 prb1 prb2 prb3 prb4 prb5 " +
                                "prb6 prb7 argmax denoize prot state id now\n"
                    )
                }
                var line = ""
                if (data.isNotEmpty())
                    line = data.dropLast(2)

                if (isAppend) {
                    val logLine = "$line $prot ${currentPhase.gestureName} $gestureId ${
                        ((currentPhase.timeGesture.toDouble() / 10).roundToInt() / 100.0).toString()
                    }\n"
                    Log.d("WriteFileDebugCheck", "LOGLINE: $logLine")
                    //                file.appendText(
//                    "$line $prot ${curData[]} ${curData["id"]} ${
//                        ((curData["generalTime"].toDouble()?.div(10)?.roundToInt()
//                            ?.div(100.0)) ?: curData[generalTime.toInt()]).toString()
//                  }\n"
//               )
                    file.appendText(logLine)
                    Log.d("trainingDataProcessing1", "LOGLINE:${logLine.trim()}")
                } else {
                    file.writeText(line)
                }
                prot++
                val fileContent = file.readText()
                Log.i("FileInfoWriteFile", "File contain: ${fileContent.lines().size} lines")

            } catch (e: IOException) {
                Log.i("WriteFileDebugCheck", "File writing failed: $e")
            }
        }
    }
    private fun deleteSerialDataFile() {
        val path = requireContext().getExternalFilesDir(null)
        val file = File(path, "serial_data")
        if (file.exists()) {
            val deleted = file.delete()
            Log.d("FileDeletion", "File serial_data deleted: $deleted")
        } else {
            Log.d("FileDeletion", "File serial_data does not exist.")
        }
    }


//    private fun learningPreprocessingParse() {
//        var lines = requireContext().assets.open("data.emg8.protocol").bufferedReader().readLines()
//        lines = lines.drop(1)
//        for (line in lines) {
//            gestures.add(
//                mapOf(
//                    "n" to line.split(",")[0],
//                    "state" to line.split(",")[1],
//                    "id" to line.split(",")[2],
//                    "indicativeTime" to line.split(",")[4]
//                )
//            )
//        }
//        dataCollection = Gson().fromJson(
//            requireContext().assets.open("config.json").reader(),
//            object : TypeToken<Map<String, Any>>() {}.type
//        )
//    }



    private fun trainingDataProcessing(): MutableList<GesturePhase> {
        dataCollection = Gson().fromJson(
            requireContext().assets.open("config.json").reader(),
            object : TypeToken<Map<String, Any>>() {}.type
        )
        val json =
            requireContext().assets.open("config.json").bufferedReader().use { it.readText() }
        val gson = Gson()
        val config: ConfigOMGDataCollection =
            gson.fromJson(json, ConfigOMGDataCollection::class.java)

        var lineData = mutableListOf<GesturePhase>()
        var previousGesture: GesturePhase? = null


        generalTime = (SystemClock.elapsedRealtime() - learningTimer.base) / 1000
        nCycles = config.nCycles ?: 0
//        Log.d("trainingDataProcessing", "nCycles $nCycles")
        gestureSequence = config.gestureSequence
        Log.d("GestureDebug", "gestureSequence $gestureSequence")
        gestureNumber = (gestureSequence as ArrayList<*>).size - 1
//        Log.d("trainingDataProcessing", "gestureNumber $gestureNumber")
        preGestDuration = config.preGestDuration?.toDouble() ?: 0.0
//        Log.d("trainingDataProcessing", "preGestDuration $preGestDuration")
        atGestDuration = config.atGestDuration?.toDouble() ?: 0.0
//        Log.d("trainingDataProcessing", "atGestDuration $atGestDuration")
        postGestDuration = config.postGestDuration?.toDouble() ?: 0.0
//        Log.d("trainingDataProcessing", "postGestDuration $postGestDuration")
        gestureDuration = (preGestDuration + atGestDuration + postGestDuration).toInt()
//        Log.d("trainingDataProcessing", "gestureDuration $gestureDuration")
        gesturesId = config.gesturesId
//        Log.d("trainingDataProcessing", "gesturesId ${gesturesId}")
        baselineDuration = config.baselineDuration?.toDouble() ?: 0.0
//        Log.d("trainingDataProcessing", "baselineDuration $baselineDuration")

        val firsGestureName = gestureSequence.firstOrNull() ?: ""
        val firstGestureAnimation =
            sprGestureItemsProvider.getAnimationIdByKeyNameGesture(firsGestureName)


//        val currentTime = generalTime - baselineDuration
//        val currentLoop = (currentTime / (gestureNumber * gestureDuration)).toInt()
//        val timeInLoop = currentTime % (gestureNumber * gestureDuration)
//        val gestureInd = (timeInLoop / gestureDuration).toInt() + 1
//        val timeInGesture = timeInLoop % gestureDuration

        lineData.add(
            GesturePhase(
                prePhase = 0.0,
                timeGesture = baselineDuration,
                postPhase = 0.0,
                animation = firstGestureAnimation,
                headerText = "Подготовьтесь к выполнению первого жеста",
                description = "Подготовьтесь к выполнению первого жеста",
                gestureName = "BaseLine",
                gestureId = -1
            )
        )

        //добавление этапов для каждого жеста
        gestureSequence.forEachIndexed { index, keyName ->
            val animationId =
                keyName?.let { sprGestureItemsProvider.getAnimationIdByKeyNameGesture(it) }
                    ?: R.drawable.sleeping

            val gestureId = gesturesId?.getGestureValueByName(keyName) ?: run {
                Log.e("MotionTrainingFragment", "Unknown gesture name: $keyName")
                0
            }

            if (index < gestureSequence.size) {
                // Получение ID для Neutral фазы
                val neutralId = gesturesId?.getGestureValueByName("Neutral") ?: run {
                    Log.e("MotionTrainingFragment", "Neutral gesture ID not found")
                    0
                }
                val neutralPhase = GesturePhase(
                    prePhase = 0.0,
                    timeGesture = postGestDuration + preGestDuration,
                    postPhase = 0.0,
                    animation = animationId,
                    headerText = "Подготовьтесь к выполнению жеста",
                    description = "Отдохните перед следующим жестом",
                    gestureName = "Neutral",
                    gestureId = neutralId
                )
                lineData.add(neutralPhase)

            }

            val currentGesture = GesturePhase(
                prePhase = preGestDuration,
                timeGesture = atGestDuration,
                postPhase = postGestDuration,
                animation = animationId,
                headerText = sprGestureItemsProvider.getNameGestureByKeyName(keyName),
                description = "Выполните жест: ${sprGestureItemsProvider.getNameGestureByKeyName((keyName))}",
                gestureName = keyName,
                gestureId = gestureId
            )
            lineData.add(currentGesture)


        }
        lineData.add(
            GesturePhase(
                prePhase = 0.0,
                timeGesture = baselineDuration,
                postPhase = 0.0,
                animation = 0,
                headerText = "Отдохните перед следующим жестом",
                description = "Отдохните перед следующим жестом",
                gestureName = "Finish",
                gestureId = -1
            )
        )

        Log.d("DebugCheck", "Training phases generated: ${lineData.size}")
        lineData.forEachIndexed { idx, phase ->
            Log.d("DebugCheck", "Phase $idx: ${phase.gestureName}")
        }

        return lineData
    }

    private fun stopTraining() {
        // Отмена всех таймеров
        timer?.cancel()
        preparationTimer?.cancel()

        // Переход к другому фрагменту или выполнению другой логики
        parentFragmentManager.beginTransaction().replace(
            R.id.fragmentContainer, SprTrainingFragment()
        ).commitNow()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LagSpr", "Motion onDestroy")
        _bindig = null
        timer?.cancel()
        preparationTimer?.cancel()
        disposables.clear()
    }


}

//    private fun writeToFile(data: String, isAppend: Boolean = true) {
//        Log.i("FileInfo", "WriteFile Start")
//        try {
//            // Запуск или сброс таймера здесь может быть неуместен,
//            // если он уже управляется в другом месте (например, в executeTraining).
//            // Если необходимо, оставьте эти строки.
//            learningTimer.base = SystemClock.elapsedRealtime()
//            learningTimer.start()
//
//            val path = requireContext().getExternalFilesDir(null)
//            val file = File(path, loggingFilename)
//
//            // Получаем список фаз жестов
//            val gesturePhases = trainingDataProcessing()
//            Log.d("GestureUpdate", "gesturePhases = $gesturePhases ")
//
//            // Если файл не существует, создаём его и записываем заголовок
//            if (!file.exists()) {
//                file.writeText(
//                    "ts td omg0 omg1 omg2 omg3 omg4 omg5 omg6 omg7 omg8 omg9 omg10 omg11 omg12 omg13 omg14 omg15 " +
//                            "emg0 emg1 emg2 emg3 emg4 emg5 emg6 emg7 bno0 bno1 bno2 prb0 prb1 prb2 prb3 prb4 prb5 " +
//                            "prb6 prb7 argmax denoize prot state id now\n"
//                )
//            }
//
//            var line = ""
//            if (data.isNotEmpty())
//                line = data.dropLast(2)
//
//            if (isAppend) {
//                // Получаем последнюю (текущую) фазу жеста
//                val currentPhase = gesturePhases.lastOrNull()
//                if (currentPhase != null) {
//                    // Форматируем generalTime по вашей логике
//                    val formattedGeneralTime = try {
//                        (currentPhase.timeGesture / 10).roundToInt() / 100.0
//                    } catch (e: NumberFormatException) {
//                        currentPhase.timeGesture.toString()
//                    }
//
//                    // Формируем строку для записи
//                    val logLine = "$line $prot ${currentPhase.gestureName} ${currentPhase.gestureId} $formattedGeneralTime\n"
//
//                    // Записываем строку в файл
//                    file.appendText(logLine)
//
//                    // Логируем запись
//                    Log.d("trainingDataProcessing", logLine.trim())
//                } else {
//                    // Если текущая фаза отсутствует, записываем без неё
//                    val logLine = "$line $prot Unknown Unknown Unknown\n"
//                    file.appendText(logLine)
//                    Log.d("trainingDataProcessing", logLine.trim())
//                }
//            } else {
//                // Если не нужно добавлять, просто записываем строку
//                file.writeText(line)
//                Log.d("trainingDataProcessing", line.trim())
//            }
//
//            // Увеличиваем протокол
//            prot++
//
//            // Читаем содержимое файла для логирования
//            val fileContent = file.readText()
//            Log.i("FileInfo", "File contain: $fileContent")
//
//        } catch (e: IOException) {
//            Log.i("file_writing_error", "File writing failed: $e")
//        }
//    }
//


