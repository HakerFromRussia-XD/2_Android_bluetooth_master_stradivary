package com.bailout.stickk.ubi4

import android.Manifest
import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.data.network.FirmwareRepositoryProvider
import com.bailout.stickk.ubi4.data.network.HttpFirmwareRepository
import com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentUBI4.BootloaderBoardItemUBI4
import com.bailout.stickk.ubi4.ui.fragments.account.mainFragmentV3.AccountFragmentMainV3
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.testing.V3BleEmulatorTestHooks
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ProductionFirmwarePickerInstrumentedTest {
    @get:Rule
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.POST_NOTIFICATIONS
    )

    private val cacheDirectory =
        File(ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir, "http_firmware")

    @After
    fun tearDown() {
        V3BleEmulatorTestHooks.disable()
        cacheDirectory.deleteRecursively()
    }

    @Test
    fun accountFirmwarePickerDownloadsFromProductionSourceWithoutBluetooth() {
        assertTrue(FirmwareRepositoryProvider.active() is HttpFirmwareRepository)
        cacheDirectory.deleteRecursively()
        V3BleEmulatorTestHooks.reset()
        V3BleEmulatorTestHooks.enable()

        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivityUBI4::class.java
        ).apply {
            putExtra(ConstantManagerUBI4.EXTRAS_DEVICE_NAME, "FTHS3-EMU")
            putExtra(ConstantManagerUBI4.EXTRAS_DEVICE_ADDRESS, "00:11:22:33:44:55")
            putExtra(V3BleEmulatorTestHooks.EXTRA_ENABLED, true)
        }

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val activity = instrumentation.startActivitySync(
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ) as MainActivityUBI4
        try {
            instrumentation.runOnMainSync(activity::showAccountScreen)

            val fragment = waitForAccountFragment(activity)
            waitUntil("production firmware catalog") {
                catalogLoaded(fragment)
            }

            instrumentation.runOnMainSync {
                val method = AccountFragmentMainV3::class.java.getDeclaredMethod(
                    "showFirmwareFilesDialog",
                    BootloaderBoardItemUBI4::class.java
                )
                method.isAccessible = true
                method.invoke(
                    fragment,
                    BootloaderBoardItemUBI4(
                        boardName = "FAM",
                        deviceCode = 0,
                        deviceAddress = 0x00,
                        canUpdate = true,
                        version = "0.0.0"
                    )
                )
            }

            assertTrue(
                "Firmware picker was not displayed",
                device.wait(
                    Until.hasObject(By.res(APP_PACKAGE, "dialogFirmwareFileRv")),
                    UI_TIMEOUT_MS
                )
            )
            val firmwareRow = device.wait(
                Until.findObject(By.res(APP_PACKAGE, "ubi4DialogFileItemBtn")),
                UI_TIMEOUT_MS
            ) ?: throw AssertionError("Firmware row was not displayed")
            firmwareRow.click()

            assertTrue(
                "Firmware confirmation dialog was not displayed",
                device.wait(
                    Until.hasObject(By.res(APP_PACKAGE, "ubi4DialogConfirmSendFirmwareFileTitleTv")),
                    UI_TIMEOUT_MS
                )
            )
            waitUntil("downloaded production firmware ZIP") {
                cacheDirectory.listFiles()?.any { it.isFile && it.extension.equals("zip", true) && it.length() > 0L } == true
            }
            device.findObject(By.res(APP_PACKAGE, "ubi4DialogSendFirmwareCancelBtn"))?.click()
        } finally {
            instrumentation.runOnMainSync(activity::finish)
        }
    }

    private fun waitForAccountFragment(
        activity: MainActivityUBI4
    ): AccountFragmentMainV3 {
        var result: AccountFragmentMainV3? = null
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        waitUntil("account firmware screen") {
            instrumentation.runOnMainSync {
                result = activity.supportFragmentManager
                    .findFragmentById(R.id.fragmentContainer) as? AccountFragmentMainV3
            }
            result?.view != null
        }
        return requireNotNull(result)
    }

    private fun catalogLoaded(fragment: AccountFragmentMainV3): Boolean {
        val field = AccountFragmentMainV3::class.java.getDeclaredField("remoteFirmwareCatalog")
        field.isAccessible = true
        return (field.get(fragment) as? List<*>)?.isNotEmpty() == true
    }

    private fun waitUntil(description: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + UI_TIMEOUT_MS
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(100L)
        }
        throw AssertionError("Timed out waiting for $description")
    }

    private companion object {
        const val APP_PACKAGE = "com.bailout.stickk"
        const val UI_TIMEOUT_MS = 30_000L
    }
}
