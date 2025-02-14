package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
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
import com.bailout.stickk.ubi4.data.local.PlotThresholds
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.ParameterInfo
import com.bailout.stickk.ubi4.models.PlotItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4.ParameterDataCodeEnum
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.countBinding
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.graphThreadFlag
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.ConstantManager.Companion.DURATION_ANIMATION
import com.bailout.stickk.ubi4.utility.ParameterInfoProvider
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

class PlotDelegateAdapter (
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) :
    ViewBindingDelegateAdapter<PlotItem, Ubi4WidgetPlotBinding>(Ubi4WidgetPlotBinding::inflate) {
    private var scope: CoroutineScope? = null
    private var count: Int = 0
    private var dataSens1 = 0
    private var dataSens2 = 0
    private var dataSens3 = 0
    private var dataSens4 = 0
    private var dataSens5 = 0
    private var dataSens6 = 0
    private var numberOfCharts = 2
    private var parameterInfoSet: MutableSet<ParameterInfo<Int, Int, Int, Int>> = mutableSetOf()

    private var widgetPlotsInfo: ArrayList<WidgetPlotInfo> = ArrayList()
    private val defaultEntry = Entry(count.toFloat(), 250.toFloat())

    private var firstInit = true
    private var openThreshold = 0
    private var closeThreshold = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetPlotBinding.onBind(plotItem: PlotItem) {
        onDestroyParent{ onDestroy() }
        System.err.println("PlotDelegateAdapter  isEmpty = ${EMGChartLc.isEmpty}")
        System.err.println("PlotDelegateAdapter ${plotItem.title}    data = ${EMGChartLc.data}")
        var deviceAddress = 0
        val parameterID = 0




        when (plotItem.widget) {
            is PlotParameterWidgetEStruct -> {
                parameterInfoSet = plotItem.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet
                deviceAddress = plotItem.widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.deviceId
            }
            is PlotParameterWidgetSStruct -> {
                parameterInfoSet = plotItem.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet
                deviceAddress = plotItem.widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.deviceId
            }
        }
        widgetPlotsInfo.add(WidgetPlotInfo(deviceAddress, parameterID, openThreshold, closeThreshold,0,0,0,0, limitCH1, limitCH2, openThresholdTv, closeThresholdTv, allCHRl))

        Log.d("PlotDelegateAdapter", "deviceAddress = $deviceAddress")
        // а лучше чтоб функция выдавала параметр по адресу девайса и айди параметра
        if (PreferenceKeysUBI4.ParameterTypeEnum.entries[ParameterProvider.getParameter(deviceAddress, parameterID).type].sizeOf != 0) {
            numberOfCharts = ParameterProvider.getParameter(deviceAddress, parameterID).parameterDataSize / PreferenceKeysUBI4.ParameterTypeEnum.entries[ParameterProvider.getParameter(deviceAddress, parameterID).type].sizeOf
        } else {
            numberOfCharts = 0
        }

        countBinding += 1

        main.bleCommandWithQueue(BLECommands.requestThresholds(ParameterInfoProvider.getDeviceAddressByDataCode(ParameterDataCodeEnum.PDCE_OPEN_CLOSE_THRESHOLD.number, parameterInfoSet), ParameterInfoProvider.getParameterIDByCode(ParameterDataCodeEnum.PDCE_OPEN_CLOSE_THRESHOLD.number, parameterInfoSet)) , MAIN_CHANNEL, WRITE){}
        Log.d("PlotDelegateAdapter", "parametersIDAndDataCodes = $parameterInfoSet")


        val filteredSet = parameterInfoSet.filter { it.dataCode == ParameterDataCodeEnum.PDCE_OPEN_CLOSE_THRESHOLD.number }.toSet()

        openCHV.setOnTouchListener { p0, event ->
            p0.parent.requestDisallowInterceptTouchEvent(true)
            openThreshold = setLimitPosition(limitCH1, openThresholdTv, allCHRl, event)
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    Log.d("setOnTouchListener", "openThreshold send $openThreshold")
                    main.bleCommandWithQueue(BLECommands.sendThresholdsCommand(filteredSet.elementAt(0).deviceAddress, filteredSet.elementAt(0).parameterID, arrayListOf(openThreshold,0,closeThreshold,0)), MAIN_CHANNEL, WRITE){}
                }
            }
            true
        }
        closeCHV.setOnTouchListener { p0, event ->
            p0.parent.requestDisallowInterceptTouchEvent(true)
            closeThreshold = setLimitPosition(limitCH2, closeThresholdTv, allCHRl, event)
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    Log.d("setOnTouchListener", "closeThreshold send $closeThreshold  deviceAddress = $deviceAddress  parameterID = $parameterID")
                    main.bleCommandWithQueue(BLECommands.sendThresholdsCommand(filteredSet.elementAt(1).deviceAddress, filteredSet.elementAt(1).parameterID, arrayListOf(openThreshold,0,closeThreshold,0)), MAIN_CHANNEL, WRITE){}
                }
            }
            true
        }

    }
    override fun Ubi4WidgetPlotBinding.onAttachedToWindow() {
        Log.d("Plot view","View attached")
        if (scope != null) {
            Log.d("Plot view", "2 Scope already exists, skipping.")
        } else {
            // Создаем новый scope
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            initializedSensorGraph(EMGChartLc)
            plotArrayFlowCollect()
        }
        graphThreadFlag = true
        scope?.launch {
            startGraphEnteringDataCoroutine(EMGChartLc)
        }
    }
    override fun Ubi4WidgetPlotBinding.onDetachedFromWindow() {
        Log.d("Plot view","View detached")
//        onDestroy()
    }
    override fun isForViewType(item: Any): Boolean = item is PlotItem
    override fun PlotItem.getItemId(): Any = title
    private fun plotArrayFlowCollect() {
        scope?.launch(Dispatchers.IO) {
            try {
                merge(
                    MainActivityUBI4.plotArrayFlow.map { plotParameterRef ->
                        val indexWidgetPlot = getIndexWidgetPlot(
                            plotParameterRef.addressDevice,
                            plotParameterRef.parameterID
                        )

                        if (plotParameterRef.dataPlots.isNotEmpty()) {
                            System.err.println("FLOW TEST plotArrayFlow ${plotParameterRef.dataPlots.size} indexWidgetPlot: $indexWidgetPlot")
                            if (plotParameterRef.dataPlots.size >= 1) {
                                dataSens1 = plotParameterRef.dataPlots[0]
                            } // нулевой всегда датчик открытия
                            if (plotParameterRef.dataPlots.size >= 2) {
                                dataSens2 = plotParameterRef.dataPlots[1]
                            } // первый всегда датчик закрытия
                            if (plotParameterRef.dataPlots.size >= 3) {
                                dataSens3 = plotParameterRef.dataPlots[2]
                            }
                            if (plotParameterRef.dataPlots.size >= 4) {
                                dataSens4 = plotParameterRef.dataPlots[3]
                            }
                            if (plotParameterRef.dataPlots.size >= 5) {
                                dataSens5 = plotParameterRef.dataPlots[4]
                            }
                            if (plotParameterRef.dataPlots.size >= 6) {
                                dataSens6 = plotParameterRef.dataPlots[5]
                            }
                        }
                    },
                    MainActivityUBI4.thresholdFlow.map { parameterRef ->
                        val parameter = ParameterProvider.getParameter(
                            parameterRef.addressDevice,
                            parameterRef.parameterID
                        )
                        val plotThresholds = Json.decodeFromString<PlotThresholds>("\"${parameter.data}\"")
                        Log.d("plotThresholds", "plotThresholds $plotThresholds")
                        //TODO тонкое место, переписать (по факту мы должны парсить все данные в структуры и делать это защищённо (как в BaseParameterInfoStruct) даже если там всего два инта)
                        // что не так? Мы упадём при несоответствии длины данных в параметре при эммите
                        //запись пороговых значений при изменении данных в параметре
                        Log.d(
                            "thresholdFlow",
                            "thresholdFlow = $parameterRef   data = ${parameter.data}"
                        )
                        if (parameter.data != "") {
                            widgetPlotsInfo[0].apply {
                                openThreshold   = plotThresholds.threshold1
                                closeThreshold  = plotThresholds.threshold2
                                threshold3      = plotThresholds.threshold3
                                threshold4      = plotThresholds.threshold4
                                threshold5      = plotThresholds.threshold5
                                threshold6      = plotThresholds.threshold6
                            }
                        }


                        //изменение UI в соответствии с новыми порогами
                        widgetPlotsInfo[0].openThresholdTv.text =
                            widgetPlotsInfo[0].openThreshold.toString()
                        widgetPlotsInfo[0].closeThresholdTv.text =
                            widgetPlotsInfo[0].closeThreshold.toString()
                        setLimitPosition2(
                            widgetPlotsInfo[0].limitCH1,
                            widgetPlotsInfo[0].allCHRl,
                            widgetPlotsInfo[0].openThreshold
                        )
                        setLimitPosition2(
                            widgetPlotsInfo[0].limitCH2,
                            widgetPlotsInfo[0].allCHRl,
                            widgetPlotsInfo[0].closeThreshold
                        )
                        openThreshold = widgetPlotsInfo[0].openThreshold
                        closeThreshold = widgetPlotsInfo[0].closeThreshold
                    }
                ).collect()
            } catch (e: CancellationException) {
                Log.d("plotArrayFlowCollect", "Job was cancelled: ${e.message}")
            } catch (e: Exception) {
                main.runOnUiThread {
                    main.showToast("ERROR plotArrayFlowCollect")
                }
                Log.e("plotArrayFlowCollect", "Exception: ${e.message}")

            }
        }
    }


    //////////////////////////////////////////////////////////////////////////////
    /**                          работа с графиками                            **/
    //////////////////////////////////////////////////////////////////////////////
    private fun createSet(): LineDataSet {
        val set = LineDataSet(null, null)
        set.setDrawCircles(false)
        set.setDrawValues(false)
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
        set1.setDrawCircles(false)
        set1.setDrawValues(false)
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
        set2.setDrawCircles(false)
        set2.setDrawValues(false)
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
        set3.setDrawCircles(false)
        set3.setDrawValues(false)
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
        set4.setDrawCircles(false)
        set4.setDrawValues(false)
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
        set5.setDrawCircles(false)
        set5.setDrawValues(false)
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
        set6.setDrawCircles(false)
        set6.setDrawValues(false)
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

    private suspend fun prepareAndAddEntry(sens1: Int, sens2: Int, sens3: Int, sens4: Int, sens5: Int, sens6: Int, emgChart: LineChart) {
        if (graphThreadFlag) {
            Log.d("Plot view", "graphThreadFlag")
        } else {
            Log.d("Plot view", "false graphThreadFlag")
        }
        val preparedEntries = withContext(Dispatchers.IO) {
            listOf(
                Entry(count.toFloat(), sens1.toFloat()),
                Entry(count.toFloat(), sens2.toFloat()),
                Entry(count.toFloat(), sens3.toFloat()),
                Entry(count.toFloat(), sens4.toFloat()),
                Entry(count.toFloat(), sens5.toFloat()),
                Entry(count.toFloat(), sens6.toFloat())
            )
        }
        try {
            // Передаём обработанные данные в addEntry
            addEntry(preparedEntries, emgChart)
        } catch (e:ConcurrentModificationException){
            main.showToast("Ошибка: изменение данных во время отрисовки!")
        }

    }
    private fun addEntry(preparedEntries: List<Entry>, emgChart: LineChart) {
        val data: LineData =  emgChart.data
        var set = data.getDataSetByIndex(0)
        var set1 = data.getDataSetByIndex(1)
        var set2 = data.getDataSetByIndex(2)
        var set3 = data.getDataSetByIndex(3)
        var set4 = data.getDataSetByIndex(4)
        var set5 = data.getDataSetByIndex(5)
        var set6 = data.getDataSetByIndex(6)

        if (set1 == null) {
            Log.d("Plot view","создание новых DataSet  numberOfCharts = $numberOfCharts  countBinding = $countBinding ")
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

        main.runOnUiThread {
            if (set1.entryCount > 200) {
                set.removeFirst()
                set1.removeFirst()
                if (numberOfCharts >= 2) { set2.removeFirst() }
                if (numberOfCharts >= 3) { set3.removeFirst() }
                if (numberOfCharts >= 4) { set4.removeFirst() }
                if (numberOfCharts >= 5) { set5.removeFirst() }
                if (numberOfCharts >= 6) { set6.removeFirst() }
            }

            data.addEntry(defaultEntry, 0)
            data.addEntry(preparedEntries[0], 1)
            if (numberOfCharts >= 2) {data.addEntry(preparedEntries[1], 2)}
            if (numberOfCharts >= 3) {data.addEntry(preparedEntries[2], 3)}
            if (numberOfCharts >= 4) {data.addEntry(preparedEntries[3], 4)}
            if (numberOfCharts >= 5) {data.addEntry(preparedEntries[4], 5)}
            if (numberOfCharts >= 6) {data.addEntry(preparedEntries[5], 6)}

            data.notifyDataChanged()
            emgChart.notifyDataSetChanged()
            emgChart.moveViewToX(preparedEntries[0].x - 200.toFloat()) // Прокрутка графика

            if (firstInit) {
                emgChart.setVisibleXRangeMaximum(200f)
                firstInit = false
            }
        }
        count += 1
    }
    private fun initializedSensorGraph(emgChart: LineChart) {
        emgChart.setHardwareAccelerationEnabled(true) // Включение аппаратного ускорения
        emgChart.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        emgChart.setDragEnabled(false) // Отключение перемещения графика, если оно не нужно
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
        emgChart.animateX(0)
        emgChart.animateY(0)
//        emgChart.animateY(2000)

        val x: XAxis = emgChart.xAxis
        x.textColor = Color.TRANSPARENT
        x.setDrawGridLines(false)
        x.setDrawLabels(false)
        emgChart.axisLeft.setDrawGridLines(false)
        emgChart.axisLeft.setDrawLabels(false)
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
        widgetPlotsInfo.forEachIndexed { index, widgetPlotInfo ->
            if (widgetPlotInfo.addressDevice == addressDevice && widgetPlotInfo.parameterID == parameterID) {
                return index
            }
        }
        return -1
    }
    private fun setLimitPosition(limit_CH: RelativeLayout, thresholdTv: TextView, allCHRl: LinearLayout, event: MotionEvent): Int {
        var y = event.y
        if (y < 0)
            y = 0f
        if (y > allCHRl.height)
            y = allCHRl.height.toFloat()
        limit_CH.y = y - limit_CH.height/2 + allCHRl.marginTop
        thresholdTv.text = ((allCHRl.height - y)/allCHRl.height * 255).toInt().toString()
        return ((allCHRl.height - y)/allCHRl.height * 255).toInt()
    }
//    private fun setLimitPosition2(limit_CH: RelativeLayout, allCHRl: LinearLayout, threshold: Int) {
//        var y = allCHRl.height - allCHRl.height*threshold/255
//        limit_CH.y = (y - limit_CH.height/2 + allCHRl.marginTop).toFloat()//end position
//    }


    private fun setLimitPosition2(limit_CH: RelativeLayout, allCHRl: LinearLayout, threshold: Int, duration: Long = DURATION_ANIMATION) {
        // Выполняем вычисления после того, как layout уже измерен
        allCHRl.post {
            val targetY = (allCHRl.height - (allCHRl.height * threshold / 255) - limit_CH.height / 2 + allCHRl.marginTop).toFloat()
            val startY = limit_CH.y

            ValueAnimator.ofFloat(startY, targetY).apply {
                this.duration = duration
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animator ->
                    limit_CH.y = animator.animatedValue as Float
                }
                start()
            }
        }
    }

    private suspend fun startGraphEnteringDataCoroutine(emgChart: LineChart)  {
//        Log.d("Plot view","startGraphEnteringDataCoroutine")
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

        prepareAndAddEntry(dataSens1, dataSens2, dataSens3, dataSens4, dataSens5, dataSens6, emgChart)
        delay(ConstantManager.GRAPH_UPDATE_DELAY.toLong())
        if (graphThreadFlag) {
            startGraphEnteringDataCoroutine(emgChart)
        }
    }
    fun onDestroy() {
        graphThreadFlag = false
        setLimitPosition2(widgetPlotsInfo[0].limitCH1, widgetPlotsInfo[0].allCHRl, 0)
        setLimitPosition2(widgetPlotsInfo[0].limitCH2, widgetPlotsInfo[0].allCHRl, 0)
//        scope?.cancel()
        Log.d("onDestroy" , "onDestroy plot")
    }
}

data class WidgetPlotInfo (
    var addressDevice: Int = 0,
    var parameterID: Int = 0,
    var openThreshold: Int = 0,
    var closeThreshold: Int = 0,
    var threshold3: Int = 0,
    var threshold4: Int = 0,
    var threshold5: Int = 0,
    var threshold6: Int = 0,
    var limitCH1: RelativeLayout,
    var limitCH2: RelativeLayout,
    var openThresholdTv: TextView,
    var closeThresholdTv: TextView,
    var allCHRl: LinearLayout,
)