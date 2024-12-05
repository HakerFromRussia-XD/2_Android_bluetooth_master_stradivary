package com.bailout.stickk.ubi4.adapters.widgetDelegeteAdapters

import android.R.attr.button
import android.R.attr.padding
import android.graphics.Color
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.marginTop
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetPlotMyBinding
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.PlotItem
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
import com.simform.refresh.SSPullToRefreshLayout
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PlotDelegateAdapterMy (
    val plotIsReadyToData:(num: Int) -> Unit) :
    ViewBindingDelegateAdapter<PlotItem, Ubi4WidgetPlotMyBinding>(Ubi4WidgetPlotMyBinding::inflate) {
    private var count: Int = 0
    private var dataSens1 = 0
    private var dataSens2 = 0
    private var dataSens3 = 0
    private var dataSens4 = 0
    private var dataSens5 = 0
    private var dataSens6 = 0
    private var dataSenses: MutableList<Int> = ArrayList()
    private var numberOfCharts = 9

    private var raw_data_set1: List<Entry> = ArrayList()
    private var g_data_sets: MutableList<LineDataSet> = ArrayList()
    private var g_data_set: LineDataSet = LineDataSet(null,null)
    private var invisible_top_data_set: LineDataSet = LineDataSet(null, null)
    private var invisible_bottom_data_set: LineDataSet = LineDataSet(null, null)

    private var g_select_chart_buttons: MutableList<Button> = ArrayList()
    private var g_selected_chart: Int = -1


    private var color: Int = 0

    private var firstInit = true

    @OptIn(DelicateCoroutinesApi::class)
    override fun Ubi4WidgetPlotMyBinding.onBind(plotItem: PlotItem) {
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
        if (PreferenceKeysUBI4.ParameterTypeEnum.entries[ParameterProvider.getParameter(deviceAddress, parameterID).type].sizeOf != 0){
            numberOfCharts = ParameterProvider.getParameter(deviceAddress, parameterID).parameterDataSize / PreferenceKeysUBI4.ParameterTypeEnum.entries[ParameterProvider.getParameter(deviceAddress, parameterID).type].sizeOf;
        } else {
//            numberOfCharts = 0 TODO:вернуть обратно
            numberOfCharts = 9
        }
        Log.d("PlotDelegateAdapter", "numberOfCharts = $numberOfCharts parametrSize = ${ParameterProvider.getParameter(deviceAddress, parameterID).parameterDataSize}   type = ${ParameterProvider.getParameter(deviceAddress, parameterID).type}")

        plotArrayFlowCollect()

        initializedSensorGraph(EMGChartLc)
        graphThreadFlag = true
        countBinding += 1
        for (i in 0..numberOfCharts-1){
            val data_set: LineDataSet = LineDataSet(null,null)
            data_set.lineWidth = 2f
            data_set.color = Color.WHITE
            data_set.mode = LineDataSet.Mode.LINEAR
            data_set.setCircleColor(Color.TRANSPARENT)
            data_set.circleHoleColor = Color.TRANSPARENT
            data_set.fillColor = ColorTemplate.getHoloBlue()
            data_set.highLightColor = Color.rgb(244, 117, 177)
            data_set.valueTextColor = Color.TRANSPARENT
            g_data_sets.add(data_set)

            dataSenses.add(i,0)
        }
        GlobalScope.launch(CoroutineName("startGraphEnteringDataCoroutine $countBinding")) {
            startGraphEnteringDataCoroutine(EMGChartLc)
        }

        main.bleCommand(BLECommands.requestTransferFlow(1), MAIN_CHANNEL, WRITE)
//        System.err.println("plotIsReadyToData")
        plotIsReadyToData(0)

        color = main.applicationContext.getColor(R.color.open_threshold)


        g_data_set.axisDependency = YAxis.AxisDependency.LEFT
        for(i in 0..< numberOfCharts){
            g_select_chart_buttons.add(i, Button(main))
//            g_select_chart_buttons[i].layoutParams = LinearLayout(main).layoutParams

            g_select_chart_buttons[i].background = AppCompatResources.getDrawable(main, R.drawable.ubi4_view_with_corners_gray)
            g_select_chart_buttons[i].layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            val margin = (25 * main.resources.displayMetrics.density).toInt()
            val param = g_select_chart_buttons[i].layoutParams as ViewGroup.MarginLayoutParams
            if(i == 0 )
                param.setMargins(20,20,10,10)
            else if (i == numberOfCharts-1)
                param.setMargins(10,20,20,10)
            else
                param.setMargins(10,20,10,10)
            g_select_chart_buttons[i].setPadding(1,1,1,1);
            g_select_chart_buttons[i].layoutParams = param
            g_select_chart_buttons[i].text = (i+1).toString()
            g_select_chart_buttons[i].setTextColor(Color.WHITE)
            g_select_chart_buttons[i].textSize = 20f
            g_select_chart_buttons[i].setOnClickListener {
                if(i != g_selected_chart) {
                    if(g_selected_chart != -1) {
                        g_select_chart_buttons[g_selected_chart].background =
                            AppCompatResources.getDrawable(
                                main,
                                R.drawable.ubi4_view_with_corners_gray
                            )


                    }
                    g_select_chart_buttons[i].background = AppCompatResources.getDrawable(main, R.drawable.ubi4_view_with_corners_gray_active)
                    touchAriaOpen.background = AppCompatResources.getDrawable(main, R.drawable.ubi4_chart_limit_active)
                    openThresholdIv.background = AppCompatResources.getDrawable(main,R.color.ubi4_active)

                    g_selected_chart = i
                }else{
                    g_select_chart_buttons[i].background = AppCompatResources.getDrawable(main, R.drawable.ubi4_view_with_corners_gray)
                    g_selected_chart = -1
                    touchAriaOpen.background = AppCompatResources.getDrawable(main, R.drawable.ubi4_chart_limit_base)
                    openThresholdIv.background = AppCompatResources.getDrawable(main,R.color.ubi4_gray_border)
                }


            }
            topPanel.addView(g_select_chart_buttons[i])
        }

//        val mybtn = Button(main)
//        mybtn.background = AppCompatResources.getDrawable(main, R.drawable.ubi4_view_with_corners_gray_active)
//        mybtn.setOnClickListener {
//            if( mybtn.background == AppCompatResources.getDrawable(main, R.drawable.ubi4_view_with_corners_gray_active) ){
//                mybtn.background = AppCompatResources.getDrawable(main, R.drawable.ubi4_view_with_corners_gray)
//            }else{
//                mybtn.background = AppCompatResources.getDrawable(main, R.drawable.ubi4_view_with_corners_gray_active)
//            }
//        }

//        topPanel.addView(mybtn)


        g_data_set.lineWidth = 2f
        g_data_set.color = Color.WHITE
        g_data_set.mode = LineDataSet.Mode.LINEAR
        g_data_set.setCircleColor(Color.TRANSPARENT)
        g_data_set.circleHoleColor = Color.TRANSPARENT
        g_data_set.fillColor = ColorTemplate.getHoloBlue()
        g_data_set.highLightColor = Color.rgb(244, 117, 177)
        g_data_set.valueTextColor = Color.TRANSPARENT


        invisible_top_data_set.color = Color.TRANSPARENT
        invisible_top_data_set.valueTextColor = Color.TRANSPARENT
        invisible_top_data_set.fillColor = Color.TRANSPARENT
        invisible_top_data_set.circleHoleColor =  Color.TRANSPARENT
        invisible_top_data_set.setCircleColor(Color.TRANSPARENT)
//        invisible_top_data_set.axisDependency = EMGChartLc.axisLeft.axisDependency


        invisible_bottom_data_set.color = Color.TRANSPARENT
        invisible_bottom_data_set.valueTextColor = Color.TRANSPARENT
        invisible_bottom_data_set.fillColor = Color.TRANSPARENT
        invisible_bottom_data_set.circleHoleColor =  Color.TRANSPARENT
        invisible_bottom_data_set.setCircleColor(Color.TRANSPARENT)


        openCHV.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(p0: View, p1: MotionEvent): Boolean {
//                p0.performClick()
                p0.parent.requestDisallowInterceptTouchEvent(true)

                var y = p1.y
                if (y < 0)
                    y = 0f
                if (y > allCHRl.height)
                    y = allCHRl.height.toFloat()
                limitCH1.y = y - limitCH1.height/2 + allCHRl.marginTop
                openThresholdTv.text = ((allCHRl.height - y)/allCHRl.height * 255).toInt().toString()
                return true
            }
        })
