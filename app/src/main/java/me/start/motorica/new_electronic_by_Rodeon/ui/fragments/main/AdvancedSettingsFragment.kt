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
import com.yandex.metrica.YandexMetrica
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.layout_advanced_settings.*
import me.start.motorica.BuildConfig
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager
import me.start.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.*
import me.start.motorica.new_electronic_by_Rodeon.events.rx.RxUpdateMainEvent
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceManager
import me.start.motorica.new_electronic_by_Rodeon.persistence.sqlite.SqliteManager
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.gripper.test_encoders.GripperTestScreenWithEncodersActivity
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import org.jetbrains.anko.textColor
import javax.inject.Inject

@Suppress("DEPRECATION")
class AdvancedSettingsFragment : Fragment() {

  @Inject
  lateinit var preferenceManager: PreferenceManager
  @Inject
  lateinit var sqliteManager: SqliteManager

  private var rootView: View? = null
  private var mContext: Context? = null
  private var main: MainActivity? = null
  private var mSettings: SharedPreferences? = null
  private var scale = 0F
  private var mode: Byte = 0x00
  private var sensorGestureSwitching: Byte = 0x00
  private var lockProsthesis: Byte = 0x00

  private var current = 0
  private var current1 = 0
  private var current2 = 0
  private var current3 = 0
  private var current4 = 0
  private var current5 = 0
  private var current6 = 0

  @SuppressLint("CheckResult")
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val rootView = inflater.inflate(R.layout.layout_advanced_settings, container, false)
    WDApplication.component.inject(this)
    if (activity != null) { main = activity as MainActivity? }
    this.rootView = rootView
    this.mContext = context
    scale = resources.displayMetrics.density

