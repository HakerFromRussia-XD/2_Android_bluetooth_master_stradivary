package com.bailout.stickk.ubi4.ui.fragments

import android.animation.ValueAnimator
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
import android.widget.TextView
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
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.data.local.SprGestureItemsProvider
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.sync.withLock
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
    private var indicationTimer: CountDownTimer? = null
    private var preparationTimer: CountDownTimer? = null
    private var dialogWarningTimer: CountDownTimer? = null
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
    private var stamp = 0
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
    private var currentDialog: Dialog? = null
    private var counterTimer: Double = 0.0



//    private var indicatorOpticStreamIv: ImageView? = null

    private var lineData: MutableList<GesturePhase> = mutableListOf()
    private var currentPhaseIndex: Int = 0

    private var totalRealGesturesCount: Int = 0
    private var currentRealGestureIndex: Int = 0

    // Сет реальных жестов (если нужно, можете дополнять или изменять):
    private val pseudoGestures = setOf("Neutral", "BaseLine", "Finish")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LagSpr", "Motion onCreate")
        deleteSerialDataFile()
        stamp = ((System.currentTimeMillis() / 1000) % Int.MAX_VALUE).toInt()
        loggingFilename += stamp
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



        // Подписка на события оптического обучения
        val opticStreamDisposable = rxUpdateMainEvent.uiOpticTrainingObservable
            .subscribeOn(Schedulers.io())
            .doOnNext { parameterRef ->
                try {
                    val parameter = ParameterProvider.getParameter(parameterRef.addressDevice, parameterRef.parameterID)
                    Log.d("TestOptic", "OpticTrainingStruct = ${parameter.data.length}")
                    val dataStringRaw = parameter.data
                    if (dataStringRaw.isBlank() || dataStringRaw == "None") {
                        Log.e("TestFileContain", "Data is empty or invalid")
                        return@doOnNext
                    }
                    val opticTrainingStruct =
                        Json.decodeFromString<OpticTrainingStruct>("\"${parameter.data}\"")
                    val dataString = opticTrainingStruct.data.joinToString(separator = " ") {
                        String.format("%.1f", it)
                    }
                    Log.d("TestFileContain", "OpticTrainingStruct = $dataString")
                    Log.d("TestFileContain", "Number Frame = ${opticTrainingStruct.numberOfFrame}")

                    writeToFile(dataString)
                } catch (e: Exception) {
                    Log.e("TestOptic", "Error decoding data: ${e.message}")
                }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                onDataPacketReceived()
            }
        disposables.add(opticStreamDisposable)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("LagSpr", "Motion onCreateView")
        onDataPacketReceived()
        _binding = Ubi4FragmentMotionTrainingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility", "SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("LagSpr", "Motion onViewCreated")
        super.onViewCreated(view, savedInstanceState)

//        indicatorOpticStreamIv = _binding?.indicatorOpticStreamIv
        // Инициализация Chronometer
        learningTimer = Chronometer(requireContext())
        learningStepTimer = Chronometer(requireContext())

        // Инициализация поставщика жестов
        sprGestureItemsProvider = SprGestureItemsProvider(requireContext())

        // Обработка данных тренировки
        lineData = trainingDataProcessing()

        binding.motionRemainingGesturesTv.text =
            getString(R.string.remaining_gestures_count, totalRealGesturesCount)

//         Проверяем и объединяем BaseLine и Neutral
//        if (lineData.size > 1 &&
//            lineData[0].gestureName == "BaseLine" &&
//            lineData[1].gestureName == "Neutral"
//        ) {
//            val combinedPhase = lineData[0].copy(
//                timeGesture = lineData[0].timeGesture + lineData[1].timeGesture
//            )
//            lineData[0] = combinedPhase
//            lineData.removeAt(1)

            switchAnimationSmoothly(lineData[0].animation, 0)
//        }

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
        (activity as? MainActivityUBI4)?.getBottomNavigationController()?.setNavigationEnabled(false)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("LagSpr", "Motion onDestroyView")
        _binding = null
        (activity as? MainActivityUBI4)?.getBottomNavigationController()?.setNavigationEnabled(true)

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
        if (currentDialog != null && currentDialog?.isShowing == true) {
            return
        }
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
            Log.d("StateCallBack", "confirmClick() run")
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


