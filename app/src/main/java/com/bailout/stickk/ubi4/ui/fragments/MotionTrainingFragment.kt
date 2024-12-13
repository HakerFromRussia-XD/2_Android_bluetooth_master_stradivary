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
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.math.roundToInt

class MotionTrainingFragment(
    private val onFinishTraining: () -> Unit
) : Fragment() {

    // View Binding
    private var _binding: Ubi4FragmentMotionTrainingBinding? = null
    private val binding get() = _binding!!

    private lateinit var path: File
    private lateinit var file: File
    private lateinit var writer: BufferedWriter

    // Timers
    private var timer: CountDownTimer? = null
    private var preparationTimer: CountDownTimer? = null
    private var timerDuration: Long = 0L
    private var preparationDuration: Long = 0L
    private var remainingTimerTime: Long = 0L
    private var remainingPreparationTime: Long = 0L

    // Timer Type
    private enum class TimerType { COUNTDOWN, PREPARATION, NONE }
    private var currentTimerType: TimerType = TimerType.NONE

    // Flags
    private var isTrainingPaused: Boolean = false
    private var isRecordingPaused: Boolean = false

    // Chronometers
    private lateinit var learningTimer: Chronometer
    private lateinit var learningStepTimer: Chronometer
    private var elapsedLearningTime: Long = 0L
    private var elapsedLearningStepTime: Long = 0L

    // File Logging
    private var loggingFilename = "serial_data"
    private val fileLock = Any()

    // RxJava
    private val disposables = CompositeDisposable()
    private val rxUpdateMainEvent = RxUpdateMainEventUbi4.getInstance()

    // Gesture Data
    private val gestures: MutableList<Map<String, String>> = mutableListOf()
    private var prot = 0
    private var startLineInLearningTable = 0
    private var dataCollection: Map<String, Any> = mapOf()
    private lateinit var gestureConfig: GestureConfig
    private lateinit var sprGestureItemsProvider: SprGestureItemsProvider

    // Training Phases
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LagSpr", "Motion onCreate")
        deleteSerialDataFile()

        path = requireContext().getExternalFilesDir(null)!!
        file = File(path, loggingFilename)

        if (!file.exists()) {
            file.createNewFile()
            file.writeText(
                "ts td omg0 omg1 omg2 omg3 omg4 omg5 omg6 omg7 omg8 omg9 omg10 omg11 omg12 omg13 omg14 omg15 " +
                        "emg0 emg1 emg2 emg3 emg4 emg5 emg6 emg7 bno0 bno1 bno2 prb0 prb1 prb2 prb3 prb4 prb5 " +
                        "prb6 prb7 argmax denoize prot state id now\n"
            )
        }
        writer = BufferedWriter(FileWriter(file, true))

        val mBLEParser = main?.let { BLEParser(it) }


        // Подписка на события оптического обучения
        val parameter = ParameterProvider.getParameter(6, 15)
        Log.d("TestOptic", "OpticTrainingStruct = ${parameter.parameterDataSize}")
        val opticStreamDisposable = rxUpdateMainEvent.uiOpticTrainingObservable
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe { dataCode ->
                if (isRecordingPaused) return@subscribe // Пропустить запись, если приостановлено

                Log.d("FileInfoWriteFile", "Received dataCode: $dataCode")
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
//                    Log.d("WriteFileDebugCheck", "OpticTrainingStruct = $dataString")
//                    Log.d(
//                        "WriteFileDebugCheck",
//                        "Number Frame = ${opticTrainingStruct.numberOfFrame}"
//                    )
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
        _binding = Ubi4FragmentMotionTrainingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("LagSpr", "Motion onViewCreated")
        super.onViewCreated(view, savedInstanceState)

        // Инициализация Chronometer
        learningTimer = Chronometer(requireContext())
        learningStepTimer = Chronometer(requireContext())

        // Инициализация поставщика жестов
        sprGestureItemsProvider = SprGestureItemsProvider(requireContext())

        // Обработка данных тренировки
        lineData = trainingDataProcessing()

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

        // Запуск первой фазы тренировки
        startPhase(currentPhaseIndex)

        // Обработка нажатия на кнопку остановки тренировки
        binding.stopTrainingBtn.setOnClickListener {
            showConfirmCancelTrainingDialog {
                // Подтверждение отмены обучения: переход на SprTrainingFragment
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, SprTrainingFragment())
                    .commit()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("LagSpr", "Motion onDestroyView")
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LagSpr", "Motion onDestroy")
        // Отмена всех таймеров и остановка Chronometer
        timer?.cancel()
        preparationTimer?.cancel()
        learningTimer.stop()
        learningStepTimer.stop()
        disposables.clear()
    }


    @SuppressLint("MissingInflatedId")
    private fun showConfirmCancelTrainingDialog(confirmClick: () -> Unit) {
        pauseTimers() // Приостановить таймеры и запись

        val dialogBinding =
            layoutInflater.inflate(R.layout.ubi4_dialog_cancel_training, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        val confirmBtn = dialogBinding.findViewById<View>(R.id.ubi4DialogConfirmCancelTrainingBtn)
        confirmBtn.setOnClickListener {
            stopTimers() // Убедимся, что все таймеры остановлены
            myDialog.dismiss()
            confirmClick()
        }
        val cancelBtn = dialogBinding.findViewById<View>(R.id.ubi4DialogCancelTrainingCancelBtn)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
            resumeTimers() // Возобновить таймеры и запись
        }
    }


    @SuppressLint("MissingInflatedId")
    private fun showConfirmCompletedTrainingDialog(confirmClick: () -> Unit) {
        pauseTimers()
        preparationTimer?.cancel()
        val dialogBinding =
            layoutInflater.inflate(R.layout.ubi4_dialog_confirm_finish_training, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()

        val confirmBtn = dialogBinding.findViewById<View>(R.id.ubi4CompletedTrainingBtn)
        confirmBtn.setOnClickListener {
            myDialog.dismiss()
            confirmClick()
            onFinishTraining()
        }
    }


    private fun pauseTimers() {
        // Отмена таймеров
        timer?.cancel()
        preparationTimer?.cancel()

        // Приостановка Chronometer и сохранение прошедшего времени
        elapsedLearningTime = SystemClock.elapsedRealtime() - learningTimer.base
        learningTimer.stop()
        elapsedLearningStepTime = SystemClock.elapsedRealtime() - learningStepTimer.base
        learningStepTimer.stop()

        // Установка флагов паузы
        isTrainingPaused = true
        isRecordingPaused = true


    }

    /**
     * Возобновляет все приостановленные таймеры и запись в файл.
     */
    private fun resumeTimers() {
        if (!isTrainingPaused) return

        isTrainingPaused = false
        isRecordingPaused = false

        // Возобновление Chronometer
        learningTimer.base = SystemClock.elapsedRealtime() - elapsedLearningTime
        learningTimer.start()
        learningStepTimer.base = SystemClock.elapsedRealtime() - elapsedLearningStepTime
        learningStepTimer.start()

        // Возобновление CountDownTimer
        when (currentTimerType) {
            TimerType.COUNTDOWN -> {
                if (remainingTimerTime > 0L) {
                    Log.d("Timers", "Resuming COUNTDOWN timer with remaining time: $remainingTimerTime ms")
                    timer = object : CountDownTimer(remainingTimerTime, 30L) {
                        override fun onTick(millisUntilFinished: Long) {
                            remainingTimerTime = millisUntilFinished
                            val secondsRemaining = (millisUntilFinished / 1000).toInt()
                            binding.countdownTextView.text = secondsRemaining.toString()
                            val progress = ((timerDuration - millisUntilFinished) / 30).toInt()
                            binding.motionProgressBar.progress = progress
                        }

                        override fun onFinish() {
                            remainingTimerTime = 0L
                            binding.countdownTextView.text = "0"
                            binding.countdownTextView.visibility = View.GONE
                            binding.motionProgressBar.progress = 0
                            // Переход к следующей фазе
                            startPhase(currentPhaseIndex + 1)
                            Log.d("DebugCheck", "Countdown finished for phase index: $currentPhaseIndex")
                        }
                    }.start()
                }
            }
            TimerType.PREPARATION -> {
                if (remainingPreparationTime > 0L) {
                    Log.d("Timers", "Resuming PREPARATION timer with remaining time: $remainingPreparationTime ms")
                    preparationTimer = object : CountDownTimer(remainingPreparationTime, 1000L) {
                        override fun onTick(millisUntilFinished: Long) {
                            remainingPreparationTime = millisUntilFinished
                            val secondsRemaining = (millisUntilFinished / 1000).toInt()
                            binding.countdownTextView.text = secondsRemaining.toString()
                        }

                        override fun onFinish() {
                            remainingPreparationTime = 0L
                            startPhase(currentPhaseIndex + 1)
                        }
                    }.start()
                }
            }
            TimerType.NONE -> {
                // Нет таймера для возобновления
            }
        }

        Log.d(
            "Timers",
            "Таймеры возобновлены. Remaining Timer Time: $remainingTimerTime, Remaining Preparation Time: $remainingPreparationTime"
        )
    }

    private fun stopTimers() {
        timer?.cancel()
        preparationTimer?.cancel()
        currentTimerType = TimerType.NONE
    }


    private fun startPhase(phaseIndex: Int) {
        currentPhaseIndex = phaseIndex
        if (phaseIndex >= lineData.size) {
            Log.d("DebugCheck", "All phases completed. Showing finish dialog.")
            // Все фазы завершены
            showConfirmCompletedTrainingDialog {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, SprTrainingFragment())
                    .commitNow()
            }
            return
        }

        val currentPhase = lineData[phaseIndex]
        Log.d(
            "DebugCheck",
            "Current phase: ${currentPhase.gestureName}, animationId: ${currentPhase.animation}"
        )

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
        currentTimerType = TimerType.COUNTDOWN

        timerDuration = (phase.timeGesture * 1000).toLong()
        remainingTimerTime = timerDuration
        val interval = 30L

        // Обновление названия текущего жеста
        binding.motionNameOfGesturesTv.text = phase.headerText
        binding.prepareForPerformTv.visibility = View.VISIBLE
        binding.prepareForPerformTv.text = phase.description
        binding.motionProgressBar.max = (timerDuration / interval).toInt()
        binding.motionProgressBar.progress = 0

        binding.countdownTextView.visibility = View.VISIBLE
        binding.motionProgressBar.visibility = View.VISIBLE

        timer = object : CountDownTimer(timerDuration, interval) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimerTime = millisUntilFinished
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                binding.countdownTextView.text = secondsRemaining.toString()
                val progress = ((timerDuration - millisUntilFinished) / interval).toInt()
                binding.motionProgressBar.progress = progress
            }

            override fun onFinish() {
                remainingTimerTime = 0L
                binding.countdownTextView.text = "0"
                binding.countdownTextView.visibility = View.GONE
                binding.motionProgressBar.progress = 0
                // Переход к следующей фазе
                startPhase(phaseIndex + 1)
                Log.d("DebugCheck", "Countdown finished for phase index: $currentPhaseIndex")
            }
        }.start()
    }


    private fun startPreparationCountDown(phase: GesturePhase, phaseIndex: Int) {
        stopTimers()
        currentTimerType = TimerType.PREPARATION

        preparationDuration = (phase.timeGesture * 1000).toLong()
        remainingPreparationTime = preparationDuration

        // Определение следующего жеста
        val nextGestureName = getNextGestureName(phaseIndex)
        Log.d("GestureName", "gestureName: $nextGestureName")

        if (nextGestureName.isEmpty()) {
            // Следующего жеста нет, это последний этап
            binding.motionNameOfGesturesTv.text = "Можете расслабить руку"
            binding.prepareForPerformTv.text = "Дождитесь окончания"
            binding.prepareForPerformTv.visibility = View.VISIBLE

            // Установка дефолтной анимации или скрытие ImageView
            switchAnimationSmoothly(R.raw.clock)
            binding.motionProgressBar.visibility = View.INVISIBLE
            binding.countdownTextView.visibility = View.VISIBLE
            binding.countdownTextView.text = (preparationDuration / 1000).toString()

            preparationTimer = object : CountDownTimer(preparationDuration, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingPreparationTime = millisUntilFinished
                    val secondsRemaining = (millisUntilFinished / 1000).toInt()
                    binding.countdownTextView.text = secondsRemaining.toString()
                }

                override fun onFinish() {
                    remainingPreparationTime = 0L
                    binding.prepareForPerformTv.visibility = View.GONE
                    binding.countdownTextView.visibility = View.GONE
                    binding.motionProgressBar.visibility = View.INVISIBLE
                    Log.d("DebugCheck", "Preparation countdown finished. Ending training.")
                    // Завершение тренировки
                    showConfirmCompletedTrainingDialog {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, SprTrainingFragment())
                            .commitNow()
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
            binding.countdownTextView.text = (preparationDuration / 1000).toString()

            preparationTimer = object : CountDownTimer(preparationDuration, 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingPreparationTime = millisUntilFinished
                    val secondsRemaining = (millisUntilFinished / 1000).toInt()
                    binding.countdownTextView.text = secondsRemaining.toString()
                }

                override fun onFinish() {
                    remainingPreparationTime = 0L
                    binding.prepareForPerformTv.visibility = View.GONE
                    binding.countdownTextView.visibility = View.GONE
                    binding.motionProgressBar.visibility = View.VISIBLE
                    Log.d("DebugCheck", "Preparation countdown finished for phase index: $currentPhaseIndex")
                    startPhase(phaseIndex + 1)
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
            if (nextPhase.gestureName != "BaseLine" &&
                nextPhase.gestureName != "Neutral" &&
                nextPhase.gestureName != "Finish"
            ) {
                val gestureName =
                    sprGestureItemsProvider.getNameGestureByKeyName(nextPhase.gestureName)
                return gestureName ?: ""
            }
            nextIndex++
        }

        return ""
    }

    private fun writeToFile(data: String, isAppend: Boolean = true) {
        if (isRecordingPaused) return

        synchronized(fileLock) {
            try {
                if (lineData.isEmpty()) {
                    Log.e("WriteFileDebugCheck", "lineData $lineData")
                    Log.e("WriteFileDebugCheck", "No phases available in lineData")
                    return
                }

                val currentPhase = lineData.getOrNull(currentPhaseIndex) ?: run {
                    return
                }
                val gestureId = currentPhase.gestureId
                val line = data.dropLast(2)

                val logLine = "$line $prot ${currentPhase.gestureName} $gestureId ${
                    ((currentPhase.timeGesture / 10).roundToInt() / 100.0)
                }"

                Log.d("WriteFileDebugCheck", "LOGLINE: $logLine")

                // Запись через BufferedWriter
                writer.write(logLine)
                writer.newLine()
                writer.flush()
               // val fileContent = file.readText()
                Log.i("FileInfoWriteFile", "File contains: $prot lines")

                Log.d("trainingDataProcessing1", "LOGLINE:${logLine.trim()}")

                prot++
                // Убрали чтение всего файла для ускорения

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


    private fun trainingDataProcessing(): MutableList<GesturePhase> {
        // Чтение и парсинг конфигурационного файла
        val json =
            requireContext().assets.open("config.json").bufferedReader().use { it.readText() }
        val gson = Gson()
        val config: ConfigOMGDataCollection =
            gson.fromJson(json, ConfigOMGDataCollection::class.java)

        val lineData = mutableListOf<GesturePhase>()
        nCycles = config.nCycles ?: 0
        gestureSequence = config.gestureSequence
        Log.d("GestureDebug", "gestureSequence $gestureSequence")
        gestureNumber = (gestureSequence as ArrayList<*>).size - 1
        preGestDuration = config.preGestDuration?.toDouble() ?: 0.0
        atGestDuration = config.atGestDuration?.toDouble() ?: 0.0
        postGestDuration = config.postGestDuration?.toDouble() ?: 0.0
        gestureDuration = (preGestDuration + atGestDuration + postGestDuration).toInt()
        gesturesId = config.gesturesId
        baselineDuration = config.baselineDuration?.toDouble() ?: 0.0

        val firstGestureName = gestureSequence.firstOrNull() ?: ""
        val firstGestureAnimation =
            sprGestureItemsProvider.getAnimationIdByKeyNameGesture(firstGestureName)

        // Добавление начальной фазы BaseLine
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

        // Добавление фаз тренировки по циклам и последовательности жестов
        repeat(nCycles) {
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
                    description = "Выполните жест: ${
                        sprGestureItemsProvider.getNameGestureByKeyName(keyName)
                    }",
                    gestureName = keyName,
                    gestureId = gestureId
                )
                lineData.add(currentGesture)
            }
        }

        // Добавление конечной фазы Finish
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
}
