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
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.compose.BaseActivity
import me.start.motorica.new_electronic_by_Rodeon.compose.qualifiers.RequirePresenter
import me.start.motorica.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.presenters.GripperScreenPresenter
import me.start.motorica.new_electronic_by_Rodeon.viewTypes.GripperScreenActivityView


@Suppress("DEPRECATION")
@RequirePresenter(GripperScreenPresenter::class)
class GripperScreenWithEncodersActivity
    : BaseActivity<GripperScreenPresenter, GripperScreenActivityView>(), GripperScreenActivityView{
    private var withEncodersRenderer: GripperSettingsWithEncodersRenderer? = null
    private var editMode: Boolean = false
    private var mSettings: SharedPreferences? = null
    private var gestureNumber: Int = 0
    private var gestureNameList =  ArrayList<String>()

    @SuppressLint("CheckResult", "ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_gripper_settings_le_with_encoders)
        initBaseView(this)
        window.navigationBarColor = resources.getColor(R.color.colorPrimaryDark)
        window.statusBarColor = this.resources.getColor(R.color.blueStatusBar, theme)
        mSettings = this.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        gestureNumber = mSettings!!.getInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 0)

        myLoadGesturesList()
        gesture_name_tv.text = gestureNameList[gestureNumber - 1]


        RxView.clicks(findViewById(R.id.gripper_use_le))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    finish()
                }
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

    private fun mySaveText(key: String, text: String) {
        val editor: SharedPreferences.Editor = mSettings!!.edit()
        editor.putString(key, text)
        editor.apply()
    }

    private fun myLoadGesturesList() {
        val text = "load not work"
        for (i in 0 until PreferenceKeys.NUM_GESTURES) {
            gestureNameList.add(mSettings!!.getString(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + i, text).toString())
        }
    }
}