//    private fun startPhase(phaseIndex: Int) {
//        currentPhaseIndex = phaseIndex
//        if (phaseIndex >= lineData.size) {
//            Log.d("DebugCheck", "All phases completed. Showing finish dialog.")
//            // Все фазы завершены
//            showConfirmCompletedTrainingDialog {
//                parentFragmentManager.beginTransaction()
//                    .replace(R.id.fragmentContainer, SprTrainingFragment())
//                    .commitNow()
//            }
//            return
//        }

    private fun startPhase(phaseIndex: Int) {
        // Гарантируем, что анимация видима и сброшены alpha, scale и progress для новой фазы
        binding.motionHandIv.visibility = View.VISIBLE
        binding.motionHandIv.alpha = 1f
        binding.motionHandIv.scaleX = 1f
        binding.motionHandIv.scaleY = 1f
        binding.motionHandIv.progress = 0f

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
            switchAnimationSmoothly(currentPhase.animation, 50)
        }

        if (!pseudoGestures.contains(currentPhase.gestureName)) {
            currentRealGestureIndex++
            val remainingCount = totalRealGesturesCount - currentRealGestureIndex
            // Отобразим в TextView (добавьте в верстку TextView с id=motionRemainingGesturesTv)
            binding.motionRemainingGesturesTv.text =
                getString(R.string.remaining_gestures_count, remainingCount)
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

                // Modified: для активных фаз при завершении ставим анимацию на 50% и запускаем плавное уменьшение (scale down)
                if (!pseudoGestures.contains(phase.gestureName)) {
                    binding.motionHandIv.pauseAnimation()
                    binding.motionHandIv.progress = 0.5f  // замораживаем на 50%
                    binding.motionHandIv.animate().cancel() // отменяем предыдущие анимации
                    binding.motionHandIv.animate()
                        .scaleX(0f)
                        .scaleY(0f)
                        .setDuration(350)
                        .withEndAction {
                            // Сброс масштабов для следующей анимации
                            binding.motionHandIv.scaleX = 1f
                            binding.motionHandIv.scaleY = 1f
                            startPhase(phaseIndex + 1)
                        }
                        .start()
                } else {
                    startPhase(phaseIndex + 1)
                }
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


        // Если нет следующего "настоящего" жеста, значит либо впереди "Finish", либо вообще конец списка
        if (nextGestureName.isEmpty()) {
            Log.d("nextGestureName", "isEmpty() gestureName: $nextGestureName")
            // Показываем подсказку для пользователя: "Можно расслабить руку", и ждём
            binding.motionNameOfGesturesTv.text = getString(R.string.you_can_relax_your_hand)
            binding.prepareForPerformTv.text = getString(R.string.wait_until_the_end)
            binding.prepareForPerformTv.visibility = View.VISIBLE

            // Допустим, какая-то базовая анимация
            switchAnimationSmoothly(R.raw.open, 1)
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

                    // Вместо того чтобы звать диалог напрямую,
                    // даём «Finish»-фазе закончиться штатно и идём дальше:
                    startPhase(phaseIndex + 1)
                }
            }.start()

        } else {
            Log.d("nextGestureName", "not isEmpty() gestureName: $nextGestureName")
            // Если есть "настоящий" жест — обычная логика подготовки к следующему жесту
            if (phase.gestureName == "Neutral") {
                switchAnimationSmoothly(phase.animation, 50)
            }
            binding.motionNameOfGesturesTv.text = getString(R.string.next_gesture, nextGestureName)
            binding.prepareForPerformTv.text = getString(R.string.prepare_to_perform_the_gesture)
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


    private fun switchAnimationSmoothly(newAnimation: Int, endPercent: Int) {
        binding.motionHandIv.animate()
            .setDuration(0)
            .scaleX(0f)
            .scaleY(0f)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                binding.motionHandIv.removeAllUpdateListeners()

                binding.motionHandIv.setAnimation(newAnimation)

                if (endPercent == 0) {
                    binding.motionHandIv.progress = 0f
                    binding.motionHandIv.pauseAnimation()
                } else {
                    // Для остальных вызываем play и добавляем слушатель
                    binding.motionHandIv.playAnimation()
                    binding.motionHandIv.addAnimatorUpdateListener { animation ->
                        if (animation.animatedFraction >= endPercent / 100f) {
                            binding.motionHandIv.pauseAnimation()
                        }
                    }
                }

                // Анимация «всплытия» (scale back)
                binding.motionHandIv.animate()
                    .setDuration(350)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
            .start()
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
                var line = data.dropLast(2)
                data.forEach {
                    Log.d("TestOptic", "data: $it")
                }
                // Заменяем все вхождения "0,00" на "0.00"
                line = line.replace("-0,0", "0.0")
                    .replace("0,0", "0.0")

                counterTimer += currentPhase.timeGesture
                val logLine = "$line $prot ${currentPhase.gestureName} $gestureId ${
                    ((counterTimer/ 10).roundToInt() / 100.0)
                }"
                val finalLogLine = logLine.replace(',', '.')

                Log.d("WriteFileDebugCheck", "LOGLINE: $logLine")

                // Запись через BufferedWriter
                writer.write(finalLogLine)
                writer.newLine()
                writer.flush()
                Log.i("FileInfoWriteFile", "File contains: $prot lines")

                Log.d("trainingDataProcessing1", "LOGLINE:${logLine.trim()}")

                prot++
                // Убрали чтение всего файла для ускорения

            } catch (e: IOException) {
                Log.i("WriteFileDebugCheck", "File writing failed: $e")
            }
        }

    }



    override fun onPause() {
        super.onPause()
        pauseTimers()
    }
    override fun onResume() {
        super.onResume()
        resumeTimers()
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

        if (gestureSequence.isNotEmpty()) (gestureSequence as MutableList<String>).removeAt(0)

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


        lineData.add(
            GesturePhase(
                prePhase = 0.0,
                timeGesture = baselineDuration,
                postPhase = 0.0,
                animation = sprGestureItemsProvider.getAnimationIdByKeyNameGesture(""),
                headerText = requireContext().getString(R.string.prepare_to_perform_the_gesture),
                description = requireContext().getString(R.string.prepare_to_perform_the_gesture),
                gestureName = "BaseLine",
                gestureId = -1
            )
        )

//        lineData.add(
//            GesturePhase(
//                prePhase = 0.0,
//                timeGesture = 4.0,
//                postPhase = 0.0,
//                animation = firstGestureAnimation,
//                headerText = requireContext().getString(R.string.prepare_to_perform_the_gesture),
//                description = requireContext().getString(R.string.prepare_to_perform_the_gesture),
//                gestureName = "Neutral",
//                gestureId = 0
//            )
//        )

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


                if (index < gestureSequence.size) {//&& index > 0
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
                        headerText = requireContext().getString(R.string.prepare_to_perform_the_gesture),
                        description = requireContext().getString(R.string.rest_before_the_next_gesture),
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
                    description = requireContext().getString(
                        R.string.perform_gesture,
                        sprGestureItemsProvider.getNameGestureByKeyName(keyName)
                    ),
                    gestureName = keyName,
                    gestureId = gestureId
                )
                lineData.add(currentGesture)
            }
        }
