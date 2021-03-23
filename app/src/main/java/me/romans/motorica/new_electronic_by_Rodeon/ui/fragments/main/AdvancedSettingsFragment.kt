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

package me.romans.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import me.romans.motorica.R
import me.romans.motorica.new_electronic_by_Rodeon.WDApplication
import me.romans.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.*
import me.romans.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceManager
import me.romans.motorica.new_electronic_by_Rodeon.persistence.sqlite.SqliteManager
import me.romans.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import kotlinx.android.synthetic.main.layout_advanced_settings.*
import me.romans.motorica.new_electronic_by_Rodeon.ble.ConstantManager
import me.romans.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import javax.inject.Inject

class AdvancedSettingsFragment : Fragment() {

  @Inject
  lateinit var preferenceManager: PreferenceManager
  @Inject
  lateinit var sqliteManager: SqliteManager

  private var rootView: View? = null
  private var mContext: Context? = null
  private var main: MainActivity? = null
  private var mSettings: SharedPreferences? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val rootView = inflater.inflate(R.layout.layout_advanced_settings, container, false)
    WDApplication.component.inject(this)
    if (activity != null) { main = activity as MainActivity? }
    this.rootView = rootView
    this.mContext = context
    return rootView
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initializeUI()
  }

  @SuppressLint("SetTextI18n", "CheckResult")
  private fun initializeUI() {
    mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)

    shutdown_current_sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        shutdown_current_tv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        main?.bleCommandConnector(byteArrayOf(seekBar.progress.toByte()), SHUTDOWN_CURRENT_HDLE, WRITE, 0)
        main?.incrementCountCommand()
        preferenceManager.putInt(PreferenceKeys.SHUTDOWN_CURRENT_NUM, seekBar.progress)
      }
    })
    start_up_step_sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        start_up_step_tv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        main?.bleCommandConnector(byteArrayOf(seekBar.progress.toByte()), START_UP_STEP_HDLE, WRITE, 1)
        main?.incrementCountCommand()
        preferenceManager.putInt(PreferenceKeys.STAR_UP_STEP_NUM, seekBar.progress)
      }
    })
    dead_zone_sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        dead_zone_tv.text = (seekBar.progress + 30).toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        main?.bleCommandConnector(byteArrayOf((seekBar.progress + 30).toByte()), DEAD_ZONE_HDLE, WRITE, 3)
        main?.incrementCountCommand()
        preferenceManager.putInt(PreferenceKeys.DEAD_ZONE_NUM, (seekBar.progress))
      }
    })
    brake_motor_sw.setOnClickListener {
      if (brake_motor_sw.isChecked) {
        brake_motor_tv.text = 1.toString()
        main?.bleCommandConnector(byteArrayOf(0x01), BRAKE_MOTOR_HDLE, WRITE, 10)
        main?.incrementCountCommand()
        preferenceManager.putBoolean(PreferenceKeys.USE_BRAKE_MOTOR_NUM, true)
      } else {
        brake_motor_tv.text = 0.toString()
        main?.bleCommandConnector(byteArrayOf(0x00), BRAKE_MOTOR_HDLE, WRITE, 10)
        main?.incrementCountCommand()
        preferenceManager.putBoolean(PreferenceKeys.USE_BRAKE_MOTOR_NUM, false)
      }
    }
    swap_sensors_sw.setOnClickListener {
      if (swap_sensors_sw.isChecked) {
        swap_sensors_tv.text = 1.toString()
        main?.bleCommandConnector(byteArrayOf(0x01), SET_REVERSE, WRITE, 14)
        main?.incrementCountCommand()
        preferenceManager.putBoolean(PreferenceKeys.SET_REVERSE_NUM, true)
      } else {
        swap_sensors_tv.text = 0.toString()
        main?.bleCommandConnector(byteArrayOf(0x00), SET_REVERSE, WRITE, 14)
        main?.incrementCountCommand()
        preferenceManager.putBoolean(PreferenceKeys.SET_REVERSE_NUM, false)
      }
    }
    swap_open_close_sw.setOnClickListener {
      if (swap_open_close_sw.isChecked) {
        swap_open_close_tv.text = 1.toString()
        main?.setSwapOpenCloseButton(true)
        preferenceManager.putBoolean(PreferenceKeys.SWAP_OPEN_CLOSE_NUM, true)
      } else {
        swap_open_close_tv.text = 0.toString()
        main?.setSwapOpenCloseButton(false)
        preferenceManager.putBoolean(PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)
      }
    }
    reset_to_factory_settings_btn.setOnClickListener {
      main?.bleCommandConnector(byteArrayOf(0x01), RESET_TO_FACTORY_SETTINGS, WRITE, 15)
      main?.incrementCountCommand()
    }

    //Скрывает настройки, которые не актуальны для многосхватной бионики
    if ( main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE) || main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_2) || main?.mDeviceType!!.contains(ConstantManager.EXTRAS_DEVICE_TYPE_3)) {
      shutdown_current_rl.visibility = View.GONE
      start_up_step_rl.visibility = View.GONE
      dead_zone_rl.visibility = View.GONE
      brake_motor_rl.visibility = View.GONE
    }

//    if (preferenceManager.getBoolean(PreferenceKeys.USE_BRAKE_MOTOR, true) == null) preferenceManager.putBoolean(PreferenceKeys.USE_BRAKE_MOTOR, false)
    brake_motor_sw.isChecked = preferenceManager.getBoolean(PreferenceKeys.USE_BRAKE_MOTOR_NUM, true)
    swap_sensors_sw.isChecked = preferenceManager.getBoolean(PreferenceKeys.SET_REVERSE_NUM, false)
    swap_open_close_sw.isChecked = preferenceManager.getBoolean(PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)
    if (preferenceManager.getBoolean(PreferenceKeys.USE_BRAKE_MOTOR_NUM, true)) brake_motor_tv.text = 1.toString()
    if (preferenceManager.getBoolean(PreferenceKeys.SET_REVERSE_NUM, false)) swap_sensors_tv.text = 1.toString()
    if (preferenceManager.getBoolean(PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false)) swap_open_close_tv.text = 1.toString()

    main?.runOnUiThread {
      ObjectAnimator.ofInt(shutdown_current_sb, "progress", preferenceManager.getInt(PreferenceKeys.SHUTDOWN_CURRENT_NUM, 250)).setDuration(200).start()
      ObjectAnimator.ofInt(start_up_step_sb, "progress", preferenceManager.getInt(PreferenceKeys.STAR_UP_STEP_NUM, 50)).setDuration(200).start()
      ObjectAnimator.ofInt(dead_zone_sb, "progress", preferenceManager.getInt(PreferenceKeys.DEAD_ZONE_NUM, 40)).setDuration(200).start()
    }
  }
}