    RxUpdateMainEvent.getInstance().uiAdvancedSettings
      .compose(main?.bindToLifecycle())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe {
        updateAllParameters()
      }
    return rootView
  }


  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initializeUI()
    updateAllParameters()
  }

  @SuppressLint("SetTextI18n", "CheckResult", "Recycle")
  private fun initializeUI() {
    mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
//    if (main?.locate?.contains("ru")!!) {
      shutdown_current_text_tv?.textSize = 11f
      swap_button_open_close_tv?.textSize = 11f
      single_channel_control_text_tv?.textSize = 11f
      on_off_sensor_gesture_switching_text_tv?.textSize = 11f
      mode_text_tv?.textSize = 11f
      peak_time_text_tv?.textSize = 11f
      peak_time_vm_text_tv?.textSize = 11f
      downtime_text_tv?.textSize = 11f
      mode_tv?.textSize = 11f
      reset_to_factory_settings_btn?.textSize = 12f
      calibration_adv_btn?.textSize = 10f
      calibration_status_adv_btn?.textSize = 10f
      side_text_tv?.textSize = 11f
      time_delay_of_fingers_tv?.textSize = 11f
      left_right_side_swap_tv?.textSize = 11f
      shutdown_current_1_text_tv?.textSize = 11f
      shutdown_current_2_text_tv?.textSize = 11f
      shutdown_current_3_text_tv?.textSize = 11f
      shutdown_current_4_text_tv?.textSize = 11f
      shutdown_current_5_text_tv?.textSize = 11f
      shutdown_current_6_text_tv?.textSize = 11f
      version_app_tv?.textSize = 11f
      scale_tv?.textSize = 11f
      on_off_prosthesis_blocking_text_tv?.textSize = 11f
      hold_to_lock_time_text_tv?.textSize = 10f
//    }
    if (mSettings?.getInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1) == 1) {
      left_right_side_swap_sw?.isChecked = true
      left_right_side_swap_tv?.text = Html.fromHtml(getString(R.string.right))
    } else {
      left_right_side_swap_sw?.isChecked = false
      left_right_side_swap_tv?.text = resources.getString(R.string.left)
    }
    if (mSettings?.getInt(main?.mDeviceAddress + PreferenceKeys.SET_FINGERS_DELAY, 0) == 1){
      time_delay_of_fingers_swap_sw?.isChecked = true
      time_delay_of_fingers_swap_tv?.text = "1"
    } else {
      time_delay_of_fingers_swap_sw?.isChecked = false
      time_delay_of_fingers_swap_tv?.text = "0"
    }

    if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
      val calibrationStartParams: LinearLayout.LayoutParams =
        calibration_start_button_ll.layoutParams as LinearLayout.LayoutParams
      calibrationStartParams.weight = 1f
      val calibrationStatusParams: LinearLayout.LayoutParams =
        calibration_status_button_ll.layoutParams as LinearLayout.LayoutParams
      calibrationStatusParams.weight = 1f
    } else {
      time_delay_of_fingers_rl?.visibility = View.GONE
      if (main?.locate?.contains("w")!!) { calibration_status_adv_btn?.textSize = 12f }
    }

    var eventYandexMetricaParametersShutdownCurrent = "{\"Screen advanced settings\":\"Change shutdown current\"}"
    shutdown_current_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        shutdown_current_tv?.text = seekBar.progress.toString()
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
    shutdown_current_1_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        shutdown_current_1_tv?.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(
              shutdown_current_1_sb?.progress?.toByte()!!,
              shutdown_current_2_sb?.progress?.toByte()!!,
              shutdown_current_3_sb?.progress?.toByte()!!,
              shutdown_current_4_sb?.progress?.toByte()!!,
              shutdown_current_5_sb?.progress?.toByte()!!,
              shutdown_current_6_sb?.progress?.toByte()!!),
              SHUTDOWN_CURRENT_NEW_VM, 50)
          }
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            main?.bleCommandConnector(byteArrayOf(
              shutdown_current_1_sb?.progress?.toByte()!!,
              shutdown_current_2_sb?.progress?.toByte()!!,
              shutdown_current_3_sb?.progress?.toByte()!!,
              shutdown_current_4_sb?.progress?.toByte()!!,
              shutdown_current_5_sb?.progress?.toByte()!!,
              shutdown_current_6_sb?.progress?.toByte()!!),
              SHUTDOWN_CURRENT_NEW, WRITE, 0)
          }
          saveInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_1, seekBar.progress)
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersShutdownCurrent)
          RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(SHUTDOWN_CURRENT_NEW_VM)
        }
      }
    })
    eventYandexMetricaParametersShutdownCurrent = "{\"Screen advanced settings\":\"Change shutdown current 2\"}"
    shutdown_current_2_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        shutdown_current_2_tv?.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(
              shutdown_current_1_sb?.progress?.toByte()!!,
              shutdown_current_2_sb?.progress?.toByte()!!,
              shutdown_current_3_sb?.progress?.toByte()!!,
              shutdown_current_4_sb?.progress?.toByte()!!,
              shutdown_current_5_sb?.progress?.toByte()!!,
              shutdown_current_6_sb?.progress?.toByte()!!),
              SHUTDOWN_CURRENT_NEW_VM, 50)
          }
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            main?.bleCommandConnector(
              byteArrayOf(
                shutdown_current_1_sb?.progress?.toByte()!!,
                shutdown_current_2_sb?.progress?.toByte()!!,
                shutdown_current_3_sb?.progress?.toByte()!!,
                shutdown_current_4_sb?.progress?.toByte()!!,
                shutdown_current_5_sb?.progress?.toByte()!!,
                shutdown_current_6_sb?.progress?.toByte()!!
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
    shutdown_current_3_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        shutdown_current_3_tv?.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(
              shutdown_current_1_sb?.progress?.toByte()!!,
              shutdown_current_2_sb?.progress?.toByte()!!,
              shutdown_current_3_sb?.progress?.toByte()!!,
              shutdown_current_4_sb?.progress?.toByte()!!,
              shutdown_current_5_sb?.progress?.toByte()!!,
              shutdown_current_6_sb?.progress?.toByte()!!),
              SHUTDOWN_CURRENT_NEW_VM, 50)
          }
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            main?.bleCommandConnector(
              byteArrayOf(
                shutdown_current_1_sb?.progress?.toByte()!!,
                shutdown_current_2_sb?.progress?.toByte()!!,
                shutdown_current_3_sb?.progress?.toByte()!!,
                shutdown_current_4_sb?.progress?.toByte()!!,
                shutdown_current_5_sb?.progress?.toByte()!!,
                shutdown_current_6_sb?.progress?.toByte()!!
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
    shutdown_current_4_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        shutdown_current_4_tv?.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(
              shutdown_current_1_sb?.progress?.toByte()!!,
              shutdown_current_2_sb?.progress?.toByte()!!,
              shutdown_current_3_sb?.progress?.toByte()!!,
              shutdown_current_4_sb?.progress?.toByte()!!,
              shutdown_current_5_sb?.progress?.toByte()!!,
              shutdown_current_6_sb?.progress?.toByte()!!),
              SHUTDOWN_CURRENT_NEW_VM, 50)
          }
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            main?.bleCommandConnector(
              byteArrayOf(
                shutdown_current_1_sb?.progress?.toByte()!!,
                shutdown_current_2_sb?.progress?.toByte()!!,
                shutdown_current_3_sb?.progress?.toByte()!!,
                shutdown_current_4_sb?.progress?.toByte()!!,
                shutdown_current_5_sb?.progress?.toByte()!!,
                shutdown_current_6_sb?.progress?.toByte()!!
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
    shutdown_current_5_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        shutdown_current_5_tv?.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(
              shutdown_current_1_sb?.progress?.toByte()!!,
              shutdown_current_2_sb?.progress?.toByte()!!,
              shutdown_current_3_sb?.progress?.toByte()!!,
              shutdown_current_4_sb?.progress?.toByte()!!,
              shutdown_current_5_sb?.progress?.toByte()!!,
              shutdown_current_6_sb?.progress?.toByte()!!),
              SHUTDOWN_CURRENT_NEW_VM, 50)
          }
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            main?.bleCommandConnector(
              byteArrayOf(
                shutdown_current_1_sb?.progress?.toByte()!!,
                shutdown_current_2_sb?.progress?.toByte()!!,
                shutdown_current_3_sb?.progress?.toByte()!!,
                shutdown_current_4_sb?.progress?.toByte()!!,
                shutdown_current_5_sb?.progress?.toByte()!!,
                shutdown_current_6_sb?.progress?.toByte()!!
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
    shutdown_current_6_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        shutdown_current_6_tv?.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(
              shutdown_current_1_sb?.progress?.toByte()!!,
              shutdown_current_2_sb?.progress?.toByte()!!,
              shutdown_current_3_sb?.progress?.toByte()!!,
              shutdown_current_4_sb?.progress?.toByte()!!,
              shutdown_current_5_sb?.progress?.toByte()!!,
              shutdown_current_6_sb?.progress?.toByte()!!),
              SHUTDOWN_CURRENT_NEW_VM, 50)
          }
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            main?.bleCommandConnector(
              byteArrayOf(
                shutdown_current_1_sb?.progress?.toByte()!!,
                shutdown_current_2_sb?.progress?.toByte()!!,
                shutdown_current_3_sb?.progress?.toByte()!!,
                shutdown_current_4_sb?.progress?.toByte()!!,
                shutdown_current_5_sb?.progress?.toByte()!!,
                shutdown_current_6_sb?.progress?.toByte()!!
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
    swap_open_close_sw?.setOnClickListener {
      if (swap_open_close_sw?.isChecked!!) {
        swap_open_close_tv?.text = "1"
        main?.setSwapOpenCloseButton(true)
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, true)
      } else {
        swap_open_close_tv?.text = "0"
        main?.setSwapOpenCloseButton(false)
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)
      }
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersSwapOpenCloseButton)
    }


    val eventYandexMetricaParametersSingleChannel = "{\"Screen advanced settings\":\"Tup single channel control switch\"}"
    single_channel_control_sw?.setOnClickListener {
      if (!main?.lockWriteBeforeFirstRead!!) {
        if (single_channel_control_sw.isChecked) {
          single_channel_control_tv?.text = "1"
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
//          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, true)
          saveBool(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, true)
        } else {
          single_channel_control_tv?.text = "0"
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
//          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)
          saveBool(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)
        }
        YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersSingleChannel)
      }
    }

    on_off_prosthesis_blocking_sw?.setOnClickListener {
      if (!main?.lockWriteBeforeFirstRead!!) {
        if (on_off_prosthesis_blocking_sw?.isChecked!!) {
          on_off_prosthesis_blocking_tv?.text = "1"
          lockProsthesis = 0x01
          hold_to_lock_time_rl?.visibility = View.VISIBLE
          main?.stage = "advanced activity"
          main?.runSendCommand(byteArrayOf(sensorGestureSwitching,
            0.toByte(),
            peak_time_vm_sb?.progress?.toByte()!!,
            0.toByte(),
            lockProsthesis,
            (hold_to_lock_time_sb?.progress!!).toByte()), ROTATION_GESTURE_NEW_VM, 50)
          RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(ROTATION_GESTURE_NEW_VM)
          saveBool(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_LOCK_NUM, true)
        } else {
          on_off_prosthesis_blocking_tv?.text = "0"
          lockProsthesis = 0x00
          hold_to_lock_time_rl?.visibility = View.GONE
          main?.stage = "advanced activity"
          main?.runSendCommand(byteArrayOf(sensorGestureSwitching,
            0.toByte(),
            peak_time_vm_sb?.progress?.toByte()!!,
            0.toByte(), lockProsthesis,
            (hold_to_lock_time_sb?.progress!!).toByte()), ROTATION_GESTURE_NEW_VM, 50)
          RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(ROTATION_GESTURE_NEW_VM)
          saveBool(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_LOCK_NUM, false)
        }
      }
    }
    hold_to_lock_time_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        val time: String = when {
          ((hold_to_lock_time_sb?.progress?.plus(1))?.times(0.1)).toString().length == 4 -> {
            ((hold_to_lock_time_sb?.progress?.plus(1))?.times(0.1)).toString() + "c"
          }
          ((hold_to_lock_time_sb?.progress?.plus(1))?.times(0.1)).toString().length > 4 -> {
            ((hold_to_lock_time_sb?.progress?.plus(1))?.times(0.1)).toString().substring(0, 4) + "c"
          }
          else -> {
            ((hold_to_lock_time_sb?.progress?.plus(1))?.times(0.1)).toString() + "0c"
          }
        }
        hold_to_lock_time_tv?.text = time
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          main?.stage = "advanced activity"
          main?.runSendCommand(byteArrayOf(sensorGestureSwitching,
            0.toByte(),
            peak_time_vm_sb?.progress?.toByte()!!,
            0.toByte(),
            lockProsthesis,
            (hold_to_lock_time_sb?.progress!!).toByte()), ROTATION_GESTURE_NEW_VM, 50)
          RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(ROTATION_GESTURE_NEW_VM)
          saveInt(main?.mDeviceAddress + PreferenceKeys.HOLD_TO_LOCK_TIME_NUM, seekBar.progress)
        }
      }
    })


    val eventYandexMetricaParametersOnOffGesturesSwitching = "{\"Screen advanced settings\":\"On/off gesture switch using sensors\"}"
    on_off_sensor_gesture_switching_sw?.setOnClickListener {
      if (!main?.lockWriteBeforeFirstRead!!) {
        if (on_off_sensor_gesture_switching_sw?.isChecked!!) {
          on_off_sensor_gesture_switching_tv?.text = "1"
          sensorGestureSwitching = 0x01
          peak_time_rl?.visibility = View.VISIBLE
          if (mode.toInt() == 0) downtime_rl?.visibility = View.VISIBLE
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            downtime_rl?.visibility = View.GONE
            peak_time_rl?.visibility = View.GONE
            mode_new_rl?.visibility = View.GONE
            peak_time_vm_rl?.visibility = View.VISIBLE
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(sensorGestureSwitching,
              0.toByte(),
              peak_time_vm_sb?.progress?.toByte()!!,
              0.toByte(),
              lockProsthesis,
              (hold_to_lock_time_sb?.progress!!).toByte()), ROTATION_GESTURE_NEW_VM, 50)
            RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(ROTATION_GESTURE_NEW_VM)
          } else {
            if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
              mode_new_rl?.visibility = View.VISIBLE
              main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode, peak_time_sb?.progress?.toByte()!!, downtime_sb?.progress?.toByte()!!), ROTATION_GESTURE_NEW, WRITE)
            } else {
              mode_rl?.visibility = View.VISIBLE
              main?.bleCommandConnector(byteArrayOf(sensorGestureSwitching, mode, (peak_time_sb?.progress?.plus(5))?.toByte()!!, (downtime_sb?.progress?.plus(5))?.toByte()!!),
                ROTATION_GESTURE_NEW, WRITE, 17)
            }
          }
          saveBool(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, true)
        } else {
          on_off_sensor_gesture_switching_tv?.text = "0"
          sensorGestureSwitching = 0x00
          peak_time_rl?.visibility = View.GONE
          downtime_rl?.visibility = View.GONE
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            mode_new_rl?.visibility = View.GONE
            peak_time_vm_rl?.visibility = View.GONE
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(sensorGestureSwitching,
              0.toByte(),
              peak_time_vm_sb?.progress?.toByte()!!,
              0.toByte(),
              lockProsthesis,
              (hold_to_lock_time_sb?.progress!!).toByte()), ROTATION_GESTURE_NEW_VM, 50)
            RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(ROTATION_GESTURE_NEW_VM)
          } else {
            if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
              mode_new_rl?.visibility = View.GONE
              main?.runWriteData(
                byteArrayOf(
                  sensorGestureSwitching,
                  mode,
                  peak_time_sb?.progress?.toByte()!!,
                  downtime_sb?.progress?.toByte()!!
                ), ROTATION_GESTURE_NEW, WRITE
              )
            } else {
              mode_rl?.visibility = View.GONE
              main?.bleCommandConnector(
                byteArrayOf(
                  sensorGestureSwitching,
                  mode,
                  (peak_time_sb?.progress?.plus(5))?.toByte()!!,
                  (downtime_sb?.progress?.plus(5))?.toByte()!!
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
    mode_sw?.setOnClickListener {
      if (!main?.lockWriteBeforeFirstRead!!) {
        if (mode_sw?.isChecked!!) {
          mode_tv?.text = "двумя\nдатчиками"
          mode = 0x01
          downtime_rl?.visibility = View.GONE
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H) || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(sensorGestureSwitching, mode, peak_time_sb?.progress?.toByte()!!, downtime_sb?.progress?.toByte()!!), ROTATION_GESTURE_NEW, 50)
          } else {
            main?.bleCommandConnector(byteArrayOf(sensorGestureSwitching, mode, (peak_time_sb?.progress?.plus(5))?.toByte()!!, (downtime_sb?.progress?.plus(5))?.toByte()!!),
              ROTATION_GESTURE_NEW, WRITE, 17)
          }
          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, true)
        } else {
          mode_tv?.text = "одним\nдатчиком"
          mode = 0x00
          downtime_rl?.visibility = View.VISIBLE
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H) || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.stage = "advanced activity"
            main?.runSendCommand(byteArrayOf(sensorGestureSwitching, mode, peak_time_sb?.progress?.toByte()!!, downtime_sb?.progress?.toByte()!!), ROTATION_GESTURE_NEW, 50)
          } else {
            main?.bleCommandConnector(byteArrayOf(sensorGestureSwitching, mode, (peak_time_sb?.progress?.plus(5))?.toByte()!!, (downtime_sb?.progress?.plus(5))?.toByte()!!),
              ROTATION_GESTURE_NEW, WRITE, 17)
          }
          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, false)
        }
        YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersMode)
      }
    }
    mode_new_sw?.setOnSwitchListener { position, _ ->
      if (position == 0) {
//        Toast.makeText(main?.baseContext,  "0", Toast.LENGTH_SHORT).show()
        saveInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NEW_NUM, 0)
        mode = 0
        if (sensorGestureSwitching.toInt() == 1) downtime_rl?.visibility = View.VISIBLE
        main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode, peak_time_sb?.progress?.toByte()!!, downtime_sb?.progress?.toByte()!!), ROTATION_GESTURE_NEW, WRITE)
      }
      if (position == 1) {
//        Toast.makeText(main?.baseContext, "1", Toast.LENGTH_SHORT).show()
        saveInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NEW_NUM, 1)
        mode = 1
        downtime_rl?.visibility = View.GONE
        main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode, peak_time_sb?.progress?.toByte()!!, downtime_sb?.progress?.toByte()!!), ROTATION_GESTURE_NEW, WRITE)
      }
      if (position == 2) {
//        Toast.makeText(main?.baseContext, "2", Toast.LENGTH_SHORT).show()
        saveInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NEW_NUM, 2)
        mode = 2
        downtime_rl?.visibility = View.GONE
        main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode, peak_time_sb?.progress?.toByte()!!, downtime_sb?.progress?.toByte()!!), ROTATION_GESTURE_NEW, WRITE)
      }
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersMode)
    }
    mode_new_sw?.selectedTab = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NEW_NUM, 0)

    val eventYandexMetricaParametersPeakTimeVM = "{\"Screen advanced settings\":\"Change peak time VM\"}"
    peak_time_vm_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
        peak_time_vm_tv?.text = time
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
        peak_time_vm_tv?.text = time
        if (!main?.lockWriteBeforeFirstRead!!) {
          main?.stage = "advanced activity"
          main?.runSendCommand(byteArrayOf(sensorGestureSwitching,
            0.toByte(),
            peak_time_vm_sb?.progress?.toByte()!!,
            0.toByte(),
            lockProsthesis,
            (hold_to_lock_time_sb?.progress!!).toByte()), ROTATION_GESTURE_NEW_VM, 50)
          RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(ROTATION_GESTURE_NEW_VM)

          saveInt(main?.mDeviceAddress + PreferenceKeys.SET_PEAK_TIME_VM_NUM, seekBar.progress)
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersPeakTimeVM)
        }
      }
    })

    val eventYandexMetricaParametersPeakTime = "{\"Screen advanced settings\":\"Change peak time\"}"
    peak_time_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
          time = (peak_time_sb?.progress?.times(0.04)).toString() + "c"
        }
        peak_time_tv?.text = time
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
          time = (peak_time_sb?.progress?.times(0.04)).toString() + "c"
        }
        peak_time_tv?.text = time
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode, peak_time_sb?.progress?.toByte()!!, downtime_sb?.progress?.toByte()!!), ROTATION_GESTURE_NEW, WRITE)
          } else {
            main?.bleCommandConnector(byteArrayOf(0x00, sensorGestureSwitching, mode, (peak_time_sb?.progress?.plus(5))?.toByte()!!, (downtime_sb?.progress?.plus(5))?.toByte()!!),
              ROTATION_GESTURE_NEW, WRITE, 17)
          }
          preferenceManager.putInt(main?.mDeviceAddress + PreferenceKeys.SET_PEAK_TIME_NUM, seekBar.progress)
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersPeakTime)
        }
      }
    })

    val eventYandexMetricaParametersDowntime = "{\"Screen advanced settings\":\"Change downtime\"}"
    downtime_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
          time = (downtime_sb?.progress?.times(0.04)).toString() + "c"
        }
        downtime_tv?.text = time
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
          time = (downtime_sb?.progress?.times(0.04)).toString() + "c"
        }
        downtime_tv?.text = time
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode, peak_time_sb?.progress?.toByte()!!, downtime_sb?.progress?.toByte()!!), ROTATION_GESTURE_NEW, WRITE)
          } else {
            main?.bleCommandConnector(byteArrayOf(0x00, sensorGestureSwitching, mode, (peak_time_sb?.progress?.plus(5))?.toByte()!!, (downtime_sb?.progress?.plus(5))?.toByte()!!),
              ROTATION_GESTURE_NEW, WRITE, 17)
          }
          preferenceManager.putInt(main?.mDeviceAddress + PreferenceKeys.SET_DOWNTIME_NUM, seekBar.progress)
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersDowntime)
        }
      }
    })


    val eventYandexMetricaParametersGetTelemetryNumber = "{\"Screen advanced settings\":\"Tup get telemetry number button\"}"
    get_setup_btn?.setOnClickListener {
      if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
        main?.bleCommandConnector(byteArrayOf(0x00), TELEMETRY_NUMBER_NEW, READ, 17)
      }
      if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
        main?.bleCommandConnector(byteArrayOf(0x00), TELEMETRY_NUMBER_NEW_VM, READ, 17)
      }
      main?.lockChangeTelemetryNumber = true
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersGetTelemetryNumber)
    }
    val eventYandexMetricaParametersSetTelemetryNumber = "{\"Screen advanced settings\":\"Tup set telemetry number button\"}"
    set_setup_btn?.setOnClickListener {
      if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
        main?.bleCommandConnector(
          telemetry_number_et?.text.toString().toByteArray(Charsets.UTF_8),
          TELEMETRY_NUMBER_NEW,
          WRITE,
          17
        )
      }
      if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
        main?.stage = "advanced activity"
        main?.runSendCommand(telemetry_number_et?.text.toString().toByteArray(Charsets.UTF_8),
          TELEMETRY_NUMBER_NEW_VM, 50)


        val dataset256 = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345"
        System.err.println("dataset_256:" + dataset256.length)

        main?.runWriteData(dataset256.toByteArray(Charsets.UTF_8),
          TELEMETRY_NUMBER_NEW_VM,
          WRITE)
      }
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersSetTelemetryNumber)
    }
    main?.telemetryNumber = telemetry_number_et?.text.toString()


    val eventYandexMetricaParametersLeftRight = "{\"Screen advanced settings\":\"Tup left right side swap switch\"}"
    left_right_side_swap_sw?.setOnClickListener{
      System.err.println(" LOLOLOEFWEF --->  side key : ${main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE}")
      showAlertChangeSideDialog()
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersLeftRight)
    }


    val eventYandexMetricaParametersFingersDelay = "{\"Screen advanced settings\":\"Tup  time delay fingers swap switch\"}"
    time_delay_of_fingers_swap_sw?.setOnClickListener {
      if (time_delay_of_fingers_swap_sw.isChecked) {
        System.err.println("time_delay_of_fingers_swap_sw 1")
        time_delay_of_fingers_swap_tv?.text = "1"
        saveInt(main?.mDeviceAddress + PreferenceKeys.SET_FINGERS_DELAY, 1)
      } else {
        System.err.println("time_delay_of_fingers_swap_sw 0")
        time_delay_of_fingers_swap_tv?.text = "0"
        saveInt(main?.mDeviceAddress + PreferenceKeys.SET_FINGERS_DELAY, 0)
      }
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersFingersDelay)
    }


    val eventYandexMetricaParametersReset = "{\"Screen advanced settings\":\"Tup reset to factory settings button\"}"
    reset_to_factory_settings_btn?.setOnClickListener {
      if (!main?.lockWriteBeforeFirstRead!!) {
        System.err.println("tuk reset_to_factory_settings_btn")
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
          main?.stage = "advanced activity"
          main?.runSendCommand(byteArrayOf(0x01), RESET_TO_FACTORY_SETTINGS_NEW_VM, 50)
        } else {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            main?.runWriteData(byteArrayOf(0x01), RESET_TO_FACTORY_SETTINGS_NEW, WRITE)
          } else {
            main?.firstActivateSetScaleDialog = false
            main?.bleCommandConnector(byteArrayOf(0x01), RESET_TO_FACTORY_SETTINGS, WRITE, 15)
          }
        }



        swap_open_close_tv?.text = "0"
        main?.setSwapOpenCloseButton(false)
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)

        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, false)

        swap_open_close_sw?.isChecked = false
        swap_open_close_tv?.text = "0"
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)

        preferenceManager.putInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM, 80)
        ObjectAnimator.ofInt(shutdown_current_sb, "progress", preferenceManager.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM, 80)).setDuration(200).start()

        single_channel_control_sw?.isChecked = false
        single_channel_control_tv?.text = "0"