//        lineData.add(
//            GesturePhase(
//                prePhase = 0.0,
//                //90
//                timeGesture = 60.0,
//                postPhase = 0.0,
//                animation = sprGestureItemsProvider.getAnimationIdByKeyNameGesture(""),
//                headerText = requireContext().getString(R.string.rest_before_the_next_gesture),
//                description = requireContext().getString(R.string.rest_before_the_next_gesture),
//                gestureName = "Neutral",
//                gestureId = 0
//            )
//        )

        // Добавление конечной фазы Finish
        lineData.add(
            GesturePhase(
                prePhase = 0.0,
                timeGesture = baselineDuration,
                postPhase = 0.0,
                animation = sprGestureItemsProvider.getAnimationIdByKeyNameGesture(""),
                headerText = requireContext().getString(R.string.rest_before_the_next_gesture),
                description = requireContext().getString(R.string.rest_before_the_next_gesture),
                gestureName = "Finish",
                gestureId = -1
            )
        )

        totalRealGesturesCount = lineData.count { !pseudoGestures.contains(it.gestureName) }

        Log.d("DebugCheck", "Training phases generated: ${lineData.size}")
        lineData.forEachIndexed { idx, phase ->
            Log.d("DebugCheck", "Phase $idx: ${phase.gestureName}")
        }

        return lineData
    }

    private fun showWarningDialog() {
        if (!isAdded) return
        val dialogFileBinding =
            layoutInflater.inflate(R.layout.ubi4_dialog_warning_load_checkpoint, null)
        val myDialog = Dialog(requireContext())
        myDialog.setContentView(dialogFileBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()
        val titleTextView = dialogFileBinding.findViewById<TextView>(R.id.ubi4DialogWarningTitleTv)
        titleTextView.text = "Обучение прерванно"
        val subTitleTextView = dialogFileBinding.findViewById<TextView>(R.id.ubi4DialogWarningMessageTv)
        subTitleTextView.text = "Потеря соединения с оптическими датчиками. Повторите обучение"

        val confirmBtn = dialogFileBinding.findViewById<View>(R.id.ubi4WarningLoadingTrainingBtn)
        confirmBtn.setOnClickListener {
            stopTimers()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, SprTrainingFragment())
                .commit()
            myDialog.dismiss()

        }

    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private fun onDataPacketReceived() {
        // Сбрасываем предыдущий таймер
        Log.d("onDataPacketReceived", "onDataPacketReceived run")

        _binding?.indicatorOpticStreamIv?.setImageDrawable(main.resources.getDrawable(R.drawable.circle_16_green))
        indicationTimer?.cancel()
        dialogWarningTimer?.cancel()
        // Запускаем новый таймер на 100 мс
        indicationTimer = object : CountDownTimer(100, 100) {
            override fun onTick(millisUntilFinished: Long) = Unit

            override fun onFinish() {
                _binding?.indicatorOpticStreamIv?.setImageDrawable(main.resources.getDrawable(R.drawable.circle_16_red))
            }
        }.start()
        dialogWarningTimer = object : CountDownTimer(2000, 2000) {
            override fun onTick(millisUntilFinished: Long) = Unit

            override fun onFinish() {
                _binding?.indicatorOpticStreamIv?.setImageDrawable(main.resources.getDrawable(R.drawable.circle_16_red))
                showWarningDialog()
                pauseTimers()
            }
        }.start()
    }
}
