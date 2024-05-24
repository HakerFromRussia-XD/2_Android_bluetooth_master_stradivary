package com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.gripper.without_encoders

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import com.bailout.stickk.R
import com.bailout.stickk.databinding.LayoutGripperSettingsLeWithoutEncodersBinding
import com.bailout.stickk.new_electronic_by_Rodeon.compose.BaseActivity
import com.bailout.stickk.new_electronic_by_Rodeon.compose.qualifiers.RequirePresenter
import com.bailout.stickk.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import com.bailout.stickk.new_electronic_by_Rodeon.models.offlineModels.GestureState
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.presenters.GripperScreenPresenter
import com.bailout.stickk.new_electronic_by_Rodeon.viewTypes.GripperScreenActivityView
import kotlin.properties.Delegates


@Suppress("DEPRECATION")
@RequirePresenter(GripperScreenPresenter::class)
class GripperScreenWithoutEncodersActivity
    : BaseActivity<GripperScreenPresenter, GripperScreenActivityView>(), GripperScreenActivityView{
    private var withoutEncodersRenderer: GripperSettingsWithoutEncodersRenderer? = null
    companion object {
        var angleFinger1 by Delegates.notNull<Int>()
        var angleFinger2 by Delegates.notNull<Int>()
        var angleFinger3 by Delegates.notNull<Int>()
        var angleFinger4 by Delegates.notNull<Int>()
        var angleFinger5 by Delegates.notNull<Int>()
        var angleFinger6 by Delegates.notNull<Int>()
    }
    private var numberFinger = 0
    private var fingerState1 = 0
    private var fingerState2 = 0
    private var fingerState3 = 0
    private var fingerState4 = 0
    private var fingerState5 = 0
    private var fingerState6 = 0
    private var gestureState = 1
    private var side: Int = 1

    private var openStage = 0b00000000
    private var closeStage = 0b00000000
    private var oldOpenStage = 0b00000000
    private var oldCloseStage = 0b00000000
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

    private lateinit var binding: LayoutGripperSettingsLeWithoutEncodersBinding

    @SuppressLint("CheckResult", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutGripperSettingsLeWithoutEncodersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initBaseView(this)
        window.navigationBarColor = resources.getColor(R.color.color_primary_dark)
        window.statusBarColor = this.resources.getColor(R.color.blue_status_bar, theme)
        mSettings = this.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        gestureNumber = mSettings!!.getInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 0)
        angleFinger1 = 0
        angleFinger2 = 0
        angleFinger3 = 0
        angleFinger4 = 0
        angleFinger5 = 0
        angleFinger6 = 0

        loadOldState()
        myLoadGesturesList()
        binding.gestureNameWTv.text = gestureNameList[gestureNumber - 1]

        RxView.clicks(findViewById(R.id.edit_gesture_name_w_btn))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    val imm = this.getSystemService(Service.INPUT_METHOD_SERVICE) as InputMethodManager
                    if (editMode) {
                        binding.editGestureNameWBtn.setImageResource(R.drawable.ic_edit_24)
                        binding.gestureNameWTv.visibility = View.VISIBLE
                        imm.hideSoftInputFromWindow(this.currentFocus?.windowToken, 0)
                        binding.gestureNameWTv.text = binding.gestureNameWEt.text
                        binding.gestureNameWEt.visibility = View.GONE
                        gestureNameList[(gestureNumber - 1)] = binding.gestureNameWTv.text.toString()
                        val macKey = mSettings!!.getString(PreferenceKeys.LAST_CONNECTION_MAC, "text")
                        System.err.println("2 LAST_CONNECTION_MAC: $macKey")
                        for (i in 0 until gestureNameList.size) {
                            mySaveText(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + macKey + i, gestureNameList[i])
                        }
                        editMode = false
                    } else {
                        binding.editGestureNameWBtn.setImageResource(R.drawable.ic_ok_24)
                        binding.gestureNameWEt.visibility = View.VISIBLE
                        binding.gestureNameWEt.setText(binding.gestureNameWTv.text, TextView.BufferType.EDITABLE)
                        binding.gestureNameWTv.visibility = View.GONE
                        binding.gestureNameWEt.requestFocus()
                        imm.hideSoftInputFromWindow(binding.gestureNameWEt.windowToken, 0)
                        imm.showSoftInput(binding.gestureNameWEt, 0)
                        binding.gestureNameWEt.isFocusableInTouchMode = true
                        editMode = true
                    }
                }
        RxUpdateMainEvent.getInstance().selectedObjectObservable
                .compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { station ->
                    numberFinger = station
                    if (numberFinger == 1) {
                        closeRotation()
                        animateFinger1 ()
                        compileBLEMassage ()
                    }
                    if (numberFinger == 2) {
                        closeRotation()
                        animateFinger2 ()
                        compileBLEMassage ()
                    }
                    if (numberFinger == 3) {
                        closeRotation()
                        animateFinger3 ()
                        compileBLEMassage ()
                    }
                    if (numberFinger == 4) {
                        closeRotation()
                        animateFinger4 ()
                        compileBLEMassage ()
                    }
                    if (numberFinger == 5) {
                        openRotation()
                        animateFinger5 ()
                        compileBLEMassage ()
                    }
                    if (numberFinger == 55) { closeRotation() }
                }
        RxView.clicks(findViewById(R.id.gripper_state_le))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (gestureState == 0 ) {
                        gestureState = 1
                        binding.gripperStateLe.text = getString(R.string.gesture_state_open)
                        if (openStage shr 0 and 0b00000001 != fingerState1) { animateFinger1 () }
                        if (openStage shr 1 and 0b00000001 != fingerState2) { animateFinger2 () }
                        if (openStage shr 2 and 0b00000001 != fingerState3) { animateFinger3 () }
                        if (openStage shr 3 and 0b00000001 != fingerState4) { animateFinger4 () }
                        if (openStage shr 4 and 0b00000001 != fingerState5) { animateFinger5 () }
                        if (openStage shr 5 and 0b00000001 != fingerState6) { animateFinger6 () }
                    } else
                    {
                        gestureState = 0
                        binding.gripperStateLe.text = getString(R.string.gesture_state_close)
                        if (closeStage shr 0 and 0b00000001 != fingerState1) { animateFinger1 () }
                        if (closeStage shr 1 and 0b00000001 != fingerState2) { animateFinger2 () }
                        if (closeStage shr 2 and 0b00000001 != fingerState3) { animateFinger3 () }
                        if (closeStage shr 3 and 0b00000001 != fingerState4) { animateFinger4 () }
                        if (closeStage shr 4 and 0b00000001 != fingerState5) { animateFinger5 () }
                        if (closeStage shr 5 and 0b00000001 != fingerState6) { animateFinger6 () }
                    }
                    compileBLEMassage ()
                }
        RxView.clicks(findViewById(R.id.gripper_position_finger_le))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    animateFinger6 ()
                    compileBLEMassage ()
                }
        RxView.clicks(findViewById(R.id.gripper_use_le_save))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    System.err.println("SAVE STATE DEVICE_ADDRESS: " + (mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "").toString()))
                    saveInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "").toString() + PreferenceKeys.GESTURE_OPEN_STATE_NUM + gestureNumber, openStage)
                    System.err.println("SAVE STATE openStage: $openStage")
                    saveInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "").toString() + PreferenceKeys.GESTURE_CLOSE_STATE_NUM + gestureNumber, closeStage)
                    System.err.println("SAVE STATE closeStage: $closeStage")
                    val macKey = mSettings!!.getString(PreferenceKeys.LAST_CONNECTION_MAC, "text")
                    System.err.println("3 LAST_CONNECTION_MAC: $macKey")
                    if (editMode) {
                        binding.gestureNameWEt
                        //переезжаемнаbinding
//                        gestureNameList[(gestureNumber - 1)] = gesture_name_et.text.toString()
                        for (i in 0 until gestureNameList.size) {
                            mySaveText(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + macKey + i, gestureNameList[i])
                        }
                    }
                    finish()
                }
    }
    override fun initializeUI() {
        val activityManager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo
        val supportsEs2 = configurationInfo.reqGlEsVersion >= 0x00020000

        if (supportsEs2) {
            binding.glSurfaceViewLeWithoutEncoders.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            binding.glSurfaceViewLeWithoutEncoders.holder.setFormat(PixelFormat.TRANSLUCENT)
            binding.glSurfaceViewLeWithoutEncoders.setBackgroundResource(R.drawable.gradient_background)
            binding.glSurfaceViewLeWithoutEncoders.setZOrderOnTop(true)

            binding.glSurfaceViewLeWithoutEncoders.setEGLContextClientVersion(2)

            val displayMetrics = DisplayMetrics()
            this.windowManager.defaultDisplay.getMetrics(displayMetrics)
            withoutEncodersRenderer = GripperSettingsWithoutEncodersRenderer(this, binding.glSurfaceViewLeWithoutEncoders)
            binding.glSurfaceViewLeWithoutEncoders.setRenderer(withoutEncodersRenderer, displayMetrics.density)
        }
    }

    private fun animateFinger1 () {
        if (fingerState1 == 0 ) {
            if (angleFinger1 < 50) {
                val anim1 = ValueAnimator.ofInt(score1, 100)
                anim1.duration = ((100 - score1) * 10).toLong()
                anim1.addUpdateListener {
                    angleFinger1 = anim1.animatedValue as Int
                    score1 = anim1.animatedValue as Int
                }
                anim1.start()
                fingerState1 = 1
                if (side == 1) {
                    if (gestureState == 1) {
                        openStage = openStage or 0b00000001
                        System.err.println("OPEN STATE CHANGE $openStage")
                    } else {
                        closeStage = closeStage or 0b00000001
                        System.err.println("CLOSE STATE CHANGE $closeStage")
                    }
                } else {
                    if (gestureState == 1) {
                        openStage = openStage or 0b00001000
                        System.err.println("OPEN STATE CHANGE $openStage")
                    } else {
                        closeStage = closeStage or 0b00001000
                        System.err.println("CLOSE STATE CHANGE $closeStage")
                    }
                }

            }
        } else
        {
            if (angleFinger1 > 50) {
                val anim1 = ValueAnimator.ofInt(score1, 0)
                anim1.duration = (score1 * 10).toLong()
                anim1.addUpdateListener {
                    angleFinger1 = anim1.animatedValue as Int
                    score1 = anim1.animatedValue as Int
                }
                anim1.start()
                fingerState1 = 0
                if (side == 1) {
                    if (gestureState == 1) {
                        openStage = openStage and 0b11111110
                        System.err.println("OPEN STATE CHANGE $openStage")
                    } else {
                        closeStage = closeStage and 0b11111110
                        System.err.println("CLOSE STATE CHANGE $closeStage")
                    }
                } else {
                    if (gestureState == 1) {
                        openStage = openStage and 0b11110111
                        System.err.println("OPEN STATE CHANGE $openStage")
                    } else {
                        closeStage = closeStage and 0b11110111
                        System.err.println("CLOSE STATE CHANGE $closeStage")
                    }
                }
            }
        }
    }
    private fun animateFinger2 () {
        if (fingerState2 == 0 ) {
            if (angleFinger2 < 50) {
                val anim1 = ValueAnimator.ofInt(score2, 100)
                anim1.duration = ((100 - score2) * 10).toLong()
                anim1.addUpdateListener {
                    angleFinger2 = anim1.animatedValue as Int
                    score2 = anim1.animatedValue as Int
                }
                anim1.start()
                fingerState2 = 1
                if (side == 1) {
                    if (gestureState == 1) {
                        openStage = openStage or 0b00000010
                        System.err.println("OPEN STATE CHANGE $openStage")
                    } else {
                        closeStage = closeStage or 0b00000010
                        System.err.println("CLOSE STATE CHANGE $closeStage")
                    }
                } else {
                    if (gestureState == 1) {
                        openStage = openStage or 0b00000100
                        System.err.println("OPEN STATE CHANGE $openStage")
                    } else {
                        closeStage = closeStage or 0b00000100
                        System.err.println("CLOSE STATE CHANGE $closeStage")
                    }
                }
            }
        } else
        {
            if (angleFinger2 > 50) {
                val anim1 = ValueAnimator.ofInt(score2, 0)
                anim1.duration = (score2 * 10).toLong()
                anim1.addUpdateListener {
                    angleFinger2 = anim1.animatedValue as Int
                    score2 = anim1.animatedValue as Int
                }
                anim1.start()
                fingerState2 = 0
                if (side == 1) {
                    if (gestureState == 1) {
                        openStage = openStage and 0b11111101
                        System.err.println("OPEN STATE CHANGE $openStage")
                    } else {
                        closeStage = closeStage and 0b11111101
                        System.err.println("CLOSE STATE CHANGE $closeStage")
                    }
                } else {
                    if (gestureState == 1) {
                        openStage = openStage and 0b11111011
                        System.err.println("OPEN STATE CHANGE $openStage")
                    } else {
                        closeStage = closeStage and 0b11111011
                        System.err.println("CLOSE STATE CHANGE $closeStage")
                    }
                }
            }
        }
    }
    private fun animateFinger3 () {
        if (fingerState3 == 0 ) {
            if (angleFinger3 < 50) {
                val anim3 = ValueAnimator.ofInt(score3, 100)
                anim3.duration = ((100 - score3) * 10).toLong()
                anim3.addUpdateListener {
                    angleFinger3 = anim3.animatedValue as Int
                    score3 = anim3.animatedValue as Int
                }
                anim3.start()
                fingerState3 = 1
                if (side == 1) {
                    if (gestureState == 1) {
                        openStage = openStage or 0b00000100
                        System.err.println("OPEN STATE CHANGE $openStage")
                    } else {
                        closeStage = closeStage or 0b00000100
                        System.err.println("CLOSE STATE CHANGE $closeStage")
                    }
                } else {
                    if (gestureState == 1) {
                        openStage = openStage or 0b00000010
                        System.err.println("OPEN STATE CHANGE $openStage")
                    } else {
                        closeStage = closeStage or 0b00000010
                        System.err.println("CLOSE STATE CHANGE $closeStage")
                    }
                }
            }
        } else
        {
            if (angleFinger3 > 50) {
                val anim3 = ValueAnimator.ofInt(score3, 0)
                anim3.duration = (score3 * 10).toLong()
                anim3.addUpdateListener {
                    angleFinger3 = anim3.animatedValue as Int
                    score3 = anim3.animatedValue as Int
                }
                anim3.start()
                fingerState3 = 0
                if (side == 1) {
                    if (gestureState == 1) {
                        openStage = openStage and 0b11111011
                        System.err.println("OPEN STATE CHANGE $openStage")
                    } else {
                        closeStage = closeStage and 0b11111011
                        System.err.println("CLOSE STATE CHANGE $closeStage")
                    }
                } else {
                    if (gestureState == 1) {
                        openStage = openStage and 0b11111101
                        System.err.println("OPEN STATE CHANGE $openStage")
                    } else {
                        closeStage = closeStage and 0b11111101
                        System.err.println("CLOSE STATE CHANGE $closeStage")
                    }
                }
            }
        }
    }
    private fun animateFinger4 () {
        if (fingerState4 == 0 ) {
            if (angleFinger4 < 50) {
                val anim4 = ValueAnimator.ofInt(score4, 100)
                anim4.duration = ((100 - score4) * 10).toLong()
                anim4.addUpdateListener {
                    angleFinger4 = anim4.animatedValue as Int
                    score4 = anim4.animatedValue as Int
                }
                anim4.start()
                fingerState4 = 1
                if (side == 1) {
                    if (gestureState == 1) {
                        openStage = openStage or 0b00001000
                        System.err.println("OPEN STATE CHANGE $openStage")
                    } else {
                        closeStage = closeStage or 0b00001000
                        System.err.println("CLOSE STATE CHANGE $closeStage")
                    }
                } else {
                    if (gestureState == 1) {
                        openStage = openStage or 0b00000001
                        System.err.println("OPEN STATE CHANGE $openStage")
                    } else {
                        closeStage = closeStage or 0b00000001
                        System.err.println("CLOSE STATE CHANGE $closeStage")
                    }
                }
            }
        } else
        {
            if (angleFinger4 > 50) {
                val anim4 = ValueAnimator.ofInt(score4, 0)
                anim4.duration = (score4 * 10).toLong()
                anim4.addUpdateListener {
                    angleFinger4 = anim4.animatedValue as Int
                    score4 = anim4.animatedValue as Int
                }
                anim4.start()
                fingerState4 = 0
                if (side == 1) {
                    if (gestureState == 1) {
                        openStage = openStage and 0b11110111
                        System.err.println("OPEN STATE CHANGE $openStage")
                    } else {
                        closeStage = closeStage and 0b11110111
                        System.err.println("CLOSE STATE CHANGE $closeStage")
                    }
                } else {
                    if (gestureState == 1) {
                        openStage = openStage and 0b11111110
                        System.err.println("OPEN STATE CHANGE $openStage")
                    } else {
                        closeStage = closeStage and 0b11111110
                        System.err.println("CLOSE STATE CHANGE $closeStage")
                    }
                }
            }
        }
    }
    private fun animateFinger5 () {
        System.err.println("STATE angleFinger5 $angleFinger5")
        if (fingerState5 == 0 ) {
            if (angleFinger5 > -10) {
                val anim5 = ValueAnimator.ofInt(score5, -60)
                anim5.duration = (1000).toLong()
                anim5.addUpdateListener {
                    angleFinger5 = anim5.animatedValue as Int
                    score5 = anim5.animatedValue as Int
                }
                anim5.start()
                fingerState5 = 1
                if (gestureState == 1) {
                    openStage = openStage or 0b00010000
                    System.err.println("OPEN STATE CHANGE $openStage")
                } else {
                    closeStage = closeStage or 0b00010000
                    System.err.println("CLOSE STATE CHANGE $closeStage")
                }
            }
        } else
        {
            if (angleFinger5 < -10) {
                val anim5 = ValueAnimator.ofInt(score5, 30)
                anim5.duration = (1000).toLong()
                anim5.addUpdateListener {
                    angleFinger5 = anim5.animatedValue as Int
                    score5 = anim5.animatedValue as Int
                }
                anim5.start()
                fingerState5 = 0
                if (gestureState == 1) {
                    openStage = openStage and 0b11101111
                    System.err.println("OPEN STATE CHANGE $openStage")
                } else {
                    closeStage = closeStage and 0b11101111
                    System.err.println("CLOSE STATE CHANGE $closeStage")
                }
            }
        }
    }
    private fun animateFinger6 () {
        System.err.println("STATE fingerState6: $fingerState6   angleFinger6: $angleFinger6")
        if (fingerState6 == 0 ) {
            if (angleFinger6 < 50) {
                val anim6 = ValueAnimator.ofInt(score6, 100)
                anim6.duration = ((100 - score6) * 10).toLong()
                anim6.addUpdateListener {
                    angleFinger6 = anim6.animatedValue as Int
                    score6 = anim6.animatedValue as Int
                }
                anim6.start()
                binding.gripperPositionFingerLe.text = getString(R.string.rotation_state_open)
                fingerState6 = 1
                if (gestureState == 1) {
                    openStage = openStage or 0b00100000
                    System.err.println("OPEN STATE CHANGE $openStage")
                } else {
                    closeStage = closeStage or 0b00100000
                    System.err.println("CLOSE STATE CHANGE $closeStage")
                }
            }
        } else
        {
            if (angleFinger6 > 50) {
                val anim6 = ValueAnimator.ofInt(score6, 0)
                anim6.duration = (score6 * 10).toLong()
                anim6.addUpdateListener {
                    angleFinger6 = anim6.animatedValue as Int
                    score6 = anim6.animatedValue as Int
                }
                anim6.start()
                binding.gripperPositionFingerLe.text = getString(R.string.rotation_state_close)
                fingerState6 = 0
                if (gestureState == 1) {
                    openStage = openStage and 0b11011111
                    System.err.println("OPEN STATE CHANGE $openStage")
                } else {
                    closeStage = closeStage and 0b11011111
                    System.err.println("CLOSE STATE CHANGE $closeStage")
                }
            }
        }
    }
    private fun openRotation() {
        if ((binding.gestureStateRl.layoutParams as LayoutParams).weight == 2.0f) {
            val lParams = binding.gestureStateRl.layoutParams as LayoutParams
            val anim7 = ValueAnimator.ofFloat(2.0f, 1.0f)
            anim7.duration = (250).toLong()
            anim7.addUpdateListener {
                lParams.weight = anim7.animatedValue as Float
                binding.gestureStateRl.layoutParams = lParams
            }
            anim7.start()
        }
    }
    private fun closeRotation() {
        if ((binding.gestureStateRl.layoutParams as LayoutParams).weight == 1.0f) {
            val lParams = binding.gestureStateRl.layoutParams as LayoutParams
            val anim8 = ValueAnimator.ofFloat(1.0f, 2.0f)
            anim8.duration = (250).toLong()
            anim8.addUpdateListener {
                lParams.weight = anim8.animatedValue as Float
                binding.gestureStateRl.layoutParams = lParams
            }
            anim8.start()
        }
    }
    private fun compileBLEMassage () {
        if (gestureState == 1) {
            val gestureStateModel = GestureState(gestureNumber - 1, openStage, closeStage,gestureState)
            RxUpdateMainEvent.getInstance().updateGestureState(gestureStateModel)
        } else {
            val gestureStateModel = GestureState(gestureNumber - 1, openStage, closeStage,gestureState)
            RxUpdateMainEvent.getInstance().updateGestureState(gestureStateModel)
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
        openStage = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_OPEN_STATE_NUM + gestureNumber, 0)
        closeStage = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.GESTURE_CLOSE_STATE_NUM + gestureNumber, 0)
        oldOpenStage = openStage
        System.err.println("LOAD STATE openStage: $openStage")
        oldCloseStage = closeStage
        System.err.println("LOAD STATE closeStage: $closeStage")
        side = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, text).toString() + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1)

        if (side == 1) {
            if (openStage shr 0 and 0b00000001 != fingerState1) { animateFinger1 () }
            if (openStage shr 1 and 0b00000001 != fingerState2) { animateFinger2 () }
            if (openStage shr 2 and 0b00000001 != fingerState3) { animateFinger3 () }
            if (openStage shr 3 and 0b00000001 != fingerState4) { animateFinger4 () }
            if (openStage shr 4 and 0b00000001 != fingerState5) { animateFinger5 () }
            if (openStage shr 5 and 0b00000001 != fingerState6) { animateFinger6 () }
        } else {
            if (openStage shr 0 and 0b00000001 != fingerState4) { animateFinger4 () }
            if (openStage shr 1 and 0b00000001 != fingerState3) { animateFinger3 () }
            if (openStage shr 2 and 0b00000001 != fingerState2) { animateFinger2 () }
            if (openStage shr 3 and 0b00000001 != fingerState1) { animateFinger1 () }
            if (openStage shr 4 and 0b00000001 != fingerState5) { animateFinger5 () }
            if (openStage shr 5 and 0b00000001 != fingerState6) { animateFinger6 () }
        }

        System.err.println("STATE fingerState1: $fingerState1   angleFinger1: $angleFinger1")
        System.err.println("STATE fingerState2: $fingerState2   angleFinger2: $angleFinger2")
        System.err.println("STATE fingerState3: $fingerState6   angleFinger3: $angleFinger3")
        System.err.println("STATE fingerState4: $fingerState4   angleFinger4: $angleFinger4")
        System.err.println("STATE fingerState5: $fingerState5   angleFinger5: $angleFinger5")
        System.err.println("STATE fingerState6: $fingerState6   angleFinger6: $angleFinger6")
        compileBLEMassage ()
    }
    private fun myLoadGesturesList() {
        val text = "load not work"
        val macKey = mSettings!!.getString(PreferenceKeys.LAST_CONNECTION_MAC, text)
        System.err.println("4 LAST_CONNECTION_MAC: $macKey")
        for (i in 0 until PreferenceKeys.NUM_GESTURES) {
            gestureNameList.add(mSettings!!.getString(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + macKey + i, text).toString())
        }
    }
}