//        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)
        saveBool(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)

        on_off_sensor_gesture_switching_sw?.isChecked = false
        on_off_sensor_gesture_switching_tv?.text = "0"
        sensorGestureSwitching = 0x00
        mode_rl.visibility = View.GONE
        peak_time_rl.visibility = View.GONE
        downtime_rl.visibility = View.GONE
        saveBool(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false)


        mode_tv?.text = "одним\nдатчиком"
        mode = 0x00
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, false)

        YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersReset)
        RxUpdateMainEvent.getInstance().updateReadCharacteristicBLE(SHUTDOWN_CURRENT_NEW_VM)
      }
    }


    val eventYandexMetricaParametersCalibrationAdv = "{\"Screen advanced settings\":\"Tup calibration button\"}"
    calibration_adv_btn?.setOnClickListener {
      if (mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1) == 1) {
        main?.stage = "advanced activity"
        main?.runSendCommand(byteArrayOf(0x09), CALIBRATION_NEW_VM, 50)
      } else {
        main?.stage = "advanced activity"
        main?.runSendCommand(byteArrayOf(0x0a), CALIBRATION_NEW_VM, 50)
      }
      saveInt(main?.mDeviceAddress + PreferenceKeys.CALIBRATING_STATUS, 1)
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersCalibrationAdv)
    }
    val eventYandexMetricaParametersCalibrationStatusAdv = "{\"Screen advanced settings\":\"Tup calibration status button\"}"
    calibration_status_adv_btn?.setOnClickListener {
      if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
        main?.runReadDataAllCharacteristics(STATUS_CALIBRATION_NEW_VM)
      } else {
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
          main?.runReadDataAllCharacteristics(STATUS_CALIBRATION_NEW)
        }
      }
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersCalibrationStatusAdv)
    }


    debug_screen_btn?.setOnClickListener {
      val intent = Intent(context, GripperTestScreenWithEncodersActivity::class.java)
      startActivity(intent)
    }

    //Скрывает настройки, которые не актуальны для многосхватной бионики
    if ( main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_FEST_A)
      || main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_BT05)
      || main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_MY_IPHONE)
      || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)
      || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
      shutdown_current_rl?.visibility = View.GONE
    }
    //Скрывает настройки, которые не актуальны для бионик кроме FEST-H и FEST-X
    when {
        main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X) -> {
          telemetry_rl?.visibility = View.VISIBLE
          on_off_prosthesis_blocking_rl?.visibility = View.VISIBLE
          scale_tv?.visibility = View.GONE
        }
        main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H) -> {
          telemetry_rl?.visibility = View.VISIBLE
          debug_screen_rl?.visibility = View.GONE
          scale_tv?.visibility = View.GONE
        }
        else -> {
          debug_screen_rl?.visibility = View.GONE
          telemetry_rl?.visibility = View.GONE
          calibration_rl?.visibility = View.GONE
          shutdown_current_1_rl?.visibility = View.GONE
          shutdown_current_2_rl?.visibility = View.GONE
          shutdown_current_3_rl?.visibility = View.GONE
          shutdown_current_4_rl?.visibility = View.GONE
          shutdown_current_5_rl?.visibility = View.GONE
          shutdown_current_6_rl?.visibility = View.GONE
          side_rl?.visibility = View.GONE
          on_off_sensor_gesture_switching_rl?.visibility = View.GONE
        }
    }

    swap_open_close_sw?.isChecked = preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)
    single_channel_control_sw?.isChecked = mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)


    if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
      preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, false)
    }
    mode_sw?.isChecked = preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, false)
    if (preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)) swap_open_close_tv?.text = "1"
    if (mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)) single_channel_control_tv?.text = "1"


    if (preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, false)) {
      mode_tv?.text = "двумя\nдатчиками"
      mode = 0x01
      downtime_rl?.visibility = View.GONE
    } else {
      mode_tv?.text = "одним\nдатчиком"
      mode = 0x00
    }

    val versionName = BuildConfig.VERSION_NAME
    version_app_tv?.text = (mContext?.resources?.getString(R.string.version_app) ?: "lol: ") + " " + versionName
  }

  @SuppressLint("Recycle", "SetTextI18n")
  private fun updateAllParameters() {
    main?.runOnUiThread {
      System.err.println("Принятые данные состояния токов ОБНОВЛЕНИЕ")
      ObjectAnimator.ofInt(shutdown_current_sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM, 80)).setDuration(200).start()
      ObjectAnimator.ofInt(shutdown_current_1_sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_1, 80)).setDuration(200).start()
      ObjectAnimator.ofInt(shutdown_current_2_sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_2, 80)).setDuration(200).start()
      ObjectAnimator.ofInt(shutdown_current_3_sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_3, 80)).setDuration(200).start()
      ObjectAnimator.ofInt(shutdown_current_4_sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_4, 80)).setDuration(200).start()
      ObjectAnimator.ofInt(shutdown_current_5_sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_5, 80)).setDuration(200).start()
      ObjectAnimator.ofInt(shutdown_current_6_sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_6, 80)).setDuration(200).start()


      ObjectAnimator.ofInt(hold_to_lock_time_sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.HOLD_TO_LOCK_TIME_NUM, 15)).setDuration(200).start()
      ObjectAnimator.ofInt(peak_time_vm_sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SET_PEAK_TIME_VM_NUM, 15)).setDuration(200).start()
      ObjectAnimator.ofInt(peak_time_sb, "progress", preferenceManager.getInt(main?.mDeviceAddress + PreferenceKeys.SET_PEAK_TIME_NUM, 15)).setDuration(200).start()
      ObjectAnimator.ofInt(downtime_sb, "progress", preferenceManager.getInt(main?.mDeviceAddress + PreferenceKeys.SET_DOWNTIME_NUM, 15)).setDuration(200).start()
    }

    shutdown_current_1_tv?.text = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_1, 80).toString()
    shutdown_current_2_tv?.text = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_2, 80).toString()
    shutdown_current_3_tv?.text = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_3, 80).toString()
    shutdown_current_4_tv?.text = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_4, 80).toString()
    shutdown_current_5_tv?.text = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_5, 80).toString()
    shutdown_current_6_tv?.text = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_6, 80).toString()
    shutdown_current_tv?.text = shutdown_current_sb?.progress.toString()

    current  = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM,   80)
    current1 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_1, 80)
    current2 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_2, 80)
    current3 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_3, 80)
    current4 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_4, 80)
    current5 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_5, 80)
    current6 = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_6, 80)

    if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
      var time: String = when {
        ((peak_time_vm_sb?.progress?.plus(1))?.times(0.1)).toString().length == 4 -> {
          ((peak_time_vm_sb?.progress?.plus(1))?.times(0.1)).toString() + "c"
        }
        ((peak_time_vm_sb?.progress?.plus(1))?.times(0.1)).toString().length > 4 -> {
          ((peak_time_vm_sb?.progress?.plus(1))?.times(0.1)).toString().substring(0, 4) + "c"
        }
        else -> {
          ((peak_time_vm_sb?.progress?.plus(1))?.times(0.1)).toString() + "0c"
        }
      }
      peak_time_vm_tv?.text = time
      time = when {
        ((hold_to_lock_time_sb?.progress?.plus(1))?.times(0.1)).toString().length == 4 -> {
          ((hold_to_lock_time_sb?.progress?.plus(1))?.times(0.1)).toString() + "c"
        }
        ((hold_to_lock_time_sb?.progress?.plus(1))?.times(0.1)).toString().length > 4 -> {
          ((hold_to_lock_time_sb?.progress?.plus(1))?.times(0.1)).toString().substring(0, 4) + "c"
        }
        else -> {
          ((hold_to_lock_time_sb?.progress?.plus(1))?.times(0.1)).toString() + "0c"
        }
      }
      hold_to_lock_time_tv?.text = time
    }
    if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
      var time: String = when {
      ((peak_time_sb?.progress?.plus(5))?.times(0.05)).toString().length == 4 -> {
        ((peak_time_sb?.progress?.plus(5))?.times(0.05)).toString() + "c"
      }
      ((peak_time_sb?.progress?.plus(5))?.times(0.05)).toString().length > 4 -> {
        ((peak_time_sb?.progress?.plus(5))?.times(0.05)).toString().substring(0, 4) + "c"
      }
      else -> {
        ((peak_time_sb?.progress?.plus(5))?.times(0.05)).toString() + "0c"
      }
    }
