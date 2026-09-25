package com.bailout.stickk.ubi4.versions.v3.presentation.togglesliders

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatSeekBar
import com.bailout.stickk.databinding.Ubi4WidgetToggleSliderBinding
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.widget.endStructures.ToggleSliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.widgets.ToggleSliderItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SCREEN_TIMEOUT
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderValue
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ToggleSliderDelegateAdapterV3Test {
    private val key = P_KEY_SCREEN_TIMEOUT
    private val seekBar = mockk<AppCompatSeekBar>(relaxed = true)
    private var listener: SeekBar.OnSeekBarChangeListener? = null
    private var toggleClick: View.OnClickListener? = null
    private val actions = mutableListOf<V3ToggleSliderAction>()
    private val delegate = ToggleSliderDelegateAdapterV3({}, setOf(key), actions::add) { false }
    private lateinit var binding: Ubi4WidgetToggleSliderBinding
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val state = ToggleSliderUiStateV3(V3ToggleSliderValue(timeTenths = 30, isEnabled = true), 1..127, true)
    private val item = ToggleSliderItemV3("Screen timeout", ToggleSliderParameterWidgetSStruct(
        baseParameterWidgetSStruct = BaseParameterWidgetSStruct(BaseParameterWidgetStruct(
            display = 2, parameterInfoSet = mutableSetOf(ParameterInfoRegistry.require(key)),
        )), minProgress = 1, maxProgress = 127, increment = 0.1f, unitLabel = "s",
    ))

    @BeforeEach
    fun setUp() {
        mockkStatic(AppCompatResources::class)
        val drawable = mockk<Drawable>(relaxed = true)
        every { AppCompatResources.getDrawable(any(), any()) } returns drawable
        every { drawable.mutate() } returns drawable
        every { seekBar.setOnSeekBarChangeListener(any()) } answers { listener = firstArg() }
        every { seekBar.progress } returns 39
        val constructor = Ubi4WidgetToggleSliderBinding::class.java.declaredConstructors.single().apply { isAccessible = true }
        binding = constructor.newInstance(*constructor.parameterTypes.map { type ->
            if (SeekBar::class.java.isAssignableFrom(type)) seekBar else mockkClass(type.kotlin, relaxed = true)
        }.toTypedArray()) as Ubi4WidgetToggleSliderBinding
        every { binding.toggleTurnOffRipple1Btn.setOnClickListener(any()) } answers { toggleClick = firstArg() }
    }

    @AfterEach
    fun tearDown() {
        delegate.onDestroy()
        UiState.v3WidgetsInteractionEnabled.value = originalInteraction
        unmockkStatic(AppCompatResources::class)
    }

    private fun bind() = delegate.javaClass.getDeclaredMethod(
        "onBind", Ubi4WidgetToggleSliderBinding::class.java, ToggleSliderItemV3::class.java,
    ).apply { isAccessible = true }.invoke(delegate, binding, item)

    @Test
    fun `screen state controls rendering and user events go only to the screen`() {
        UiState.v3WidgetsInteractionEnabled.value = false
        delegate.renderToggleSliders(mapOf(key to state))
        bind()
        verify { seekBar.isEnabled = true; seekBar.progress = 29 }
        requireNotNull(listener).onProgressChanged(seekBar, 29, false)
        assertTrue(actions.isEmpty())
        requireNotNull(listener).onProgressChanged(seekBar, 39, true)
        requireNotNull(listener).onStopTrackingTouch(seekBar)
        requireNotNull(toggleClick).onClick(binding.toggleTurnOffRipple1Btn)
        assertEquals(listOf(
            V3ToggleSliderAction.ToggleSliderValueChanged(key, 40),
            V3ToggleSliderAction.ToggleSliderChangeCommitted(key, 40),
            V3ToggleSliderAction.ToggleSliderEnabledChanged(key, false),
        ), actions)
        delegate.renderToggleSliders(mapOf(key to state.copy(isInteractionEnabled = false)))
        verify { seekBar.isEnabled = false; binding.toggleTurnOffRipple1Btn.isClickable = false }
        assertEquals(3, actions.size)
    }

    @Test
    fun `rebind recycle and destroy invalidate callbacks without another write path`() {
        delegate.renderToggleSliders(mapOf(key to state)); bind()
        val old = requireNotNull(listener)
        bind()
        old.onStopTrackingTouch(seekBar)
        val current = requireNotNull(listener)
        delegate.javaClass.getDeclaredMethod("onRecycled", Ubi4WidgetToggleSliderBinding::class.java)
            .apply { isAccessible = true }.invoke(delegate, binding)
        current.onStopTrackingTouch(seekBar)
        assertNull(listener)
        bind()
        val last = requireNotNull(listener)
        val click = requireNotNull(toggleClick)
        delegate.onDestroy()
        last.onStopTrackingTouch(seekBar)
        click.onClick(binding.toggleTurnOffRipple1Btn)
        assertTrue(actions.isEmpty())
        assertNull(listener)
        assertNull(toggleClick)
    }
}
