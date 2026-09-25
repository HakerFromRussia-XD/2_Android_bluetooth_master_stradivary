package com.bailout.stickk.ubi4.versions.v3.presentation.service

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat
import com.bailout.stickk.databinding.Ubi4WidgetTextInputBinding
import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.*
import com.bailout.stickk.ubi4.models.widgets.TextInputItemV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_DEVICE_NAME
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoField
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class ServiceTextInputStateBindingTest {
    private val input = mockk<EditText>(relaxed = true)
    private var text = ""
    private var watcher: TextWatcher? = null
    private var click: View.OnClickListener? = null
    private var touch: View.OnTouchListener? = null
    private var send: View.OnClickListener? = null
    private lateinit var destroy: () -> Unit
    private lateinit var binding: Ubi4WidgetTextInputBinding
    private lateinit var adapter: TextInputDelegateAdapterV3
    private val edits = mutableListOf<String>()
    private var prefills = 0
    private var sends = 0
    private val field = V3DeviceInfoField.DEVICE_NAME
    private var state = V3ServiceTextInputUiState("Initial", true)
    private fun render() = adapter.renderTextInputs(mapOf(field to state))
    private fun editable(value: String): Editable {
        val result = mockk<Editable>()
        every { result.toString() } returns value
        return result
    }

    @BeforeEach fun setUp() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.getColor(any(), any()) } returns 0
        val constructor = Ubi4WidgetTextInputBinding::class.java.declaredConstructors.single().apply { isAccessible = true }
        binding = constructor.newInstance(*constructor.parameterTypes.map { type ->
            if (EditText::class.java.isAssignableFrom(type)) input else mockkClass(type.kotlin, relaxed = true)
        }.toTypedArray()) as Ubi4WidgetTextInputBinding
        every { binding.root.context } returns mockk<Context>(relaxed = true)
        every { input.text } answers { editable(text) }
        every { input.addTextChangedListener(any()) } answers { watcher = firstArg() }
        every { input.removeTextChangedListener(any()) } answers { if (watcher === firstArg<TextWatcher>()) watcher = null }
        every { input.setText(any<CharSequence>()) } answers {
            text = firstArg<CharSequence>().toString()
            watcher?.afterTextChanged(editable(text))
        }
        every { input.setOnClickListener(any()) } answers { click = firstArg() }
        every { input.setOnTouchListener(any()) } answers { touch = firstArg() }
        every { binding.sendBtnOverlay.setOnClickListener(any()) } answers { send = firstArg() }
        adapter = TextInputDelegateAdapterV3({ destroy = it },
            onTextChanged = { f, value -> assertEquals(field, f); edits += value },
            onPrefillRequested = { prefills++ }, onSendClicked = { sends++ })
        render(); bind()
    }
    @AfterEach fun tearDown() { destroy(); unmockkStatic(ContextCompat::class) }

    private fun bind() {
        val item = TextInputItemV3("Name", "Send", CommandParameterWidgetSStruct(
            BaseParameterWidgetSStruct(BaseParameterWidgetStruct(display = 4,
                parameterInfoSet = mutableSetOf(ParameterInfoRegistry.require(P_KEY_SET_DEVICE_NAME))))))
        adapter.javaClass.getDeclaredMethod("onBind", Ubi4WidgetTextInputBinding::class.java, TextInputItemV3::class.java)
            .apply { isAccessible = true }.invoke(adapter, binding, item)
    }

    @Test fun `programmatic rendering is silent and availability changes preserve cursor`() {
        assertEquals("Initial", text)
        assertTrue(edits.isEmpty())
        clearMocks(input, answers = false)
        state = state.copy(canSend = false); render()
        verify(exactly = 0) { input.setText(any<CharSequence>()); input.setSelection(any<Int>()) }
        verify(exactly = 0) { input.isEnabled = any() }
        text = "Typed"; watcher?.afterTextChanged(editable(text))
        assertEquals(listOf("Typed"), edits)
        state = state.copy(text = "Short", cursorRevision = 1); render()
        assertEquals("Short", text)
        verify { input.setSelection(5) }
        assertEquals(1, edits.size)
        assertEquals(0, sends)
    }

    @Test fun `touch and click request current value and send is gated by screen state`() {
        val event = mockk<MotionEvent> { every { actionMasked } returns MotionEvent.ACTION_UP }
        assertFalse(requireNotNull(touch).onTouch(input, event))
        click?.onClick(input)
        assertEquals(2, prefills)
        send?.onClick(binding.sendBtnOverlay)
        assertEquals(1, sends)
        state = state.copy(canSend = false); render()
        send?.onClick(binding.sendBtnOverlay)
        assertEquals(1, sends)
        assertTrue(edits.isEmpty())
    }

    @Test fun `rebind recycle and destroy discard old callbacks`() {
        val oldWatcher = requireNotNull(watcher)
        val oldClick = requireNotNull(click)
        val oldSend = requireNotNull(send)
        bind()
        oldWatcher.afterTextChanged(editable("Stale")); oldClick.onClick(input); oldSend.onClick(binding.sendBtnOverlay)
        adapter.javaClass.getDeclaredMethod("onRecycled", Ubi4WidgetTextInputBinding::class.java)
            .apply { isAccessible = true }.invoke(adapter, binding)
        assertNull(watcher); assertNull(click); assertNull(send); assertNull(touch)
        bind()
        val lastSend = requireNotNull(send)
        destroy(); lastSend.onClick(binding.sendBtnOverlay)
        assertTrue(edits.isEmpty()); assertEquals(0, prefills); assertEquals(0, sends)
    }
}
