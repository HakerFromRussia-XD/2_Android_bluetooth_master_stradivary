package com.bailout.stickk.ubi4

import android.Manifest
import android.content.Intent
import android.graphics.Rect
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.firmware.user.UserFirmwareUiState
import com.bailout.stickk.ubi4.shared.SharedRes
import com.bailout.stickk.ubi4.testing.V3BleEmulatorTestHooks
import com.bailout.stickk.ubi4.ui.dialog.UserFirmwareUpdateDialog
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Checks actual window layout without connecting to or flashing a device. */
@RunWith(AndroidJUnit4::class)
class UserFirmwareUpdateDialogTest {
    @get:Rule val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.POST_NOTIFICATIONS,
    )

    @After fun tearDown() = V3BleEmulatorTestHooks.disable()

    @Test fun offerAndCompletionHaveVisibleContent() {
        V3BleEmulatorTestHooks.reset()
        V3BleEmulatorTestHooks.enable()
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivityUBI4::class.java).apply {
            putExtra(ConstantManagerUBI4.EXTRAS_DEVICE_NAME, "FTHS3-EMU")
            putExtra(ConstantManagerUBI4.EXTRAS_DEVICE_ADDRESS, "00:11:22:33:44:55")
            putExtra(V3BleEmulatorTestHooks.EXTRA_ENABLED, true)
        }
        ActivityScenario.launch<MainActivityUBI4>(intent).use { scenario ->
            lateinit var fragment: UserFirmwareUpdateDialog
            scenario.onActivity { activity ->
                fragment = UserFirmwareUpdateDialog()
                fragment.showNow(activity.supportFragmentManager, "firmware_dialog_layout_test")
                fragment.render(UserFirmwareUiState(phase = "offered", boardCount = 3))
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertContentVisible(fragment, activity.getString(SharedRes.strings.user_firmware_offer.resourceId))
                fragment.render(UserFirmwareUiState(phase = "updating", boardCount = 3, progress = 50))
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity {
                fragment.render(UserFirmwareUiState(phase = "complete", boardCount = 3, progress = 100))
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                assertContentVisible(fragment, activity.getString(SharedRes.strings.user_firmware_complete.resourceId))
                fragment.dismissNow()
            }
        }
    }

    private fun assertContentVisible(fragment: UserFirmwareUpdateDialog, expectedMessage: String) {
        val dialog = fragment.requireDialog()
        val windowBounds = Rect()
        assertTrue("Dialog window must have a visible area", dialog.window!!.decorView.getGlobalVisibleRect(windowBounds))
        for (id in listOf(R.id.ubi4DialogRotationGroupTitleTv, R.id.ubi4DialogRotationGroupMessageTv, R.id.ubi4CompletedTrainingBtn)) {
            val bounds = Rect()
            assertTrue("Dialog content $id must be visible", dialog.findViewById<android.view.View>(id).getGlobalVisibleRect(bounds))
            assertTrue("Dialog content $id must fit inside the window", windowBounds.contains(bounds))
        }
        assertEquals(expectedMessage, dialog.findViewById<TextView>(R.id.ubi4DialogRotationGroupMessageTv).text.toString())
    }
}
