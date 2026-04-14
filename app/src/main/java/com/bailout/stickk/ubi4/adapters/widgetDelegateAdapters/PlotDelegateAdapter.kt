package com.bailout.stickk.ubi4.adapters.widgetDelegateAdapters

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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.marginTop
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetPlotBinding
import com.bailout.stickk.new_electronic_by_Rodeon.ble.ConstantManager
import com.bailout.stickk.ubi4.ble.BLECommands
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.MAIN_CHANNEL_CHARACTERISTIC
import com.bailout.stickk.ubi4.ble.SampleGattAttributes.WRITE
import com.bailout.stickk.ubi4.data.local.PlotThresholds
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.data.state.WidgetState.countBinding
import com.bailout.stickk.ubi4.data.state.WidgetState.graphThreadFlag
import com.bailout.stickk.ubi4.data.state.WidgetState.plotArrayFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.thresholdFlow
import com.bailout.stickk.ubi4.data.state.WidgetState.widgetsMergeEventFlow
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.PlotItem
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterDataCodeEnum
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.main
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.DURATION_ANIMATION
import com.bailout.stickk.ubi4.utility.ParameterInfoProvider
import com.bailout.stickk.ubi4.utility.RetryUtils
import com.bailout.stickk.ubi4.utility.logging.platformLog
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.renderer.XAxisRenderer
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

