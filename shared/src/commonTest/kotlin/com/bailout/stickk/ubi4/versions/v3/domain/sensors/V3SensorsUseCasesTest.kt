package com.bailout.stickk.ubi4.versions.v3.domain.sensors

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase.EditPlotThresholdUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase.RefreshSensorsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase.RequestPlotThresholdsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase.SetPlotThresholdsUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase.StartProsthesisMovementUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.sensors.usecase.StopProsthesisMovementUseCaseV3

class V3SensorsUseCasesTest {
    @Test
    fun `threshold requests forward raw targets and repeats independently of the interaction lock`() {
        val repository = PlotRepository()
        repository.interactionEnabled.value = false
        val request = RequestPlotThresholdsUseCaseV3(repository)
        request(15, 47)
        request(15, 47)
        request(7, 26)
        assertEquals(listOf(15 to 47, 15 to 47, 7 to 26), repository.requests)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `editing clamps only the selected threshold and leaves the original unchanged`() {
        val edit = EditPlotThresholdUseCaseV3()
        val original = V3PlotThresholds(open = 37, close = 91)
        val cases = listOf(-1 to 0, 0 to 0, 128 to 128, 255 to 255, 256 to 255)
        for ((input, expected) in cases) {
            assertEquals(V3PlotThresholds(expected, 91), edit(original, V3PlotThreshold.OPEN, input))
            assertEquals(V3PlotThresholds(37, expected), edit(original, V3PlotThreshold.CLOSE, input))
        }
        assertEquals(V3PlotThresholds(37, 91), original)
    }

    @Test
    fun `saving accepts boundary pairs but rejects invalid values and respects the current lock`() {
        val repository = PlotRepository()
        val save = SetPlotThresholdsUseCaseV3(repository)
        val valid = listOf(V3PlotThresholds(0, 255), V3PlotThresholds(255, 0))
        valid.forEach(save::invoke)
        val invalid = listOf(
            V3PlotThresholds(-1, 91), V3PlotThresholds(256, 91),
            V3PlotThresholds(37, -1), V3PlotThresholds(37, 256),
        )
        invalid.forEach { assertFailsWith<IllegalArgumentException> { save(it) } }
        repository.interactionEnabled.value = false
        save(V3PlotThresholds(37, 91))
        assertEquals(valid, repository.saved)
        repository.interactionEnabled.value = true
        save(V3PlotThresholds(37, 91))
        assertEquals(valid + V3PlotThresholds(37, 91), repository.saved)
    }

    @Test
    fun `start respects the lock while stop and refresh retain their independent rules`() {
        val repository = CommandsRepository()
        val start = StartProsthesisMovementUseCaseV3(repository)
        val stop = StopProsthesisMovementUseCaseV3(repository)
        val refresh = RefreshSensorsUseCaseV3(repository)
        val address = "device-address"

        assertFalse(start(address, V3ProsthesisMovement.OPEN))
        assertTrue(repository.started.isEmpty())
        stop(address)
        assertEquals(listOf(address), repository.stopped)
        // Refresh does not use the movement lock; the repository checks the current device.
        assertTrue(refresh(address))
        repository.refreshInProgress.value = true
        assertFalse(refresh(address))
        assertEquals(listOf(address), repository.refreshed)

        repository.interactionEnabled.value = true
        repository.acceptCommands = false
        assertFalse(start(address, V3ProsthesisMovement.OPEN))
        repository.acceptCommands = true
        assertTrue(start(address, V3ProsthesisMovement.CLOSE))
        assertEquals(listOf(address to V3ProsthesisMovement.OPEN, address to V3ProsthesisMovement.CLOSE), repository.started)
        repository.refreshInProgress.value = false
        repository.acceptCommands = false
        assertFalse(refresh(address))
        assertEquals(listOf(address, address), repository.refreshed)
    }

    private class PlotRepository : V3SensorsPlotRepository {
        override val interactionEnabled = MutableStateFlow(true)
        val saved = mutableListOf<V3PlotThresholds>()
        val requests = mutableListOf<Pair<Int, Int>>()
        override fun getThresholds(): V3PlotThresholds? = null
        override fun observeThresholds() = emptyFlow<V3PlotThresholds?>()
        override fun observeSamples() = emptyFlow<List<Int>>()
        override fun getChannelCount() = 2
        override fun arePlotPointsPaused() = false
        override fun requestThresholds(parameterID: Int, dataCode: Int) { requests.add(parameterID to dataCode) }
        override fun setThresholds(thresholds: V3PlotThresholds) { saved.add(thresholds) }
    }

    private class CommandsRepository : V3SensorsCommandsRepository {
        override val interactionEnabled = MutableStateFlow(false)
        override val refreshInProgress = MutableStateFlow(false)
        var acceptCommands = true
        val started = mutableListOf<Pair<String, V3ProsthesisMovement>>()
        val stopped = mutableListOf<String>()
        val refreshed = mutableListOf<String>()
        override fun startMovement(deviceAddress: String, movement: V3ProsthesisMovement): Boolean {
            started.add(deviceAddress to movement)
            return acceptCommands
        }
        override fun stopMovement(deviceAddress: String) { stopped.add(deviceAddress) }
        override fun refreshSensors(deviceAddress: String): Boolean {
            refreshed.add(deviceAddress)
            return acceptCommands
        }
    }
}
