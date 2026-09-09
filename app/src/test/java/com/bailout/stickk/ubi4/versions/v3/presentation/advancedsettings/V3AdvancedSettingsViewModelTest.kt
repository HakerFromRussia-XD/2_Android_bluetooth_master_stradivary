package com.bailout.stickk.ubi4.versions.v3.presentation.advancedsettings

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_FORCE_SETTINGS
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3DeviceSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase.SetSliderValueUseCaseV3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V3AdvancedSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRepository()
    private val store = ViewModelStore()
    private lateinit var viewModel: V3AdvancedSettingsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = V3AdvancedSettingsViewModel(
            repository, SetSliderValueUseCaseV3(repository),
            mapOf(P_KEY_SPEED_SETTINGS to 0..100, P_KEY_FORCE_SETTINGS to 0..100)
        )
        store.put("settings", viewModel)
    }

    @AfterEach
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun attach() = viewModel.onAction(V3AdvancedSettingsAction.ViewAttached)
    private fun step(value: Int = 1, parameterKey: String = P_KEY_SPEED_SETTINGS) = viewModel.onAction(
        V3AdvancedSettingsAction.SliderStepClicked(parameterKey, value)
    )
    private fun sliderState(key: String = P_KEY_SPEED_SETTINGS) = viewModel.uiState.value.sliders.getValue(key)

    @Test
    fun `initial display incoming values and reattach never send commands`() = runTest(dispatcher) {
        attach()
        runCurrent()
        assertEquals(17, sliderState().value)
        assertEquals(70, sliderState(P_KEY_FORCE_SETTINGS).value)
        repository.value.value = 63
        runCurrent()
        assertEquals(63, sliderState().value)
        viewModel.onAction(V3AdvancedSettingsAction.ViewDetached)
        attach()
        assertEquals(emptyList<Int>(), repository.writes)
    }

    @Test
    fun `drag changes draft and sends only on release`() = runTest(dispatcher) {
        attach()
        runCurrent()
        viewModel.onAction(V3AdvancedSettingsAction.SliderValueChanged(P_KEY_SPEED_SETTINGS, 42))
        assertEquals(42, sliderState().value)
        assertEquals(17, repository.value.value)
        assertEquals(emptyList<Int>(), repository.writes)
        viewModel.onAction(V3AdvancedSettingsAction.SliderChangeCommitted(P_KEY_SPEED_SETTINGS, 42))
        runCurrent()
        assertEquals(listOf(42), repository.writes)
    }

    @Test
    fun `rapid steps send once 300 ms after the last click`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        advanceTimeBy(200)
        step()
        advanceTimeBy(299)
        runCurrent()
        assertEquals(19, sliderState().value)
        assertEquals(emptyList<Int>(), repository.writes)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(19), repository.writes)
    }

    @Test
    fun `lock cancels pending write and blocks release even before state collector runs`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        step(parameterKey = P_KEY_FORCE_SETTINGS)
        repository.sliderInteractionEnabled.value = false
        viewModel.onAction(V3AdvancedSettingsAction.SliderChangeCommitted(P_KEY_SPEED_SETTINGS, 55))
        runCurrent()
        assertFalse(sliderState().isEnabled)
        assertFalse(sliderState(P_KEY_FORCE_SETTINGS).isEnabled)
        advanceTimeBy(301)
        runCurrent()
        repository.sliderInteractionEnabled.value = true
        runCurrent()
        advanceTimeBy(301)
        runCurrent()
        assertEquals(emptyList<Int>(), repository.writes)
    }

    @Test
    fun `closing and reopening discards unsent draft and does not replay write`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        step(parameterKey = P_KEY_FORCE_SETTINGS)
        viewModel.onAction(V3AdvancedSettingsAction.ViewDetached)
        advanceTimeBy(301)
        runCurrent()
        attach()
        assertEquals(17, sliderState().value)
        assertEquals(70, sliderState(P_KEY_FORCE_SETTINGS).value)
        advanceTimeBy(301)
        runCurrent()
        assertEquals(emptyList<Int>(), repository.writes)
    }

    @Test
    fun `release replaces pending button write without duplicate command`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        viewModel.onAction(V3AdvancedSettingsAction.SliderChangeCommitted(P_KEY_SPEED_SETTINGS, 42))
        advanceTimeBy(301)
        runCurrent()
        assertEquals(listOf(42), repository.writes)
    }

    @Test
    fun `slider input stays within bounds and rejects another parameter`() = runTest(dispatcher) {
        attach()
        runCurrent()
        viewModel.onAction(V3AdvancedSettingsAction.SliderChangeCommitted("another_parameter", 42))
        viewModel.onAction(V3AdvancedSettingsAction.SliderChangeCommitted(P_KEY_SPEED_SETTINGS, 200))
        runCurrent()
        step()
        advanceTimeBy(300)
        runCurrent()
        viewModel.onAction(V3AdvancedSettingsAction.SliderChangeCommitted(P_KEY_SPEED_SETTINGS, -1))
        assertEquals(listOf(100, 100, 0), repository.writes)
    }

    @Test
    fun `clearing viewmodel cancels delayed write`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        step(parameterKey = P_KEY_FORCE_SETTINGS)
        store.clear()
        advanceTimeBy(301)
        runCurrent()
        assertEquals(emptyList<Int>(), repository.writes)
    }

    @Test
    fun `use case guards allowed range and current interaction lock`() {
        val useCase = SetSliderValueUseCaseV3(repository)
        assertThrows(IllegalArgumentException::class.java) {
            useCase(P_KEY_SPEED_SETTINGS, 101, 0..100)
        }
        repository.sliderInteractionEnabled.value = false
        useCase(P_KEY_SPEED_SETTINGS, 42, 0..100)
        assertEquals(emptyList<Int>(), repository.writes)
    }

    @Test
    fun `speed and force have independent button deadlines`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        advanceTimeBy(100)
        step(parameterKey = P_KEY_FORCE_SETTINGS)
        advanceTimeBy(100)
        step()
        advanceTimeBy(199)
        runCurrent()
        assertEquals(emptyList<Pair<String, Int>>(), repository.writesByParameter)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(P_KEY_FORCE_SETTINGS to 71), repository.writesByParameter)
        assertEquals(19, sliderState().value)
        advanceTimeBy(100)
        runCurrent()
        assertEquals(
            listOf(P_KEY_FORCE_SETTINGS to 71, P_KEY_SPEED_SETTINGS to 19),
            repository.writesByParameter
        )
    }

    @Test
    fun `force response preserves unsent speed draft`() = runTest(dispatcher) {
        attach()
        runCurrent()
        viewModel.onAction(V3AdvancedSettingsAction.SliderValueChanged(P_KEY_SPEED_SETTINGS, 42))
        repository.values.getValue(P_KEY_FORCE_SETTINGS).value = 83
        runCurrent()
        assertEquals(42, sliderState().value)
        assertEquals(83, sliderState(P_KEY_FORCE_SETTINGS).value)
        assertEquals(17, repository.value.value)
        assertEquals(emptyList<Pair<String, Int>>(), repository.writesByParameter)
    }

    @Test
    fun `force release sends own value and keeps pending speed write`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        viewModel.onAction(V3AdvancedSettingsAction.SliderValueChanged(P_KEY_FORCE_SETTINGS, 42))
        assertEquals(70, repository.values.getValue(P_KEY_FORCE_SETTINGS).value)
        viewModel.onAction(V3AdvancedSettingsAction.SliderChangeCommitted(P_KEY_FORCE_SETTINGS, 42))
        runCurrent()
        assertEquals(listOf(P_KEY_FORCE_SETTINGS to 42), repository.writesByParameter)
        advanceTimeBy(300)
        runCurrent()
        assertEquals(
            listOf(P_KEY_FORCE_SETTINGS to 42, P_KEY_SPEED_SETTINGS to 18),
            repository.writesByParameter
        )
    }

    private class FakeRepository : V3DeviceSettingsRepository {
        val values = mapOf(
            P_KEY_SPEED_SETTINGS to MutableStateFlow<Int?>(17),
            P_KEY_FORCE_SETTINGS to MutableStateFlow<Int?>(70),
        )
        val value get() = values.getValue(P_KEY_SPEED_SETTINGS)
        override val sliderInteractionEnabled = MutableStateFlow(true)
        val writes = mutableListOf<Int>()
        val writesByParameter = mutableListOf<Pair<String, Int>>()
        override fun getSliderValue(parameterKey: String) = values.getValue(parameterKey).value
        override fun observeSliderValue(parameterKey: String) = values.getValue(parameterKey)
        override fun setSliderValue(parameterKey: String, value: Int) {
            writes.add(value)
            writesByParameter.add(parameterKey to value)
            values.getValue(parameterKey).value = value
        }
    }
}
