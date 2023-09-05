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
    private var startGestureInLoop = 0
    private var endGestureInLoop = 0

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


        offGesturesUIBeforeConnection()
        binding.onOffSensorGestureSwitchingSw.setOnClickListener {
            if (binding.onOffSensorGestureSwitchingSw.isChecked) {
                binding.onOffSensorGestureSwitchingTv.text = "1"
                binding.toggleGestureClasterRl.animate().alpha(1.0f).duration = 300
                binding.peakTimeVmRl.animate().alpha(1.0f).duration = 300
                binding.dividerV.animate().translationY(0F).duration = 300
                binding.gesturesButtonsSv.animate().translationY(0F).duration = 300
            } else {
                binding.onOffSensorGestureSwitchingTv.text = "0"
                binding.toggleGestureClasterRl.animate().alpha(0.0f).duration = 300
                binding.peakTimeVmRl.animate().alpha(0.0f).duration = 300
                binding.dividerV.animate().translationY(-(binding.toggleGestureClasterRl.height + binding.peakTimeVmRl.height + 16).toFloat()).duration = 300
                binding.gesturesButtonsSv.animate().translationY(-(binding.toggleGestureClasterRl.height + binding.peakTimeVmRl.height + 16).toFloat()).duration = 300
            }
        }

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

    private fun offGesturesUIBeforeConnection () {
        binding.gesture1Btn.isEnabled = false
        binding.gesture2Btn.isEnabled = false
        binding.gesture3Btn.isEnabled = false
        binding.gesture4Btn.isEnabled = false
        binding.gesture5Btn.isEnabled = false
        binding.gesture6Btn.isEnabled = false
        binding.gesture7Btn.isEnabled = false
        binding.gesture8Btn.isEnabled = false
        binding.gesture9Btn.isEnabled = false
        binding.gesture10Btn.isEnabled = false
        binding.gesture11Btn.isEnabled = false
        binding.gesture12Btn.isEnabled = false
        binding.gesture13Btn.isEnabled = false
        binding.gesture14Btn.isEnabled = false
        binding.gestureSettings2Btn.isEnabled = false
        binding.gestureSettings3Btn.isEnabled = false
        binding.gestureSettings4Btn.isEnabled = false
        binding.gestureSettings5Btn.isEnabled = false
        binding.gestureSettings6Btn.isEnabled = false
        binding.gestureSettings7Btn.isEnabled = false
        binding.gestureSettings8Btn.isEnabled = false
        binding.gestureSettings9Btn.isEnabled = false
        binding.gestureSettings10Btn.isEnabled = false
        binding.gestureSettings11Btn.isEnabled = false
        binding.gestureSettings12Btn.isEnabled = false
        binding.gestureSettings13Btn.isEnabled = false
        binding.gestureSettings14Btn.isEnabled = false
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
        binding.gesture9Btn.text = gestureNameList[8]
        binding.gesture10Btn.text = gestureNameList[9]
        binding.gesture11Btn.text = gestureNameList[10]
        binding.gesture12Btn.text = gestureNameList[11]
        binding.gesture13Btn.text = gestureNameList[12]
        binding.gesture14Btn.text = gestureNameList[13]
        binding.gestureLoop1Psv.apply {
            setSpinnerAdapter(IconSpinnerAdapter(this))
            setOnSpinnerItemSelectedListener(
                OnSpinnerItemSelectedListener<IconSpinnerItem?> {
                        oldIndex, _, newIndex, _ ->
                    startGestureInLoop = newIndex
                    selectRotationGroup(startGestureInLoop, endGestureInLoop, true)
                    if (oldIndex != newIndex) {
                        binding.gestureLoop2Psv.selectItemByIndex(endGestureInLoop)
                    }
                })
            setItems(
                arrayListOf(
                    IconSpinnerItem(text = gestureNameList[0], iconRes = hand_palm_1, gravity = 100),//iconRes = hand_palm_1,iconPadding= 1,
                    IconSpinnerItem(text = gestureNameList[1], iconRes = hand_palm_2, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[2], iconRes = hand_palm_3, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[3], iconRes = hand_palm_4, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[4], iconRes = hand_palm_5, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[5], iconRes = hand_palm_6, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[6], iconRes = hand_palm_7, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[7], iconRes = hand_palm_8, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[8], iconRes = hand_palm_9, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[9], iconRes = hand_palm_10, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[10], iconRes = hand_palm_11, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[11], iconRes = hand_palm_12, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[12], iconRes = hand_palm_13, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[13], iconRes = hand_palm_14, gravity = 100)))
            showDivider = true
            dividerSize = 2
            lifecycleOwner = this@GestureFragment
        }
        binding.gestureLoop2Psv.apply {
            setSpinnerAdapter(IconSpinnerAdapter(this))
            setOnSpinnerItemSelectedListener(
                OnSpinnerItemSelectedListener<IconSpinnerItem?> {
                        oldIndex, _, newIndex, _ ->
                    endGestureInLoop = newIndex
                    selectRotationGroup(startGestureInLoop, endGestureInLoop, false)
                    if (oldIndex != newIndex) {
                        binding.gestureLoop1Psv.selectItemByIndex(startGestureInLoop)
                    }
                })
            setItems(
                arrayListOf(
                    IconSpinnerItem(text = gestureNameList[0], iconRes = hand_palm_1, gravity = 100),//iconRes = hand_palm_1,iconPadding= 1,
                    IconSpinnerItem(text = gestureNameList[1], iconRes = hand_palm_2, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[2], iconRes = hand_palm_3, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[3], iconRes = hand_palm_4, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[4], iconRes = hand_palm_5, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[5], iconRes = hand_palm_6, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[6], iconRes = hand_palm_7, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[7], iconRes = hand_palm_8, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[8], iconRes = hand_palm_9, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[9], iconRes = hand_palm_10, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[10], iconRes = hand_palm_11, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[11], iconRes = hand_palm_12, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[12], iconRes = hand_palm_13, gravity = 100),
                    IconSpinnerItem(text = gestureNameList[13], iconRes = hand_palm_14, gravity = 100)))
            showDivider = true
            dividerSize = 2
            lifecycleOwner = this@GestureFragment
        }
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
            9 -> { binding.gesture9Btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                binding.gesture9Btn.textColor = resources.getColor(R.color.orange)
                binding.gestureSettings9Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop9Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            10 -> { binding.gesture10Btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                binding.gesture10Btn.textColor = resources.getColor(R.color.orange)
                binding.gestureSettings10Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop10Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            11 -> { binding.gesture11Btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                binding.gesture11Btn.textColor = resources.getColor(R.color.orange)
                binding.gestureSettings11Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop11Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            12 -> { binding.gesture12Btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                binding.gesture12Btn.textColor = resources.getColor(R.color.orange)
                binding.gestureSettings12Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop12Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            13 -> { binding.gesture13Btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                binding.gesture13Btn.textColor = resources.getColor(R.color.orange)
                binding.gestureSettings13Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop13Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            14 -> { binding.gesture14Btn.backgroundDrawable = resources.getDrawable(custom_button_le_selected)
                binding.gesture14Btn.textColor = resources.getColor(R.color.orange)
                binding.gestureSettings14Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop14Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
        }
        main?.saveInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, active)
    }
    private fun selectRotationGroup(startGestureInLoop: Int, endGestureInLoop: Int, changeStartGestureInLoop: Boolean){
        //блок проверки количества жестов в цикле ротации и подгонка верхней или нижней границы
        if (endGestureInLoop - startGestureInLoop > 3) {
            main?.showToast(
                context?.resources?.getText(R.string.the_number_of_gestures_per_cycle_should_not_exceed_4)
                    .toString()
            )
            if (changeStartGestureInLoop) {
                this.endGestureInLoop = startGestureInLoop + 3
                main?.showToast(
                    context?.resources?.getText(R.string.the_ending_gesture_of_the_cycle_was_changed_to)
                        .toString() + " " + gestureNameList[this.startGestureInLoop]
                )
            } else {
                this.startGestureInLoop = endGestureInLoop - 3
                main?.showToast(
                    context?.resources?.getText(R.string.the_starting_gesture_of_the_cycle_was_changed_to)
                        .toString() + " " + gestureNameList[this.startGestureInLoop]
                )
            }
        }


        //блок отрисовки картинок цикла на нужных кнопках
        val indicatorGestureLoop = arrayOf(
            binding.gestureLoop1Iv,
            binding.gestureLoop2Iv,
            binding.gestureLoop3Iv,
            binding.gestureLoop4Iv,
            binding.gestureLoop5Iv,
            binding.gestureLoop6Iv,
            binding.gestureLoop7Iv,
            binding.gestureLoop8Iv,
            binding.gestureLoop9Iv,
            binding.gestureLoop10Iv,
            binding.gestureLoop11Iv,
            binding.gestureLoop12Iv,
            binding.gestureLoop13Iv,
            binding.gestureLoop14Iv)
        for (i in 0 until 14) {
            indicatorGestureLoop[i].visibility = View.GONE
        }
        for (i in startGestureInLoop until endGestureInLoop+1) {
            indicatorGestureLoop[i].visibility = View.VISIBLE
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
            R.id.gesture_settings_9_btn -> {
                main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 9)
            }
            R.id.gesture_settings_10_btn -> {
                main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 10)
            }
            R.id.gesture_settings_11_btn -> {
                main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 11)
            }
            R.id.gesture_settings_12_btn -> {
                main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 12)
            }
            R.id.gesture_settings_13_btn -> {
                main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 13)
            }
            R.id.gesture_settings_14_btn -> {
                main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 14)
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

