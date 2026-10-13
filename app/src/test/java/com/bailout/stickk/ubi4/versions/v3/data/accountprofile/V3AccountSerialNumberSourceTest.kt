package com.bailout.stickk.ubi4.versions.v3.data.accountprofile

import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.*
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_SERIAL_NUMBER
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class V3AccountSerialNumberSourceTest {
    private val info = ParameterInfoRegistry.require(P_KEY_SET_SERIAL_NUMBER)
    private val originalProfile = UiState.activeV3DeviceProfile
    private val originalMac = WidgetRepoProvider.mac()
    private val originalState = BLEState.state.value

    @BeforeEach fun setUp() {
        ParameterStoreV3.clear()
        WidgetRepoProvider.setCurrentMac("first")
        UiState.activeV3DeviceProfile = V3DeviceProfile.STANDARD_V3
        BLEState.publishDisconnect()
    }

    @AfterEach fun tearDown() {
        ParameterStoreV3.clear()
        WidgetRepoProvider.setCurrentMac(originalMac)
        UiState.activeV3DeviceProfile = originalProfile
        when (originalState) {
            BLEState.State.DISCONNECTED -> BLEState.publishDisconnect()
            BLEState.State.CONNECTING -> BLEState.publishConnecting()
            BLEState.State.READY -> BLEState.publishReady()
            BLEState.State.ERROR -> BLEState.publishError()
        }
    }

    @Test fun `stale global snapshot is ignored and synchronous response after subscription is received`() = runTest {
        ParameterStoreV3.put(info, ParameterTypedValueV3.Text("FEST-old"))
        BLEState.publishReady()
        val packets = mutableListOf<ByteArray>()
        val source = V3AccountSerialNumberSource { packet, _ ->
            packets += packet
            ParameterStoreV3.put(info, ParameterTypedValueV3.Text(" \u0000FEST-HM-13261\u0000 "))
        }
        val seen = mutableListOf<String?>()
        source.observe().onEach { seen += it }.launchIn(backgroundScope)
        runCurrent()
        assertEquals(listOf(null, "FEST-HM-13261"), seen)
        assertEquals(1, packets.size)
        assertArrayEquals(byteArrayOf(0, 1, 10, 0), packets.single().copyOfRange(0, 4))
    }

    @Test fun `disconnected connection and reconnect wait for a new response even for the same serial`() = runTest {
        var requests = 0
        val seen = mutableListOf<String?>()
        V3AccountSerialNumberSource { _, _ -> requests++ }.observe().onEach { seen += it }.launchIn(backgroundScope)
        runCurrent()
        assertEquals(listOf<String?>(null), seen)
        assertEquals(0, requests)
        BLEState.publishReady(); runCurrent()
        assertEquals(1, requests)
        ParameterStoreV3.put(info, ParameterTypedValueV3.Text("12345")); runCurrent()
        assertEquals("12345", seen.last())
        BLEState.publishDisconnect(); runCurrent()
        assertNull(seen.last())
        BLEState.publishReady(); runCurrent()
        assertEquals(2, requests)
        assertNull(seen.last())
        ParameterStoreV3.put(info, ParameterTypedValueV3.Text("12345")); runCurrent()
        assertEquals(listOf(null, "12345", null, "12345"), seen)
    }

    @Test fun `blank nontext and responses from a changed device cannot become account serial`() = runTest {
        BLEState.publishReady()
        val seen = mutableListOf<String?>()
        V3AccountSerialNumberSource { _, _ -> }.observe().onEach { seen += it }.launchIn(backgroundScope)
        runCurrent()
        ParameterStoreV3.put(info, ParameterTypedValueV3.Text(" \u0000 ")); runCurrent()
        ParameterStoreV3.put(info, ParameterTypedValueV3.UInt32(42)); runCurrent()
        assertEquals(listOf<String?>(null), seen)
        ParameterStoreV3.put(info, ParameterTypedValueV3.Text("FEST-HM-13261")); runCurrent()
        WidgetRepoProvider.setCurrentMac("second")
        ParameterStoreV3.put(info, ParameterTypedValueV3.Text("FEST-other")); runCurrent()
        assertNull(seen.last())
        WidgetRepoProvider.setCurrentMac("first")
        UiState.activeV3DeviceProfile = V3DeviceProfile.INDY3
        ParameterStoreV3.put(info, ParameterTypedValueV3.Text("FEST-another")); runCurrent()
        assertFalse(seen.contains("FEST-other"))
        assertFalse(seen.contains("FEST-another"))
    }
}
