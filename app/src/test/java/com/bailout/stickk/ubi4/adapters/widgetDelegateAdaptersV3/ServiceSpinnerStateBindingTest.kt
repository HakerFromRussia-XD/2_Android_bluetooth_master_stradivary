package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.bailout.stickk.databinding.Ubi4WidgetSpinnerBinding
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.widget.endStructures.DataSpinnerParameterWidgetStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SpinnerParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.widgets.SpinnerItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_CONTROL_MODE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_LEFT_RIGHT_HAND
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_DEVICE_ROLE
import com.bailout.stickk.ubi4.versions.v3.presentation.service.V3ServiceViewModel
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.SpinnerUiStateV3
import com.bailout.stickk.ubi4.versions.v3.presentation.spinners.V3SpinnerAction
import com.skydoves.powerspinner.DefaultSpinnerAdapter
import com.skydoves.powerspinner.OnSpinnerItemSelectedListener
import com.skydoves.powerspinner.PowerSpinnerInterface
import com.skydoves.powerspinner.PowerSpinnerView
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ServiceSpinnerStateBindingTest {
    private val context = mockk<Context>(relaxed = true)
    private val spinner = mockk<PowerSpinnerView>(relaxed = true)
    private val actions = mutableListOf<V3SpinnerAction>()
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private lateinit var delegate: SpinnerDelegateAdapterV3
    private lateinit var popup: DefaultSpinnerAdapter
    private lateinit var binding: Ubi4WidgetSpinnerBinding

    @BeforeEach
    fun setUp() {
        mockkStatic(ContextCompat::class, ResourcesCompat::class, Log::class)
        every { ContextCompat.getColor(any(), any()) } returns 0
        every { ResourcesCompat.getFont(any<Context>(), any()) } returns mockk<Typeface>()
        every { Log.d(any(), any()) } returns 0
        every { spinner.context } returns context
        every { spinner.selectedIndex } returns -1
        // Keep actual PowerSpinner selection/listener behavior; stub only Android redraw.
        mockkConstructor(DefaultSpinnerAdapter::class)
        every { anyConstructed<DefaultSpinnerAdapter>().notifyDataSetChanged() } just Runs
        every { spinner.setSpinnerAdapter(any<PowerSpinnerInterface<CharSequence>>()) } answers {
            popup = firstArg<PowerSpinnerInterface<CharSequence>>() as DefaultSpinnerAdapter
        }
        every { spinner.getSpinnerAdapter<CharSequence>() } answers { popup }
        every { spinner.setItems(any<List<CharSequence>>()) } answers { popup.setItems(firstArg()) }
        every { spinner.setOnSpinnerItemSelectedListener(any<(Int, CharSequence?, Int, CharSequence) -> Unit>()) } answers {
            val block = firstArg<(Int, CharSequence?, Int, CharSequence) -> Unit>()
            popup.onSpinnerItemSelectedListener = OnSpinnerItemSelectedListener { oldIndex, oldItem, index, item ->
                block(oldIndex, oldItem, index, item)
            }
        }
        val constructor = Ubi4WidgetSpinnerBinding::class.java.declaredConstructors.single().apply { isAccessible = true }
        binding = constructor.newInstance(*constructor.parameterTypes.map { type ->
            if (type == PowerSpinnerView::class.java) spinner else mockkClass(type.kotlin, relaxed = true)
        }.toTypedArray()) as Ubi4WidgetSpinnerBinding
        delegate = SpinnerDelegateAdapterV3({}, V3ServiceViewModel.spinnerParameterKeys + P_KEY_DEVICE_ROLE, actions::add)
    }

    @AfterEach
    fun tearDown() {
        delegate.onDestroy()
        UiState.v3WidgetsInteractionEnabled.value = originalInteraction
        unmockkConstructor(DefaultSpinnerAdapter::class)
        unmockkStatic(ContextCompat::class, ResourcesCompat::class, Log::class)
    }

    private fun bind(key: String) {
        val item = SpinnerItemV3(key, SpinnerParameterWidgetSStruct(
            BaseParameterWidgetSStruct(BaseParameterWidgetStruct(display = 4,
                parameterInfoSet = mutableSetOf(ParameterInfoRegistry.require(key)))),
            DataSpinnerParameterWidgetStruct(listOf("First", "Second"), 0)))
        delegate.javaClass.getDeclaredMethod("onBind", Ubi4WidgetSpinnerBinding::class.java, SpinnerItemV3::class.java)
            .apply { isAccessible = true }.invoke(delegate, binding, item)
    }

    @Test
    fun `role selection and PIN cancellation are rendered from state without preference reads or direct writes`() {
        val key = P_KEY_DEVICE_ROLE
        delegate.renderSpinners(mapOf(key to SpinnerUiStateV3(1, true)))
        bind(key)
        assertEquals(1, popup.index)
        assertTrue(actions.isEmpty())
        popup.notifyItemSelected(0)
        assertEquals(listOf(V3SpinnerAction.SpinnerValueSelected(key, 0)), actions)
        delegate.renderSpinners(mapOf(key to SpinnerUiStateV3(0, true)))
        delegate.renderSpinners(mapOf(key to SpinnerUiStateV3(1, true)))
        assertEquals(1, popup.index)
        assertEquals(1, actions.size)
        verify(exactly = 0) { context.getSharedPreferences(any(), any()) }
        listOf("collectJob", "interactionJob").forEach { name ->
            assertNull(delegate.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(delegate))
        }
    }

    @Test
    fun `role without screen ownership never falls back to direct device writes`() {
        delegate = SpinnerDelegateAdapterV3({}, onAction = actions::add)
        bind(P_KEY_DEVICE_ROLE)
        verify(exactly = 0) { spinner.setSpinnerAdapter(any<PowerSpinnerInterface<CharSequence>>()) }
        verify(exactly = 0) { context.getSharedPreferences(any(), any()) }
        assertTrue(actions.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `both Service keys render silently and user selection delivers one screen action`(handSide: Boolean) {
        val key = if (handSide) P_KEY_LEFT_RIGHT_HAND else P_KEY_EMG_CONTROL_MODE
        UiState.v3WidgetsInteractionEnabled.value = false
        delegate.renderSpinners(mapOf(key to SpinnerUiStateV3(1, true)))
        bind(key)
        assertEquals(1, popup.index)
        verify { spinner.isEnabled = true }
        listOf("collectJob", "interactionJob").forEach { name ->
            assertNull(delegate.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(delegate))
        }
        assertTrue(actions.isEmpty())
        delegate.renderSpinners(mapOf(key to SpinnerUiStateV3(0, true)))
        assertEquals(0, popup.index)
        assertTrue(actions.isEmpty())
        popup.notifyItemSelected(1)
        assertEquals(listOf(V3SpinnerAction.SpinnerValueSelected(key, 1)), actions)
        delegate.renderSpinners(mapOf(key to SpinnerUiStateV3(1, false)))
        popup.notifyItemSelected(0)
        assertEquals(1, actions.size)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `rebind recycle and destroy reject old callbacks for either Service key`(handSide: Boolean) {
        val key = if (handSide) P_KEY_LEFT_RIGHT_HAND else P_KEY_EMG_CONTROL_MODE
        delegate.renderSpinners(mapOf(key to SpinnerUiStateV3(1, true)))
        bind(key)
        val old = requireNotNull(popup.onSpinnerItemSelectedListener)
        bind(key)
        old.onItemSelected(0, "First", 1, "Second")
        val current = popup
        delegate.javaClass.getDeclaredMethod("onRecycled", Ubi4WidgetSpinnerBinding::class.java)
            .apply { isAccessible = true }.invoke(delegate, binding)
        assertNull(current.onSpinnerItemSelectedListener)
        bind(key)
        val beforeDestroy = requireNotNull(popup.onSpinnerItemSelectedListener)
        delegate.onDestroy()
        beforeDestroy.onItemSelected(0, "First", 1, "Second")
        assertTrue(actions.isEmpty())
    }
}
