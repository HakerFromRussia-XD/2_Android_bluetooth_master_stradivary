package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.SeekBar
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatSeekBar
import com.bailout.stickk.databinding.Ubi4WidgetSliderBinding
import com.bailout.stickk.ubi4.models.widgets.SliderItemV3
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.SliderUiStateV3
import com.bailout.stickk.ubi4.versions.v3.presentation.sliders.V3SliderAction
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.*
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SliderDelegateAdapterV3Test {
    private val seekBar = mockk<AppCompatSeekBar>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val drawable = mockk<Drawable>(relaxed = true)
    private var listener: SeekBar.OnSeekBarChangeListener? = null
    private val actions = mutableListOf<V3SliderAction>()
    private val delegate = SliderDelegateAdapterV3({}, actions::add) { false }
    private lateinit var binding: Ubi4WidgetSliderBinding
    private val state = SliderUiStateV3(P_KEY_SPEED_SETTINGS, 59, 0..100, true)
    private val item = V3SpecialSettingsWidgetMapper().toItems(listOf(
        V3SpecialSettingsWidget.Slider(V3SpecialSettingsWidgetInfo(P_KEY_SPEED_SETTINGS, "Speed", 1), 0, 100, 1f),
    )).single() as SliderItemV3

    @BeforeEach
    fun setUp() {
        mockkStatic(AppCompatResources::class)
        every { AppCompatResources.getDrawable(any(), any()) } returns drawable
        every { drawable.mutate() } returns drawable
        every { seekBar.context } returns context
        every { seekBar.setOnSeekBarChangeListener(any()) } answers { listener = firstArg() }
        val constructor = Ubi4WidgetSliderBinding::class.java.declaredConstructors.single().apply { isAccessible = true }
        binding = constructor.newInstance(*constructor.parameterTypes.map { type ->
            if (SeekBar::class.java.isAssignableFrom(type)) seekBar else mockkClass(type.kotlin, relaxed = true)
        }.toTypedArray()) as Ubi4WidgetSliderBinding
    }

    @AfterEach
    fun tearDown() {
        delegate.onDestroy()
        unmockkStatic(AppCompatResources::class)
    }

    private fun bind() {
        delegate.javaClass.getDeclaredMethod("onBind", Ubi4WidgetSliderBinding::class.java, SliderItemV3::class.java)
            .apply { isAccessible = true }.invoke(delegate, binding, item)
    }

    @Test
    fun `render is silent and recycled or replaced listeners cannot commit a value`() {
        delegate.renderSliders(mapOf(P_KEY_SPEED_SETTINGS to state))
        bind()
        val old = requireNotNull(listener)
        old.onProgressChanged(seekBar, 60, false)
        assertTrue(actions.isEmpty())
        bind()
        old.onStopTrackingTouch(seekBar)
        assertTrue(actions.isEmpty())
        val current = requireNotNull(listener)
        current.onProgressChanged(seekBar, 61, true)
        assertEquals(listOf(V3SliderAction.SliderValueChanged(P_KEY_SPEED_SETTINGS, 61)), actions)
        delegate.javaClass.getDeclaredMethod("onRecycled", Ubi4WidgetSliderBinding::class.java)
            .apply { isAccessible = true }.invoke(delegate, binding)
        assertNull(listener)
        current.onStopTrackingTouch(seekBar)
        assertEquals(1, actions.size)
    }

    @Test
    fun `disabled and destroyed rows reject delayed callbacks`() {
        delegate.renderSliders(mapOf(P_KEY_SPEED_SETTINGS to state))
        bind()
        val current = requireNotNull(listener)
        delegate.renderSliders(mapOf(P_KEY_SPEED_SETTINGS to state.copy(isEnabled = false)))
        current.onStopTrackingTouch(seekBar)
        assertTrue(actions.isEmpty())
        delegate.onDestroy()
        current.onProgressChanged(seekBar, 80, true)
        assertTrue(actions.isEmpty())
        assertNull(listener)
    }
}
