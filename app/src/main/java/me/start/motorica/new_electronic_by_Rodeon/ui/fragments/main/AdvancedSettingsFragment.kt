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
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.*
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceManager
import me.start.motorica.new_electronic_by_Rodeon.persistence.sqlite.SqliteManager
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import kotlinx.android.synthetic.main.layout_advanced_settings.*
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
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
  private var updatingUIThread: Thread? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val rootView = inflater.inflate(R.layout.layout_advanced_settings, container, false)
    WDApplication.component.inject(this)
    if (activity != null) { main = activity as MainActivity? }
    this.rootView = rootView
    this.mContext = context
    scale = resources.displayMetrics.density
    return rootView
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initializeUI()
    Handler().postDelayed({
      startUpdatingUIThread()
    }, 500)
  }

  @SuppressLint("SetTextI18n", "CheckResult")
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
      side_text_tv?.textSize = 11f
      left_right_side_swap_tv?.textSize = 11f
    }
    if (mSettings?.getInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1) == 1) {
      left_right_side_swap_sw?.isChecked = true
      left_right_side_swap_tv?.text = Html.fromHtml(getString(R.string.right))
    } else {
      left_right_side_swap_sw?.isChecked = false
      left_right_side_swap_tv?.text = resources.getString(R.string.left)
    }


    shutdown_current_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        shutdown_current_tv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          main?.bleCommandConnector(byteArrayOf(seekBar.progress.toByte()), SHUTDOWN_CURRENT_HDLE, WRITE, 0)