//      time = (peak_time_sb?.progress?.times(0.04)).toString() + "c"
      peak_time_tv?.text = time
      time = when {
        ((downtime_sb?.progress?.plus(5))?.times(0.05)).toString().length == 4 -> {
          ((downtime_sb?.progress?.plus(5))?.times(0.05)).toString() + "c"
        }
        ((downtime_sb?.progress?.plus(5))?.times(0.05)).toString().length > 4 -> {
          ((downtime_sb?.progress?.plus(5))?.times(0.05)).toString().substring(0, 4) + "c"
        }
        else -> {
          ((downtime_sb?.progress?.plus(5))?.times(0.05)).toString() + "0c"
        }
      }
//      time = (downtime_sb?.progress?.times(0.04)).toString() + "c"
      downtime_tv?.text = time
    }


    if (mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_LOCK_NUM, false)) {
      on_off_prosthesis_blocking_sw?.isChecked = true
      on_off_prosthesis_blocking_tv?.text = "1"
      if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
        hold_to_lock_time_rl?.visibility = View.VISIBLE
      }
    } else {
      on_off_prosthesis_blocking_sw?.isChecked = false
      on_off_prosthesis_blocking_tv?.text = "0"
      hold_to_lock_time_rl?.visibility = View.GONE
    }


    when (mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SET_SCALE, 0)) {
      0 -> { scale_tv?.text = resources.getString(R.string.scale) + " S" }
      1 -> { scale_tv?.text = resources.getString(R.string.scale) + " M" }
      2 -> { scale_tv?.text = resources.getString(R.string.scale) + " L" }
      3 -> { scale_tv?.text = resources.getString(R.string.scale) + " XL" }
    }

    single_channel_control_sw?.isChecked = mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)
    if (mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)) { single_channel_control_tv?.text = "1" } else { single_channel_control_tv?.text = "0" }

    on_off_sensor_gesture_switching_sw?.isChecked = mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false)
    if (mSettings!!.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false)) {
      if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
        peak_time_vm_rl?.visibility = View.VISIBLE
      }
      if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
        mode_new_rl?.visibility = View.VISIBLE
        peak_time_rl?.visibility = View.VISIBLE
        //в зависимости от выбранного мода показывать тот или иной сетап сикбаров
        when(mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NEW_NUM, 0)) {
          0 -> {
            downtime_rl?.visibility = View.VISIBLE
          }
        }

      }
      on_off_sensor_gesture_switching_tv?.text = "1"
      sensorGestureSwitching = 0x01
    } else {
      on_off_sensor_gesture_switching_tv?.text = "0"
      sensorGestureSwitching = 0x00
      mode_rl?.visibility = View.GONE
      peak_time_vm_rl?.visibility = View.GONE
      peak_time_rl?.visibility = View.GONE
      downtime_rl?.visibility = View.GONE
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
      if (left_right_side_swap_sw.isChecked) {
        left_right_side_swap_tv?.text = Html.fromHtml(getString(R.string.right))
        saveInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1)
      } else {
        left_right_side_swap_tv?.text = resources.getString(R.string.left)
        saveInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 0)
      }


      //TODO запускать таймер, который раз в полсекунды отслеживает, была ли отправлена команда
      // (по состоянию ключа CALIBRATING_STATUS =    1-была    0-не была   )
      // и если не была, то повторяет отправку
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
        left_right_side_swap_sw?.isChecked = true
        left_right_side_swap_tv?.text = Html.fromHtml(getString(R.string.right))
      } else {
        left_right_side_swap_sw?.isChecked = false
        left_right_side_swap_tv?.text = resources.getString(R.string.left)
      }
      myDialog.dismiss()
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
}
