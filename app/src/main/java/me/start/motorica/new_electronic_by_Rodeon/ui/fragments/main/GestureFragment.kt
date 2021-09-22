package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color.*
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.layout_gestures.*
import me.start.motorica.R
import me.start.motorica.R.drawable.*
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager
import me.start.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes
import me.start.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.*
import me.start.motorica.new_electronic_by_Rodeon.compose.BaseActivity
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceManager
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.gripper.with_encoders.GripperScreenWithEncodersActivity
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.gripper.without_encoders.GripperScreenWithoutEncodersActivity
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import org.jetbrains.anko.backgroundDrawable
import org.jetbrains.anko.textColor
import javax.inject.Inject

@Suppress("DEPRECATION")
class GestureFragment: Fragment(), OnChartValueSelectedListener {
    @Inject
    lateinit var preferenceManager: PreferenceManager

    private var rootView: View? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null
    private var gestureNameList =  ArrayList<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.layout_gestures, container, false)
        WDApplication.component.inject(this)
        this.rootView = rootView
        if (activity != null) { main = activity as MainActivity? }
        return rootView
    }

    @SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables", "UseCompatLoadingForColorStateLists")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        selectActiveGesture(mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, 1))
        main?.offGesturesUIBeforeConnection()



        gesture_1_btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                resetStateButtons()
                selectActiveGesture(1)
                main?.saveInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, 1)
//                main?.bleCommandConnector(byteArrayOf(0), SET_GESTURE, WRITE,13)
                compileBLEMassage (0)
            }
        }
        gesture_settings_1_btn.setOnClickListener {
            main?.openFragment(1)
            main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 1)
        }
        gesture_2_btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                resetStateButtons()
                selectActiveGesture(2)
                main?.saveInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, 2)
//                main?.bleCommandConnector(byteArrayOf(1), SET_GESTURE, WRITE, 13)
                compileBLEMassage (1)
            }
        }
        gesture_settings_2_btn.setOnClickListener {
            val intent = Intent(context, GripperScreenWithoutEncodersActivity::class.java)
            startActivity(intent)
            main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 2)
        }
        gesture_3_btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                resetStateButtons()
                selectActiveGesture(3)
                main?.saveInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, 3)
//                main?.bleCommandConnector(byteArrayOf(2), SET_GESTURE, WRITE, 13)
                compileBLEMassage (2)
            }
        }
        gesture_settings_3_btn.setOnClickListener {
            val intent = Intent(context, GripperScreenWithoutEncodersActivity::class.java)
            startActivity(intent)
            main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 3)
        }
        gesture_4_btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                resetStateButtons()
                selectActiveGesture(4)
                main?.saveInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, 4)
                compileBLEMassage (3)
//                main?.bleCommandConnector(byteArrayOf(3), SET_GESTURE, WRITE, 13)
            }
        }
        gesture_settings_4_btn.setOnClickListener {
            val intent = Intent(context, GripperScreenWithoutEncodersActivity::class.java)
            startActivity(intent)
            main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 4)
        }
        gesture_5_btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                resetStateButtons()
                selectActiveGesture(5)
                main?.saveInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, 5)
                compileBLEMassage (4)
//                main?.bleCommandConnector(byteArrayOf(4), SET_GESTURE, WRITE, 13)
            }
        }
        gesture_settings_5_btn.setOnClickListener {
            val intent = Intent(context, GripperScreenWithoutEncodersActivity::class.java)
            startActivity(intent)
            main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 5)
        }
        gesture_6_btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                resetStateButtons()
                selectActiveGesture(6)
                main?.saveInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, 6)
                compileBLEMassage (5)
//                main?.bleCommandConnector(byteArrayOf(5), SET_GESTURE, WRITE, 13)
            }
        }
        gesture_settings_6_btn.setOnClickListener {
            val intent = Intent(context, GripperScreenWithoutEncodersActivity::class.java)
            startActivity(intent)
            main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 6)
        }
        gesture_7_btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                resetStateButtons()
                selectActiveGesture(7)
                main?.saveInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, 7)
                compileBLEMassage (6)
