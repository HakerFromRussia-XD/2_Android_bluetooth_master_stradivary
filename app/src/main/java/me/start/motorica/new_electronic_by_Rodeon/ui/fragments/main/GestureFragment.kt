package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color.*
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.layout_gestures.*
import me.start.motorica.R
import me.start.motorica.R.drawable.*
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager
import me.start.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.*
import me.start.motorica.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceManager
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.gripper.with_encoders.GripperScreenWithEncodersActivity
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.gripper.without_encoders.GripperScreenWithoutEncodersActivity
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import org.jetbrains.anko.backgroundDrawable
import org.jetbrains.anko.textColor
import javax.inject.Inject

@Suppress("DEPRECATION")
class GestureFragment: Fragment(), OnChartValueSelectedListener, View.OnClickListener {
    @Inject
    lateinit var preferenceManager: PreferenceManager

    private var rootView: View? = null
    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null
    private var gestureNameList =  ArrayList<String>()
    private var testThreadFlag = true
    private var updatingUIThread: Thread? = null

    @SuppressLint("CheckResult")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.layout_gestures, container, false)
        WDApplication.component.inject(this)
        this.rootView = rootView
        if (activity != null) { main = activity as MainActivity? }

        RxUpdateMainEvent.getInstance().uiGestures
            .compose(main?.bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                selectActiveGesture(it)
            }
        return rootView
    }

    @SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables", "UseCompatLoadingForColorStateLists")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        main?.offGesturesUIBeforeConnection()
        Handler().postDelayed({
            startUpdatingUIThread()
        }, 500)



        gesture_settings_1_btn.setOnClickListener(this)
        gesture_settings_2_btn.setOnClickListener(this)
        gesture_settings_3_btn.setOnClickListener(this)
        gesture_settings_4_btn.setOnClickListener(this)
        gesture_settings_5_btn.setOnClickListener(this)
        gesture_settings_6_btn.setOnClickListener(this)
        gesture_settings_7_btn.setOnClickListener(this)
        gesture_settings_8_btn.setOnClickListener(this)
        gesture_1_btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(1)
                compileBLEMassage (0)
            }
        }
        gesture_2_btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(2)
                compileBLEMassage (1)
            }
        }
        gesture_3_btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(3)
                compileBLEMassage (2)
            }
        }
        gesture_4_btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(4)
                compileBLEMassage (3)
            }
        }
        gesture_5_btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(5)
                compileBLEMassage (4)
            }
        }
        gesture_6_btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(6)
                compileBLEMassage (5)
            }
        }
        gesture_7_btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(7)
                compileBLEMassage (6)
            }
        }
        gesture_8_btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(8)
                compileBLEMassage (7)
            }
        }

    }

    override fun onClick(v: View?) {
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H) ||
            main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            val intent = Intent(context, GripperScreenWithEncodersActivity::class.java)
            startActivity(intent)
        } else {
            val intent = Intent(context, GripperScreenWithoutEncodersActivity::class.java)
            startActivity(intent)
        }
        when (v?.id) {
            R.id.gesture_settings_1_btn -> {
                main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 1)
            }
            R.id.gesture_settings_2_btn -> {
                main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 2)
            }
            R.id.gesture_settings_3_btn -> {
                main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 3)
            }
            R.id.gesture_settings_4_btn -> {
                main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 4)
            }
            R.id.gesture_settings_5_btn -> {
                main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 5)
            }
            R.id.gesture_settings_6_btn -> {
                main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 6)
            }
            R.id.gesture_settings_7_btn -> {
                main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 7)
            }
            R.id.gesture_settings_8_btn -> {
                main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 8)
            }
        }

        //выключение работы протеза от датчиков
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            main?.runWriteData(byteArrayOf(0x00.toByte()), SENS_ENABLED_NEW, WRITE)
        }
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.runWriteData(byteArrayOf(0x00.toByte()), SENS_ENABLED_NEW_VM, WRITE)
        }
    }

    private fun compileBLEMassage (useGesture: Int) {
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.runWriteData(byteArrayOf(useGesture.toByte()), SET_GESTURE_NEW_VM, WRITE)
        } else {
            if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
                main?.runWriteData(byteArrayOf(useGesture.toByte()), SET_GESTURE_NEW, WRITE)
            } else {
                main?.bleCommandConnector(byteArrayOf(useGesture.toByte()), SET_GESTURE, WRITE, 13)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        gestureNameList.clear()
        loadNameGestures()
        testThreadFlag = true

        //включение работы протеза от датчиков
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            main?.runWriteData(byteArrayOf(0x01.toByte()), SENS_ENABLED_NEW, WRITE)
        }
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.runWriteData(byteArrayOf(0x01.toByte()), SENS_ENABLED_NEW_VM, WRITE)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        testThreadFlag = false
    }


    @SuppressLint("UseCompatLoadingForDrawables", "UseCompatLoadingForColorStateLists")
    fun resetStateButtons() {
        gesture_1_btn?.backgroundDrawable = resources.getDrawable(custom_button_le)
        gesture_2_btn?.backgroundDrawable = resources.getDrawable(custom_button_le)
        gesture_3_btn?.backgroundDrawable = resources.getDrawable(custom_button_le)
        gesture_4_btn?.backgroundDrawable = resources.getDrawable(custom_button_le)
        gesture_5_btn?.backgroundDrawable = resources.getDrawable(custom_button_le)
        gesture_6_btn?.backgroundDrawable = resources.getDrawable(custom_button_le)
        gesture_7_btn?.backgroundDrawable = resources.getDrawable(custom_button_le)
        gesture_8_btn?.backgroundDrawable = resources.getDrawable(custom_button_le)
        gesture_1_btn?.textColor = WHITE
        gesture_2_btn?.textColor = WHITE
        gesture_3_btn?.textColor = WHITE
        gesture_4_btn?.textColor = WHITE
        gesture_5_btn?.textColor = WHITE
        gesture_6_btn?.textColor = WHITE
        gesture_7_btn?.textColor = WHITE
        gesture_8_btn?.textColor = WHITE
        gesture_settings_1_btn?.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        gesture_settings_2_btn?.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        gesture_settings_3_btn?.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        gesture_settings_4_btn?.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        gesture_settings_5_btn?.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        gesture_settings_6_btn?.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        gesture_settings_7_btn?.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        gesture_settings_8_btn?.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
    }

    private fun loadNameGestures() {
        myLoadGesturesList()
        gesture_1_btn?.text = gestureNameList[0]
        gesture_2_btn?.text = gestureNameList[1]
        gesture_3_btn?.text = gestureNameList[2]
        gesture_4_btn?.text = gestureNameList[3]
        gesture_5_btn?.text = gestureNameList[4]
        gesture_6_btn?.text = gestureNameList[5]
        gesture_7_btn?.text = gestureNameList[6]
        gesture_8_btn?.text = gestureNameList[7]
    }

    @SuppressLint("UseCompatLoadingForDrawables", "UseCompatLoadingForColorStateLists")
    private fun selectActiveGesture(active: Int) {
        resetStateButtons()
        when (active) {
            1 -> { gesture_1_btn?.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                   gesture_1_btn?.textColor = resources.getColor(R.color.orange)
                   gesture_settings_1_btn?.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            2 -> { gesture_2_btn?.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                   gesture_2_btn?.textColor = resources.getColor(R.color.orange)
                   gesture_settings_2_btn?.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            3 -> { gesture_3_btn?.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                   gesture_3_btn?.textColor = resources.getColor(R.color.orange)
                   gesture_settings_3_btn?.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            4 -> { gesture_4_btn?.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                   gesture_4_btn?.textColor = resources.getColor(R.color.orange)
                   gesture_settings_4_btn?.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            5 -> { gesture_5_btn?.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                   gesture_5_btn?.textColor = resources.getColor(R.color.orange)
                   gesture_settings_5_btn?.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            6 -> { gesture_6_btn?.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                   gesture_6_btn?.textColor = resources.getColor(R.color.orange)
                   gesture_settings_6_btn?.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            7 -> { gesture_7_btn?.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                   gesture_7_btn?.textColor = resources.getColor(R.color.orange)
                   gesture_settings_7_btn?.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            8 -> { gesture_8_btn?.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                   gesture_8_btn?.textColor = resources.getColor(R.color.orange)
                   gesture_settings_8_btn?.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
        }
        main?.saveInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, active)
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {}
    override fun onNothingSelected() {}

    private fun myLoadGesturesList() {
        val text = "load not work"
        for (i in 0 until PreferenceKeys.NUM_GESTURES) {
            gestureNameList.add(mSettings!!.getString(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + i, text).toString())
        }
    }

    private fun startUpdatingUIThread() {
        updatingUIThread =  Thread {
            while (testThreadFlag) {
                main?.runOnUiThread {
                    resetStateButtons()
                    selectActiveGesture(mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, 1))
//                    testThreadFlag = false //выключаем поток повторного запроса информации после её получения и обновления на экране
                }
                try {
                    Thread.sleep(1000)
                } catch (ignored: Exception) { }
            }
        }
        updatingUIThread?.start()
    }
}

