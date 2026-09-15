package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.view.MotionEvent
import android.view.View
import com.bailout.stickk.databinding.Ubi4Widget1ButtonBinding
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.models.widgets.ButtonsItemV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.V3ProsthesisMovement
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.buttons.V3SensorsButtonsAction
import com.bailout.stickk.ubi4.versions.v3.presentation.sensors.buttons.V3SensorsButtonsUiState
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SensorsButtonsDelegateAdapterV3Test {
    private val actions = mutableListOf<V3SensorsButtonsAction>()
    private val callbacks = mutableListOf<() -> Unit>()
    private val delegate = SensorsButtonsDelegateAdapterV3(callbacks::add, actions::add)
    private lateinit var binding: Ubi4Widget1ButtonBinding
    private var openListener: View.OnTouchListener? = null
    private var closeListener: View.OnTouchListener? = null
    private val enabled = V3SensorsButtonsUiState(setOf(V3ProsthesisMovement.OPEN, V3ProsthesisMovement.CLOSE), isEnabled = true)
    private fun item(display: Int = 1) = ButtonsItemV3("Открыть", "Закрыть", "", "", widget = CommandParameterWidgetSStruct(
        BaseParameterWidgetSStruct(BaseParameterWidgetStruct(display = display,
            parameterInfoSet = mutableSetOf(ParameterInfo(15, 1, 5, 0), ParameterInfo(15, 2, 6, 1)),
        )),
    ))

    @BeforeEach
    fun setUp() {
        val constructor = Ubi4Widget1ButtonBinding::class.java.declaredConstructors.single().apply { isAccessible = true }
        binding = constructor.newInstance(*constructor.parameterTypes.map { mockkClass(it.kotlin, relaxed = true) }.toTypedArray()) as Ubi4Widget1ButtonBinding
        every { binding.root.isAttachedToWindow } returns true
        every { binding.widget1Button.setOnTouchListener(any()) } answers { openListener = firstArg() }
        every { binding.widget2Button.setOnTouchListener(any()) } answers { closeListener = firstArg() }
        delegate.render(enabled)
        bind()
    }

    @AfterEach
    fun tearDown() = delegate.onDestroy()

    private fun bind() = delegate.javaClass.getDeclaredMethod("onBind", Ubi4Widget1ButtonBinding::class.java, ButtonsItemV3::class.java)
        .apply { isAccessible = true }.invoke(delegate, binding, item())
    private fun lifecycle(name: String) = delegate.javaClass.getDeclaredMethod(name, Ubi4Widget1ButtonBinding::class.java)
        .apply { isAccessible = true }.invoke(delegate, binding)
    private fun touch(listener: View.OnTouchListener, action: Int, button: View = binding.widget1Button) {
        val event = mockk<MotionEvent>()
        every { event.action } returns action
        listener.onTouch(button, event)
    }
    private fun pressed() = actions.filterIsInstance<V3SensorsButtonsAction.ButtonPressed>()
    private fun released() = actions.filterIsInstance<V3SensorsButtonsAction.ButtonReleased>()

    @Test
    fun `existing labels and visibility stay unchanged and calibration uses its own adapter`() {
        assertTrue(delegate.isForViewType(item()))
        assertFalse(delegate.isForViewType(item(display = 4)))
        assertFalse(delegate.isForViewType(Any()))
        verify {
            binding.widget1ButtonTv.text = "Открыть"
            binding.widget2ButtonTv.text = "Закрыть"
            binding.btn1Container.visibility = View.VISIBLE
            binding.btn2Container.visibility = View.VISIBLE
            binding.btn3Container.visibility = View.GONE
        }
        delegate.render(enabled)
        delegate.render(enabled.copy(isEnabled = false))
        delegate.render(enabled)
        assertTrue(actions.isEmpty())
    }

    @Test
    fun `open and close preserve down then up or cancel with one release per gesture`() {
        val open = requireNotNull(openListener)
        touch(open, MotionEvent.ACTION_DOWN)
        touch(open, MotionEvent.ACTION_DOWN)
        touch(open, MotionEvent.ACTION_UP)
        touch(open, MotionEvent.ACTION_UP)
        val close = requireNotNull(closeListener)
        touch(close, MotionEvent.ACTION_DOWN, binding.widget2Button)
        touch(close, MotionEvent.ACTION_CANCEL, binding.widget2Button)
        touch(close, MotionEvent.ACTION_UP, binding.widget2Button)
        assertEquals(listOf(V3ProsthesisMovement.OPEN, V3ProsthesisMovement.CLOSE), pressed().map { it.movement })
        assertEquals(pressed().map { it.pressId }, released().map { it.pressId })
        assertEquals(2, pressed().map { it.pressId }.toSet().size)
    }

    @Test
    fun `rendering a lock sends nothing and release remains delivered`() {
        val listener = requireNotNull(openListener)
        touch(listener, MotionEvent.ACTION_DOWN)
        delegate.render(enabled.copy(isEnabled = false))
        assertEquals(1, actions.size)
        touch(listener, MotionEvent.ACTION_UP)
        touch(listener, MotionEvent.ACTION_DOWN)
        assertEquals(2, actions.size)
        assertEquals(pressed().single().pressId, released().single().pressId)
        delegate.render(enabled)
        assertEquals(2, actions.size)
    }

    @Test
    fun `detach releases once and the same holder can accept a new gesture after attach`() {
        val listener = requireNotNull(openListener)
        touch(listener, MotionEvent.ACTION_DOWN)
        lifecycle("onDetachedFromWindow")
        touch(listener, MotionEvent.ACTION_UP)
        touch(listener, MotionEvent.ACTION_DOWN)
        assertEquals(2, actions.size)
        lifecycle("onAttachedToWindow")
        touch(listener, MotionEvent.ACTION_DOWN)
        touch(listener, MotionEvent.ACTION_CANCEL)
        assertEquals(4, actions.size)
        assertEquals(pressed().map { it.pressId }, released().map { it.pressId })
    }

    @Test
    fun `rebind recycle and destroy release gestures and invalidate saved listeners`() {
        val old = requireNotNull(openListener)
        touch(old, MotionEvent.ACTION_DOWN)
        bind()
        val current = requireNotNull(openListener)
        touch(current, MotionEvent.ACTION_DOWN)
        val before = actions.size
        touch(old, MotionEvent.ACTION_UP)
        assertEquals(before, actions.size)
        lifecycle("onRecycled")
        assertNull(openListener)
        assertNull(closeListener)
        touch(current, MotionEvent.ACTION_UP)
        assertEquals(2, released().size)
        bind()
        val last = requireNotNull(closeListener)
        touch(last, MotionEvent.ACTION_DOWN, binding.widget2Button)
        assertEquals(1, callbacks.size)
        delegate.onDestroy()
        delegate.onDestroy()
        touch(last, MotionEvent.ACTION_UP, binding.widget2Button)
        touch(last, MotionEvent.ACTION_DOWN, binding.widget2Button)
        assertEquals(3, released().size)
        assertEquals(pressed().map { it.pressId }, released().map { it.pressId })
    }
}
