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
import com.bailout.stickk.ubi4.models.SprGestureItem
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

class MotionTrainingFragment(
    val onFinishTraining: () -> Unit,
) : Fragment() {

    private var _bindig: Ubi4FragmentMotionTrainingBinding? = null
    private val binding get() = _bindig!!

    private val countDownTime = 0L
    private val interval = 30L
    private val pauseBeforeStart = 100L
    private lateinit var sprGestureItemList: ArrayList<SprGestureItem>
    var currentGestureIndex = 0
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

    private var generalTime: Long = 0
    private var nCycles: Int = 0
    private var gestureSequence: List<String> = emptyList()
    private var gestureNumber: Int = 0
    private var preGestDuration: Double = 0.0
    private var atGestDuration: Double = 0.0
    private var postGestDuration: Double = 0.0
    private var gestureDuration: Int = 0
    private var gesturesId: GesturesId? = null
    private var gestureIndex = -1
    private var baselineDuration: Double = 0.0

    private var currentGestureId: Int = 0


    ///////////////////////////////////////////////////////////////////////////////////////////////


//    private fun buildGesturePhases(
//        baselineDuration: Double,
//        preDuration: Double,
//        atDuration: Double,
//        postDuration: Double,
//        gestureSequence: List<String>,
////        gestureIdMap: Map<String, Int>
//    ): List<GesturePhase> {
//
//        if (gestureSequence.isEmpty()) return emptyList()
//
//        val phases = mutableListOf<GesturePhase>()
//
//        val baselinePhase = PhaseBaseline(timeGesture = baselineDuration)
//        phases.add(baselinePhase)
//
//        val firstGesture = gestureSequence.first()
////        val firstGestureId = gestureIdMap[firstGesture] ?: run {
////            return emptyList()
////        }
//
////        // Pre фаза для первого жеста (после Baseline)
////        phases.add(PhasePre(timeGesture = 5, gestureName = firstGesture, gestureId = firstGestureId))
////
////        // At фаза для первого жеста
////        phases.add(PhaseAt(timeGesture = atDuration.toInt(), gestureName = firstGesture, gestureId = firstGestureId))
//
//        for (i in 1 until gestureSequence.size) {
//            val gestureName = gestureSequence[i]
////            val gestureId = gestureIdMap[gestureName]?: run {
////                // Если нет id для жеста - показ Toast:
////                // Возвращаемся или пропускаем жест
////                return emptyList()
////            }
//
//            // Так как у нас после первого жеста идет Post предыдущего + Pre следующего = 4с
//            // Но мы можем разбить на две фазы:
//            // Сначала Post фаза предыдущего жеста (от gestureSequence[i-1])
//            val prevGestureName = gestureSequence[i - 1]
////            val prevGestureId = gestureIdMap[prevGestureName]?.toInt() ?: -1
//
////            phases.add(PhasePost(timeGesture = postDuration.toInt(), gestureName = prevGestureName, gestureId = prevGestureId))
//
////            // Затем Pre фаза для текущего жеста
////            phases.add(PhasePre(
////                timeGesture = preDuration.toInt(),
////                gestureName = gestureName,
////                gestureId = gestureId
////            ))
////
////            // И теперь At фаза для текущего жеста
////            phases.add(PhaseAt(
////                timeGesture = atDuration.toInt(),
////                gestureName = gestureName,
////                gestureId = gestureId
////            ))
//        }
//
//        return phases
//
//
//    }


////////////////////////////////////////////////////////////////////////////////////

    @SuppressLint("CheckResult", "DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LagSpr", "Motion onCreate")
        //loadGestureConfig()
        val result = trainingDataProcessing()
        result.forEach { Log.d("trainingDataProcessing", "trainingDataProcessing ${it}") }

        val mBLEParser = main.let { BLEParser(it) }
        //фейковые данные принимаемого потока
//        android.os.Handler().postDelayed({
//            mBLEParser?.parseReceivedData(BLECommands.testDataTransfer())
//        }, 1000)
//        Handler().postDelayed({
//            mBLEParser?.parseReceivedData(BLECommands.testDataTransfer())


        val parameter = ParameterProvider.getParameter(10, 5)
        Log.d("TestOptic", "OpticTrainingStruct = ${parameter.parameterDataSize}")
        val opticStreamDisposable = rxUpdateMainEvent.uiOpticTrainingObservable
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe { dataCode ->
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
                    Log.d("TestFileContain", "OpticTrainingStruct = $dataString")
                    Log.d("TestFileContain", "Number Frame = ${opticTrainingStruct.numberOfFrame}")

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


//        val gestureItemsProvider = SprGestureItemsProvider()
//        sprGestureItemList = gestureItemsProvider.getSprGestureItemList(requireContext())
//        startPreparationCountDown()

//        learningPreprocessingParse()
    }


//    private fun loadGestureConfig() {
//        dataCollection = Gson().fromJson(
//            requireContext().assets.open("config.json").reader(),
//            object : TypeToken<Map<String, Any>>() {}.type
//        )
//        Log.d("GestureUpdate", "dataCollection = $dataCollection ")
//
//
//
//        learningTimer = Chronometer(requireContext())
//        learningStepTimer = Chronometer(requireContext())
//        generalTime = (SystemClock.elapsedRealtime() - learningTimer.base) / 1000
//        nCycles = dataCollection["N_CYCLES"].toString().toDouble().toInt()
//        val gestureSequence =
//            (dataCollection["GESTURE_SEQUENCE"] to ArrayList<String>()).first as List<String>
//        gestureNumber = (gestureSequence as ArrayList<*>).size - 1
//        preGestDuration = dataCollection["PRE_GEST_DURATION"].toString().toDouble()
//        atGestDuration = dataCollection["AT_GEST_DURATION"].toString().toDouble()
//        postGestDuration = dataCollection["POST_GEST_DURATION"].toString().toDouble()
//        gestureDuration = (preGestDuration + atGestDuration + postGestDuration).toInt()
//        gesturesId = dataCollection["GESTURES_ID"]
//        baselineDuration = dataCollection["BASELINE_DURATION"].toString().toDouble()
//
//        // Извлекаем последовательность жестов из JSON (массив строк)
////        val gestureSequenceJson = jsonObject.getJSONArray("GESTURE_SEQUENCE")
////        val gestureSequence =
////            mutableListOf<String>() // Список для хранения последовательности жестов
////        for (i in 0 until gestureSequenceJson.length()) {
////            // Добавляем каждый жест из массива JSON в список
////            gestureSequence.add(gestureSequenceJson.getString(i))
////        }
//
//        // Извлекаем идентификаторы жестов из JSON (объект с парами "жест - ID")
////        val gesturesIdJson = jsonObject.getJSONObject("GESTURES_ID")
////        val gesturesId = mutableMapOf<String, Int>()//Словарь для хранения идентификаторов жестов
////        val keys = gesturesIdJson.keys()
////        while (keys.hasNext()) {
////            val key = keys.next()
////            gesturesId[key] = gesturesIdJson.getInt(key)
////        }
//
//        gestureConfig = GestureConfig(
//            baselineDuration = baselineDuration,
//            preGestDuration = preGestDuration,
//            atGestDuration = atGestDuration,
//            postGestDuration = postGestDuration,
//            gestureSequence = gestureSequence,
////            gesturesId = gesturesId
//        )
//        Log.d("GestureUpdate", "Gesture Config: $gestureConfig")
//
////        val phases = buildGesturePhases(
////            baselineDuration = baselineDuration,
////            preDuration = preGestDuration,
////            atDuration = atGestDuration,
////            postDuration = postGestDuration,
////            gestureSequence = gestureSequence,
//////            gestureIdMap = gesturesId,
////        )
//        val phases = trainingDataProcessing()
//        phases.forEach {
//            //   Log.d("GestureUpdate", "Phases: ${it.value}")
//        }
//
//
//    }


    // Метод для обновления UI с текущим жестом
    private fun updateGestures(gestureName: String) {
        try {
            val gestureImageResId = getGestureImageResource(gestureName)
            binding.motionHandIv.setImageResource(gestureImageResId)
            binding.motionNameOfGesturesTv.text = gestureName
            Log.d("GestureUpdate", "Gesture updated: $gestureName")
        } catch (e: Exception) {
            Log.e("GestureError", "Failed to update gesture: ${e.message}")
        }
    }

    private fun getGestureImageResource(gestureName: String): Int {
        return when (gestureName) {
            "ThumbFingers" -> R.drawable.grip_the_ball
            "Close" -> R.drawable.koza
            "Open" -> R.drawable.ok
            "Neutral" -> R.drawable.ok
            "Pinch" -> R.drawable.kulak
            "Indication" -> R.drawable.koza
            "Wrist_Flex" -> R.drawable.grip_the_ball
            "Wrist_Extend" -> R.drawable.kulak
            else -> R.drawable.grip_the_ball
        }
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

    private fun writeToFile(data: String, isAppend: Boolean = true) {
        Log.i("FileInfo", "WriteFile Start")

        try {
            learningTimer.base = SystemClock.elapsedRealtime()
            learningTimer.start()
            val path = requireContext().getExternalFilesDir(null)
            val file = File(path, loggingFilename)
            val curData = trainingDataProcessing()
            Log.d("GestureUpdate", "curData = $curData ")

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
//                file.appendText(
//                    "$line $prot ${curData["state"]} ${curData["id"]} ${
//                        ((curData["generalTime"]?.toDouble()?.div(10)?.roundToInt()
//                            ?.div(100.0)) ?: curData["generalTime"]).toString()
//                    }\n"
//                )
            } else
                file.writeText(line)
            prot++
            val fileContent = file.readText()
            Log.i("FileInfo", "File contain: $fileContent")

        } catch (e: IOException) {
            Log.i("file_writing_error", "File writing failed: $e")
        }
    }

    private fun learningPreprocessingParse() {
        var lines = requireContext().assets.open("data.emg8.protocol").bufferedReader().readLines()
        lines = lines.drop(1)
        for (line in lines) {
            gestures.add(
                mapOf(
                    "n" to line.split(",")[0],
                    "state" to line.split(",")[1],
                    "id" to line.split(",")[2],
                    "indicativeTime" to line.split(",")[4]
                )
            )
        }
//        dataCollection = Gson().fromJson(
//            requireContext().assets.open("config.json").reader(),
//            object : TypeToken<Map<String, Any>>() {}.type
//        )
    }

    private fun trainingDataProcessing(): MutableList<GesturePhase> {
        dataCollection = Gson().fromJson(
            requireContext().assets.open("config.json").reader(),
            object : TypeToken<Map<String, Any>>() {}.type
        )

        val json =
            requireContext().assets.open("config.json").bufferedReader().use { it.readText() }
        val gson = Gson()
        val config: ConfigOMGDataCollection = gson.fromJson(json, ConfigOMGDataCollection::class.java)

        var lineData = mutableListOf<GesturePhase>()

        nCycles = config.nCycles ?: 0
//        Log.d("trainingDataProcessing", "nCycles $nCycles")
        gestureSequence = config.gestureSequence
//        Log.d("trainingDataProcessing", "gestureSequence $gestureSequence")
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
        lineData.add(GesturePhase(
            prePhase = 0.0,
            timeGesture = baselineDuration,
            postPhase = 0.0,
            animation = 0,
            headerText = "Подготовьтесь к выполнению первого жеста",
            description = "Подготовьтесь к выполнению первого жеста",
            gestureName = "BaseLine",
            gestureId = -1
        ))

        gestureSequence.forEach {
//            Log.d("trainingDataProcessing", "getGestureValueByName ${gesturesId?.getGestureValueByName(it)}")
            //TODO дописать генерацию ресурса анимации по имени текущего обрабатываемого жеста
            var animation = 0
            if (it == "ThumbFingers") {animation = R.raw.loading_training_animation} // тут вместо рандомной ссылки дописать соответствующую
            lineData.add(GesturePhase(
                prePhase = preGestDuration,
                timeGesture = atGestDuration,
                postPhase = postGestDuration,
                animation = animation,
                headerText = "Подготовьтесь к выполнению первого жеста",
                description = "Подготовьтесь к выполнению первого жеста",
                gestureName = it,
                gestureId = gesturesId?.getGestureValueByName(it) ?: 0
            ))
        }

        lineData.add(GesturePhase(
            prePhase = 0.0,
            timeGesture = baselineDuration,
            postPhase = 0.0,
            animation = 0,
            headerText = "Подготовьтесь к выполнению первого жеста",
            description = "Подготовьтесь к выполнению первого жеста",
            gestureName = "Finish",
            gestureId = -1
        ))

//        if (generalTime < baselineDuration)
//            return mapOf(
//                "n" to "0",
//                "state" to "Baseline",
//                "id" to "-1",
//                "generalTime" to (generalTime * 1000).toInt().toString(),
//                "stepTime" to preGestDuration.toString(),
//            )
//        val currentTime = generalTime - baselineDuration
//        val currentLoop = (currentTime / (gestureNumber * gestureDuration)).toInt()
//        val timeInLoop = currentTime % (gestureNumber * gestureDuration)
//        val gestureInd = (timeInLoop / gestureDuration).toInt() + 1
//        val timeInGesture = timeInLoop % gestureDuration
//        val overallGestureNumber = currentLoop * gestureNumber + gestureInd
//        if (currentLoop > nCycles) {
//            return mapOf(
//                "n" to overallGestureNumber.toString(),
//                "state" to "Finish",
//                "id" to "-1",
//                "generalTime" to (generalTime * 1000).toInt().toString(),
//                "stepTime" to postGestDuration.toString(),
//            )
//        }
//        if (preGestDuration < timeInGesture && timeInGesture <= preGestDuration + atGestDuration)
//            lineData = mapOf(
//                "n" to overallGestureNumber.toString(),
//                "state" to gestureSequence[gestureInd].toString(),
//                "id" to (gesturesId as Map<*, *>)[gestureSequence[gestureInd].toString()].toString(),
//                "generalTime" to (generalTime * 1000).toInt().toString(),
//                "stepTime" to atGestDuration.toString(),
//            )
//        else
//            lineData = mapOf(
//                "n" to overallGestureNumber.toString(),
//                "state" to "Neutral",
//                "id" to "0",
//                "generalTime" to generalTime.toString(),
//                "stepTime" to preGestDuration.toString(),
//            )

        return lineData
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LagSpr", "Motion onDestroy")
        _bindig = null
        timer?.cancel()
        preparationTimer?.cancel()
        disposables.clear()
    }

    private fun startCountdown() {

        timer = object : CountDownTimer(countDownTime, interval) {
            override fun onTick(millisUntilFinished: Long) {

                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                binding.countdownTextView.text = secondsRemaining.toString()
                val progress = (millisUntilFinished / interval).toInt()
                binding.motionProgressBar.progress = progress

            }

            override fun onFinish() {
                binding.countdownTextView.text = "0"
                binding.countdownTextView.visibility = View.GONE
                // Переход к следующему жесту
                currentGestureIndex = (currentGestureIndex + 1) % sprGestureItemList.size
                //TODO вместо currentGestureIndex использоваться кол-во оставшихся фаз
                if (currentGestureIndex == 0) {
                    binding.motionProgressBar.progress = 0
                    binding.motionProgressBar.trackColor = Color.TRANSPARENT
                    showConfirmCompletedTrainingDialog {
                        parentFragmentManager.beginTransaction().replace(
                            R.id.fragmentContainer, SprTrainingFragment()
                        ).commitNow()
                    }
                } else {
                    //TODO запуск следующей фазы

                }
            }
        }.start()
    }
}

