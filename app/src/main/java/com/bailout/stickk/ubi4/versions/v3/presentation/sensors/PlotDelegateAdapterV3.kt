package com.bailout.stickk.ubi4.versions.v3.presentation.sensors

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.view.marginTop
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4WidgetPlotBinding
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.PlotItemV3
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.DURATION_ANIMATION
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3PlotThreshold
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3PlotThresholds
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot.V3PlotAction
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot.V3PlotFrame
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot.V3PlotUiState
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.renderer.XAxisRenderer
import com.github.mikephil.charting.utils.ColorTemplate
import com.livermor.delegateadapter.delegate.ViewBindingDelegateAdapter

/** Passive Plot UI: no device/store subscriptions, timers or writes. Called on Main. */
class PlotDelegateAdapterV3(
    private val onDestroyParent: (() -> Unit) -> Unit,
    private val onAction: (V3PlotAction) -> Unit,
    private val animationsEnabled: () -> Boolean,
) : ViewBindingDelegateAdapter<PlotItemV3, Ubi4WidgetPlotBinding>(Ubi4WidgetPlotBinding::inflate) {
    private class BoundPlot(val binding: Ubi4WidgetPlotBinding) {
        var attached = false
        var gesture: V3PlotThreshold? = null
        var thresholds: V3PlotThresholds? = null
        var enabled: Boolean? = null
        var animationsEnabled: Boolean? = null
        var frameSequence: Long? = null
        var count = 0
        var firstInit = true
        val pendingPositions = mutableMapOf<RelativeLayout, Runnable>()
        val animators = mutableMapOf<RelativeLayout, ValueAnimator>()
    }

    private var state: V3PlotUiState? = null
    private var bound: BoundPlot? = null
    private var destroyCallbackRegistered = false

    @SuppressLint("ClickableViewAccessibility")
    override fun Ubi4WidgetPlotBinding.onBind(plotItem: PlotItemV3) {
        if (!destroyCallbackRegistered) {
            onDestroyParent(::onDestroy)
            destroyCallbackRegistered = true
        }
        val previous = bound?.takeIf { it.binding === this }
        bound?.let(::release)
        val row = BoundPlot(this).also {
            it.attached = previous?.attached ?: root.isAttachedToWindow
            it.count = previous?.count ?: 0
            it.firstInit = previous?.firstInit ?: true
            it.frameSequence = previous?.frameSequence
        }
        bound = row
        openCHV.setOnTouchListener { view, event -> onThresholdTouch(row, V3PlotThreshold.OPEN, view, event) }
        closeCHV.setOnTouchListener { view, event -> onThresholdTouch(row, V3PlotThreshold.CLOSE, view, event) }
        render(state)
    }

    private fun onThresholdTouch(row: BoundPlot, threshold: V3PlotThreshold, view: View, event: MotionEvent): Boolean {
        if (bound !== row || !row.attached || state?.isEnabled != true) return true
        if (event.action == MotionEvent.ACTION_DOWN) row.gesture = threshold
        if (row.gesture != threshold) return true
        view.parent.requestDisallowInterceptTouchEvent(true)
        val height = row.binding.allCHRl.height
        if (height <= 0) return true
        val y = event.y.coerceIn(0f, height.toFloat())
        val value = ((height - y) / height * 255).toInt()
        onAction(V3PlotAction.ThresholdValueChanged(threshold, value))
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                row.gesture = null
                onAction(V3PlotAction.ThresholdChangeCommitted)
            }
            // As before: a cancelled drag never sends a command.
            MotionEvent.ACTION_CANCEL -> row.gesture = null
        }
        return true
    }

    fun render(next: V3PlotUiState?) {
        state = next
        val row = bound ?: return
        val value = next ?: V3PlotUiState()
        val animate = animationsEnabled()
        if (row.thresholds != value.thresholds || row.enabled != value.isEnabled || row.animationsEnabled != animate) {
            if (!value.isEnabled) row.gesture = null
            with(row.binding) {
                openCHV.isEnabled = value.isEnabled
                openCHV.isClickable = value.isEnabled
                closeCHV.isEnabled = value.isEnabled
                closeCHV.isClickable = value.isEnabled
                openThresholdTv.text = value.thresholds.open.toString()
                closeThresholdTv.text = value.thresholds.close.toString()
                val duration = if (animate && value.animateThresholdChanges) DURATION_ANIMATION else 0L
                setThresholdPosition(row, limitCH2, value.thresholds.open, duration)
                setThresholdPosition(row, limitCH1, value.thresholds.close, duration)
            }
            row.thresholds = value.thresholds
            row.enabled = value.isEnabled
            row.animationsEnabled = animate
        }
        val frame = value.frame
        if (row.attached && !value.isPaused && frame != null && row.frameSequence != frame.sequence) {
            addFrame(row, frame, value.channelCount)
            row.frameSequence = frame.sequence
        }
    }

    override fun Ubi4WidgetPlotBinding.onAttachedToWindow() {
        val row = bound?.takeIf { it.binding === this } ?: return
        if (row.attached && EMGChartLc.data != null) return
        row.attached = true
        row.count = 0
        row.firstInit = true
        row.frameSequence = null
        row.thresholds = null
        initializedSensorGraph(EMGChartLc)
        render(state)
    }

    override fun Ubi4WidgetPlotBinding.onDetachedFromWindow() {
        bound?.takeIf { it.binding === this }?.let {
            it.attached = false
            it.gesture = null
            cancelPositions(it)
            it.thresholds = null
        }
    }

    override fun Ubi4WidgetPlotBinding.onRecycled() {
        bound?.takeIf { it.binding === this }?.let {
            release(it)
            bound = null
        }
    }

    private fun setThresholdPosition(row: BoundPlot, line: RelativeLayout, threshold: Int, duration: Long) {
        row.pendingPositions.remove(line)?.let(row.binding.allCHRl::removeCallbacks)
        row.animators.remove(line)?.cancel()
        val plotArea = row.binding.allCHRl
        val position = Runnable {
            row.pendingPositions.remove(line)
            if (bound !== row || !row.attached) return@Runnable
            val targetY = (plotArea.height - plotArea.height * threshold / 255 - line.height / 2 + plotArea.marginTop).toFloat()
            if (duration == 0L) line.y = targetY else {
                row.animators[line] = ValueAnimator.ofFloat(line.y, targetY).apply {
                    this.duration = duration
                    interpolator = AccelerateDecelerateInterpolator()
                    addUpdateListener { if (bound === row && row.attached) line.y = it.animatedValue as Float }
                    start()
                }
            }
        }
        row.pendingPositions[line] = position
        plotArea.post(position)
    }

    private fun cancelPositions(row: BoundPlot) {
        row.pendingPositions.values.forEach(row.binding.allCHRl::removeCallbacks)
        row.pendingPositions.clear()
        row.animators.values.forEach(ValueAnimator::cancel)
        row.animators.clear()
    }

    private fun release(row: BoundPlot) {
        cancelPositions(row)
        row.gesture = null
        row.binding.openCHV.setOnTouchListener(null)
        row.binding.closeCHV.setOnTouchListener(null)
    }

    fun onDestroy() {
        bound?.let(::release)
        bound = null
        state = null
        destroyCallbackRegistered = false
    }

    private fun addFrame(row: BoundPlot, frame: V3PlotFrame, numberOfCharts: Int) {
        val chart = row.binding.EMGChartLc
        val data = chart.data ?: LineData().also { chart.data = it }
        if (data.getDataSetByIndex(1) == null) {
            listOf(createSet(), createSet1(chart), createSet2(chart), createSet3(), createSet4(), createSet5(), createSet6(),
                createBoundsSet(), createBoundsSet()).forEach(data::addDataSet)
        }
        if (data.getDataSetByIndex(1).entryCount > 200) {
            data.getDataSetByIndex(0).removeFirst()
            data.getDataSetByIndex(1).removeFirst()
            for (channel in 2..numberOfCharts) data.getDataSetByIndex(channel).removeFirst()
            data.getDataSetByIndex(7).removeFirst()
            data.getDataSetByIndex(8).removeFirst()
        }
        val x = row.count.toFloat()
        data.addEntry(Entry(x, 250f), 0)
        data.addEntry(Entry(x, frame.values[0].toFloat()), 1)
        for (channel in 2..numberOfCharts) data.addEntry(Entry(x, frame.values[channel - 1].toFloat()), channel)
        data.addEntry(Entry(x, 255f), 7)
        data.addEntry(Entry(x, 0f), 8)
        data.notifyDataChanged()
        chart.notifyDataSetChanged()
        chart.moveViewToX(x - 200f)
        if (row.firstInit) {
            chart.setVisibleXRangeMaximum(200f)
            row.firstInit = false
        }
        row.count++
    }

    override fun isForViewType(item: Any): Boolean = item is PlotItemV3
    override fun PlotItemV3.getItemId(): Any = when (val w = widget) {
        is PlotParameterWidgetSStruct -> {
            val s = w.baseParameterWidgetSStruct.baseParameterWidgetStruct
            val paramsKey = s.parameterInfoSet
                .toList()
                .sortedWith(
                    compareBy<ParameterInfo<Int, Int, Int, Int>> { it.dataOffsets }
                        .thenBy { it.deviceAddress }
                        .thenBy { it.parameterID }
                        .thenBy { it.dataCode }
                )
                .joinToString("_") { p ->
                    "${p.deviceAddress}-${p.parameterID}-${p.dataCode}-${p.dataOffsets}"
                }
            "plot-${s.widgetPosition}-${paramsKey}"
        }
        is PlotParameterWidgetEStruct -> {
            val s = w.baseParameterWidgetEStruct.baseParameterWidgetStruct
            val paramsKey = s.parameterInfoSet
                .toList()
                .sortedWith(
                    compareBy<ParameterInfo<Int, Int, Int, Int>> { it.dataOffsets }
                        .thenBy { it.deviceAddress }
                        .thenBy { it.parameterID }
                        .thenBy { it.dataCode }
                )
                .joinToString("_") { p ->
                    "${p.deviceAddress}-${p.parameterID}-${p.dataCode}-${p.dataOffsets}"
                }
            "plot-${s.widgetPosition}-${paramsKey}"
        }
        else -> "plot-$title"
    }

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
}
