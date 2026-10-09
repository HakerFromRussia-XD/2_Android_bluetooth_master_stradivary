package com.bailout.stickk.ubi4.versions.v3.domain.settings.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ParameterKeys
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsChange
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.settings.V3ToggleSliderValue
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ToggleSliderUseCasesV3Test {
    private val keys = listOf(
        V3ParameterKeys.P_KEY_EMG_CHANGE_GESTURE,
        V3ParameterKeys.P_KEY_EMG_MOVEMENT_LOCK,
        V3ParameterKeys.P_KEY_SCREEN_TIMEOUT,
    )
    private val repository = FakeRepository()
    private val edit = EditToggleSliderUseCaseV3(repository)
    private val send = SendToggleSliderValueUseCaseV3(repository)

    @Test
    fun `flag changes preserve zero and time edits preserve flag without sending`() {
        keys.forEach { key ->
            assertEquals(V3ToggleSliderValue(0, true), edit.setEnabled(key, true))
            for (time in listOf(10, 100)) {
                assertEquals(V3ToggleSliderValue(time, true), edit.setTime(key, time))
            }
            assertEquals(V3ToggleSliderValue(100, false), edit.setEnabled(key, false))
            assertNull(edit.setTime(key, 42))
            assertEquals(V3ToggleSliderValue(100, false), repository.getToggleSliderValue(key))
        }
        assertEquals(keys.size * 4, repository.saved.size)
        assertTrue(repository.sent.isEmpty())
    }

    @Test
    fun `invalid input and live interaction lock cannot save or send`() {
        keys.forEach { key ->
            repository.values.getOrPut(key) { MutableStateFlow(null) }.value = V3ToggleSliderValue(37, true)
            listOf(0, 9, 101, Int.MAX_VALUE).forEach { value ->
                assertFailsWith<IllegalArgumentException> { edit.setTime(key, value) }
            }
        }
        val unrelated = V3ParameterKeys.P_KEY_FORCE_SETTINGS
        assertFailsWith<IllegalArgumentException> { edit.setTime(unrelated, 42) }
        assertFailsWith<IllegalArgumentException> { edit.setEnabled(unrelated, true) }
        assertFailsWith<IllegalArgumentException> { send(unrelated) }
        repository.toggleSliderInteractionEnabled.value = false
        keys.forEach { key ->
            assertNull(edit.setTime(key, 42))
            assertNull(edit.setEnabled(key, false))
            send(key)
        }
        assertTrue(repository.saved.isEmpty())
        assertTrue(repository.sent.isEmpty())
    }

    @Test
    fun `send reads the latest value and preserves zero without repeating local persistence`() {
        keys.forEach { key ->
            send(key)
            assertTrue(repository.sent.none { it.first == key })
            edit.setEnabled(key, true)
            edit.setTime(key, 42)
            repository.values.getValue(key).value = V3ToggleSliderValue(0, false)
            send(key)
        }
        assertEquals(keys.map { it to V3ToggleSliderValue(0, false) }, repository.sent)
        assertEquals(keys.size * 2, repository.saved.size)
    }

    @Test
    fun `snapshot and observation keep missing values and follow updates without writes`() = runTest {
        val key = keys.first()
        val selected = keys.take(2).toSet()
        val snapshot = GetToggleSliderSettingsUseCaseV3(repository)(selected)
        assertEquals(selected.associateWith { null }, snapshot.values)
        assertTrue(snapshot.isInteractionEnabled)
        val changes = mutableListOf<V3ToggleSliderSettingsChange>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveToggleSliderSettingsUseCaseV3(repository)(selected).collect { changes.add(it) }
        }
        runCurrent()
        assertEquals(
            selected.map { V3ToggleSliderSettingsChange.ValueChanged(it, null) }.toSet() +
                V3ToggleSliderSettingsChange.InteractionChanged(true),
            changes.toSet(),
        )
        changes.clear()
        val incoming = V3ToggleSliderValue(0, true)
        repository.values.getValue(key).value = incoming
        repository.toggleSliderInteractionEnabled.value = false
        runCurrent()
        assertEquals(setOf(
            V3ToggleSliderSettingsChange.ValueChanged(key, incoming),
            V3ToggleSliderSettingsChange.InteractionChanged(false),
        ), changes.toSet())
        job.cancelAndJoin()
        changes.clear()
        repository.values.getValue(key).value = V3ToggleSliderValue(50, false)
        runCurrent()
        assertTrue(changes.isEmpty())
        assertTrue(repository.saved.isEmpty())
        assertTrue(repository.sent.isEmpty())
    }

    private class FakeRepository : V3ToggleSliderSettingsRepository {
        override val toggleSliderInteractionEnabled = MutableStateFlow(true)
        val values = mutableMapOf<String, MutableStateFlow<V3ToggleSliderValue?>>()
        val saved = mutableListOf<Pair<String, V3ToggleSliderValue>>()
        val sent = mutableListOf<Pair<String, V3ToggleSliderValue>>()
        override fun observeToggleSliderValue(parameterKey: String) =
            values.getOrPut(parameterKey) { MutableStateFlow(null) }
        override fun getToggleSliderValue(parameterKey: String) = values[parameterKey]?.value
        override fun requestToggleSliderValue(parameterKey: String) = error("Unexpected toggle slider request")
        override fun saveToggleSliderValue(parameterKey: String, value: V3ToggleSliderValue) {
            saved.add(parameterKey to value)
            observeToggleSliderValue(parameterKey).value = value
        }
        override fun sendToggleSliderValue(parameterKey: String, value: V3ToggleSliderValue) {
            sent.add(parameterKey to value)
        }
    }
}
