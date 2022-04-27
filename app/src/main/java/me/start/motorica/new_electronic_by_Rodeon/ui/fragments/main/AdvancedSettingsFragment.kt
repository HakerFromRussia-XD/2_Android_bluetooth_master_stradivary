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
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.yandex.metrica.YandexMetrica
import kotlinx.android.synthetic.main.layout_advanced_settings.*
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager
import me.start.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.*
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceManager
import me.start.motorica.new_electronic_by_Rodeon.persistence.sqlite.SqliteManager
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
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
  private var threadFlag = true
  private var changeParameter = false
  private var updatingUIThread: Thread? = null
//  private var showCalibratingStatus: Boolean = false

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val rootView = inflater.inflate(R.layout.layout_advanced_settings, container, false)
    WDApplication.component.inject(this)
    if (activity != null) { main = activity as MainActivity? }
    this.rootView = rootView
    this.mContext = context
    scale = resources.displayMetrics.density



    return rootView
  }

  override fun onPause() {
    super.onPause()
    threadFlag = false
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initializeUI()
    updateAllParameters()
    Handler().postDelayed({
      startUpdatingUIThread()
    }, 2000)

//    RxUpdateMainEvent.getInstance().gestureStateObservable
//            .compose(bindToLifecycle())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe { parameters ->
//            }
  }

  @SuppressLint("SetTextI18n", "CheckResult", "Recycle")
  private fun initializeUI() {
    mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
    if (main?.locate?.contains("ru")!!) {
      shutdown_current_text_tv?.textSize = 11f
      swap_button_open_close_tv?.textSize = 11f
      single_channel_control_text_tv?.textSize = 11f
      on_off_sensor_gesture_switching_text_tv?.textSize = 11f
      mode_text_tv?.textSize = 11f
      peak_time_text_tv?.textSize = 11f
      downtime_text_tv?.textSize = 11f
      mode_tv?.textSize = 11f
      reset_to_factory_settings_btn?.textSize = 12f
      calibration_adv_btn?.textSize = 10f
      calibration_status_adv_btn?.textSize = 10f
      side_text_tv?.textSize = 11f
      left_right_side_swap_tv?.textSize = 11f
      shutdown_current_1_text_tv?.textSize = 11f
      shutdown_current_2_text_tv?.textSize = 11f
      shutdown_current_3_text_tv?.textSize = 11f
      shutdown_current_4_text_tv?.textSize = 11f
      shutdown_current_5_text_tv?.textSize = 11f
      shutdown_current_6_text_tv?.textSize = 11f
    }
    if (mSettings?.getInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1) == 1) {
      left_right_side_swap_sw?.isChecked = true
      left_right_side_swap_tv?.text = Html.fromHtml(getString(R.string.right))
    } else {
      left_right_side_swap_sw?.isChecked = false
      left_right_side_swap_tv?.text = resources.getString(R.string.left)
    }

    if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
      val calibrationStartParams: LinearLayout.LayoutParams =
        calibration_start_button_ll.layoutParams as LinearLayout.LayoutParams
      calibrationStartParams.weight = 1f
      val calibrationStatusParams: LinearLayout.LayoutParams =
        calibration_status_button_ll.layoutParams as LinearLayout.LayoutParams
      calibrationStatusParams.weight = 1f
    } else {
      if (main?.locate?.contains("ru")!!) { calibration_status_adv_btn?.textSize = 12f }
    }

    var eventYandexMetricaParametersShutdownCurrent = "{\"Screen advanced settings\":\"Change shutdown current\"}"
    shutdown_current_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        changeParameter = true
        shutdown_current_tv?.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        changeParameter = false
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
        changeParameter = true
        shutdown_current_1_tv?.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        changeParameter = false
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.bleCommandConnector(byteArrayOf(shutdown_current_1_sb?.progress?.toByte()!!, shutdown_current_2_sb?.progress?.toByte()!!,
              shutdown_current_3_sb?.progress?.toByte()!!, shutdown_current_4_sb?.progress?.toByte()!!,
              shutdown_current_5_sb?.progress?.toByte()!!, shutdown_current_6_sb?.progress?.toByte()!!), SHUTDOWN_CURRENT_NEW_VM, WRITE, 0)
          }
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            main?.bleCommandConnector(byteArrayOf(shutdown_current_1_sb?.progress?.toByte()!!, shutdown_current_2_sb?.progress?.toByte()!!,
              shutdown_current_3_sb?.progress?.toByte()!!, shutdown_current_4_sb?.progress?.toByte()!!,
              shutdown_current_5_sb?.progress?.toByte()!!, shutdown_current_6_sb?.progress?.toByte()!!), SHUTDOWN_CURRENT_NEW, WRITE, 0)
          }
          saveInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_1, seekBar.progress)
          YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersShutdownCurrent)
        }
      }
    })
    eventYandexMetricaParametersShutdownCurrent = "{\"Screen advanced settings\":\"Change shutdown current 2\"}"
    shutdown_current_2_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        changeParameter = true
        shutdown_current_2_tv?.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        changeParameter = false
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.bleCommandConnector(
              byteArrayOf(
                shutdown_current_1_sb?.progress?.toByte()!!,
                shutdown_current_2_sb?.progress?.toByte()!!,
                shutdown_current_3_sb?.progress?.toByte()!!,
                shutdown_current_4_sb?.progress?.toByte()!!,
                shutdown_current_5_sb?.progress?.toByte()!!,
                shutdown_current_6_sb?.progress?.toByte()!!
              ), SHUTDOWN_CURRENT_NEW_VM, WRITE, 0
            )
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
        }
      }
    })
    eventYandexMetricaParametersShutdownCurrent = "{\"Screen advanced settings\":\"Change shutdown current 3\"}"
    shutdown_current_3_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        changeParameter = true
        shutdown_current_3_tv?.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        changeParameter = false
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.bleCommandConnector(
              byteArrayOf(
                shutdown_current_1_sb?.progress?.toByte()!!,
                shutdown_current_2_sb?.progress?.toByte()!!,
                shutdown_current_3_sb?.progress?.toByte()!!,
                shutdown_current_4_sb?.progress?.toByte()!!,
                shutdown_current_5_sb?.progress?.toByte()!!,
                shutdown_current_6_sb?.progress?.toByte()!!
              ), SHUTDOWN_CURRENT_NEW_VM, WRITE, 0
            )
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
        }
      }
    })
    eventYandexMetricaParametersShutdownCurrent = "{\"Screen advanced settings\":\"Change shutdown current 4\"}"
    shutdown_current_4_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        changeParameter = true
        shutdown_current_4_tv?.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        changeParameter = false
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.bleCommandConnector(
              byteArrayOf(
                shutdown_current_1_sb?.progress?.toByte()!!,
                shutdown_current_2_sb?.progress?.toByte()!!,
                shutdown_current_3_sb?.progress?.toByte()!!,
                shutdown_current_4_sb?.progress?.toByte()!!,
                shutdown_current_5_sb?.progress?.toByte()!!,
                shutdown_current_6_sb?.progress?.toByte()!!
              ), SHUTDOWN_CURRENT_NEW_VM, WRITE, 0
            )
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
        }
      }
    })
    eventYandexMetricaParametersShutdownCurrent = "{\"Screen advanced settings\":\"Change shutdown current 5\"}"
    shutdown_current_5_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        changeParameter = true
        shutdown_current_5_tv?.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        changeParameter = false
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.bleCommandConnector(
              byteArrayOf(
                shutdown_current_1_sb?.progress?.toByte()!!,
                shutdown_current_2_sb?.progress?.toByte()!!,
                shutdown_current_3_sb?.progress?.toByte()!!,
                shutdown_current_4_sb?.progress?.toByte()!!,
                shutdown_current_5_sb?.progress?.toByte()!!,
                shutdown_current_6_sb?.progress?.toByte()!!
              ), SHUTDOWN_CURRENT_NEW_VM, WRITE, 0
            )
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
        }
      }
    })
    eventYandexMetricaParametersShutdownCurrent = "{\"Screen advanced settings\":\"Change shutdown current 6\"}"
    shutdown_current_6_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        changeParameter = true
        shutdown_current_6_tv?.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        changeParameter = false
        if (!main?.lockWriteBeforeFirstRead!!) {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.bleCommandConnector(
              byteArrayOf(
                shutdown_current_1_sb?.progress?.toByte()!!,
                shutdown_current_2_sb?.progress?.toByte()!!,
                shutdown_current_3_sb?.progress?.toByte()!!,
                shutdown_current_4_sb?.progress?.toByte()!!,
                shutdown_current_5_sb?.progress?.toByte()!!,
                shutdown_current_6_sb?.progress?.toByte()!!
              ), SHUTDOWN_CURRENT_NEW_VM, WRITE, 0
            )
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
        }
      }
    })


    val eventYandexMetricaParametersSwapOpenCloseButton = "{\"Screen advanced settings\":\"Tup swap open close button\"}"
    swap_open_close_sw?.setOnClickListener {
      if (swap_open_close_sw?.isChecked!!) {
        swap_open_close_tv?.text = 1.toString()
        main?.setSwapOpenCloseButton(true)
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, true)
      } else {
        swap_open_close_tv?.text = 0.toString()
        main?.setSwapOpenCloseButton(false)
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)
      }
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersSwapOpenCloseButton)
    }


    val eventYandexMetricaParametersSingleChannel = "{\"Screen advanced settings\":\"Tup single channel control switch\"}"
    single_channel_control_sw?.setOnClickListener {
      if (!main?.lockWriteBeforeFirstRead!!) {
        if (single_channel_control_sw.isChecked) {
          single_channel_control_tv?.text = 1.toString()
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.runWriteData(byteArrayOf(0x01), SET_ONE_CHANNEL_NEW_VM, WRITE)
          } else {
            if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
              main?.runWriteData(byteArrayOf(0x01), SET_ONE_CHANNEL_NEW, WRITE)
            } else {
              main?.bleCommandConnector(byteArrayOf(0x01), SET_ONE_CHANNEL, WRITE, 16)
            }
          }
          main?.setOneChannelNum = 1
          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, true)
        } else {
          single_channel_control_tv?.text = 0.toString()
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
            main?.runWriteData(byteArrayOf(0x00), SET_ONE_CHANNEL_NEW_VM, WRITE)
          } else {
            if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
              main?.runWriteData(byteArrayOf(0x00), SET_ONE_CHANNEL_NEW, WRITE)
            } else {
              main?.bleCommandConnector(byteArrayOf(0x00), SET_ONE_CHANNEL, WRITE, 16)
            }
          }
          main?.setOneChannelNum = 0
          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)
        }
        YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersSingleChannel)
      }
    }

    val eventYandexMetricaParametersOnOffGesturesSwitching = "{\"Screen advanced settings\":\"On/off gesture switch using sensors\"}"
    on_off_sensor_gesture_switching_sw?.setOnClickListener {
      if (!main?.lockWriteBeforeFirstRead!!) {
        if (on_off_sensor_gesture_switching_sw?.isChecked!!) {
          on_off_sensor_gesture_switching_tv?.text = 1.toString()
          sensorGestureSwitching = 0x01
          peak_time_rl?.visibility = View.VISIBLE
          if (mode.toInt() == 0) downtime_rl?.visibility = View.VISIBLE
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            //TODO показывать меню пройного выбора, а обычный свич (переключение двумя/одним датчиком) скрывать
            mode_new_rl?.visibility = View.VISIBLE
            main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode, peak_time_sb?.progress?.toByte()!!, downtime_sb?.progress?.toByte()!!), ROTATION_GESTURE_NEW, WRITE)
          } else {
            mode_rl?.visibility = View.VISIBLE
            main?.bleCommandConnector(byteArrayOf(sensorGestureSwitching, mode, (peak_time_sb?.progress?.plus(5))?.toByte()!!, (downtime_sb?.progress?.plus(5))?.toByte()!!),
              ROTATION_GESTURE_NEW, WRITE, 17)
          }
          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, true)
        } else {
          on_off_sensor_gesture_switching_tv?.text = 0.toString()
          sensorGestureSwitching = 0x00
          peak_time_rl?.visibility = View.GONE
          downtime_rl?.visibility = View.GONE
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            //TODO скрывать меню пройного выбора
            mode_new_rl?.visibility = View.GONE
            main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode, peak_time_sb?.progress?.toByte()!!, downtime_sb?.progress?.toByte()!!), ROTATION_GESTURE_NEW, WRITE)
          } else {
            mode_rl?.visibility = View.GONE
            main?.bleCommandConnector(byteArrayOf(sensorGestureSwitching, mode, (peak_time_sb?.progress?.plus(5))?.toByte()!!, (downtime_sb?.progress?.plus(5))?.toByte()!!),
              ROTATION_GESTURE_NEW, WRITE, 17)
          }
          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false)
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
            main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode, peak_time_sb?.progress?.toByte()!!, downtime_sb?.progress?.toByte()!!), ROTATION_GESTURE_NEW, WRITE)
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
            main?.runWriteData(byteArrayOf(sensorGestureSwitching, mode, peak_time_sb?.progress?.toByte()!!, downtime_sb?.progress?.toByte()!!), ROTATION_GESTURE_NEW, WRITE)
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
        main?.bleCommandConnector(
          telemetry_number_et?.text.toString().toByteArray(Charsets.UTF_8),
          TELEMETRY_NUMBER_NEW_VM,
          WRITE,
          17
        )
      }
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersSetTelemetryNumber)
    }
    main?.telemetryNumber = telemetry_number_et?.text.toString()


    val eventYandexMetricaParametersLeftRight = "{\"Screen advanced settings\":\"Tup left right side swap switch\"}"
    left_right_side_swap_sw?.setOnClickListener{
      if (left_right_side_swap_sw.isChecked) {
        left_right_side_swap_tv?.text = Html.fromHtml(getString(R.string.right))
        saveInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1)
      } else {
        left_right_side_swap_tv?.text = resources.getString(R.string.left)
        saveInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 0)
      }
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersLeftRight)
    }


    val eventYandexMetricaParametersPeakTime = "{\"Screen advanced settings\":\"Change peak time\"}"
    peak_time_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        changeParameter = true
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
        changeParameter = false
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
        changeParameter = true
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
        changeParameter = false
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


    val eventYandexMetricaParametersReset = "{\"Screen advanced settings\":\"Tup reset to factory settings button\"}"
    reset_to_factory_settings_btn?.setOnClickListener {
      if (!main?.lockWriteBeforeFirstRead!!) {
        System.err.println("tuk reset_to_factory_settings_btn")
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
          main?.runWriteData(byteArrayOf(0x01), RESET_TO_FACTORY_SETTINGS_NEW_VM, WRITE)
        } else {
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
            main?.runWriteData(byteArrayOf(0x01), RESET_TO_FACTORY_SETTINGS_NEW, WRITE)
          } else {
            main?.bleCommandConnector(byteArrayOf(0x01), RESET_TO_FACTORY_SETTINGS, WRITE, 15)
          }
        }


        swap_open_close_tv?.text = 0.toString()
        main?.setSwapOpenCloseButton(false)
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)

        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, false)

        swap_open_close_sw?.isChecked = false
        swap_open_close_tv?.text = 0.toString()
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)

        preferenceManager.putInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM, 80)
        ObjectAnimator.ofInt(shutdown_current_sb, "progress", preferenceManager.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM, 80)).setDuration(200).start()

        single_channel_control_sw?.isChecked = false
        single_channel_control_tv?.text = 0.toString()
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)

        on_off_sensor_gesture_switching_sw?.isChecked = false
        on_off_sensor_gesture_switching_tv?.text = 0.toString()
        sensorGestureSwitching = 0x00
        mode_rl.visibility = View.GONE
        peak_time_rl.visibility = View.GONE
        downtime_rl.visibility = View.GONE
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false)


        mode_tv?.text = "одним\nдатчиком"
        mode = 0x00
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, false)

        YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersReset)
      }
    }


    val eventYandexMetricaParametersCalibrationAdv = "{\"Screen advanced settings\":\"Tup calibration button\"}"
    calibration_adv_btn?.setOnClickListener {
      System.err.println("запись глобальной калибровки тык")
      if (mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1) == 1) {
        main?.runWriteData(byteArrayOf(0x09), CALIBRATION_NEW, WRITE)
      } else {
        main?.runWriteData(byteArrayOf(0x0a), CALIBRATION_NEW, WRITE)
      }
      saveInt(main?.mDeviceAddress + PreferenceKeys.CALIBRATING_STATUS, 1)
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersCalibrationAdv)
    }
    val eventYandexMetricaParametersCalibrationStatusAdv = "{\"Screen advanced settings\":\"Tup calibration status button\"}"
    calibration_status_adv_btn?. setOnClickListener {
      if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
        main?.runReadDataAllCharacteristics(STATUS_CALIBRATION_NEW_VM)
      } else {
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
          main?.runReadDataAllCharacteristics(STATUS_CALIBRATION_NEW)
        }
      }
      YandexMetrica.reportEvent(main?.mDeviceType!!, eventYandexMetricaParametersCalibrationStatusAdv)
    }

    //Скрывает настройки, которые не актуальны для многосхватной бионики
    if ( main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_FEST_A)
      || main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_BT05)
      || main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_MY_IPHONE)
      || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)
      || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
      shutdown_current_rl?.visibility = View.GONE
    }
    //Скрывает настройки, которые не актуальны для бионик кроме FEST-H
    if ( main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)
      || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_X)) {
        telemetry_rl?.visibility = View.VISIBLE
    } else {
      telemetry_rl?.visibility = View.GONE
      calibration_rl?.visibility = View.GONE
      shutdown_current_1_rl?.visibility = View.GONE
      shutdown_current_2_rl?.visibility = View.GONE
      shutdown_current_3_rl?.visibility = View.GONE
      shutdown_current_4_rl?.visibility = View.GONE
      shutdown_current_5_rl?.visibility = View.GONE
      shutdown_current_6_rl?.visibility = View.GONE
    }

    swap_open_close_sw?.isChecked = preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)
    single_channel_control_sw?.isChecked = preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)
    on_off_sensor_gesture_switching_sw?.isChecked = preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false)
    if (preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false)) mode_new_rl?.visibility = View.VISIBLE
    mode_sw?.isChecked = preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, false)
    if (preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)) swap_open_close_tv?.text = 1.toString()
    if (preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)) single_channel_control_tv?.text = 1.toString()
    if (preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false)) {
      on_off_sensor_gesture_switching_tv?.text = 1.toString()
      sensorGestureSwitching = 0x01
    } else {
      sensorGestureSwitching = 0x00
      mode_rl?.visibility = View.GONE
      peak_time_rl?.visibility = View.GONE
      downtime_rl?.visibility = View.GONE
    }
    if (preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, false)) {
      mode_tv?.text = "двумя\nдатчиками"
      mode = 0x01
      downtime_rl?.visibility = View.GONE
    } else {
      mode_tv?.text = "одним\nдатчиком"
      mode = 0x00
    }
  }

  @SuppressLint("Recycle")
  private fun updateAllParameters() {
    if(!changeParameter) {
      main?.runOnUiThread {
//      System.err.println("Принятые данные состояния токов shutdown_current_1_sb: " + mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_1, 80))
        ObjectAnimator.ofInt(shutdown_current_sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM, 80)).setDuration(200).start()
        ObjectAnimator.ofInt(shutdown_current_1_sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_1, 80)).setDuration(200).start()
        ObjectAnimator.ofInt(shutdown_current_2_sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_2, 80)).setDuration(200).start()
        ObjectAnimator.ofInt(shutdown_current_3_sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_3, 80)).setDuration(200).start()
        ObjectAnimator.ofInt(shutdown_current_4_sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_4, 80)).setDuration(200).start()
        ObjectAnimator.ofInt(shutdown_current_5_sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_5, 80)).setDuration(200).start()
        ObjectAnimator.ofInt(shutdown_current_6_sb, "progress", mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM_6, 80)).setDuration(200).start()

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
      if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
        time = (peak_time_sb?.progress?.times(0.04)).toString() + "c"
      }
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
      if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_FEST_H)) {
        time = (downtime_sb?.progress?.times(0.04)).toString() + "c"
      }
      downtime_tv?.text = time

    }
  }

  private fun startUpdatingUIThread() {
    updatingUIThread =  Thread {
      while (threadFlag) {
        main?.runOnUiThread {
          if (main?.setOneChannelNum == 1) {
            preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, true)
          } else {
            preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)
          }
          if (main?.lockChangeTelemetryNumber == true) {
            telemetry_number_et?.setText(main?.telemetryNumber)
            main?.lockChangeTelemetryNumber = false
            System.err.println("telemetry_number_et записали принятые данные")
          }
          //////// блок кода применим только если у нас протез с новым протоколом

          //////
//          initializeUI()
          updateAllParameters()
        }
        try {
          Thread.sleep(1000)
        } catch (ignored: Exception) {  }
      }
    }
    updatingUIThread?.start()
  }

  internal fun saveInt(key: String, variable: Int) {
    val editor: SharedPreferences.Editor = mSettings!!.edit()
    editor.putInt(key, variable)
    editor.apply()
  }
}
