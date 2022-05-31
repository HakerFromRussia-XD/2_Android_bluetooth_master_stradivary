package me.start.motorica.new_electronic_by_Rodeon.ui.activities.gripper.with_encoders

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
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.dialog_fingers_delay.*
import kotlinx.android.synthetic.main.layout_gripper_settings_le_with_encoders.*
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager
import me.start.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes
import me.start.motorica.new_electronic_by_Rodeon.compose.BaseActivity
import me.start.motorica.new_electronic_by_Rodeon.compose.qualifiers.RequirePresenter
import me.start.motorica.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import me.start.motorica.new_electronic_by_Rodeon.models.GestureStateWithEncoders
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.presenters.GripperScreenPresenter
import me.start.motorica.new_electronic_by_Rodeon.viewTypes.GripperScreenActivityView
import kotlin.math.abs
import kotlin.properties.Delegates


@Suppress("DEPRECATION")
@RequirePresenter(GripperScreenPresenter::class)
class GripperScreenWithEncodersActivity
    : BaseActivity<GripperScreenPresenter, GripperScreenActivityView>(), GripperScreenActivityView{
    private var withEncodersRenderer: GripperSettingsWithEncodersRenderer? = null
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
        var setTimeDelayOfFingers by Delegates.notNull<Boolean>()
    }

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

    private var timerCheckReceivingData: CountDownTimer? = null

    private var gestureState = 0

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
    private var gestureNameList =  ArrayList<String>()
    private var editMode: Boolean = false
    private var mDeviceType: String? = null

    @SuppressLint("CheckResult", "ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_gripper_settings_le_with_encoders)
        initBaseView(this)
        window.navigationBarColor = resources.getColor(R.color.colorPrimaryDark)
        window.statusBarColor = this.resources.getColor(R.color.blueStatusBar, theme)
        mSettings = this.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        gestureNumber = mSettings!!.getInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 0)
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

        setTimeDelayOfFingers = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                                + PreferenceKeys.SET_FINGERS_DELAY, 0) == 1
        if (setTimeDelayOfFingers) {
            fingers_delay_btn?.visibility = View.VISIBLE
        } else {
            fingers_delay_btn?.visibility = View.GONE
        }


        loadOldState()
        myLoadGesturesList()
        gesture_name_tv.text = gestureNameList[gestureNumber - 1]


        RxView.clicks(findViewById(R.id.edit_gesture_name_btn))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val imm = this.getSystemService(Service.INPUT_METHOD_SERVICE) as InputMethodManager
                    if (editMode) {
                        edit_gesture_name_btn.setImageResource(R.drawable.ic_edit_24)
                        gesture_name_tv.visibility = View.VISIBLE
                        imm.hideSoftInputFromWindow(this.currentFocus?.windowToken, 0)
                        gesture_name_tv.text = gesture_name_et.text
                        gesture_name_et.visibility = View.GONE
                        gestureNameList[(gestureNumber - 1)] = gesture_name_tv.text.toString()
                        for (i in 0 until gestureNameList.size) {
                            mySaveText(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + i, gestureNameList[i])
                        }
                        editMode = false
                    } else {
                        edit_gesture_name_btn.setImageResource(R.drawable.ic_ok_24)
                        gesture_name_et.visibility = View.VISIBLE
//                        gesture_name_et.background.setColorFilter(resources.getColor(R.color.darkOrange), PorterDuff.Mode.SRC_ATOP)
                        gesture_name_et.setText(gesture_name_tv.text, TextView.BufferType.EDITABLE)
                        gesture_name_tv.visibility = View.GONE
                        gesture_name_et.requestFocus()
                        imm.hideSoftInputFromWindow(gesture_name_et.windowToken, 0)
                        imm.showSoftInput(gesture_name_et, 0)
                        gesture_name_et.isFocusableInTouchMode = true
                        editMode = true
                    }
                }
        RxUpdateMainEvent.getInstance().fingerAngleObservable
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
                        compileBLEMassage (withChangeGesture = true, onlyNumberGesture = false)
                    }
                    if (numberFinger == 2) {
                        changeStateFinger2 (angleFinger)
                        compileBLEMassage (withChangeGesture = true, onlyNumberGesture = false)
                    }
                    if (numberFinger == 3) {
                        changeStateFinger3 (angleFinger)
                        compileBLEMassage (withChangeGesture = true, onlyNumberGesture = false)
                    }
                    if (numberFinger == 4) {
                        changeStateFinger4 (angleFinger)
                        compileBLEMassage (withChangeGesture = true, onlyNumberGesture = false)
                    }
                    if (numberFinger == 5) {
                        changeStateFinger5 (88 - angleFinger)
                        compileBLEMassage (withChangeGesture = true, onlyNumberGesture = false)
                    }
                    if (numberFinger == 6) {
                        changeStateFinger6 (98 - angleFinger)
                        compileBLEMassage (withChangeGesture = true, onlyNumberGesture = false)
                    }
                    if (numberFinger == 55) { }
                }
        RxView.clicks(findViewById(R.id.gripper_state_le))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    animateFinger1 ()
                    animateFinger2 ()
                    animateFinger3 ()
                    animateFinger4 ()
                    animateFinger5 ()
                    animateFinger6 ()
                    if (gestureState == 0 ) {
                        gestureState = 1
                        gripper_state_le.text = getString(R.string.gesture_state_open)
                        } else
                    {
                        gestureState = 0
                        gripper_state_le.text = getString(R.string.gesture_state_close)
                    }
                    compileBLEMassage (withChangeGesture = false, onlyNumberGesture = false)
                }
        RxView.clicks(findViewById(R.id.gripper_use_le))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (editMode) {
                        gestureNameList[(gestureNumber - 1)] = gesture_name_et.text.toString()
                        for (i in 0 until gestureNameList.size) {
                            mySaveText(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + i, gestureNameList[i])
                        }
                    }
                    finish()
                }

        fingers_delay_btn.setOnClickListener {
            showFingersDelayDialog()
        }

        seekBarSpeedFingerLE.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (seekBarSpeedFingerLE.progress < 10) {
                    textSpeedFingerLE.text = "0" + seekBarSpeedFingerLE.progress
                } else {
                    textSpeedFingerLE.text = "" + seekBarSpeedFingerLE.progress
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                RxUpdateMainEvent.getInstance().updateFingerSpeed(seekBarSpeedFingerLE.progress)
            }
        })
    }

    override fun initializeUI() {
        val activityManager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo
        val supportsEs2 = configurationInfo.reqGlEsVersion >= 0x00020000

        if (supportsEs2) {
            gl_surface_view_le_with_encoders.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            gl_surface_view_le_with_encoders.holder.setFormat(PixelFormat.TRANSLUCENT)
            gl_surface_view_le_with_encoders.setBackgroundResource(R.drawable.gradient_background)
            gl_surface_view_le_with_encoders.setZOrderOnTop(true)

            gl_surface_view_le_with_encoders.setEGLContextClientVersion(2)

            val displayMetrics = DisplayMetrics()
            this.windowManager.defaultDisplay.getMetrics(displayMetrics)
            withEncodersRenderer = GripperSettingsWithEncodersRenderer(this, gl_surface_view_le_with_encoders)
            gl_surface_view_le_with_encoders.setRenderer(withEncodersRenderer, displayMetrics.density)
        }
    }

    private fun changeStateFinger1(angleFinger: Int) {
        System.err.println("Изменили палец 1 $angleFinger")
        if (gestureState == 1) {
            fingerOpenState1 = angleFinger
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_1_NUM + (gestureNumber + 1),
                    angleFinger
                )
            }
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_1_NUM + gestureNumber,
                    angleFinger
                )
            }
        } else {
            fingerCloseState1 = angleFinger
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_1_NUM + (gestureNumber + 1),
                    angleFinger
                )
            }
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_1_NUM + gestureNumber,
                    angleFinger
                )
            }
        }
        score1 = angleFinger
    }
    private fun changeStateFinger2(angleFinger: Int) {
        System.err.println("Изменили палец 2 $angleFinger")
        if (gestureState == 1) {
            fingerOpenState2 = angleFinger
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_2_NUM + (gestureNumber + 1),
                    angleFinger
                )
            }
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_2_NUM + gestureNumber,
                    angleFinger
                )
            }
        } else {
            fingerCloseState2 = angleFinger
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_2_NUM + (gestureNumber + 1),
                    angleFinger
                )
            }
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_2_NUM + gestureNumber,
                    angleFinger
                )
            }
        }
        score2 = angleFinger
    }
    private fun changeStateFinger3(angleFinger: Int) {
        System.err.println("Изменили палец 3 $angleFinger")
        if (gestureState == 1) {
            fingerOpenState3 = angleFinger
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_3_NUM + (gestureNumber + 1),
                    angleFinger
                )
            }
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_3_NUM + gestureNumber,
                    angleFinger
                )
            }
        } else {
            fingerCloseState3 = angleFinger
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_3_NUM + (gestureNumber + 1),
                    angleFinger
                )
            }
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_3_NUM + gestureNumber,
                    angleFinger
                )
            }
        }
        score3 = angleFinger
    }
    private fun changeStateFinger4(angleFinger: Int) {
        System.err.println("Изменили палец4 $angleFinger")
        if (gestureState == 1) {
            fingerOpenState4 = angleFinger
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_4_NUM + (gestureNumber + 1), // проверить тут + 0
                    angleFinger
                )
            }
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_4_NUM + gestureNumber,
                    angleFinger
                )
            }
        } else {
            fingerCloseState4 = angleFinger
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_4_NUM + (gestureNumber + 1),
                    angleFinger
                )
            }
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_4_NUM + gestureNumber,
                    angleFinger
                )
            }
        }
        score4 = angleFinger
    }
    private fun changeStateFinger5(angleFinger: Int) {
//        System.err.println("Изменили палец 5 ${(angleFinger.toFloat()/100*91).toInt()-49}") //-16
//        System.err.println("Изменили отправляемые значения палец 5 ${(100-((() +58).toFloat()/86*100).toInt())}")
        if (gestureState == 1) {
            System.err.println("Изменили палец 5 gestureState = 1")
            fingerOpenState5 = (angleFinger.toFloat()/100*91).toInt()-49
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_5_NUM + (gestureNumber + 1),
                    (angleFinger.toFloat() / 100 * 91).toInt() - 49
                )
            }
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_5_NUM + gestureNumber,
                    (angleFinger.toFloat() / 100 * 91).toInt() - 49
                )
            }
        } else {
            System.err.println("Изменили палец 5 gestureState = 0")
            fingerCloseState5 = (angleFinger.toFloat() / 100 * 91).toInt() - 49
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_5_NUM + (gestureNumber + 1),
                    (angleFinger.toFloat() / 100 * 91).toInt() - 49
                )
            }
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_5_NUM + gestureNumber,
                    (angleFinger.toFloat() / 100 * 91).toInt() - 49
                )
            }
        }
        score5 = (angleFinger.toFloat()/100*91).toInt()-49
    }
    private fun changeStateFinger6(angleFinger: Int) {
//        System.err.println("Изменили палец 6 ${(angleFinger.toFloat()/100*90).toInt()}")
//        System.err.println("Изменили отправляемые значения палец 6 ${abs((().toFloat()/85*100).toInt())}")
        if (gestureState == 1) {
            fingerOpenState6 = (angleFinger.toFloat() / 100 * 90).toInt()
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_6_NUM + (gestureNumber + 1),
                    (angleFinger.toFloat() / 100 * 90).toInt()
                )
            }
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_6_NUM + gestureNumber,
                    (angleFinger.toFloat() / 100 * 90).toInt()
                )
            }
        } else {
            fingerCloseState6 = (angleFinger.toFloat() / 100 * 90).toInt()
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_6_NUM + (gestureNumber + 1),
                    (angleFinger.toFloat() / 100 * 90).toInt()
                )
            }
            if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
                saveInt(
                    mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                        .toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_6_NUM + gestureNumber,
                    (angleFinger.toFloat() / 100 * 90).toInt()
                )
            }
        }
        score6 = (angleFinger.toFloat()/100*90).toInt()
    }

    private fun animateFinger1 () {
        if (gestureState == 1) {
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
        if (gestureState == 1) {
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
        if (gestureState == 1) {
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
        if (gestureState == 1) {
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
        if (gestureState == 1) {
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
        if (gestureState == 1) {
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
        val dialogBinding = layoutInflater.inflate(R.layout.dialog_fingers_delay, null)
        val myDialog = Dialog(this)
        myDialog.setContentView(dialogBinding)
        myDialog.setCancelable(false)
        myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog.show()
        myDialog.delay_fingers_animation_view.setAnimation(R.raw.loader_calibrating)

        loadFingersDelay()
        if (gestureState == 1) {
            myDialog.dialog_fingers_delay_title_tv.text = getString(R.string.delay_state_open)
            myDialog.dialog_fingers_delay_1_sb.progress = fingerOpenStateDelay1
            myDialog.dialog_fingers_delay_2_sb.progress = fingerOpenStateDelay2
            myDialog.dialog_fingers_delay_3_sb.progress = fingerOpenStateDelay3
            myDialog.dialog_fingers_delay_4_sb.progress = fingerOpenStateDelay4
            myDialog.dialog_fingers_delay_5_sb.progress = fingerOpenStateDelay5
            myDialog.dialog_fingers_delay_6_sb.progress = fingerOpenStateDelay6
        } else {
            myDialog.dialog_fingers_delay_title_tv.text = getString(R.string.delay_state_close)
            myDialog.dialog_fingers_delay_1_sb.progress = fingerCloseStateDelay1
            myDialog.dialog_fingers_delay_2_sb.progress = fingerCloseStateDelay2
            myDialog.dialog_fingers_delay_3_sb.progress = fingerCloseStateDelay3
            myDialog.dialog_fingers_delay_4_sb.progress = fingerCloseStateDelay4
            myDialog.dialog_fingers_delay_5_sb.progress = fingerCloseStateDelay5
            myDialog.dialog_fingers_delay_6_sb.progress = fingerCloseStateDelay6
        }

        timerCheckReceivingData = object : CountDownTimer(5000000, 500) {
            override fun onTick(millisUntilFinished: Long) {
                if (mSettings!!.getBoolean(PreferenceKeys.RECEIVE_FINGERS_DELAY_BOOL, false)) {
                    timerCheckReceivingData?.onFinish()
                    timerCheckReceivingData?.cancel()
                    firstRequest = true
                    countTick = 0
                } else {
                    if (firstRequest || (countTick%2 == 0)) {
                        compileBLERead(SampleGattAttributes.CHANGE_GESTURE_NEW_VM)
                        firstRequest = false
                    }
                    myDialog.delay_fingers_animation_view.visibility = View.VISIBLE

                    myDialog.first_cl.visibility = View.GONE
                    myDialog.second_cl.visibility = View.GONE
                    myDialog.third_cl.visibility = View.GONE
                    myDialog.fourth_cl.visibility = View.GONE
                    myDialog.fifth_cl.visibility = View.GONE
                    myDialog.sixth_cl.visibility = View.GONE

                    System.err.println("compileBLERead тык $countTick")
                    countTick++
                }
            }

            override fun onFinish() {
                myDialog.delay_fingers_animation_view.visibility = View.GONE

                myDialog.first_cl.visibility = View.VISIBLE
                myDialog.second_cl.visibility = View.VISIBLE
                myDialog.third_cl.visibility = View.VISIBLE
                myDialog.fourth_cl.visibility = View.VISIBLE
                myDialog.fifth_cl.visibility = View.VISIBLE
                myDialog.sixth_cl.visibility = View.VISIBLE

                myDialog.dialog_fingers_delay_first_2_tv.text = getString(R.string.delay_finger_ms, myDialog.dialog_fingers_delay_1_sb.progress*10)
                myDialog.dialog_fingers_delay_second_2_tv.text = getString(R.string.delay_finger_ms, myDialog.dialog_fingers_delay_2_sb.progress*10)
                myDialog.dialog_fingers_delay_third_2_tv.text = getString(R.string.delay_finger_ms, myDialog.dialog_fingers_delay_3_sb.progress*10)
                myDialog.dialog_fingers_delay_fourth_2_tv.text = getString(R.string.delay_finger_ms, myDialog.dialog_fingers_delay_4_sb.progress*10)
                myDialog.dialog_fingers_delay_fifth_2_tv.text = getString(R.string.delay_finger_ms, myDialog.dialog_fingers_delay_5_sb.progress*10)
                myDialog.dialog_fingers_delay_sixth_2_tv.text = getString(R.string.delay_finger_ms, myDialog.dialog_fingers_delay_6_sb.progress*10)

                myDialog.dialog_fingers_delay_1_sb.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        myDialog.dialog_fingers_delay_first_2_tv.text = getString(R.string.delay_finger_ms, myDialog.dialog_fingers_delay_1_sb.progress*10)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        if (seekBar != null) {
                            if (gestureState == 1) {
                                fingerOpenStateDelay1 = seekBar.progress
                            } else {
                                fingerCloseStateDelay1 = seekBar.progress
                            }
                            compileBLEMassage (withChangeGesture = true, onlyNumberGesture = false)
                            saveBool(PreferenceKeys.RECEIVE_FINGERS_DELAY_BOOL, false)
                        }
                    }
                })
                myDialog.dialog_fingers_delay_2_sb.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        myDialog.dialog_fingers_delay_second_2_tv.text = getString(R.string.delay_finger_ms, myDialog.dialog_fingers_delay_2_sb.progress*10)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        if (seekBar != null) {
                            if (gestureState == 1) {
                                fingerOpenStateDelay2 = seekBar.progress
                            } else {
                                fingerCloseStateDelay2 = seekBar.progress
                            }
                            compileBLEMassage (withChangeGesture = true, onlyNumberGesture = false)
                            saveBool(PreferenceKeys.RECEIVE_FINGERS_DELAY_BOOL, false)
                        }
                    }
                })
                myDialog.dialog_fingers_delay_3_sb.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        myDialog.dialog_fingers_delay_third_2_tv.text = getString(R.string.delay_finger_ms, myDialog.dialog_fingers_delay_3_sb.progress*10)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        if (seekBar != null) {
                            if (gestureState == 1) {
                                fingerOpenStateDelay3 = seekBar.progress
                            } else {
                                fingerCloseStateDelay3 = seekBar.progress
                            }
                            compileBLEMassage (withChangeGesture = true, onlyNumberGesture = false)
                            saveBool(PreferenceKeys.RECEIVE_FINGERS_DELAY_BOOL, false)
                        }
                    }
                })
                myDialog.dialog_fingers_delay_4_sb.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        myDialog.dialog_fingers_delay_fourth_2_tv.text = getString(R.string.delay_finger_ms, myDialog.dialog_fingers_delay_4_sb.progress*10)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        if (seekBar != null) {
                            if (gestureState == 1) {
                                fingerOpenStateDelay4 = seekBar.progress
                            } else {
                                fingerCloseStateDelay4 = seekBar.progress
                            }
                            compileBLEMassage (withChangeGesture = true, onlyNumberGesture = false)
                            saveBool(PreferenceKeys.RECEIVE_FINGERS_DELAY_BOOL, false)
                        }
                    }
                })
                myDialog.dialog_fingers_delay_5_sb.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        myDialog.dialog_fingers_delay_fifth_2_tv.text = getString(R.string.delay_finger_ms, myDialog.dialog_fingers_delay_5_sb.progress*10)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        if (seekBar != null) {
                            if (gestureState == 1) {
                                fingerOpenStateDelay5 = seekBar.progress
                            } else {
                                fingerCloseStateDelay5 = seekBar.progress
                            }
                            compileBLEMassage (withChangeGesture = true, onlyNumberGesture = false)
                            saveBool(PreferenceKeys.RECEIVE_FINGERS_DELAY_BOOL, false)
                        }
                    }
                })
                myDialog.dialog_fingers_delay_6_sb.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        myDialog.dialog_fingers_delay_sixth_2_tv.text = getString(R.string.delay_finger_ms, myDialog.dialog_fingers_delay_6_sb.progress*10)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        if (seekBar != null) {
                            if (gestureState == 1) {
                                fingerOpenStateDelay6 = seekBar.progress
                            } else {
                                fingerCloseStateDelay6 = seekBar.progress
                            }
                            compileBLEMassage (withChangeGesture = true, onlyNumberGesture = false)
                            saveBool(PreferenceKeys.RECEIVE_FINGERS_DELAY_BOOL, false)
                        }
                    }

                })
            }
        }.start()


        val cancelBtn = dialogBinding.findViewById<View>(R.id.dialog_fingers_delay_cancel)
        cancelBtn.setOnClickListener {
            myDialog.dismiss()
        }
    }

    private fun compileBLEMassage (withChangeGesture: Boolean, onlyNumberGesture: Boolean) {
        if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            System.err.println("GripperSettingsRender--------> compileBLEMassage FEST_H withChangeGesture =$withChangeGesture")
            val gestureStateModel = GestureStateWithEncoders(gestureNumber - 1,
                fingerOpenState1, fingerOpenState2, fingerOpenState3,
                fingerOpenState4, (100 - (((fingerOpenState5) + 58).toFloat() / 86 * 100).toInt()), abs(((fingerOpenState6).toFloat() / 85 * 100).toInt()),
                fingerCloseState1, fingerCloseState2, fingerCloseState3,
                fingerCloseState4, (100 - (((fingerCloseState5) + 58).toFloat() / 86 * 100).toInt()), abs(((fingerCloseState6).toFloat() / 85 * 100).toInt()),
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                gestureState, withChangeGesture, onlyNumberGesture)
            RxUpdateMainEvent.getInstance().updateGestureWithEncodersState(gestureStateModel)
        }
        if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            System.err.println("GripperSettingsRender--------> compileBLEMassage FEST_X withChangeGesture =$withChangeGesture   fingerOpenState5 : " + (100 - (((fingerOpenState5) + 58).toFloat() / 86 * 100).toInt()) + "   fingerCloseState5 : " + (100 - (((fingerCloseState5) + 58).toFloat() / 86 * 100).toInt()))
            val gestureStateModel = GestureStateWithEncoders(gestureNumber - 1 , // проверить тут -2
                fingerOpenState4, fingerOpenState3, fingerOpenState2,
                fingerOpenState1, (100 - (((fingerOpenState5) + 58).toFloat() / 86 * 100).toInt()), abs(((fingerOpenState6).toFloat() / 85 * 100).toInt()),
                fingerCloseState4, fingerCloseState3, fingerCloseState2,
                fingerCloseState1, (100 - (((fingerCloseState5) + 58).toFloat() / 86 * 100).toInt()), abs(((fingerCloseState6).toFloat() / 85 * 100).toInt()),
                fingerOpenStateDelay1, fingerOpenStateDelay2, fingerOpenStateDelay3, fingerOpenStateDelay4, fingerOpenStateDelay5, fingerOpenStateDelay6,
                fingerCloseStateDelay1, fingerCloseStateDelay2, fingerCloseStateDelay3, fingerCloseStateDelay4, fingerCloseStateDelay5, fingerCloseStateDelay6,
                gestureState, withChangeGesture, onlyNumberGesture)
            RxUpdateMainEvent.getInstance().updateGestureWithEncodersState(gestureStateModel)
        }
    }
    private fun compileBLERead (characteristic: String) {
        RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(characteristic)
    }

    private fun saveInt(key: String, variable: Int) {
        val editor: SharedPreferences.Editor = mSettings!!.edit()
        editor.putInt(key, variable)
        editor.apply()
    }
    private fun saveBool(key: String, variable: Boolean) {
        val editor: SharedPreferences.Editor = mSettings!!.edit()
        editor.putBoolean(key, variable)
        editor.apply()
    }
    private fun mySaveText(key: String, text: String) {
        val editor: SharedPreferences.Editor = mSettings!!.edit()
        editor.putString(key, text)
        editor.apply()
    }
    private fun loadOldState() {
        val text = "load not work"
        mDeviceType = mSettings!!.getString((ConstantManager.EXTRAS_DEVICE_ADDRESS),text).toString()
        if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            fingerOpenState1 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_1_NUM + gestureNumber, 0
            )
            fingerOpenState2 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_2_NUM + gestureNumber, 0
            )
            fingerOpenState3 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_3_NUM + gestureNumber, 0
            )
            fingerOpenState4 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_4_NUM + gestureNumber, 0
            )
            fingerOpenState5 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_5_NUM + gestureNumber, 0
            )
            fingerOpenState6 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_6_NUM + gestureNumber, 0
            )
            fingerCloseState1 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_1_NUM + gestureNumber, 0
            )
            fingerCloseState2 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_2_NUM + gestureNumber, 0
            )
            fingerCloseState3 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_3_NUM + gestureNumber, 0
            )
            fingerCloseState4 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_4_NUM + gestureNumber, 0
            )
            fingerCloseState5 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_5_NUM + gestureNumber, 0
            )
            fingerCloseState6 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_6_NUM + gestureNumber, 0
            )
        }
        if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            fingerOpenState4 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_4_NUM + (gestureNumber + 1), 0 //проверить тут +0
            )
            fingerOpenState3 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_3_NUM + (gestureNumber + 1), 0
            )
            fingerOpenState2 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_2_NUM + (gestureNumber + 1), 0
            )
            fingerOpenState1 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_1_NUM + (gestureNumber + 1), 0
            )
            fingerOpenState5 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_5_NUM + (gestureNumber + 1), 0
            )
            fingerOpenState6 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_6_NUM + (gestureNumber + 1), 0
            )
            fingerCloseState4 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_4_NUM + (gestureNumber + 1), 0
            )
            fingerCloseState3 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_3_NUM + (gestureNumber + 1), 0
            )
            fingerCloseState2 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_2_NUM + (gestureNumber + 1), 0
            )
            fingerCloseState1 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_1_NUM + (gestureNumber + 1), 0
            )
            fingerCloseState5 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_5_NUM + (gestureNumber + 1), 0
            )
            fingerCloseState6 = mSettings!!.getInt(
                mSettings!!.getString(
                    PreferenceKeys.DEVICE_ADDRESS_CONNECTED,
                    text
                ).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_6_NUM + (gestureNumber + 1), 0
            )
        }


        Handler().postDelayed({
            animateFinger1 ()
            animateFinger2 ()
            animateFinger3 ()
            animateFinger4 ()
            animateFinger5 ()
            animateFinger6 ()
            gestureState = 1
            //отправка массива из шести байт для движения протеза в открытое состояние как у макета кисти
            compileBLEMassage (withChangeGesture = false, onlyNumberGesture = false)
            compileBLEMassage (withChangeGesture = false, onlyNumberGesture = false)
        }, 200)

        Handler().postDelayed({
            //флаг становящийся истиным только когда данные запроса приходят
            saveBool(PreferenceKeys.RECEIVE_FINGERS_DELAY_BOOL, false)

            //отправка одного байта(номера жеста) для получения задержек старта пальцев по нему
            compileBLEMassage (withChangeGesture = false, onlyNumberGesture = true)
            compileBLEMassage (withChangeGesture = false, onlyNumberGesture = true)
        }, 600)

        Handler().postDelayed({
            //считывание данных
            compileBLERead(SampleGattAttributes.CHANGE_GESTURE_NEW_VM)
            compileBLERead(SampleGattAttributes.CHANGE_GESTURE_NEW_VM)
            compileBLERead(SampleGattAttributes.CHANGE_GESTURE_NEW_VM)
        }, 1000)
    }
    private fun loadFingersDelay() {
        val text = "load not work"
        mDeviceType = mSettings!!.getString((ConstantManager.EXTRAS_DEVICE_ADDRESS),text).toString()
        if (mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            fingerOpenStateDelay1 = mSettings!!.getInt(
                PreferenceKeys.GESTURE_OPEN_DELAY_FINGER + 1, 0)
            fingerOpenStateDelay2 =  mSettings!!.getInt(
                PreferenceKeys.GESTURE_OPEN_DELAY_FINGER + 2, 0)
            fingerOpenStateDelay3 =  mSettings!!.getInt(
                PreferenceKeys.GESTURE_OPEN_DELAY_FINGER + 3, 0)
            fingerOpenStateDelay4 = mSettings!!.getInt(
                PreferenceKeys.GESTURE_OPEN_DELAY_FINGER + 4, 0)
            fingerOpenStateDelay5 =  mSettings!!.getInt(
                PreferenceKeys.GESTURE_OPEN_DELAY_FINGER + 5, 0)
            fingerOpenStateDelay6 =  mSettings!!.getInt(
                PreferenceKeys.GESTURE_OPEN_DELAY_FINGER + 6, 0)

            fingerCloseStateDelay1 = mSettings!!.getInt(
                PreferenceKeys.GESTURE_CLOSE_DELAY_FINGER + 1, 0)
            fingerCloseStateDelay2 = mSettings!!.getInt(
                PreferenceKeys.GESTURE_CLOSE_DELAY_FINGER + 2, 0)
            fingerCloseStateDelay3 = mSettings!!.getInt(
                PreferenceKeys.GESTURE_CLOSE_DELAY_FINGER + 3, 0)
            fingerCloseStateDelay4 = mSettings!!.getInt(
                PreferenceKeys.GESTURE_CLOSE_DELAY_FINGER + 4, 0)
            fingerCloseStateDelay5 = mSettings!!.getInt(
                PreferenceKeys.GESTURE_CLOSE_DELAY_FINGER + 5, 0)
            fingerCloseStateDelay6 = mSettings!!.getInt(
                PreferenceKeys.GESTURE_CLOSE_DELAY_FINGER + 6, 0)


        }
    }
    private fun myLoadGesturesList() {
        val text = "load not work"
        for (i in 0 until PreferenceKeys.NUM_GESTURES) {
            gestureNameList.add(mSettings!!.getString(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + i, text).toString())
        }
    }
}