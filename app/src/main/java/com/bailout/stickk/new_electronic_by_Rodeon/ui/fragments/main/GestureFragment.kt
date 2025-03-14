package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.main

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color.WHITE
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.bailout.stickk.R
import com.bailout.stickk.R.drawable.custom_button_le
import com.bailout.stickk.R.drawable.custom_button_le_selected
import com.bailout.stickk.R.drawable.hand_palm_15
import com.bailout.stickk.R.drawable.hand_palm_1
import com.bailout.stickk.R.drawable.hand_palm_10
import com.bailout.stickk.R.drawable.hand_palm_11
import com.bailout.stickk.R.drawable.hand_palm_12
import com.bailout.stickk.R.drawable.hand_palm_13
import com.bailout.stickk.R.drawable.hand_palm_14
import com.bailout.stickk.R.drawable.*
import com.bailout.stickk.R.drawable.hand_palm_2
import com.bailout.stickk.R.drawable.hand_palm_3
import com.bailout.stickk.R.drawable.hand_palm_4
import com.bailout.stickk.R.drawable.hand_palm_5
import com.bailout.stickk.R.drawable.hand_palm_6
import com.bailout.stickk.R.drawable.hand_palm_7
import com.bailout.stickk.R.drawable.hand_palm_8
import com.bailout.stickk.R.drawable.hand_palm_9
import com.bailout.stickk.databinding.LayoutGesturesBinding
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes.ROTATION_GESTURE_NEW_VM
import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes.SENS_ENABLED_NEW
import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes.SENS_ENABLED_NEW_VM
import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes.SET_GESTURE
import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes.SET_GESTURE_NEW
import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes.SET_GESTURE_NEW_VM
import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys.NUM_GESTURES
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.gripper.with_encoders.GripperScreenWithEncodersActivity
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.gripper.without_encoders.GripperScreenWithoutEncodersActivity
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.skydoves.powerspinner.IconSpinnerAdapter
import com.skydoves.powerspinner.IconSpinnerItem
import io.reactivex.android.schedulers.AndroidSchedulers

//import org.jetbrains.anko.backgroundDrawable
//import org.jetbrains.anko.textColor


@Suppress("DEPRECATION")
class GestureFragment: Fragment(), OnChartValueSelectedListener, View.OnClickListener{
    private var sendFlag: Boolean = false

    private var main: MainActivity? = null
    private var mSettings: SharedPreferences? = null
    private var gestureNameList =  ArrayList<String>()
    private val handPalms = arrayOf(
        hand_palm_1,
        hand_palm_2,
        hand_palm_3,
        hand_palm_4,
        hand_palm_5,
        hand_palm_6,
        hand_palm_7,
        hand_palm_8,
        hand_palm_9,
        hand_palm_10,
        hand_palm_11,
        hand_palm_12,
        hand_palm_13,
        hand_palm_14,
        hand_palm_15,
        hand_palm_16,
        hand_palm_17,
        hand_palm_18,
        hand_palm_19,
        hand_palm_20,
        hand_palm_21,
        hand_palm_22,
        hand_palm_23,
        hand_palm_24,
        hand_palm_25,
    )
    private var testThreadFlag = true
    private var activeGestures = 8  //  NUM_ACTIVE_GESTURES
    private var startGestureInLoopNum = 0
    private var endGestureInLoopNum = 0
    private var peakTimeVmNum = 0

    private var sensorGestureSwitching = 0
    private var lockProstheses = 0
    private var holdToLockTimeSb = 0
    private var firstStart = true
    private val countRestart = 3

    private lateinit var binding: LayoutGesturesBinding

    private val gesturesButtonsSVLocation = IntArray(2)
    private val relativeLayout88Location = IntArray(2)
    private var density = 0f
    private var distance = 0
    private val heightActiveRotationGroupWithSwichDp = 222
    private val heightActiveRotationGroupDp = 166
    private var heightActiveRotationGroupPx = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = LayoutGesturesBinding.inflate(layoutInflater)
        if (activity != null) { main = activity as MainActivity? }

        main?.startScrollForGesturesFragment = {
            System.err.println("GestureFragment startScroll")
            binding.gestureLoop1Psv.dismiss()
            binding.gestureLoop2Psv.dismiss()
        }

        return binding.root
    }
    @Deprecated("Deprecated in Java")
    @SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables", "UseCompatLoadingForColorStateLists")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
        density = requireContext().resources.displayMetrics.density
        heightActiveRotationGroupPx = (heightActiveRotationGroupDp * density + 0.5f).toInt()
//        // Получаем координаты
//        binding.gesturesButtonsSv.getLocationOnScreen(gesturesButtonsSVLocation)
//        binding.relativeLayout88.getLocationOnScreen(relativeLayout88Location)
//        // Вычисляем расстояние между верхними границами
//        distance = relativeLayout88Location[1] - gesturesButtonsSVLocation[1]
//        Log.d("Distance", "distance = $distance")