//                main?.bleCommandConnector(byteArrayOf(6), SET_GESTURE, WRITE, 13)
            }
        }
        gesture_settings_7_btn.setOnClickListener {
            val intent = Intent(context, GripperScreenWithoutEncodersActivity::class.java)
            startActivity(intent)
            main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 7)
        }
        gesture_8_btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                resetStateButtons()
                selectActiveGesture(8)
                main?.saveInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, 8)
                compileBLEMassage (7)
//                main?.bleCommandConnector(byteArrayOf(7), SET_GESTURE, WRITE, 13)
            }
        }
        gesture_settings_8_btn.setOnClickListener {
            val intent = Intent(context, GripperScreenWithoutEncodersActivity::class.java)
            startActivity(intent)
            main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 8)
        }
    }

    private fun compileBLEMassage (useGesture: Int) {
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_4)) {
            main?.runWriteData(byteArrayOf(useGesture.toByte()), SET_GESTURE_NEW, WRITE)
        } else {
            main?.bleCommandConnector(byteArrayOf(useGesture.toByte()), SET_GESTURE, WRITE, 13)
        }
    }

    override fun onResume() {
        super.onResume()
        gestureNameList.clear()
        loadNameGestures()
    }

    @SuppressLint("UseCompatLoadingForDrawables", "UseCompatLoadingForColorStateLists")
    fun resetStateButtons() {
        gesture_1_btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        gesture_2_btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        gesture_3_btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        gesture_4_btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        gesture_5_btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        gesture_6_btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        gesture_7_btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        gesture_8_btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        gesture_1_btn.textColor = WHITE
        gesture_2_btn.textColor = WHITE
        gesture_3_btn.textColor = WHITE
        gesture_4_btn.textColor = WHITE
        gesture_5_btn.textColor = WHITE
        gesture_6_btn.textColor = WHITE
        gesture_7_btn.textColor = WHITE
        gesture_8_btn.textColor = WHITE
        gesture_settings_1_btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        gesture_settings_2_btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        gesture_settings_3_btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        gesture_settings_4_btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        gesture_settings_5_btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        gesture_settings_6_btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        gesture_settings_7_btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        gesture_settings_8_btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
    }

    private fun loadNameGestures() {
        myLoadGesturesList()
        gesture_1_btn.text = gestureNameList[0]
        gesture_2_btn.text = gestureNameList[1]
        gesture_3_btn.text = gestureNameList[2]
        gesture_4_btn.text = gestureNameList[3]
        gesture_5_btn.text = gestureNameList[4]
        gesture_6_btn.text = gestureNameList[5]
        gesture_7_btn.text = gestureNameList[6]
        gesture_8_btn.text = gestureNameList[7]
    }


    @SuppressLint("UseCompatLoadingForDrawables", "UseCompatLoadingForColorStateLists")
    private fun selectActiveGesture(active: Int) {
        when (active) {
            1 -> { gesture_1_btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                   gesture_1_btn.textColor = resources.getColor(R.color.orange)
                   gesture_settings_1_btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            2 -> { gesture_2_btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                   gesture_2_btn.textColor = resources.getColor(R.color.orange)
                   gesture_settings_2_btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            3 -> { gesture_3_btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                   gesture_3_btn.textColor = resources.getColor(R.color.orange)
                   gesture_settings_3_btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            4 -> { gesture_4_btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                   gesture_4_btn.textColor = resources.getColor(R.color.orange)
                   gesture_settings_4_btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            5 -> { gesture_5_btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                   gesture_5_btn.textColor = resources.getColor(R.color.orange)
                   gesture_settings_5_btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            6 -> { gesture_6_btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                   gesture_6_btn.textColor = resources.getColor(R.color.orange)
                   gesture_settings_6_btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            7 -> { gesture_7_btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                   gesture_7_btn.textColor = resources.getColor(R.color.orange)
                   gesture_settings_7_btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            8 -> { gesture_8_btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                   gesture_8_btn.textColor = resources.getColor(R.color.orange)
                   gesture_settings_8_btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
        }
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {}
    override fun onNothingSelected() {}

    private fun myLoadGesturesList() {
        val text = "load not work"
        for (i in 0 until PreferenceKeys.NUM_GESTURES) {
            gestureNameList.add(mSettings!!.getString(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + i, text).toString())
        }
    }
}