//    private fun startTrainingSequence() {
//        if (gestureIndex >= gestureConfig.gestureSequence.size) {
//            // Если это первый жест, добавим фазу BASELINE
//            if (gestureIndex == -1) {
//                startBaselinePhase()
//            } else {
//                startPerformGesturePhase()
//            }
//        } else {
//            // Завершение тренировки
//            showConfirmCompletedTrainingDialog {
//                parentFragmentManager.beginTransaction().replace(
//                    R.id.fragmentContainer, SprTrainingFragment()
//                ).commitNow()
//            }
//        }
//    }
//
//    private fun startBaselinePhase() {
//        Log.d("GestureUpdate", "startBaselinePhase START")
//
//        currentGestureId = 0
//
//
//        // Получаем имя текущего жеста
//        val gestureName = gestureConfig.gestureSequence[gestureIndex]
//        Log.d("GestureUpdate", "startBaselinePhase Name: $gestureName (Index: $currentGestureId)")
//
//        // Устанавливаем текст и видимость для подготовительного этапа
//        binding.prepareForPerformTv.text = "Подготовьтесь к выполнению жеста"
//        binding.prepareForPerformTv.visibility = View.VISIBLE
//        binding.motionProgressBar.visibility = View.INVISIBLE
//        binding.countdownTextView.visibility = View.VISIBLE
//
//        // Устанавливаем общее время для фазы BASELINE + PREPARE
//        val baselineAndPrepareDuration = baselineDuration + preGestDuration
//        binding.countdownTextView.text =
//            (baselineAndPrepareDuration / 1000).toString() // Отображаем общее время в секундах
//
//        // Устанавливаем ID для BASELINE
//
//        Log.d("GestureUpdate", "Current Gesture Index: $currentGestureIndex")
//
//        // Запускаем таймер для фазы BASELINE + PREPARE
//        timer = object : CountDownTimer(baselineAndPrepareDuration.toLong(), 1000) {
//            override fun onTick(millisUntilFinished: Long) {
//                // Обновляем текст обратного отсчета каждую секунду
//                val secondsRemaining = (millisUntilFinished / 1000).toInt()
//                binding.countdownTextView.text = secondsRemaining.toString()
//            }
//
//            override fun onFinish() {
//                // Переход к следующей фазе подготовки жеста
//                currentGestureId = 0
//                binding.countdownTextView.visibility = View.GONE
//                startPerformGesturePhase()
//            }
//        }.start()
//    }
//
//    private fun startPreparationPhase() {
//        val gestureName = gestureConfig.gestureSequence[gestureIndex]
//        updateGestures(gestureName)
//        binding.prepareForPerformTv.visibility = View.VISIBLE
//        binding.motionProgressBar.visibility = View.INVISIBLE
//        binding.countdownTextView.visibility = View.VISIBLE
//        binding.countdownTextView.text = (gestureConfig.preGestDuration / 1000).toString()
//        // Установите ID жеста на соответствующий из GESTURES_ID
////        currentGestureId = gestureConfig.gesturesId[gestureName] ?: 0
//
//        timer = object : CountDownTimer(preGestDuration.toLong(), 1000) {
//            override fun onTick(millisUntilFinished: Long) {
//                val secondsRemaining = (millisUntilFinished / 1000).toInt()
//                binding.countdownTextView.text = secondsRemaining.toString()
//            }
//
//            override fun onFinish() {
//                binding.countdownTextView.visibility = View.GONE
//                startPerformGesturePhase()
//            }
//        }.start()
//    }
//
//    private fun startPerformGesturePhase() {
//        val gestureName = gestureConfig.gestureSequence[currentGestureIndex]
//        updateGestures(gestureName)
//        binding.prepareForPerformTv.text = "Выполните жест"
//        binding.prepareForPerformTv.visibility = View.VISIBLE
//        binding.motionProgressBar.visibility = View.VISIBLE
//        binding.countdownTextView.visibility = View.VISIBLE
//        binding.countdownTextView.text = (atGestDuration / 1000).toString()
//        binding.motionProgressBar.max = (atGestDuration / 30).toInt()
//        isCountingDown = true
//
//        timer = object : CountDownTimer(atGestDuration.toLong(), 30) {
//            override fun onTick(millisUntilFinished: Long) {
//                val progress = (millisUntilFinished / 30).toInt()
//                binding.motionProgressBar.progress = progress
//            }
//
//            override fun onFinish() {
//                binding.countdownTextView.visibility = View.GONE
//                startNeutralPhase()
//            }
//        }.start()
//    }
//
//    private fun startNeutralPhase() {
//        val gestureName = "Neutral"
//        updateGestures(gestureName)
//        binding.prepareForPerformTv.text = "Подготовьтесь к выполнению жеста"
//        binding.prepareForPerformTv.visibility = View.VISIBLE
//        binding.motionProgressBar.visibility = View.INVISIBLE
//        binding.countdownTextView.visibility = View.VISIBLE
//        binding.countdownTextView.text = ((preGestDuration + postGestDuration) / 1000).toString()
//        currentGestureId = 0
//
//        timer = object : CountDownTimer((preGestDuration + postGestDuration).toLong(), 1000) {
//            override fun onTick(millisUntilFinished: Long) {
//                val secondsRemaining = (millisUntilFinished / 1000).toInt()
//                binding.countdownTextView.text = secondsRemaining.toString()
//            }
//
//            override fun onFinish() {
//                binding.countdownTextView.visibility = View.GONE
//                currentGestureIndex++
//                startTrainingSequence()
//            }
//        }.start()
//    }
//
//
//    private fun updateGestures() {
//        val currentGestures = sprGestureItemList[currentGestureIndex]
//        binding.motionHandIv.setImageResource(currentGestures.image)
//        binding.motionNameOfGesturesTv.text = currentGestures.title
//    }
//
//    private fun startPreparationCountDown() {
//        binding.prepareForPerformTv.visibility = View.VISIBLE
//        binding.motionProgressBar.visibility = View.INVISIBLE
//        updateGestures()
//        isCountingDown = false
//        preparationTimer = object : CountDownTimer(pauseBeforeStart, 1000) {
//            override fun onTick(millisUntilFinished: Long) {
//                val secondsRemaining = (millisUntilFinished / 1000).toInt()
//                binding.countdownTextView.text = secondsRemaining.toString()
//                binding.countdownTextView.visibility = View.VISIBLE
//            }
//
//            override fun onFinish() {
//                binding.prepareForPerformTv.visibility = View.INVISIBLE
//                binding.motionProgressBar.visibility = View.VISIBLE
//                isCountingDown = true
//                startCountdown()
//            }
//
//        }.start()
//    }
//

//    }