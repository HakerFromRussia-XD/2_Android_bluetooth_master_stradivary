package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.main

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.bailout.stickk.R
import com.bailout.stickk.R.drawable.*
import com.bailout.stickk.databinding.LayoutGesturesBinding
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes.*
import com.bailout.stickk.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.gripper.with_encoders.GripperScreenWithEncodersActivity
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.gripper.without_encoders.GripperScreenWithoutEncodersActivity
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.skydoves.powerspinner.IconSpinnerAdapter
import com.skydoves.powerspinner.IconSpinnerItem
import io.reactivex.android.schedulers.AndroidSchedulers
import org.jetbrains.anko.backgroundDrawable
import org.jetbrains.anko.textColor


@Suppress("DEPRECATION")
class GestureFragment: Fragment(), OnChartValueSelectedListener, View.OnClickListener{

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
        hand_palm_14
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
    private val countRestart = 5//TODO поставить по больше после отладки (50)

    private lateinit var binding: LayoutGesturesBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = LayoutGesturesBinding.inflate(layoutInflater)
        if (activity != null) { main = activity as MainActivity? }

        return binding.root
    }
    @Deprecated("Deprecated in Java")
    @SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables", "UseCompatLoadingForColorStateLists")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)


        onOffUIAll(false)
        binding.onOffSensorGestureSwitchingSw.setOnClickListener {
            if (binding.onOffSensorGestureSwitchingSw.isChecked) {
                sensorGestureSwitching = 0x01
                binding.onOffSensorGestureSwitchingTv.text = resources.getString(R.string.on_sw)
                binding.toggleGestureClasterRl.animate().alpha(1.0f).duration = 300
                binding.peakTimeVmRl.animate().alpha(1.0f).duration = 300
                binding.dividerV.animate().translationY(0F).duration = 300
                binding.gesturesButtonsSv.animate().translationY(0F).duration = 300
                selectRotationGroup(startGestureInLoopNum, endGestureInLoopNum, true)
                binding.gesturesButtonsSv.layoutParams.height = 1000
            } else {
                binding.gesturesButtonsSv.layoutParams.height = 0
                sensorGestureSwitching = 0x00
                binding.onOffSensorGestureSwitchingTv.text = resources.getString(R.string.off_sw)
                binding.toggleGestureClasterRl.animate().alpha(0.0f).duration = 300
                binding.peakTimeVmRl.animate().alpha(0.0f).duration = 300
                binding.dividerV.animate().translationY(-(binding.toggleGestureClasterRl.height + binding.peakTimeVmRl.height + 16).toFloat()).duration = 300
                binding.gesturesButtonsSv.animate().translationY(-(binding.toggleGestureClasterRl.height + binding.peakTimeVmRl.height + 16).toFloat()).duration = 300
                offAllRotationImage()
            }
            main?.runSendCommand(byteArrayOf(
                sensorGestureSwitching.toByte(),
                0.toByte(),
                binding.peakTimeVmSb.progress.toByte(),
                0.toByte(),
                lockProstheses.toByte(),
                holdToLockTimeSb.toByte(),
                startGestureInLoopNum.toByte(),
                endGestureInLoopNum.toByte()
            ), ROTATION_GESTURE_NEW_VM, countRestart)
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

                main?.runSendCommand(byteArrayOf(
                    sensorGestureSwitching.toByte(),
                    0.toByte(),
                    binding.peakTimeVmSb.progress.toByte(),
                    0.toByte(),
                    lockProstheses.toByte(),
                    holdToLockTimeSb.toByte(),
                    startGestureInLoopNum.toByte(),
                    endGestureInLoopNum.toByte()
                ), ROTATION_GESTURE_NEW_VM, countRestart)
                RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(ROTATION_GESTURE_NEW_VM)

                main?.saveInt(main?.mDeviceAddress + PreferenceKeys.SET_PEAK_TIME_VM_NUM, seekBar.progress)
            }
        })


        binding.gestureSettings2Btn.setOnClickListener(this)
        binding.gestureSettings3Btn.setOnClickListener(this)
        binding.gestureSettings4Btn.setOnClickListener(this)
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
        binding.gesturesResetBtn.setOnClickListener { main?.showGestureResetDialog() }

        //скрываем интерфейс управления группами ротации
        if (!main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            hideUIRotationGroup(false)
        }
    }
    @SuppressLint("CheckResult")
    override fun onResume() {
        super.onResume()
        gestureNameList.clear()
        loadAllVariables()
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
                        selectActiveGesture(it)
                        selectRotationGroup(startGestureInLoopNum, endGestureInLoopNum, true)
                        //тут нет onOffUIAll потому что
                        onOffUIAll(true)
                    }
                    if (it == 100) {
                        //активирует интерфейс
                        loadAllVariables()
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
            main?.runSendCommand(byteArrayOf(0x01.toByte()), SENS_ENABLED_NEW_VM, 50)
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
        }
    }
    private fun setNumActiveGestures(activeGestures: Int) {
        System.err.println("my setNumActiveGestures activeGestures = $activeGestures")
        if (activeGestures == 8) {
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
        } else {
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
        }
        setNameGesturesAndRotationGroup(activeGestures)
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
    private fun resetStateButtons() {
        binding.gesture1Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture2Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture3Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture4Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture5Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture6Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture7Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture8Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture9Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture10Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture11Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture12Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture13Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture14Btn.backgroundDrawable = resources.getDrawable(custom_button_le)
        binding.gesture1Btn.textColor = WHITE
        binding.gesture2Btn.textColor = WHITE
        binding.gesture3Btn.textColor = WHITE
        binding.gesture4Btn.textColor = WHITE
        binding.gesture5Btn.textColor = WHITE
        binding.gesture6Btn.textColor = WHITE
        binding.gesture7Btn.textColor = WHITE
        binding.gesture8Btn.textColor = WHITE
        binding.gesture9Btn.textColor = WHITE
        binding.gesture10Btn.textColor = WHITE
        binding.gesture11Btn.textColor = WHITE
        binding.gesture12Btn.textColor = WHITE
        binding.gesture13Btn.textColor = WHITE
        binding.gesture14Btn.textColor = WHITE
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
    }
    private fun setNameGesturesAndRotationGroup(activeGestures: Int) {
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
        binding.gestureLoop1Psv.let {
            it.apply {
                setSpinnerAdapter(IconSpinnerAdapter(this))
                val list: MutableList<IconSpinnerItem> = ArrayList()
                for (i in 0 until activeGestures) {
                    list.add(IconSpinnerItem(text = gestureNameList[i], iconRes = handPalms[i], gravity = 100))
                }
                setItems(list)
                showDivider = true
                dividerSize = 2
                lifecycleOwner = this@GestureFragment
            }
            it.setOnSpinnerItemSelectedListener<IconSpinnerItem> { oldIndex, _, newIndex, _ ->
                startGestureInLoopNum = newIndex
                System.err.println("test gestures in loop    GF gestureLoop1Psv selectRotationGroup startGestureInLoop=$startGestureInLoopNum  endGestureInLoop=$endGestureInLoopNum")
                selectRotationGroup(startGestureInLoopNum, endGestureInLoopNum, true)

                if (oldIndex != newIndex) {
                    binding.gestureLoop2Psv.selectItemByIndex(endGestureInLoopNum)// - startGestureInLoop - 1
                }

                main?.runSendCommand(byteArrayOf(
                    sensorGestureSwitching.toByte(),
                    0.toByte(),
                    binding.peakTimeVmSb.progress.toByte(),
                    0.toByte(),
                    lockProstheses.toByte(),
                    holdToLockTimeSb.toByte(),
                    startGestureInLoopNum.toByte(),
                    endGestureInLoopNum.toByte()
                ), ROTATION_GESTURE_NEW_VM, countRestart)
                main?.saveInt(main?.mDeviceAddress + PreferenceKeys.START_GESTURE_IN_LOOP, startGestureInLoopNum)
//                System.err.println("gonka gesture 2 onViewCreated true")
                RxUpdateMainEvent.getInstance().updateUIChart(true)
            }
        }
        binding.gestureLoop2Psv.let {
            it.apply {
                setSpinnerAdapter(IconSpinnerAdapter(this))
                val list: MutableList<IconSpinnerItem> = ArrayList()
                for (i in 0 until activeGestures) {
                    list.add(
                        IconSpinnerItem(
                            text = gestureNameList[i],
                            iconRes = handPalms[i],
                            gravity = 100
                        )
                    )
                }
                setItems(list)
                showDivider = true
                dividerSize = 2
                lifecycleOwner = this@GestureFragment
            }
            it.setOnSpinnerItemSelectedListener<IconSpinnerItem> {
                    oldIndex, _, newIndex, _ ->
                endGestureInLoopNum = newIndex
                System.err.println("test gestures in loop  GF gestureLoop2Psv selectRotationGroup startGestureInLoop=$startGestureInLoopNum  endGestureInLoop=$endGestureInLoopNum")
                selectRotationGroup(startGestureInLoopNum, endGestureInLoopNum, false)
                if (oldIndex != newIndex) {
                    binding.gestureLoop1Psv.selectItemByIndex(startGestureInLoopNum)
                }

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
                main?.saveInt(
                    main?.mDeviceAddress + PreferenceKeys.END_GESTURE_IN_LOOP,
                    endGestureInLoopNum
                )
//                System.err.println("gonka gesture 3 onViewCreated true")
                RxUpdateMainEvent.getInstance().updateUIChart(true)
            }
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
            binding.gesturesButtonsSv.layoutParams.height = 1000
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
                binding.gestureLoop14Iv
            )
            for (i in 0 until 14) {
                indicatorGestureLoop[i].visibility = View.GONE
            }
            for (i in startGestureInLoopNum until endGestureInLoopNum + 1) {
//                System.err.println("my цикл отрисовки картинок цикла на нужных кнопках 2.2 i=$i")
                indicatorGestureLoop[i].visibility = View.VISIBLE
            }
        } else {
            binding.gesturesButtonsSv.layoutParams.height = 0
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
            main?.runSendCommand(byteArrayOf(0x00.toByte()), SENS_ENABLED_NEW_VM, 50)
        }
    }
    override fun onValueSelected(e: Entry?, h: Highlight?) {}
    override fun onNothingSelected() {}

    private fun loadAllVariables() {
        val text = "load not work"
        val macKey = mSettings!!.getString(PreferenceKeys.LAST_CONNECTION_MAC, text)

        for (i in 0 until PreferenceKeys.NUM_GESTURES) {
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
                binding.gesturesButtonsSv.layoutParams.height = 1000
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
                binding.gesturesButtonsSv.layoutParams.height = 0
            }
        }


        activeGestures = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.NUM_ACTIVE_GESTURES, 8)
        setNumActiveGestures(activeGestures)

        startGestureInLoopNum = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.START_GESTURE_IN_LOOP, 0)
        endGestureInLoopNum = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.END_GESTURE_IN_LOOP, 0)
        try {
            binding.gestureLoop1Psv.selectItemByIndex(startGestureInLoopNum)
            binding.gestureLoop2Psv.selectItemByIndex(endGestureInLoopNum)
        } catch (e : Exception) {
            binding.gestureLoop1Psv.selectItemByIndex(activeGestures - 1 )
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
}