//        binding.gesturesButtonsSv.layoutParams.height = distance

        onOffUIAll(false)

        // 166dp это высота группы ротации + слайдера управления временем + девайдер

        binding.onOffSensorGestureSwitchingSw.setOnClickListener {
            // Получаем координаты
            binding.gesturesButtonsSv.getLocationOnScreen(gesturesButtonsSVLocation)
            binding.relativeLayout88.getLocationOnScreen(relativeLayout88Location)
            // Вычисляем расстояние между верхними границами
            distance = relativeLayout88Location[1] - gesturesButtonsSVLocation[1]

            if (binding.onOffSensorGestureSwitchingSw.isChecked) {
                sensorGestureSwitching = 0x01
                binding.onOffSensorGestureSwitchingTv.text = resources.getString(R.string.on_sw)
                binding.toggleGestureClasterRl.animate().alpha(1.0f).duration = 300
                binding.peakTimeVmRl.animate().alpha(1.0f).duration = 300
                binding.dividerV.animate().translationY(0F).duration = 300
                binding.gesturesButtonsSv.animate().translationY(0F).duration = 300
                selectRotationGroup(startGestureInLoopNum, endGestureInLoopNum, true)
                binding.gesturesButtonsSv.layoutParams.height = distance - heightActiveRotationGroupPx
            } else {
                sensorGestureSwitching = 0x00
                binding.onOffSensorGestureSwitchingTv.text = resources.getString(R.string.off_sw)
                binding.toggleGestureClasterRl.animate().alpha(0.0f).duration = 300
                binding.peakTimeVmRl.animate().alpha(0.0f).duration = 300
                binding.dividerV.animate().translationY(-(binding.toggleGestureClasterRl.height + binding.peakTimeVmRl.height + 16).toFloat()).duration = 300
                binding.gesturesButtonsSv.animate().translationY(-(binding.toggleGestureClasterRl.height + binding.peakTimeVmRl.height + 16).toFloat()).duration = 300
                offAllRotationImage()
                binding.gesturesButtonsSv.layoutParams.height = distance
            }
            if (useNewSystemSendCommand()) {
                main?.runSendCommand(
                    byteArrayOf(
                        sensorGestureSwitching.toByte(),
                        0.toByte(),
                        binding.peakTimeVmSb.progress.toByte(),
                        0.toByte(),
                        lockProstheses.toByte(),
                        holdToLockTimeSb.toByte(),
                        startGestureInLoopNum.toByte(),
                        endGestureInLoopNum.toByte()
                    ), ROTATION_GESTURE_NEW_VM, countRestart
                )
            }
            main?.saveBool(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, binding.onOffSensorGestureSwitchingSw.isChecked)
            RxUpdateMainEvent.getInstance().updateUIAdvancedSettings(true)
            RxUpdateMainEvent.getInstance().updateUIChart(true)
        }
        binding.peakTimeVmSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val time: String = when {
                    ((seekBar.progress + 1) * 0.1).toString().length == 4 -> {
                        ((seekBar.progress + 1) * 0.1).toString() + "c"
                    }
                    ((seekBar.progress + 1) * 0.1).toString().length > 4 -> {
                        ((seekBar.progress + 1) * 0.1).toString().substring(0,4) + "c"
                    }
                    else -> {
                        ((seekBar.progress + 1) * 0.1).toString() + "0c"
                    }
                }
                binding.peakTimeVmTv.text = time
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val time: String = when {
                    ((seekBar.progress + 1) * 0.1).toString().length == 4 -> {
                        ((seekBar.progress + 1) * 0.1).toString() + "c"
                    }
                    ((seekBar.progress + 1) * 0.1).toString().length > 4 -> {
                        ((seekBar.progress + 1) * 0.1).toString().substring(0,4) + "c"
                    }
                    else -> {
                        ((seekBar.progress + 1) * 0.1).toString() + "0c"
                    }
                }
                binding.peakTimeVmTv.text = time

                if (useNewSystemSendCommand()) {
                    main?.runSendCommand(
                        byteArrayOf(
                            sensorGestureSwitching.toByte(),
                            0.toByte(),
                            binding.peakTimeVmSb.progress.toByte(),
                            0.toByte(),
                            lockProstheses.toByte(),
                            holdToLockTimeSb.toByte(),
                            startGestureInLoopNum.toByte(),
                            endGestureInLoopNum.toByte()
                        ), ROTATION_GESTURE_NEW_VM, countRestart
                    )
                }
                RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(ROTATION_GESTURE_NEW_VM)

                main?.saveInt(main?.mDeviceAddress + PreferenceKeys.SET_PEAK_TIME_VM_NUM, seekBar.progress)
            }
        })


        binding.gestureSettings5Btn.setOnClickListener(this)
        binding.gestureSettings6Btn.setOnClickListener(this)
        binding.gestureSettings7Btn.setOnClickListener(this)
        binding.gestureSettings8Btn.setOnClickListener(this)
        binding.gestureSettings9Btn.setOnClickListener(this)
        binding.gestureSettings10Btn.setOnClickListener(this)
        binding.gestureSettings11Btn.setOnClickListener(this)
        binding.gestureSettings12Btn.setOnClickListener(this)
        binding.gestureSettings13Btn.setOnClickListener(this)
        binding.gestureSettings14Btn.setOnClickListener(this)
        binding.gestureSettings15Btn.setOnClickListener(this)
        binding.gestureSettings16Btn.setOnClickListener(this)
        binding.gestureSettings17Btn.setOnClickListener(this)
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
        binding.gesture9Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(9)
                compileBLEMassage (8)
            }
        }
        binding.gesture10Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(10)
                compileBLEMassage (9)
            }
        }
        binding.gesture11Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(11)
                compileBLEMassage (10)
            }
        }
        binding.gesture12Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(12)
                compileBLEMassage (11)
            }
        }
        binding.gesture13Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(13)
                compileBLEMassage (12)
            }
        }
        binding.gesture14Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(14)
                compileBLEMassage (13)
            }
        }
        binding.gesture15Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(15)
                compileBLEMassage (14)
            }
        }
        binding.gesture16Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(16)
                compileBLEMassage (15)
            }
        }
        binding.gesture17Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(17)
                compileBLEMassage (16)
            }
        }
        binding.gesture18Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(18)
                compileBLEMassage (17)
            }
        }
        binding.gesture19Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(19)
                compileBLEMassage (18)
            }
        }
        binding.gesture20Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(20)
                compileBLEMassage (19)
            }
        }
        binding.gesture21Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(21)
                compileBLEMassage (20)
            }
        }
        binding.gesture22Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(22)
                compileBLEMassage (21)
            }
        }
        binding.gesture23Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(23)
                compileBLEMassage (22)
            }
        }
        binding.gesture24Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(24)
                compileBLEMassage (23)
            }
        }
        binding.gesture25Btn.setOnClickListener {
            if (!main?.lockWriteBeforeFirstRead!!) {
                selectActiveGesture(25)
                compileBLEMassage (24)
            }
        }

        binding.gesturesResetBtn.setOnClickListener { main?.showGestureResetDialog() }

        //скрываем интерфейс управления группами ротации
        if (!main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            hideUIRotationGroup(false)
        }
    }

    override fun onResume() {
        super.onResume()
        gestureNameList.clear()
        loadAllVariables()
        Log.d("Distance", "loadAllVariables 0")
        setNameGesturesAndRotationGroup(activeGestures)
        testThreadFlag = true
        RxUpdateMainEvent.getInstance().uiGestures
            .compose(main?.bindToLifecycle())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (context != null) {
                    //скрываем интерфейс управления группами ротации
                    hideUIRotationGroup(checkDriverVersionGreaterThan237())
                    if (it < 100) {
                        //передаёт номер активного жеста
                        loadAllVariables()
                        Log.d("Distance", "loadAllVariables 1")
                        selectActiveGesture(it)
                        selectRotationGroup(startGestureInLoopNum, endGestureInLoopNum, true)
                        //тут нет onOffUIAll потому что
                        onOffUIAll(true)
                    }
                    if (it == 100) {
                        //активирует интерфейс
                        loadAllVariables()
                        Log.d("Distance", "loadAllVariables 2")
                        System.err.println("RxUpdateMainEvent selectRotationGroup startGestureInLoop=$startGestureInLoopNum  endGestureInLoop=$endGestureInLoopNum")
                        selectRotationGroup(startGestureInLoopNum, endGestureInLoopNum, true)
                        setPeakTimeVmNum(peakTimeVmNum)
                        onOffUIAll(true)
                    }
                    if (it == 101) {
                        //деактивирует интерфейс
                        onOffUIAll(false)
                    }
                } else {
                    System.err.println("context GestureFragment NULL!")
                }
            }

        //включение работы протеза от датчиков
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            main?.runWriteData(byteArrayOf(0x01.toByte()), SENS_ENABLED_NEW, WRITE)
        }
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "gesture activity 1"
            if (useNewSystemSendCommand()) {
                main?.runSendCommand(byteArrayOf(0x01.toByte()), SENS_ENABLED_NEW_VM, countRestart)
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        testThreadFlag = false
    }


    private fun onOffUIAll (enabled: Boolean) {
        binding.onOffSensorGestureSwitchingSw.isEnabled = enabled
        binding.gestureLoop1Psv.isEnabled = enabled
        binding.gestureLoop2Psv.isEnabled = enabled
        binding.peakTimeVmSb.isEnabled = enabled
        binding.gesture1Btn.isEnabled = enabled
        binding.gesture2Btn.isEnabled = enabled
        binding.gesture3Btn.isEnabled = enabled
        binding.gesture4Btn.isEnabled = enabled
        binding.gesture5Btn.isEnabled = enabled
        binding.gesture6Btn.isEnabled = enabled
        binding.gesture7Btn.isEnabled = enabled
        binding.gesture8Btn.isEnabled = enabled
        binding.gesture9Btn.isEnabled = enabled
        binding.gesture10Btn.isEnabled = enabled
        binding.gesture11Btn.isEnabled = enabled
        binding.gesture12Btn.isEnabled = enabled
        binding.gesture13Btn.isEnabled = enabled
        binding.gesture14Btn.isEnabled = enabled
        binding.gesture15Btn.isEnabled = enabled
        binding.gesture16Btn.isEnabled = enabled
        binding.gesture17Btn.isEnabled = enabled
        binding.gesture18Btn.isEnabled = enabled
        binding.gesture19Btn.isEnabled = enabled
        binding.gesture20Btn.isEnabled = enabled
        binding.gesture21Btn.isEnabled = enabled
        binding.gesture22Btn.isEnabled = enabled
        binding.gesture23Btn.isEnabled = enabled
        binding.gesture24Btn.isEnabled = enabled
        binding.gesture25Btn.isEnabled = enabled
        binding.gestureSettings2Btn.isEnabled = enabled
        binding.gestureSettings3Btn.isEnabled = enabled
        binding.gestureSettings4Btn.isEnabled = enabled
        binding.gestureSettings5Btn.isEnabled = enabled
        binding.gestureSettings6Btn.isEnabled = enabled
        binding.gestureSettings7Btn.isEnabled = enabled
        binding.gestureSettings8Btn.isEnabled = enabled
        binding.gestureSettings9Btn.isEnabled = enabled
        binding.gestureSettings10Btn.isEnabled = enabled
        binding.gestureSettings11Btn.isEnabled = enabled
        binding.gestureSettings12Btn.isEnabled = enabled
        binding.gestureSettings13Btn.isEnabled = enabled
        binding.gestureSettings14Btn.isEnabled = enabled
        binding.gestureSettings15Btn.isEnabled = enabled
        binding.gestureSettings16Btn.isEnabled = enabled
        binding.gestureSettings17Btn.isEnabled = enabled
        binding.gestureSettings18Btn.isEnabled = enabled
        binding.gestureSettings19Btn.isEnabled = enabled
        binding.gestureSettings20Btn.isEnabled = enabled
        binding.gestureSettings21Btn.isEnabled = enabled
        binding.gestureSettings22Btn.isEnabled = enabled
        binding.gestureSettings23Btn.isEnabled = enabled
        binding.gestureSettings24Btn.isEnabled = enabled
        binding.gestureSettings25Btn.isEnabled = enabled
        binding.gesturesResetBtn.isEnabled = enabled
    }
    private fun hideUIRotationGroup (enabled: Boolean) {
        if (enabled) {
            binding.toggleGestureClasterRl.visibility = View.VISIBLE
            binding.peakTimeVmRl.visibility = View.VISIBLE
            binding.onOffSensorGestureSwitchingRl.visibility = View.VISIBLE
            binding.dividerV.visibility = View.VISIBLE
            binding.gesture9Btn.visibility = View.VISIBLE
            binding.gesture10Btn.visibility = View.VISIBLE
            binding.gesture11Btn.visibility = View.VISIBLE
            binding.gesture12Btn.visibility = View.VISIBLE
            binding.gesture13Btn.visibility = View.VISIBLE
            binding.gesture14Btn.visibility = View.VISIBLE
            binding.gestureSettings9Btn.visibility = View.VISIBLE
            binding.gestureSettings10Btn.visibility = View.VISIBLE
            binding.gestureSettings11Btn.visibility = View.VISIBLE
            binding.gestureSettings12Btn.visibility = View.VISIBLE
            binding.gestureSettings13Btn.visibility = View.VISIBLE
            binding.gestureSettings14Btn.visibility = View.VISIBLE
            binding.gesturesResetBtn.visibility = View.VISIBLE
        } else {
            offAllRotationImage()
            binding.toggleGestureClasterRl.visibility = View.GONE
            binding.peakTimeVmRl.visibility = View.GONE
            binding.onOffSensorGestureSwitchingRl.visibility = View.GONE
            binding.dividerV.visibility = View.GONE
            binding.gesture9Btn.visibility = View.GONE
            binding.gesture10Btn.visibility = View.GONE
            binding.gesture11Btn.visibility = View.GONE
            binding.gesture12Btn.visibility = View.GONE
            binding.gesture13Btn.visibility = View.GONE
            binding.gesture14Btn.visibility = View.GONE
            binding.gestureSettings9Btn.visibility = View.GONE
            binding.gestureSettings10Btn.visibility = View.GONE
            binding.gestureSettings11Btn.visibility = View.GONE
            binding.gestureSettings12Btn.visibility = View.GONE
            binding.gestureSettings13Btn.visibility = View.GONE
            binding.gestureSettings14Btn.visibility = View.GONE
            binding.gesturesResetBtn.visibility = View.GONE
        }
    }
    private fun setNumActiveGestures(activeGestures: Int) {
        System.err.println("my setNumActiveGestures activeGestures = $activeGestures")
        for (i in 1..activeGestures) {
            val gestureBtn = binding::class.java.getDeclaredField("gesture${i}Btn").get(binding) as Button
            val gestureSettingsBtn = binding::class.java.getDeclaredField("gestureSettings${i}Btn").get(binding) as ImageView
            gestureBtn.visibility = View.VISIBLE
            if (i in 5..17) {
                gestureSettingsBtn.visibility = View.VISIBLE
            }
        }
        for (i in activeGestures+1..NUM_GESTURES) {
            val gestureBtn = binding::class.java.getDeclaredField("gesture${i}Btn").get(binding) as Button
            val gestureSettingsBtn = binding::class.java.getDeclaredField("gestureSettings${i}Btn").get(binding) as ImageView
            gestureBtn.visibility = View.GONE
            gestureSettingsBtn.visibility = View.GONE
        }
        setNameGesturesAndRotationGroup(activeGestures)
    }
    private fun compileBLEMassage (useGesture: Int) {
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "gesture activity 2"
            main?.runSendCommand(byteArrayOf(useGesture.toByte()), SET_GESTURE_NEW_VM, countRestart)
        } else {
            if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
                main?.runWriteData(byteArrayOf(useGesture.toByte()), SET_GESTURE_NEW, WRITE)
            } else {
                main?.bleCommandConnector(byteArrayOf(useGesture.toByte()), SET_GESTURE, WRITE, 13)
            }
        }
    }
    @SuppressLint("UseCompatLoadingForDrawables", "UseCompatLoadingForColorStateLists")
    private fun resetStateButtons() {
        binding.gesture1Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture2Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture3Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture4Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture5Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture6Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture7Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture8Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture9Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture10Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture11Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture12Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture13Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture14Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture15Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture16Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture17Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture18Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture19Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture20Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture21Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture22Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture23Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture24Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture25Btn.background = resources.getDrawable(custom_button_le)
        binding.gesture1Btn.setTextColor(WHITE)
        binding.gesture2Btn.setTextColor(WHITE)
        binding.gesture3Btn.setTextColor(WHITE)
        binding.gesture4Btn.setTextColor(WHITE)
        binding.gesture5Btn.setTextColor(WHITE)
        binding.gesture6Btn.setTextColor(WHITE)
        binding.gesture7Btn.setTextColor(WHITE)
        binding.gesture8Btn.setTextColor(WHITE)
        binding.gesture9Btn.setTextColor(WHITE)
        binding.gesture10Btn.setTextColor(WHITE)
        binding.gesture11Btn.setTextColor(WHITE)
        binding.gesture12Btn.setTextColor(WHITE)
        binding.gesture13Btn.setTextColor(WHITE)
        binding.gesture14Btn.setTextColor(WHITE)
        binding.gesture15Btn.setTextColor(WHITE)
        binding.gesture16Btn.setTextColor(WHITE)
        binding.gesture17Btn.setTextColor(WHITE)
        binding.gesture18Btn.setTextColor(WHITE)
        binding.gesture19Btn.setTextColor(WHITE)
        binding.gesture20Btn.setTextColor(WHITE)
        binding.gesture21Btn.setTextColor(WHITE)
        binding.gesture22Btn.setTextColor(WHITE)
        binding.gesture23Btn.setTextColor(WHITE)
        binding.gesture24Btn.setTextColor(WHITE)
        binding.gesture25Btn.setTextColor(WHITE)
        binding.gestureSettings1Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings2Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings3Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings4Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings5Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings6Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings7Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings8Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings9Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings10Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings11Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings12Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings13Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings14Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings15Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings16Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings17Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings18Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings19Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings20Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings21Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings22Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings23Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings24Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureSettings25Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop1Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop2Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop3Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop4Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop5Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop6Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop7Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop8Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop9Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop10Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop11Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop12Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop13Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop14Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop15Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop16Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop17Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop18Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop19Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop20Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop21Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop22Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop23Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop24Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
        binding.gestureLoop25Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.white)
    }
    private fun setNameGesturesAndRotationGroup(activeGestures: Int) {
        for (i in 1..NUM_GESTURES) {
            val gestureBtn = binding::class.java.getDeclaredField("gesture${i}Btn").get(binding) as Button
            gestureBtn.text = gestureNameList[i-1]
        }
        //it.apply {
        //                setSpinnerAdapter(IconSpinnerAdapter(this))
        //                val list: MutableList<IconSpinnerItem> = ArrayList()
        //                for (i in 0 until activeGestures) {
        //                    list.add(IconSpinnerItem(text = gestureNameList[i], iconRes = handPalms[i], gravity = 100))
        //                }
        //                setItems(list)
        //                showDivider = true
        //                dividerSize = 2
        //                lifecycleOwner = this@GestureFragment
        //            }
        val list: MutableList<String> = ArrayList()
        for (i in 0 until activeGestures) {
            list.add(gestureNameList[i])
        }
        binding.gestureLoop1Psv.setItems(list)
        binding.gestureLoop2Psv.setItems(list)
        binding.gestureLoop1Psv.setOnSpinnerItemSelectedListener<String> { oldIndex, _, newIndex, _ ->
            startGestureInLoopNum = newIndex
            selectRotationGroup(startGestureInLoopNum, endGestureInLoopNum, true)
            System.err.println("test gestures in loop  GF gestureLoop1Psv selectRotationGroup startGestureInLoop=${startGestureInLoopNum+1}  endGestureInLoop=${endGestureInLoopNum+1} sendFlag = $sendFlag")
            System.err.println("DeviceControlActivity-------> gestureLoop1Psv sendFlag = $sendFlag")
            if (useNewSystemSendCommand() && sendFlag) {
                main?.runSendCommand(
                    byteArrayOf(
                        sensorGestureSwitching.toByte(),
                        0.toByte(),
                        binding.peakTimeVmSb.progress.toByte(),
                        0.toByte(),
                        lockProstheses.toByte(),
                        holdToLockTimeSb.toByte(),
                        startGestureInLoopNum.toByte(),
                        endGestureInLoopNum.toByte()
                    ), ROTATION_GESTURE_NEW_VM, countRestart
                )
            } else {
                sendFlag = true
            }

            if (oldIndex != newIndex) {
                sendFlag = false
                binding.gestureLoop2Psv.selectItemByIndex(endGestureInLoopNum)// - startGestureInLoop - 1
            }

            main?.saveInt(main?.mDeviceAddress + PreferenceKeys.START_GESTURE_IN_LOOP, startGestureInLoopNum)
            RxUpdateMainEvent.getInstance().updateUIChart(true)
        }
        binding.gestureLoop2Psv.setOnSpinnerItemSelectedListener<String> { oldIndex, _, newIndex, _ ->
            endGestureInLoopNum = newIndex
            System.err.println("test gestures in loop  GF gestureLoop2Psv selectRotationGroup startGestureInLoop=${startGestureInLoopNum+1}  endGestureInLoop=${endGestureInLoopNum+1} sendFlag = $sendFlag")
            selectRotationGroup(startGestureInLoopNum, endGestureInLoopNum, false)

            System.err.println("DeviceControlActivity-------> gestureLoop2Psv sendFlag = $sendFlag")
            if (useNewSystemSendCommand() && sendFlag) {
                main?.runSendCommand(byteArrayOf(
                        sensorGestureSwitching.toByte(),
                        0.toByte(),
                        binding.peakTimeVmSb.progress.toByte(),
                        0.toByte(),
                        lockProstheses.toByte(),
                        holdToLockTimeSb.toByte(),
                        startGestureInLoopNum.toByte(),
                        endGestureInLoopNum.toByte()
                    ), ROTATION_GESTURE_NEW_VM, countRestart
                )
            } else {
                sendFlag = true
            }

            if (oldIndex != newIndex) {
                sendFlag = false
                binding.gestureLoop1Psv.selectItemByIndex(startGestureInLoopNum)
            }

            main?.saveInt(main?.mDeviceAddress + PreferenceKeys.END_GESTURE_IN_LOOP, endGestureInLoopNum)
            RxUpdateMainEvent.getInstance().updateUIChart(true)
        }
    }
    @SuppressLint("UseCompatLoadingForDrawables", "UseCompatLoadingForColorStateLists")
    private fun selectActiveGesture(active: Int) {
        resetStateButtons()
        when (active) {
            1 -> { binding.gesture1Btn.background = resources.getDrawable(custom_button_le_selected)
                binding.gesture1Btn.setTextColor(resources.getColor(R.color.orange))
                binding.gestureSettings1Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop1Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            2 -> { binding.gesture2Btn.background = resources.getDrawable(custom_button_le_selected)
                binding.gesture2Btn.setTextColor(resources.getColor(R.color.orange))
                binding.gestureSettings2Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop2Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            3 -> { binding.gesture3Btn.background = resources.getDrawable(custom_button_le_selected)
                binding.gesture3Btn.setTextColor(resources.getColor(R.color.orange))
                binding.gestureSettings3Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop3Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            4 -> { binding.gesture4Btn.background = resources.getDrawable(custom_button_le_selected)
                binding.gesture4Btn.setTextColor(resources.getColor(R.color.orange))
                binding.gestureSettings4Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop4Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            5 -> { binding.gesture5Btn.background = resources.getDrawable(custom_button_le_selected)
                binding.gesture5Btn.setTextColor(resources.getColor(R.color.orange))
                binding.gestureSettings5Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop5Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            6 -> { binding.gesture6Btn.background = resources.getDrawable(custom_button_le_selected)
                binding.gesture6Btn.setTextColor(resources.getColor(R.color.orange))
                binding.gestureSettings6Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop6Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            7 -> { binding.gesture7Btn.background = resources.getDrawable(custom_button_le_selected)
                binding.gesture7Btn.setTextColor(resources.getColor(R.color.orange))
                binding.gestureSettings7Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop7Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            8 -> { binding.gesture8Btn.background = resources.getDrawable(custom_button_le_selected)
                binding.gesture8Btn.setTextColor(resources.getColor(R.color.orange))
                binding.gestureSettings8Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop8Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            9 -> { binding.gesture9Btn.background = resources.getDrawable(custom_button_le_selected)
                binding.gesture9Btn.setTextColor(resources.getColor(R.color.orange))
                binding.gestureSettings9Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop9Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            10 -> { binding.gesture10Btn.background = resources.getDrawable(custom_button_le_selected)
                binding.gesture10Btn.setTextColor(resources.getColor(R.color.orange))
                binding.gestureSettings10Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop10Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            11 -> { binding.gesture11Btn.background = resources.getDrawable(custom_button_le_selected)
                binding.gesture11Btn.setTextColor(resources.getColor(R.color.orange))
                binding.gestureSettings11Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop11Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            12 -> { binding.gesture12Btn.background = resources.getDrawable(custom_button_le_selected)
                binding.gesture12Btn.setTextColor(resources.getColor(R.color.orange))
                binding.gestureSettings12Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop12Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            13 -> { binding.gesture13Btn.background = resources.getDrawable(custom_button_le_selected)
                binding.gesture13Btn.setTextColor(resources.getColor(R.color.orange))
                binding.gestureSettings13Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop13Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
            14 -> { binding.gesture14Btn.background = resources.getDrawable(custom_button_le_selected)
                binding.gesture14Btn.setTextColor(resources.getColor(R.color.orange))
                binding.gestureSettings14Btn.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)
                binding.gestureLoop14Iv.backgroundTintList = context?.resources?.getColorStateList(R.color.orange)}
        }
        main?.saveInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, active)
    }
    private fun offAllRotationImage() {
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
    }
    private fun selectRotationGroup(startGestureInLoopNum: Int, endGestureInLoopNum: Int, changeStartGestureInLoop: Boolean){
        //проверка чтобы границы группы ротации не были больше числа активных жестов
        if (startGestureInLoopNum >= activeGestures) {
           this.startGestureInLoopNum = activeGestures - 1
        }
        if (endGestureInLoopNum >= activeGestures) {
            this.endGestureInLoopNum = activeGestures - 1
        }

        //блок ограничения жестов для группы ротации
        if (startGestureInLoopNum > endGestureInLoopNum) {
            this.endGestureInLoopNum = startGestureInLoopNum
            this.startGestureInLoopNum = endGestureInLoopNum
        }


        //блок отрисовки картинок цикла на нужных кнопках
//        System.err.println("my блок отрисовки картинок цикла на нужных кнопках 1")
        if (binding.onOffSensorGestureSwitchingSw.isChecked) {
//            System.err.println("my блок отрисовки картинок цикла на нужных кнопках 2 true")
//            binding.gesturesButtonsSv.layoutParams.height = 1000
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
                binding.gestureLoop14Iv,
                binding.gestureLoop15Iv,
                binding.gestureLoop16Iv,
                binding.gestureLoop17Iv,
                binding.gestureLoop18Iv,
                binding.gestureLoop19Iv,
                binding.gestureLoop20Iv,
                binding.gestureLoop21Iv,
                binding.gestureLoop22Iv,
                binding.gestureLoop23Iv,
                binding.gestureLoop24Iv,
                binding.gestureLoop25Iv,
            )
            for (i in 0 until NUM_GESTURES) {
                indicatorGestureLoop[i].visibility = View.GONE
            }
            for (i in startGestureInLoopNum until endGestureInLoopNum + 1) {
//                System.err.println("my цикл отрисовки картинок цикла на нужных кнопках 2.2 i=$i")
                indicatorGestureLoop[i].visibility = View.VISIBLE
            }
        } else {
//            binding.gesturesButtonsSv.layoutParams.height = 0
//            System.err.println("my блок отрисовки картинок цикла на нужных кнопках 3 false")
        }
    }
    private fun setPeakTimeVmNum(peakTimeVmNum: Int) {
        main?.runOnUiThread {
            ObjectAnimator.ofInt(
                binding.peakTimeVmSb,
                "progress",
                peakTimeVmNum
            ).setDuration(200).start()
        }
        val time: String = when {
            ((peakTimeVmNum + 1) * 0.1).toString().length == 4 -> {
                ((peakTimeVmNum + 1) * 0.1).toString() + "c"
            }
            ((peakTimeVmNum + 1) * 0.1).toString().length > 4 -> {
                ((peakTimeVmNum + 1) * 0.1).toString().substring(0,4) + "c"
            }
            else -> {
                ((peakTimeVmNum + 1) * 0.1).toString() + "0c"
            }
        }
        binding.peakTimeVmTv.text = time
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
        if (checkDriverVersionGreaterThan237()) {
            if (checkDriverVersionGreaterThan240()) {
                when (v?.id) {
                    R.id.gesture_settings_5_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 4)
                    }

                    R.id.gesture_settings_6_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 5)
                    }

                    R.id.gesture_settings_7_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 6)
                    }

                    R.id.gesture_settings_8_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 7)
                    }

                    R.id.gesture_settings_9_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 8)
                    }

                    R.id.gesture_settings_10_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 9)
                    }

                    R.id.gesture_settings_11_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 10)
                    }

                    R.id.gesture_settings_12_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 11)
                    }

                    R.id.gesture_settings_13_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 12)
                    }

                    R.id.gesture_settings_14_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 13)
                    }

                    R.id.gesture_settings_15_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 14)
                    }

                    R.id.gesture_settings_16_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 15)
                    }

                    R.id.gesture_settings_17_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 16)
                    }
                }
            } else {
                when (v?.id) {
                    R.id.gesture_settings_2_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 1)
                    }

                    R.id.gesture_settings_3_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 2)
                    }

                    R.id.gesture_settings_4_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 3)
                    }

                    R.id.gesture_settings_5_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 4)
                    }

                    R.id.gesture_settings_6_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 5)
                    }

                    R.id.gesture_settings_7_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 6)
                    }

                    R.id.gesture_settings_8_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 7)
                    }

                    R.id.gesture_settings_9_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 8)
                    }

                    R.id.gesture_settings_10_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 9)
                    }

                    R.id.gesture_settings_11_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 10)
                    }

                    R.id.gesture_settings_12_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 11)
                    }

                    R.id.gesture_settings_13_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 12)
                    }

                    R.id.gesture_settings_14_btn -> {
                        main?.saveInt(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM, 13)
                    }
                }
            }
        } else {
            when (v?.id) {
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
        }


        //выключение работы протеза от датчиков
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            main?.runWriteData(byteArrayOf(0x00.toByte()), SENS_ENABLED_NEW, WRITE)
        }
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "gesture activity 3"
            if (useNewSystemSendCommand()) {
                main?.runSendCommand(byteArrayOf(0x00.toByte()), SENS_ENABLED_NEW_VM, countRestart)
            }
        }
    }
    override fun onValueSelected(e: Entry?, h: Highlight?) {}
    override fun onNothingSelected() {}

    private fun loadAllVariables() {
        val text = "load not work"
        val macKey = mSettings!!.getString(PreferenceKeys.LAST_CONNECTION_MAC, text)

        for (i in 0 until NUM_GESTURES) {
            System.err.println("9 LAST_CONNECTION_MAC: "+PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + macKey + i)
            gestureNameList.add(mSettings!!.getString(PreferenceKeys.SELECT_GESTURE_SETTINGS_NUM + macKey + i, text).toString())
        }

        if (checkDriverVersionGreaterThan237()) {
            if (mSettings!!.getBoolean(
                    main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM,
                    false
                )
            ) {
                binding.onOffSensorGestureSwitchingSw.isChecked = true
                binding.onOffSensorGestureSwitchingTv.text = resources.getString(R.string.on_sw)
                binding.toggleGestureClasterRl.animate().alpha(1.0f).duration = 300
                binding.peakTimeVmRl.animate().alpha(1.0f).duration = 300
                binding.dividerV.animate().translationY(0F).duration = 300
                binding.gesturesButtonsSv.animate().translationY(0F).duration = 300

//                Handler().postDelayed({
                    // Получаем координаты
                    binding.gesturesButtonsSv.getLocationOnScreen(gesturesButtonsSVLocation)
                    binding.relativeLayout88.getLocationOnScreen(relativeLayout88Location)
//
//                    // Вычисляем расстояние между верхними границами
                    distance = relativeLayout88Location[1] - gesturesButtonsSVLocation[1]
                    Log.d("Distance", "distance = $distance  loadAllVariables isChecked")
                    binding.gesturesButtonsSv.layoutParams.height = distance// + heightActiveRotationGroupPx
//                }, 1000)
            } else {
                binding.onOffSensorGestureSwitchingSw.isChecked = false
                binding.onOffSensorGestureSwitchingTv.text = resources.getString(R.string.off_sw)
                binding.toggleGestureClasterRl.animate().alpha(0.0f).duration = 300
                binding.peakTimeVmRl.animate().alpha(0.0f).duration = 300
                binding.dividerV.animate()
                    .translationY(-(binding.toggleGestureClasterRl.height + binding.peakTimeVmRl.height + 16).toFloat()).duration =
                    300
                binding.gesturesButtonsSv.animate()
                    .translationY(-(binding.toggleGestureClasterRl.height + binding.peakTimeVmRl.height + 16).toFloat()).duration =
                    300
                offAllRotationImage()

//                Handler().postDelayed({
                    heightActiveRotationGroupPx = (heightActiveRotationGroupDp * density + 0.5f).toInt()
                    // Получаем координаты
                    binding.gesturesButtonsSv.getLocationOnScreen(gesturesButtonsSVLocation)
                    binding.relativeLayout88.getLocationOnScreen(relativeLayout88Location)
                    // Вычисляем расстояние между верхними границами
                    distance = relativeLayout88Location[1] - gesturesButtonsSVLocation[1]
                    Log.d("Distance", "distance = $distance  loadAllVariables notChecked")

                    binding.gesturesButtonsSv.layoutParams.height = distance// + heightActiveRotationGroupPx
//                }, 1000)
            }
        }


        activeGestures = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.NUM_ACTIVE_GESTURES, 8)
        setNumActiveGestures(activeGestures)

        startGestureInLoopNum = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.START_GESTURE_IN_LOOP, 0)
        endGestureInLoopNum = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.END_GESTURE_IN_LOOP, 0)
        try {
            sendFlag = false
            binding.gestureLoop1Psv.selectItemByIndex(startGestureInLoopNum)
            sendFlag = false
            binding.gestureLoop2Psv.selectItemByIndex(endGestureInLoopNum)
        } catch (e : Exception) {
            sendFlag = false
            binding.gestureLoop1Psv.selectItemByIndex(activeGestures - 1 )
            sendFlag = false
            binding.gestureLoop2Psv.selectItemByIndex(activeGestures - 1 )
        }

        peakTimeVmNum = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SET_PEAK_TIME_VM_NUM, 15)

        //данные необходимые для формирования правильных блютуз команд, но не изменяемые на этом фрагменте
        sensorGestureSwitching = if (mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false)) { 0x01 } else { 0x00 }
        lockProstheses = if (mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_LOCK_NUM, false)) { 0x01 } else { 0x00 }
        holdToLockTimeSb = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.HOLD_TO_LOCK_TIME_NUM, 15)
        main?.runOnUiThread {
            ObjectAnimator.ofInt(
                binding.peakTimeVmSb,
                "progress",
                mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SET_PEAK_TIME_VM_NUM, 15)
            ).setDuration(200).start()
        }
    }
    private fun checkDriverVersionGreaterThan237():Boolean {
        return if (main?.driverVersionS != null) {
            val driverNum = main?.driverVersionS?.substring(0, 1) + main?.driverVersionS?.substring(2, 4)
            driverNum.toInt() >= 237
        } else {
            false
        }
    }
    private fun checkDriverVersionGreaterThan240():Boolean {
        return if (main?.driverVersionS != null) {
            val driverNum = main?.driverVersionS?.substring(0, 1) + main?.driverVersionS?.substring(2, 4)
            driverNum.toInt() >= 240
        } else {
            false
        }
    }
    private fun useNewSystemSendCommand(): Boolean {
        //спиннеры имеют свойство спамить блютуз команды при установке в них значения из памяти,
        // что мешает нормальной инициализации блютуза и запросу стартовых параметров. Для
        // отключения этого спама мы делаем эту проверку
        var useNewSystemSendCommand = false
        if (main?.driverVersionS != null) {
            val driverNum = main?.driverVersionS?.substring(0, 1) + main?.driverVersionS?.substring(2, 4)
            useNewSystemSendCommand = driverNum.toInt() > 233
        }
        return useNewSystemSendCommand
    }
}

