package com.bailout.stickk.ubi4

import android.Manifest
import android.content.Intent
import android.os.SystemClock
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.testing.V3BleEmulatorTestHooks
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** All commands are intercepted locally; no physical prosthesis is used. */
@RunWith(AndroidJUnit4::class)
class V3InitializationCommandsEmulatorTest {
    @get:Rule val permissions = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.POST_NOTIFICATIONS)
    @After fun tearDown() = V3BleEmulatorTestHooks.disable()

    @Test fun eachButtonQueuesOneUniqueCommandInScreenshotOrder() {
        V3BleEmulatorTestHooks.reset()
        V3BleEmulatorTestHooks.enable()
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivityUBI4::class.java).apply {
            putExtra(ConstantManagerUBI4.EXTRAS_DEVICE_NAME, "FTHS3-EMU")
            putExtra(ConstantManagerUBI4.EXTRAS_DEVICE_ADDRESS, "00:11:22:33:44:55")
            putExtra(V3BleEmulatorTestHooks.EXTRA_ENABLED, true)
        }
        ActivityScenario.launch<MainActivityUBI4>(intent).use { scenario ->
            waitUntil { runCatching { onView(withId(R.id.homeRv)).check(matches(isDisplayed())) }.isSuccess }
            scenario.onActivity { it.showSecretScreen() }
            waitUntil { runCatching { onView(withId(R.id.serviceFragmentRv)).check(matches(isDisplayed())) }.isSuccess }
            onView(withId(R.id.serviceFragmentRv)).check { view, error ->
                if (error != null) throw error
                assertEquals(27, (view as RecyclerView).adapter?.itemCount)
            }
            val before = V3BleEmulatorTestHooks.outgoingPacketsSnapshot().size
            for (index in 0 until 18) {
                onView(withId(R.id.serviceFragmentRv)).perform(
                    RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(9 + index))
                val rowTitle = String.format("%02d. ", index + 1) + title(index)
                onView(withText(rowTitle)).check(matches(isDisplayed()))
                if (index == 0 || index == 17) capturePreview(index)
                onView(allOf(withId(R.id.sendCommand), withParent(hasDescendant(withText(rowTitle)))))
                    .check(matches(isEnabled())).perform(click())
                waitUntil { V3BleEmulatorTestHooks.outgoingPacketsSnapshot().size == before + index + 1 }
            }
            val packets = V3BleEmulatorTestHooks.outgoingPacketsSnapshot().drop(before)
            assertEquals(listOf(
                6 to 1, 16 to 33, 15 to 48, 18 to 2, 18 to 6, 18 to 4, 15 to 22,
                16 to 13, 15 to 58, 16 to 15, 16 to 16, 15 to 60, 15 to 46, 15 to 62,
                15 to 64, 1 to 10, 15 to 69, 15 to 71,
            ), packets.map { (it[1].toInt() and 255) to (it[if (it.size > 5) 4 else 2].toInt() and 255) })
            scenario.onActivity { BLEState.publishDisconnect() }
            onView(allOf(withId(R.id.sendCommand), withParent(hasDescendant(withText("18. " + title(17)))))).check(matches(isNotEnabled()))
            assertEquals(before + 18, V3BleEmulatorTestHooks.outgoingPacketsSnapshot().size)
        }
    }

    private fun capturePreview(index: Int) {
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val output = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "init-commands-$index.png")
        output.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun title(index: Int) = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.getString(intArrayOf(
        R.string.v3_init_device_data, R.string.v3_init_phone_date_time, R.string.v3_init_threshold,
        R.string.v3_init_emg_gain, R.string.v3_init_emg_mode, R.string.v3_init_emg_max_gain,
        R.string.v3_init_emg_change_gesture, R.string.v3_init_screen_timeout, R.string.v3_init_movement_lock,
        R.string.v3_init_hand_side, R.string.v3_init_battery, R.string.v3_init_hand_control_mode,
        R.string.v3_init_gesture_change_mode, R.string.v3_init_speed, R.string.v3_init_force,
        R.string.v3_init_serial_number, R.string.v3_init_thumb_position, R.string.v3_init_fingers_position,
    )[index])

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        throw AssertionError("Timed out waiting for initialization command UI/packet")
    }
}
