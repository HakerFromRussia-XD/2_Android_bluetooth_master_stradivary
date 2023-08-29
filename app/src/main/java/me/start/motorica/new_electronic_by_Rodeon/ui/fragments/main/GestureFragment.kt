package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.skydoves.powerspinner.IconSpinnerAdapter
import com.skydoves.powerspinner.IconSpinnerItem
import com.skydoves.powerspinner.OnSpinnerItemSelectedListener
import io.reactivex.android.schedulers.AndroidSchedulers
import me.start.motorica.R
import me.start.motorica.R.drawable.*
import me.start.motorica.databinding.LayoutGesturesBinding
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager
import me.start.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.*
import me.start.motorica.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.gripper.with_encoders.GripperScreenWithEncodersActivity
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.gripper.without_encoders.GripperScreenWithoutEncodersActivity
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import org.jetbrains.anko.backgroundDrawable
import org.jetbrains.anko.textColor


@Suppress("DEPRECATION")
class GestureFragment: Fragment(), OnChartValueSelectedListener, View.OnClickListener {

    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null
    private var gestureNameList =  ArrayList<String>()
    private var testThreadFlag = true

    private lateinit var binding: LayoutGesturesBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = LayoutGesturesBinding.inflate(layoutInflater)
        if (activity != null) { main = activity as MainActivity? }

