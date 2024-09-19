package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.graphics.Color
import com.bailout.stickk.databinding.WidgetPlotBinding
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.ubi4.adapters.models.PlotItem
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.countBinding
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.graphThreadFlag
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class OnePlotDelegateAdapter :
    ViewBindingDelegateAdapter<PlotItem, WidgetPlotBinding>(WidgetPlotBinding::inflate) {
    private var count: Int = 0
    private var dataSens1 = 0
    private var dataSens2 = 0
    private var dataSens3 = 0
    private var dataSens4 = 0
    private var dataSens5 = 0
    private var dataSens6 = 0

    override fun WidgetPlotBinding.onBind(plotItem: PlotItem) {
        System.err.println("OnePlotDelegateAdapter  isEmpty = ${EMGChartLc.isEmpty}")
        System.err.println("OnePlotDelegateAdapter ${plotItem.title}    data = ${EMGChartLc.data}")

        plotArrayFlowCollect()

        initializedSensorGraph(EMGChartLc)
        graphThreadFlag = true
        countBinding += 1
        GlobalScope.launch(CoroutineName("startGraphEnteringDataCoroutine $countBinding")) {
            startGraphEnteringDataCoroutine(EMGChartLc)
        }
    }
    override fun isForViewType(item: Any): Boolean = item is PlotItem
    override fun PlotItem.getItemId(): Any = title

    //////////////////////////////////////////////////////////////////////////////
    /**                          работа с графиками                            **/
    //////////////////////////////////////////////////////////////////////////////
    private fun createSet(): LineDataSet {
        val set = LineDataSet(null, null)
        set.axisDependency = YAxis.AxisDependency.LEFT //.AxisDependency.LEFT
        set.lineWidth = 0.1f
        set.color = Color.WHITE
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
        set3.color = Color.rgb(255, 171, 0)
        set3.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set3.setCircleColor(Color.TRANSPARENT)
        set3.circleHoleColor = Color.TRANSPARENT
        set3.fillColor = ColorTemplate.getHoloBlue()
        set3.highLightColor = Color.rgb(244, 117, 177)
        set3.valueTextColor = Color.TRANSPARENT

        return set3
    }
    private fun createSet4(): LineDataSet {
        val set4 = LineDataSet(null, null)
        set4.axisDependency = YAxis.AxisDependency.LEFT //.AxisDependency.LEFT
        set4.lineWidth = 2f
        set4.color = Color.GREEN
        set4.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set4.setCircleColor(Color.TRANSPARENT)
        set4.circleHoleColor = Color.TRANSPARENT
        set4.fillColor = ColorTemplate.getHoloBlue()
        set4.highLightColor = Color.rgb(244, 117, 177)
        set4.valueTextColor = Color.TRANSPARENT
        return set4
    }
    private fun createSet5(): LineDataSet {
        val set5 = LineDataSet(null, null)
        set5.axisDependency = YAxis.AxisDependency.LEFT //.AxisDependency.LEFT
        set5.lineWidth = 2f
        set5.color = Color.BLUE
        set5.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set5.setCircleColor(Color.TRANSPARENT)
        set5.circleHoleColor = Color.TRANSPARENT
        set5.fillColor = ColorTemplate.getHoloBlue()
        set5.highLightColor = Color.rgb(244, 117, 177)
        set5.valueTextColor = Color.TRANSPARENT
        return set5
    }
    private fun createSet6(): LineDataSet {
        val set6 = LineDataSet(null, null)
        set6.axisDependency = YAxis.AxisDependency.LEFT //.AxisDependency.LEFT
        set6.lineWidth = 2f
        set6.color = Color.YELLOW
        set6.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set6.setCircleColor(Color.TRANSPARENT)
        set6.circleHoleColor = Color.TRANSPARENT
        set6.fillColor = ColorTemplate.getHoloBlue()
        set6.highLightColor = Color.rgb(244, 117, 177)
        set6.valueTextColor = Color.TRANSPARENT
        return set6
    }
    private fun createSet7(): LineDataSet {
        val set7 = LineDataSet(null, null)
        set7.axisDependency = YAxis.AxisDependency.LEFT //.AxisDependency.LEFT
        set7.lineWidth = 2f
        set7.color = Color.RED
        set7.mode = LineDataSet.Mode.HORIZONTAL_BEZIER
        set7.setCircleColor(Color.TRANSPARENT)
        set7.circleHoleColor = Color.TRANSPARENT
        set7.fillColor = ColorTemplate.getHoloBlue()
        set7.highLightColor = Color.rgb(244, 117, 177)
        set7.valueTextColor = Color.TRANSPARENT
        return set7
    }
    private fun addEntry(sens1: Int, sens2: Int, sens3: Int, sens4: Int, sens5: Int, sens6: Int, emgChart: LineChart) {
        val data: LineData =  emgChart.data //binding.chartMainchart.data!!
        var set = data.getDataSetByIndex(0)
        var set2 = data.getDataSetByIndex(1)
//        var set3 = data.getDataSetByIndex(2)
//        var set4 = data.getDataSetByIndex(3)
//        var set5 = data.getDataSetByIndex(4)
//        var set6 = data.getDataSetByIndex(5)
//        var set7 = data.getDataSetByIndex(6)

        if (set2 == null) {
            set = createSet()
            set2 = createSet2()
//            set3 = createSet3()
//            set4 = createSet4()
//            set5 = createSet5()
//            set6 = createSet6()
//            set7 = createSet7()

            data.addDataSet(set)
            data.addDataSet(set2)
//            data.addDataSet(set3)
//            data.addDataSet(set4)
//            data.addDataSet(set5)
//            data.addDataSet(set6)
//            data.addDataSet(set7)
        }


        System.err.println("addEntry set2.entryCount: ${set2.entryCount}    set.entryCount: ${set.entryCount}    count: $count")
        main.runOnUiThread {
            if (set2.entryCount > 200) {
                set.removeFirst()
                set2.removeFirst()
//                set3.removeFirst()
//                set4.removeFirst()
//                set5.removeFirst()
//                set6.removeFirst()
//                set7.removeFirst()
//                set2.addEntryOrdered(Entry(1f, 255f))

            } else {
//                data.addEntry(Entry(count.toFloat(), 255f), 0)
            }
        }

        main.runOnUiThread {
            data.addEntry(Entry(count.toFloat(), 250.toFloat()), 0)
            data.addEntry(Entry(count.toFloat(), sens1.toFloat()), 1)
//            data.addEntry(Entry(count.toFloat(), sens2.toFloat()), 2)
//            data.addEntry(Entry(count.toFloat(), sens3.toFloat()), 3)
//            data.addEntry(Entry(count.toFloat(), sens4.toFloat()), 4)
//            data.addEntry(Entry(count.toFloat(), sens5.toFloat()), 5)
//            data.addEntry(Entry(count.toFloat(), sens6.toFloat()), 6)
            data.notifyDataChanged()

            emgChart.notifyDataSetChanged()
            emgChart.setVisibleXRangeMaximum(200f)
            emgChart.moveViewToX(set2.entryCount - 200.toFloat())
        }
        count += 1
        System.err.println("count = $count")
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
    private fun plotArrayFlowCollect() {
        GlobalScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.IO) {
                MainActivityUBI4.plotArrayFlow.collect { value ->
//                    System.err.println("FLOW TEST plotArrayFlow ${value.size}")
                    if (value.size != 0) {
                        System.err.println("FLOW TEST plotArrayFlow ${value[0]}")
                        dataSens1 = value[0]
                        dataSens2 = value[1]
                        dataSens3 = value[2]
                        dataSens4 = value[3]
                        dataSens5 = value[4]
                        dataSens6 = value[5]
                    }
                }
            }
        }
    }


    private suspend fun startGraphEnteringDataCoroutine(emgChart: LineChart)  {
        System.err.println("startGraphEnteringDataCoroutine ${coroutineContext[CoroutineName.Key]}")
        dataSens1 += 1
        if (dataSens1 > 250) { dataSens1 = 0 }
        addEntry(dataSens1, dataSens2, dataSens3, dataSens4, dataSens5, dataSens6, emgChart)
        delay(ConstantManager.GRAPH_UPDATE_DELAY.toLong())
        if (graphThreadFlag) {
            startGraphEnteringDataCoroutine(emgChart)
        }
    }
}