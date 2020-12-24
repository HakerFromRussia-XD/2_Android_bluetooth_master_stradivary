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
import android.graphics.Color
import android.os.Bundle
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
import me.romans.motorica.R
import me.romans.motorica.new_electronic_by_Rodeon.WDApplication
import me.romans.motorica.new_electronic_by_Rodeon.ble.ConstantManager
import me.romans.motorica.new_electronic_by_Rodeon.ble.SampleGattAttributes.*
import me.romans.motorica.new_electronic_by_Rodeon.persistence.sqlite.SqliteManager
import me.romans.motorica.new_electronic_by_Rodeon.ui.activities.main.MainActivity
import me.romans.motorica.new_electronic_by_Rodeon.utils.DateUtils
import kotlinx.android.synthetic.main.layout_chart.*
import javax.inject.Inject

@Suppress("DEPRECATION")
class ChartFragment : Fragment(), OnChartValueSelectedListener {

  @Inject
  lateinit var sqliteManager: SqliteManager

  private var rootView: View? = null
  private var dateCount = 0
  private var main: MainActivity? = null
  private var graphThread: Thread? = null
  private var graphThreadFlag = false
  private var plotData = true
  var objectAnimator: ObjectAnimator? = null
  var objectAnimator2: ObjectAnimator? = null


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

    // set dateCount
//    dateCount = -DateUtils.getDateDay(DateUtils.getFarDay(0), DateUtils.dateFormat)
//    initializeChart(DateUtils.getDateDay("2020-10-16", DateUtils.dateFormat))//2020-10-14  DateUtils.getFarDay(0)

    ////////initialized graph
    initializedSensorGraph()


    val shutdownCurrentTv = rootView!!.findViewById(R.id.shutdown_current_tv) as TextView
    val startUpStepTv = rootView!!.findViewById(R.id.start_up_step_tv) as TextView
    val deadZoneTv = rootView!!.findViewById(R.id.dead_zone_tv) as TextView
    val brakeMotorTv = rootView!!.findViewById(R.id.brake_motor_tv) as TextView
    val scale = resources.displayMetrics.density
    val limitCH1 = rootView!!.findViewById(R.id.limit_CH1) as LinearLayout
    val limitCh2 = rootView!!.findViewById(R.id.limit_CH2) as LinearLayout


    close_btn.setOnTouchListener { _, event ->
      if (event.action == MotionEvent.ACTION_DOWN) {
        main?.bleCommand(byteArrayOf(0x01, 0x00), CLOSE_MOTOR_HDLE, WRITE)
      }
      if (event.action == MotionEvent.ACTION_UP) {
        main?.bleCommand(byteArrayOf(0x00, 0x00), CLOSE_MOTOR_HDLE, WRITE)
      }
      false
    }
    open_btn.setOnTouchListener { _, event ->
      if (event.action == MotionEvent.ACTION_DOWN) {
        main?.bleCommand(byteArrayOf(0x01, 0x00), OPEN_MOTOR_HDLE, WRITE)
      }
      if (event.action == MotionEvent.ACTION_UP) {
        main?.bleCommand(byteArrayOf(0x00, 0x00), OPEN_MOTOR_HDLE, WRITE)
      }
      false
    }
    shutdown_current_sb.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        shutdownCurrentTv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        main?.bleCommand(byteArrayOf(seekBar.progress.toByte()), SHUTDOWN_CURRENT_HDLE, WRITE)
      }
    })
    start_up_step_sb.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        startUpStepTv.text = seekBar.progress.toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        main?.bleCommand(byteArrayOf(seekBar.progress.toByte()), START_UP_STEP_HDLE, WRITE)
      }
    })
    dead_zone_sb.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        deadZoneTv.text = (seekBar.progress + 30).toString()
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        main?.bleCommand(byteArrayOf((seekBar.progress + 30).toByte()), DEAD_ZONE_HDLE, WRITE)
      }
    })
    open_CH_sb.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        System.err.println("CH1 = " + seekBar.progress)
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        main?.bleCommand(byteArrayOf(seekBar.progress.toByte()), OPEN_THRESHOLD_HDLE, WRITE)
        objectAnimator = ObjectAnimator.ofFloat(limitCH1, "y", 300 * scale + 10f - (seekBar.progress * scale * 1.04f))
        objectAnimator?.duration = 200
        objectAnimator?.start()
      }
    })
    close_CH_sb.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        System.err.println("CH2 = " + seekBar.progress)
      }

      override fun onStartTrackingTouch(seekBar: SeekBar) {}
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        main?.bleCommand(byteArrayOf(seekBar.progress.toByte()), CLOSE_THRESHOLD_HDLE, WRITE)
        objectAnimator2 = ObjectAnimator.ofFloat(limitCh2, "y", 300 * scale + 10f - (seekBar.progress * scale * 1.04f))
        objectAnimator2?.duration = 200
        objectAnimator2?.start()
      }
    })
    brake_motor_sb.setOnClickListener {
      if (brake_motor_sb.isChecked) {
        brakeMotorTv.text = 1.toString()
        main?.bleCommand(byteArrayOf(0x01), BRAKE_MOTOR_HDLE, WRITE)
      } else {
        brakeMotorTv.text = 0.toString()
        main?.bleCommand(byteArrayOf(0x00), BRAKE_MOTOR_HDLE, WRITE)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    System.err.println("ChartFragment onResume")
    graphThreadFlag = true
    startGraphEnteringDataThread()
  }
  override fun onPause() {
    super.onPause()
    graphThreadFlag = false
    System.err.println("ChartFragment onPause")
  }

  //////////////////////////////////////////////////////////////////////////////
  /**                          работа с графиками                            **/
  //////////////////////////////////////////////////////////////////////////////
  private fun createSet(): LineDataSet? {
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
  private fun createSet2(): LineDataSet? {
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

    data.addEntry(Entry(set!!.entryCount.toFloat(), sens1.toFloat()), 0)
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
            addEntry(10,255)
            addEntry(115,150)
            addEntry(10,255)
            addEntry(115,150)
            addEntry(10,255)
            addEntry(115,150)
            addEntry(10,255)
            addEntry(115,150)
            addEntry(10,255)
            addEntry(115,150)
            addEntry(10,255)
            addEntry(115,150)
            plotData = false
          }
          addEntry(main?.getDataSens1()!!, main?.getDataSens2()!!)
        }
        try {
          Thread.sleep(ConstantManager.GRAPH_UPDATE_DELAY.toLong())
        } catch (ignored: Exception) {
        }
      }
    }
    graphThread?.start()
  }

  override fun onValueSelected(e: Entry?, h: Highlight?) {}
  override fun onNothingSelected() {}
}
