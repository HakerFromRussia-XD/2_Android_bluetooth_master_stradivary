package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import android.widget.CompoundButton
import android.widget.Switch
import com.bailout.stickk.databinding.Ubi4WidgetSwitcherBinding
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.MobileSettingsKey
import com.bailout.stickk.ubi4.versions.v3.presentation.autologin.V3AutoLoginUiState
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidget
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets.V3SpecialSettingsWidgetInfo
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutoLoginDelegateAdapterV3Test {
    private val button = mockk<Switch>(relaxed = true)
    private var checked = false
    private var listener: CompoundButton.OnCheckedChangeListener? = null
    private val actions = mutableListOf<Boolean>()
    private val destroyCallbacks = mutableListOf<() -> Unit>()
    private val adapter = AutoLoginDelegateAdapterV3(actions::add, destroyCallbacks::add) { false }
    private lateinit var binding: Ubi4WidgetSwitcherBinding
    private val item = V3SpecialSettingsWidget.Switch(
        V3SpecialSettingsWidgetInfo(MobileSettingsKey.AUTO_LOGIN.key, "Auto login", 0), false,
    )

    @BeforeEach
    fun setUp() {
        every { button.setOnCheckedChangeListener(any()) } answers { listener = firstArg() }
        every { button.isChecked } answers { checked }
        every { button.isChecked = any() } answers {
            val next = firstArg<Boolean>()
            if (checked != next) {
                checked = next
                listener?.onCheckedChanged(button, next)
            }
        }
        // Use the generated binding with mocked Android views; do not inflate a framework layout on the JVM.
        val constructor = Ubi4WidgetSwitcherBinding::class.java.declaredConstructors.single()
        constructor.isAccessible = true
        binding = constructor.newInstance(*constructor.parameterTypes.map { type ->
            if (type == Switch::class.java) button else mockkClass(type.kotlin, relaxed = true)
        }.toTypedArray()) as Ubi4WidgetSwitcherBinding
    }

    private fun bind() {
        adapter.javaClass.getDeclaredMethod("onBind", Ubi4WidgetSwitcherBinding::class.java, V3SpecialSettingsWidget.Switch::class.java)
            .apply { isAccessible = true }.invoke(adapter, binding, item)
    }

    @Test
    fun `render and rebind never emit changes while user input emits once`() {
        adapter.render(V3AutoLoginUiState(isChecked = true, isEnabled = true, isLoading = false))
        bind()
        assertTrue(checked)
        assertTrue(actions.isEmpty())
        button.isChecked = false
        assertEquals(listOf(false), actions)
        adapter.render(V3AutoLoginUiState(isChecked = true, isEnabled = false, isLoading = false))
        button.isChecked = false
        assertTrue(checked)
        assertEquals(listOf(false), actions)
        bind()
        assertEquals(listOf(false), actions)
    }

    @Test
    fun `recycled and destroyed bindings cannot send stale callbacks`() {
        adapter.render(V3AutoLoginUiState(isEnabled = true, isLoading = false))
        bind()
        val oldListener = requireNotNull(listener)
        adapter.javaClass.getDeclaredMethod("onRecycled", Ubi4WidgetSwitcherBinding::class.java)
            .apply { isAccessible = true }.invoke(adapter, binding)
        bind()
        oldListener.onCheckedChanged(button, true)
        assertTrue(actions.isEmpty())
        val currentListener = requireNotNull(listener)
        destroyCallbacks.toList().forEach { it() }
        currentListener.onCheckedChanged(button, true)
        assertTrue(actions.isEmpty())
        assertNull(listener)
    }
}
