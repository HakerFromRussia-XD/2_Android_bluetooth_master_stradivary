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
import com.bailout.stickk.ubi4.models.SprGestureItem
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.SprGestureItemsProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

class MotionTrainingFragment(
    val onFinishTraining: () -> Unit,
    ) : Fragment() {

    private var _bindig: Ubi4FragmentMotionTrainingBinding? = null
    private val binding get() = _bindig!!

    private val countDownTime = 1000L
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


    @SuppressLint("CheckResult", "DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LagSpr", "Motion onCreate")

        val mBLEParser = main?.let { BLEParser(it) }
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
                try {
                    val dataStringRaw = parameter.data ?: ""
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
        inflater: LayoutInflater, container: ViewGroup?,
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

        val gestureItemsProvider = SprGestureItemsProvider()
        sprGestureItemList = gestureItemsProvider.getSprGestureItemList(requireContext())
        binding.motionProgressBar.max = (countDownTime / interval).toInt()
        startPreparationCountDown()
        learningTimer = Chronometer(requireContext())
        learningStepTimer = Chronometer(requireContext())
        learningPreprocessingParse()
    }


    private fun updateGestures() {
        val currentGestures = sprGestureItemList[currentGestureIndex]
        binding.motionHandIv.setImageResource(currentGestures.image)
        binding.motionNameOfGesturesTv.text = currentGestures.title
    }

    private fun startPreparationCountDown() {
        binding.prepareForPerformTv.visibility = View.VISIBLE
        binding.motionProgressBar.visibility = View.INVISIBLE
        updateGestures()
        isCountingDown = false
        preparationTimer = object : CountDownTimer(pauseBeforeStart, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                binding.countdownTextView.text = secondsRemaining.toString()
                binding.countdownTextView.visibility = View.VISIBLE
            }

            override fun onFinish() {
                binding.prepareForPerformTv.visibility = View.INVISIBLE
                binding.motionProgressBar.visibility = View.VISIBLE
                isCountingDown = true
                startCountdown()
            }

        }.start()
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
                if (currentGestureIndex == 0) {
                    binding.motionProgressBar.progress = 0
                    binding.motionProgressBar.trackColor = Color.TRANSPARENT
                    showConfirmCompletedTrainingDialog {
                        parentFragmentManager.beginTransaction().replace(
                            R.id.fragmentContainer, SprTrainingFragment()
                        ).commitNow()
                    }
                } else {
                    startPreparationCountDown()
                }
            }
        }.start()
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
            startCountdown()
        } else {
            startPreparationCountDown()
        }
    }


    private fun writeToFile(data: String, isAppend: Boolean = true) {
        try {
            learningTimer.base = SystemClock.elapsedRealtime()
            learningTimer.start()
            val path = requireContext().getExternalFilesDir(null)
            val file = File(path, loggingFilename)
            val curData = trainingDataProcessing()
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
                file.appendText(
                    "$line $prot ${curData["state"]} ${curData["id"]} ${
                        ((curData["generalTime"]?.toDouble()?.div(10)?.roundToInt()
                            ?.div(100.0)) ?: curData["generalTime"]).toString()
                    }\n"
                )
                Log.d(
                    "trainingDataProcessing", "$line $prot ${curData["state"]} ${curData["id"]} ${
                        ((curData["generalTime"]?.toDouble()?.div(10)?.roundToInt()
                            ?.div(100.0)) ?: curData["generalTime"]).toString()
                    }"
                )
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
        dataCollection = Gson().fromJson(
            requireContext().assets.open("config.json").reader(),
            object : TypeToken<Map<String, Any>>() {}.type
        )
    }

    private fun trainingDataProcessing(): Map<String, String> {
        var lineData = mapOf(
            "n" to "None",
            "state" to "None",
            "id" to "None",
            "generalTime" to "None",
            "stepTime" to "None"
        )

        val generalTime = (SystemClock.elapsedRealtime() - learningTimer.base) / 1000
        val nCycles = dataCollection["N_CYCLES"].toString().toDouble().toInt()
        val gestureSequence = (dataCollection["GESTURE_SEQUENCE"] to ArrayList<String>()).first
        val gestureNumber = (gestureSequence as ArrayList<*>).size - 1
        val preGestDuration = dataCollection["PRE_GEST_DURATION"].toString().toDouble()
        val atGestDuration = dataCollection["AT_GEST_DURATION"].toString().toDouble()
        val postGestDuration = dataCollection["POST_GEST_DURATION"].toString().toDouble()
        val gestureDuration = preGestDuration + atGestDuration + postGestDuration
        val gesturesId = dataCollection["GESTURES_ID"]
        val baselineDuration = dataCollection["BASELINE_DURATION"].toString().toDouble()
        if (generalTime < baselineDuration)
            return mapOf(
                "n" to "0",
                "state" to "Baseline",
                "id" to "-1",
                "generalTime" to (generalTime * 1000).toInt().toString(),
                "stepTime" to preGestDuration.toString(),
            )
        val currentTime = generalTime - baselineDuration
        val currentLoop = (currentTime / (gestureNumber * gestureDuration)).toInt()
        val timeInLoop = currentTime % (gestureNumber * gestureDuration)
        val gestureInd = (timeInLoop / gestureDuration).toInt() + 1
        val timeInGesture = timeInLoop % gestureDuration
        val overallGestureNumber = currentLoop * gestureNumber + gestureInd
        if (currentLoop > nCycles) {
            return mapOf(
                "n" to overallGestureNumber.toString(),
                "state" to "Finish",
                "id" to "-1",
                "generalTime" to (generalTime * 1000).toInt().toString(),
                "stepTime" to postGestDuration.toString(),
            )
        }
        if (preGestDuration < timeInGesture && timeInGesture <= preGestDuration + atGestDuration)
            lineData = mapOf(
                "n" to overallGestureNumber.toString(),
                "state" to gestureSequence[gestureInd].toString(),
                "id" to (gesturesId as Map<*, *>)[gestureSequence[gestureInd].toString()].toString(),
                "generalTime" to (generalTime * 1000).toInt().toString(),
                "stepTime" to atGestDuration.toString(),
            )
        else
            lineData = mapOf(
                "n" to overallGestureNumber.toString(),
                "state" to "Neutral",
                "id" to "0",
                "generalTime" to generalTime.toString(),
                "stepTime" to preGestDuration.toString(),
            )

    return lineData}

    override fun onDestroy() {
    super.onDestroy()
    Log.d("LagSpr", "Motion onDestroy")
    _bindig = null
    timer?.cancel()
    preparationTimer?.cancel()
    disposables.clear()
}
}