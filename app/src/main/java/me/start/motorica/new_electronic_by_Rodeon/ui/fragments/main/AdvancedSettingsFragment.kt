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

package me.start.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.skydoves.powerspinner.IconSpinnerAdapter
import com.skydoves.powerspinner.IconSpinnerItem
import com.skydoves.powerspinner.OnSpinnerItemSelectedListener
import com.yandex.metrica.YandexMetrica
import io.reactivex.android.schedulers.AndroidSchedulers
import me.start.motorica.BuildConfig
import me.start.motorica.R
import me.start.motorica.databinding.LayoutAdvancedSettingsBinding
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager
import me.start.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.*
import me.start.motorica.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceManager
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.gripper.test_encoders.GripperTestScreenWithEncodersActivity
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import javax.inject.Inject

@Suppress("DEPRECATION", "UNNECESSARY_SAFE_CALL")
class AdvancedSettingsFragment : Fragment() {

  @Inject
  lateinit var preferenceManager: PreferenceManager

  private var mContext: Context? = null
  private var main: MainActivity? = null
  private var mSettings: SharedPreferences? = null
  private var scale = 0F
  private var mode: Byte = 0x00
  private var sensorGestureSwitching: Byte = 0x00
  private var lockProstheses: Byte = 0x00

  private var current  = 0
  private var current1 = 0
  private var current2 = 0
  private var current3 = 0
  private var current4 = 0
  private var current5 = 0
  private var current6 = 0

  private var correlatorNoiseThreshold1 = 0
  private var correlatorNoiseThreshold2 = 0
  private var modeEMGSend = 0

  private var startGestureInLoop = 0
  private var endGestureInLoop = 0

