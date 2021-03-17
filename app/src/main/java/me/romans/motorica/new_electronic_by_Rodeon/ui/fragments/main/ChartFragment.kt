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

package me.romans.motorica.new_electronic_by_Rodeon.ui.fragments.main

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
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
import me.romans.motorica.R
import me.romans.motorica.new_electronic_by_Rodeon.WDApplication
import me.romans.motorica.new_electronic_by_Rodeon.ble.ConstantManager.*
import me.romans.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.*
import me.romans.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceKeys
import me.romans.motorica.new_electronic_by_Rodeon.persistence.preference.PreferenceManager
import me.romans.motorica.new_electronic_by_Rodeon.persistence.sqlite.SqliteManager
import me.romans.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
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
  var objectAnimator: ObjectAnimator? = null
  var objectAnimator2: ObjectAnimator? = null
  private var showAdvancedSettings = false
  private var mSettings: SharedPreferences? = null

  private var testThread: Thread? = null


  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val rootView = inflater.inflate(R.layout.layout_chart, container, false)
    WDApplication.component.inject(this)
    this.rootView = rootView
    if (activity != null) { main = activity as MainActivity? }
    return rootView
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    System.err.println("фрагмент   onActivityCreated")
    // set dateCount
//    dateCount = -DateUtils.getDateDay(DateUtils.getFarDay(0), DateUtils.dateFormat)
//    initializeChart(DateUtils.getDateDay("2020-10-16", DateUtils.dateFormat))//2020-10-14  DateUtils.getFarDay(0)


    initializedSensorGraph()
    initializedUI()
    showAdvancedSettings = preferenceManager.getBoolean(PreferenceKeys.ADVANCED_SETTINGS, false)


    mSettings = context?.getSharedPreferences(PreferenceKeys.APP_PREFERENCES, Context.MODE_PRIVATE)
    startTestThread()

//    main?.showAdvancedSettings(showAdvancedSettings)

