package com.bailout.stickk.ubi4.ui.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bailout.stickk.R
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.ubi4.contract.OnChatClickListener
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.coroutineContext

class HomeAdapter(private val typeCellsList: ArrayList<String>,
                  private val initList: ArrayList<Int>,
                  private val onChatClickListener: OnChatClickListener,
                  private val main: MainActivityUBI4
) : RecyclerView.Adapter<HomeAdapter.ChatViewHolder>() {
    private var count: Int = 0
    private var graphThreadFlag = true
    private var dataSens1 = 0
    private var dataSens2 = 0

    private var bindingCount = 1

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val emgChart: LineChart = view.findViewById(R.id.EMG_chart_lc) as LineChart
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.widget_plot, parent, false)
        test()
        System.err.println("HomeAdapter onCreateViewHolder")
        return ChatViewHolder(itemView)
    }
    fun test() {
        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.Default) {
                MainActivityUBI4.testSignal.collectLatest { value ->
                    dataSens1 = value
                    dataSens2 = 255 - value
                }
            }
        }
    }


    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        System.err.println("HomeAdapter onBindViewHolder")
        println("onBindViewHolder selectedProfile = " + MainActivityUBI4.connectedDeviceName)
        val typeCell = typeCellsList[position]
        initializedSensorGraph(holder.emgChart)
//        GlobalScope.launch(CoroutineName("onBindViewHolder $bindingCount")) {
//            startGraphEnteringDataThreadNew(holder.emgChart)
//        }
        bindingCount += 1
    }
    override fun getItemCount(): Int {
        return typeCellsList.size
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
    private fun createSet3(): LineDataSet {
        val set3 = LineDataSet(null, null)
        set3.axisDependency = YAxis.AxisDependency.LEFT //.AxisDependency.LEFT
        set3.lineWidth = 2f
        set3.color = Color.TRANSPARENT
        set3.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set3.setCircleColor(Color.TRANSPARENT)
        set3.circleHoleColor = Color.TRANSPARENT
        set3.fillColor = ColorTemplate.getHoloBlue()
        set3.highLightColor = Color.rgb(244, 117, 177)
        set3.valueTextColor = Color.TRANSPARENT
        return set3
    }
//    private fun createSet4(): LineDataSet {
//        val set4 = LineDataSet(null, null)
//        set4.axisDependency = YAxis.AxisDependency.LEFT //.AxisDependency.LEFT
//        set4.lineWidth = 2f
////        set4.setDrawFilled(true)
////        set4.fillDrawable = ContextCompat.getDrawable(main.baseContext,R.drawable.fade_green)
//        set4.color = Color.rgb(198, 241, 88)
//        set4.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
//        set4.setCircleColor(Color.TRANSPARENT)
//        set4.circleHoleColor = Color.TRANSPARENT
//        set4.fillColor = ColorTemplate.getHoloBlue()
////        set4.highLightColor = Color.rgb(244, 117, 177)
//        set4.valueTextColor = Color.TRANSPARENT
//        return set4
//    }
//    private fun createSet5(): LineDataSet {
//        val set5 = LineDataSet(null, null)
//        set5.axisDependency = YAxis.AxisDependency.LEFT //.AxisDependency.LEFT
//        set5.lineWidth = 2f
//        set5.color = Color.rgb(90, 200, 250)
//        set5.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
//        set5.setCircleColor(Color.TRANSPARENT)
//        set5.circleHoleColor = Color.TRANSPARENT
//        set5.fillColor = ColorTemplate.getHoloBlue()
//        set5.highLightColor = Color.rgb(244, 117, 177)
//        set5.valueTextColor = Color.TRANSPARENT
//        return set5
//    }
    private fun addEntry(sens1: Int, sens2: Int, emgChart: LineChart) {
        val data: LineData =  emgChart.data //binding.chartMainchart.data!!
        var set = data.getDataSetByIndex(0)
        var set2 = data.getDataSetByIndex(1)
        val set3: ILineDataSet
//        var set4 = data.getDataSetByIndex(3)
//        var set5 = data.getDataSetByIndex(4)
        if (set == null) {
            set = createSet()
            set2 = createSet2()
            set3 = createSet3()
//            set4 = createSet4()
//            set5 = createSet5()
            data.addDataSet(set)
            data.addDataSet(set2)
            data.addDataSet(set3)
//            data.addDataSet(set4)
//            data.addDataSet(set5)
        }
        System.err.println("addEntry entryCount 1 = ${set.entryCount} entryCount 2 = ${set2.entryCount}")


        if (set.entryCount >= 200 ) {
            main.runOnUiThread {
                set.removeFirst()
                set2.removeFirst()
//                set4.removeFirst()
//                set5.removeFirst()
                set.addEntryOrdered(Entry(1f, 255f))
            }
        } else {
            main.runOnUiThread {
                data.addEntry(Entry(count.toFloat(), 255f), 2)
            }
        }


        main.runOnUiThread {
            data.addEntry(Entry(count.toFloat(), sens1.toFloat()), 0)
            data.addEntry(Entry(count.toFloat(), sens2.toFloat()), 1)
//            data.addEntry(Entry(count.toFloat(), sens1.toFloat()/2), 3)
//            data.addEntry(Entry(count.toFloat(), sens2.toFloat()/2), 4)
            data.notifyDataChanged()

            emgChart.notifyDataSetChanged()
            emgChart.setVisibleXRangeMaximum(200f)
            emgChart.moveViewToX(set.entryCount - 200.toFloat())
        }
        count += 1
    }
    private fun initializedSensorGraph(emgChart: LineChart) {
        emgChart.contentDescription
        emgChart.setTouchEnabled(false)
        emgChart.isDragEnabled = false
        emgChart.isDragDecelerationEnabled = false
        emgChart.setScaleEnabled(false)
        emgChart.setDrawGridBackground(false)
        emgChart.setPinchZoom(false)
        emgChart.setBackgroundColor(Color.TRANSPARENT)
        emgChart.getHighlightByTouchPoint(1f, 1f)
        val data = LineData()
        val data2 = LineData()
        emgChart.data = data
        emgChart.data = data2
        emgChart.legend.isEnabled = false
        emgChart.description.textColor = Color.TRANSPARENT
        emgChart.animateY(2000)

        val x: XAxis = emgChart.xAxis
        x.textColor = Color.TRANSPARENT
        x.setDrawGridLines(false)
        x.axisMaximum = 4000000f
        x.setAvoidFirstLastClipping(true)
        x.position = XAxis.XAxisPosition.BOTTOM

        val y: YAxis = emgChart.axisLeft
        y.textColor = Color.WHITE
        y.mAxisMaximum = 255f
        y.mAxisMinimum = 255f
        y.textSize = 0f
        y.textColor = Color.TRANSPARENT
        y.setDrawGridLines(true)
        y.setDrawAxisLine(false)
        y.setStartAtZero(true)
        y.gridColor = Color.WHITE
        emgChart.axisRight.gridColor = Color.TRANSPARENT
        emgChart.axisRight.axisLineColor = Color.TRANSPARENT
        emgChart.axisRight.textColor = Color.TRANSPARENT
    }

    private suspend fun startGraphEnteringDataThreadNew(emgChart: LineChart)  {
        addEntry(dataSens1, dataSens2, emgChart)
        delay(ConstantManager.GRAPH_UPDATE_DELAY.toLong())
        System.err.println("I'm working in coroutine ${coroutineContext[CoroutineName.Key]} addEntry")
        if (graphThreadFlag) {startGraphEnteringDataThreadNew(emgChart)}
    }
}