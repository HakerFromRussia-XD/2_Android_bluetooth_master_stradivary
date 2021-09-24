package me.start.motorica.new_electronic_by_Rodeon.ui.activities.gripper.with_encoders

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.TextView
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.layout_gripper_settings_le_with_encoders.*
import kotlinx.android.synthetic.main.layout_gripper_settings_le_with_encoders.gripper_state_le
import kotlinx.android.synthetic.main.layout_gripper_settings_le_without_encoders.*
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.compose.BaseActivity
import me.start.motorica.new_electronic_by_Rodeon.compose.qualifiers.RequirePresenter
import me.start.motorica.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import me.start.motorica.new_electronic_by_Rodeon.models.GestureState
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.presenters.GripperScreenPresenter
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.gripper.without_encoders.GripperScreenWithoutEncodersActivity
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

    private var gestureState = 1
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
                    System.err.println(" MainActivity -----> change gripper.  numberFinger = ${parameters.numberFinger} "+
                            "fingerAngel = ${parameters.fingerAngel}")
                    numberFinger = parameters.numberFinger
                    angleFinger = parameters.fingerAngel
                    if (numberFinger == 1) {
//                        closeRotation()
//                        animateFinger1 ()
                        changeStateFinger1 (angleFinger)
                        //TODO меняем переменную угла соответствующего пальца, сохраняем её
                        compileBLEMassage ()
                    }
                    if (numberFinger == 2) {
//                        closeRotation()
//                        animateFinger2 ()
                        changeStateFinger2 (angleFinger)
                        compileBLEMassage ()
                    }
                    if (numberFinger == 3) {
//                        closeRotation()
//                        animateFinger3 ()
                        changeStateFinger3 (angleFinger)
                        compileBLEMassage ()
                    }
                    if (numberFinger == 4) {
//                        closeRotation()
//                        animateFinger4 ()
                        changeStateFinger4 (angleFinger)
                        compileBLEMassage ()
                    }
                    if (numberFinger == 5) {
//                        openRotation()
//                        animateFinger5 ()
                        changeStateFinger5 (angleFinger)
                        compileBLEMassage ()
                    }
                    if (numberFinger == 6) {
//                        openRotation()
//                        animateFinger5 ()
                        changeStateFinger6 (angleFinger)
                        compileBLEMassage ()
                    }
                    if (numberFinger == 55) {
//                        closeRotation()
                    }
                }
        RxView.clicks(findViewById(R.id.gripper_state_le))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (gestureState == 0 ) {
                        gestureState = 1
                        gripper_state_le.text = getString(R.string.gesture_state_open)
                        } else
                    {
                        gestureState = 0
                        gripper_state_le.text = getString(R.string.gesture_state_close)
                    }
                    compileBLEMassage ()
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
        } else {
            fingerCloseState1 = angleFinger
        }
    }
    private fun changeStateFinger2 (angleFinger: Int) {
        System.err.println("Изменили палец 2 $angleFinger")
        if (gestureState == 1) {
            fingerOpenState2 = angleFinger
        } else {
            fingerCloseState2 = angleFinger
        }
    }
    private fun changeStateFinger3 (angleFinger: Int) {
        System.err.println("Изменили палец 3 $angleFinger")
        if (gestureState == 1) {
            fingerOpenState3 = angleFinger
        } else {
            fingerCloseState3 = angleFinger
        }
    }
    private fun changeStateFinger4 (angleFinger: Int) {
        System.err.println("Изменили палец41 $angleFinger")
        if (gestureState == 1) {
            fingerOpenState4 = angleFinger
        } else {
            fingerCloseState4 = angleFinger
        }
    }
    private fun changeStateFinger5 (angleFinger: Int) {
        System.err.println("Изменили палец 5 $angleFinger")
        if (gestureState == 1) {
            fingerOpenState5 = angleFinger
        } else {
            fingerCloseState5 = angleFinger
        }
    }
    private fun changeStateFinger6 (angleFinger: Int) {
        System.err.println("Изменили палец 6 $angleFinger")
        if (gestureState == 1) {
            fingerOpenState6 = angleFinger
        } else {
            fingerCloseState6 = angleFinger
        }
    }

    private fun compileBLEMassage () {
        if (gestureState == 1) {
//            val gestureStateModel = GestureState(gestureNumber - 1, openStage, closeStage,gestureState)
//            RxUpdateMainEvent.getInstance().updateGestureState(gestureStateModel)
        } else {
//            val gestureStateModel = GestureState(gestureNumber - 1, openStage, closeStage,gestureState)
//            RxUpdateMainEvent.getInstance().updateGestureState(gestureStateModel)
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
//        openStage = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_OPEN_STATE_NUM + gestureNumber, 0)
//        closeStage = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_NUM + gestureNumber, 0)
//        oldOpenStage = openStage
//        System.err.println("LOAD STATE openStage: $openStage")
//        oldCloseStage = closeStage
//        System.err.println("LOAD STATE closeStage: $closeStage")
//        side = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1)
//
//        if (side == 1) {
//            if (openStage shr 0 and 0b00000001 != fingerState1) { animateFinger1 () }
//            if (openStage shr 1 and 0b00000001 != fingerState2) { animateFinger2 () }
//            if (openStage shr 2 and 0b00000001 != fingerState3) { animateFinger3 () }
//            if (openStage shr 3 and 0b00000001 != fingerState4) { animateFinger4 () }
//            if (openStage shr 4 and 0b00000001 != fingerState5) { animateFinger5 () }
//            if (openStage shr 5 and 0b00000001 != fingerState6) { animateFinger6 () }
//        } else {
//            if (openStage shr 0 and 0b00000001 != fingerState4) { animateFinger4 () }
//            if (openStage shr 1 and 0b00000001 != fingerState3) { animateFinger3 () }
//            if (openStage shr 2 and 0b00000001 != fingerState2) { animateFinger2 () }
//            if (openStage shr 3 and 0b00000001 != fingerState1) { animateFinger1 () }
//            if (openStage shr 4 and 0b00000001 != fingerState5) { animateFinger5 () }
//            if (openStage shr 5 and 0b00000001 != fingerState6) { animateFinger6 () }
//        }
//
//        System.err.println("STATE fingerState1: $fingerState1   angleFinger1: ${GripperScreenWithoutEncodersActivity.angleFinger1}")
//        System.err.println("STATE fingerState2: $fingerState2   angleFinger2: ${GripperScreenWithoutEncodersActivity.angleFinger2}")
//        System.err.println("STATE fingerState3: $fingerState6   angleFinger3: ${GripperScreenWithoutEncodersActivity.angleFinger3}")
//        System.err.println("STATE fingerState4: $fingerState4   angleFinger4: ${GripperScreenWithoutEncodersActivity.angleFinger4}")
//        System.err.println("STATE fingerState5: $fingerState5   angleFinger5: ${GripperScreenWithoutEncodersActivity.angleFinger5}")
//        System.err.println("STATE fingerState6: $fingerState6   angleFinger6: ${GripperScreenWithoutEncodersActivity.angleFinger6}")
//        compileBLEMassage ()
    }
    private fun myLoadGesturesList() {
        val text = "load not work"
        for (i in 0 until PreferenceKeys.NUM_GESTURES) {
            gestureNameList.add(mSettings!!.getString(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + i, text).toString())
        }
    }
}