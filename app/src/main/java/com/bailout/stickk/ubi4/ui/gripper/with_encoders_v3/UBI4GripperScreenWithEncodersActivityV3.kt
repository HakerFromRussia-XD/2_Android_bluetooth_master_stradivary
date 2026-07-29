package com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3

import android.animation.ArgbEvaluator
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Dialog
import android.app.Service
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4LayoutGripperSettingsLeWithEncodersV3Binding
import com.bailout.stickk.new_electronic_by_Rodeon.compose.BaseActivity
import com.bailout.stickk.new_electronic_by_Rodeon.compose.qualifiers.RequirePresenter
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.presenters.GripperScreenPresenter
import com.bailout.stickk.new_electronic_by_Rodeon.viewTypes.GripperScreenActivityView
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL_CHARACTERISTIC
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.SERIALPORTCHAR_UUID
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.data.local.repository.AchievementEventManager
import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileManager
import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.ble.GestureV3
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.gestures.GestureWithAddress
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.DEVICE_ID_IN_SYSTEM_UBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GESTURE_ID_IN_SYSTEM_UBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.PARAMETER_ID_IN_SYSTEM_UBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.ui.gripper.v3model.Load3DModelFesth3
import com.bailout.stickk.ubi4.ui.gripper.v3model.V3ModelLoadMetrics
import com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3.UBI4GripperSettingsWithEncodersRendererV3
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_SETTING
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_LEFT_RIGHT_HAND
import com.bailout.stickk.ubi4.utility.EncodeByteToHex
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.properties.Delegates


