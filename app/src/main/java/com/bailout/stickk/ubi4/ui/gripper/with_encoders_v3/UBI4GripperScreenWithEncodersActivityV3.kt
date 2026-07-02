package com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3

import android.animation.ArgbEvaluator
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
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
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
import com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3.UBI4GripperSettingsWithEncodersRendererV3
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_SETTING
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.properties.Delegates


@Suppress("DEPRECATION")
@RequirePresenter(GripperScreenPresenter::class)
class UBI4GripperScreenWithEncodersActivityV3
    : BaseActivity<GripperScreenPresenter, GripperScreenActivityView>(), GripperScreenActivityView{
    companion object {
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

    private var score1 = 0
    private var score2 = 0
    private var score3 = 0
    private var score4 = 0
    private var score5 = 0
    private var score6 = 0

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
        initBaseView(this)
        window.navigationBarColor = resources.getColor(R.color.ubi4_dark_back)
        window.statusBarColor = this.resources.getColor(R.color.ubi4_back, theme)
        mSettings = this.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE)
        gestureNumber = mSettings!!.getInt(PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM, 0)


        angleFinger1 = 0
        angleFinger2 = 0
        angleFinger3 = 0
        angleFinger4 = 0
        angleFinger5 = 0
        angleFinger6 = 0
        animationInProgress1 = false
        animationInProgress2 = false
        animationInProgress3 = false
        animationInProgress4 = false
        animationInProgress5 = false
        animationInProgress6 = false
        //control side in 3D
        side = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1)

        deviceAddress = intent.getIntExtra(DEVICE_ID_IN_SYSTEM_UBI4, 0)
        parameterID = intent.getIntExtra(PARAMETER_ID_IN_SYSTEM_UBI4, 0)
        gestureID = intent.getIntExtra(GESTURE_ID_IN_SYSTEM_UBI4, 0)


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
                gestureSettings?.let { loadGestureState(it)
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
                    changeStateFinger5 (88 - angleFinger)
//                    compileBLEMassage ()
                    scheduleF56Send()
                }
                if (numberFinger == 6) {
                    changeStateFinger6 (98 - angleFinger)
//                    compileBLEMassage ()
                    sendF56Now()
                }

                if (numberFinger == 55) { }
            }

        RxView.clicks(findViewById(R.id.gripperSaveBtn))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                gestureState = States.GESTURE_SAVE_BUTTON.number
                compileBLEMassage()
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
    private fun parseGestureInfoSafely(data: String): GestureV3? {
        if (data.isBlank()) return null
        return runCatching { json.decodeFromString<GestureV3>(data) }
            .getOrNull()
    }
    override fun initializeUI() {
        val activityManager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo
        val supportsEs2 = configurationInfo.reqGlEsVersion >= 0x00020000

        if (supportsEs2) {
            val eglClientVersion = if (configurationInfo.reqGlEsVersion >= 0x00030000) 3 else 2
            binding.glSurfaceViewLeWithEncodersV3.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            binding.glSurfaceViewLeWithEncodersV3.holder.setFormat(PixelFormat.TRANSLUCENT)
            binding.glSurfaceViewLeWithEncodersV3.setBackgroundResource(R.color.ubi4_back)
            binding.glSurfaceViewLeWithEncodersV3.setZOrderOnTop(true)


            binding.glSurfaceViewLeWithEncodersV3.setEGLContextClientVersion(eglClientVersion)

            val displayMetrics = DisplayMetrics()
            this.windowManager.defaultDisplay.getMetrics(displayMetrics)

            withEncodersRendererV3 = UBI4GripperSettingsWithEncodersRendererV3(this, binding.glSurfaceViewLeWithEncodersV3)

            binding.glSurfaceViewLeWithEncodersV3.setRenderer(withEncodersRendererV3, displayMetrics.density)
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
        if (gestureState == States.GESTURE_STATE_OPEN.number) {
            System.err.println("Изменили палец 5 gestureState = 1")
            fingerOpenState5 = (angleFinger.toFloat()/100*91).toInt()-49
        } else {
            System.err.println("Изменили палец 5 gestureState = 0")
            fingerCloseState5 = (angleFinger.toFloat() / 100 * 91).toInt() - 49
        }
        score5 = (angleFinger.toFloat()/100*91).toInt()-49
    }
    private fun changeStateFinger6(angleFinger: Int) {
        if (gestureState == States.GESTURE_STATE_OPEN.number) {
            fingerOpenState6 = (angleFinger.toFloat() / 100 * 90).toInt()
        } else {
            fingerCloseState6 = (angleFinger.toFloat() / 100 * 90).toInt()
        }
        score6 = (angleFinger.toFloat()/100*90).toInt()
    }

    private fun animateFinger1 () {
        if (gestureState == States.GESTURE_STATE_OPEN.number) {
            val anim1 = ValueAnimator.ofInt(score1, fingerCloseState1)
            anim1.duration = (abs(fingerCloseState1 - score1) * 10).toLong()
            animationInProgress1 = true
            anim1.addUpdateListener {
                angleFinger1 = anim1.animatedValue as Int
                score1 = anim1.animatedValue as Int
                if (score1 == fingerCloseState1) {
                    animationInProgress1 = false
                }
            }
            anim1.start()
        } else
        {
            val anim1 = ValueAnimator.ofInt(score1, fingerOpenState1)
            anim1.duration = (abs(fingerOpenState1 - score1) * 10).toLong()
            animationInProgress1 = true
            anim1.addUpdateListener {
                angleFinger1 = anim1.animatedValue as Int
                score1 = anim1.animatedValue as Int
                if (score1 == fingerOpenState1) {
                    animationInProgress1 = false
                }
            }
            anim1.start()
        }
    }
    private fun animateFinger2 () {
        if (gestureState == States.GESTURE_STATE_OPEN.number) {
            val anim2 = ValueAnimator.ofInt(score2, fingerCloseState2)
            anim2.duration = (abs(fingerCloseState2 - score2) * 10).toLong()
            animationInProgress2 = true
            anim2.addUpdateListener {
                angleFinger2 = anim2.animatedValue as Int
                score2 = anim2.animatedValue as Int
                if (score2 == fingerCloseState2) {
                    animationInProgress2 = false
                }
            }
            anim2.start()
        } else
        {
            val anim2 = ValueAnimator.ofInt(score2, fingerOpenState2)
            anim2.duration = (abs(fingerOpenState2 - score2) * 10).toLong()
            animationInProgress2 = true
            anim2.addUpdateListener {
                angleFinger2 = anim2.animatedValue as Int
                score2 = anim2.animatedValue as Int
                if (score2 == fingerOpenState2) {
                    animationInProgress2 = false
                }
            }
            anim2.start()
        }
    }
    private fun animateFinger3 () {
        if (gestureState == States.GESTURE_STATE_OPEN.number) {
            val anim3 = ValueAnimator.ofInt(score3, fingerCloseState3)
            anim3.duration = (abs(fingerCloseState3 - score3) * 10).toLong()
            animationInProgress3 = true
            anim3.addUpdateListener {
                angleFinger3 = anim3.animatedValue as Int
                score3 = anim3.animatedValue as Int
                if (score3 == fingerCloseState3) {
                    animationInProgress3 = false
                }
            }
            anim3.start()
        } else
        {
            val anim3 = ValueAnimator.ofInt(score3, fingerOpenState3)
            anim3.duration = (abs(fingerOpenState3 - score3) * 10).toLong()
            animationInProgress3 = true
            anim3.addUpdateListener {
                angleFinger3 = anim3.animatedValue as Int
                score3 = anim3.animatedValue as Int
                if (score3 == fingerOpenState3) {
                    animationInProgress3 = false
                }
            }
            anim3.start()
        }
    }
    private fun animateFinger4 () {
        if (gestureState == States.GESTURE_STATE_OPEN.number) {
            val anim4 = ValueAnimator.ofInt(score4, fingerCloseState4)
            anim4.duration = (abs(fingerCloseState4 - score4) * 10).toLong()
            animationInProgress4 = true
            anim4.addUpdateListener {
                angleFinger4 = anim4.animatedValue as Int
                score4 = anim4.animatedValue as Int
                if (score4 == fingerCloseState4) {
                    animationInProgress4 = false
                }
            }
            anim4.start()
        } else
        {
            val anim4 = ValueAnimator.ofInt(score4, fingerOpenState4)
            anim4.duration = (abs(fingerOpenState4 - score4) * 10).toLong()
            animationInProgress4 = true
            anim4.addUpdateListener {
                angleFinger4 = anim4.animatedValue as Int
                score4 = anim4.animatedValue as Int
                if (score4 == fingerOpenState4) {
                    animationInProgress4 = false
                }
            }
            anim4.start()
        }
    }
    private fun animateFinger5 () {
        if (gestureState == States.GESTURE_STATE_OPEN.number) {
            val anim5 = ValueAnimator.ofInt(score5, fingerCloseState5)
            anim5.duration = (abs(fingerCloseState5 - score5) * 10).toLong()
            animationInProgress5 = true
            anim5.addUpdateListener {
                angleFinger5 = anim5.animatedValue as Int
                score5 = anim5.animatedValue as Int
                if (score5 == fingerCloseState5) {
                    animationInProgress5 = false
                }
            }
            anim5.start()
        } else
        {
            val anim5 = ValueAnimator.ofInt(score5, fingerOpenState5)
            anim5.duration = (abs(fingerOpenState5 - score5) * 10).toLong()
            animationInProgress5 = true
            anim5.addUpdateListener {
                angleFinger5 = anim5.animatedValue as Int
                score5 = anim5.animatedValue as Int
                if (score5 == fingerOpenState5) {
                    animationInProgress5 = false
                }
            }
            anim5.start()
        }
    }
    private fun animateFinger6 () {
        if (gestureState == States.GESTURE_STATE_OPEN.number) {
            val anim6 = ValueAnimator.ofInt(score6, fingerCloseState6)
            anim6.duration = (abs(fingerCloseState6 - score6) * 10).toLong()
            animationInProgress6 = true
            anim6.addUpdateListener {
                angleFinger6 = anim6.animatedValue as Int
                score6 = anim6.animatedValue as Int
                if (score6 == fingerCloseState6) {
                    animationInProgress6 = false
                }
            }
            anim6.start()
        } else
        {
            val anim6 = ValueAnimator.ofInt(score6, fingerOpenState6)
            anim6.duration = (abs(fingerOpenState6 - score6) * 10).toLong()
            animationInProgress6 = true
            anim6.addUpdateListener {
                angleFinger6 = anim6.animatedValue as Int
                score6 = anim6.animatedValue as Int
                if (score6 == fingerOpenState6) {
                    animationInProgress6 = false
                }
            }
            anim6.start()
        }
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

    private fun compileBLEMassage () {
        val gesture = Gesture(gestureID, // проверить тут -2
            validationRange(fingerOpenState4), validationRange(fingerOpenState3), validationRange(fingerOpenState2),
            validationRange(fingerOpenState1), validationRange(inverseRangConversion(fingerOpenState5, 85, -53)), validationRange(inverseRangConversion(fingerOpenState6, 85, 15)),
            validationRange(fingerCloseState4), validationRange(fingerCloseState3), validationRange(fingerCloseState2),
            validationRange(fingerCloseState1), validationRange(inverseRangConversion(fingerCloseState5, 85, -53)), validationRange(inverseRangConversion(fingerCloseState6, 85, 15)),
            fingerOpenStateDelay1, fingerOpenStateDelay2, fingerOpenStateDelay3, fingerOpenStateDelay4, fingerOpenStateDelay5, fingerOpenStateDelay6,
            fingerCloseStateDelay1, fingerCloseStateDelay2, fingerCloseStateDelay3, fingerCloseStateDelay4, fingerCloseStateDelay5, fingerCloseStateDelay6, gestureNameList[gestureNumber-1],0)
        val gestureStateModel = GestureWithAddress(deviceAddress, parameterID, gesture, gestureState)
        Log.d("uiGestureSettingsObservable", "gestureStateModel = $gestureStateModel")
        persistGestureSettings(gesture)
        main.bleCommandWithQueue(BLECommandsV3.sendGestureInfo(gestureStateModel), SERIALPORTCHAR_UUID, WRITE){}
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
        Log.d("uiGestureSettingsObservable", "compileBLERead gesture id = $gestureID")
        main.bleCommandWithQueue(BLECommandsV3.requestGestureInfo(gestureID), SERIALPORTCHAR_UUID, WRITE){}
    }
    private fun inverseRangConversion(inputNumber: Int, range: Int, offset: Int) : Int {
//        val _inputNumber = validationRange(inputNumber)
        var result = inputNumber.toFloat() / range.toFloat() * 100
        result = range - result
        result += offset
        return result.toInt()
    }
    private fun rangConversion(inputNumber: Int, range: Int, offset: Int) : Int {
        val _inputNumber = validationRange(inputNumber)
        var result = _inputNumber.toFloat() / 100 * range.toFloat()
        result = range - result
        result += offset
        return result.toInt()
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
        fingerOpenState1 = validationRange( gestureSettings.openPosition4 )
        fingerOpenState2 = validationRange( gestureSettings.openPosition3 )
        fingerOpenState3 = validationRange( gestureSettings.openPosition2 )
        fingerOpenState4 = validationRange( gestureSettings.openPosition1 )
        fingerOpenState5 = rangConversion( gestureSettings.openPosition5, 90, -59)
        fingerOpenState6 = rangConversion( gestureSettings.openPosition6, 92, -1)

        fingerCloseState1 = validationRange( gestureSettings.closePosition4 )
        fingerCloseState2 = validationRange( gestureSettings.closePosition3 )
        fingerCloseState3 = validationRange( gestureSettings.closePosition2 )
        fingerCloseState4 = validationRange( gestureSettings.closePosition1 )
        fingerCloseState5 = rangConversion( gestureSettings.closePosition5, 90, -59)
        fingerCloseState6 = rangConversion( gestureSettings.closePosition6, 92, -1)

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

        f56Handler.postDelayed({
            animateFinger1 ()
            animateFinger2 ()
            animateFinger3 ()
            animateFinger4 ()
            animateFinger5 ()
            animateFinger6 ()
            gestureState = States.GESTURE_STATE_OPEN.number
        }, 200)
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
            // animate closing → opening
            animateFinger1(); animateFinger2(); animateFinger3()
            animateFinger4(); animateFinger5(); animateFinger6()
            // update selector UI
            isOpenMode = true
            updateSelectorUI(true)
            // send OPEN command
            gestureState = States.GESTURE_OPEN_DELAY.number
            compileBLEMassage()
            gestureState = States.GESTURE_STATE_OPEN.number
        }
    }
    binding.gestureCloseBtn.setOnClickListener {
        if (isOpenMode) {
            // animate opening → closing
            animateFinger1(); animateFinger2(); animateFinger3()
            animateFinger4(); animateFinger5(); animateFinger6()
            // update selector UI
            isOpenMode = false
            updateSelectorUI(false)
            // send CLOSE command
            gestureState = States.GESTURE_CLOSE_DELAY.number
            compileBLEMassage()
            gestureState = States.GESTURE_STATE_CLOSE.number


        }
    }
}

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

    override fun onDestroy() {
        f56Handler.removeCallbacksAndMessages(null)
        f56PendingSend = null
        super.onDestroy()
    }

}
