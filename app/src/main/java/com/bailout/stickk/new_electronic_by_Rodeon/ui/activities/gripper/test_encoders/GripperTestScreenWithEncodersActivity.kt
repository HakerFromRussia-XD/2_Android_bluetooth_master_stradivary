package com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.gripper.test_encoders

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.DisplayMetrics
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
//import kotlinx.android.synthetic.main.layout_gripper_settings_le_with_encoders.fingers_delay_btn
//import kotlinx.android.synthetic.main.layout_gripper_settings_le_with_encoders.gesture_name_et
//import kotlinx.android.synthetic.main.layout_gripper_settings_le_with_encoders.gesture_name_tv
//import kotlinx.android.synthetic.main.layout_gripper_settings_le_with_encoders.seekBarSpeedFingerLE
//import kotlinx.android.synthetic.main.layout_gripper_settings_le_with_encoders.textSpeedFingerLE
//import kotlinx.android.synthetic.main.layout_gripper_test_settings_le_with_encoders.*
import com.bailout.stickk.R
import com.bailout.stickk.databinding.LayoutAdvancedSettingsBinding
import com.bailout.stickk.databinding.LayoutGripperSettingsLeWithEncodersBinding
import com.bailout.stickk.databinding.LayoutGripperTestSettingsLeWithEncodersBinding
import com.bailout.stickk.new_electronic_by_Rodeon.compose.BaseActivity
import com.bailout.stickk.new_electronic_by_Rodeon.compose.qualifiers.RequirePresenter
import com.bailout.stickk.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.presenters.GripperScreenPresenter
import com.bailout.stickk.new_electronic_by_Rodeon.viewTypes.GripperScreenActivityView
import kotlin.properties.Delegates


@Suppress("DEPRECATION")
@RequirePresenter(GripperScreenPresenter::class)
class GripperTestScreenWithEncodersActivity
    : BaseActivity<GripperScreenPresenter, GripperScreenActivityView>(), GripperScreenActivityView{
    private var testWithEncodersRenderer: GripperTestSettingsWithEncodersRenderer? = null
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

    private var mSettings: SharedPreferences? = null
    private var gestureNumber: Int = 0
    private var gestureNameList =  ArrayList<String>()
    private var editMode: Boolean = false

    private lateinit var binding: LayoutGripperTestSettingsLeWithEncodersBinding
    @SuppressLint("CheckResult", "ResourceAsColor", "StringFormatInvalid")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutGripperTestSettingsLeWithEncodersBinding.inflate(layoutInflater)
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
        animationInProgress1 = true
        animationInProgress2 = true
        animationInProgress3 = true
        animationInProgress4 = true
        animationInProgress5 = true
        animationInProgress6 = true

        //control side in 3D
        side = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                                       + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1)

        setTimeDelayOfFingers = mSettings!!.getInt(mSettings!!.getString(PreferenceKeys.DEVICE_ADDRESS_CONNECTED, "")
                                + PreferenceKeys.SET_FINGERS_DELAY, 0) == 1


        RxUpdateMainEvent.getInstance().encoders
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                angleFinger1 = it.forefingerEncoder
                angleFinger2 = it.middleFingerEncoder
                angleFinger3 = it.ringFingerEncoder
                angleFinger4 = it.littleFingerEncoder
                angleFinger5 = it.bigEncoder
                angleFinger6 = it.rotationEncoder
            }
        RxUpdateMainEvent.getInstance().encodersError
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                binding.gestureNameTv.text = getString(R.string.error_code, it)
            }
        RxView.clicks(findViewById(R.id.gripper_use_le))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (editMode) {
                        gestureNameList[(gestureNumber - 1)] = binding.gestureNameEt.text.toString()
                        val macKey = mSettings!!.getString(PreferenceKeys.LAST_CONNECTION_MAC, "text")
                        System.err.println("5 LAST_CONNECTION_MAC: $macKey")
                        for (i in 0 until gestureNameList.size) {
                            mySaveText(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + macKey + i, gestureNameList[i-1])
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
            binding.glTestSurfaceViewLeWithEncoders.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            binding.glTestSurfaceViewLeWithEncoders.holder.setFormat(PixelFormat.TRANSLUCENT)
            binding.glTestSurfaceViewLeWithEncoders.setBackgroundResource(R.drawable.gradient_background)
            binding.glTestSurfaceViewLeWithEncoders.setZOrderOnTop(true)

            binding.glTestSurfaceViewLeWithEncoders.setEGLContextClientVersion(2)

            val displayMetrics = DisplayMetrics()
            this.windowManager.defaultDisplay.getMetrics(displayMetrics)
            testWithEncodersRenderer = GripperTestSettingsWithEncodersRenderer(this, binding.glTestSurfaceViewLeWithEncoders)
            binding.glTestSurfaceViewLeWithEncoders.setRenderer(testWithEncodersRenderer, displayMetrics.density)
        }
    }

    private fun mySaveText(key: String, text: String) {
        val editor: SharedPreferences.Editor = mSettings!!.edit()
        editor.putString(key, text)
        editor.apply()
    }
}