@Suppress("DEPRECATION")
@RequirePresenter(GripperScreenPresenter::class)
class UBI4GripperScreenWithEncodersActivityV3
    : BaseActivity<GripperScreenPresenter, GripperScreenActivityView>(), GripperScreenActivityView{
    companion object {
        private const val FLOW_TAG = "V3GripperFlow"
        private const val FINGER_DELAY_UNIT_MS = 10L
        private const val FINGER_ANIMATION_MS_PER_PERCENT = 5L
        var angleFinger1 by Delegates.notNull<Int>()
        var angleFinger2 by Delegates.notNull<Int>()
        var angleFinger3 by Delegates.notNull<Int>()
        var angleFinger4 by Delegates.notNull<Int>()
        var angleFinger5 by Delegates.notNull<Int>()
        var angleFinger6 by Delegates.notNull<Int>()
        var animationInProgress1 by Delegates.notNull<Boolean>()
        var animationInProgress2 by Delegates.notNull<Boolean>()
        var animationInProgress3 by Delegates.notNull<Boolean>()
        var animationInProgress4 by Delegates.notNull<Boolean>()
        var animationInProgress5 by Delegates.notNull<Boolean>()
        var animationInProgress6 by Delegates.notNull<Boolean>()
        var side by Delegates.notNull<Int>()
    }

    private val json = Json { encodeDefaults = true }
    private var withEncodersRendererV3: UBI4GripperSettingsWithEncodersRendererV3? = null
    private var rendererFirstFrameReady = false
    private var pendingInitialTransition = false
    private var initialTransitionStarted = false
    private var fingerTransitionAnimator: ValueAnimator? = null
    private val fingerTransitionInterpolator = AccelerateDecelerateInterpolator()
    private var numberFinger = 0
    private var angleFinger = 0

    private var fingerOpenState1 = 0
    private var fingerOpenState2 = 0
    private var fingerOpenState3 = 0
    private var fingerOpenState4 = 0
    private var fingerOpenState5 = 0
    private var fingerOpenState6 = 0

    private var fingerCloseState1 = 0
    private var fingerCloseState2 = 0
    private var fingerCloseState3 = 0
    private var fingerCloseState4 = 0
    private var fingerCloseState5 = 0
    private var fingerCloseState6 = 0

    private var fingerOpenStateDelay1 = 0
    private var fingerOpenStateDelay2 = 0
    private var fingerOpenStateDelay3 = 0
    private var fingerOpenStateDelay4 = 0
    private var fingerOpenStateDelay5 = 0
    private var fingerOpenStateDelay6 = 0

    private var fingerCloseStateDelay1 = 0
    private var fingerCloseStateDelay2 = 0
    private var fingerCloseStateDelay3 = 0
    private var fingerCloseStateDelay4 = 0
    private var fingerCloseStateDelay5 = 0
    private var fingerCloseStateDelay6 = 0


    private val f56Handler = Handler(Looper.getMainLooper())
    private var f56PendingSend: Runnable? = null
    private val F56_TIMEOUT_MS = 12L


    private var timerCheckReceivingData: CountDownTimer? = null

    private var gestureState = 1
    private enum class States(val number: Int) {
        GESTURE_STATE_OPEN  (0),
        GESTURE_STATE_CLOSE (1),
        GESTURE_OPEN_DELAY(128),
        GESTURE_CLOSE_DELAY(129),
        GESTURE_SAVE_BUTTON(255)
    }

    private enum class FingerTargetState {
        OPEN,
        CLOSE
    }

    private var score1 = 0
    private var score2 = 0
    private var score3 = 0
    private var score4 = 0
    private var score5 = V3FingerPositionMapping.thumbFirstAxisPercent(0)
    private var score6 = V3FingerPositionMapping.thumbSecondAxisPercent(
        V3FingerPositionMapping.THUMB_SECOND_AXIS_INITIAL_DEGREES
    )

    private var firstRequest = true
    private var countTick = 0
    private var mSettings: SharedPreferences? = null
    private var gestureNumber: Int = 0
    private var driverVersionS: String? = null
    private var gestureNameList =  ArrayList<String>()
    private var editMode: Boolean = false
    private var mDeviceType: String? = null

    private var deviceAddress = 0
    private var parameterID = 0
    private var gestureID = 0
    private var initialFingerPositions: List<Int>? = null

    private lateinit var binding: Ubi4LayoutGripperSettingsLeWithEncodersV3Binding

    // --- Open/Close selector ---
    private var isOpenMode = true
    private var halfSelectorWidth = 0f
    private val animDuration = 200L

    @SuppressLint("CheckResult", "ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = Ubi4LayoutGripperSettingsLeWithEncodersV3Binding.inflate(layoutInflater)
        setContentView(binding.root)
        window.navigationBarColor = resources.getColor(R.color.ubi4_dark_back)
        window.statusBarColor = this.resources.getColor(R.color.ubi4_back, theme)
        mSettings = this.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE)
        gestureNumber = mSettings!!.getInt(PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM, 0)


        angleFinger1 = 0
        angleFinger2 = 0
        angleFinger3 = 0
        angleFinger4 = 0
        angleFinger5 = score5
        angleFinger6 = score6
        animationInProgress1 = false
        animationInProgress2 = false
        animationInProgress3 = false
        animationInProgress4 = false
        animationInProgress5 = false
        animationInProgress6 = false
        //control side in 3D
        side = resolveV3HandSide()

        deviceAddress = intent.getIntExtra(DEVICE_ID_IN_SYSTEM_UBI4, 0)
        parameterID = intent.getIntExtra(PARAMETER_ID_IN_SYSTEM_UBI4, 0)
        gestureID = intent.getIntExtra(GESTURE_ID_IN_SYSTEM_UBI4, 0)

        initBaseView(this)


        lifecycleScope.launchWhenStarted {
            BLEState.state.filter { it == BLEState.State.READY }
                .first()
            compileBLERead()
        }

        Log.d("gestureNameList" , "onCreate")
        loadGestureNameList()
        binding.gestureNameTv.text = gestureNameList[gestureNumber-1]
        gestureNameList.forEach {
            Log.d("gestureNameList" , "gestureNameList = $it   gestureNumber = ${gestureNumber-1}")
        }


        RxUpdateMainEventUbi4.getInstance().uiGestureSettingsV3Observable
            .compose(bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { parameterInfo ->
                val parameter = ParameterProvider.getParameterV3(parameterInfo)
                val gestureSettings = parseGestureInfoSafely(parameter.data)
                gestureSettings?.let {
                    Log.i(
                        FLOW_TAG,
                        "BLE RX gesture=${it.gestureId} " +
                            "open=${positionSummary(it.openPosition1, it.openPosition2, it.openPosition3, it.openPosition4, it.openPosition5, it.openPosition6)} " +
                            "close=${positionSummary(it.closePosition1, it.closePosition2, it.closePosition3, it.closePosition4, it.closePosition5, it.closePosition6)}"
                    )
                    loadGestureState(it)
                } ?: main.showToast(getString(SharedRes.strings.gesture_state_update_error.resourceId))
            }

        RxView.clicks(findViewById(R.id.editGestureNameBtn))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                val imm = this.getSystemService(Service.INPUT_METHOD_SERVICE) as InputMethodManager
                if (editMode) {
                    binding.editGestureNameBtn.setImageResource(R.drawable.ic_edit_24)
                    binding.gestureNameTv.visibility = View.VISIBLE
                    imm.hideSoftInputFromWindow(this.currentFocus?.windowToken, 0)
                    binding.gestureNameTv.text = binding.gestureNameEt.text
                    binding.gestureNameEt.visibility = View.GONE

                    gestureNameList[gestureNumber-1] = binding.gestureNameTv.text.toString()


                    val macKey = mSettings!!.getString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, "text")
                    System.err.println("6 LAST_CONNECTION_MAC: $macKey")
                    for (i in 0 until gestureNameList.size) {
                        mySaveText(PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM + macKey + i, gestureNameList[i])
                    }

                    editMode = false


                    UiState.updateFlow.tryEmit(0) // перерисовать виджеты/списки

                } else {
                    //переезжаем на binding
                    binding.editGestureNameBtn.setImageResource(R.drawable.ic_ok_24)
                    binding.gestureNameEt.visibility = View.VISIBLE
                    binding.gestureNameEt.setText(binding.gestureNameTv.text, TextView.BufferType.EDITABLE)
                    binding.gestureNameTv.visibility = View.GONE
                    binding.gestureNameEt.requestFocus()
                    imm.hideSoftInputFromWindow(binding.gestureNameEt.windowToken, 0)
                    imm.showSoftInput(binding.gestureNameEt, 0)
                    binding.gestureNameEt.isFocusableInTouchMode = true
                    editMode = true
                }
            }

        RxUpdateMainEventUbi4.getInstance().fingerAngleObservable
            .compose(bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { parameters ->
//                    System.err.println(
//                        "GripperSettingsRender--------> fingerAngleObservable  numberFinger = "
//                                + parameters.numberFinger + "     fingerAngel = " + parameters.fingerAngel)
                numberFinger = parameters.numberFinger
                angleFinger = parameters.fingerAngel
                if (numberFinger == 1) {
                    changeStateFinger1 (angleFinger)
                    compileBLEMassage ()
                }
                if (numberFinger == 2) {
                    changeStateFinger2 (angleFinger)
                    compileBLEMassage ()
                }
                if (numberFinger == 3) {
                    changeStateFinger3 (angleFinger)
                    compileBLEMassage ()
                }
                if (numberFinger == 4) {
                    changeStateFinger4 (angleFinger)
                    compileBLEMassage ()
                }
                if (numberFinger == 5) {
                    changeStateFinger5(angleFinger)
//                    compileBLEMassage ()
                    scheduleF56Send()
                }
                if (numberFinger == 6) {
                    changeStateFinger6(angleFinger)
//                    compileBLEMassage ()
                    sendF56Now()
                }

                if (numberFinger == 55) { }
            }

        RxView.clicks(findViewById(R.id.gripperSaveBtn))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                gestureState = States.GESTURE_SAVE_BUTTON.number
                val savedGesture = compileBLEMassage()
                recordCustomGripConfigurationIfChanged(savedGesture)
                if (editMode) {
                    gestureNameList[gestureNumber - 1] = binding.gestureNameEt.text.toString()
                    val macKey = mSettings!!.getString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, "text")
                    System.err.println("1 LAST_CONNECTION_MAC: $macKey")
                    for (i in 0 until gestureNameList.size) {
                        mySaveText(PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM + macKey + i, gestureNameList[i])
                    }
                    UiState.updateFlow.tryEmit(0)
                }
                finish()
            }

        binding.fingersDelayBtn.setOnClickListener {
            showFingersDelayDialog()
        }

        // initialize selector
        binding.gestureStateSelectorContainer.post { initSelector() }
    }

    private fun resolveV3HandSide(): Int {
        val handSideInfo = ParameterInfoRegistry.require(P_KEY_LEFT_RIGHT_HAND)
        val storedSide = (ParameterStoreV3.get(handSideInfo) as? ParameterTypedValueV3.Spinner)
            ?.value
            ?.spinnerValue
            ?.coerceIn(0, 1)
        if (storedSide != null) return storedSide

        return mSettings!!.getInt(
            mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "") + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE,
            1
        )
    }

    private fun parseGestureInfoSafely(data: String): GestureV3? {
        if (data.isBlank()) return null
        return runCatching { json.decodeFromString<GestureV3>(data) }
            .getOrNull()
    }
    override fun initializeUI() {
        V3ModelLoadMetrics.init(applicationContext)
        V3ModelLoadMetrics.log(
            "productionScreen initializeUI modelReady=${Load3DModelFesth3.isReady()}"
        )
        Load3DModelFesth3.preloadAsync(applicationContext)

        val activityManager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo
        val supportsEs2 = configurationInfo.reqGlEsVersion >= 0x00020000

        if (supportsEs2) {
            val eglClientVersion = if (configurationInfo.reqGlEsVersion >= 0x00030000) 3 else 2
            binding.glSurfaceViewLeWithEncodersV3.setV3EGLConfigChooser(eglClientVersion)
            binding.glSurfaceViewLeWithEncodersV3.holder.setFormat(PixelFormat.TRANSLUCENT)
            binding.glSurfaceViewLeWithEncodersV3.setBackgroundResource(R.color.ubi4_back)
            binding.glSurfaceViewLeWithEncodersV3.setZOrderOnTop(true)
            binding.glSurfaceViewLeWithEncodersV3.setPreserveEGLContextOnPause(true)
            binding.glSurfaceViewLeWithEncodersV3.setEGLContextClientVersion(eglClientVersion)

            val displayMetrics = DisplayMetrics()
            this.windowManager.defaultDisplay.getMetrics(displayMetrics)

            withEncodersRendererV3 = UBI4GripperSettingsWithEncodersRendererV3(this, binding.glSurfaceViewLeWithEncodersV3)
            withEncodersRendererV3?.setOnFirstFrameRenderedListener {
                rendererFirstFrameReady = true
                startInitialTransitionIfReady()
            }

            binding.glSurfaceViewLeWithEncodersV3.setRenderer(withEncodersRendererV3, displayMetrics.density)
            binding.glSurfaceViewLeWithEncodersV3.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }

    private fun changeStateFinger1(angleFinger: Int) {
        System.err.println("Изменили палец 1 $angleFinger")
        if (gestureState == States.GESTURE_STATE_OPEN.number) {
            fingerOpenState1 = angleFinger
        } else {
            fingerCloseState1 = angleFinger
        }
        score1 = angleFinger
    }
    private fun changeStateFinger2(angleFinger: Int) {
        System.err.println("Изменили палец 2 $angleFinger")
        if (gestureState == States.GESTURE_STATE_OPEN.number) {
            fingerOpenState2 = angleFinger
        } else {
            fingerCloseState2 = angleFinger
        }
        score2 = angleFinger
    }
    private fun changeStateFinger3(angleFinger: Int) {
        System.err.println("Изменили палец 3 $angleFinger")
        if (gestureState == States.GESTURE_STATE_OPEN.number) {
            fingerOpenState3 = angleFinger
        } else {
            fingerCloseState3 = angleFinger
        }
        score3 = angleFinger
    }
    private fun changeStateFinger4(angleFinger: Int) {
        System.err.println("Изменили палец4 $angleFinger")
        if (gestureState == States.GESTURE_STATE_OPEN.number) {
            fingerOpenState4 = angleFinger
        } else {
            fingerCloseState4 = angleFinger
        }
        score4 = angleFinger
    }
    private fun changeStateFinger5(angleFinger: Int) {
        val position = validationRange(angleFinger)
        if (gestureState == States.GESTURE_STATE_OPEN.number) {
            fingerOpenState5 = position
        } else {
            fingerCloseState5 = position
        }
        score5 = position
    }
    private fun changeStateFinger6(angleFinger: Int) {
        val position = validationRange(angleFinger)
        if (gestureState == States.GESTURE_STATE_OPEN.number) {
            fingerOpenState6 = position
        } else {
            fingerCloseState6 = position
        }
        score6 = position
    }

    private fun startFingerTransition(targetState: FingerTargetState, sendBleCommand: Boolean) {
        val opening = targetState == FingerTargetState.OPEN
        val startPositions = intArrayOf(score1, score2, score3, score4, score5, score6)
        val targetPositions = if (opening) {
            intArrayOf(
                fingerOpenState1,
                fingerOpenState2,
                fingerOpenState3,
                fingerOpenState4,
                fingerOpenState5,
                fingerOpenState6
            )
        } else {
            intArrayOf(
                fingerCloseState1,
                fingerCloseState2,
                fingerCloseState3,
                fingerCloseState4,
                fingerCloseState5,
                fingerCloseState6
            )
        }
        val animatedPositions = IntArray(startPositions.size)
        val startDelaysMs = transitionDelaysMs(opening)
        val movementDurationsMs = LongArray(startPositions.size) { index ->
            abs(targetPositions[index] - startPositions[index]) * FINGER_ANIMATION_MS_PER_PERCENT
        }
        var transitionDurationMs = 1L
        for (index in startPositions.indices) {
            transitionDurationMs = maxOf(
                transitionDurationMs,
                startDelaysMs[index] + movementDurationsMs[index]
            )
        }

        fingerTransitionAnimator?.cancel()
        setFingerAnimationInProgress(true)

        Log.i(
            FLOW_TAG,
            "animation transition=${if (opening) "TO_OPEN" else "TO_CLOSE"} " +
                "from=${currentPositionSummary()} " +
                "to=${if (opening) openPositionSummary() else closePositionSummary()} " +
                "delaysMs=${startDelaysMs.contentToString()} durationMs=$transitionDurationMs"
        )

        val selectorStateChanged = isOpenMode != opening
        isOpenMode = opening
        if (selectorStateChanged) {
            updateSelectorUI(opening)
        }
        if (sendBleCommand) {
            gestureState = if (opening) {
                States.GESTURE_OPEN_DELAY.number
            } else {
                States.GESTURE_CLOSE_DELAY.number
            }
            compileBLEMassage()
        }
        gestureState = if (opening) {
            States.GESTURE_STATE_OPEN.number
        } else {
            States.GESTURE_STATE_CLOSE.number
        }

        var cancelled = false
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = transitionDurationMs
            addUpdateListener { valueAnimator ->
                val playTimeMs = valueAnimator.currentPlayTime
                updateAnimatedFingerPositions(
                    startPositions,
                    targetPositions,
                    startDelaysMs,
                    movementDurationsMs,
                    playTimeMs,
                    animatedPositions
                )
                requestModelRender()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (cancelled || fingerTransitionAnimator !== animation) {
                        return
                    }
                    applyFingerPositions(targetPositions)
                    val renderer = withEncodersRendererV3
                    if (renderer != null) {
                        renderer.setOnNextFrameRenderedListener {
                            if (!isDestroyed && fingerTransitionAnimator == null) {
                                setFingerAnimationInProgress(false)
                            }
                        }
                    } else {
                        setFingerAnimationInProgress(false)
                    }
                    requestModelRender()
                    fingerTransitionAnimator = null
                }
            })
        }
        fingerTransitionAnimator = animator
        animator.start()
    }

    private fun transitionDelaysMs(opening: Boolean): LongArray {
        // Renderer order is little, ring, middle, index, thumb flexion, thumb rotation.
        val delays = if (opening) {
            intArrayOf(
                fingerCloseStateDelay4,
                fingerCloseStateDelay3,
                fingerCloseStateDelay2,
                fingerCloseStateDelay1,
                fingerCloseStateDelay5,
                fingerCloseStateDelay6
            )
        } else {
            intArrayOf(
                fingerOpenStateDelay4,
                fingerOpenStateDelay3,
                fingerOpenStateDelay2,
                fingerOpenStateDelay1,
                fingerOpenStateDelay5,
                fingerOpenStateDelay6
            )
        }
        return LongArray(delays.size) { index ->
            delays[index].coerceAtLeast(0) * FINGER_DELAY_UNIT_MS
        }
    }

    private fun updateAnimatedFingerPositions(
        startPositions: IntArray,
        targetPositions: IntArray,
        startDelaysMs: LongArray,
        movementDurationsMs: LongArray,
        playTimeMs: Long,
        positions: IntArray
    ) {
        for (index in startPositions.indices) {
            positions[index] = interpolatedFingerPosition(
                startPositions[index],
                targetPositions[index],
                startDelaysMs[index],
                movementDurationsMs[index],
                playTimeMs
            )
        }
        applyFingerPositions(positions)
    }

    private fun interpolatedFingerPosition(
        startPosition: Int,
        targetPosition: Int,
        startDelayMs: Long,
        movementDurationMs: Long,
        playTimeMs: Long
    ): Int {
        if (playTimeMs <= startDelayMs) {
            return startPosition
        }
        if (movementDurationMs == 0L) {
            return targetPosition
        }
        val linearFraction = ((playTimeMs - startDelayMs).toFloat() / movementDurationMs)
            .coerceIn(0f, 1f)
        val interpolatedFraction = fingerTransitionInterpolator.getInterpolation(linearFraction)
        return (startPosition + (targetPosition - startPosition) * interpolatedFraction).roundToInt()
    }

    private fun applyFingerPositions(positions: IntArray) {
        angleFinger1 = positions[0]
        angleFinger2 = positions[1]
        angleFinger3 = positions[2]
        angleFinger4 = positions[3]
        angleFinger5 = positions[4]
        angleFinger6 = positions[5]
        score1 = positions[0]
        score2 = positions[1]
        score3 = positions[2]
        score4 = positions[3]
        score5 = positions[4]
        score6 = positions[5]
    }

    private fun setFingerAnimationInProgress(inProgress: Boolean) {
        animationInProgress1 = inProgress
        animationInProgress2 = inProgress
        animationInProgress3 = inProgress
        animationInProgress4 = inProgress
        animationInProgress5 = inProgress
        animationInProgress6 = inProgress
    }

    @SuppressLint("InflateParams")
    private fun showFingersDelayDialog() {
        val dialogBinding = layoutInflater.inflate(R.layout.ubi4_dialog_fingers_delay, null)
        val myDialog = Dialog(this)
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()
//        myDialog.findViewById<LottieAnimationView>(R.id.delay_fingers_animation_view).setAnimation(R.raw.loader_calibrating)

        if (gestureState == States.GESTURE_STATE_CLOSE.number) {
            myDialog.findViewById<TextView>(R.id.ubi4DialogFingersDelayDescriptionTv).text = getString(R.string.delay_state_open_description)
            Log.d("uiGestureSettingsObservable", "fingerOpenStateDelay1 = $fingerOpenStateDelay1   fingerOpenStateDelay4 = $fingerOpenStateDelay4")
            myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay1Sb).progress = fingerOpenStateDelay1
            myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay2Sb).progress = fingerOpenStateDelay2
            myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay3Sb).progress = fingerOpenStateDelay3
            myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay4Sb).progress = fingerOpenStateDelay4
            myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay5Sb).progress = fingerOpenStateDelay5
            myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay6Sb).progress = fingerOpenStateDelay6
        } else {
            myDialog.findViewById<TextView>(R.id.ubi4DialogFingersDelayDescriptionTv).text = getString(R.string.delay_state_close_description)
            Log.d("uiGestureSettingsObservable", "fingerCloseStateDelay1 = $fingerCloseStateDelay1   fingerCloseStateDelay4 = $fingerCloseStateDelay4")
            myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay1Sb).progress = fingerCloseStateDelay1
            myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay2Sb).progress = fingerCloseStateDelay2
            myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay3Sb).progress = fingerCloseStateDelay3
            myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay4Sb).progress = fingerCloseStateDelay4
            myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay5Sb).progress = fingerCloseStateDelay5
            myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay6Sb).progress = fingerCloseStateDelay6
        }


        myDialog.findViewById<TextView>(R.id.dialogFingersDelayFirst2Tv).text = getString(R.string.delay_finger_ms, myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay1Sb).progress*10)
        myDialog.findViewById<TextView>(R.id.dialogFingersDelaySecond2Tv).text = getString(R.string.delay_finger_ms, myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay2Sb).progress*10)
        myDialog.findViewById<TextView>(R.id.dialogFingersDelayThird2Tv).text = getString(R.string.delay_finger_ms, myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay3Sb).progress*10)
        myDialog.findViewById<TextView>(R.id.dialogFingersDelayFourth2Tv).text = getString(R.string.delay_finger_ms, myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay4Sb).progress*10)
        myDialog.findViewById<TextView>(R.id.dialogFingersDelayFifth2Tv).text = getString(R.string.delay_finger_ms, myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay5Sb).progress*10)
        myDialog.findViewById<TextView>(R.id.dialogFingersDelaySixth2Tv).text = getString(R.string.delay_finger_ms, myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay6Sb).progress*10)


        myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay1Sb).setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                myDialog.findViewById<TextView>(R.id.dialogFingersDelayFirst2Tv).text = getString(R.string.delay_finger_ms, myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay1Sb).progress*10)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    if (gestureState == States.GESTURE_STATE_CLOSE.number) {
                        fingerOpenStateDelay1 = seekBar.progress
                    } else {
                        fingerCloseStateDelay1 = seekBar.progress
                    }
                    compileBLEMassage ()
                }
            }
        })
        myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay2Sb).setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                myDialog.findViewById<TextView>(R.id.dialogFingersDelaySecond2Tv).text = getString(R.string.delay_finger_ms, myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay2Sb).progress*10)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    if (gestureState == States.GESTURE_STATE_CLOSE.number) {
                        fingerOpenStateDelay2 = seekBar.progress
                    } else {
                        fingerCloseStateDelay2 = seekBar.progress
                    }
                    compileBLEMassage ()
                }
            }
        })
        myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay3Sb).setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                myDialog.findViewById<TextView>(R.id.dialogFingersDelayThird2Tv).text = getString(R.string.delay_finger_ms, myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay3Sb).progress*10)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    if (gestureState == States.GESTURE_STATE_CLOSE.number) {
                        fingerOpenStateDelay3 = seekBar.progress
                    } else {
                        fingerCloseStateDelay3 = seekBar.progress
                    }
                    compileBLEMassage ()
                }
            }
        })
        myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay4Sb).setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                myDialog.findViewById<TextView>(R.id.dialogFingersDelayFourth2Tv).text = getString(R.string.delay_finger_ms, myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay4Sb).progress*10)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    if (gestureState == States.GESTURE_STATE_CLOSE.number) {
                        fingerOpenStateDelay4 = seekBar.progress
                    } else {
                        fingerCloseStateDelay4 = seekBar.progress
                    }
                    compileBLEMassage ()
                }
            }
        })
        myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay5Sb).setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                myDialog.findViewById<TextView>(R.id.dialogFingersDelayFifth2Tv).text = getString(R.string.delay_finger_ms, myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay5Sb).progress*10)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    if (gestureState == States.GESTURE_STATE_CLOSE.number) {
                        fingerOpenStateDelay5 = seekBar.progress
                    } else {
                        fingerCloseStateDelay5 = seekBar.progress
                    }
                    compileBLEMassage ()
                }
            }
        })
        myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay6Sb).setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                myDialog.findViewById<TextView>(R.id.dialogFingersDelaySixth2Tv).text = getString(R.string.delay_finger_ms, myDialog.findViewById<SeekBar>(R.id.dialogFingersDelay6Sb).progress*10)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    if (gestureState == States.GESTURE_STATE_CLOSE.number) {
                        fingerOpenStateDelay6 = seekBar.progress
                    } else {
                        fingerCloseStateDelay6 = seekBar.progress
                    }
                    compileBLEMassage ()
                }
            }

        })


        val cancelBtn = dialogBinding.findViewById<View>(R.id.dialogFingersDelayCancel)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
        }
    }

    private fun compileBLEMassage(): Gesture {
        val gesture = Gesture(gestureID, // проверить тут -2
            validationRange(fingerOpenState4), validationRange(fingerOpenState3), validationRange(fingerOpenState2),
            validationRange(fingerOpenState1), validationRange(fingerOpenState5), validationRange(fingerOpenState6),
            validationRange(fingerCloseState4), validationRange(fingerCloseState3), validationRange(fingerCloseState2),
            validationRange(fingerCloseState1), validationRange(fingerCloseState5), validationRange(fingerCloseState6),
            fingerOpenStateDelay1, fingerOpenStateDelay2, fingerOpenStateDelay3, fingerOpenStateDelay4, fingerOpenStateDelay5, fingerOpenStateDelay6,
            fingerCloseStateDelay1, fingerCloseStateDelay2, fingerCloseStateDelay3, fingerCloseStateDelay4, fingerCloseStateDelay5, fingerCloseStateDelay6, gestureNameList[gestureNumber-1],0)
        val gestureStateModel = GestureWithAddress(deviceAddress, parameterID, gesture, gestureState)
        Log.d("uiGestureSettingsObservable", "gestureStateModel = $gestureStateModel")
        persistGestureSettings(gesture)
        val command = BLECommandsV3.sendGestureInfo(gestureStateModel)
        Log.i(
            FLOW_TAG,
            "BLE TX gesture=${gesture.gestureId} state=$gestureState " +
                "open=${positionSummary(gesture.openPosition1, gesture.openPosition2, gesture.openPosition3, gesture.openPosition4, gesture.openPosition5, gesture.openPosition6)} " +
                "close=${positionSummary(gesture.closePosition1, gesture.closePosition2, gesture.closePosition3, gesture.closePosition4, gesture.closePosition5, gesture.closePosition6)} " +
                "hex=${EncodeByteToHex.bytesToHexString(command)}"
        )
        main.bleCommandWithQueue(command, SERIALPORTCHAR_UUID, WRITE){}
        return gesture
    }

    private fun persistGestureSettings(gesture: Gesture) {
        val baseInfo = ParameterInfoRegistry.require(P_KEY_GESTURE_SETTING)
        val parameterInfo = ParameterInfo(
            baseInfo.parameterID,
            baseInfo.dataCode,
            baseInfo.deviceAddress,
            gesture.gestureId
        )
        val typedValue = ParameterTypedValueV3.GestureSettings(
            GestureV3(
                gestureId = gesture.gestureId,
                openPosition1 = gesture.openPosition1,
                openPosition2 = gesture.openPosition2,
                openPosition3 = gesture.openPosition3,
                openPosition4 = gesture.openPosition4,
                openPosition5 = gesture.openPosition5,
                openPosition6 = gesture.openPosition6,
                closePosition1 = gesture.closePosition1,
                closePosition2 = gesture.closePosition2,
                closePosition3 = gesture.closePosition3,
                closePosition4 = gesture.closePosition4,
                closePosition5 = gesture.closePosition5,
                closePosition6 = gesture.closePosition6,
                openToCloseTimeShift1 = gesture.openToCloseTimeShift1,
                openToCloseTimeShift2 = gesture.openToCloseTimeShift2,
                openToCloseTimeShift3 = gesture.openToCloseTimeShift3,
                openToCloseTimeShift4 = gesture.openToCloseTimeShift4,
                openToCloseTimeShift5 = gesture.openToCloseTimeShift5,
                openToCloseTimeShift6 = gesture.openToCloseTimeShift6,
                closeToOpenTimeShift1 = gesture.closeToOpenTimeShift1,
                closeToOpenTimeShift2 = gesture.closeToOpenTimeShift2,
                closeToOpenTimeShift3 = gesture.closeToOpenTimeShift3,
                closeToOpenTimeShift4 = gesture.closeToOpenTimeShift4,
                closeToOpenTimeShift5 = gesture.closeToOpenTimeShift5,
                closeToOpenTimeShift6 = gesture.closeToOpenTimeShift6
            )
        )
        ParameterStoreV3.put(parameterInfo, typedValue)
        SettingsProfileManager.saveBleValue(parameterInfo, typedValue)
    }
    private fun compileBLERead () {
        val command = BLECommandsV3.requestGestureInfo(gestureID)
        Log.i(
            FLOW_TAG,
            "BLE TX requestGesture gesture=$gestureID hex=${EncodeByteToHex.bytesToHexString(command)}"
        )
        main.bleCommandWithQueue(command, SERIALPORTCHAR_UUID, WRITE){}
    }
    private fun validationRange(inputNumber: Int) : Int {
        var _inputNumber = inputNumber
        if (_inputNumber > 100) { _inputNumber = 100}
        if (_inputNumber < 0) { _inputNumber = 0}
        return _inputNumber
    }

    private fun mySaveText(key: String, text: String) {
        val editor: SharedPreferences.Editor = mSettings!!.edit()
        editor.putString(key, text)
        editor.apply()
    }
    private fun loadGestureState(gestureSettings: GestureV3) {
        if (initialFingerPositions == null) {
            initialFingerPositions = gestureSettings.fingerPositions()
        }
        fingerOpenState1 = validationRange( gestureSettings.openPosition4 )
        fingerOpenState2 = validationRange( gestureSettings.openPosition3 )
        fingerOpenState3 = validationRange( gestureSettings.openPosition2 )
        fingerOpenState4 = validationRange( gestureSettings.openPosition1 )
        fingerOpenState5 = validationRange(gestureSettings.openPosition5)
        fingerOpenState6 = validationRange(gestureSettings.openPosition6)

        fingerCloseState1 = validationRange( gestureSettings.closePosition4 )
        fingerCloseState2 = validationRange( gestureSettings.closePosition3 )
        fingerCloseState3 = validationRange( gestureSettings.closePosition2 )
        fingerCloseState4 = validationRange( gestureSettings.closePosition1 )
        fingerCloseState5 = validationRange(gestureSettings.closePosition5)
        fingerCloseState6 = validationRange(gestureSettings.closePosition6)

        fingerOpenStateDelay1 = gestureSettings.openToCloseTimeShift1
        fingerOpenStateDelay2 = gestureSettings.openToCloseTimeShift2
        fingerOpenStateDelay3 = gestureSettings.openToCloseTimeShift3
        fingerOpenStateDelay4 = gestureSettings.openToCloseTimeShift4
        fingerOpenStateDelay5 = gestureSettings.openToCloseTimeShift5
        fingerOpenStateDelay6 = gestureSettings.openToCloseTimeShift6

        fingerCloseStateDelay1 = gestureSettings.closeToOpenTimeShift1
        fingerCloseStateDelay2 = gestureSettings.closeToOpenTimeShift2
        fingerCloseStateDelay3 = gestureSettings.closeToOpenTimeShift3
        fingerCloseStateDelay4 = gestureSettings.closeToOpenTimeShift4
        fingerCloseStateDelay5 = gestureSettings.closeToOpenTimeShift5
        fingerCloseStateDelay6 = gestureSettings.closeToOpenTimeShift6

        Log.i(
            FLOW_TAG,
            "animation targets " +
                "open=${openPositionSummary()} close=${closePositionSummary()} " +
                "current=${currentPositionSummary()}"
        )
        if (!initialTransitionStarted) {
            pendingInitialTransition = true
            startInitialTransitionIfReady()
        }
    }

    private fun recordCustomGripConfigurationIfChanged(savedGesture: Gesture) {
        val initialPositions = initialFingerPositions ?: return
        if (savedGesture.fingerPositions() == initialPositions) return

        AchievementEventManager.recordCustomGripConfiguration(savedGesture.gestureId)
    }

    private fun GestureV3.fingerPositions(): List<Int> = listOf(
        openPosition1,
        openPosition2,
        openPosition3,
        openPosition4,
        openPosition5,
        openPosition6,
        closePosition1,
        closePosition2,
        closePosition3,
        closePosition4,
        closePosition5,
        closePosition6
    ).map { it.coerceIn(0, 100) }

    private fun Gesture.fingerPositions(): List<Int> = listOf(
        openPosition1,
        openPosition2,
        openPosition3,
        openPosition4,
        openPosition5,
        openPosition6,
        closePosition1,
        closePosition2,
        closePosition3,
        closePosition4,
        closePosition5,
        closePosition6
    )

    private fun startInitialTransitionIfReady() {
        if (!rendererFirstFrameReady || !pendingInitialTransition || initialTransitionStarted) {
            return
        }
        initialTransitionStarted = true
        pendingInitialTransition = false
        binding.glSurfaceViewLeWithEncodersV3.postOnAnimation {
            if (isFinishing || isDestroyed) {
                return@postOnAnimation
            }
            startFingerTransition(FingerTargetState.OPEN, sendBleCommand = true)
        }
    }

    private fun requestModelRender() {
        if (withEncodersRendererV3 != null) {
            binding.glSurfaceViewLeWithEncodersV3.requestRender()
        }
    }
    private fun loadGestureNameList() {
        val text = "load not work"
        val macKey = mSettings!!.getString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, text)
        gestureNameList.clear()
        for (i in 0 until PreferenceKeysUbi4.NUM_GESTURES) {
            gestureNameList.add(
                mSettings!!.getString((PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM + macKey + i), text).toString()
            )
        }
    }
    private fun initSelector() {
    halfSelectorWidth = binding.gestureStateSelectorContainer.width / 2f
    updateSelectorUI(isOpenMode)
    binding.gestureOpenBtn.setOnClickListener {
        if (!isOpenMode) {
            startFingerTransition(FingerTargetState.OPEN, sendBleCommand = true)
        }
    }
    binding.gestureCloseBtn.setOnClickListener {
        if (isOpenMode) {
            startFingerTransition(FingerTargetState.CLOSE, sendBleCommand = true)
        }
    }
}

    private fun currentPositionSummary(): String = positionSummary(
        score4,
        score3,
        score2,
        score1,
        score5,
        score6
    )

    private fun openPositionSummary(): String = positionSummary(
        fingerOpenState4,
        fingerOpenState3,
        fingerOpenState2,
        fingerOpenState1,
        fingerOpenState5,
        fingerOpenState6
    )

    private fun closePositionSummary(): String = positionSummary(
        fingerCloseState4,
        fingerCloseState3,
        fingerCloseState2,
        fingerCloseState1,
        fingerCloseState5,
        fingerCloseState6
    )

    private fun positionSummary(
        index: Int,
        middle: Int,
        ring: Int,
        little: Int,
        thumbDeltaY: Int,
        thumbDeltaX: Int
    ): String = "[index=$index,middle=$middle,ring=$ring,little=$little," +
        "thumbDeltaY=$thumbDeltaY,thumbDeltaX=$thumbDeltaX]"

    private fun updateSelectorUI(isOpen: Boolean) {
        ObjectAnimator.ofFloat(
            binding.selectorIndicator,
            "translationX",
            if (isOpen) 0f else halfSelectorWidth
        ).setDuration(animDuration).start()

        ObjectAnimator.ofInt(
            binding.gestureOpenBtn,
            "textColor",
            if (isOpen) resources.getColor(android.R.color.darker_gray) else resources.getColor(R.color.white),
            if (isOpen) resources.getColor(R.color.white) else resources.getColor(android.R.color.darker_gray)
        ).apply { duration = animDuration; setEvaluator(ArgbEvaluator()); start() }

        ObjectAnimator.ofInt(
            binding.gestureCloseBtn,
            "textColor",
            if (!isOpen) resources.getColor(android.R.color.darker_gray) else resources.getColor(R.color.white),
            if (!isOpen) resources.getColor(R.color.white) else resources.getColor(android.R.color.darker_gray)
        ).apply { duration = animDuration; setEvaluator(ArgbEvaluator()); start() }
    }

    private fun scheduleF56Send() {
        // перезапускаем таймер на отправку
        f56PendingSend?.let { f56Handler.removeCallbacks(it) }
        f56PendingSend = Runnable {
            compileBLEMassage()
            f56PendingSend = null
        }
        f56Handler.postDelayed(f56PendingSend!!, F56_TIMEOUT_MS)
    }

    private fun sendF56Now() {
        // если висел таймер — отменяем
        f56PendingSend?.let { f56Handler.removeCallbacks(it); f56PendingSend = null }
        compileBLEMassage()
    }

    override fun onResume() {
        super.onResume()
        if (withEncodersRendererV3 != null) {
            binding.glSurfaceViewLeWithEncodersV3.onResume()
            requestModelRender()
        }
    }

    override fun onPause() {
        if (withEncodersRendererV3 != null) {
            binding.glSurfaceViewLeWithEncodersV3.onPause()
        }
        super.onPause()
    }

    override fun onDestroy() {
        fingerTransitionAnimator?.cancel()
        fingerTransitionAnimator = null
        setFingerAnimationInProgress(false)
        f56Handler.removeCallbacksAndMessages(null)
        f56PendingSend = null
        withEncodersRendererV3?.setOnFirstFrameRenderedListener(null)
        withEncodersRendererV3?.setOnNextFrameRenderedListener(null)
        super.onDestroy()
    }

}
