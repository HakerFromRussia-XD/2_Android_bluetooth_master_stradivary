/*Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bailout.stickk.new_electronic_by_Rodeon.ui.fragments.main

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.gesture.GestureOverlayView
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bailout.stickk.BuildConfig
import com.bailout.stickk.R
import com.bailout.stickk.databinding.LayoutAdvancedSettingsBinding
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.DEVICE_TYPE_FEST_F
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.DEVICE_TYPE_FEST_H
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.DEVICE_TYPE_FEST_H_EB
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.DEVICE_TYPE_FEST_H_EP
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.DEVICE_TYPE_FEST_X
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.DEVICE_TYPE_INDY_EB
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.DEVICE_TYPE_INDY_EP
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.DEVICE_TYPE_INDY_H
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.DEVICE_TYPE_INDY_SH
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.NEW_DEVICE_TYPE_FEST_EB
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.NEW_DEVICE_TYPE_FEST_EP
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.NEW_DEVICE_TYPE_FEST_F
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager.NEW_DEVICE_TYPE_FEST_H
import com.bailout.stickk.new_electronic_by_Rodeon.ble.SampleGattAttributes.*
import com.bailout.stickk.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import com.bailout.stickk.new_electronic_by_Rodeon.persistence.preference.PreferenceManager
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.gripper.test_encoders.GripperTestScreenWithEncodersActivity
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

@Suppress("DEPRECATION", "UNNECESSARY_SAFE_CALL")
class AdvancedSettingsFragment : Fragment() {
  private var sendFlag: Boolean = false
  @Inject
  lateinit var preferenceManager: PreferenceManager

  private var mContext: Context? = null
  private var main: MainActivity? = null
  private var mSettings: SharedPreferences? = null
  private var scale = 0F
  private var mode: Byte = 0x00
  private var sensorGestureSwitching: Byte = 0x00
  private var lockProstheses: Byte = 0x00
  private var validationError = ""

//  private var current  = 0
  private var current1 = 0
  private var current2 = 0
  private var current3 = 0
  private var current4 = 0
  private var current5 = 0
  private var current6 = 0

  private var correlatorNoiseThreshold1 = 0
  private var correlatorNoiseThreshold2 = 0
  private var modeEMGSend = 0

  private var startGestureInLoopNum = 0
  private var endGestureInLoopNum = 0
  private val countRestart = 3

  private lateinit var binding: LayoutAdvancedSettingsBinding
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    binding = LayoutAdvancedSettingsBinding.inflate(layoutInflater)
    WDApplication.component.inject(this)
    if (activity != null) { main = activity as MainActivity? }
    this.mContext = context
    scale = resources.displayMetrics.density
    main?.startScrollForAdvancedSettingsFragment = {
      System.err.println("AdvancedSettingsFragment startScroll")
      binding.EMGModeSwapPsv.dismiss()
    }

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initializeUI()
    updateAllParameters()
    enableInterface(false)
  }

  @SuppressLint("CheckResult")
  override fun onResume() {
    super.onResume()
    main!!.setDebugScreenIsOpen(false)
    RxUpdateMainEvent.getInstance().uiAdvancedSettings
      .compose(main?.bindToLifecycle())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe {
        if (context != null) {
          updateAllParameters()
          enableInterface(it)
        } else {
          System.err.println("context AdvancedSettingsFragment NULL!")
        }
      }

    RxUpdateMainEvent.getInstance().resetAdvancedSettings
      .compose(main?.bindToLifecycle())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe {
        resetUI()
      }

//    System.err.println("serialNumber validationAndConversionSerialNumber "+validationAndConversionSerialNumber("FEST-H-1234"))
//    System.err.println("serialNumber validationAndConversionSerialNumber "+validationAndConversionSerialNumber("FEST-H-12345"))
//    System.err.println("serialNumber validationAndConversionSerialNumber "+validationAndConversionSerialNumber("FEST-H-123456"))
//    System.err.println("serialNumber validationAndConversionSerialNumber "+validationAndConversionSerialNumber("FEST-F-1234"))
//    System.err.println("serialNumber validationAndConversionSerialNumber "+validationAndConversionSerialNumber("FEST-F-12345"))
//    System.err.println("serialNumber validationAndConversionSerialNumber "+validationAndConversionSerialNumber("FEST-F-123456"))
//    System.err.println("serialNumber validationAndConversionSerialNumber "+validationAndConversionSerialNumber("FEST-EP-12345"))
//    System.err.println("serialNumber validationAndConversionSerialNumber "+validationAndConversionSerialNumber("FEST-EB-12345"))
//    binding.serialNumberEt.setText("INDY-H-04440")
//    System.err.println("serialNumber validationAndConversionSerialNumber "+validationAndConversionSerialNumber("INDY-H-12345"))
//    System.err.println("serialNumber validationAndConversionSerialNumber "+validationAndConversionSerialNumber("INDY-EP-12345"))
//    System.err.println("serialNumber validationAndConversionSerialNumber "+validationAndConversionSerialNumber("INDY-EB-12345"))
//    System.err.println("serialNumber validationAndConversionSerialNumber "+validationAndConversionSerialNumber("INDY-SH-12345"))
//    System.err.println("serialNumber validationAndConversionSerialNumber "+validationAndConversionSerialNumber("FEST-D-12345"))
  }

  @SuppressLint("SetTextI18n")
  private fun resetUI() {
    if (!main?.lockWriteBeforeFirstRead!!) {
      preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, false)

      main?.firstActivateSetScaleDialog = false
      main?.setSwapOpenCloseButton(false)
      binding.swapOpenCloseSw.isChecked = false
      binding.swapOpenCloseTv.text = resources.getString(R.string.off_sw)
      preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)

      preferenceManager.putInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM, 80)

      if (checkINDYCanControlEMGModes()) {
        binding.shutdownCurrentSb.max = 50
        System.err.println("resetUI 50")
      } else {
        binding.shutdownCurrentSb.max = 100
        System.err.println("resetUI 100")
      }

      if (checkINDYCanControlEMGModes()) {
        ObjectAnimator.ofInt(binding.shutdownCurrentSb, "progress", preferenceManager.getInt(main?.mDeviceAddress + PreferenceKeys.MIN_SHUTDOWN_CURRENT_NUM, 25)).setDuration(200).start()
      } else {
        ObjectAnimator.ofInt(binding.shutdownCurrentSb, "progress", preferenceManager.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM, 80)).setDuration(200).start()
      }


      binding.singleChannelControlSw.isChecked = false
      binding.singleChannelControlTv.text = resources.getString(R.string.off_sw)
      saveBool(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)

      binding.onOffSensorGestureSwitchingSw.isChecked = false
      binding.onOffSensorGestureSwitchingTv.text = resources.getString(R.string.off_sw)
      sensorGestureSwitching = 0x00
      binding.modeRl.visibility = View.GONE
      binding.peakTimeRl.visibility = View.GONE
      binding.downtimeRl.visibility = View.GONE
      saveBool(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false)


      binding.modeTv.text = "одним\nдатчиком"
      mode = 0x00
      preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, false)

      RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(SHUTDOWN_CURRENT_NEW_VM)
    } else {
      updateAllParameters()
      main?.showToast(resources.getString(R.string.waiting_for_data_transfer_from_the_prosthesis))
    }
  }

  @SuppressLint("SetTextI18n", "CheckResult", "Recycle")
  private fun initializeUI() {
    mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)

    if (checkINDYCanControlEMGModes()) {
      binding.shutdownCurrentSb.max = 50
    } else {
      binding.shutdownCurrentSb.max = 100
    }

    binding.EMGModeSwapPsv.setOnSpinnerItemSelectedListener<String> { _, _, newIndex, _ ->

      modeEMGSend = when (newIndex) {
        0 -> {
          saveInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_EMG_SENSORS, 9)
          9
        }
        1 -> {
          saveInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_EMG_SENSORS, 7)
          7
        }
        2 -> {
          saveInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_EMG_SENSORS, 10)
          10
        }
        else -> {
          saveInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_EMG_SENSORS, 9)
          9
        }
      }

      if (useNewSystemSendCommand() && sendFlag) {
        sendEMGMode(modeEMGSend)
        System.err.println("TEST отправляем блютуз команду $sendFlag")
      } else {
        sendFlag = true
        System.err.println("TEST просто меняем значение в спиннере $sendFlag")
      }
    }
//    recursia(0)

    when (mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_EMG_SENSORS,9)) {
      9 -> {
        sendFlag = false
        binding.EMGModeSwapPsv.selectItemByIndex(0) }
      7 -> {
        sendFlag = false
        binding.EMGModeSwapPsv.selectItemByIndex(1) }
      10 -> {
        sendFlag = false
        binding.EMGModeSwapPsv.selectItemByIndex(2) }
    }


    if (mSettings?.getInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1) == 1) {
      binding.leftRightSideSwapSw.isChecked = true
      binding.leftRightSideSwapTv.text = resources.getString(R.string.right)
    } else {
      binding.leftRightSideSwapSw.isChecked = false
      binding.leftRightSideSwapTv.text = resources.getString(R.string.left)
    }
    if (mSettings?.getInt(main?.mDeviceAddress + PreferenceKeys.SET_FINGERS_DELAY, 0) == 1){
      binding.timeDelayOfFingersSwapSw.isChecked = true
      binding.timeDelayOfFingersSwapTv.text = resources.getString(R.string.on_sw)
    } else {
      binding.timeDelayOfFingersSwapSw.isChecked = false
      binding.timeDelayOfFingersSwapTv.text = resources.getString(R.string.off_sw)
    }
    if (mSettings!!.getBoolean(PreferenceKeys.SET_MODE_SMART_CONNECTION, false)) {
      binding.smartConnectionSwapSw.isChecked = true
      binding.smartConnectionSwapTv.text = resources.getString(R.string.on_sw)
    } else {
      binding.smartConnectionSwapSw.isChecked = false
      binding.smartConnectionSwapTv.text = resources.getString(R.string.off_sw)
    }
    when (mSettings?.getInt(main?.mDeviceAddress + PreferenceKeys.NUM_ACTIVE_GESTURES, 8)) {
      7 -> {
        binding.gesturePsvRl.visibility = View.VISIBLE
        sendFlag = false
        binding.gesturePsv.selectItemByIndex(0) }
      8 -> {
        if (checkDriverVersionGreaterThan240()) {
          binding.gesturePsvRl.visibility = View.VISIBLE
          binding.activeGesturesSwapTv.text = "7"
          saveInt(main?.mDeviceAddress + PreferenceKeys.NUM_ACTIVE_GESTURES, 7)
          sendFlag = false
          binding.gesturePsv.selectItemByIndex(0)
        } else {
          binding.gesturePsvRl.visibility = View.GONE
          binding.activeGesturesSwapSw.isChecked = false
          binding.activeGesturesSwapTv.text = "8"
        }
      }
      13 -> {
        binding.gesturePsvRl.visibility = View.VISIBLE
        sendFlag = false
        binding.gesturePsv.selectItemByIndex(1) }
      14 -> {
        if (checkDriverVersionGreaterThan240()) {
          binding.gesturePsvRl.visibility = View.VISIBLE
          binding.activeGesturesSwapTv.text = "13"
          saveInt(main?.mDeviceAddress + PreferenceKeys.NUM_ACTIVE_GESTURES, 13)
          sendFlag = false
          binding.gesturePsv.selectItemByIndex(1)
        } else {
          binding.gesturePsvRl.visibility = View.GONE
          binding.activeGesturesSwapSw.isChecked = true
          binding.activeGesturesSwapTv.text = "14"
        } }
      23 -> {
        binding.gesturePsvRl.visibility = View.VISIBLE
        sendFlag = false
        binding.gesturePsv.selectItemByIndex(2) }
      25 -> {
        binding.gesturePsvRl.visibility = View.VISIBLE
        sendFlag = false
        binding.gesturePsv.selectItemByIndex(3) }
    }

    if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
      val calibrationStartParams: LinearLayout.LayoutParams =
        binding.calibrationStartButtonLl.layoutParams as LinearLayout.LayoutParams
      calibrationStartParams.weight = 1f
      val calibrationStatusParams: LinearLayout.LayoutParams =
        binding.calibrationStatusButtonLl.layoutParams as LinearLayout.LayoutParams
      calibrationStatusParams.weight = 1f
    } else {
      binding.timeDelayOfFingersRl.visibility = View.GONE
      if (main?.locate?.contains("w")!!) { binding.calibrationStatusAdvBtn.textSize = 12f }
    }


    binding.shutdownCurrentSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        binding.shutdownCurrentTv.text = (seekBar.progress).toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (checkINDYCanControlEMGModes()) {
            main?.bleCommandConnector(byteArrayOf((seekBar.progress).toByte(), (seekBar.progress).toByte()), SHUTDOWN_CURRENT_HDLE, WRITE, 0)
            saveInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM, seekBar.progress)
            saveInt(main?.mDeviceAddress + PreferenceKeys.MIN_SHUTDOWN_CURRENT_NUM, seekBar.progress)
          } else {
            main?.bleCommandConnector(byteArrayOf((seekBar.progress).toByte()), SHUTDOWN_CURRENT_HDLE, WRITE, 0)
            saveInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM, seekBar.progress)
          }
        }
        RxUpdateMainEvent.getInstance().updateUIChart(true)
      }
    })

    binding.shutdownCurrent1Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        binding.shutdownCurrent1Tv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(
              binding.shutdownCurrent1Sb.progress.toByte(),
              binding.shutdownCurrent2Sb.progress.toByte(),
              binding.shutdownCurrent3Sb.progress.toByte(),
              binding.shutdownCurrent4Sb.progress.toByte(),
              binding.shutdownCurrent5Sb.progress.toByte(),
              binding.shutdownCurrent6Sb.progress.toByte()),
              SHUTDOWN_CURRENT_NEW_VM, countRestart)
          }
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
            main?.bleCommandConnector(byteArrayOf(
              binding.shutdownCurrent1Sb.progress.toByte(),
              binding.shutdownCurrent2Sb.progress.toByte(),
              binding.shutdownCurrent3Sb.progress.toByte(),
              binding.shutdownCurrent4Sb.progress.toByte(),
              binding.shutdownCurrent5Sb.progress.toByte(),
              binding.shutdownCurrent6Sb.progress.toByte()),
              SHUTDOWN_CURRENT_NEW, WRITE, 0)
          }
          saveInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_1, seekBar.progress)
          RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(SHUTDOWN_CURRENT_NEW_VM)
        }
      }
    })
    binding.shutdownCurrent2Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        binding.shutdownCurrent2Tv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(
              binding.shutdownCurrent1Sb.progress.toByte(),
              binding.shutdownCurrent2Sb.progress.toByte(),
              binding.shutdownCurrent3Sb.progress.toByte(),
              binding.shutdownCurrent4Sb.progress.toByte(),
              binding.shutdownCurrent5Sb.progress.toByte(),
              binding.shutdownCurrent6Sb.progress.toByte()),
              SHUTDOWN_CURRENT_NEW_VM, countRestart)
          }
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
            main?.bleCommandConnector(
              byteArrayOf(
                binding.shutdownCurrent1Sb.progress.toByte(),
                binding.shutdownCurrent2Sb.progress.toByte(),
                binding.shutdownCurrent3Sb.progress.toByte(),
                binding.shutdownCurrent4Sb.progress.toByte(),
                binding.shutdownCurrent5Sb.progress.toByte(),
                binding.shutdownCurrent6Sb.progress.toByte()
              ), SHUTDOWN_CURRENT_NEW, WRITE, 0
            )
          }
          saveInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_2, seekBar.progress)
          RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(SHUTDOWN_CURRENT_NEW_VM)
        }
      }
    })

    binding.shutdownCurrent3Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        binding.shutdownCurrent3Tv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(
              binding.shutdownCurrent1Sb.progress.toByte(),
              binding.shutdownCurrent2Sb.progress.toByte(),
              binding.shutdownCurrent3Sb.progress.toByte(),
              binding.shutdownCurrent4Sb.progress.toByte(),
              binding.shutdownCurrent5Sb.progress.toByte(),
              binding.shutdownCurrent6Sb.progress.toByte()),
              SHUTDOWN_CURRENT_NEW_VM, countRestart)
          }
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
            main?.bleCommandConnector(
              byteArrayOf(
                binding.shutdownCurrent1Sb.progress.toByte(),
                binding.shutdownCurrent2Sb.progress.toByte(),
                binding.shutdownCurrent3Sb.progress.toByte(),
                binding.shutdownCurrent4Sb.progress.toByte(),
                binding.shutdownCurrent5Sb.progress.toByte(),
                binding.shutdownCurrent6Sb.progress.toByte()
              ), SHUTDOWN_CURRENT_NEW, WRITE, 0
            )
          }
          saveInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_3, seekBar.progress)
          RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(SHUTDOWN_CURRENT_NEW_VM)
        }
      }
    })
    binding.shutdownCurrent4Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        binding.shutdownCurrent4Tv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(
              binding.shutdownCurrent1Sb.progress.toByte(),
              binding.shutdownCurrent2Sb.progress.toByte(),
              binding.shutdownCurrent3Sb.progress.toByte(),
              binding.shutdownCurrent4Sb.progress.toByte(),
              binding.shutdownCurrent5Sb.progress.toByte(),
              binding.shutdownCurrent6Sb.progress.toByte()),
              SHUTDOWN_CURRENT_NEW_VM, countRestart)
          }
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
            main?.bleCommandConnector(
              byteArrayOf(
                binding.shutdownCurrent1Sb.progress.toByte(),
                binding.shutdownCurrent2Sb.progress.toByte(),
                binding.shutdownCurrent3Sb.progress.toByte(),
                binding.shutdownCurrent4Sb.progress.toByte(),
                binding.shutdownCurrent5Sb.progress.toByte(),
                binding.shutdownCurrent6Sb.progress.toByte()
              ), SHUTDOWN_CURRENT_NEW, WRITE, 0
            )
          }
          saveInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_4, seekBar.progress)
          RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(SHUTDOWN_CURRENT_NEW_VM)
        }
      }
    })
    binding.shutdownCurrent5Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        binding.shutdownCurrent5Tv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(
              binding.shutdownCurrent1Sb.progress.toByte(),
              binding.shutdownCurrent2Sb.progress.toByte(),
              binding.shutdownCurrent3Sb.progress.toByte(),
              binding.shutdownCurrent4Sb.progress.toByte(),
              binding.shutdownCurrent5Sb.progress.toByte(),
              binding.shutdownCurrent6Sb.progress.toByte()),
              SHUTDOWN_CURRENT_NEW_VM, countRestart)
          }
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
            main?.bleCommandConnector(
              byteArrayOf(
                binding.shutdownCurrent1Sb.progress.toByte(),
                binding.shutdownCurrent2Sb.progress.toByte(),
                binding.shutdownCurrent3Sb.progress.toByte(),
                binding.shutdownCurrent4Sb.progress.toByte(),
                binding.shutdownCurrent5Sb.progress.toByte(),
                binding.shutdownCurrent6Sb.progress.toByte()
              ), SHUTDOWN_CURRENT_NEW, WRITE, 0
            )
          }
          saveInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_5, seekBar.progress)
          RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(SHUTDOWN_CURRENT_NEW_VM)
        }
      }
    })
    binding.shutdownCurrent6Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        binding.shutdownCurrent6Tv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(
              binding.shutdownCurrent1Sb.progress.toByte(),
              binding.shutdownCurrent2Sb.progress.toByte(),
              binding.shutdownCurrent3Sb.progress.toByte(),
              binding.shutdownCurrent4Sb.progress.toByte(),
              binding.shutdownCurrent5Sb.progress.toByte(),
              binding.shutdownCurrent6Sb.progress.toByte()),
              SHUTDOWN_CURRENT_NEW_VM, countRestart)
          }
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
            main?.bleCommandConnector(
              byteArrayOf(
                binding.shutdownCurrent1Sb.progress.toByte(),
                binding.shutdownCurrent2Sb.progress.toByte(),
                binding.shutdownCurrent3Sb.progress.toByte(),
                binding.shutdownCurrent4Sb.progress.toByte(),
                binding.shutdownCurrent5Sb.progress.toByte(),
                binding.shutdownCurrent6Sb.progress.toByte()
              ), SHUTDOWN_CURRENT_NEW, WRITE, 0
            )
          }
          saveInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_6, seekBar.progress)
          RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(SHUTDOWN_CURRENT_NEW_VM)
        }
      }
    })


    binding.swapOpenCloseSw.setOnClickListener {
      if (binding.swapOpenCloseSw.isChecked) {
        binding.swapOpenCloseTv.text = resources.getString(R.string.on_sw)
        main?.setSwapOpenCloseButton(true)
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, true)
      } else {
        binding.swapOpenCloseTv.text = resources.getString(R.string.off_sw)
        main?.setSwapOpenCloseButton(false)
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)
      }
    }


    binding.singleChannelControlSw.setOnClickListener {
      if (!main?.lockWriteBeforeFirstRead!!) {
        if (binding.singleChannelControlSw.isChecked) {
          binding.singleChannelControlTv.text = resources.getString(R.string.on_sw)
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(0x01), SET_ONE_CHANNEL_NEW_VM, countRestart)
          } else {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
              main?.runWriteData(byteArrayOf(0x01), SET_ONE_CHANNEL_NEW, WRITE)
            } else {
              main?.bleCommandConnector(byteArrayOf(0x01), SET_ONE_CHANNEL, WRITE, 16)
            }
          }
          main?.setOneChannelNum = 1
          saveBool(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, true)
        } else {
          binding.singleChannelControlTv.text = resources.getString(R.string.off_sw)
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(0x00), SET_ONE_CHANNEL_NEW_VM, countRestart)
          } else {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
              main?.runWriteData(byteArrayOf(0x00), SET_ONE_CHANNEL_NEW, WRITE)
            } else {
              main?.bleCommandConnector(byteArrayOf(0x00), SET_ONE_CHANNEL, WRITE, 16)
            }
          }
          main?.setOneChannelNum = 0
          saveBool(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)
        }
      }
    }


    binding.onOffProsthesesBlockingSw.setOnClickListener {
      if (!main?.lockWriteBeforeFirstRead!!) {
        if (binding.onOffProsthesesBlockingSw.isChecked) {
          binding.onOffProsthesesBlockingTv.text = resources.getString(R.string.on_sw)
          lockProstheses = 0x01
          binding.holdToLockTimeRl.visibility = View.VISIBLE
          sendGestureRotation()
        } else {
          binding.onOffProsthesesBlockingTv.text = resources.getString(R.string.off_sw)
          lockProstheses = 0x00
          binding.holdToLockTimeRl.visibility = View.GONE
          sendGestureRotation()
        }
        saveBool(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_LOCK_NUM, binding.onOffProsthesesBlockingSw.isChecked)
      }
    }
    binding.holdToLockTimeSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        val time: String = when {
          ((binding.holdToLockTimeSb.progress?.plus(1))?.times(0.1)).toString().length == 4 -> {
            ((binding.holdToLockTimeSb.progress?.plus(1))?.times(0.1)).toString() + "c"
          }
          ((binding.holdToLockTimeSb.progress?.plus(1))?.times(0.1)).toString().length > 4 -> {
            ((binding.holdToLockTimeSb.progress?.plus(1))?.times(0.1)).toString().substring(0, 4) + "c"
          }
          else -> {
            ((binding.holdToLockTimeSb.progress?.plus(1))?.times(0.1)).toString() + "0c"
          }
        }
        binding.holdToLockTimeTv.text = time
      }
      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          main?.stage = "advanced activity"
          sendGestureRotation()
          saveInt(main?.mDeviceAddress + PreferenceKeys.HOLD_TO_LOCK_TIME_NUM, seekBar.progress)
        }
      }
    })


    binding.onOffSensorGestureSwitchingSw.setOnClickListener {
      if (!main?.lockWriteBeforeFirstRead!!) {
        if (binding.onOffSensorGestureSwitchingSw.isChecked) {
          binding.onOffSensorGestureSwitchingTv.text = resources.getString(R.string.on_sw)
          sensorGestureSwitching = 0x01
          binding.peakTimeRl.visibility = View.VISIBLE
          if (mode.toInt() == 0) binding.downtimeRl.visibility = View.VISIBLE
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
            binding.downtimeRl.visibility = View.GONE
            binding.peakTimeRl.visibility = View.GONE
            binding.modeNewRl.visibility = View.GONE
            binding.peakTimeVmRl.visibility = View.VISIBLE
            sendGestureRotation()
            System.err.println("sendGestureRotation FEST_X")
          } else {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
              System.err.println("sendGestureRotation FEST_H")
              binding.modeNewRl.visibility = View.VISIBLE
              main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode, binding.peakTimeSb.progress.toByte(), binding.downtimeSb.progress.toByte()), ROTATION_GESTURE_NEW, WRITE)
            } else {
              System.err.println("sendGestureRotation не то и не то")
              binding.modeRl.visibility = View.VISIBLE
              main?.bleCommandConnector(byteArrayOf(sensorGestureSwitching, mode, (binding.peakTimeSb.progress?.plus(5))?.toByte()!!, (binding.downtimeSb.progress?.plus(5))?.toByte()!!),
                ROTATION_GESTURE_NEW, WRITE, 17)
            }
          }
          saveBool(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, true)
        } else {
          binding.onOffSensorGestureSwitchingTv.text = resources.getString(R.string.off_sw)
          sensorGestureSwitching = 0x00
          binding.peakTimeRl.visibility = View.GONE
          binding.downtimeRl.visibility = View.GONE
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
            binding.modeNewRl.visibility = View.GONE
            binding.peakTimeVmRl.visibility = View.GONE
            System.err.println("sendGestureRotation FEST_X")
            sendGestureRotation()
          } else {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
              System.err.println("sendGestureRotation FEST_H")
              binding.modeNewRl.visibility = View.GONE
              main?.runWriteData(
                byteArrayOf(
                  sensorGestureSwitching,
                  mode,
                  binding.peakTimeSb.progress.toByte(),
                  binding.downtimeSb.progress.toByte()
                ), ROTATION_GESTURE_NEW, WRITE
              )
            } else {
              System.err.println("sendGestureRotation не то и не то")
              binding.modeRl.visibility = View.GONE
              main?.bleCommandConnector(
                byteArrayOf(
                  sensorGestureSwitching,
                  mode,
                  (binding.peakTimeSb.progress?.plus(5))?.toByte()!!,
                  (binding.downtimeSb.progress?.plus(5))?.toByte()!!
                ),
                ROTATION_GESTURE_NEW, WRITE, 17
              )
            }
          }
          saveBool(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false)
        }
      }
    }


    binding.modeSw.setOnClickListener {
      if (!main?.lockWriteBeforeFirstRead!!) {
        if (binding.modeSw.isChecked) {
          binding.modeTv.text = "двумя\nдатчиками"
          mode = 0x01
          binding.downtimeRl.visibility = View.GONE
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H) || main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(sensorGestureSwitching, mode, binding.peakTimeSb.progress.toByte(), binding.downtimeSb.progress.toByte()), ROTATION_GESTURE_NEW, countRestart)
          } else {
            main?.bleCommandConnector(byteArrayOf(sensorGestureSwitching, mode, (binding.peakTimeSb.progress?.plus(5))?.toByte()!!, (binding.downtimeSb.progress?.plus(5))?.toByte()!!),
              ROTATION_GESTURE_NEW, WRITE, 17)
          }
          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, true)
        } else {
          binding.modeTv.text = "одним\nдатчиком"
          mode = 0x00
          binding.downtimeRl.visibility = View.VISIBLE
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H) || main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(sensorGestureSwitching, mode, binding.peakTimeSb.progress.toByte(), binding.downtimeSb.progress.toByte()), ROTATION_GESTURE_NEW, countRestart)
          } else {
            main?.bleCommandConnector(byteArrayOf(sensorGestureSwitching, mode, (binding.peakTimeSb.progress?.plus(5))?.toByte()!!, (binding.downtimeSb.progress?.plus(5))?.toByte()!!),
              ROTATION_GESTURE_NEW, WRITE, 17)
          }
          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, false)
        }
      }
    }

//    binding.modeNewSw.setOnSwitchListener { position, _ ->
//      if (position == 0) {
////        Toast.makeText(main?.baseContext,  "0", Toast.LENGTH_SHORT).show()
//        saveInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NEW_NUM, 0)
//        mode = 0
//        if (sensorGestureSwitching.toInt() == 1) binding.downtimeRl.visibility = View.VISIBLE
//        main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode, binding.peakTimeSb.progress.toByte(), binding.downtimeSb.progress.toByte()), ROTATION_GESTURE_NEW, WRITE)
//      }
//      if (position == 1) {
////        Toast.makeText(main?.baseContext, "1", Toast.LENGTH_SHORT).show()
//        saveInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NEW_NUM, 1)
//        mode = 1
//        binding.downtimeRl.visibility = View.GONE
//        main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode, binding.peakTimeSb.progress.toByte(), binding.downtimeSb.progress.toByte()), ROTATION_GESTURE_NEW, WRITE)
//      }
//      if (position == 2) {
////        Toast.makeText(main?.baseContext, "2", Toast.LENGTH_SHORT).show()
//        saveInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NEW_NUM, 2)
//        mode = 2
//        binding.downtimeRl.visibility = View.GONE
//        main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode, binding.peakTimeSb.progress.toByte(), binding.downtimeSb.progress.toByte()), ROTATION_GESTURE_NEW, WRITE)
//      }
//    }
//    binding.modeNewSw.selectedTab = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NEW_NUM, 0)

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
        if (!main?.lockWriteBeforeFirstRead!!) {
          sendGestureRotation()
          saveInt(main?.mDeviceAddress + PreferenceKeys.SET_PEAK_TIME_VM_NUM, seekBar.progress)
        }
      }
    })

    binding.peakTimeSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        var time: String = when {
          ((seekBar.progress + 5) * 0.05).toString().length == 4 -> {
            ((seekBar.progress + 5) * 0.05).toString() + "c"
          }
          ((seekBar.progress + 5) * 0.05).toString().length > 4 -> {
            ((seekBar.progress + 5) * 0.05).toString().substring(0,4) + "c"
          }
          else -> {
            ((seekBar.progress + 5) * 0.05).toString() + "0c"
          }
        }
        if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
          time = (binding.peakTimeSb.progress?.times(0.04)).toString() + "c"
        }
        binding.peakTimeTv.text = time
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        var time: String = when {
          ((seekBar.progress + 5) * 0.05).toString().length == 4 -> {
            ((seekBar.progress + 5) * 0.05).toString() + "c"
          }
          ((seekBar.progress + 5) * 0.05).toString().length > 4 -> {
            ((seekBar.progress + 5) * 0.05).toString().substring(0,4) + "c"
          }
          else -> {
            ((seekBar.progress + 5) * 0.05).toString() + "0c"
          }
        }
        if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
          time = (binding.peakTimeSb.progress?.times(0.04)).toString() + "c"
        }
        binding.peakTimeTv.text = time
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
            main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode,
              binding.peakTimeSb.progress.toByte(), binding.downtimeSb.progress.toByte()
            ), ROTATION_GESTURE_NEW, WRITE)
          } else {
            main?.bleCommandConnector(byteArrayOf(0x00, sensorGestureSwitching, mode, (binding.peakTimeSb.progress?.plus(5))?.toByte()!!, (binding.downtimeSb.progress?.plus(5))?.toByte()!!),
              ROTATION_GESTURE_NEW, WRITE, 17)
          }
          preferenceManager.putInt(main?.mDeviceAddress + PreferenceKeys.SET_PEAK_TIME_NUM, seekBar.progress)
        }
      }
    })

    binding.downtimeSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        var time: String = when {
          ((seekBar.progress + 5) * 0.05).toString().length == 4 -> {
            ((seekBar.progress + 5) * 0.05).toString() + "c"
          }
          ((seekBar.progress + 5) * 0.05).toString().length > 4 -> {
            ((seekBar.progress + 5) * 0.05).toString().substring(0,4) + "c"
          }
          else -> {
            ((seekBar.progress + 5) * 0.05).toString() + "0c"
          }
        }
        if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
          time = (binding.downtimeSb.progress?.times(0.04)).toString() + "c"
        }
        binding.downtimeTv.text = time
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        var time: String = when {
          ((seekBar.progress + 5) * 0.05).toString().length == 4 -> {
            ((seekBar.progress + 5) * 0.05).toString() + "c"
          }
          ((seekBar.progress + 5) * 0.05).toString().length > 4 -> {
            ((seekBar.progress + 5) * 0.05).toString().substring(0,4) + "c"
          }
          else -> {
            ((seekBar.progress + 5) * 0.05).toString() + "0c"
          }
        }
        if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
          time = (binding.downtimeSb.progress?.times(0.04)).toString() + "c"
        }
        binding.downtimeTv.text = time
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
            main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode,
              binding.peakTimeSb.progress.toByte(), binding.downtimeSb.progress.toByte()
            ), ROTATION_GESTURE_NEW, WRITE)
          } else {
            main?.bleCommandConnector(byteArrayOf(0x00, sensorGestureSwitching, mode, (binding.peakTimeSb.progress?.plus(5))?.toByte()!!, (binding.downtimeSb.progress?.plus(5))?.toByte()!!),
              ROTATION_GESTURE_NEW, WRITE, 17)
          }
          preferenceManager.putInt(main?.mDeviceAddress + PreferenceKeys.SET_DOWNTIME_NUM, seekBar.progress)
        }
      }
    })


    binding.getSetupBtn.setOnClickListener {
      if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
        main?.bleCommandConnector(byteArrayOf(0x00), SERIAL_NUMBER_NEW, READ, 17)
      }
      if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
        main?.bleCommandConnector(byteArrayOf(0x00), SERIAL_NUMBER_NEW_VM, READ, 17)
      }
      main?.lockChangeSerialNumber = true
    }

    binding.setSetupBtn.setOnClickListener {
      //TODO тут можно отключать валидацию отправляемого серийного номера
      if (validationAndConversionSerialNumber(binding.serialNumberEt.text.toString()) != "false") {
        if (!mSettings!!.getBoolean(PreferenceKeys.ENTER_SECRET_PIN, false)) {
          main?.showPinCodeDialog(validationAndConversionSerialNumber(binding.serialNumberEt.text.toString()))
        } else {
          main?.showSetSerialNumberDialog(validationAndConversionSerialNumber(binding.serialNumberEt.text.toString()))
        }
      } else {
        main?.showToast(validationError)
      }

//      main?.showSetSerialNumberDialog(binding.serialNumberEt.text.toString())
    }
    main?.serialNumber = binding.serialNumberEt.text.toString()

    binding.leftRightSideSwapSw.setOnClickListener{
      showAlertChangeSideDialog()
    }

    binding.timeDelayOfFingersSwapSw.setOnClickListener {
      if (binding.timeDelayOfFingersSwapSw.isChecked) {
        System.err.println("time_delay_of_fingers_swap_sw 1")
        binding.timeDelayOfFingersSwapTv.text = resources.getString(R.string.on_sw)
        saveInt(main?.mDeviceAddress + PreferenceKeys.SET_FINGERS_DELAY, 1)
      } else {
        System.err.println("time_delay_of_fingers_swap_sw 0")
        binding.timeDelayOfFingersSwapTv.text = resources.getString(R.string.off_sw)
        saveInt(main?.mDeviceAddress + PreferenceKeys.SET_FINGERS_DELAY, 0)
      }
    }

    binding.smartConnectionSwapSw.setOnClickListener {
      if (binding.smartConnectionSwapSw.isChecked) {
        binding.smartConnectionSwapTv.text = resources.getString(R.string.on_sw)
        saveBool(PreferenceKeys.SET_MODE_SMART_CONNECTION, true)
      } else {
        binding.smartConnectionSwapTv.text = resources.getString(R.string.off_sw)
        saveBool(PreferenceKeys.SET_MODE_SMART_CONNECTION, false)
      }
    }


    binding.activeGesturesSwapSw.setOnClickListener {
      var numActiveGestures = 8
      if (binding.activeGesturesSwapSw.isChecked) {
        numActiveGestures = 14
        binding.activeGesturesSwapTv.text = numActiveGestures.toString()
        saveInt(main?.mDeviceAddress + PreferenceKeys.NUM_ACTIVE_GESTURES, numActiveGestures)
        RxUpdateMainEvent.getInstance().updateUIGestures(100)
      } else {
        binding.activeGesturesSwapTv.text = numActiveGestures.toString()
        saveInt(main?.mDeviceAddress + PreferenceKeys.NUM_ACTIVE_GESTURES, numActiveGestures)
        //ограничиваем диапазон старт/стопа ргуппы ротации
        if (startGestureInLoopNum >= numActiveGestures) {
          startGestureInLoopNum = (numActiveGestures - 1)
          saveInt(main?.mDeviceAddress + PreferenceKeys.START_GESTURE_IN_LOOP, startGestureInLoopNum)
        }
        if (endGestureInLoopNum >= numActiveGestures) {
          endGestureInLoopNum = (numActiveGestures - 1)
          saveInt(main?.mDeviceAddress + PreferenceKeys.END_GESTURE_IN_LOOP, endGestureInLoopNum)
        }
        //если активный жест больше 8 то он устанавливается на 8
        val activeGesture = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, 1)
        if (activeGesture >= numActiveGestures) {
          saveInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, numActiveGestures)
          RxUpdateMainEvent.getInstance().updateUIGestures(numActiveGestures)
          //отправлять используемый жест
          main?.runSendCommand(byteArrayOf((numActiveGestures - 1).toByte()), SET_GESTURE_NEW_VM, countRestart)
        } else {
          RxUpdateMainEvent.getInstance().updateUIGestures(100)
        }
      }
      RxUpdateMainEvent.getInstance().updateUIChart(true)
      sendActiveGestures(numActiveGestures)
      sendGestureRotation()
    }
    binding.gesturePsv.setOnSpinnerItemSelectedListener<String> { _, _, newIndex, _ ->
      val numActiveGestures: Int
      when (newIndex) {
        0 -> {
          numActiveGestures = 7
          saveInt(main?.mDeviceAddress + PreferenceKeys.NUM_ACTIVE_GESTURES, numActiveGestures)
          //ограничиваем диапазон старт/стопа ргуппы ротации
          if (startGestureInLoopNum >= numActiveGestures) {
            startGestureInLoopNum = (numActiveGestures - 1)
            saveInt(main?.mDeviceAddress + PreferenceKeys.START_GESTURE_IN_LOOP, startGestureInLoopNum)
          }
          if (endGestureInLoopNum >= numActiveGestures) {
            endGestureInLoopNum = (numActiveGestures - 1)
            saveInt(main?.mDeviceAddress + PreferenceKeys.END_GESTURE_IN_LOOP, endGestureInLoopNum)
          }
          //если активный жест больше 7 то он устанавливается на 7
          val activeGesture = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, 1)
          if (activeGesture >= numActiveGestures) {
            saveInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, numActiveGestures)
            RxUpdateMainEvent.getInstance().updateUIGestures(numActiveGestures)
            //отправлять используемый жест
            main?.runSendCommand(byteArrayOf((numActiveGestures - 1).toByte()), SET_GESTURE_NEW_VM, countRestart)
          } else {
            RxUpdateMainEvent.getInstance().updateUIGestures(100)
          }
        }
        1 -> {
          numActiveGestures = 13
          saveInt(main?.mDeviceAddress + PreferenceKeys.NUM_ACTIVE_GESTURES, numActiveGestures)
          //ограничиваем диапазон старт/стопа ргуппы ротации
          if (startGestureInLoopNum >= numActiveGestures) {
            startGestureInLoopNum = (numActiveGestures - 1)
            saveInt(main?.mDeviceAddress + PreferenceKeys.START_GESTURE_IN_LOOP, startGestureInLoopNum)
          }
          if (endGestureInLoopNum >= numActiveGestures) {
            endGestureInLoopNum = (numActiveGestures - 1)
            saveInt(main?.mDeviceAddress + PreferenceKeys.END_GESTURE_IN_LOOP, endGestureInLoopNum)
          }
          //если активный жест больше 13 то он устанавливается на 13
          val activeGesture = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, 1)
          if (activeGesture >= numActiveGestures) {
            saveInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, numActiveGestures)
            RxUpdateMainEvent.getInstance().updateUIGestures(numActiveGestures)
            //отправлять используемый жест
            main?.runSendCommand(byteArrayOf((numActiveGestures - 1).toByte()), SET_GESTURE_NEW_VM, countRestart)
          } else {
            RxUpdateMainEvent.getInstance().updateUIGestures(100)
          }
        }
        2 -> {
          numActiveGestures = 23
          saveInt(main?.mDeviceAddress + PreferenceKeys.NUM_ACTIVE_GESTURES, numActiveGestures)
          //ограничиваем диапазон старт/стопа ргуппы ротации
          if (startGestureInLoopNum >= numActiveGestures) {
            startGestureInLoopNum = (numActiveGestures - 1)
            saveInt(main?.mDeviceAddress + PreferenceKeys.START_GESTURE_IN_LOOP, startGestureInLoopNum)
          }
          if (endGestureInLoopNum >= numActiveGestures) {
            endGestureInLoopNum = (numActiveGestures - 1)
            saveInt(main?.mDeviceAddress + PreferenceKeys.END_GESTURE_IN_LOOP, endGestureInLoopNum)
          }
          //если активный жест больше 23 то он устанавливается на 23
          val activeGesture = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, 1)
          if (activeGesture >= numActiveGestures) {
            saveInt(main?.mDeviceAddress + PreferenceKeys.SELECT_GESTURE_NUM, numActiveGestures)
            RxUpdateMainEvent.getInstance().updateUIGestures(numActiveGestures)
            //отправлять используемый жест
            main?.runSendCommand(byteArrayOf((numActiveGestures - 1).toByte()), SET_GESTURE_NEW_VM, countRestart)
          } else {
            RxUpdateMainEvent.getInstance().updateUIGestures(100)
          }
        }
        3 -> {
          numActiveGestures = 25
          saveInt(main?.mDeviceAddress + PreferenceKeys.NUM_ACTIVE_GESTURES, numActiveGestures)
          RxUpdateMainEvent.getInstance().updateUIGestures(100)
        }
        else -> {numActiveGestures = 7}
      }
      RxUpdateMainEvent.getInstance().updateUIChart(true)
      if (sendFlag) {
        sendActiveGestures(numActiveGestures)
        sendGestureRotation()
      } else {
        sendFlag = true
      }
    }


    binding.activeAppPinCodeSwapSw.setOnClickListener {
      if (binding.activeAppPinCodeSwapSw.isChecked) {
        binding.appPinCodeSwapTv.text = resources.getString(R.string.on_sw)
        main?.saveBool(PreferenceKeys.USE_APP_PIN_CODE, true)
        showPinCodeAppDialog()
      } else {
        binding.appPinCodeSwapTv.text = resources.getString(R.string.off_sw)
        main?.saveBool(PreferenceKeys.USE_APP_PIN_CODE, false)
      }
    }

    binding.resetToFactorySettingsBtn.setOnClickListener {
      if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
        main?.showSoftResetDialog()
      } else {
        main?.showHardResetDialog()
      }
    }
    binding.calibrationAdvBtn.setOnClickListener { main?.showCalibrationDialog() }
    binding.calibrationStatusAdvBtn.setOnClickListener {
      if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
        main?.runReadDataAllCharacteristics(STATUS_CALIBRATION_NEW_VM)
      } else {
        if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
          main?.runReadDataAllCharacteristics(STATUS_CALIBRATION_NEW)
        }
      }
    }

    binding.debugScreenBtn.setOnClickListener {
      val intent = Intent(context, GripperTestScreenWithEncodersActivity::class.java)//context
      main!!.setDebugScreenIsOpen(true)
      startActivity(intent)
    }

    binding.testConnectionBtn.setOnClickListener{
      //start communication test
      main?.openTestConnectionProgressDialog()
      main?.testingConnection = true
      main?.runSendCommand(byteArrayOf(0x01), ROTATION_GESTURE_NEW_VM, countRestart)
      main?.runSendCommand(byteArrayOf(0x02), ROTATION_GESTURE_NEW_VM, countRestart)
      main?.runSendCommand(byteArrayOf(0x03), ROTATION_GESTURE_NEW_VM, countRestart)
      main?.runSendCommand(byteArrayOf(0x04), ROTATION_GESTURE_NEW_VM, countRestart)
      main?.runSendCommand(byteArrayOf(0x05), ROTATION_GESTURE_NEW_VM, countRestart)
      main?.runSendCommand(byteArrayOf(0x11), ROTATION_GESTURE_NEW_VM, countRestart)
      main?.runSendCommand(byteArrayOf(0x12), ROTATION_GESTURE_NEW_VM, countRestart)
      main?.runSendCommand(byteArrayOf(0x13), ROTATION_GESTURE_NEW_VM, countRestart)
      main?.runSendCommand(byteArrayOf(0x14), ROTATION_GESTURE_NEW_VM, countRestart)
      main?.runSendCommand(byteArrayOf(0x15), ROTATION_GESTURE_NEW_VM, countRestart)
      //end
    }

    //Скрывает настройки, которые не актуальны для многосхватной бионики
    if ( main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_A)
      || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_BT05)
      || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_MY_IPHONE)
      || main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)
      || main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
      binding.shutdownCurrentRl.visibility = View.GONE
    }
    //Скрывает настройки, которые не актуальны для бионик кроме FEST-H и FEST-X
    when {
        main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X) -> {
          binding.serialRl.visibility = View.VISIBLE
          binding.onOffProsthesesBlockingRl.visibility = View.VISIBLE
          binding.testConnectionRl.visibility = View.VISIBLE
          binding.scaleTv.visibility = View.GONE
          binding.EMGModeRl.visibility = View.GONE
          binding.appPinCodeRl.visibility = View.GONE
        }
        main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H) -> {
          binding.EMGModeRl.visibility = View.GONE
          binding.serialRl.visibility = View.VISIBLE
          binding.debugScreenRl.visibility = View.GONE
          binding.scaleTv.visibility = View.GONE
          binding.activeGesturesRl.visibility = View.GONE
          binding.appPinCodeRl.visibility = View.GONE
        }
        else -> {
          binding.EMGModeRl.visibility = View.GONE
          //TODO кнопка дебагинга в INDY для доступа к секретным настройкам не совсем годится, потому что там модель FEST-H (если закомментить, то будет кнопка)
          binding.debugScreenRl.visibility = View.GONE
          binding.appPinCodeRl.visibility = View.GONE

          //для тестовой версии приложения ИНДИ
//          binding.swapOpenCloseRl.visibility = View.GONE
//          binding.singleChannelControlRl.visibility = View.GONE
//          binding.smartConnectionRl.visibility = View.GONE
//          binding.resetToFactorySettingsRl.visibility = View.GONE

//          binding.serialRl.visibility = View.GONE
          binding.calibrationRl.visibility = View.GONE
          binding.shutdownCurrent1Rl.visibility = View.GONE
          binding.shutdownCurrent2Rl.visibility = View.GONE
          binding.shutdownCurrent3Rl.visibility = View.GONE
          binding.shutdownCurrent4Rl.visibility = View.GONE
          binding.shutdownCurrent5Rl.visibility = View.GONE
          binding.shutdownCurrent6Rl.visibility = View.GONE
          binding.sideRl.visibility = View.GONE
          binding.onOffSensorGestureSwitchingRl.visibility = View.GONE
          binding.activeGesturesRl.visibility = View.GONE
        }
    }

    binding.swapOpenCloseSw.isChecked = preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)
    binding.singleChannelControlSw.isChecked = mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)

    //только для FEST-X введены различные еровни ресетов
    if (!main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
      binding.resetToFactorySettingsBtn.text = getString(R.string.full_reset)
    }
    if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
      preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, false)
    }
    binding.modeSw.isChecked = preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, false)
    if (preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)) binding.swapOpenCloseTv.text = resources.getString(R.string.on_sw)
    if (mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)) binding.singleChannelControlTv.text = resources.getString(R.string.on_sw)


    if (preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, false)) {
      binding.modeTv.text = "двумя\nдатчиками"
      mode = 0x01
      binding.downtimeRl.visibility = View.GONE
    } else {
      binding.modeTv.text = "одним\nдатчиком"
      mode = 0x00
    }

    val versionName = BuildConfig.VERSION_NAME
    binding.versionAppTv.text = (mContext?.resources?.getString(R.string.version_app) ?: "lol: ") + " " + versionName
  }
  private fun enableInterface (enabled: Boolean) {
    binding.EMGModeSwapPsv.isEnabled = enabled
    binding.swapOpenCloseSw.isEnabled = enabled
    binding.singleChannelControlSw.isEnabled = enabled
    binding.resetToFactorySettingsBtn.isEnabled = enabled
    binding.getSetupBtn.isEnabled = enabled
    binding.setSetupBtn.isEnabled = enabled
    binding.shutdownCurrentSb.isEnabled = enabled
    binding.shutdownCurrent1Sb.isEnabled = enabled
    binding.shutdownCurrent2Sb.isEnabled = enabled
    binding.shutdownCurrent3Sb.isEnabled = enabled
    binding.shutdownCurrent4Sb.isEnabled = enabled
    binding.shutdownCurrent5Sb.isEnabled = enabled
    binding.shutdownCurrent6Sb.isEnabled = enabled
    binding.swapOpenCloseSw.isEnabled = enabled
    binding.singleChannelControlSw.isEnabled = enabled
    binding.onOffSensorGestureSwitchingSw.isEnabled = enabled
    binding.onOffProsthesesBlockingSw.isEnabled = enabled
    binding.holdToLockTimeSb.isEnabled = enabled
    binding.peakTimeVmSb.isEnabled = enabled
    binding.getSetupBtn.isEnabled = enabled
    binding.setSetupBtn.isEnabled = enabled
    binding.leftRightSideSwapSw.isEnabled = enabled
    binding.timeDelayOfFingersSwapSw.isEnabled = enabled
    binding.smartConnectionSwapSw.isEnabled = enabled
    binding.activeGesturesSwapSw.isEnabled = enabled
    binding.activeAppPinCodeSwapSw.isEnabled = enabled
    binding.resetToFactorySettingsBtn.isEnabled = enabled
    binding.calibrationAdvBtn.isEnabled = enabled
    binding.calibrationStatusAdvBtn.isEnabled = enabled
    binding.debugScreenBtn.isEnabled = enabled
    binding.testConnectionBtn.isEnabled = enabled
    binding.gesturePsv.isEnabled = enabled
    if (checkDriverVersionGreaterThan237() || checkINDYCanControlEMGModes()) {
      binding.EMGModeRl.visibility = View.VISIBLE
      when (mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_EMG_SENSORS,9)) {
        9 -> {
          sendFlag = false
          binding.EMGModeSwapPsv.selectItemByIndex(0) }
        7 -> {
          sendFlag = false
          binding.EMGModeSwapPsv.selectItemByIndex(1) }
        10 -> {
          sendFlag = false
          binding.EMGModeSwapPsv.selectItemByIndex(2) }
      }
    }
    else { binding.EMGModeRl.visibility = View.GONE }
  }
  @SuppressLint("Recycle", "SetTextI18n")
  private fun updateAllParameters() {
    main?.runOnUiThread {
      System.err.println("Принятые данные состояния токов ОБНОВЛЕНИЕ")

      val compressionForce = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM, 80)
      val minCompressionForce = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.MIN_SHUTDOWN_CURRENT_NUM, 25)
      if (checkINDYCanControlEMGModes()) {
        binding.shutdownCurrentSb.max = 50
        binding.shutdownCurrentTextTv.text = resources.getString(R.string.min_shutdown_current)
        ObjectAnimator.ofInt(binding.shutdownCurrentSb, "progress", minCompressionForce).setDuration(200).start()
        System.err.println("resetUI 50     updateAllParameters")
      } else {
        binding.shutdownCurrentSb.max = 100
        binding.shutdownCurrentTextTv.text = resources.getString(R.string.shutdown_current)
        ObjectAnimator.ofInt(binding.shutdownCurrentSb, "progress", compressionForce).setDuration(200).start()
        System.err.println("resetUI 100  updateAllParameters")
      }

      ObjectAnimator.ofInt(binding.shutdownCurrent1Sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_1, 80)).setDuration(200).start()
      ObjectAnimator.ofInt(binding.shutdownCurrent2Sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_2, 80)).setDuration(200).start()
      ObjectAnimator.ofInt(binding.shutdownCurrent3Sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_3, 80)).setDuration(200).start()
      ObjectAnimator.ofInt(binding.shutdownCurrent4Sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_4, 80)).setDuration(200).start()
      ObjectAnimator.ofInt(binding.shutdownCurrent5Sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_5, 80)).setDuration(200).start()
      ObjectAnimator.ofInt(binding.shutdownCurrent6Sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_6, 80)).setDuration(200).start()


      ObjectAnimator.ofInt(binding.holdToLockTimeSb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.HOLD_TO_LOCK_TIME_NUM, 15)).setDuration(200).start()
      ObjectAnimator.ofInt(binding.peakTimeVmSb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SET_PEAK_TIME_VM_NUM, 15)).setDuration(200).start()
      ObjectAnimator.ofInt(binding.peakTimeSb, "progress", preferenceManager.getInt(main?.mDeviceAddress + PreferenceKeys.SET_PEAK_TIME_NUM, 15)).setDuration(200).start()
      ObjectAnimator.ofInt(binding.downtimeSb, "progress", preferenceManager.getInt(main?.mDeviceAddress + PreferenceKeys.SET_DOWNTIME_NUM, 15)).setDuration(200).start()
    }

    binding.shutdownCurrent1Tv.text = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_1, 80).toString()
    binding.shutdownCurrent2Tv.text = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_2, 80).toString()
    binding.shutdownCurrent3Tv.text = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_3, 80).toString()
    binding.shutdownCurrent4Tv.text = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_4, 80).toString()
    binding.shutdownCurrent5Tv.text = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_5, 80).toString()
    binding.shutdownCurrent6Tv.text = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_6, 80).toString()
    binding.shutdownCurrentTv.text = binding.shutdownCurrentSb.progress.toString()


    current1 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_1, 80)
    current2 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_2, 80)
    current3 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_3, 80)
    current4 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_4, 80)
    current5 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_5, 80)
    current6 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_6, 80)

    if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
      var time: String = when {
        ((binding.peakTimeVmSb.progress?.plus(1))?.times(0.1)).toString().length == 4 -> {
          ((binding.peakTimeVmSb.progress?.plus(1))?.times(0.1)).toString() + "c"
        }
        ((binding.peakTimeVmSb.progress?.plus(1))?.times(0.1)).toString().length > 4 -> {
          ((binding.peakTimeVmSb.progress?.plus(1))?.times(0.1)).toString().substring(0, 4) + "c"
        }
        else -> {
          ((binding.peakTimeVmSb.progress?.plus(1))?.times(0.1)).toString() + "0c"
        }
      }
      binding.peakTimeVmTv.text = time
      time = when {
        ((binding.holdToLockTimeSb.progress?.plus(1))?.times(0.1)).toString().length == 4 -> {
          ((binding.holdToLockTimeSb.progress?.plus(1))?.times(0.1)).toString() + "c"
        }
        ((binding.holdToLockTimeSb.progress?.plus(1))?.times(0.1)).toString().length > 4 -> {
          ((binding.holdToLockTimeSb.progress?.plus(1))?.times(0.1)).toString().substring(0, 4) + "c"
        }
        else -> {
          ((binding.holdToLockTimeSb.progress?.plus(1))?.times(0.1)).toString() + "0c"
        }
      }
      binding.holdToLockTimeTv.text = time

      startGestureInLoopNum = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.START_GESTURE_IN_LOOP, 0)
      endGestureInLoopNum = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.END_GESTURE_IN_LOOP, 0)
      when (mSettings?.getInt(main?.mDeviceAddress + PreferenceKeys.NUM_ACTIVE_GESTURES, 8)) {
        7 -> {
          binding.gesturePsvRl.visibility = View.VISIBLE
          sendFlag = false
          binding.gesturePsv.selectItemByIndex(0) }
        8 -> {
          if (checkDriverVersionGreaterThan240()) {
            binding.gesturePsvRl.visibility = View.VISIBLE
            binding.activeGesturesSwapTv.text = "7"
            saveInt(main?.mDeviceAddress + PreferenceKeys.NUM_ACTIVE_GESTURES, 7)
            sendFlag = false
            binding.gesturePsv.selectItemByIndex(0)
          } else {
            binding.gesturePsvRl.visibility = View.GONE
            binding.activeGesturesSwapSw.isChecked = false
            binding.activeGesturesSwapTv.text = "8"
          }
        }
        13 -> {
          binding.gesturePsvRl.visibility = View.VISIBLE
          sendFlag = false
          binding.gesturePsv.selectItemByIndex(1) }
        14 -> {
          if (checkDriverVersionGreaterThan240()) {
            binding.gesturePsvRl.visibility = View.VISIBLE
            binding.activeGesturesSwapTv.text = "13"
            saveInt(main?.mDeviceAddress + PreferenceKeys.NUM_ACTIVE_GESTURES, 13)
            sendFlag = false
            binding.gesturePsv.selectItemByIndex(1)
          } else {
            binding.gesturePsvRl.visibility = View.GONE
            binding.activeGesturesSwapSw.isChecked = true
            binding.activeGesturesSwapTv.text = "14"
          }
        }
        23 -> {
          binding.gesturePsvRl.visibility = View.VISIBLE
          sendFlag = false
          binding.gesturePsv.selectItemByIndex(2) }
        25 -> {
          binding.gesturePsvRl.visibility = View.VISIBLE
          sendFlag = false
          binding.gesturePsv.selectItemByIndex(3) }
      }
    }
    if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
      var time: String = when {
      ((binding.peakTimeSb.progress?.plus(5))?.times(0.05)).toString().length == 4 -> {
        ((binding.peakTimeSb.progress?.plus(5))?.times(0.05)).toString() + "c"
      }
      ((binding.peakTimeSb.progress?.plus(5))?.times(0.05)).toString().length > 4 -> {
        ((binding.peakTimeSb.progress?.plus(5))?.times(0.05)).toString().substring(0, 4) + "c"
      }
      else -> {
        ((binding.peakTimeSb.progress?.plus(5))?.times(0.05)).toString() + "0c"
      }
    }
//      time = (binding.peakTimeSb.progress?.times(0.04)).toString() + "c"
      binding.peakTimeTv.text = time
      time = when {
        ((binding.downtimeSb.progress?.plus(5))?.times(0.05)).toString().length == 4 -> {
          ((binding.downtimeSb.progress?.plus(5))?.times(0.05)).toString() + "c"
        }
        ((binding.downtimeSb.progress?.plus(5))?.times(0.05)).toString().length > 4 -> {
          ((binding.downtimeSb.progress?.plus(5))?.times(0.05)).toString().substring(0, 4) + "c"
        }
        else -> {
          ((binding.downtimeSb.progress?.plus(5))?.times(0.05)).toString() + "0c"
        }
      }
//      time = (binding.downtimeSb.progress?.times(0.04)).toString() + "c"
      binding.downtimeTv.text = time
    }

    if (mSettings?.getBoolean(PreferenceKeys.USE_APP_PIN_CODE, false) == true) {
      System.err.println("USE_APP_PIN_CODE true")
      binding.appPinCodeSwapTv.text = resources.getString(R.string.on_sw)
      binding.activeAppPinCodeSwapSw.isChecked = true
    } else {
      System.err.println("USE_APP_PIN_CODE false")
      binding.appPinCodeSwapTv.text = resources.getString(R.string.off_sw)
      binding.activeAppPinCodeSwapSw.isChecked = false
    }
    if (mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_LOCK_NUM, false)) {
      binding.onOffProsthesesBlockingSw.isChecked = true
      lockProstheses = 0x01
      binding.onOffProsthesesBlockingTv.text = resources.getString(R.string.on_sw)
      if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
        binding.holdToLockTimeRl.visibility = View.VISIBLE
      }
    } else {
      binding.onOffProsthesesBlockingSw.isChecked = false
      lockProstheses = 0x00
      binding.onOffProsthesesBlockingTv.text = resources.getString(R.string.off_sw)
      binding.holdToLockTimeRl.visibility = View.GONE
    }


    when (mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SET_SCALE, 0)) {
      0 -> { binding.scaleTv.text = resources.getString(R.string.scale) + " S" }
      1 -> { binding.scaleTv.text = resources.getString(R.string.scale) + " M" }
      2 -> { binding.scaleTv.text = resources.getString(R.string.scale) + " L" }
      3 -> { binding.scaleTv.text = resources.getString(R.string.scale) + " XL" }
    }

    binding.singleChannelControlSw.isChecked = mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)
    if (mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)) { binding.singleChannelControlTv.text = resources.getString(R.string.on_sw) } else { binding.singleChannelControlTv.text = resources.getString(R.string.off_sw) }

    binding.onOffSensorGestureSwitchingSw.isChecked = mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false)
    if (mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false)) {
      if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
        binding.peakTimeVmRl.visibility = View.VISIBLE
      }
      if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
        binding.modeNewRl.visibility = View.VISIBLE
        binding.peakTimeRl.visibility = View.VISIBLE
        //в зависимости от выбранного мода показывать тот или иной сетап сикбаров
        when(mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NEW_NUM, 0)) {
          0 -> {
            binding.downtimeRl.visibility = View.VISIBLE
          }
        }

      }
      binding.onOffSensorGestureSwitchingTv.text = resources.getString(R.string.on_sw)
      sensorGestureSwitching = 0x01
    } else {
      binding.onOffSensorGestureSwitchingTv.text = resources.getString(R.string.off_sw)
      sensorGestureSwitching = 0x00
      binding.modeRl.visibility = View.GONE
      binding.peakTimeVmRl.visibility = View.GONE
      binding.peakTimeRl.visibility = View.GONE
      binding.downtimeRl.visibility = View.GONE
    }
  }
  @SuppressLint("InflateParams", "SetTextI18n", "StringFormatInvalid")
  private fun showAlertChangeSideDialog() {
    val dialogBinding = layoutInflater.inflate(R.layout.dialog_chage_hand_side, null)
    val myDialog = Dialog(requireContext())
    myDialog.setContentView(dialogBinding)
    myDialog.setCancelable(false)
    myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    myDialog.show()


    val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_change_hand_side_confirm)
    yesBtn.setOnClickListener {
      if (binding.leftRightSideSwapSw.isChecked) {
        binding.leftRightSideSwapTv.text = resources.getString(R.string.right)
        saveInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1)
      } else {
        binding.leftRightSideSwapTv.text = resources.getString(R.string.left)
        saveInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 0)

      }


      if (mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1) == 1) {
        main?.stage = "advanced activity"
        main?.runSendCommand(byteArrayOf(0x09), CALIBRATION_NEW_VM, countRestart)
      } else {
        main?.stage = "advanced activity"
        main?.runSendCommand(byteArrayOf(0x0a), CALIBRATION_NEW_VM, countRestart)
      }

      myDialog.dismiss()
    }


    val noBtn = dialogBinding.findViewById<View>(R.id.dialog_change_hand_side_cancel)
    noBtn.setOnClickListener {
      if (mSettings?.getInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1) == 1) {
        binding.leftRightSideSwapSw.isChecked = true
        binding.leftRightSideSwapTv.text = Html.fromHtml(getString(R.string.right))
      } else {
        binding.leftRightSideSwapSw.isChecked = false
        binding.leftRightSideSwapTv.text = resources.getString(R.string.left)
      }
      myDialog.dismiss()
    }
  }
  @SuppressLint("InflateParams")
  @Suppress("DEPRECATION")
  private fun showPinCodeAppDialog () {
    val dialogBinding = layoutInflater.inflate(R.layout.dialog_enter_app_pin, null)
    val myDialog = Dialog(requireContext())
    myDialog.setContentView(dialogBinding)
    myDialog.setCancelable(false)
    myDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    myDialog.show()

    val enterAppPinDialogEt = dialogBinding.findViewById<EditText>(R.id.enter_app_pin_dialog_et)

    Handler().postDelayed({
      enterAppPinDialogEt.showKeyboard()
      enterAppPinDialogEt.requestFocus()
      enterAppPinDialogEt.isFocusableInTouchMode = true
    }, 200)

    val yesBtn = dialogBinding.findViewById<View>(R.id.dialog_enter_app_pin_confirm)
    yesBtn.setOnClickListener {
      if (enterAppPinDialogEt.text.length < 4) {
        Toast.makeText(context, getString(R.string.dialog_pin_app_varning), Toast.LENGTH_SHORT).show()
      } else {
        val pinCodeApp = enterAppPinDialogEt.text.toString()
        main?.saveText(PreferenceKeys.APP_PIN_CODE, pinCodeApp)
        Toast.makeText(context, "pinCodeApp: $pinCodeApp", Toast.LENGTH_SHORT).show()
        hideKeyboard(enterAppPinDialogEt)
        myDialog.dismiss()
      }
    }
  }
  private fun sendEMGMode(value: Int) {
    correlatorNoiseThreshold1 =
      mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM,16)
    correlatorNoiseThreshold2 =
      mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_2_NUM,16)

    //TODO отправка команды на индюшку
    if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_X)) {
      main?.runSendCommand(byteArrayOf(
        (correlatorNoiseThreshold1).toByte(), 6, 1, 0x10, 36, 18, 44, 52, 64, 72, 0x40, 5,
        64, (correlatorNoiseThreshold2).toByte(), 6, 1, 0x10, 36, 18,
        44, 52, 64, 72, 0x40, 5, 64, value.toByte()
      ), SENS_OPTIONS_NEW_VM, countRestart)
    } else {
      System.err.println("sendEMGMode INDY: $value")
      main?.bleCommandConnector(byteArrayOf(value.toByte()), SET_EMG_MODE, WRITE, 11)
    }
  }
  private fun sendGestureRotation () {
    main?.stage = "advanced activity"
    sensorGestureSwitching = if (mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false)) { 1 } else { 0 }
    lockProstheses = if (mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_LOCK_NUM, false)) { 1 } else { 0 }
    startGestureInLoopNum = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.START_GESTURE_IN_LOOP, 0)
    endGestureInLoopNum = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.END_GESTURE_IN_LOOP, 0)

    if (checkDriverVersionGreaterThan237() || checkINDYCanControlEMGModes()) {
      System.err.println("sendGestureRotation  237=${checkDriverVersionGreaterThan237()}  startGestureInLoopNum:$startGestureInLoopNum   endGestureInLoopNum:$endGestureInLoopNum")
      main?.runSendCommand(byteArrayOf(
        sensorGestureSwitching,
        0.toByte(),
        binding.peakTimeVmSb.progress.toByte(),
        0.toByte(),
        lockProstheses,
        (binding.holdToLockTimeSb.progress).toByte(),
        startGestureInLoopNum.toByte(),
        endGestureInLoopNum.toByte()
      ), ROTATION_GESTURE_NEW_VM, countRestart)
    } else {
      System.err.println("sendGestureRotation 237=${checkDriverVersionGreaterThan237()} else")
      if (main?.driverVersionS != null) {
        main?.runSendCommand(byteArrayOf(
          sensorGestureSwitching,
          0.toByte(),
          binding.peakTimeVmSb.progress.toByte(),
          0.toByte(),
          lockProstheses,
          (binding.holdToLockTimeSb.progress).toByte()
        ), ROTATION_GESTURE_NEW_VM, countRestart)
      }
    }
    RxUpdateMainEvent.getInstance().updateUIGestures(100)
  }

  private fun sendActiveGestures(numActiveGestures: Int) {
    //TODO функция не отправляет активный жест
    System.err.println("sendActiveGestures activeGesture = $numActiveGestures")
    val setReverse = if (mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, false)) { 1 } else { 0 }
    val prosthesisMode = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_PROSTHESIS, 0)
    val numberOfCyclesStand = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.MAX_STAND_CYCLES, 0)

    if (main?.driverVersionS != null) {
      main?.runSendCommand(
        byteArrayOf(
          setReverse.toByte(),
          numActiveGestures.toByte(),
          prosthesisMode.toByte(),
          numberOfCyclesStand.toByte(),
          (numberOfCyclesStand / 256).toByte()
        ),
        SET_REVERSE_NEW_VM, countRestart
      )
    }
  }

  internal fun saveInt(key: String, variable: Int) {
    val editor: SharedPreferences.Editor = mSettings!!.edit()
    editor.putInt(key, variable)
    editor.apply()
  }
  private fun saveBool(key: String, variable: Boolean) {
    val editor: SharedPreferences.Editor = mSettings!!.edit()
    editor.putBoolean(key, variable)
    editor.apply()
  }
  private fun checkDriverVersionGreaterThan237():Boolean {
    System.err.println("driverVersionS " + main?.driverVersionS)
    if (main?.driverVersionS != null) {
      val driverNum = main?.driverVersionS?.substring(0, 1) + main?.driverVersionS?.substring(2, 4)
      if (driverNum.toInt() >= 237) {
        return true
      }
    }
    return false
  }
  private fun checkDriverVersionGreaterThan240():Boolean {
    return if (main?.driverVersionS != null) {
      val driverNum = main?.driverVersionS?.substring(0, 1) + main?.driverVersionS?.substring(2, 4)
      driverNum.toInt() >= 240
    } else {
      false
    }
  }

  private fun checkINDYCanControlEMGModes():Boolean {
    val indyVersion = (mSettings!!.getInt(
      main?.mDeviceAddress + PreferenceKeys.DRIVER_NUM,
      1
    ))
    System.err.println("driverVersion indy $indyVersion")
    if (indyVersion >= 120) {
      return true
    }
    return false
  }

  private fun validationAndConversionSerialNumber(serialNumber: String): String {
    System.err.println("serialNumber: $serialNumber")//"FEST-F-11111"


    val namePrefix = serialNumber.split("-")[0]+"-"+serialNumber.split("-")[1]
    System.err.println("namePrefix = $namePrefix    serialNumber")
    when (namePrefix) {
      DEVICE_TYPE_INDY_H -> { }
      DEVICE_TYPE_INDY_EP -> { }
      DEVICE_TYPE_INDY_EB -> { }
      DEVICE_TYPE_INDY_SH -> { }
      DEVICE_TYPE_FEST_F -> { }
      DEVICE_TYPE_FEST_H -> { }
      DEVICE_TYPE_FEST_H_EP -> { }
      DEVICE_TYPE_FEST_H_EB -> { }
      else -> {
        validationError = "В нашей линейке продуктов нет: $namePrefix"
        System.err.println("В нашей линейке продуктов нет: $namePrefix    serialNumber")
        return "false"
      }
    }

    var nameBridge: String = serialNumber.substring(6,7)
    if (serialNumber.split("-")[1].length == 2) { nameBridge = serialNumber.substring(7,8)}
    if (nameBridge != "-") {
      validationError = "Буквенную и числовую части должен разделять дефис"
      System.err.println("Буквенную и числовую части должен разделять дефис    serialNumber")
      return "false"
    }

    if ((serialNumber.split("-")[1].length == 1) && serialNumber.length < 12 || ((serialNumber.split("-")[1].length == 2) && serialNumber.length < 13)) {
      validationError = "Вы ввели слишком короткий серийный номер"
      System.err.println("Вы ввели слишком короткий серийный номер    serialNumber")
      return "false"
    } else {
      if (((serialNumber.split("-")[1].length == 1) && serialNumber.length > 12) || ((serialNumber.split("-")[1].length == 2) && serialNumber.length > 13)) {
        validationError = "Вы ввели слишком длинный серийный номер"
        System.err.println("Вы ввели слишком длинный серийный номер    serialNumber")
        return "false"
      }
    }

    var nameCode: String = serialNumber.substring(7, serialNumber.length)
    if (serialNumber.split("-")[1].length == 2) { nameCode = serialNumber.substring(8, serialNumber.length)}
    try {
      nameCode.toInt()
    } catch (e: Exception) {
      validationError = "Вторая часть серийного номера не число: $nameCode"
      System.err.println("Вторая часть серийного номера не число: $nameCode    serialNumber")
      return "false"
    }

    when (namePrefix) {
      DEVICE_TYPE_INDY_H -> {
        return "INDY-H-$nameCode"
      }
      DEVICE_TYPE_INDY_EP -> {

        return "INDY-EP-$nameCode"
      }
      DEVICE_TYPE_INDY_EB -> {

        return "INDY-EB-$nameCode"
      }
      DEVICE_TYPE_INDY_SH -> {

        return "INDY-SH-$nameCode"
      }
      DEVICE_TYPE_FEST_F -> {
        return DEVICE_TYPE_FEST_X+NEW_DEVICE_TYPE_FEST_F+nameCode
      }
      DEVICE_TYPE_FEST_H -> {
        return DEVICE_TYPE_FEST_X+NEW_DEVICE_TYPE_FEST_H+nameCode
      }
      DEVICE_TYPE_FEST_H_EP -> {
        return DEVICE_TYPE_FEST_X+ NEW_DEVICE_TYPE_FEST_EP+nameCode
      }
      DEVICE_TYPE_FEST_H_EB -> {
        return DEVICE_TYPE_FEST_X+ NEW_DEVICE_TYPE_FEST_EB+nameCode
      }
    }

    return "false"
  }

  private fun View.showKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
  }
  private fun hideKeyboard(view: View) {
    val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
  }
  private fun useNewSystemSendCommand(): Boolean {
    //спиннеры имеют свойство спамить блютуз команды при установке в них значения из памяти,
    // что мешает нормальной инициализации блютуза и запросу стартовых параметров. Для
    // отключения этого спама мы делаем эту проверку
    var useNewSystemSendCommand = false
    System.err.println("useNewSystemSendCommand driverVersionINDY = ${main?.driverVersionINDY}")
    if (main?.driverVersionS != null) {
      try {
        val driverNum = main?.driverVersionS?.substring(0, 1) + main?.driverVersionS?.substring(2, 4)
        useNewSystemSendCommand = driverNum.toInt() > 233
      } catch (_: Exception) { }
    }
    if (main?.driverVersionINDY != null) {
      useNewSystemSendCommand = main?.driverVersionINDY!! >= 120
    }
    return useNewSystemSendCommand
  }
}