//    graphThreadFlag = false
//    Handler().postDelayed({
//      main?.showAdvancedSettings(showAdvancedSettings)
//    }, 100)


    val shutdownCurrentTv = rootView!!.findViewById(R.id.shutdown_current_tv) as TextView
    val startUpStepTv = rootView!!.findViewById(R.id.start_up_step_tv) as TextView
    val deadZoneTv = rootView!!.findViewById(R.id.dead_zone_tv) as TextView
    val brakeMotorTv = rootView!!.findViewById(R.id.brake_motor_tv) as TextView
    val scale = resources.displayMetrics.density
    val limitCH1 = rootView!!.findViewById(R.id.limit_CH1) as LinearLayout
    val limitCh2 = rootView!!.findViewById(R.id.limit_CH2) as LinearLayout


    close_btn.setOnTouchListener { _, event ->
      if (event.action == MotionEvent.ACTION_DOWN) {
        main?.bleCommandConnector(byteArrayOf(0x01, 0x00), CLOSE_MOTOR_HDLE, WRITE, 7)
      }
      if (event.action == MotionEvent.ACTION_UP) {
        main?.bleCommandConnector(byteArrayOf(0x00, 0x00), CLOSE_MOTOR_HDLE, WRITE, 7)
      }
      false
    }
    open_btn.setOnTouchListener { _, event ->
      if (event.action == MotionEvent.ACTION_DOWN) {
        main?.bleCommandConnector(byteArrayOf(0x01, 0x00), OPEN_MOTOR_HDLE, WRITE, 6)

      }
      if (event.action == MotionEvent.ACTION_UP) {
        main?.bleCommandConnector(byteArrayOf(0x00, 0x00), OPEN_MOTOR_HDLE, WRITE, 6)
      }
      false
    }
    shutdown_current_sb.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        shutdownCurrentTv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        main?.bleCommandConnector(byteArrayOf(seekBar.progress.toByte()), SHUTDOWN_CURRENT_HDLE, WRITE, 0)
      }
    })
    start_up_step_sb.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        startUpStepTv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        main?.bleCommandConnector(byteArrayOf(seekBar.progress.toByte()), START_UP_STEP_HDLE, WRITE, 1)
      }
    })
    dead_zone_sb.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        deadZoneTv.text = (seekBar.progress + 30).toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        main?.bleCommandConnector(byteArrayOf((seekBar.progress + 30).toByte()), DEAD_ZONE_HDLE, WRITE, 3)
      }
    })
    open_CH_sb.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        System.err.println("CH1 = " + seekBar.progress)
        objectAnimator = ObjectAnimator.ofFloat(limitCH1, "y", 300 * scale + 10f - (seekBar.progress * scale * 1.04f))
        objectAnimator?.duration = 200
        objectAnimator?.start()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!preferenceManager.getBoolean(PreferenceKeys.THRESHOLDS_BLOCKING, false)) {//отправка команды изменения порога на протез только если блокировка не активна
          main?.bleCommandConnector(byteArrayOf(seekBar.progress.toByte()), OPEN_THRESHOLD_HDLE, WRITE, 4)
        }
      }
    })
    close_CH_sb.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        System.err.println("CH2 = " + seekBar.progress)
        objectAnimator2 = ObjectAnimator.ofFloat(limitCh2, "y", 300 * scale + 10f - (seekBar.progress * scale * 1.04f))
        objectAnimator2?.duration = 200
        objectAnimator2?.start()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (!preferenceManager.getBoolean(PreferenceKeys.THRESHOLDS_BLOCKING, false)) {//отправка команды изменения порога на протез только если блокировка не активна
          main?.bleCommandConnector(byteArrayOf(seekBar.progress.toByte()), CLOSE_THRESHOLD_HDLE, WRITE, 5)
        }
      }
    })
    brake_motor_sw.setOnClickListener {
      if (brake_motor_sw.isChecked) {
        brakeMotorTv.text = 1.toString()
        main?.bleCommandConnector(byteArrayOf(0x01), BRAKE_MOTOR_HDLE, WRITE, 10)
      } else {
        brakeMotorTv.text = 0.toString()
        main?.bleCommandConnector(byteArrayOf(0x00), BRAKE_MOTOR_HDLE, WRITE, 10)
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
      preferenceManager.putBoolean(PreferenceKeys.ADVANCED_SETTINGS, showAdvancedSettings)
      false
    }



    thresholds_blocking_sw.setOnClickListener{
      if (thresholds_blocking_sw.isChecked) {
        thresholds_blocking_tv.text = "on"
        preferenceManager.putBoolean(PreferenceKeys.THRESHOLDS_BLOCKING, true)
      } else {
        thresholds_blocking_tv.text = "off"
        preferenceManager.putBoolean(PreferenceKeys.THRESHOLDS_BLOCKING, false)
      }
    }

    //Скрывает настройки, которые не актуальны для многосхватной бионики
    if ( main?.mDeviceType!!.contains(EXTRAS_DEVICE_TYPE) || main?.mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_2) || main?.mDeviceType!!.contains(EXTRAS_DEVICE_TYPE_3)) {
      shutdown_current_rl.visibility = View.GONE
      start_up_step_rl.visibility = View.GONE
      dead_zone_rl.visibility = View.GONE
      brake_motor_rl.visibility = View.GONE
    }
  }

  private fun initializedUI() {
    thresholds_blocking_sw.isChecked = preferenceManager.getBoolean(PreferenceKeys.THRESHOLDS_BLOCKING, false)
    if (preferenceManager.getBoolean(PreferenceKeys.THRESHOLDS_BLOCKING, false)) thresholds_blocking_tv.text = "on"
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
    chart_mainchart.setVisibleXRangeMaximum(50f)
    chart_mainchart.moveViewToX(set.entryCount - 50.toFloat()) //data.getEntryCount()
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
    chart_mainchart.animateY(700)

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
    y.textSize = 12f
    y.setDrawGridLines(true)
    y.setDrawAxisLine(false)
    y.setStartAtZero(true)
    y.gridColor = Color.WHITE
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

  fun testChange (test: Int) {
    open_CH_sb.progress = test
  }

  private fun startTestThread() {
    testThread = Thread {
      while (testThreadFlag) {
        open_CH_sb.progress = mSettings!!.getInt(PreferenceKeys.ADVANCED_SETTINGS, 0)
        System.err.println("фрагмент startTestThread " + mSettings!!.getInt(PreferenceKeys.ADVANCED_SETTINGS, 0))
        try {
          Thread.sleep(1000)
        } catch (ignored: Exception) { }
      }
    }
    testThread?.start()
  }
}
