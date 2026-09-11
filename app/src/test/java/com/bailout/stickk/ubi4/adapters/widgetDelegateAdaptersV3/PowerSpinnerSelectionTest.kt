package com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3

import com.skydoves.powerspinner.DefaultSpinnerAdapter
import com.skydoves.powerspinner.OnSpinnerItemSelectedListener
import com.skydoves.powerspinner.PowerSpinnerView
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PowerSpinnerSelectionTest {
    private val view = mockk<PowerSpinnerView>(relaxed = true).also {
        every { it.selectedIndex } returns -1
    }
    private val adapter = spyk(DefaultSpinnerAdapter(view)).apply {
        // Android Observable is a stub on the JVM; keep the library's selection logic real.
        every { notifyDataSetChanged() } just Runs
        setItems(listOf("Normal", "Sport", "Smooth"))
    }

    @Test
    fun `real library adapter renders silently and still delivers the next user selection`() {
        val selections = mutableListOf<Int>()
        adapter.onSpinnerItemSelectedListener = OnSpinnerItemSelectedListener { _, _, index, _ -> selections.add(index) }
        adapter.selectItemWithoutCallback(1)
        adapter.selectItemWithoutCallback(2)
        adapter.selectItemWithoutCallback(2)
        assertEquals(emptyList<Int>(), selections)
        assertEquals(2, adapter.index)
        verify(exactly = 2) { view.notifyItemSelected(2, "Smooth") }
        // This is the same library method called by the popup's item click listener.
        adapter.notifyItemSelected(2)
        adapter.notifyItemSelected(0)
        assertEquals(listOf(2, 0), selections)
    }

    @Test
    fun `listener is restored even if library selection throws`() {
        val listener = OnSpinnerItemSelectedListener<CharSequence> { _, _, _, _ -> }
        adapter.onSpinnerItemSelectedListener = listener
        assertThrows(IndexOutOfBoundsException::class.java) { adapter.selectItemWithoutCallback(3) }
        assertSame(listener, adapter.onSpinnerItemSelectedListener)
    }

    @Test
    fun `profile adapter also renders silently and retains user selection`() {
        val profiles = spyk(SettingsProfileSpinnerAdapterV3(view, { true }, { _, _ -> })).apply {
            every { notifyDataSetChanged() } just Runs
            setItems(listOf("Profile 1", "Sport", "+"))
        }
        val selections = mutableListOf<Int>()
        profiles.onSpinnerItemSelectedListener = OnSpinnerItemSelectedListener { _, _, index, _ -> selections.add(index) }
        profiles.selectItemWithoutCallback(1)
        profiles.setItems(listOf("Profile 1", "Renamed", "+"))
        profiles.selectItemWithoutCallback(1)
        assertTrue(selections.isEmpty())
        profiles.notifyItemSelected(1)
        profiles.notifyItemSelected(2)
        assertEquals(listOf(1, 2), selections)
    }
}
