package me.start.motorica.new_electronic_by_Rodeon.ui.activities.gripper.with_encoders

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.TextView
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.layout_gripper_settings_le_with_encoders.*
import kotlinx.android.synthetic.main.layout_gripper_settings_le_with_encoders.gripper_state_le
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.compose.BaseActivity
import me.start.motorica.new_electronic_by_Rodeon.compose.qualifiers.RequirePresenter
import me.start.motorica.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import me.start.motorica.new_electronic_by_Rodeon.models.GestureStateWithEncoders
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.presenters.GripperScreenPresenter
import me.start.motorica.new_electronic_by_Rodeon.viewTypes.GripperScreenActivityView
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

    private var gestureState = 0
    private var side: Int = 1

    private var score1 = 0
    private var score2 = 0
    private var score3 = 0
    private var score4 = 0
    private var score5 = 0
    private var score6 = 0

    private var mSettings: SharedPreferences? = null
    private var gestureNumber: Int = 0
    private var gestureNameList =  ArrayList<String>()
    private var editMode: Boolean = false

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
                        edit_gesture_name_btn.setImageResource(R.drawable.ic_cancel_24)
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
                    numberFinger = parameters.numberFinger
                    angleFinger = parameters.fingerAngel
                    if (numberFinger == 1) {
                        changeStateFinger1 (angleFinger)
                        compileBLEMassage (true)
                    }
                    if (numberFinger == 2) {
                        changeStateFinger2 (angleFinger)
                        compileBLEMassage (true)
                    }
                    if (numberFinger == 3) {
                        changeStateFinger3 (angleFinger)
                        compileBLEMassage (true)
                    }
                    if (numberFinger == 4) {
                        changeStateFinger4 (angleFinger)
                        compileBLEMassage (true)
                    }
                    if (numberFinger == 5) {
                        changeStateFinger5 (angleFinger)
                        compileBLEMassage (true)
                    }
                    if (numberFinger == 6) {
                        changeStateFinger6 (angleFinger)
                        compileBLEMassage (true)
                    }
                    if (numberFinger == 55) { }
                }
        RxView.clicks(findViewById(R.id.gripper_state_le))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    animateFinger1 ()
                    if (gestureState == 0 ) {
                        gestureState = 1
                        gripper_state_le.text = getString(R.string.gesture_state_open)
                        } else
                    {
                        gestureState = 0
                        gripper_state_le.text = getString(R.string.gesture_state_close)
                    }
                    compileBLEMassage (false)
                }
        RxView.clicks(findViewById(R.id.gripper_use_le))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    //TODO дописать логику сохранения и принятия в работу данных пред закрытием активити
                    finish()
                }

        seekBarSpeedFingerLE.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
        val supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000

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

    private fun changeStateFinger1 (angleFinger: Int) {
        System.err.println("Изменили палец 1 $angleFinger")
        if (gestureState == 1) {
            fingerOpenState1 = angleFinger
            saveInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "").toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_1_NUM + gestureNumber, angleFinger)
        } else {
            fingerCloseState1 = angleFinger
            saveInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "").toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_1_NUM + gestureNumber, angleFinger)
        }
        score1 = angleFinger
    }
    private fun changeStateFinger2 (angleFinger: Int) {
        System.err.println("Изменили палец 2 $angleFinger")
        if (gestureState == 1) {
            fingerOpenState2 = angleFinger
            saveInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "").toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_2_NUM + gestureNumber, angleFinger)
        } else {
            fingerCloseState2 = angleFinger
            saveInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "").toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_2_NUM + gestureNumber, angleFinger)
        }
        score2 = angleFinger
    }
    private fun changeStateFinger3 (angleFinger: Int) {
        System.err.println("Изменили палец 3 $angleFinger")
        if (gestureState == 1) {
            fingerOpenState3 = angleFinger
            saveInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "").toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_3_NUM + gestureNumber, angleFinger)
        } else {
            fingerCloseState3 = angleFinger
            saveInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "").toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_3_NUM + gestureNumber, angleFinger)
        }
        score3 = angleFinger
    }
    private fun changeStateFinger4 (angleFinger: Int) {
        System.err.println("Изменили палец41 $angleFinger")
        if (gestureState == 1) {
            fingerOpenState4 = angleFinger
            saveInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "").toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_4_NUM + gestureNumber, angleFinger)
        } else {
            fingerCloseState4 = angleFinger
            saveInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "").toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_4_NUM + gestureNumber, angleFinger)
        }
        score4 = angleFinger
    }
    private fun changeStateFinger5 (angleFinger: Int) {
        System.err.println("Изменили палец 5 $angleFinger")
        if (gestureState == 1) {
            fingerOpenState5 = angleFinger
            saveInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "").toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_5_NUM + gestureNumber, angleFinger)
        } else {
            fingerCloseState5 = angleFinger
            saveInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "").toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_5_NUM + gestureNumber, angleFinger)
        }
        score5 = angleFinger
    }
    private fun changeStateFinger6 (angleFinger: Int) {
        System.err.println("Изменили палец 6 $angleFinger")
        if (gestureState == 1) {
            fingerOpenState6 = angleFinger
            saveInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "").toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_6_NUM + gestureNumber, angleFinger)
        } else {
            fingerCloseState6 = angleFinger
            saveInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "").toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_6_NUM + gestureNumber, angleFinger)
        }
        score6 = angleFinger
    }

    private fun animateFinger1 () {
        if (gestureState == 1) {
            System.err.println("Анимация палец 1 $fingerCloseState1 - $score1 = " + (fingerCloseState1-score1))
            val anim1 = ValueAnimator.ofInt(score1, fingerCloseState1)
            anim1.duration = (kotlin.math.abs(fingerCloseState1 - score1) * 10).toLong()
            animationInProgress1 = true
            System.err.println("Анимация animationInProgress = $animationInProgress1" )
            anim1.addUpdateListener {
                angleFinger1 = anim1.animatedValue as Int
                score1 = anim1.animatedValue as Int
                System.err.println("Closing score = $score1")
                if (score1 == fingerCloseState1) {
                    animationInProgress1 = false
                    System.err.println("Анимация animationInProgress = $animationInProgress1" )
                }
            }
            anim1.start()
        } else
        {
            System.err.println("Анимация палец 1 $fingerOpenState1 - $score1 = " + (fingerOpenState1-score1))
            val anim1 = ValueAnimator.ofInt(score1, fingerOpenState1)
            anim1.duration = (kotlin.math.abs(fingerOpenState1 - score1) * 10).toLong()
            animationInProgress1 = true
            System.err.println("Анимация animationInProgress = $animationInProgress1" )
            anim1.addUpdateListener {
                angleFinger1 = anim1.animatedValue as Int
                score1 = anim1.animatedValue as Int
                System.err.println("Opening score = $score1")
                if (score1 == fingerOpenState1) {
                    animationInProgress1 = false
                    System.err.println("Анимация animationInProgress = $animationInProgress1" )
                }
            }
            anim1.start()
        }
    }
    private fun animateFinger2 () {
        if (gestureState == 1) {
            System.err.println("Анимация палец 1 $fingerCloseState1 - $score1 = " + (fingerCloseState1-score1))
            val anim1 = ValueAnimator.ofInt(score1, fingerCloseState1)
            anim1.duration = (kotlin.math.abs(fingerCloseState1 - score1) * 10).toLong()
            animationInProgress1 = true
            System.err.println("Анимация animationInProgress = $animationInProgress1" )
            anim1.addUpdateListener {
                angleFinger1 = anim1.animatedValue as Int
                score1 = anim1.animatedValue as Int
                System.err.println("Closing score = $score1")
                if (score1 == fingerCloseState1) {
                    animationInProgress1 = false
                    System.err.println("Анимация animationInProgress = $animationInProgress1" )
                }
            }
            anim1.start()
        } else
        {
            System.err.println("Анимация палец 1 $fingerOpenState1 - $score1 = " + (fingerOpenState1-score1))
            val anim1 = ValueAnimator.ofInt(score1, fingerOpenState1)
            anim1.duration = (kotlin.math.abs(fingerOpenState1 - score1) * 10).toLong()
            animationInProgress1 = true
            System.err.println("Анимация animationInProgress = $animationInProgress1" )
            anim1.addUpdateListener {
                angleFinger1 = anim1.animatedValue as Int
                score1 = anim1.animatedValue as Int
                System.err.println("Opening score = $score1")
                if (score1 == fingerOpenState1) {
                    animationInProgress1 = false
                    System.err.println("Анимация animationInProgress = $animationInProgress1" )
                }
            }
            anim1.start()
        }
    }
    private fun animateFinger3 () {
        if (gestureState == 1) {
            System.err.println("Анимация палец 1 $fingerCloseState1 - $score1 = " + (fingerCloseState1-score1))
            val anim1 = ValueAnimator.ofInt(score1, fingerCloseState1)
            anim1.duration = (kotlin.math.abs(fingerCloseState1 - score1) * 10).toLong()
            animationInProgress1 = true
            System.err.println("Анимация animationInProgress = $animationInProgress1" )
            anim1.addUpdateListener {
                angleFinger1 = anim1.animatedValue as Int
                score1 = anim1.animatedValue as Int
                System.err.println("Closing score = $score1")
                if (score1 == fingerCloseState1) {
                    animationInProgress1 = false
                    System.err.println("Анимация animationInProgress = $animationInProgress1" )
                }
            }
            anim1.start()
        } else
        {
            System.err.println("Анимация палец 1 $fingerOpenState1 - $score1 = " + (fingerOpenState1-score1))
            val anim1 = ValueAnimator.ofInt(score1, fingerOpenState1)
            anim1.duration = (kotlin.math.abs(fingerOpenState1 - score1) * 10).toLong()
            animationInProgress1 = true
            System.err.println("Анимация animationInProgress = $animationInProgress1" )
            anim1.addUpdateListener {
                angleFinger1 = anim1.animatedValue as Int
                score1 = anim1.animatedValue as Int
                System.err.println("Opening score = $score1")
                if (score1 == fingerOpenState1) {
                    animationInProgress1 = false
                    System.err.println("Анимация animationInProgress = $animationInProgress1" )
                }
            }
            anim1.start()
        }
    }
    private fun animateFinger4 () {
        if (gestureState == 1) {
            System.err.println("Анимация палец 1 $fingerCloseState1 - $score1 = " + (fingerCloseState1-score1))
            val anim1 = ValueAnimator.ofInt(score1, fingerCloseState1)
            anim1.duration = (kotlin.math.abs(fingerCloseState1 - score1) * 10).toLong()
            animationInProgress1 = true
            System.err.println("Анимация animationInProgress = $animationInProgress1" )
            anim1.addUpdateListener {
                angleFinger1 = anim1.animatedValue as Int
                score1 = anim1.animatedValue as Int
                System.err.println("Closing score = $score1")
                if (score1 == fingerCloseState1) {
                    animationInProgress1 = false
                    System.err.println("Анимация animationInProgress = $animationInProgress1" )
                }
            }
            anim1.start()
        } else
        {
            System.err.println("Анимация палец 1 $fingerOpenState1 - $score1 = " + (fingerOpenState1-score1))
            val anim1 = ValueAnimator.ofInt(score1, fingerOpenState1)
            anim1.duration = (kotlin.math.abs(fingerOpenState1 - score1) * 10).toLong()
            animationInProgress1 = true
            System.err.println("Анимация animationInProgress = $animationInProgress1" )
            anim1.addUpdateListener {
                angleFinger1 = anim1.animatedValue as Int
                score1 = anim1.animatedValue as Int
                System.err.println("Opening score = $score1")
                if (score1 == fingerOpenState1) {
                    animationInProgress1 = false
                    System.err.println("Анимация animationInProgress = $animationInProgress1" )
                }
            }
            anim1.start()
        }
    }
    private fun animateFinger5 () {
        if (gestureState == 1) {
            System.err.println("Анимация палец 1 $fingerCloseState1 - $score1 = " + (fingerCloseState1-score1))
            val anim1 = ValueAnimator.ofInt(score1, fingerCloseState1)
            anim1.duration = (kotlin.math.abs(fingerCloseState1 - score1) * 10).toLong()
            animationInProgress1 = true
            System.err.println("Анимация animationInProgress = $animationInProgress1" )
            anim1.addUpdateListener {
                angleFinger1 = anim1.animatedValue as Int
                score1 = anim1.animatedValue as Int
                System.err.println("Closing score = $score1")
                if (score1 == fingerCloseState1) {
                    animationInProgress1 = false
                    System.err.println("Анимация animationInProgress = $animationInProgress1" )
                }
            }
            anim1.start()
        } else
        {
            System.err.println("Анимация палец 1 $fingerOpenState1 - $score1 = " + (fingerOpenState1-score1))
            val anim1 = ValueAnimator.ofInt(score1, fingerOpenState1)
            anim1.duration = (kotlin.math.abs(fingerOpenState1 - score1) * 10).toLong()
            animationInProgress1 = true
            System.err.println("Анимация animationInProgress = $animationInProgress1" )
            anim1.addUpdateListener {
                angleFinger1 = anim1.animatedValue as Int
                score1 = anim1.animatedValue as Int
                System.err.println("Opening score = $score1")
                if (score1 == fingerOpenState1) {
                    animationInProgress1 = false
                    System.err.println("Анимация animationInProgress = $animationInProgress1" )
                }
            }
            anim1.start()
        }
    }
    private fun animateFinger6 () {
        if (gestureState == 1) {
            System.err.println("Анимация палец 1 $fingerCloseState1 - $score1 = " + (fingerCloseState1-score1))
            val anim1 = ValueAnimator.ofInt(score1, fingerCloseState1)
            anim1.duration = (kotlin.math.abs(fingerCloseState1 - score1) * 10).toLong()
            animationInProgress1 = true
            System.err.println("Анимация animationInProgress = $animationInProgress1" )
            anim1.addUpdateListener {
                angleFinger1 = anim1.animatedValue as Int
                score1 = anim1.animatedValue as Int
                System.err.println("Closing score = $score1")
                if (score1 == fingerCloseState1) {
                    animationInProgress1 = false
                    System.err.println("Анимация animationInProgress = $animationInProgress1" )
                }
            }
            anim1.start()
        } else
        {
            System.err.println("Анимация палец 1 $fingerOpenState1 - $score1 = " + (fingerOpenState1-score1))
            val anim1 = ValueAnimator.ofInt(score1, fingerOpenState1)
            anim1.duration = (kotlin.math.abs(fingerOpenState1 - score1) * 10).toLong()
            animationInProgress1 = true
            System.err.println("Анимация animationInProgress = $animationInProgress1" )
            anim1.addUpdateListener {
                angleFinger1 = anim1.animatedValue as Int
                score1 = anim1.animatedValue as Int
                System.err.println("Opening score = $score1")
                if (score1 == fingerOpenState1) {
                    animationInProgress1 = false
                    System.err.println("Анимация animationInProgress = $animationInProgress1" )
                }
            }
            anim1.start()
        }
    }

    private fun compileBLEMassage (withChangeGesture: Boolean) {
        if (gestureState == 1) {
            val gestureStateModel = GestureStateWithEncoders(gestureNumber - 1,
                    fingerOpenState1, fingerOpenState2, fingerOpenState3, fingerOpenState4, fingerOpenState5, fingerOpenState6,
                    fingerCloseState1, fingerCloseState2, fingerCloseState3, fingerCloseState4, fingerCloseState5, fingerCloseState6,
                    gestureState, withChangeGesture)
            RxUpdateMainEvent.getInstance().updateGestureWithEncodersState(gestureStateModel)
        } else {
            val gestureStateModel = GestureStateWithEncoders(gestureNumber - 1,
                    fingerOpenState1, fingerOpenState2, fingerOpenState3, fingerOpenState4, fingerOpenState5, fingerOpenState6,
                    fingerCloseState1, fingerCloseState2, fingerCloseState3, fingerCloseState4, fingerCloseState5, fingerCloseState6,
                    gestureState, withChangeGesture)
            RxUpdateMainEvent.getInstance().updateGestureWithEncodersState(gestureStateModel)
        }
    }

    private fun saveInt(key: String, variable: Int) {
        val editor: SharedPreferences.Editor = mSettings!!.edit()
        editor.putInt(key, variable)
        editor.apply()
    }
    private fun mySaveText(key: String, text: String) {
        val editor: SharedPreferences.Editor = mSettings!!.edit()
        editor.putString(key, text)
        editor.apply()
    }
    private fun loadOldState() {
        val text = "load not work"
        fingerOpenState1 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_1_NUM + gestureNumber, 0)
        fingerOpenState2 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_2_NUM + gestureNumber, 0)
        fingerOpenState3 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_3_NUM + gestureNumber, 0)
        fingerOpenState4 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_4_NUM + gestureNumber, 0)
        fingerOpenState5 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_5_NUM + gestureNumber, 0)
        fingerOpenState6 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_OPEN_STATE_FINGER_6_NUM + gestureNumber, 0)
        fingerCloseState1 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_1_NUM + gestureNumber, 0)
        fingerCloseState2 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_2_NUM + gestureNumber, 0)
        fingerCloseState3 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_3_NUM + gestureNumber, 0)
        fingerCloseState4 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_4_NUM + gestureNumber, 0)
        fingerCloseState5 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_5_NUM + gestureNumber, 0)
        fingerCloseState6 = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_FINGER_6_NUM + gestureNumber, 0)

        side = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1)

        Handler().postDelayed({
            animateFinger1 ()
            gestureState = 1
        }, 200)
//        animateFinger2 ()
//        animateFinger3 ()
//        animateFinger4 ()
//        animateFinger5 ()
//        animateFinger6 ()



        System.err.println("STATE fingerState1: $fingerOpenState1")
        System.err.println("STATE fingerState2: $fingerCloseState1")
//        System.err.println("STATE fingerState3: $fingerState6   angleFinger3: $angleFinger3")
//        System.err.println("STATE fingerState4: $fingerState4   angleFinger4: $angleFinger4")
//        System.err.println("STATE fingerState5: $fingerState5   angleFinger5: $angleFinger5")
//        System.err.println("STATE fingerState6: $fingerState6   angleFinger6: $angleFinger6")
        compileBLEMassage (false)
    }
    private fun myLoadGesturesList() {
        val text = "load not work"
        for (i in 0 until PreferenceKeys.NUM_GESTURES) {
            gestureNameList.add(mSettings!!.getString(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + i, text).toString())
        }
    }
}