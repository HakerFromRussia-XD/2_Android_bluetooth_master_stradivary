package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.graphics.Color
import android.util.Log
import com.bailout.stickk.databinding.Ubi4WidgetPlotBinding
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.models.PlotItem
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
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
import com.github.mikephil.charting.utils.ColorTemplate
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlotDelegateAdapter (
    val plotIsReadyToData:(num: Int) -> Unit) :
    ViewBindingDelegateAdapter<PlotItem, Ubi4WidgetPlotBinding>(Ubi4WidgetPlotBinding::inflate) {
    private var count: Int = 0
    private var dataSens1 = 0
    private var dataSens2 = 0
    private var dataSens3 = 0
    private var dataSens4 = 0
    private var dataSens5 = 0
    private var dataSens6 = 0
    private var numberOfCharts = 2

    private var firstInit = true

    override fun Ubi4WidgetPlotBinding.onBind(plotItem: PlotItem) {
        System.err.println("PlotDelegateAdapter  isEmpty = ${EMGChartLc.isEmpty}")
        System.err.println("PlotDelegateAdapter ${plotItem.title}    data = ${EMGChartLc.data}")

        var parameterID = 0
        var deviceAddress = 0
        when (plotItem.widget) {
            is PlotParameterWidgetEStruct -> {
                parameterID = plotItem.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.elementAt(0).first
                deviceAddress = plotItem.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId
            }
            is PlotParameterWidgetSStruct -> {
                parameterID = plotItem.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.elementAt(0).first
                deviceAddress = plotItem.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId
            }
        }


//        Log.d("PlotDelegateAdapter", "deviceAddress = $deviceAddress")
        // а лучше чтоб функция выдавала параметр по адресу девайса и айди параметра
        numberOfCharts = ParameterProvider.getParameter(deviceAddress, parameterID).parametrSize / PreferenceKeysUBI4.ParameterTypeEnum.entries[ParameterProvider.getParameter(deviceAddress, parameterID).type].sizeOf
        Log.d("PlotDelegateAdapter", "numberOfCharts = $numberOfCharts parametrSize = ${ParameterProvider.getParameter(deviceAddress, parameterID).parametrSize}   type = ${ParameterProvider.getParameter(deviceAddress, parameterID).type}")

        plotArrayFlowCollect()

        initializedSensorGraph(EMGChartLc)
        graphThreadFlag = true
        countBinding += 1
        GlobalScope.launch(CoroutineName("startGraphEnteringDataCoroutine $countBinding")) {
            startGraphEnteringDataCoroutine(EMGChartLc)
        }

        main.bleCommand(BLECommands.requestTransferFlow(1), MAIN_CHANNEL, WRITE)
//        System.err.println("plotIsReadyToData")
        plotIsReadyToData(0)
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
        set.mode = LineDataSet.Mode.LINEAR
        set.setCircleColor(Color.TRANSPARENT)
        set.circleHoleColor = Color.TRANSPARENT
        set.fillColor = ColorTemplate.getHoloBlue()
        set.highLightColor = Color.rgb(244, 117, 177)
        set.valueTextColor = Color.TRANSPARENT
        return set
    }
    private fun createSet1(): LineDataSet {
        val set1 = LineDataSet(null, null)
        set1.axisDependency = YAxis.AxisDependency.LEFT
        set1.lineWidth = 2f
        set1.color = Color.WHITE
        set1.mode = LineDataSet.Mode.LINEAR
        set1.setCircleColor(Color.TRANSPARENT)
        set1.circleHoleColor = Color.TRANSPARENT
        set1.fillColor = ColorTemplate.getHoloBlue()
        set1.highLightColor = Color.rgb(244, 117, 177)
        set1.valueTextColor = Color.TRANSPARENT
        return set1
    }
    private fun createSet2(): LineDataSet {
        val set2 = LineDataSet(null, null)
        set2.axisDependency = YAxis.AxisDependency.LEFT
        set2.lineWidth = 2f
        set2.color = Color.GRAY
        set2.mode = LineDataSet.Mode.LINEAR
        set2.setCircleColor(Color.TRANSPARENT)
        set2.circleHoleColor = Color.TRANSPARENT
        set2.fillColor = ColorTemplate.getHoloBlue()
        set2.highLightColor = Color.rgb(244, 117, 177)
        set2.valueTextColor = Color.TRANSPARENT
        return set2
    }
    private fun createSet3(): LineDataSet {
        val set3 = LineDataSet(null, null)
        set3.axisDependency = YAxis.AxisDependency.LEFT
        set3.lineWidth = 2f
        set3.color = Color.rgb(255, 171, 0)
        set3.mode = LineDataSet.Mode.LINEAR
        set3.setCircleColor(Color.TRANSPARENT)
        set3.circleHoleColor = Color.TRANSPARENT
        set3.fillColor = ColorTemplate.getHoloBlue()
        set3.highLightColor = Color.rgb(244, 117, 177)
        set3.valueTextColor = Color.TRANSPARENT

        return set3
    }
    private fun createSet4(): LineDataSet {
        val set4 = LineDataSet(null, null)
        set4.axisDependency = YAxis.AxisDependency.LEFT
        set4.lineWidth = 2f
        set4.color = Color.GREEN
        set4.mode = LineDataSet.Mode.LINEAR
        set4.setCircleColor(Color.TRANSPARENT)
        set4.circleHoleColor = Color.TRANSPARENT
        set4.fillColor = ColorTemplate.getHoloBlue()
        set4.highLightColor = Color.rgb(244, 117, 177)
        set4.valueTextColor = Color.TRANSPARENT
        return set4
    }
    private fun createSet5(): LineDataSet {
        val set5 = LineDataSet(null, null)
        set5.axisDependency = YAxis.AxisDependency.LEFT
        set5.lineWidth = 2f
        set5.color = Color.BLUE
        set5.mode = LineDataSet.Mode.LINEAR
        set5.setCircleColor(Color.TRANSPARENT)
        set5.circleHoleColor = Color.TRANSPARENT
        set5.fillColor = ColorTemplate.getHoloBlue()
        set5.highLightColor = Color.rgb(244, 117, 177)
        set5.valueTextColor = Color.TRANSPARENT
        return set5
    }
    private fun createSet6(): LineDataSet {
        val set6 = LineDataSet(null, null)
        set6.axisDependency = YAxis.AxisDependency.LEFT
        set6.lineWidth = 2f
        set6.color = Color.YELLOW
        set6.mode = LineDataSet.Mode.LINEAR
        set6.setCircleColor(Color.TRANSPARENT)
        set6.circleHoleColor = Color.TRANSPARENT
        set6.fillColor = ColorTemplate.getHoloBlue()
        set6.highLightColor = Color.rgb(244, 117, 177)
        set6.valueTextColor = Color.TRANSPARENT
        return set6
    }

    private fun addEntry(sens1: Int, sens2: Int, sens3: Int, sens4: Int, sens5: Int, sens6: Int, emgChart: LineChart) {
        val data: LineData =  emgChart.data //binding.chartMainchart.data!!
        var set = data.getDataSetByIndex(0)
        var set1 = data.getDataSetByIndex(1)
        var set2 = data.getDataSetByIndex(2)
        var set3 = data.getDataSetByIndex(3)
        var set4 = data.getDataSetByIndex(4)
        var set5 = data.getDataSetByIndex(5)
        var set6 = data.getDataSetByIndex(6)

        if (set1 == null) {
            set = createSet()
            set1 = createSet1()
            set2 = createSet2()
            set3 = createSet3()
            set4 = createSet4()
            set5 = createSet5()
            set6 = createSet6()

            data.addDataSet(set)
            data.addDataSet(set1)
            data.addDataSet(set2)
            data.addDataSet(set3)
            data.addDataSet(set4)
            data.addDataSet(set5)
            data.addDataSet(set6)
        }



        if (set1.entryCount > 200) {
            main.runOnUiThread {
                set.removeFirst()
                set1.removeFirst()
                if (numberOfCharts >= 2) { set2.removeFirst() }
                if (numberOfCharts >= 3) { set3.removeFirst() }
                if (numberOfCharts >= 4) { set4.removeFirst() }
                if (numberOfCharts >= 5) { set5.removeFirst() }
                if (numberOfCharts >= 6) { set6.removeFirst() }
            }
        }

        main.runOnUiThread {
            data.addEntry(Entry(count.toFloat(), 250.toFloat()), 0)
            data.addEntry(Entry(count.toFloat(), sens1.toFloat()), 1)
            if (numberOfCharts >= 2) {data.addEntry(Entry(count.toFloat(), sens2.toFloat()), 2)}
            if (numberOfCharts >= 3) {data.addEntry(Entry(count.toFloat(), sens3.toFloat()), 3)}
            if (numberOfCharts >= 4) {data.addEntry(Entry(count.toFloat(), sens4.toFloat()), 4)}
            if (numberOfCharts >= 5) {data.addEntry(Entry(count.toFloat(), sens5.toFloat()), 5)}
            if (numberOfCharts >= 5) {data.addEntry(Entry(count.toFloat(), sens6.toFloat()), 6)}

            data.notifyDataChanged()
            emgChart.notifyDataSetChanged()
            if (firstInit) {
                emgChart.setVisibleXRangeMaximum(200f)
                firstInit = false
            }
        }
        emgChart.moveViewToX(set1.entryCount - 200.toFloat())
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
    private fun plotArrayFlowCollect() {
        GlobalScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.IO) {
                MainActivityUBI4.plotArrayFlow.collect { value ->
                    if (value.size != 0) {
                        System.err.println("FLOW TEST plotArrayFlow ${value.size}")
                        if (value.size >= 1) { dataSens1 = value[0] }
                        if (value.size >= 2) { dataSens2 = value[1] }
                        if (value.size >= 3) { dataSens3 = value[2] }
                        if (value.size >= 4) { dataSens4 = value[3] }
                        if (value.size >= 5) { dataSens5 = value[4] }
                        if (value.size >= 6) { dataSens6 = value[5] }
                    }
                }
            }
        }
    }


    private suspend fun startGraphEnteringDataCoroutine(emgChart: LineChart)  {
//        dataSens1 += 1
//        dataSens2 += 1
//        dataSens3 += 1
//        dataSens4 += 2
//        dataSens5 += 2
//        dataSens6 += 2
        if (dataSens1 > 255) { dataSens1 = 0 }
        if (dataSens2 > 255) { dataSens2 = 0 }
        if (dataSens3 > 255) { dataSens3 = 0 }
        if (dataSens4 > 255) { dataSens4 = 0 }
        if (dataSens5 > 255) { dataSens5 = 0 }
        if (dataSens6 > 255) { dataSens6 = 0 }

        addEntry(dataSens1, dataSens2, dataSens3, dataSens4, dataSens5, dataSens6, emgChart)
        delay(ConstantManager.GRAPH_UPDATE_DELAY.toLong())
        if (graphThreadFlag) {
            startGraphEnteringDataCoroutine(emgChart)
        }
    }
}