        return binding.root
    }
    @Deprecated("Deprecated in Java")
    @SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables", "UseCompatLoadingForColorStateLists")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        main?.offGesturesUIBeforeConnection()

        val startGesture = 6

        binding.gestureLoop1Psv.apply {
            setSpinnerAdapter(IconSpinnerAdapter(this))
            setItems(
                arrayListOf(
                    IconSpinnerItem(text = "It1", iconRes = hand_palm_1, gravity = 100),//iconRes = hand_palm_1,iconPadding= 1,
                    IconSpinnerItem(text = "Item67654321", iconRes = hand_palm_2, gravity = 100),
                    IconSpinnerItem(text = "It21", iconRes = hand_palm_3, gravity = 100),
                    IconSpinnerItem(text = "Item67654321", iconRes = hand_palm_4, gravity = 100),
                    IconSpinnerItem(text = "It321", iconRes = hand_palm_5, gravity = 100),
                    IconSpinnerItem(text = "Item67654321", iconRes = hand_palm_6, gravity = 100),
                    IconSpinnerItem(text = "I", iconRes = hand_palm_7, gravity = 100),
                    IconSpinnerItem(text = "Item67654321", iconRes = hand_palm_8, gravity = 100),
                    IconSpinnerItem(text = "It54321", iconRes = hand_palm_9, gravity = 100),
                    IconSpinnerItem(text = "Item67654321", iconRes = hand_palm_10, gravity = 100),
                    IconSpinnerItem(text = "Item67654321", iconRes = hand_palm_11, gravity = 100),
                    IconSpinnerItem(text = "Item67654321", iconRes = hand_palm_12, gravity = 100),
                    IconSpinnerItem(text = "Item67654321", iconRes = hand_palm_13, gravity = 100),
                    IconSpinnerItem(text = "Item67654321", iconRes = hand_palm_14, gravity = 100)))

            showDivider = true
            dividerSize = 2
            selectItemByIndex(startGesture)
            lifecycleOwner = this@GestureFragment
        }
        binding.gestureLoop1Psv.setOnSpinnerItemSelectedListener(
            OnSpinnerItemSelectedListener<IconSpinnerItem?> { oldIndex, _, newIndex, _ ->
                System.err.println("1 Psv    $newIndex selected!") })

        binding.gestureLoop2Psv.apply {
            setSpinnerAdapter(IconSpinnerAdapter(this))
            setItems(
                arrayListOf(
                    IconSpinnerItem(text = "It1", iconRes = hand_palm_1, gravity = 100),//iconRes = hand_palm_1,iconPadding= 1,
                    IconSpinnerItem(text = "Item67654321", iconRes = hand_palm_2, gravity = 100),
                    IconSpinnerItem(text = "It21", iconRes = hand_palm_3, gravity = 100),
                    IconSpinnerItem(text = "Item67654321", iconRes = hand_palm_4, gravity = 100),
                    IconSpinnerItem(text = "It321", iconRes = hand_palm_5, gravity = 100),
                    IconSpinnerItem(text = "Item67654321", iconRes = hand_palm_6, gravity = 100),
                    IconSpinnerItem(text = "It4321", iconRes = hand_palm_7, gravity = 100),
                    IconSpinnerItem(text = "Item67654321", iconRes = hand_palm_8, gravity = 100),
                    IconSpinnerItem(text = "It54321", iconRes = hand_palm_9, gravity = 100),
                    IconSpinnerItem(text = "Item67654321", iconRes = hand_palm_10, gravity = 100),
                    IconSpinnerItem(text = "Item67654321", iconRes = hand_palm_11, gravity = 100),
                    IconSpinnerItem(text = "Item67654321", iconRes = hand_palm_12, gravity = 100),
                    IconSpinnerItem(text = "Item67654321", iconRes = hand_palm_13, gravity = 100),
                    IconSpinnerItem(text = "Item67654321", iconRes = hand_palm_14, gravity = 100)))

            showDivider = true
            dividerSize = 2
            selectItemByIndex(startGesture+4)
            lifecycleOwner = this@GestureFragment
        }
        binding.gestureLoop2Psv.setOnSpinnerItemSelectedListener(
            OnSpinnerItemSelectedListener<IconSpinnerItem?> { oldIndex, _, newIndex, _ ->
                System.err.println("2 Psv    $newIndex selected!") })



        binding.gestureSettings1Btn.setOnClickListener(this)
        binding.gestureSettings2Btn.setOnClickListener(this)
        binding.gestureSettings3Btn.setOnClickListener(this)
        binding.gestureSettings4Btn.setOnClickListener(this)
        binding.gestureSettings5Btn.setOnClickListener(this)
        binding.gestureSettings6Btn.setOnClickListener(this)
        binding.gestureSettings7Btn.setOnClickListener(this)
        binding.gestureSettings8Btn.setOnClickListener(this)
        binding.gesture1Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(1)
                compileBLEMassage (0)
            }
        }
        binding.gesture2Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(2)
                compileBLEMassage (1)
            }
        }
        binding.gesture3Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(3)
                compileBLEMassage (2)
            }
        }
        binding.gesture4Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(4)
                compileBLEMassage (3)
            }
        }
        binding.gesture5Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(5)
                compileBLEMassage (4)
            }
        }
        binding.gesture6Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(6)
                compileBLEMassage (5)
            }
        }
        binding.gesture7Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(7)
                compileBLEMassage (6)
            }
        }
        binding.gesture8Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(8)
                compileBLEMassage (7)
            }
        }
    }
    @SuppressLint("CheckResult")
    override fun onResume() {
        super.onResume()
        gestureNameList.clear()
        loadNameGestures()
        testThreadFlag = true
        RxUpdateMainEvent.getInstance().uiGestures
            .compose(main?.bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                selectActiveGesture(it)
            }

        //включение работы протеза от датчиков
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            main?.runWriteData(byteArrayOf(0x01.toByte()), SENS_ENABLED_NEW, WRITE)
        }
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "gesture activity 1"
            main?.runSendCommand(byteArrayOf(0x01.toByte()), SENS_ENABLED_NEW_VM, 50)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        testThreadFlag = false
    }

    private fun compileBLEMassage (useGesture: Int) {
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "gesture activity 2"
            main?.runSendCommand(byteArrayOf(useGesture.toByte()), SET_GESTURE_NEW_VM, 50)
        } else {
            if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
                main?.runWriteData(byteArrayOf(useGesture.toByte()), SET_GESTURE_NEW, WRITE)
            } else {
                main?.bleCommandConnector(byteArrayOf(useGesture.toByte()), SET_GESTURE, WRITE, 13)
            }
        }
    }
    @SuppressLint("UseCompatLoadingForDrawables", "UseCompatLoadingForColorStateLists")
    fun resetStateButtons() {
        binding.gesture1Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture2Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture3Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture4Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture5Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture6Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture7Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture8Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture1Btn.textColor = WHITE
        binding.gesture2Btn.textColor = WHITE
        binding.gesture3Btn.textColor = WHITE
        binding.gesture4Btn.textColor = WHITE
        binding.gesture5Btn.textColor = WHITE
        binding.gesture6Btn.textColor = WHITE
        binding.gesture7Btn.textColor = WHITE
        binding.gesture8Btn.textColor = WHITE
        binding.gestureSettings1Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings2Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings3Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings4Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings5Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings6Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings7Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings8Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop1Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop2Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop3Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop4Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop5Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop6Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop7Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop8Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
    }
    private fun loadNameGestures() {
        myLoadGesturesList()
        binding.gesture1Btn.text = gestureNameList[0]
        binding.gesture2Btn.text = gestureNameList[1]
        binding.gesture3Btn.text = gestureNameList[2]
        binding.gesture4Btn.text = gestureNameList[3]
        binding.gesture5Btn.text = gestureNameList[4]
        binding.gesture6Btn.text = gestureNameList[5]
        binding.gesture7Btn.text = gestureNameList[6]
        binding.gesture8Btn.text = gestureNameList[7]
    }
    @SuppressLint("UseCompatLoadingForDrawables", "UseCompatLoadingForColorStateLists")
    private fun selectActiveGesture(active: Int) {
        resetStateButtons()
        when (active) {
            1 -> { binding.gesture1Btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                binding.gesture1Btn.textColor = resources.getColor(R.color.orange)
                binding.gestureSettings1Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop1Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            2 -> { binding.gesture2Btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                binding.gesture2Btn.textColor = resources.getColor(R.color.orange)
                binding.gestureSettings2Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop2Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            3 -> { binding.gesture3Btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                binding.gesture3Btn.textColor = resources.getColor(R.color.orange)
                binding.gestureSettings3Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop3Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            4 -> { binding.gesture4Btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                binding.gesture4Btn.textColor = resources.getColor(R.color.orange)
                binding.gestureSettings4Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop4Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            5 -> { binding.gesture5Btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                binding.gesture5Btn.textColor = resources.getColor(R.color.orange)
                binding.gestureSettings5Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop5Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            6 -> { binding.gesture6Btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                binding.gesture6Btn.textColor = resources.getColor(R.color.orange)
                binding.gestureSettings6Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop6Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            7 -> { binding.gesture7Btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                binding.gesture7Btn.textColor = resources.getColor(R.color.orange)
                binding.gestureSettings7Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop7Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            8 -> { binding.gesture8Btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                binding.gesture8Btn.textColor = resources.getColor(R.color.orange)
                binding.gestureSettings8Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop8Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
        }
        main?.saveInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, active)
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
            main?.stage = "gesture activity 3"
            main?.runSendCommand(byteArrayOf(0x00.toByte()), SENS_ENABLED_NEW_VM, 50)
        }
    }
    override fun onValueSelected(e: Entry?, h: Highlight?) {}
    override fun onNothingSelected() {}

    private fun myLoadGesturesList() {
        val text = "load not work"
        val macKey = mSettings!!.getString(PreferenceKeys.LAST_CONNECTION_MAC, text)

        for (i in 0 until PreferenceKeys.NUM_GESTURES) {
            System.err.println("9 LAST_CONNECTION_MAC: "+PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + macKey + i)
            gestureNameList.add(mSettings!!.getString(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + macKey + i, text).toString())
        }
    }
}

