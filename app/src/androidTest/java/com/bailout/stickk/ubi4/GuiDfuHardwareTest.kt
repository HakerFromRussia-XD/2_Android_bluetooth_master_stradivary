package com.bailout.stickk.ubi4

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.bailout.stickk.ubi4.ble.AndroidFirmwareCommandSender
import com.bailout.stickk.ubi4.firmware.*
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import com.bailout.stickk.ubi4.utility.firmware.FirmwareUpdateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/** Explicitly opt-in bench tests. Default test runs never connect or flash hardware. */
@RunWith(AndroidJUnit4::class)
class GuiDfuHardwareTest {
    @get:Rule val permissions = GrantPermissionRule.grant(
        Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS
    )

    @Test fun scanBench() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("guiBenchScan") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scanner = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager)
            .adapter.bluetoothLeScanner
        val found = ConcurrentHashMap<String, Int>()
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                found[result.device.address] = result.rssi
            }
            override fun onScanFailed(errorCode: Int) { Log.e(TAG, "SCAN_FAILED code=$errorCode") }
        }
        scanner.startScan(listOf(ScanFilter.Builder().setDeviceName("FTHS3-00000").build()),
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback)
        try { Thread.sleep(12_000) } finally { scanner.stopScan(callback) }
        found.forEach { (mac, rssi) -> Log.i(TAG, "BENCH_SCAN name=FTHS3-00000 mac=$mac rssi=$rssi") }
        assertTrue("No FTHS3-00000 found", found.isNotEmpty())
    }

    @Test fun updateGuiThroughProductionCoordinator(): Unit = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("guiDfuHardware") == "true")
        val mac = requireNotNull(args.getString("deviceAddress"))
        require(mac.matches(Regex("([0-9A-F]{2}:){5}[0-9A-F]{2}")))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "gui-hardware-test.zip")
        val firmware = FirmwareUpdateUtils.readFirmwarePackage(file)
        val sha = MessageDigest.getInstance("SHA-256").digest(firmware.payload)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        // Only the already verified GUI main image, never boot/FAM or an arbitrary ZIP.
        assertEquals("7440cedfeb03fda5510f80fc41184ea7a7570a19f7df9493b20d9a48764908f1", sha)
        assertEquals(449468, firmware.payload.size)
        val messages = CopyOnWriteArrayList<String>()
        val logger = object : FirmwareUpdateLogger {
            override fun debug(tag: String, message: String) { Log.d(tag, message) }
            override fun info(tag: String, message: String) { messages += message; Log.i(tag, message) }
            override fun warn(tag: String, message: String) { Log.w(tag, message) }
            override fun error(tag: String, message: String, throwable: Throwable?) { Log.e(tag, message, throwable) }
        }
        val intent = Intent(context, MainActivityUBI4::class.java).apply {
            putExtra(ConstantManagerUBI4.EXTRAS_DEVICE_NAME, "FTHS3-00000")
            putExtra(ConstantManagerUBI4.EXTRAS_DEVICE_ADDRESS, mac)
        }
        ActivityScenario.launch<MainActivityUBI4>(intent).use { scenario ->
            var activity: MainActivityUBI4? = null
            scenario.onActivity { activity = it }
            delay(12_000)
            val controller = requireNotNull(activity?.getBLEController())
            withContext(Dispatchers.Main) { controller.setFirmwareUpdateSessionActive(true) }
            try {
                check(controller.prepareFirmwareSessionNotifications())
                val coordinator = FirmwareUpdateCoordinator(
                    Ubi4FirmwareUpdater(AndroidFirmwareCommandSender, logger),
                    V3FirmwareUpdater(AndroidFirmwareCommandSender, logger = logger,
                        bulkTransport = PlatformFirmwareBulkTransport),
                    LegacyV3FirmwareUpdater(AndroidFirmwareCommandSender, logger), logger
                )
                val result = withTimeout(180_000) {
                    coordinator.runFirmwareUpdate(FirmwareUpdateProtocol.V3, 9, firmware) { _, _ -> }
                }
                assertEquals(FirmwareUpdateResult.Success, result)
                assertTrue(messages.any { it.contains("GUI v2 selected addr=9") })
                assertTrue(messages.any { it.contains("GUI v2 SUCCESS addr=9 CRC=GOOD MAIN_APP") })
                Log.i(TAG, "HARDWARE_PASS mac=$mac address=9 sha256=$sha")
            } finally {
                withContext(Dispatchers.Main) { controller.setFirmwareUpdateSessionActive(false) }
            }
        }
    }

    companion object { private const val TAG = "GUI_DFU_HARDWARE" }
}