//        openCHV.
//        closeCHV.setOnTouchListener(object : View.OnTouchListener{
//            override fun onTouch(p0: View, p1: MotionEvent): Boolean {
//                p0.parent.requestDisallowInterceptTouchEvent(true)
//                var y = p1.y
//                if (y < 0)
//                    y = 0f
//                if (y > allCHRl.height)
//                    y = allCHRl.height.toFloat()
//                limitCH2.y = y - limitCH2.height/2 + allCHRl.marginTop
//                closeThresholdTv.text = ((allCHRl.height - y)/allCHRl.height * 255).toInt().toString()
//                return true
//            }
//        })


    }

    override fun isForViewType(item: Any): Boolean = item is PlotItem
    override fun PlotItem.getItemId(): Any = title

    //////////////////////////////////////////////////////////////////////////////
    /**                          работа с графиками                            **/
    //////////////////////////////////////////////////////////////////////////////

    private fun addEntrys(senses: MutableList<Int>, dataSets: MutableList<LineDataSet>, emgChart: LineChart) {
        val data: LineData =  emgChart.data

        for(i in 0..numberOfCharts-1){
            if(dataSets[i].entryCount > 200){
                main.runOnUiThread {
                    dataSets[i].removeFirst()
                }
            }
        }
        main.runOnUiThread {

            for(i in 0..<numberOfCharts){
                dataSets[i].addEntry(Entry(count.toFloat(), senses[i].toFloat()))
                data.removeDataSet(dataSets[i])
                if(i == g_selected_chart)
                    dataSets[i].color = main.getColor(R.color.ubi4_active)
                else if(g_selected_chart == -1 )
                    dataSets[i].color = Color.WHITE
                else
                    dataSets[i].color = Color.DKGRAY
                data.addDataSet(dataSets[i])
            }

            invisible_top_data_set.addEntry(Entry(count.toFloat(), 255f))
            invisible_bottom_data_set.addEntry(Entry(count.toFloat(), 0f))

            data.removeDataSet(invisible_top_data_set)
            data.addDataSet(invisible_top_data_set)
            data.removeDataSet(invisible_bottom_data_set)
            data.addDataSet(invisible_bottom_data_set)

            emgChart.notifyDataSetChanged()

            if (firstInit) {
                emgChart.setVisibleXRangeMaximum(200f)
                firstInit = false
            }
        }

        emgChart.moveViewToX(count - 200.toFloat())
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
        emgChart.isAutoScaleMinMaxEnabled = false
//        emgChart.isScaleYEnabled = false
        emgChart.setBackgroundColor(Color.TRANSPARENT)
        emgChart.getHighlightByTouchPoint(1f, 1f)
        val data = LineData()
//        val data2 = LineData()
        emgChart.data = data
//        emgChart.data = data2
        emgChart.legend.isEnabled = false
        emgChart.description.textColor = Color.TRANSPARENT
//        emgChart.animateY(2000)

        val x: XAxis = emgChart.xAxis
        x.textColor = Color.TRANSPARENT
        x.setDrawGridLines(false)
        x.axisMaximum = 4000000f
        x.setAvoidFirstLastClipping(true)
        x.position = XAxis.XAxisPosition.BOTTOM
//
        val y: YAxis = emgChart.axisLeft
        y.textColor = Color.WHITE
        y.mAxisMaximum = 255f
        y.mAxisMinimum = 0f
        y.textSize = 0f
        y.textColor = Color.TRANSPARENT
        y.setDrawGridLines(true)
        y.setDrawAxisLine(false)
        y.setStartAtZero(true)
        y.gridColor = Color.WHITE
        y.yOffset = 0f
        y.spaceTop = 0f
        y.spaceBottom = 0f

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
//        if (!firstInit) {
            for (i in 0..numberOfCharts - 1) {
                dataSenses[i] += i+1
                dataSenses[i] %= 256
            }
            dataSens1 += 1
//        dataSens2 += 1
//        dataSens3 += 1
//        dataSens4 += 2
//        dataSens5 += 2
//        dataSens6 += 2
            if (dataSens1 > 255) {
                dataSens1 = 0
            }
//        if (dataSens2 > 255) { dataSens2 = 0 }
//        if (dataSens3 > 255) { dataSens3 = 0 }
//        if (dataSens4 > 255) { dataSens4 = 0 }
//        if (dataSens5 > 255) { dataSens5 = 0 }
//        if (dataSens6 > 255) { dataSens6 = 0 }

//        addEntry(dataSens1, dataSens2, dataSens3, dataSens4, dataSens5, dataSens6, emgChart)
            addEntrys(dataSenses, g_data_sets, emgChart)
            delay(ConstantManager.GRAPH_UPDATE_DELAY.toLong())
            if (graphThreadFlag) {
                startGraphEnteringDataCoroutine(emgChart)
            }
//        }
    }
}