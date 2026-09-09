package com.bailout.stickk.ubi4.versions.v3.presentation.sliders

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_EMG_MAX_GAIN_VALUE
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
class V3SliderSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRepository()
    private val store = ViewModelStore()
    private lateinit var viewModel: V3SliderSettingsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = V3SliderSettingsViewModel(
            repository, SetSliderValueUseCaseV3(repository),
            setOf(
                P_KEY_SPEED_SETTINGS,
                P_KEY_FORCE_SETTINGS,
                P_KEY_EMG_MAX_GAIN_VALUE,
            )
        )
        store.put("settings", viewModel)
    }

    @AfterEach
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun attach() = viewModel.onViewAttached()
    private fun step(value: Int = 1, parameterKey: String = P_KEY_SPEED_SETTINGS) = viewModel.onAction(
        V3SliderAction.SliderStepClicked(parameterKey, value)
    )
    private fun sliderState(key: String = P_KEY_SPEED_SETTINGS) = viewModel.uiState.value.sliders.getValue(key)

    @Test
    fun `initial display incoming values and reattach never send commands`() = runTest(dispatcher) {
        attach()
        runCurrent()
        assertEquals(17, sliderState().value)
        assertEquals(70, sliderState(P_KEY_FORCE_SETTINGS).value)
        repository.value.value = 63
        assertEquals(225, sliderState(P_KEY_EMG_MAX_GAIN_VALUE).value)
        repository.values.getValue(P_KEY_EMG_MAX_GAIN_VALUE).value = 250
        runCurrent()
        assertEquals(63, sliderState().value)
        assertEquals(250, sliderState(P_KEY_EMG_MAX_GAIN_VALUE).value)
        viewModel.onViewDetached()
        attach()
        assertEquals(emptyList<Int>(), repository.writes)
    }

    @Test
    fun `drag changes draft and sends only on release`() = runTest(dispatcher) {
        attach()
        runCurrent()
        viewModel.onAction(V3SliderAction.SliderValueChanged(P_KEY_SPEED_SETTINGS, 42))
        assertEquals(42, sliderState().value)
        assertEquals(17, repository.value.value)
        assertEquals(emptyList<Int>(), repository.writes)
        viewModel.onAction(V3SliderAction.SliderChangeCommitted(P_KEY_SPEED_SETTINGS, 42))
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
        step(parameterKey = P_KEY_EMG_MAX_GAIN_VALUE)
        repository.sliderInteractionEnabled.value = false
        viewModel.onAction(V3SliderAction.SliderChangeCommitted(P_KEY_SPEED_SETTINGS, 55))
        viewModel.onAction(V3SliderAction.SliderChangeCommitted(P_KEY_EMG_MAX_GAIN_VALUE, 240))
        runCurrent()
        assertFalse(sliderState().isEnabled)
        assertFalse(sliderState(P_KEY_FORCE_SETTINGS).isEnabled)
        assertFalse(sliderState(P_KEY_EMG_MAX_GAIN_VALUE).isEnabled)
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
        step(parameterKey = P_KEY_EMG_MAX_GAIN_VALUE)
        viewModel.onViewDetached()
        advanceTimeBy(301)
        runCurrent()
        attach()
        assertEquals(17, sliderState().value)
        assertEquals(70, sliderState(P_KEY_FORCE_SETTINGS).value)
        assertEquals(225, sliderState(P_KEY_EMG_MAX_GAIN_VALUE).value)
        advanceTimeBy(301)
        runCurrent()
        assertEquals(emptyList<Int>(), repository.writes)
    }

    @Test
    fun `release replaces pending button write without duplicate command`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        viewModel.onAction(V3SliderAction.SliderChangeCommitted(P_KEY_SPEED_SETTINGS, 42))
        advanceTimeBy(301)
        runCurrent()
        assertEquals(listOf(42), repository.writes)
    }

    @Test
    fun `slider input stays within bounds and rejects another parameter`() = runTest(dispatcher) {
        attach()
        runCurrent()
        viewModel.onAction(V3SliderAction.SliderChangeCommitted("another_parameter", 42))
        viewModel.onAction(V3SliderAction.SliderChangeCommitted(P_KEY_SPEED_SETTINGS, 200))
        runCurrent()
        step()
        advanceTimeBy(300)
        runCurrent()
        viewModel.onAction(V3SliderAction.SliderChangeCommitted(P_KEY_SPEED_SETTINGS, -1))
        assertEquals(listOf(100, 100, 0), repository.writes)
    }

    @Test
    fun `clearing viewmodel cancels delayed write`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        step(parameterKey = P_KEY_FORCE_SETTINGS)
        step(parameterKey = P_KEY_EMG_MAX_GAIN_VALUE)
        store.clear()
        advanceTimeBy(301)
        runCurrent()
        assertEquals(emptyList<Int>(), repository.writes)
    }

    @Test
    fun `use case guards allowed range and current interaction lock`() {
        val useCase = SetSliderValueUseCaseV3(repository)
        assertThrows(IllegalArgumentException::class.java) {
            useCase(P_KEY_SPEED_SETTINGS, 101)
        }
        repository.sliderInteractionEnabled.value = false
        useCase(P_KEY_SPEED_SETTINGS, 42)
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
        viewModel.onAction(V3SliderAction.SliderValueChanged(P_KEY_SPEED_SETTINGS, 42))
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
        viewModel.onAction(V3SliderAction.SliderValueChanged(P_KEY_FORCE_SETTINGS, 42))
        assertEquals(70, repository.values.getValue(P_KEY_FORCE_SETTINGS).value)
        viewModel.onAction(V3SliderAction.SliderChangeCommitted(P_KEY_FORCE_SETTINGS, 42))
        runCurrent()
        assertEquals(listOf(P_KEY_FORCE_SETTINGS to 42), repository.writesByParameter)
        advanceTimeBy(300)
        runCurrent()
        assertEquals(
            listOf(P_KEY_FORCE_SETTINGS to 42, P_KEY_SPEED_SETTINGS to 18),
            repository.writesByParameter
        )
    }

    @Test
    fun `maximum sensor sensitivity supports values above 100 and clamps drag and steps to 0 through 250`() = runTest(dispatcher) {
        attach()
        runCurrent()
        val key = P_KEY_EMG_MAX_GAIN_VALUE
        viewModel.onAction(V3SliderAction.SliderValueChanged(key, 249))
        assertEquals(249, sliderState(key).value)
        assertEquals(225, repository.values.getValue(key).value)
        assertEquals(emptyList<Pair<String, Int>>(), repository.writesByParameter)
        viewModel.onAction(V3SliderAction.SliderChangeCommitted(key, 249))
        runCurrent()
        step(parameterKey = key)
        step(parameterKey = key)
        assertEquals(250, sliderState(key).value)
        advanceTimeBy(300)
        runCurrent()
        viewModel.onAction(V3SliderAction.SliderChangeCommitted(key, 300))
        runCurrent()
        assertEquals(250, sliderState(key).value)
        viewModel.onAction(V3SliderAction.SliderChangeCommitted(key, -1))
        runCurrent()
        step(value = -1, parameterKey = key)
        assertEquals(0, sliderState(key).value)
        advanceTimeBy(300)
        runCurrent()

        assertEquals(listOf(249, 250, 250, 0, 0).map { key to it }, repository.writesByParameter)
        assertEquals(17, sliderState().value)
        assertEquals(70, sliderState(P_KEY_FORCE_SETTINGS).value)
    }

    @Test
    fun `maximum sensor sensitivity release keeps both pending speed and force writes`() = runTest(dispatcher) {
        attach()
        runCurrent()
        step()
        step(parameterKey = P_KEY_FORCE_SETTINGS)
        viewModel.onAction(V3SliderAction.SliderChangeCommitted(P_KEY_EMG_MAX_GAIN_VALUE, 240))
        runCurrent()
        assertEquals(listOf(P_KEY_EMG_MAX_GAIN_VALUE to 240), repository.writesByParameter)
        advanceTimeBy(300)
        runCurrent()

        assertEquals(
            listOf(P_KEY_EMG_MAX_GAIN_VALUE to 240, P_KEY_SPEED_SETTINGS to 18, P_KEY_FORCE_SETTINGS to 71),
            repository.writesByParameter
        )
    }

    @Test
    fun `screen selection limits observed sliders and their ranges come from domain`() = runTest(dispatcher) {
        val selectedViewModel = V3SliderSettingsViewModel(
            repository, SetSliderValueUseCaseV3(repository), setOf(P_KEY_EMG_MAX_GAIN_VALUE)
        )
        store.put("selected-settings", selectedViewModel)
        selectedViewModel.onViewAttached()
        runCurrent()
        assertEquals(setOf(P_KEY_EMG_MAX_GAIN_VALUE), selectedViewModel.uiState.value.sliders.keys)
        assertEquals(0..250, selectedViewModel.uiState.value.sliders.getValue(P_KEY_EMG_MAX_GAIN_VALUE).allowedRange)
        selectedViewModel.onAction(V3SliderAction.SliderChangeCommitted(P_KEY_FORCE_SETTINGS, 50))
        selectedViewModel.onAction(V3SliderAction.SliderChangeCommitted(P_KEY_EMG_MAX_GAIN_VALUE, 225))
        assertEquals(listOf(P_KEY_EMG_MAX_GAIN_VALUE to 225), repository.writesByParameter)
    }

    private class FakeRepository : V3DeviceSettingsRepository {
        val values = mapOf(
            P_KEY_SPEED_SETTINGS to MutableStateFlow<Int?>(17),
            P_KEY_FORCE_SETTINGS to MutableStateFlow<Int?>(70),
            P_KEY_EMG_MAX_GAIN_VALUE to MutableStateFlow<Int?>(225),
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
