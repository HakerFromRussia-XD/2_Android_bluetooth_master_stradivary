package com.bailout.stickk.ubi4.ui.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import com.bailout.stickk.R
import com.bailout.stickk.databinding.Ubi4ItemGestureUsageLegendBinding
import com.bailout.stickk.databinding.Ubi4ViewGestureUsageChartBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.utils.ViewPortHandler
import kotlin.math.roundToInt

data class GestureUsageChartItem(
    val gestureId: Int,
    val title: String,
    val count: Long,
    @ColorInt val color: Int = GestureUsageChartView.colorForGestureId(gestureId)
)

class GestureUsageChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = Ubi4ViewGestureUsageChartBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )

    init {
        configureChart()
    }

    fun setTitle(title: CharSequence?) {
        val safeTitle = title?.toString().orEmpty()
        binding.gestureUsageTitleTv.text = safeTitle
        binding.gestureUsageTitleTv.visibility = if (safeTitle.isBlank()) View.GONE else View.VISIBLE
    }

    fun setItems(items: List<GestureUsageChartItem>) {
        val visibleItems = items.filter { it.count > 0L }

        binding.gestureUsageEmptyTv.visibility = if (visibleItems.isEmpty()) View.VISIBLE else View.GONE
        binding.gestureUsagePieChart.visibility = if (visibleItems.isEmpty()) View.GONE else View.VISIBLE
        binding.gestureUsageTotalTv.visibility = if (visibleItems.isEmpty()) View.GONE else View.VISIBLE
        binding.gestureUsageLegendLl.removeAllViews()

        if (visibleItems.isEmpty()) {
            binding.gestureUsagePieChart.clear()
            binding.gestureUsageTotalTv.text = context.getString(R.string.gesture_usage_total, 0L)
            return
        }

        val totalCount = visibleItems.sumOf { it.count }
        val entries = visibleItems.map { PieEntry(it.count.toFloat(), it.title) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = visibleItems.map { it.color }
            setDrawIcons(false)
            setDrawValues(true)
            sliceSpace = 1f
            selectionShift = 0f
            valueLineColor = Color.TRANSPARENT
            valueTextColor = Color.WHITE
            valueTextSize = 11f
            valueTypeface = Typeface.DEFAULT
            valueFormatter = PercentValueFormatter()
        }

        binding.gestureUsagePieChart.data = PieData(dataSet)
        binding.gestureUsageTotalTv.text = context.getString(R.string.gesture_usage_total, totalCount)
        binding.gestureUsagePieChart.invalidate()
        visibleItems.forEach(::addLegendRow)
    }

    private fun configureChart() {
        binding.gestureUsagePieChart.apply {
            setUsePercentValues(true)
            description.isEnabled = false
            legend.isEnabled = false
            setDrawEntryLabels(false)
            setDrawMarkers(false)
            setTouchEnabled(false)
            isDragDecelerationEnabled = false
            isHighlightPerTapEnabled = false
            isRotationEnabled = false
            setNoDataText("")
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 54f
            transparentCircleRadius = 58f
            setTransparentCircleColor(Color.TRANSPARENT)
            setTransparentCircleAlpha(0)
            setDrawCenterText(false)
            setExtraOffsets(0f, 0f, 0f, 0f)
        }
    }

    private fun addLegendRow(item: GestureUsageChartItem) {
        val rowBinding = Ubi4ItemGestureUsageLegendBinding.inflate(
            LayoutInflater.from(context),
            binding.gestureUsageLegendLl,
            false
        )

        rowBinding.gestureUsageLegendColorV.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(item.color)
        }
        rowBinding.gestureUsageLegendTitleTv.text = item.title
        rowBinding.gestureUsageLegendCountTv.text = item.count.toString()

        binding.gestureUsageLegendLl.addView(rowBinding.root)
    }

    private class PercentValueFormatter : IValueFormatter {
        override fun getFormattedValue(
            value: Float,
            entry: Entry?,
            dataSetIndex: Int,
            viewPortHandler: ViewPortHandler?
        ): String = "${value.roundToInt()}%"
    }

    companion object {
        private val DEFAULT_COLORS = intArrayOf(
            Color.rgb(198, 241, 88),
            Color.rgb(10, 132, 255),
            Color.rgb(249, 154, 67),
            Color.rgb(255, 69, 58),
            Color.rgb(75, 172, 200),
            Color.rgb(146, 174, 15),
            Color.rgb(131, 131, 131),
            Color.rgb(87, 107, 116)
        )

        @ColorInt
        fun colorForGestureId(gestureId: Int): Int {
            val index = ((gestureId % DEFAULT_COLORS.size) + DEFAULT_COLORS.size) % DEFAULT_COLORS.size
            return DEFAULT_COLORS[index]
        }
    }
}