class PlotDelegateAdapter (
    val onDestroyParent: (onDestroyParent: (() -> Unit)) -> Unit,
) :
    ViewBindingDelegateAdapter<PlotItem, Ubi4WidgetPlotBinding>(Ubi4WidgetPlotBinding::inflate) {
    private companion object {
        private const val SENSOR_COUNT = 6
        private const val SMOOTHING_TICKS = 3
    }

    private var scope: CoroutineScope? = null
    private var count: Int = 0
    private var numberOfCharts = 2
    private var parameterInfoSet: MutableSet<ParameterInfo<Int, Int, Int, Int>> = mutableSetOf()

    private val requestedThresholdRefs = mutableSetOf<Pair<Int, Int>>()


    private var widgetPlotsInfo: ArrayList<WidgetPlotInfo> = ArrayList()

    private var firstInit = true
    private var openThreshold = 0
    private var closeThreshold = 0

    private var rampTick = 0
    private val startSensors = DoubleArray(SENSOR_COUNT)
    private val targetSensors = DoubleArray(SENSOR_COUNT)
    private val currentSensors = DoubleArray(SENSOR_COUNT)



    private val responseReceived = AtomicBoolean(false)

    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetPlotBinding.onBind(plotItem: PlotItem) {
        onDestroyParent { onDestroy() }
        platformLog("[Ubi4WidgetPlotBinding]","работает PlotDelegateAdapter")
        System.err.println("PlotDelegateAdapter  isEmpty = ${EMGChartLc.isEmpty}")
        System.err.println("PlotDelegateAdapter ${plotItem.title}    data = ${EMGChartLc.data}")

        var dataCode = 0

        when (val widget = plotItem.widget) {
            is PlotParameterWidgetEStruct -> {
                parameterInfoSet =
                    widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet
                dataCode =
                    widget.baseParameterWidgetEStruct.baseParameterWidgetStruct.parameterInfoSet.elementAt(
                        0
                    ).dataCode
            }

            is PlotParameterWidgetSStruct -> {
                parameterInfoSet =
                    widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet
                dataCode =
                    widget.baseParameterWidgetSStruct.baseParameterWidgetStruct.parameterInfoSet.elementAt(
                        0
                    ).dataCode

            }
        }

        Log.d("PlotDelegateAdapter", "parameterInfoSet size: ${parameterInfoSet.size}")
        parameterInfoSet.forEach {
            Log.d("PlotDelegateAdapter", "ParameterInfo: $it")
        }
        platformLog("sendWidgetsArray", "▶\uFE0F▶\uFE0F▶\uFE0F parameterInfoSet: $parameterInfoSet")

        widgetPlotsInfo.add(
            WidgetPlotInfo(
                parameterInfoSet,
                openThreshold,
                closeThreshold,
                0,
                0,
                0,
                0,
                limitCH1,
                limitCH2,
                closeThresholdTv,
                openThresholdTv,
                allCHRl
            )
        )

        parameterInfoSet.forEach {
            if (it.dataCode == ParameterDataCodeEnum.PDCE_EMG_CH_1_3_VAL.number) {
                Log.d("PlotDelegateAdapter", "type = ${PreferenceKeysUbi4.ParameterTypeEnum.entries[ParameterProvider.getParameter(
                    it.deviceAddress,
                    it.parameterID
                ).type]}")
                if (PreferenceKeysUbi4.ParameterTypeEnum.entries[ParameterProvider.getParameter(
                        it.deviceAddress,
                        it.parameterID
                    ).type].sizeOf != 0
                ) {
                    numberOfCharts = ParameterProvider.getParameter(
                        it.deviceAddress,
                        it.parameterID
                    ).parameterDataSize / PreferenceKeysUbi4.ParameterTypeEnum.entries[ParameterProvider.getParameter(
                        it.deviceAddress,
                        it.parameterID
                    ).type].sizeOf
                    Log.d(
                        "PlotDelegateAdapter",
                        "Количество графиков: $numberOfCharts ${it.parameterID}"
                    )
                } else {
                    Log.d("PlotDelegateAdapter", "else Количество графиков: $numberOfCharts")

                    numberOfCharts = 0
                }
            }

        }
        Log.d("PlotDelegateAdapter", "Количество графиков: $numberOfCharts")


        countBinding += 1

        responseReceived.set(false)
        Log.d("PlotDelegateAdapter", "parametersIDAndDataCodes = $parameterInfoSet")

        // Порог открытия — слушаем openCHV
        openCHV.setOnTouchListener { v, ev ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            // двигаем ползунок открытия
            openThreshold = setLimitPosition(
                limitCH2,
                openThresholdTv,
                allCHRl,
                ev
            )
            when (ev.action) {
                MotionEvent.ACTION_UP -> {
                    val filteredSet =
                        parameterInfoSet.filter { it.dataCode == ParameterDataCodeEnum.PDCE_OPEN_CLOSE_THRESHOLD.number }
                            .toSet()
                    if (filteredSet.isNotEmpty()) {
                        Log.d("Plot", "openThreshold send $openThreshold  deviceAddress = ${filteredSet.elementAt(0).deviceAddress},  parameterID = ${filteredSet.elementAt(0).parameterID}")
                        main.bleCommandWithQueue(
                            BLECommands.sendThresholdsCommand(
                                filteredSet.elementAt(0).deviceAddress,  // 0 = открытие
                                filteredSet.elementAt(0).parameterID,
                                arrayListOf(openThreshold, 0, closeThreshold, 0)
                            ), MAIN_CHANNEL_CHARACTERISTIC, WRITE
                        ) {}
                    }
                }
            }
            true
        }

        // Порог закрытия — слушаем closeCHV
        closeCHV.setOnTouchListener { v, ev ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            // двигаем ползунок закрытия
            closeThreshold = setLimitPosition(
                limitCH1,
                closeThresholdTv,
                allCHRl,
                ev
            )
            when (ev.action) {
                MotionEvent.ACTION_UP -> {
                    val filteredSet =
                        parameterInfoSet.filter { it.dataCode == ParameterDataCodeEnum.PDCE_OPEN_CLOSE_THRESHOLD.number }
                            .toSet()
                    if (filteredSet.size >= 2) {
                        Log.d("Plot", "closeThreshold send $closeThreshold")
                        main.bleCommandWithQueue(
                            BLECommands.sendThresholdsCommand(
                                filteredSet.elementAt(1).deviceAddress,  // 1 = закрытие
                                filteredSet.elementAt(1).parameterID,
                                arrayListOf(openThreshold, 0, closeThreshold, 0)
                            ), MAIN_CHANNEL_CHARACTERISTIC, WRITE
                        ) {}
                    }
                }
            }
            true
        }

        setLimitPosition2(limitCH2, allCHRl, openThreshold)
        setLimitPosition2(limitCH1, allCHRl, closeThreshold)
    }


    override fun Ubi4WidgetPlotBinding.onAttachedToWindow() {
        Log.d("Plot view","View attached")
        if (scope != null) {
            Log.d("Plot view", "2 Scope already exists, skipping.")
        } else {
            // Создаем новый scope
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            count = 0
            firstInit = true
            resetSmoothingState()
            initializedSensorGraph(EMGChartLc)
            plotArrayFlowCollect()
        }
        graphThreadFlag = true
        requestThresholdsOnce()
        scope?.launch {
            //TODO indexWidgetPlot должен вычисляться в этом месте взависимости от того с каким посчету графиком мы работаем в этой функции
            startGraphEnteringDataCoroutine(EMGChartLc, 0)
        }
    }
    override fun Ubi4WidgetPlotBinding.onDetachedFromWindow() {
        Log.d("Plot view","View detached")
        scope?.cancel()
        scope = null
    }
    override fun isForViewType(item: Any): Boolean = item is PlotItem
    override fun PlotItem.getItemId(): Any = title
    private fun plotArrayFlowCollect() {
        scope?.launch(Dispatchers.IO) {
            try {
                System.err.println("plotArrayFlowCollectttttt")
                merge(
                    plotArrayFlow.map { plotParameterRef ->
                        val indexWidgetPlot = getIndexWidget(
                            plotParameterRef.addressDevice,
                            plotParameterRef.parameterID
                        )
                        if (indexWidgetPlot == -1) return@map

                        if (plotParameterRef.dataPlots.isNotEmpty()) {
                            System.err.println("FLOW TEST plotArrayFlow ${plotParameterRef.dataPlots.size} ")
                            if (plotParameterRef.dataPlots.size >= 1) {
                                widgetPlotsInfo[indexWidgetPlot].dataSens1 = plotParameterRef.dataPlots[0]
                            } // нулевой всегда датчик открытия
                            if (plotParameterRef.dataPlots.size >= 2) {
                                widgetPlotsInfo[indexWidgetPlot].dataSens2 = plotParameterRef.dataPlots[1]
                            } // первый всегда датчик закрытия
                            if (plotParameterRef.dataPlots.size >= 3) {
                                widgetPlotsInfo[indexWidgetPlot].dataSens3 = plotParameterRef.dataPlots[2]
                            }
                            if (plotParameterRef.dataPlots.size >= 4) {
                                widgetPlotsInfo[indexWidgetPlot].dataSens4 = plotParameterRef.dataPlots[3]
                            }
                            if (plotParameterRef.dataPlots.size >= 5) {
                                widgetPlotsInfo[indexWidgetPlot].dataSens5 = plotParameterRef.dataPlots[4]
                            }
                            if (plotParameterRef.dataPlots.size >= 6) {
                                widgetPlotsInfo[indexWidgetPlot].dataSens6 = plotParameterRef.dataPlots[5]
                            }
                        }
                    },
                    thresholdFlow.map { parameterRef ->
                        setUI(parameterRef)
                        platformLog("Test_PLOT", "thresholdFlowCollect Run")
                    },
                    // 3) (опционально) если хочешь реагировать на событие мерджа виджетов
                    widgetsMergeEventFlow.map { parameterRef ->
                        // фильтруем только наш виджет
                        val idx = getIndexWidget(parameterRef.addressDevice, parameterRef.parameterID)
                        if (idx == -1) return@map
                        // на случай, если после мерджа появился новый threshold-параметр
                        val firstThresholdRef = firstThresholdRefFrom(widgetPlotsInfo[idx].parameterInfoSet)
                        if (firstThresholdRef != null && requestedThresholdRefs.add(firstThresholdRef)) {
                            val (addr, pid) = firstThresholdRef
                            responseReceived.set(false)
                            if (RetryUtils.canSendRequestWithFirstReceiveDataFlag(addr, pid)) {
                                val activeScope = scope ?: return@map
                                RetryUtils.sendRequestWithRetry(
                                    request = {
                                        main.bleCommandWithQueue(
                                            BLECommands.requestThresholds(addr, pid),
                                            MAIN_CHANNEL_CHARACTERISTIC, WRITE
                                        ) {}
                                    },
                                    isResponseReceived = { responseReceived.get() },
                                    maxRetries = 5,
                                    delayMillis = 1000L,
                                    scope = activeScope
                                )
                            } else {
                                setUI(
                                    ParameterRef(
                                        addr,
                                        pid,
                                        ParameterDataCodeEnum.PDCE_OPEN_CLOSE_THRESHOLD.number
                                    )
                                )
                            }
                        }
                    }

                ).collect()
            } catch (e: CancellationException) {
                Log.d("plotArrayFlowCollect", "Job was cancelled: ${e.message}")
            } catch (e: Exception) {
                Log.e("plotArrayFlowCollect", "Exception: ${e.message}", e)
                if (scope?.isActive == true) {
                    plotArrayFlowCollect()
                }
            }
        }
    }

    private fun setUI(parameterRef: ParameterRef) {
        responseReceived.set(true) // чтобы RetryUtils понимал, что ответ получен

        val idx = getIndexWidget(parameterRef.addressDevice, parameterRef.parameterID)
        if (idx == -1) return
        val info = widgetPlotsInfo[idx]

        val parameter = ParameterProvider.getParameter(parameterRef.addressDevice, parameterRef.parameterID)
        if (parameter.data.isBlank()) return

        val plotThresholds = Json.decodeFromString<PlotThresholds>("\"${parameter.data}\"")

        info.apply {
            openThreshold   = plotThresholds.threshold1
            closeThreshold  = plotThresholds.threshold2
            threshold3      = plotThresholds.threshold3
            threshold4      = plotThresholds.threshold4
            threshold5      = plotThresholds.threshold5
            threshold6      = plotThresholds.threshold6
        }

        info.openThresholdTv.text  = info.openThreshold.toString()
        info.closeThresholdTv.text = info.closeThreshold.toString()

        setLimitPosition2(info.limitCH2, info.allCHRl, info.openThreshold)
        setLimitPosition2(info.limitCH1, info.allCHRl, info.closeThreshold)

        openThreshold  = info.openThreshold
        closeThreshold = info.closeThreshold
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
    private fun createSet1(emgChart: LineChart): LineDataSet {
        val set1 = LineDataSet(null, null)
        set1.setDrawCircles(false)
        set1.setDrawValues(false)
        set1.axisDependency = YAxis.AxisDependency.LEFT
        set1.lineWidth = 2f
        set1.color = ContextCompat.getColor(emgChart.context, R.color.ubi4_white)
        set1.mode = LineDataSet.Mode.LINEAR
        set1.setCircleColor(Color.TRANSPARENT)
        set1.circleHoleColor = Color.TRANSPARENT
        set1.fillColor = ColorTemplate.getHoloBlue()
        set1.highLightColor = Color.rgb(244, 117, 177)
        set1.valueTextColor = Color.TRANSPARENT
        return set1
    }
    private fun createSet2(emgChart: LineChart): LineDataSet {
        val set2 = LineDataSet(null, null)
        set2.setDrawCircles(false)
        set2.setDrawValues(false)
        set2.axisDependency = YAxis.AxisDependency.LEFT
        set2.lineWidth = 2f
        set2.color = ContextCompat.getColor(emgChart.context, R.color.ubi4_deactivate_text)
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
    private fun createBoundsSet(): LineDataSet {
        val boundsSet = LineDataSet(null, null)
        boundsSet.setDrawCircles(false)
        boundsSet.setDrawValues(false)
        boundsSet.axisDependency = YAxis.AxisDependency.LEFT
        boundsSet.lineWidth = 0f
        boundsSet.color = Color.TRANSPARENT
        boundsSet.mode = LineDataSet.Mode.LINEAR
        boundsSet.setCircleColor(Color.TRANSPARENT)
        boundsSet.circleHoleColor = Color.TRANSPARENT
        boundsSet.fillColor = Color.TRANSPARENT
        boundsSet.highLightColor = Color.TRANSPARENT
        boundsSet.valueTextColor = Color.TRANSPARENT
        boundsSet.isHighlightEnabled = false
        return boundsSet
    }

    private fun normalizeSensorValue(value: Int): Int {
        return if (value in 0..255) value else 0
    }

    private fun resetSmoothingState() {
        rampTick = 0
        for (index in 0 until SENSOR_COUNT) {
            startSensors[index] = 0.0
            targetSensors[index] = 0.0
            currentSensors[index] = 0.0
        }
    }

    private fun smoothSensorValues(rawSensors: IntArray): IntArray {
        var hasNewTarget = false
        for (index in 0 until SENSOR_COUNT) {
            if (rawSensors[index].toDouble() != targetSensors[index]) {
                hasNewTarget = true
                break
            }
        }

        if (hasNewTarget) {
            for (index in 0 until SENSOR_COUNT) {
                startSensors[index] = currentSensors[index]
                targetSensors[index] = rawSensors[index].toDouble()
            }
            rampTick = 0
        }

        val ticks = maxOf(1, SMOOTHING_TICKS)
        val progress = minOf(1.0, (rampTick + 1).toDouble() / ticks.toDouble())
        val smoothedSensors = IntArray(SENSOR_COUNT)

        for (index in 0 until SENSOR_COUNT) {
            currentSensors[index] =
                startSensors[index] + (targetSensors[index] - startSensors[index]) * progress
            smoothedSensors[index] = currentSensors[index].roundToInt()
        }

        if (rampTick < ticks - 1) {
            rampTick += 1
        }

        return smoothedSensors
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
            Log.w("Plot view", "Concurrent modification while rendering chart", e)
            emgChart.post {
                if (emgChart.isAttachedToWindow) {
                    Toast.makeText(
                        emgChart.context,
                        "Ошибка: изменение данных во время отрисовки!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    }
    private fun addEntry(preparedEntries: List<Entry>, emgChart: LineChart) {
        if (WidgetState.pausePlotPointsDuringTransition) return

        val data: LineData =  emgChart.data ?: LineData().also { emgChart.data = it }

        var set = data.getDataSetByIndex(0)
        var set1 = data.getDataSetByIndex(1)
        var set2 = data.getDataSetByIndex(2)
        var set3 = data.getDataSetByIndex(3)
        var set4 = data.getDataSetByIndex(4)
        var set5 = data.getDataSetByIndex(5)
        var set6 = data.getDataSetByIndex(6)
        var setUpperBound = data.getDataSetByIndex(7)
        var setLowerBound = data.getDataSetByIndex(8)

        if (set1 == null) {
            Log.d("Plot view","создание новых DataSet  numberOfCharts = $numberOfCharts  countBinding = $countBinding ")
            set = createSet()
            set1 = createSet1(emgChart)
            set2 = createSet2(emgChart)
            set3 = createSet3()
            set4 = createSet4()
            set5 = createSet5()
            set6 = createSet6()
            setUpperBound = createBoundsSet()
            setLowerBound = createBoundsSet()

            data.addDataSet(set)
            data.addDataSet(set1)
            data.addDataSet(set2)
            data.addDataSet(set3)
            data.addDataSet(set4)
            data.addDataSet(set5)
            data.addDataSet(set6)
            data.addDataSet(setUpperBound)
            data.addDataSet(setLowerBound)
        }
        if (setUpperBound == null) {
            setUpperBound = createBoundsSet()
            data.addDataSet(setUpperBound)
        }
        if (setLowerBound == null) {
            setLowerBound = createBoundsSet()
            data.addDataSet(setLowerBound)
        }

        if (!emgChart.isAttachedToWindow) return
        emgChart.post {
            if (!emgChart.isAttachedToWindow || !graphThreadFlag || WidgetState.pausePlotPointsDuringTransition) return@post
            if (set1.entryCount > 200) {
                set.removeFirst()
                set1.removeFirst()
                if (numberOfCharts >= 2) { set2.removeFirst() }
                if (numberOfCharts >= 3) { set3.removeFirst() }
                if (numberOfCharts >= 4) { set4.removeFirst() }
                if (numberOfCharts >= 5) { set5.removeFirst() }
                if (numberOfCharts >= 6) { set6.removeFirst() }
                setUpperBound.removeFirst()
                setLowerBound.removeFirst()
            }

            data.addEntry(Entry(preparedEntries[0].x, 250f), 0)
            data.addEntry(preparedEntries[0], 1)
            if (numberOfCharts >= 2) {data.addEntry(preparedEntries[1], 2)}
            if (numberOfCharts >= 3) {data.addEntry(preparedEntries[2], 3)}
            if (numberOfCharts >= 4) {data.addEntry(preparedEntries[3], 4)}
            if (numberOfCharts >= 5) {data.addEntry(preparedEntries[4], 5)}
            if (numberOfCharts >= 6) {data.addEntry(preparedEntries[5], 6)}
            data.addEntry(Entry(count.toFloat(), 255f), 7)
            data.addEntry(Entry(count.toFloat(), 0f), 8)

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
        emgChart.setHardwareAccelerationEnabled(true)
        emgChart.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        emgChart.setDragEnabled(false)
        emgChart.setTouchEnabled(false)
        emgChart.isDragEnabled = false
        emgChart.isDragDecelerationEnabled = false
        emgChart.setScaleEnabled(false)
        emgChart.setDrawGridBackground(false)
        emgChart.setPinchZoom(false)
        emgChart.setBackgroundColor(Color.TRANSPARENT)
        emgChart.getHighlightByTouchPoint(1f, 1f)
        emgChart.legend.isEnabled = false
        emgChart.description.textColor = Color.TRANSPARENT
        emgChart.animateX(0)
        emgChart.animateY(0)

        val x = emgChart.xAxis
        x.textColor = Color.TRANSPARENT
        x.setDrawGridLines(false)
        x.setDrawLabels(false)
        x.isGranularityEnabled = true
        x.granularity = 1f
        x.axisMaximum = 4_000_000f
        x.setAvoidFirstLastClipping(true)
        x.position = XAxis.XAxisPosition.BOTTOM
        x.isEnabled = false // как у тебя — ось скрыта

        emgChart.axisLeft.setDrawGridLines(false)
        emgChart.axisLeft.setDrawLabels(false)
        emgChart.data = LineData()

        // ====== ГЛУШИМ ВЫЧИСЛЕНИЯ ОСИ Х (no-op renderer) ======
        val noopRenderer = object : XAxisRenderer(
            emgChart.viewPortHandler,
            x,
            emgChart.getTransformer(YAxis.AxisDependency.LEFT)
        ) {
            override fun computeAxis(min: Float, max: Float, inverted: Boolean) { /* no-op */ }
            override fun computeSize() { /* no-op */ }
        }

        // 1) Попробуем публичный сеттор (есть в некоторых версиях)
        try {
            val m = emgChart.javaClass.getMethod("setXAxisRenderer", XAxisRenderer::class.java)
            m.invoke(emgChart, noopRenderer)
        } catch (_: NoSuchMethodException) {
            // 2) Если сеттора нет — ставим через рефлексию в mXAxisRenderer
            try {
                val clazz = emgChart.javaClass.superclass // BarLineChartBase
                val field = clazz?.getDeclaredField("mXAxisRenderer")
                field?.isAccessible = true
                field?.set(emgChart, noopRenderer)
            } catch (e: Exception) {
                // Если здесь упадёт — сообщи стек, но обычно это работает на старых версиях
            }
        }
        // =======================================================

        val y = emgChart.axisLeft
        y.textColor = Color.WHITE
        y.axisMaximum = 281f
        y.axisMinimum = 0f
        y.isGranularityEnabled = true
        y.granularity = 50f
        y.setLabelCount(6, false)
        y.textSize = 0f
        y.textColor = Color.TRANSPARENT
        y.setDrawGridLines(true)
        y.setDrawAxisLine(false)
        y.gridColor = Color.WHITE

        emgChart.axisRight.gridColor = Color.TRANSPARENT
        emgChart.axisRight.axisLineColor = Color.TRANSPARENT
        emgChart.axisRight.textColor = Color.TRANSPARENT
        emgChart.invalidate()
    }
    private fun getIndexWidget (addressDevice: Int, parameterID: Int): Int {
        widgetPlotsInfo.forEachIndexed { index, widgetPlotInfo ->
            if (widgetPlotInfo.parameterInfoSet.any { it.deviceAddress == addressDevice && it.parameterID == parameterID }) {
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



    private fun setLimitPosition2(limit_CH: RelativeLayout, allCHRl: LinearLayout, threshold: Int, duration: Long = DURATION_ANIMATION) {
        // Выполняем вычисления после того, как layout уже измерен
        allCHRl.post {
            val targetY = (allCHRl.height - (allCHRl.height * threshold / 255) - limit_CH.height / 2 + allCHRl.marginTop).toFloat()
            val startY = limit_CH.y

            val actualDuration = if (WidgetState.dbSnapshotAppliedWithCrc) 0L else duration

            if (actualDuration == 0L) {
                limit_CH.y = targetY
                return@post
            }

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

    private suspend fun startGraphEnteringDataCoroutine(emgChart: LineChart, indexWidgetPlot: Int) {
        while (graphThreadFlag) {
            if (WidgetState.pausePlotPointsDuringTransition) {
                delay(ConstantManager.GRAPH_UPDATE_DELAY.toLong())
                continue
            }

            val rawSensors = intArrayOf(
                normalizeSensorValue(widgetPlotsInfo[indexWidgetPlot].dataSens1),
                normalizeSensorValue(widgetPlotsInfo[indexWidgetPlot].dataSens2),
                normalizeSensorValue(widgetPlotsInfo[indexWidgetPlot].dataSens3),
                normalizeSensorValue(widgetPlotsInfo[indexWidgetPlot].dataSens4),
                normalizeSensorValue(widgetPlotsInfo[indexWidgetPlot].dataSens5),
                normalizeSensorValue(widgetPlotsInfo[indexWidgetPlot].dataSens6)
            )
            val smoothedSensors = smoothSensorValues(rawSensors)

            prepareAndAddEntry(
                smoothedSensors[0],
                smoothedSensors[1],
                smoothedSensors[2],
                smoothedSensors[3],
                smoothedSensors[4],
                smoothedSensors[5],
                emgChart
            )
            delay(ConstantManager.GRAPH_UPDATE_DELAY.toLong())
        }
    }

    private fun firstThresholdRefFrom(set: Set<ParameterInfo<Int, Int, Int, Int>>): Pair<Int, Int>? {
        return set.firstOrNull { it.dataCode == ParameterDataCodeEnum.PDCE_OPEN_CLOSE_THRESHOLD.number }
            ?.let { it.deviceAddress to it.parameterID }
    }


    fun onDestroy() {
        graphThreadFlag = false
        resetSmoothingState()
        if (widgetPlotsInfo.isNotEmpty()) {
            setLimitPosition2(widgetPlotsInfo[0].limitCH2, widgetPlotsInfo[0].allCHRl, 0)
            setLimitPosition2(widgetPlotsInfo[0].limitCH1, widgetPlotsInfo[0].allCHRl, 0)
        }
        scope?.cancel()
        scope = null
        widgetPlotsInfo.clear()
        Log.d("onDestroy" , "onDestroy plot")
    }


    private fun requestThresholdsOnce() {
        val addr = ParameterInfoProvider.getDeviceAddressByDataCode(
            ParameterDataCodeEnum.PDCE_OPEN_CLOSE_THRESHOLD.number,
            parameterInfoSet
        )
        val pid = ParameterInfoProvider.getParameterIDByCode(
            ParameterDataCodeEnum.PDCE_OPEN_CLOSE_THRESHOLD.number,
            parameterInfoSet
        )

        responseReceived.set(false)
        if (RetryUtils.canSendRequestWithFirstReceiveDataFlag(addr, pid)) {
            val activeScope = scope ?: return
            RetryUtils.sendRequestWithRetry(
                request = {
                    main.bleCommandWithQueue(
                        BLECommands.requestThresholds(addr, pid),
                        MAIN_CHANNEL_CHARACTERISTIC, WRITE
                    ) {}
                },
                isResponseReceived = { responseReceived.get() },
                maxRetries = 5,
                delayMillis = 1000L,
                scope = activeScope
            )
        } else {
            setUI(
                ParameterRef(
                    addr,
                    pid,
                    ParameterDataCodeEnum.PDCE_OPEN_CLOSE_THRESHOLD.number
                )
            )
        }
    }
}




data class WidgetPlotInfo (
    var parameterInfoSet: MutableSet<ParameterInfo<Int, Int, Int, Int>> = mutableSetOf(),
    var openThreshold: Int = 0,
    var closeThreshold: Int = 0,
    var threshold3: Int = 0,
    var threshold4: Int = 0,
    var threshold5: Int = 0,
    var threshold6: Int = 0,
    var limitCH1: RelativeLayout,
    var limitCH2: RelativeLayout,
    var closeThresholdTv: TextView,
    var openThresholdTv: TextView,
    var allCHRl: LinearLayout,
    var dataSens1: Int = 0,
    var dataSens2: Int = 0,
    var dataSens3: Int = 0,
    var dataSens4: Int = 0,
    var dataSens5: Int = 0,
    var dataSens6: Int = 0,
)
