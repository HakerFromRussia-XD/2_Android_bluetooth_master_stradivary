/* Licensed under the Apache License, Version 2.0 (the "License");
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
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.text.Html
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.android.synthetic.main.layout_chart.*
import me.start.motorica.R
import me.start.motorica.new_electronic_by_Rodeon.WDApplication
import me.start.motorica.new_electronic_by_Rodeon.ble.ConstantManager.*
import me.start.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.*
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.start.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceManager
import me.start.motorica.new_electronic_by_Rodeon.persistence.sqlite.SqliteManager
import me.start.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import me.start.motorica.new_electronic_by_Rodeon.utils.NavigationUtils
import javax.inject.Inject

@Suppress("DEPRECATION")
open class ChartFragment : Fragment(), OnChartValueSelectedListener {

  @Inject
  lateinit var sqliteManager: SqliteManager
  @Inject
  lateinit var preferenceManager: PreferenceManager

  private var rootView: View? = null
  private var main: MainActivity? = null
  private var graphThread: Thread? = null
  private var graphThreadFlag = false
  private var testThreadFlag = true
  private var plotData = true
  private var showAdvancedSettings = false
  private var mSettings: SharedPreferences? = null
  private var updatingUIThread: Thread? = null
  private var scale = 0F
  private var oldStateSync = 0


  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val rootView = inflater.inflate(R.layout.layout_chart, container, false)
    WDApplication.component.inject(this)
    this.rootView = rootView
    if (activity != null) { main = activity as MainActivity? }
    return rootView
  }

  @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    if (main?.locate?.contains("ru")!!) {
      opening_sensor_sensitivity_tv?.textSize = 8f
      closing_sensor_sensitivity_tv?.textSize = 8f
      swap_sensors_text_tv?.textSize = 11f
      settings_blocking_tv?.textSize = 11f
      calibration_btn?.textSize = 12f
    }
    initializedSensorGraph()
    initializedUI()
    showAdvancedSettings = NavigationUtils.showAdvancedSettings

    mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
    Handler().postDelayed({
      startUpdatingUIThread()
    }, 500)

    main?.setSwapOpenCloseButton(preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SWAP_OPEN_CLOSE_NUM, false))
    scale = resources.displayMetrics.density

    close_btn.setOnTouchListener { _, event ->
      if (!main?.lockWriteBeforeFirstRead!!) {
        if (!main?.getSwapOpenCloseButton()!!) {
          if (event.action == MotionEvent.ACTION_DOWN) {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
              main?.runWriteData(byteArrayOf(0x01), CLOSE_MOTOR_NEW, WRITE)
              System.err.println("НАЖАТИЕ НА КНОПКУ ЗАКРЫТЬ НОВЫЙ ПРОТОКОЛ")
            } else {
              main?.bleCommandConnector(byteArrayOf(0x01, 0x00), CLOSE_MOTOR_HDLE, WRITE, 7)
              System.err.println("НАЖАТИЕ НА КНОПКУ ЗАКРЫТЬ СТАРЫЙ ПРОТОКОЛ")
            }
          }
          if (event.action == MotionEvent.ACTION_UP) {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
              main?.runWriteData(byteArrayOf(0x00), CLOSE_MOTOR_NEW, WRITE)
            } else {
              main?.bleCommandConnector(byteArrayOf(0x00, 0x00), CLOSE_MOTOR_HDLE, WRITE, 7)

            }
          }
        } else {
          if (event.action == MotionEvent.ACTION_DOWN) {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
              main?.runWriteData(byteArrayOf(0x01), OPEN_MOTOR_NEW, WRITE)
            } else {
              main?.bleCommandConnector(byteArrayOf(0x01, 0x00), OPEN_MOTOR_HDLE, WRITE, 6)
            }
          }
          if (event.action == MotionEvent.ACTION_UP) {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
              main?.runWriteData(byteArrayOf(0x00), OPEN_MOTOR_NEW, WRITE)
            } else {
              main?.bleCommandConnector(byteArrayOf(0x00, 0x00), OPEN_MOTOR_HDLE, WRITE, 6)
            }
          }
        }
      }
      false
    }
    open_btn.setOnTouchListener { _, event ->
      if (!main?.lockWriteBeforeFirstRead!!) {
        if (!main?.getSwapOpenCloseButton()!!) {
          if (event.action == MotionEvent.ACTION_DOWN) {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
              main?.runWriteData(byteArrayOf(0x01), OPEN_MOTOR_NEW, WRITE)
            } else {
              main?.bleCommandConnector(byteArrayOf(0x01, 0x00), OPEN_MOTOR_HDLE, WRITE, 6)
            }
          }
          if (event.action == MotionEvent.ACTION_UP) {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
              main?.runWriteData(byteArrayOf(0x00), OPEN_MOTOR_NEW, WRITE)
            } else {
              main?.bleCommandConnector(byteArrayOf(0x00, 0x00), OPEN_MOTOR_HDLE, WRITE, 6)
            }
          }
        } else {
          if (event.action == MotionEvent.ACTION_DOWN) {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
              main?.runWriteData(byteArrayOf(0x01), CLOSE_MOTOR_NEW, WRITE)
            } else {
              main?.bleCommandConnector(byteArrayOf(0x01, 0x00), CLOSE_MOTOR_HDLE, WRITE, 7)
            }
          }
          if (event.action == MotionEvent.ACTION_UP) {
            if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
              main?.runWriteData(byteArrayOf(0x00), CLOSE_MOTOR_NEW, WRITE)
            } else {
              main?.bleCommandConnector(byteArrayOf(0x00, 0x00), CLOSE_MOTOR_HDLE, WRITE, 7)
            }
          }
        }
      }
      false
    }
    open_CH_sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        System.err.println("CH1 = $progress")
        ObjectAnimator.ofFloat(limit_CH1, "y", 300 * scale - 5f - (progress * scale * 1.04f)).setDuration(200).start()//  10f -> 60f
        ObjectAnimator.ofFloat(open_border, "y", 300 * scale - 5f - (progress * scale * 1.04f)).setDuration(200).start()//  10f -> 60f

        open_threshold_tv.text = progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, false) && (!main?.lockWriteBeforeFirstRead!!)) {//отправка команды изменения порога на протез только если блокировка не активна
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
            main?.runWriteData(byteArrayOf(seekBar.progress.toByte()), OPEN_THRESHOLD_NEW, WRITE)
          } else {
            main?.bleCommandConnector(byteArrayOf(seekBar.progress.toByte()), OPEN_THRESHOLD_HDLE, WRITE, 4)
          }
          if (main?.savingSettingsWhenModified == true) {
            main?.saveInt(main?.mDeviceAddress + PreferenceKeys.OPEN_CH_NUM, seekBar.progress)
          }
        }
      }
    })
    close_CH_sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        System.err.println("CH2 = $progress")
        ObjectAnimator.ofFloat(limit_CH2, "y", 300 * scale - 5f - (progress * scale * 1.04f)).setDuration(200).start()
        ObjectAnimator.ofFloat(close_border, "y", 300 * scale - 5f - (progress * scale * 1.04f)).setDuration(200).start()//  10f -> 60f
        close_threshold_tv.text = progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, false) && (!main?.lockWriteBeforeFirstRead!!)) {//отправка команды изменения порога на протез только если блокировка не активна
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
            main?.runWriteData(byteArrayOf(seekBar.progress.toByte()), CLOSE_THRESHOLD_NEW, WRITE)
          } else {
            main?.bleCommandConnector(byteArrayOf(seekBar.progress.toByte()), CLOSE_THRESHOLD_HDLE, WRITE, 5)
          }
          if (main?.savingSettingsWhenModified == true) {
            main?.saveInt(main?.mDeviceAddress + PreferenceKeys.CLOSE_CH_NUM, seekBar.progress)
          }
        }
      }
    })
    correlator_noise_threshold_1_sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        correlator_noise_threshold_1_tv.text = progress.toString()
      }
      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, false) && (!main?.lockWriteBeforeFirstRead!!)) {//отправка команды изменения порога на протез только если блокировка не активна
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
//            { 16, 6, 1, 0x10, 36, 18, 44, 52, 64, 72, 0x40, 5, 64 }
//            { 16, 6, 1, 0x10, 36, 18, 44, 52, 64, 72, 0x40, 5, 64 }
            main?.runWriteData(byteArrayOf((255 - seekBar.progress).toByte(), 6, 1, 0x10, 36, 18, 44, 52, 64, 72, 0x40, 5, 64, (255 - correlator_noise_threshold_2_sb.progress).toByte(), 6, 1, 0x10, 36, 18, 44, 52, 64, 72, 0x40, 5, 64 ), SENS_OPTIONS_NEW, WRITE)
          } else {
            main?.bleCommandConnector(byteArrayOf(0x01, (255 - seekBar.progress).toByte(), 0x01), SENS_OPTIONS, WRITE,11)
          }
          if (main?.savingSettingsWhenModified == true) {
            main?.saveInt(main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM, (255 - seekBar.progress))
          }
        }
      }
    })
    correlator_noise_threshold_2_sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        correlator_noise_threshold_2_tv.text = seekBar.progress.toString()
      }
      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, false) && (!main?.lockWriteBeforeFirstRead!!)) {//отправка команды изменения порога на протез только если блокировка не активна
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
            main?.runWriteData(byteArrayOf((255 - correlator_noise_threshold_1_sb.progress).toByte(), 6, 1, 0x10, 36, 18, 44, 52, 64, 72, 0x40, 5, 64, (255 - seekBar.progress).toByte(), 6, 1, 0x10, 36, 18, 44, 52, 64, 72, 0x40, 5, 64), SENS_OPTIONS_NEW, WRITE)
          } else {
            main?.bleCommandConnector(byteArrayOf(0x01, (255 - seekBar.progress).toByte(), 0x02), SENS_OPTIONS, WRITE,11)
          }
          if (main?.savingSettingsWhenModified == true) {
            main?.saveInt(main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_2_NUM, (255 - seekBar.progress))
          }
        }
      }
    })
    sync_sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (main?.locate?.contains("ru")!!) {
          sync_tv?.text = "снхро "+seekBar.progress.toString()+"%"
        } else {
          sync_tv?.text = "sync "+seekBar.progress.toString()+"%"
        }

      }
      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {}
    })
    swap_sensors_sw.setOnClickListener {
      if (!preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, false) && (!main?.lockWriteBeforeFirstRead!!)) {
        if (swap_sensors_sw.isChecked) {
          swap_sensors_tv.text = 1.toString()
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
            main?.runWriteData(byteArrayOf(0x01), SET_REVERSE_NEW, WRITE)
          } else {
            main?.bleCommandConnector(byteArrayOf(0x01), SET_REVERSE, WRITE, 14)
          }
          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, true)
          main?.setReverseNum = 1
        } else {
          swap_sensors_tv.text = 0.toString()
          if (main?.mDeviceType!!.contains(DEVICE_TYPE_FEST_H)) {
            main?.runWriteData(byteArrayOf(0x00), SET_REVERSE_NEW, WRITE)
          } else {
            main?.bleCommandConnector(byteArrayOf(0x00), SET_REVERSE, WRITE, 14)
          }
          preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, false)
          main?.setReverseNum = 0
        }
      }
    }
    driver_tv.setOnLongClickListener {
      showAdvancedSettings = if (showAdvancedSettings) {
        graphThreadFlag = false
        Handler().postDelayed({
          main?.showAdvancedSettings(showAdvancedSettings)
        }, 100)
        false
      } else {
        graphThreadFlag = false
        Handler().postDelayed({
          main?.showAdvancedSettings(showAdvancedSettings)
        }, 100)
        true
      }
      false
    }
    thresholds_blocking_sw.setOnClickListener{
      if (thresholds_blocking_sw.isChecked) {
        thresholds_blocking_tv.text = Html.fromHtml(getString(R.string.on))
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, true)
      } else {
        thresholds_blocking_tv.text = resources.getString(R.string.off)
        preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, false)
      }
    }
    calibration_btn?.setOnClickListener {
      System.err.println("запись глобальной калибровки тык")
      if (mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SWAP_LEFT_RIGHT_SIDE, 1) == 1) {
        main?.runWriteData(byteArrayOf(0x09), CALIBRATION_NEW, WRITE)
      } else {
        main?.runWriteData(byteArrayOf(0x0a), CALIBRATION_NEW, WRITE)
      }
      main?.saveInt(main?.mDeviceAddress + PreferenceKeys.CALIBRATING_STATUS, 1)
    }
  }

  @SuppressLint("SetTextI18n")
  private fun initializedUI() {
    thresholds_blocking_sw.isChecked = preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, false)
    if (preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.THRESHOLDS_BLOCKING, false)) thresholds_blocking_tv.text = Html.fromHtml(getString(R.string.on))
    main?.offSensorsUIBeforeConnection()
  }

  override fun onResume() {
    super.onResume()
    System.err.println("ChartFragment onResume")
    graphThreadFlag = true
    testThreadFlag = true
    startGraphEnteringDataThread()
  }
  override fun onPause() {
    super.onPause()
    graphThreadFlag = false
    System.err.println("ChartFragment onPause")
  }
  override fun onDestroy() {
    super.onDestroy()
    testThreadFlag = false
  }

  //////////////////////////////////////////////////////////////////////////////
  /**                          работа с графиками                            **/
  //////////////////////////////////////////////////////////////////////////////
  private fun createSet(): LineDataSet {
    val set = LineDataSet(null, null)
    set.axisDependency = YAxis.AxisDependency.LEFT //.AxisDependency.LEFT
    set.lineWidth = 2f
    set.color = Color.rgb(255, 171, 0)
    set.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
    set.setCircleColor(Color.TRANSPARENT)
    set.circleHoleColor = Color.TRANSPARENT
    set.fillColor = ColorTemplate.getHoloBlue()
    set.highLightColor = Color.rgb(244, 117, 177)
    set.valueTextColor = Color.TRANSPARENT
    return set
  }
  private fun createSet2(): LineDataSet {
    val set2 = LineDataSet(null, null)
    set2.axisDependency = YAxis.AxisDependency.LEFT //.AxisDependency.LEFT
    set2.lineWidth = 2f
    set2.color = Color.WHITE
    set2.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
    set2.setCircleColor(Color.TRANSPARENT)
    set2.circleHoleColor = Color.TRANSPARENT
    set2.fillColor = ColorTemplate.getHoloBlue()
    set2.highLightColor = Color.rgb(244, 117, 177)
    set2.valueTextColor = Color.TRANSPARENT
    return set2
  }
  private fun addEntry(sens1: Int, sens2: Int) {
    val data: LineData = chart_mainchart?.data!!
    var set = data.getDataSetByIndex(0)
    var set2 = data.getDataSetByIndex(1)
    if (set == null) {
      set = createSet()
      set2 = createSet2()
      data.addDataSet(set)
      data.addDataSet(set2)
    }

    data.addEntry(Entry(set.entryCount.toFloat(), sens1.toFloat()), 0)
    data.addEntry(Entry(set2!!.entryCount.toFloat(), sens2.toFloat()), 1)
    data.notifyDataChanged()
    chart_mainchart.notifyDataSetChanged()
    chart_mainchart.setVisibleXRangeMaximum(100f)
    chart_mainchart.moveViewToX(set.entryCount - 100.toFloat()) //data.getEntryCount()
  }
  private fun initializedSensorGraph() {
    chart_mainchart.contentDescription
    chart_mainchart.setTouchEnabled(false)
    chart_mainchart.isDragEnabled = false
    chart_mainchart.isDragDecelerationEnabled = false
    chart_mainchart.setScaleEnabled(false)
    chart_mainchart.setDrawGridBackground(false)
    chart_mainchart.setPinchZoom(false)
    chart_mainchart.setBackgroundColor(Color.TRANSPARENT)
    chart_mainchart.getHighlightByTouchPoint(1f, 1f)
    val data = LineData()
    val data2 = LineData()
    chart_mainchart.data = data
    chart_mainchart.data = data2
    chart_mainchart.legend.isEnabled = false
    chart_mainchart.description.textColor = Color.TRANSPARENT
    chart_mainchart.animateY(2000)

    val x: XAxis = chart_mainchart.xAxis
    x.textColor = Color.TRANSPARENT
    x.setDrawGridLines(false)
    x.axisMaximum = 4000000f
    x.setAvoidFirstLastClipping(true)
    x.position = XAxis.XAxisPosition.BOTTOM

    val y: YAxis = chart_mainchart.axisLeft
    y.textColor = Color.WHITE
    y.mAxisMaximum = 255f
    y.mAxisMinimum = 0f
    y.textSize = 0f
    y.textColor = Color.TRANSPARENT
    y.setDrawGridLines(true)
    y.setDrawAxisLine(false)
    y.setStartAtZero(true)
    y.gridColor = Color.WHITE
    chart_mainchart.axisRight.gridColor = Color.TRANSPARENT
    chart_mainchart.axisRight.axisLineColor = Color.TRANSPARENT
    chart_mainchart.axisRight.textColor = Color.TRANSPARENT
  }

  private fun startGraphEnteringDataThread() {
    graphThread = Thread {
      while (graphThreadFlag) {
        main?.runOnUiThread {
          if (plotData) {
            addEntry(10, 255)
            addEntry(115, 150)
            addEntry(10, 255)
            addEntry(115, 150)
            addEntry(10, 255)
            addEntry(115, 150)
            addEntry(10, 255)
            addEntry(115, 150)
            addEntry(10, 255)
            addEntry(115, 150)
            addEntry(10, 255)
            addEntry(115, 150)
            plotData = false
          }
          addEntry(main!!.getDataSens1(), main!!.getDataSens2())
        }
        try {
          Thread.sleep(GRAPH_UPDATE_DELAY.toLong())
        } catch (ignored: Exception) {
        }
      }
    }
    graphThread?.start()
  }

  override fun onValueSelected(e: Entry?, h: Highlight?) {}
  override fun onNothingSelected() {}


  @SuppressLint("SetTextI18n")
  private fun startUpdatingUIThread() {
    updatingUIThread = Thread {
      while (testThreadFlag) {
        open_CH_sb.progress = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.OPEN_CH_NUM, 30)
        close_CH_sb.progress = mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.CLOSE_CH_NUM, 30)

        if (main?.setReverseNum == 1) { preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, true)
        } else { preferenceManager.putBoolean(main?.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, false) }

        main?.runOnUiThread {
          swap_sensors_sw?.isChecked = preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, false)

          if (preferenceManager.getBoolean(main?.mDeviceAddress + PreferenceKeys.SET_REVERSE_NUM, false)) {
            swap_sensors_tv?.text = 1.toString()
          } else {
            swap_sensors_tv?.text = 0.toString()
          }