  private lateinit var binding: LayoutAdvancedSettingsBinding
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    binding = LayoutAdvancedSettingsBinding.inflate(layoutInflater)
    WDApplication.component.inject(this)
    if (activity != null) { main = activity as MainActivity? }
    this.mContext = context
    scale = resources.displayMetrics.density
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
  }

  @SuppressLint("SetTextI18n")
  private fun resetUI() {
    val eventYandexMetricaParametersReset = "{\"Screen advanced settings\":\"Tup reset to factory settings button\"}"
    if (!main?.lockWriteBeforeFirstRead!!) {
      preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, false)

      main?.setSwapOpenCloseButton(false)
      binding.swapOpenCloseSw.isChecked = false
      binding.swapOpenCloseTv.text = resources.getString(R.string.off_sw)
      preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)

      preferenceManager.putInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM, 80)
      ObjectAnimator.ofInt(binding.shutdownCurrentSb, "progress", preferenceManager.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM, 80)).setDuration(200).start()

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

      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersReset)
      RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(SHUTDOWN_CURRENT_NEW_VM)
    } else {
      updateAllParameters()
      main?.showToast(resources.getString(R.string.waiting_for_data_transfer_from_the_prosthesis))
    }
  }

  @SuppressLint("SetTextI18n", "CheckResult", "Recycle")
  private fun initializeUI() {
    mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)

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
          else -> {
            saveInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_EMG_SENSORS, 9)
            9
          }
      }
      sendEMGMode(modeEMGSend)
    }

    if (mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_EMG_SENSORS,9) == 9) {
      binding.EMGModeSwapPsv.selectItemByIndex(0)
    } else {
      binding.EMGModeSwapPsv.selectItemByIndex(1)
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

    if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
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

    var eventYandexMetricaParametersShutdownCurrent = "{\"Screen advanced settings\":\"Change shutdown current\"}"

    binding.shutdownCurrentSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        binding.shutdownCurrentTv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          main?.bleCommandConnector(byteArrayOf(seekBar.progress.toByte()), SHUTDOWN_CURRENT_HDLE, WRITE, 0)
          saveInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM, seekBar.progress)
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersShutdownCurrent)
        }
      }
    })

    eventYandexMetricaParametersShutdownCurrent = "{\"Screen advanced settings\":\"Change shutdown current 1\"}"
    binding.shutdownCurrent1Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        binding.shutdownCurrent1Tv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(
              binding.shutdownCurrent1Sb.progress.toByte(),
              binding.shutdownCurrent2Sb.progress.toByte(),
              binding.shutdownCurrent3Sb.progress.toByte(),
              binding.shutdownCurrent4Sb.progress.toByte(),
              binding.shutdownCurrent5Sb.progress.toByte(),
              binding.shutdownCurrent6Sb.progress.toByte()),
              SHUTDOWN_CURRENT_NEW_VM, 50)
          }
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
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
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersShutdownCurrent)
          RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(SHUTDOWN_CURRENT_NEW_VM)
        }
      }
    })
    eventYandexMetricaParametersShutdownCurrent = "{\"Screen advanced settings\":\"Change shutdown current 2\"}"
    binding.shutdownCurrent2Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        binding.shutdownCurrent2Tv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(
              binding.shutdownCurrent1Sb.progress.toByte(),
              binding.shutdownCurrent2Sb.progress.toByte(),
              binding.shutdownCurrent3Sb.progress.toByte(),
              binding.shutdownCurrent4Sb.progress.toByte(),
              binding.shutdownCurrent5Sb.progress.toByte(),
              binding.shutdownCurrent6Sb.progress.toByte()),
              SHUTDOWN_CURRENT_NEW_VM, 50)
          }
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
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
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersShutdownCurrent)
          RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(SHUTDOWN_CURRENT_NEW_VM)
        }
      }
    })
    eventYandexMetricaParametersShutdownCurrent = "{\"Screen advanced settings\":\"Change shutdown current 3\"}"
    binding.shutdownCurrent3Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        binding.shutdownCurrent3Tv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(
              binding.shutdownCurrent1Sb.progress.toByte(),
              binding.shutdownCurrent2Sb.progress.toByte(),
              binding.shutdownCurrent3Sb.progress.toByte(),
              binding.shutdownCurrent4Sb.progress.toByte(),
              binding.shutdownCurrent5Sb.progress.toByte(),
              binding.shutdownCurrent6Sb.progress.toByte()),
              SHUTDOWN_CURRENT_NEW_VM, 50)
          }
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
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
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersShutdownCurrent)
          RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(SHUTDOWN_CURRENT_NEW_VM)
        }
      }
    })
    eventYandexMetricaParametersShutdownCurrent = "{\"Screen advanced settings\":\"Change shutdown current 4\"}"
    binding.shutdownCurrent4Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        binding.shutdownCurrent4Tv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(
              binding.shutdownCurrent1Sb.progress.toByte(),
              binding.shutdownCurrent2Sb.progress.toByte(),
              binding.shutdownCurrent3Sb.progress.toByte(),
              binding.shutdownCurrent4Sb.progress.toByte(),
              binding.shutdownCurrent5Sb.progress.toByte(),
              binding.shutdownCurrent6Sb.progress.toByte()),
              SHUTDOWN_CURRENT_NEW_VM, 50)
          }
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
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
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersShutdownCurrent)
          RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(SHUTDOWN_CURRENT_NEW_VM)
        }
      }
    })
    eventYandexMetricaParametersShutdownCurrent = "{\"Screen advanced settings\":\"Change shutdown current 5\"}"
    binding.shutdownCurrent5Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        binding.shutdownCurrent5Tv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(
              binding.shutdownCurrent1Sb.progress.toByte(),
              binding.shutdownCurrent2Sb.progress.toByte(),
              binding.shutdownCurrent3Sb.progress.toByte(),
              binding.shutdownCurrent4Sb.progress.toByte(),
              binding.shutdownCurrent5Sb.progress.toByte(),
              binding.shutdownCurrent6Sb.progress.toByte()),
              SHUTDOWN_CURRENT_NEW_VM, 50)
          }
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
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
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersShutdownCurrent)
          RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(SHUTDOWN_CURRENT_NEW_VM)
        }
      }
    })
    eventYandexMetricaParametersShutdownCurrent = "{\"Screen advanced settings\":\"Change shutdown current 6\"}"
    binding.shutdownCurrent6Sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        binding.shutdownCurrent6Tv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(
              binding.shutdownCurrent1Sb.progress.toByte(),
              binding.shutdownCurrent2Sb.progress.toByte(),
              binding.shutdownCurrent3Sb.progress.toByte(),
              binding.shutdownCurrent4Sb.progress.toByte(),
              binding.shutdownCurrent5Sb.progress.toByte(),
              binding.shutdownCurrent6Sb.progress.toByte()),
              SHUTDOWN_CURRENT_NEW_VM, 50)
          }
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
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
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersShutdownCurrent)
          RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(SHUTDOWN_CURRENT_NEW_VM)
        }
      }
    })


    val eventYandexMetricaParametersSwapOpenCloseButton = "{\"Screen advanced settings\":\"Tup swap open close button\"}"
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
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersSwapOpenCloseButton)
    }


    val eventYandexMetricaParametersSingleChannel = "{\"Screen advanced settings\":\"Tup single channel control switch\"}"
    binding.singleChannelControlSw.setOnClickListener {
      if (!main?.lockWriteBeforeFirstRead!!) {
        if (binding.singleChannelControlSw.isChecked) {
          binding.singleChannelControlTv.text = resources.getString(R.string.on_sw)
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(0x01), SET_ONE_CHANNEL_NEW_VM, 50)
          } else {
            if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
              main?.runWriteData(byteArrayOf(0x01), SET_ONE_CHANNEL_NEW, WRITE)
            } else {
              main?.bleCommandConnector(byteArrayOf(0x01), SET_ONE_CHANNEL, WRITE, 16)
            }
          }
          main?.setOneChannelNum = 1
          saveBool(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, true)
        } else {
          binding.singleChannelControlTv.text = resources.getString(R.string.off_sw)
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(0x00), SET_ONE_CHANNEL_NEW_VM, 50)
          } else {
            if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
              main?.runWriteData(byteArrayOf(0x00), SET_ONE_CHANNEL_NEW, WRITE)
            } else {
              main?.bleCommandConnector(byteArrayOf(0x00), SET_ONE_CHANNEL, WRITE, 16)
            }
          }
          main?.setOneChannelNum = 0
          saveBool(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)
        }
        YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersSingleChannel)
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


    val eventYandexMetricaParametersOnOffGesturesSwitching = "{\"Screen advanced settings\":\"On/off gesture switch using sensors\"}"
    binding.onOffSensorGestureSwitchingSw.setOnClickListener {
      if (!main?.lockWriteBeforeFirstRead!!) {
        if (binding.onOffSensorGestureSwitchingSw.isChecked) {
          binding.onOffSensorGestureSwitchingTv.text = resources.getString(R.string.on_sw)
          sensorGestureSwitching = 0x01
          binding.peakTimeRl.visibility = View.VISIBLE
          if (mode.toInt() == 0) binding.downtimeRl.visibility = View.VISIBLE
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            binding.downtimeRl.visibility = View.GONE
            binding.peakTimeRl.visibility = View.GONE
            binding.modeNewRl.visibility = View.GONE
            binding.peakTimeVmRl.visibility = View.VISIBLE
            sendGestureRotation()
          } else {
            if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
              binding.modeNewRl.visibility = View.VISIBLE
              main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode, binding.peakTimeSb.progress.toByte(), binding.downtimeSb.progress.toByte()), ROTATION_GESTURE_NEW, WRITE)
            } else {
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
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            binding.modeNewRl.visibility = View.GONE
            binding.peakTimeVmRl.visibility = View.GONE
            sendGestureRotation()
          } else {
            if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
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
        YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersOnOffGesturesSwitching)
      }
    }


    val eventYandexMetricaParametersMode = "{\"Screen advanced settings\":\"Change switching modes gestures using sensors\"}"
    binding.modeSw.setOnClickListener {
      if (!main?.lockWriteBeforeFirstRead!!) {
        if (binding.modeSw.isChecked) {
          binding.modeTv.text = "двумя\nдатчиками"
          mode = 0x01
          binding.downtimeRl.visibility = View.GONE
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H) || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(sensorGestureSwitching, mode, binding.peakTimeSb.progress.toByte(), binding.downtimeSb.progress.toByte()), ROTATION_GESTURE_NEW, 50)
          } else {
            main?.bleCommandConnector(byteArrayOf(sensorGestureSwitching, mode, (binding.peakTimeSb.progress?.plus(5))?.toByte()!!, (binding.downtimeSb.progress?.plus(5))?.toByte()!!),
              ROTATION_GESTURE_NEW, WRITE, 17)
          }
          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, true)
        } else {
          binding.modeTv.text = "одним\nдатчиком"
          mode = 0x00
          binding.downtimeRl.visibility = View.VISIBLE
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H) || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(sensorGestureSwitching, mode, binding.peakTimeSb.progress.toByte(), binding.downtimeSb.progress.toByte()), ROTATION_GESTURE_NEW, 50)
          } else {
            main?.bleCommandConnector(byteArrayOf(sensorGestureSwitching, mode, (binding.peakTimeSb.progress?.plus(5))?.toByte()!!, (binding.downtimeSb.progress?.plus(5))?.toByte()!!),
              ROTATION_GESTURE_NEW, WRITE, 17)
          }
          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, false)
        }
        YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersMode)
      }
    }

    binding.modeNewSw.setOnSwitchListener { position, _ ->
      if (position == 0) {
//        Toast.makeText(main?.baseContext,  "0", Toast.LENGTH_SHORT).show()
        saveInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NEW_NUM, 0)
        mode = 0
        if (sensorGestureSwitching.toInt() == 1) binding.downtimeRl.visibility = View.VISIBLE
        main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode, binding.peakTimeSb.progress.toByte(), binding.downtimeSb.progress.toByte()), ROTATION_GESTURE_NEW, WRITE)
      }
      if (position == 1) {
//        Toast.makeText(main?.baseContext, "1", Toast.LENGTH_SHORT).show()
        saveInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NEW_NUM, 1)
        mode = 1
        binding.downtimeRl.visibility = View.GONE
        main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode, binding.peakTimeSb.progress.toByte(), binding.downtimeSb.progress.toByte()), ROTATION_GESTURE_NEW, WRITE)
      }
      if (position == 2) {
//        Toast.makeText(main?.baseContext, "2", Toast.LENGTH_SHORT).show()
        saveInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NEW_NUM, 2)
        mode = 2
        binding.downtimeRl.visibility = View.GONE
        main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode, binding.peakTimeSb.progress.toByte(), binding.downtimeSb.progress.toByte()), ROTATION_GESTURE_NEW, WRITE)
      }
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersMode)
    }
    binding.modeNewSw.selectedTab = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NEW_NUM, 0)

    val eventYandexMetricaParametersPeakTimeVM = "{\"Screen advanced settings\":\"Change peak time VM\"}"
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
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersPeakTimeVM)
        }
      }
    })

    val eventYandexMetricaParametersPeakTime = "{\"Screen advanced settings\":\"Change peak time\"}"
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
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
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
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
          time = (binding.peakTimeSb.progress?.times(0.04)).toString() + "c"
        }
        binding.peakTimeTv.text = time
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode,
              binding.peakTimeSb.progress.toByte(), binding.downtimeSb.progress.toByte()
            ), ROTATION_GESTURE_NEW, WRITE)
          } else {
            main?.bleCommandConnector(byteArrayOf(0x00, sensorGestureSwitching, mode, (binding.peakTimeSb.progress?.plus(5))?.toByte()!!, (binding.downtimeSb.progress?.plus(5))?.toByte()!!),
              ROTATION_GESTURE_NEW, WRITE, 17)
          }
          preferenceManager.putInt(main?.mDeviceAddress + PreferenceKeys.SET_PEAK_TIME_NUM, seekBar.progress)
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersPeakTime)
        }
      }
    })

    val eventYandexMetricaParametersDowntime = "{\"Screen advanced settings\":\"Change downtime\"}"
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
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
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
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
          time = (binding.downtimeSb.progress?.times(0.04)).toString() + "c"
        }
        binding.downtimeTv.text = time
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode,
              binding.peakTimeSb.progress.toByte(), binding.downtimeSb.progress.toByte()
            ), ROTATION_GESTURE_NEW, WRITE)
          } else {
            main?.bleCommandConnector(byteArrayOf(0x00, sensorGestureSwitching, mode, (binding.peakTimeSb.progress?.plus(5))?.toByte()!!, (binding.downtimeSb.progress?.plus(5))?.toByte()!!),
              ROTATION_GESTURE_NEW, WRITE, 17)
          }
          preferenceManager.putInt(main?.mDeviceAddress + PreferenceKeys.SET_DOWNTIME_NUM, seekBar.progress)
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersDowntime)
        }
      }
    })


    val eventYandexMetricaParametersGetTelemetryNumber = "{\"Screen advanced settings\":\"Tup get telemetry number button\"}"
    binding.getSetupBtn.setOnClickListener {
      if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
        main?.bleCommandConnector(byteArrayOf(0x00), TELEMETRY_NUMBER_NEW, READ, 17)
      }
      if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
        main?.bleCommandConnector(byteArrayOf(0x00), TELEMETRY_NUMBER_NEW_VM, READ, 17)
      }
      main?.lockChangeTelemetryNumber = true
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersGetTelemetryNumber)
    }

    binding.setSetupBtn.setOnClickListener {
      main?.showSetSerialNumberDialog(binding.telemetryNumberEt.text.toString())
    }
    main?.telemetryNumber = binding.telemetryNumberEt.text.toString()

    val eventYandexMetricaParametersLeftRight = "{\"Screen advanced settings\":\"Tup left right side swap switch\"}"
    binding.leftRightSideSwapSw.setOnClickListener{
//      System.err.println(" LOLOLOEFWEF --->  side key : ${main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE}")
      showAlertChangeSideDialog()
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersLeftRight)
    }



    val eventYandexMetricaParametersFingersDelay = "{\"Screen advanced settings\":\"Tup  time delay fingers swap switch\"}"
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
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersFingersDelay)
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


    binding.resetToFactorySettingsBtn.setOnClickListener { main?.showResetDialog() }
    binding.calibrationAdvBtn.setOnClickListener { main?.showCalibrationDialog() }
    val eventYandexMetricaParametersCalibrationStatusAdv = "{\"Screen advanced settings\":\"Tup calibration status button\"}"
    binding.calibrationStatusAdvBtn.setOnClickListener {
      if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
        main?.runReadDataAllCharacteristics(STATUS_CALIBRATION_NEW_VM)
      } else {
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
          main?.runReadDataAllCharacteristics(STATUS_CALIBRATION_NEW)
        }
      }
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersCalibrationStatusAdv)
    }


    binding.debugScreenBtn.setOnClickListener {
      val intent = Intent(context, GripperTestScreenWithEncodersActivity::class.java)
      main!!.setDebugScreenIsOpen(true)
      startActivity(intent)
    }

    binding.testConnectionBtn.setOnClickListener{
      //start communication test
      main?.openTestConnectionProgressDialog()
      main?.testingConnection = true
      main?.runSendCommand(byteArrayOf(0x01), ROTATION_GESTURE_NEW_VM, 6)
      main?.runSendCommand(byteArrayOf(0x02), ROTATION_GESTURE_NEW_VM, 6)
      main?.runSendCommand(byteArrayOf(0x03), ROTATION_GESTURE_NEW_VM, 6)
      main?.runSendCommand(byteArrayOf(0x04), ROTATION_GESTURE_NEW_VM, 6)
      main?.runSendCommand(byteArrayOf(0x05), ROTATION_GESTURE_NEW_VM, 6)
      main?.runSendCommand(byteArrayOf(0x11), ROTATION_GESTURE_NEW_VM, 6)
      main?.runSendCommand(byteArrayOf(0x12), ROTATION_GESTURE_NEW_VM, 6)
      main?.runSendCommand(byteArrayOf(0x13), ROTATION_GESTURE_NEW_VM, 6)
      main?.runSendCommand(byteArrayOf(0x14), ROTATION_GESTURE_NEW_VM, 6)
      main?.runSendCommand(byteArrayOf(0x15), ROTATION_GESTURE_NEW_VM, 6)
      //end
    }

    //Скрывает настройки, которые не актуальны для многосхватной бионики
    if ( main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_FEST_A)
      || main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_BT05)
      || main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_MY_IPHONE)
      || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)
      || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
      binding.shutdownCurrentRl.visibility = View.GONE
    }
    //Скрывает настройки, которые не актуальны для бионик кроме FEST-H и FEST-X
    when {
        main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X) -> {
          binding.telemetryRl.visibility = View.VISIBLE
          binding.onOffProsthesesBlockingRl.visibility = View.VISIBLE
          binding.testConnectionRl.visibility = View.VISIBLE
          binding.scaleTv.visibility = View.GONE
          binding.EMGModeRl.visibility = View.GONE
        }
        main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H) -> {
          binding.EMGModeRl.visibility = View.GONE
          binding.telemetryRl.visibility = View.VISIBLE
          binding.debugScreenRl.visibility = View.GONE
          binding.scaleTv.visibility = View.GONE
        }
        else -> {
          binding.EMGModeRl.visibility = View.GONE
          binding.debugScreenRl.visibility = View.GONE
          binding.telemetryRl.visibility = View.GONE
          binding.calibrationRl.visibility = View.GONE
          binding.shutdownCurrent1Rl.visibility = View.GONE
          binding.shutdownCurrent2Rl.visibility = View.GONE
          binding.shutdownCurrent3Rl.visibility = View.GONE
          binding.shutdownCurrent4Rl.visibility = View.GONE
          binding.shutdownCurrent5Rl.visibility = View.GONE
          binding.shutdownCurrent6Rl.visibility = View.GONE
          binding.sideRl.visibility = View.GONE
          binding.onOffSensorGestureSwitchingRl.visibility = View.GONE
        }
    }

    binding.swapOpenCloseSw.isChecked = preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)
    binding.singleChannelControlSw.isChecked = mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)


    if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
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
    binding.resetToFactorySettingsBtn.isEnabled = enabled
    binding.calibrationAdvBtn.isEnabled = enabled
    binding.calibrationStatusAdvBtn.isEnabled = enabled
    binding.debugScreenBtn.isEnabled = enabled
    binding.testConnectionBtn.isEnabled = enabled
    if (checkDriverVersionGreaterThan237()) { binding.EMGModeRl.visibility = View.VISIBLE }
  }
  @SuppressLint("Recycle", "SetTextI18n")
  private fun updateAllParameters() {
    main?.runOnUiThread {
      System.err.println("Принятые данные состояния токов ОБНОВЛЕНИЕ")
      ObjectAnimator.ofInt(binding.shutdownCurrentSb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM, 80)).setDuration(200).start()
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

    current  = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM,   80)
    current1 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_1, 80)
    current2 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_2, 80)
    current3 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_3, 80)
    current4 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_4, 80)
    current5 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_5, 80)
    current6 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_6, 80)

    if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
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

      startGestureInLoop = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.START_GESTURE_IN_LOOP, 0)
      endGestureInLoop = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.END_GESTURE_IN_LOOP, 0)
    }
    if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
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


    if (mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_LOCK_NUM, false)) {
      binding.onOffProsthesesBlockingSw.isChecked = true
      lockProstheses = 0x01
      binding.onOffProsthesesBlockingTv.text = resources.getString(R.string.on_sw)
      if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
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
      if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
        binding.peakTimeVmRl.visibility = View.VISIBLE
      }
      if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
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
        main?.runSendCommand(byteArrayOf(0x09), CALIBRATION_NEW_VM, 50)
      } else {
        main?.stage = "advanced activity"
        main?.runSendCommand(byteArrayOf(0x0a), CALIBRATION_NEW_VM, 50)
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
  private fun sendEMGMode(value: Int) {
    correlatorNoiseThreshold1 =
      mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM,16)
    correlatorNoiseThreshold2 =
      mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_2_NUM,16)

    main?.runSendCommand(byteArrayOf(
      (correlatorNoiseThreshold1).toByte(), 6, 1, 0x10, 36, 18, 44, 52, 64, 72, 0x40, 5,
      64, (correlatorNoiseThreshold2).toByte(), 6, 1, 0x10, 36, 18,
      44, 52, 64, 72, 0x40, 5, 64, value.toByte()
    ), SENS_OPTIONS_NEW_VM, 50)
  }
  private fun sendGestureRotation () {
    main?.stage = "advanced activity"
    if (checkDriverVersionGreaterThan237()) {
      System.err.println("sendGestureRotation true")
      main?.runSendCommand(byteArrayOf(sensorGestureSwitching,
        0.toByte(),
        binding.peakTimeVmSb.progress.toByte(),
        0.toByte(),
        lockProstheses,
        (binding.holdToLockTimeSb.progress).toByte(),
        startGestureInLoop.toByte(),
        endGestureInLoop.toByte()
      ), ROTATION_GESTURE_NEW_VM, 50)
    } else {
      System.err.println("sendGestureRotation else")
      main?.runSendCommand(byteArrayOf(sensorGestureSwitching,
        0.toByte(),
        binding.peakTimeVmSb.progress.toByte(),
        0.toByte(),
        lockProstheses,
        (binding.holdToLockTimeSb.progress).toByte()
      ), ROTATION_GESTURE_NEW_VM, 50)
    }
    RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(ROTATION_GESTURE_NEW_VM)
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
    if (main?.driverVersionS != null) {
      val driverNum = main?.driverVersionS?.substring(0, 1) + main?.driverVersionS?.substring(2, 4)
      if (driverNum.toInt() >= 237) {
        return true
      }
    }
    return false
  }
}
