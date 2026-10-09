package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ParameterKeys
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsChange
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3SpinnerSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SpinnerUseCasesV3Test {
    private val choices = mapOf(
        V3ParameterKeys.P_KEY_HAND_CONTROL_MODE to 0..4,
        V3ParameterKeys.P_KEY_GESTURE_CHANGE_MODE to 0..1,
        V3ParameterKeys.P_KEY_EMG_CONTROL_MODE to 0..3,
        V3ParameterKeys.P_KEY_LEFT_RIGHT_HAND to 0..1,
    )
    private val repository = FakeRepository()
    private val setValue = SetSpinnerValueUseCaseV3(repository)

    @Test
    fun `every supported choice reaches the repository unchanged`() {
        choices.forEach { (key, values) -> values.forEach { setValue(key, it) } }
        assertEquals(choices.flatMap { (key, values) -> values.map { key to it } }, repository.writes)
    }

    @Test
    fun `invalid choices and live interaction lock prevent writes`() {
        choices.forEach { (key, values) ->
            listOf(values.first - 1, values.last + 1).forEach { value ->
                assertFailsWith<IllegalArgumentException> { setValue(key, value) }
            }
        }
        listOf(V3ParameterKeys.P_KEY_DEVICE_ROLE, V3ParameterKeys.P_KEY_SETTINGS_PROFILE, "unknown")
            .forEach { key -> assertFailsWith<IllegalArgumentException> { setValue(key, 0) } }
        repository.spinnerInteractionEnabled.value = false
        choices.keys.forEach { setValue(it, 0) }
        assertTrue(repository.writes.isEmpty())
        repository.spinnerInteractionEnabled.value = true
        val key = V3ParameterKeys.P_KEY_HAND_CONTROL_MODE
        setValue(key, 4)
        assertEquals(listOf(key to 4), repository.writes)
    }

    @Test
    fun `snapshot and observation preserve incoming values without writes and stop on cancellation`() = runTest {
        val key = V3ParameterKeys.P_KEY_HAND_CONTROL_MODE
        val selected = setOf(key, V3ParameterKeys.P_KEY_GESTURE_CHANGE_MODE)
        repository.observeSpinnerValue(key).value = 4
        repository.spinnerInteractionEnabled.value = false
        val snapshot = GetSpinnerSettingsUseCaseV3(repository)(selected)
        assertEquals(mapOf(key to 4, V3ParameterKeys.P_KEY_GESTURE_CHANGE_MODE to null), snapshot.values)
        assertFalse(snapshot.isInteractionEnabled)
        val changes = mutableListOf<V3SpinnerSettingsChange>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveSpinnerSettingsUseCaseV3(repository)(selected).collect { changes.add(it) }
        }
        runCurrent()
        assertEquals(setOf(
            V3SpinnerSettingsChange.ValueChanged(key, 4),
            V3SpinnerSettingsChange.ValueChanged(V3ParameterKeys.P_KEY_GESTURE_CHANGE_MODE, null),
            V3SpinnerSettingsChange.InteractionChanged(false),
        ), changes.toSet())
        changes.clear()
        // Observation preserves even an unexpected device value; display rules belong to the UI.
        repository.observeSpinnerValue(key).value = 99
        repository.spinnerInteractionEnabled.value = true
        runCurrent()
        assertEquals(setOf(
            V3SpinnerSettingsChange.ValueChanged(key, 99),
            V3SpinnerSettingsChange.InteractionChanged(true),
        ), changes.toSet())
        job.cancelAndJoin()
        changes.clear()
        repository.observeSpinnerValue(key).value = null
        runCurrent()
        assertTrue(changes.isEmpty())
        assertTrue(repository.writes.isEmpty())
    }

    private class FakeRepository : V3SpinnerSettingsRepository {
        override val spinnerInteractionEnabled = MutableStateFlow(true)
        private val values = mutableMapOf<String, MutableStateFlow<Int?>>()
        val writes = mutableListOf<Pair<String, Int>>()
        override fun observeSpinnerValue(parameterKey: String) =
            values.getOrPut(parameterKey) { MutableStateFlow(null) }
        override fun getSpinnerValue(parameterKey: String) = values[parameterKey]?.value
        override fun requestSpinnerValue(parameterKey: String) = error("Unexpected spinner request")
        override fun setSpinnerValue(parameterKey: String, value: Int) {
            writes.add(parameterKey to value)
            observeSpinnerValue(parameterKey).value = value
        }
    }
}
