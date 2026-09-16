package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.view.MotionEvent
import android.view.View
import com.bailout.stickk.databinding.Ubi4Widget1ButtonBinding
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.*
import com.bailout.stickk.ubi4.models.widgets.ButtonsItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_START_CALIBRATE_COMMAND
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_DEVICE_NAME
import com.bailout.stickk.ubi4.versions.v3.presentation.service.V3ProsthesisCalibrationUiState
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class ProsthesisCalibrationDelegateAdapterV3Test {
    private val presses = mutableListOf<Long>()
    private val releases = mutableListOf<Long>()
    private val destroyCallbacks = mutableListOf<() -> Unit>()
    private val delegate = ProsthesisCalibrationDelegateAdapterV3(destroyCallbacks::add, presses::add, releases::add)
    private lateinit var binding: Ubi4Widget1ButtonBinding
    private var listener: View.OnTouchListener? = null
    private val enabled = V3ProsthesisCalibrationUiState(isEnabled = true)
    private fun item(display: Int = 4, key: String = P_KEY_START_CALIBRATE_COMMAND) = ButtonsItemV3(
        "Калибровка протеза", "", "", "", CommandParameterWidgetSStruct(
            BaseParameterWidgetSStruct(BaseParameterWidgetStruct(display = display, widgetPosition = 7,
                parameterInfoSet = mutableSetOf(ParameterInfoRegistry.require(key))))))

    @BeforeEach fun setUp() {
        val constructor = Ubi4Widget1ButtonBinding::class.java.declaredConstructors.single().apply { isAccessible = true }
        binding = constructor.newInstance(*constructor.parameterTypes.map { mockkClass(it.kotlin, relaxed = true) }.toTypedArray()) as Ubi4Widget1ButtonBinding
        every { binding.root.isAttachedToWindow } returns true
        every { binding.widget1Button.setOnTouchListener(any()) } answers { listener = firstArg() }
        delegate.render(enabled); bind()
    }
    @AfterEach fun tearDown() = delegate.onDestroy()
    private fun bind() = delegate.javaClass.getDeclaredMethod("onBind", Ubi4Widget1ButtonBinding::class.java, ButtonsItemV3::class.java)
        .apply { isAccessible = true }.invoke(delegate, binding, item())
    private fun lifecycle(name: String) = delegate.javaClass.getDeclaredMethod(name, Ubi4Widget1ButtonBinding::class.java)
        .apply { isAccessible = true }.invoke(delegate, binding)
    private fun touch(action: Int, saved: View.OnTouchListener = requireNotNull(listener)) {
        val event = mockk<MotionEvent> { every { this@mockk.action } returns action }
        saved.onTouch(binding.widget1Button, event)
    }

    @Test fun `existing label layout and stable id remain and routing excludes Sensors and unknown commands`() {
        assertTrue(delegate.isForViewType(item()))
        assertFalse(delegate.isForViewType(item(display = 1)))
        assertFalse(delegate.isForViewType(item(key = P_KEY_SET_DEVICE_NAME)))
        assertFalse(delegate.isForViewType(Any()))
        val id = delegate.javaClass.getDeclaredMethod("getItemId", ButtonsItemV3::class.java)
            .apply { isAccessible = true }.invoke(delegate, item())
        assertEquals("buttons-7-1-15-3-0", id)
        verify {
            binding.widget1ButtonTv.text = "Калибровка протеза"
            binding.btn1Container.visibility = View.VISIBLE
            binding.btn2Container.visibility = View.GONE
            binding.btn3Container.visibility = View.GONE
        }
        delegate.render(enabled); delegate.render(null); delegate.render(enabled)
        assertTrue(presses.isEmpty()); assertTrue(releases.isEmpty())
    }

    @Test fun `down then up or cancel produces exactly one matching release`() {
        touch(MotionEvent.ACTION_UP)
        touch(MotionEvent.ACTION_DOWN); touch(MotionEvent.ACTION_DOWN)
        touch(MotionEvent.ACTION_UP); touch(MotionEvent.ACTION_CANCEL)
        touch(MotionEvent.ACTION_DOWN); touch(MotionEvent.ACTION_CANCEL); touch(MotionEvent.ACTION_UP)
        assertEquals(2, presses.size)
        assertEquals(presses, releases)
        assertEquals(2, presses.toSet().size)
    }

    @Test fun `rendered lock is silent and does not lose release of the accepted touch`() {
        touch(MotionEvent.ACTION_DOWN)
        delegate.render(enabled.copy(isPressed = true))
        verify { binding.widget1Button.isPressed = true }
        delegate.render(enabled.copy(isEnabled = false))
        assertTrue(releases.isEmpty())
        touch(MotionEvent.ACTION_UP); touch(MotionEvent.ACTION_DOWN)
        assertEquals(1, presses.size); assertEquals(presses, releases)
        verify { binding.widget1Button.isEnabled = false; binding.widget1Button.isPressed = false }
    }

    @Test fun `detached holder releases once and cannot press until attached again`() {
        touch(MotionEvent.ACTION_DOWN)
        lifecycle("onDetachedFromWindow")
        touch(MotionEvent.ACTION_UP); touch(MotionEvent.ACTION_DOWN)
        assertEquals(1, presses.size); assertEquals(presses, releases)
        lifecycle("onAttachedToWindow")
        touch(MotionEvent.ACTION_DOWN); touch(MotionEvent.ACTION_UP)
        assertEquals(2, presses.size); assertEquals(presses, releases)
    }

    @Test fun `rebind recycle and destroy release presses and invalidate old listeners`() {
        val old = requireNotNull(listener)
        touch(MotionEvent.ACTION_DOWN); bind()
        touch(MotionEvent.ACTION_DOWN, old); touch(MotionEvent.ACTION_UP, old)
        assertEquals(1, presses.size); assertEquals(presses, releases)
        val current = requireNotNull(listener)
        touch(MotionEvent.ACTION_DOWN); lifecycle("onRecycled")
        assertNull(listener)
        touch(MotionEvent.ACTION_DOWN, current); touch(MotionEvent.ACTION_UP, current)
        bind(); touch(MotionEvent.ACTION_DOWN)
        val last = requireNotNull(listener)
        assertEquals(1, destroyCallbacks.size)
        delegate.onDestroy(); delegate.onDestroy()
        touch(MotionEvent.ACTION_UP, last); touch(MotionEvent.ACTION_DOWN, last)
        assertEquals(3, presses.size); assertEquals(presses, releases)
    }
}
