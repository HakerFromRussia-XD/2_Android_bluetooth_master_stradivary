package com.bailout.stickk.ubi4.versions.v3.presentation.sensors

import android.view.MotionEvent
import android.view.View
import android.graphics.Color
import com.github.mikephil.charting.utils.Utils
import com.bailout.stickk.databinding.Ubi4WidgetPlotBinding
import com.bailout.stickk.ubi4.models.widgets.PlotItemV3
import com.bailout.stickk.ubi4.data.widget.endStructures.PlotParameterWidgetSStruct
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3PlotThreshold
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3PlotThresholds
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot.V3PlotAction
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot.V3PlotFrame
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot.V3PlotUiState
import com.github.mikephil.charting.data.LineData
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PlotDelegateAdapterV3Test {
    private val actions = mutableListOf<V3PlotAction>()
    private val callbacks = mutableListOf<() -> Unit>()
    private val delegate = PlotDelegateAdapterV3(callbacks::add, actions::add) { false }
    private lateinit var binding: Ubi4WidgetPlotBinding
    private var openListener: View.OnTouchListener? = null
    private var closeListener: View.OnTouchListener? = null
    private val enabled = V3PlotUiState(V3PlotThresholds(158, 109), isEnabled = true)
    private val item = PlotItemV3("Plot", PlotParameterWidgetSStruct())

    @BeforeEach
    fun setUp() {
        mockkStatic(Color::class, Utils::class)
        every { Color.rgb(any<Int>(), any<Int>(), any<Int>()) } answers {
            (255 shl 24) or (firstArg<Int>() shl 16) or (secondArg<Int>() shl 8) or thirdArg<Int>()
        }
        every { Color.parseColor(any()) } returns 0
        Class.forName(Utils::class.java.name, true, Utils::class.java.classLoader)
        every { Utils.convertDpToPixel(any()) } answers { firstArg() }
        val constructor = Ubi4WidgetPlotBinding::class.java.declaredConstructors.single().apply { isAccessible = true }
        binding = constructor.newInstance(*constructor.parameterTypes.map { mockkClass(it.kotlin, relaxed = true) }.toTypedArray()) as Ubi4WidgetPlotBinding
        every { binding.root.isAttachedToWindow } returns true
        every { binding.allCHRl.height } returns 255
        every { binding.openCHV.setOnTouchListener(any()) } answers { openListener = firstArg() }
        every { binding.closeCHV.setOnTouchListener(any()) } answers { closeListener = firstArg() }
        every { binding.EMGChartLc.data } returns LineData()
        delegate.render(enabled)
        bind()
    }

    @AfterEach
    fun tearDown() {
        delegate.onDestroy()
        unmockkStatic(Color::class, Utils::class)
    }

    private fun bind() = delegate.javaClass.getDeclaredMethod("onBind", Ubi4WidgetPlotBinding::class.java, PlotItemV3::class.java)
        .apply { isAccessible = true }.invoke(delegate, binding, item)
    private fun lifecycle(name: String) = delegate.javaClass.getDeclaredMethod(name, Ubi4WidgetPlotBinding::class.java)
        .apply { isAccessible = true }.invoke(delegate, binding)
    private fun touch(listener: View.OnTouchListener, action: Int, y: Float = 55f) {
        val event = mockk<MotionEvent>()
        every { event.action } returns action
        every { event.y } returns y
        listener.onTouch(binding.openCHV, event)
    }

    @Test
    fun `render does not dispatch actions and only release commits`() {
        delegate.render(enabled.copy(thresholds = V3PlotThresholds(170, 120)))
        assertTrue(actions.isEmpty())
        val listener = requireNotNull(openListener)
        touch(listener, MotionEvent.ACTION_DOWN)
        touch(listener, MotionEvent.ACTION_MOVE, 5f)
        assertEquals(listOf(
            V3PlotAction.ThresholdValueChanged(V3PlotThreshold.OPEN, 200),
            V3PlotAction.ThresholdValueChanged(V3PlotThreshold.OPEN, 250),
        ), actions)
        touch(listener, MotionEvent.ACTION_UP, 5f)
        assertEquals(V3PlotAction.ThresholdChangeCommitted, actions.last())
        assertEquals(1, actions.count { it == V3PlotAction.ThresholdChangeCommitted })
        touch(listener, MotionEvent.ACTION_UP)
        assertEquals(1, actions.count { it == V3PlotAction.ThresholdChangeCommitted })
    }

    @Test
    fun `cancel and disabled or detached gestures never commit`() {
        val listener = requireNotNull(closeListener)
        touch(listener, MotionEvent.ACTION_DOWN)
        touch(listener, MotionEvent.ACTION_CANCEL)
        touch(listener, MotionEvent.ACTION_UP)
        assertTrue(actions.none { it == V3PlotAction.ThresholdChangeCommitted })
        touch(listener, MotionEvent.ACTION_DOWN)
        delegate.render(enabled.copy(isEnabled = false))
        val before = actions.size
        touch(listener, MotionEvent.ACTION_UP)
        assertEquals(before, actions.size)
        delegate.render(enabled)
        touch(listener, MotionEvent.ACTION_UP)
        assertEquals(before, actions.size)
        touch(listener, MotionEvent.ACTION_DOWN)
        lifecycle("onDetachedFromWindow")
        val beforeDetach = actions.size
        touch(listener, MotionEvent.ACTION_UP)
        assertEquals(beforeDetach, actions.size)
    }

    @Test
    fun `rebound recycled and destroyed listeners cannot use a new row`() {
        val old = requireNotNull(openListener)
        touch(old, MotionEvent.ACTION_DOWN)
        bind()
        val before = actions.size
        touch(old, MotionEvent.ACTION_UP)
        assertEquals(before, actions.size)
        val current = requireNotNull(openListener)
        touch(current, MotionEvent.ACTION_DOWN)
        lifecycle("onRecycled")
        assertNull(openListener)
        assertNull(closeListener)
        val beforeRecycle = actions.size
        touch(current, MotionEvent.ACTION_UP)
        assertEquals(beforeRecycle, actions.size)
        bind()
        assertEquals(1, callbacks.size)
        val last = requireNotNull(openListener)
        delegate.onDestroy()
        touch(last, MotionEvent.ACTION_DOWN)
        touch(last, MotionEvent.ACTION_UP)
        assertEquals(beforeRecycle, actions.size)
    }

    @Test
    fun `frame identity prevents duplicate insertion and graph retains existing window and bounds`() {
        val data = LineData()
        every { binding.EMGChartLc.data } returns data
        for (sequence in 1L..205L) {
            val frameState = enabled.copy(frame = V3PlotFrame(sequence, listOf(80, 90, 100, 110, 120, 130)))
            delegate.render(frameState)
            delegate.render(frameState.copy(thresholds = V3PlotThresholds(170, 120)))
        }
        assertEquals(9, data.dataSetCount)
        assertEquals(201, data.getDataSetByIndex(1).entryCount)
        assertEquals(201, data.getDataSetByIndex(2).entryCount)
        assertEquals(0, data.getDataSetByIndex(3).entryCount)
        assertEquals(80f, data.getDataSetByIndex(1).getEntryForIndex(200).y)
        assertEquals(255f, data.getDataSetByIndex(7).getEntryForIndex(200).y)
        assertEquals(0f, data.getDataSetByIndex(8).getEntryForIndex(200).y)
        delegate.render(enabled.copy(isPaused = true, frame = V3PlotFrame(206, List(6) { 0 })))
        assertEquals(204f, data.getDataSetByIndex(1).getEntryForIndex(200).x)
        assertTrue(actions.isEmpty())
    }
}
