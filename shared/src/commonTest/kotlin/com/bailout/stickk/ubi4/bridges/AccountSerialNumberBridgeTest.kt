package com.bailout.stickk.ubi4.bridges

import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.data.state.ParameterStoreKeyV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.AccountSerialNumberSource
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.AccountSerialNumberState
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.AccountSerialSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSerialNumberBridgeTest {
    @Test
    fun `cached serial is ignored until a fresh matching response and repeated states are suppressed`() = runTest {
        val fixture = Fixture()
        fixture.start(this)
        runCurrent()

        assertEquals(1, fixture.requests)
        assertEquals(0, fixture.valueReads)
        assertEquals(listOf(LOADING), fixture.states)

        fixture.updates.emit(fixture.key.copy(dataCode = fixture.key.dataCode + 1))
        runCurrent()
        assertEquals(0, fixture.valueReads)

        fixture.reply(ParameterTypedValueV3.Text(" \u0000FEST-HM-13261\u0000 "))
        runCurrent()
        assertEquals(AccountSerialNumberState("FEST-HM-13261", false, ""), fixture.states.last())

        fixture.reply(ParameterTypedValueV3.Text("FEST-HM-13261"))
        runCurrent()
        assertEquals(2, fixture.states.size)

        fixture.reply(ParameterTypedValueV3.Text("FEST-HM-13557"))
        runCurrent()
        assertEquals("FEST-HM-13557", fixture.states.last().serialNumber)
    }

    @Test
    fun `subscription exists before request so immediate response is not lost`() = runTest {
        val fixture = Fixture()
        fixture.onRequest = {
            assertEquals(1, fixture.updates.subscriptionCount.value)
            fixture.value = ParameterTypedValueV3.Text("FEST-HM-13261")
            assertTrue(fixture.updates.tryEmit(fixture.key))
        }
        fixture.start(this)
        runCurrent()

        assertEquals("FEST-HM-13261", fixture.states.last().serialNumber)
        advanceTimeBy(10_000L)
        runCurrent()
        assertEquals("", fixture.states.last().errorMessage)
    }

    @Test
    fun `timeout reports missing fresh response and late serial still recovers`() = runTest {
        val fixture = Fixture()
        fixture.start(this)
        runCurrent()
        advanceTimeBy(10_000L)
        runCurrent()

        assertEquals(0, fixture.valueReads)
        fixture.assertError("did not respond")
        assertEquals(1, fixture.updates.subscriptionCount.value)

        fixture.reply(ParameterTypedValueV3.Text("FEST-HM-13261"))
        runCurrent()
        assertEquals(AccountSerialNumberState("FEST-HM-13261", false, ""), fixture.states.last())
    }

    @Test
    fun `empty and non-text replies report an error instead of reusing serial`() = runTest {
        val fixture = Fixture()
        fixture.start(this, lang = "ru")
        runCurrent()
        fixture.reply(ParameterTypedValueV3.Text("FEST-HM-13261"))
        runCurrent()

        fixture.reply(ParameterTypedValueV3.Text(" \u0000 \n"))
        runCurrent()
        fixture.assertError("пустой или некорректный")

        fixture.reply(ParameterTypedValueV3.UInt32(13261))
        runCurrent()
        fixture.assertError("пустой или некорректный")
        assertEquals(3, fixture.states.size)

        advanceTimeBy(10_000L)
        runCurrent()
        fixture.assertError("пустой или некорректный")
        fixture.reply(ParameterTypedValueV3.Text("FEST-HM-13261"))
        runCurrent()
        assertEquals("FEST-HM-13261", fixture.states.last().serialNumber)
    }

    @Test
    fun `disconnect clears serial cancels wait and reconnect requires another fresh response`() = runTest {
        val fixture = Fixture()
        fixture.start(this)
        runCurrent()
        fixture.reply(ParameterTypedValueV3.Text("FEST-HM-13261"))
        runCurrent()

        fixture.connection.value = BLEState.State.DISCONNECTED
        runCurrent()
        fixture.assertError("Connect the prosthesis")
        assertEquals(0, fixture.updates.subscriptionCount.value)
        val disconnectedStateCount = fixture.states.size
        advanceTimeBy(10_000L)
        runCurrent()
        assertEquals(disconnectedStateCount, fixture.states.size)

        fixture.connection.value = BLEState.State.CONNECTING
        runCurrent()
        assertEquals(LOADING, fixture.states.last())
        assertEquals(1, fixture.requests)

        fixture.connection.value = BLEState.State.READY
        runCurrent()
        assertEquals(2, fixture.requests)
        assertEquals(LOADING, fixture.states.last())
        assertEquals(1, fixture.valueReads)

        fixture.reply(ParameterTypedValueV3.Text("FEST-HM-13261"))
        runCurrent()
        assertEquals("FEST-HM-13261", fixture.states.last().serialNumber)
    }

    @Test
    fun `disconnect while waiting cancels timeout instead of replacing connection error`() = runTest {
        val fixture = Fixture()
        fixture.start(this)
        runCurrent()
        fixture.connection.value = BLEState.State.DISCONNECTED
        runCurrent()
        fixture.assertError("Connect the prosthesis")
        assertEquals(0, fixture.updates.subscriptionCount.value)

        advanceTimeBy(10_000L)
        runCurrent()
        fixture.assertError("Connect the prosthesis")
        assertEquals(2, fixture.states.size)
    }

    @Test
    fun `changed address or profile cannot supply another device serial`() = runTest {
        val fixture = Fixture()
        fixture.start(this)
        runCurrent()
        fixture.reply(ParameterTypedValueV3.Text("FEST-HM-13261"))
        runCurrent()

        fixture.session = fixture.session.copy(address = "device-b")
        fixture.reply(ParameterTypedValueV3.Text("FOREIGN-DEVICE"))
        runCurrent()
        fixture.assertError("connection changed")
        assertEquals(1, fixture.valueReads)

        fixture.connection.value = BLEState.State.DISCONNECTED
        runCurrent()
        fixture.connection.value = BLEState.State.READY
        runCurrent()
        fixture.session = fixture.session.copy(profile = V3DeviceProfile.INDY3)
        fixture.reply(ParameterTypedValueV3.Text("FOREIGN-PROFILE"))
        runCurrent()
        fixture.assertError("connection changed")
        assertEquals(1, fixture.valueReads)
    }

    @Test
    fun `unavailable request packet and throwing sender produce actionable errors`() = runTest {
        val fixture = Fixture()
        fixture.packet = null
        val firstObserver = fixture.start(this)
        runCurrent()
        fixture.assertError("prepare")
        assertEquals(0, fixture.requests)
        firstObserver.cancel()
        runCurrent()

        fixture.packet = byteArrayOf(1)
        fixture.onRequest = { error("transport diagnostic must not leak to UI") }
        fixture.start(this)
        runCurrent()
        fixture.assertError("Could not request")
        assertFalse(fixture.states.last().errorMessage.contains("diagnostic"))
        advanceTimeBy(10_000L)
        runCurrent()
        fixture.assertError("Could not request")
    }

    @Test
    fun `throwing request builder produces an error and never sends`() = runTest {
        val fixture = Fixture()
        fixture.onBuildRequest = { error("builder diagnostic must not leak to UI") }
        fixture.start(this)
        runCurrent()
        fixture.assertError("Could not request")
        assertEquals(0, fixture.requests)
        assertFalse(fixture.states.last().errorMessage.contains("diagnostic"))
    }

    @Test
    fun `cancelling observer cancels pending work and observing again requires a fresh response`() = runTest {
        val fixture = Fixture()
        val observer = fixture.start(this)
        runCurrent()
        assertEquals(1, fixture.updates.subscriptionCount.value)

        observer.cancel()
        runCurrent()
        assertEquals(0, fixture.updates.subscriptionCount.value)
        advanceTimeBy(10_000L)
        runCurrent()
        assertEquals(listOf(LOADING), fixture.states)

        fixture.reply(ParameterTypedValueV3.Text("STALE-WHILE-NOT-OBSERVED"))
        fixture.start(this)
        runCurrent()
        assertEquals(2, fixture.requests)
        assertEquals(0, fixture.valueReads)
        assertEquals(LOADING, fixture.states.last())

        fixture.reply(ParameterTypedValueV3.Text("FEST-HM-13261"))
        runCurrent()
        assertEquals("FEST-HM-13261", fixture.states.last().serialNumber)
    }

    private class Fixture {
        val key = ParameterStoreKeyV3(parameterID = 1, dataCode = 11, deviceAddress = 1)
        val connection = MutableStateFlow(BLEState.State.READY)
        val updates = MutableSharedFlow<ParameterStoreKeyV3>(extraBufferCapacity = 16)
        val states = mutableListOf<AccountSerialNumberState>()
        var value: ParameterTypedValueV3? = ParameterTypedValueV3.Text("STALE-SERIAL")
        var session = AccountSerialSession("device-a", V3DeviceProfile.STANDARD_V3)
        var packet: ByteArray? = byteArrayOf(1)
        var requests = 0
        var valueReads = 0
        var onRequest: () -> Unit = {}
        var onBuildRequest: () -> Unit = {}

        fun start(scope: TestScope, lang: String = "en"): Job = scope.backgroundScope.launch {
            AccountSerialNumberSource(
                connection = connection,
                updates = updates,
                parameterKey = key,
                currentValue = { valueReads++; value },
                currentSession = { session },
                readRequest = { onBuildRequest(); packet },
            ).observe(lang, request = { requests++; onRequest() }).collect { states += it }
        }

        suspend fun reply(response: ParameterTypedValueV3) {
            value = response
            updates.emit(key)
        }

        fun assertError(expectedMessagePart: String) {
            assertEquals("", states.last().serialNumber)
            assertFalse(states.last().isLoading)
            assertTrue(states.last().errorMessage.contains(expectedMessagePart), states.last().errorMessage)
        }
    }

    private companion object {
        val LOADING = AccountSerialNumberState("", true, "")
    }
}