//          driver_tv?.text = resources.getString(R.string.driver) +(mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.DRIVER_NUM, 1)).toFloat()/100 + "v"
          bms_tv?.text = resources.getString(R.string.bms) +(mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.BMS_NUM, 1)).toFloat()/100 + "v"
          sensor_tv?.text = resources.getString(R.string.sens) +(mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.SENS_NUM, 1)).toFloat()/100 + "v"
          ObjectAnimator.ofFloat(limit_CH1, "y", 300 * scale - 5f - ((open_CH_sb?.progress?.times(scale)
                  ?: 22.0f) * 1.04f)).setDuration(200).start()
          ObjectAnimator.ofFloat(limit_CH2, "y", 300 * scale - 5f - ((close_CH_sb?.progress?.times(scale)
                  ?: 22.0f) * 1.04f)).setDuration(200).start()
          ObjectAnimator.ofFloat(open_border, "y", 300 * scale - 5f - ((open_CH_sb?.progress?.times(scale)
                  ?: 22.0f) * 1.04f)).setDuration(200).start()
          ObjectAnimator.ofFloat(close_border, "y", 300 * scale - 5f - ((close_CH_sb?.progress?.times(scale)
                  ?: 22.0f) * 1.04f)).setDuration(200).start()
          ObjectAnimator.ofInt(correlator_noise_threshold_1_sb, "progress", 255 - (mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_1_NUM, 16))).setDuration(200).start()
          ObjectAnimator.ofInt(correlator_noise_threshold_2_sb, "progress", 255 - (mSettings!!.getInt(main?.mDeviceAddress + PreferenceKeys.CORRELATOR_NOISE_THRESHOLD_2_NUM, 16))).setDuration(200).start()
          if (oldStateSync != main?.percentSynchronize!!) {
            ObjectAnimator.ofInt(sync_sb, "progress", oldStateSync, main?.percentSynchronize!!).setDuration(1000).start()
            oldStateSync = main?.percentSynchronize!!
          }
        }
        try {
          Thread.sleep(1000)
        } catch (ignored: Exception) { }
      }
    }
    updatingUIThread?.start()
  }
}
