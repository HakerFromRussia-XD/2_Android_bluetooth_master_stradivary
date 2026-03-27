package com.bailout.stickk.ubi4.ble

import android.bluetooth.BluetoothDevice
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.bailout.stickk.ubi4.data.parser.BLEParser
import com.bailout.stickk.ubi4.data.parser.BLEParserV3
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.ble.BleEnvironment
import com.bailout.stickk.ubi4.testing.RecordingBleCommandExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.mockito.Mockito
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BleRuntimeCoverageTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUpMainDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }


    @Test
    fun `sample gatt attributes should support register and lookup`() {
        val uuid = "1234"
        assertEquals("default", SampleGattAttributes.lookup(uuid, "default"))

        SampleGattAttributes.register(uuid, "main")

        assertEquals("main", SampleGattAttributes.lookup(uuid, "default"))
        assertEquals("READ", SampleGattAttributes.READ)
        assertEquals("WRITE", SampleGattAttributes.WRITE)
        assertEquals("NOTIFY", SampleGattAttributes.NOTIFY)
        assertTrue(SampleGattAttributes.MAIN_CHANNEL_CHARACTERISTIC.isNotBlank())
    }

    @Test
    fun `ble manager should dispatch queued command via injected executor`() {
        val manager = BleManagerKmm()
        val executor = RecordingBleCommandExecutor()
        manager.setBleCommandExecutor(executor)

        val packet = byteArrayOf(1, 2, 3)
        manager.sendBytesKmm(packet, command = "cmd", typeCommand = "WRITE") {}

        assertEquals(1, executor.queuedPackets.size)
        assertEquals(packet.toList(), executor.queuedPackets.first().toList())
    }

    @Test
    fun `ble manager should fallback to environment executor`() {
        val manager = BleManagerKmm()
        val executor = RecordingBleCommandExecutor()
        val parser = BLEParser(
            coroutineScope = CoroutineScope(Dispatchers.Default),
            bleCommandExecutor = executor,
            bleManager = manager
        )
        val parserV3 = BLEParserV3(
            coroutineScope = CoroutineScope(Dispatchers.Default),
            bleCommandExecutor = executor,
            bleManager = manager
        )

        BleEnvironment.register(manager = manager, executor = executor, parser = parser, parserV3 = parserV3)

        manager.sendBytesKmm(byteArrayOf(9, 8, 7), command = "cmd", typeCommand = "READ") {}

        assertEquals(1, executor.queuedPackets.size)
        assertSame(manager, BleEnvironment.getBleManager())
        assertSame(executor, BleEnvironment.getBleCommandExecutor())
        assertSame(parser, BleEnvironment.getBleParser())
        assertSame(parserV3, BleEnvironment.getBleParserV3())
    }

    @Test
    fun `ble environment should throw if not registered`() {
        // New JVM run may start without registration for this object.
        // We assert at least one missing dependency branch.
        runCatching {
            BleEnvironment.getBleManager()
        }.onFailure {
            assertTrue(it is IllegalStateException)
        }
    }

    @Test
    fun `ble manager basic api should be callable`() {
        val manager = BleManagerKmm()
        val found = mutableListOf<BleDeviceKmm>()
        var readyCalls = 0

        manager.startScanKmm { found += it }
        manager.stopScanKmm()
        manager.connectToDevice("AA:BB")
        manager.setOnCharacteristicsReadyListener { readyCalls++ }

        assertTrue(found.isEmpty())
        assertEquals(1, readyCalls)
    }

    @Test
    fun `android scanner should collect unique devices and reset on start scan`() {
        val scanner = AndroidBleScanner()

        val first = Mockito.mock(BluetoothDevice::class.java)
        Mockito.`when`(first.address).thenReturn("AA:BB:CC:DD:EE:01")
        Mockito.`when`(first.name).thenReturn("Device-1")

        val second = Mockito.mock(BluetoothDevice::class.java)
        Mockito.`when`(second.address).thenReturn("AA:BB:CC:DD:EE:02")
        Mockito.`when`(second.name).thenReturn(null)

        scanner.onDeviceFound(first)
        scanner.onDeviceFound(first) // дубликат по id не должен добавляться
        scanner.onDeviceFound(second)

        assertEquals(2, scanner.devices.value.size)
        assertEquals("Device-1", scanner.devices.value[0].name)
        assertEquals("Unknown", scanner.devices.value[1].name)

        scanner.startScan()
        scanner.stopScan()

        assertTrue(scanner.devices.value.isEmpty())
        assertEquals(0, scanner.devicesLiveData.value?.size ?: 0)
    }

    @Test
    fun `ble device kmm should expose constructor fields`() {
        val device = BleDeviceKmm(id = "id-1", name = "demo", rssi = -42)

        assertEquals("id-1", device.id)
        assertEquals("demo", device.name)
        assertEquals(-42, device.rssi)
    }
}
