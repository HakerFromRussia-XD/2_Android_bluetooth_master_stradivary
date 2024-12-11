package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.graphics.Color
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.marginTop
import com.bailout.stickk.R
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
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlotDelegateAdapter (
    val plotIsReadyToData:(numberOfCharts: Int) -> Unit,
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) :
    ViewBindingDelegateAdapter<PlotItem, Ubi4WidgetPlotBinding>(Ubi4WidgetPlotBinding::inflate) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var count: Int = 0
    private var dataSens1 = 0
    private var dataSens2 = 0
    private var dataSens3 = 0
    private var dataSens4 = 0
    private var dataSens5 = 0
    private var dataSens6 = 0
    private var numberOfCharts = 2

    private var raw_data_set1: List<Entry> = ArrayList()
    private var g_data_sets: MutableList<LineDataSet> = ArrayList()
    private var g_data_set: LineDataSet = LineDataSet(null,null)
    private var invisible_top_data_set: LineDataSet = LineDataSet(null, null)
    private var invisible_bottom_data_set: LineDataSet = LineDataSet(null, null)

    private var color: Int = 0

    private var widgetPlotsInfo: ArrayList<WidgetPlotInfo> = ArrayList()

    private var firstInit = true

    override fun Ubi4WidgetPlotBinding.onBind(plotItem: PlotItem) {
        onDestroyParent{ onDestroy() }
        System.err.println("PlotDelegateAdapter  isEmpty = ${EMGChartLc.isEmpty}")
        System.err.println("PlotDelegateAdapter ${plotItem.title}    data = ${EMGChartLc.data}")
        var deviceAddress = 0
        var parameterID = 0
        var openThreshold = 0
        var closeThreshold = 0

        when (plotItem.widget) {
            is PlotParameterWidgetEStruct -> {
                Log.d("PlotDelegateAdapter", "parametersIDAndDataCodes = ${plotItem.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes}")
                parameterID = plotItem.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.elementAt(0).first
                deviceAddress = plotItem.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId
            }
            is PlotParameterWidgetSStruct -> {
                parameterID = plotItem.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parametersIDAndDataCodes.elementAt(0).first
                deviceAddress = plotItem.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId
            }
        }
        widgetPlotsInfo.add(WidgetPlotInfo(deviceAddress, parameterID, openThreshold, closeThreshold, limitCH1, limitCH2, openThresholdTv, closeThresholdTv, allCHRl))

        Log.d("PlotDelegateAdapter", "deviceAddress = $deviceAddress")
        // а лучше чтоб функция выдавала параметр по адресу девайса и айди параметра
        if (PreferenceKeysUBI4.ParameterTypeEnum.entries[ParameterProvider.getParameter(deviceAddress, parameterID).type].sizeOf != 0) {
            numberOfCharts = ParameterProvider.getParameter(deviceAddress, parameterID).parameterDataSize / PreferenceKeysUBI4.ParameterTypeEnum.entries[ParameterProvider.getParameter(deviceAddress, parameterID).type].sizeOf
        } else {
            numberOfCharts = 0
        }

        plotArrayFlowCollect()

        initializedSensorGraph(EMGChartLc)
        graphThreadFlag = true
        countBinding += 1
        GlobalScope.launch(CoroutineName("startGraphEnteringDataCoroutine $countBinding")) {
            startGraphEnteringDataCoroutine(EMGChartLc)
        }

        main.bleCommand(BLECommands.requestTransferFlow(1), MAIN_CHANNEL, WRITE)
        plotIsReadyToData(numberOfCharts)

        openCHV.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(p0: View, event: MotionEvent): Boolean {
                p0.parent.requestDisallowInterceptTouchEvent(true)
                openThreshold = setLimitePosition(limitCH1, openThresholdTv, allCHRl, event)
                when (event.action) {
                    MotionEvent.ACTION_UP -> {
//                        Log.d("setOnTouchListener", "openThreshold send $openThreshold")
                    }
                }
                return true
            }
        })
        closeCHV.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(p0: View, event: MotionEvent): Boolean {
                p0.parent.requestDisallowInterceptTouchEvent(true)
                closeThreshold = setLimitePosition(limitCH2, closeThresholdTv, allCHRl, event)
                when (event.action) {
                    MotionEvent.ACTION_UP -> {
//                        Log.d ("setOnTouchListener", "closeThreshold send $closeThreshold")
                    }
                }
                return true
            }
        })
    }
    override fun isForViewType(item: Any): Boolean = item is PlotItem
    override fun PlotItem.getItemId(): Any = title
    private fun plotArrayFlowCollect() {
        scope.launch(Dispatchers.IO) {
            merge(
                MainActivityUBI4.plotArrayFlow.map { plotParameterRef ->
                    val indexWidgetPlot = getIndexWidgetPlot(plotParameterRef.addressDevice, plotParameterRef.parameterID)

                    if (plotParameterRef.dataPlots.isNotEmpty()) {
                        System.err.println("FLOW TEST plotArrayFlow ${plotParameterRef.dataPlots.size} indexWidgetPlot: $indexWidgetPlot")
                        if (plotParameterRef.dataPlots.size >= 1) { dataSens1 = plotParameterRef.dataPlots[0] } // нулевой всегда датчик открытия
                        if (plotParameterRef.dataPlots.size >= 2) { dataSens2 = plotParameterRef.dataPlots[1] } // первый всегда датчик закрытия
                        if (plotParameterRef.dataPlots.size >= 3) { dataSens3 = plotParameterRef.dataPlots[2] }
                        if (plotParameterRef.dataPlots.size >= 4) { dataSens4 = plotParameterRef.dataPlots[3] }
                        if (plotParameterRef.dataPlots.size >= 5) { dataSens5 = plotParameterRef.dataPlots[4] }
                        if (plotParameterRef.dataPlots.size >= 6) { dataSens6 = plotParameterRef.dataPlots[5] }
                    }
                },
                MainActivityUBI4.thresholdFlow.map { parameterRef ->
                    val parameter = ParameterProvider.getParameter(parameterRef.addressDevice, parameterRef.parameterID)
                    //TODO тонкое место, переписать (по факту мы должны парсить все данные в структуры и делать это защищённо (как в BaseParameterInfoStruct) даже если там всего два инта)
                    // что не так? Мы упадём при несоответствии длины данных в параметре при эммите
                    //запись пороговых значений при изменении данных в параметре
                    Log.d("thresholdFlow", "thresholdFlow = $parameterRef   data = ${parameter.data}")
//                    val indexWidgetPlot = getIndexWidgetPlot(parameterRef.addressDevice, parameterRef.parameterID)
//                    if (parameter.data=="") "" else widgetPlotsInfo[indexWidgetPlot].openThreshold = castUnsignedCharToInt(parameter.data.substring(0, 2).toInt(16).toByte())
//                    if (parameter.data=="") "" else widgetPlotsInfo[indexWidgetPlot].closeThreshold = castUnsignedCharToInt(parameter.data.substring(2, 4).toInt(16).toByte())
//
//                    //изменение UI в соответствии с новыми порогами
//                    widgetPlotsInfo[indexWidgetPlot].openThresholdTv.text = widgetPlotsInfo[indexWidgetPlot].openThreshold.toString()
//                    widgetPlotsInfo[indexWidgetPlot].closeThresholdTv.text = widgetPlotsInfo[indexWidgetPlot].closeThreshold.toString()
//                    setLimitePosition2(widgetPlotsInfo[indexWidgetPlot].limitCH1, widgetPlotsInfo[indexWidgetPlot].allCHRl, widgetPlotsInfo[indexWidgetPlot].openThreshold)
//                    setLimitePosition2(widgetPlotsInfo[indexWidgetPlot].limitCH2, widgetPlotsInfo[indexWidgetPlot].allCHRl, widgetPlotsInfo[indexWidgetPlot].closeThreshold)

//                    val indexWidgetPlot = getIndexWidgetPlot(parameterRef.addressDevice, parameterRef.parameterID)
                    if (parameter.data=="") "" else widgetPlotsInfo[0].openThreshold = castUnsignedCharToInt(parameter.data.substring(0, 2).toInt(16).toByte())
                    if (parameter.data=="") "" else widgetPlotsInfo[0].closeThreshold = castUnsignedCharToInt(parameter.data.substring(4, 6).toInt(16).toByte())
//
//                    //изменение UI в соответствии с новыми порогами
                    widgetPlotsInfo[0].openThresholdTv.text = widgetPlotsInfo[0].openThreshold.toString()
                    widgetPlotsInfo[0].closeThresholdTv.text = widgetPlotsInfo[0].closeThreshold.toString()
                    setLimitePosition2(widgetPlotsInfo[0].limitCH1, widgetPlotsInfo[0].allCHRl, widgetPlotsInfo[0].openThreshold)
                    setLimitePosition2(widgetPlotsInfo[0].limitCH2, widgetPlotsInfo[0].allCHRl, widgetPlotsInfo[0].closeThreshold)
                }
            ).collect()
        }
    }

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
        set1.color = main.applicationContext.getColor(R.color.ubi4_white)
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
        set2.color = main.applicationContext.getColor(R.color.ubi4_deactivate_text)
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
        val data: LineData =  emgChart.data
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
    private fun getIndexWidgetPlot (addressDevice: Int, parameterID: Int): Int {
        widgetPlotsInfo.forEachIndexed { index, widgetSliderInfo ->
            if (widgetSliderInfo.addressDevice == addressDevice && widgetSliderInfo.parameterID == parameterID) {
                return index
            }
        }
        return -1
    }
    private fun setLimitePosition(limit_CH: RelativeLayout, thresholdTv: TextView, allCHRl: LinearLayout, event: MotionEvent): Int {
        var y = event.y
        if (y < 0)
            y = 0f
        if (y > allCHRl.height)
            y = allCHRl.height.toFloat()
        limit_CH.y = y - limit_CH.height/2 + allCHRl.marginTop
        thresholdTv.text = ((allCHRl.height - y)/allCHRl.height * 255).toInt().toString()
        return ((allCHRl.height - y)/allCHRl.height * 255).toInt()
    }
    private fun setLimitePosition2(limit_CH: RelativeLayout, allCHRl: LinearLayout, threshold: Int) {
        var y = allCHRl.height - allCHRl.height*threshold/255
        limit_CH.y = (y - limit_CH.height/2 + allCHRl.marginTop).toFloat()
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
    fun onDestroy() {
        scope.cancel()
        Log.d("onDestroy" , "onDestroy plot")
    }
}

data class WidgetPlotInfo (
    var addressDevice: Int = 0,
    var parameterID: Int = 0,
    var openThreshold: Int = 0,
    var closeThreshold: Int = 0,
    var limitCH1: RelativeLayout,
    var limitCH2: RelativeLayout,
    var openThresholdTv: TextView,
    var closeThresholdTv: TextView,
    var allCHRl: LinearLayout,
)