//          main?.incrementCountCommand()
          preferenceManager.putInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM, seekBar.progress)
        }
      }
    })
    swap_open_close_sw?.setOnClickListener {
      if (swap_open_close_sw.isChecked) {
        swap_open_close_tv.text = 1.toString()
        main?.setSwapOpenCloseButton(true)
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, true)
      } else {
        swap_open_close_tv.text = 0.toString()
        main?.setSwapOpenCloseButton(false)
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)
      }
    }
    single_channel_control_sw?.setOnClickListener {
      if (!main?.lockWriteBeforeFirstRead!!) {
        if (single_channel_control_sw.isChecked) {
          single_channel_control_tv?.text = 1.toString()
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_4)) {
            main?.runWriteData(byteArrayOf(0x01), SET_ONE_CHANNEL_NEW, WRITE)
            main?.setOneChannelNum = 1
          } else {
            main?.bleCommandConnector(byteArrayOf(0x01), SET_ONE_CHANNEL, WRITE, 16)
          }

          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, true)
        } else {
          single_channel_control_tv?.text = 0.toString()
          if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_4)) {
            main?.runWriteData(byteArrayOf(0x00), SET_ONE_CHANNEL_NEW, WRITE)
            main?.setOneChannelNum = 0
          } else {
            main?.bleCommandConnector(byteArrayOf(0x00), SET_ONE_CHANNEL, WRITE, 16)
          }
          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)
        }
      }
    }
    on_off_sensor_gesture_switching_sw?.setOnClickListener {
      if (!main?.lockWriteBeforeFirstRead!!) {
        if (on_off_sensor_gesture_switching_sw.isChecked) {
          on_off_sensor_gesture_switching_tv.text = 1.toString()
          sensorGestureSwitching = 0x01
          mode_rl.visibility = View.VISIBLE
          peak_time_rl.visibility = View.VISIBLE
          downtime_rl.visibility = View.VISIBLE
          main?.bleCommandConnector(byteArrayOf(0x00, sensorGestureSwitching, mode, (peak_time_sb.progress+5).toByte(), (downtime_sb.progress+5).toByte()),
                                    SET_CHANGE_GESTURE, WRITE, 17)
//          main?.incrementCountCommand()
          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, true)
        } else {
          on_off_sensor_gesture_switching_tv.text = 0.toString()
          sensorGestureSwitching = 0x00
          mode_rl.visibility = View.GONE
          peak_time_rl.visibility = View.GONE
          downtime_rl.visibility = View.GONE
          main?.bleCommandConnector(byteArrayOf(0x00, sensorGestureSwitching, mode, (peak_time_sb.progress+5).toByte(), (downtime_sb.progress+5).toByte()),
                                    SET_CHANGE_GESTURE, WRITE, 17)
//          main?.incrementCountCommand()
          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false)
        }
      }
    }
    mode_sw?.setOnClickListener {
      if (!main?.lockWriteBeforeFirstRead!!) {
        if (mode_sw.isChecked) {
          mode_tv.text = "двумя\nдатчиками"
          mode = 0x01
          downtime_rl.visibility = View.GONE
          main?.bleCommandConnector(byteArrayOf(0x00, sensorGestureSwitching, mode, (peak_time_sb.progress+5).toByte(), (downtime_sb.progress+5).toByte()),
                                    SET_CHANGE_GESTURE, WRITE, 17)
//          main?.incrementCountCommand()
          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, true)
        } else {
          mode_tv.text = "одним\nдатчиком"
          mode = 0x00
          downtime_rl.visibility = View.VISIBLE
          main?.bleCommandConnector(byteArrayOf(0x00, sensorGestureSwitching, mode, (peak_time_sb.progress+5).toByte(), (downtime_sb.progress+5).toByte()),
                                    SET_CHANGE_GESTURE, WRITE, 17)
//          main?.incrementCountCommand()
          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, false)
        }
      }
    }
    peak_time_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        val time: String = when {
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
        peak_time_tv.text = time
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          main?.bleCommandConnector(byteArrayOf(0x00, sensorGestureSwitching, mode, (peak_time_sb.progress+5).toByte(), (downtime_sb.progress+5).toByte()),
                                    SET_CHANGE_GESTURE, WRITE, 17)
//          main?.incrementCountCommand()
          preferenceManager.putInt(main?.mDeviceAddress + PreferenceKeys.SET_PEAK_TIME_NUM, seekBar.progress)
        }
      }
    })
    downtime_sb?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        val time: String = when {
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
        downtime_tv.text = time
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!main?.lockWriteBeforeFirstRead!!) {
          main?.bleCommandConnector(byteArrayOf(0x00, sensorGestureSwitching, mode, (peak_time_sb.progress+5).toByte(), (downtime_sb.progress+5).toByte()),
                                    SET_CHANGE_GESTURE, WRITE, 17)
          preferenceManager.putInt(main?.mDeviceAddress + PreferenceKeys.SET_DOWNTIME_NUM, seekBar.progress)
        }
      }
    })

    reset_to_factory_settings_btn?.setOnClickListener {
      if (!main?.lockWriteBeforeFirstRead!!) {
        System.err.println("tuk reset_to_factory_settings_btn")
        if (main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_4)) {
          main?.runWriteData(byteArrayOf(0x01), RESET_TO_FACTORY_SETTINGS_NEW, WRITE)
        } else {
          main?.bleCommandConnector(byteArrayOf(0x01), RESET_TO_FACTORY_SETTINGS, WRITE, 15)
        }


        swap_open_close_tv.text = 0.toString()
        main?.setSwapOpenCloseButton(false)
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)

        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, false)

        swap_open_close_sw.isChecked = false
        swap_open_close_tv.text = 0.toString()
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)

        preferenceManager.putInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM, 80)
        ObjectAnimator.ofInt(shutdown_current_sb, "progress", preferenceManager.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM, 80)).setDuration(200).start()

        single_channel_control_sw.isChecked = false
        single_channel_control_tv.text = 0.toString()
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)

        on_off_sensor_gesture_switching_sw.isChecked = false
        on_off_sensor_gesture_switching_tv.text = 0.toString()
        sensorGestureSwitching = 0x00
        mode_rl.visibility = View.GONE
        peak_time_rl.visibility = View.GONE
        downtime_rl.visibility = View.GONE
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false)


        mode_tv.text = "одним\nдатчиком"
        mode = 0x00
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_MODE_NUM, false)
      }
    }

    //Скрывает настройки, которые не актуальны для многосхватной бионики
    if ( main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE) || main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_2) || main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_3) || main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_4)) {
      shutdown_current_rl?.visibility = View.GONE
    }
    //Скрывает настройки, которые не актуальны для бионик кроме FEST-H
    if ( main?.mDeviceType!!.contains(ConstantManager.DEVICE_TYPE_4) ) { telemetry_rl?.visibility = View.VISIBLE }
    else { telemetry_rl?.visibility = View.GONE }



    swap_open_close_sw?.isChecked = preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)
    single_channel_control_sw?.isChecked = preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_ONE_CHANNEL_NUM, false)
    on_off_sensor_gesture_switching_sw?.isChecked = preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_SENSORS_GESTURE_SWITCHES_NUM, false)
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

    main?.runOnUiThread {
      ObjectAnimator.ofInt(shutdown_current_sb, "progress", preferenceManager.getInt(main?.mDeviceAddress + PreferenceKeys.SHUTDOWN_CURRENT_NUM, 80)).setDuration(200).start()
      ObjectAnimator.ofInt(peak_time_sb, "progress", preferenceManager.getInt(main?.mDeviceAddress + PreferenceKeys.SET_PEAK_TIME_NUM, 15)).setDuration(200).start()
      ObjectAnimator.ofInt(downtime_sb, "progress", preferenceManager.getInt(main?.mDeviceAddress + PreferenceKeys.SET_DOWNTIME_NUM, 15)).setDuration(200).start()
    }
    shutdown_current_tv?.text = shutdown_current_sb?.progress.toString()
    var time: String = when {
      ((peak_time_sb?.progress?.plus(5))?.times(0.05)).toString().length == 4 -> {
        ((peak_time_sb?.progress?.plus(5))?.times(0.05)).toString() + "c"
      }
      ((peak_time_sb?.progress?.plus(5))?.times(0.05)).toString().length > 4 -> {
        ((peak_time_sb?.progress?.plus(5))?.times(0.05)).toString().substring(0,4) + "c"
      }
      else -> {
        ((peak_time_sb?.progress?.plus(5))?.times(0.05)).toString() + "0c"
      }
    }
    peak_time_tv?.text = time
    time = when {
      ((downtime_sb?.progress?.plus(5))?.times(0.05)).toString().length == 4 -> {
        ((downtime_sb?.progress?.plus(5))?.times(0.05)).toString() + "c"
      }
      ((downtime_sb?.progress?.plus(5))?.times(0.05)).toString().length > 4 -> {
        ((downtime_sb?.progress?.plus(5))?.times(0.05)).toString().substring(0,4) + "c"
      }
      else -> {
        ((downtime_sb?.progress?.plus(5))?.times(0.05)).toString() + "0c"
      }
    }
    downtime_tv?.text = time

    left_right_side_swap_sw?.setOnClickListener{
      if (left_right_side_swap_sw.isChecked) {
        left_right_side_swap_tv.text = Html.fromHtml(getString(R.string.right))
        main?.saveInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1)
      } else {
        left_right_side_swap_tv.text = resources.getString(R.string.left)
        main?.saveInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 0)
      }
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
          initializeUI()
        }
        try {
          Thread.sleep(1000)
        } catch (ignored: Exception) {  }
      }
    }
    updatingUIThread?.start()
  }
}
