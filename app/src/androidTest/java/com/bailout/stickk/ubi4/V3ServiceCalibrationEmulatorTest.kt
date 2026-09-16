package com.bailout.stickk.ubi4

import android.Manifest
import android.content.Intent
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.BLEState
import com.bailout.stickk.ubi4.testing.V3BleEmulatorTestHooks
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Fragment/ViewModel/adapter/queue, with all BLE intercepted by the existing debug hooks. */
@RunWith(AndroidJUnit4::class)
class V3ServiceCalibrationEmulatorTest {
    @get:Rule val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.POST_NOTIFICATIONS)

    @After fun tearDown() = V3BleEmulatorTestHooks.disable()
    @Test fun standardV3CalibrationUsesScreenStateAndExistingPackets() = checkCalibration("FTHS3-EMU", 9)
    @Test fun indy3CalibrationUsesScreenStateAndExistingPackets() = checkCalibration("INDY3-EMU", 6)

    private fun checkCalibration(name: String, expectedRows: Int) {
        V3BleEmulatorTestHooks.reset(); V3BleEmulatorTestHooks.enable()
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivityUBI4::class.java).apply {
            putExtra(ConstantManagerUBI4.EXTRAS_DEVICE_NAME, name)
            putExtra(ConstantManagerUBI4.EXTRAS_DEVICE_ADDRESS, "00:11:22:33:44:55")
            putExtra(V3BleEmulatorTestHooks.EXTRA_ENABLED, true)
        }
        ActivityScenario.launch<MainActivityUBI4>(intent).use { scenario ->
            waitUntil { runCatching { onView(withId(R.id.homeRv)).check(matches(isDisplayed())) }.isSuccess }
            // The existing debug launcher always seeds STANDARD_V3; use the real INDY3 generator for this fixture.
            if (name.startsWith("INDY3-")) runBlocking(Dispatchers.Main) {
                BLEState.bleParserV3.generatedHardcodeWidgetsINDY3()
            }
            scenario.onActivity { it.showSecretScreen() }
            waitUntil { runCatching { onView(withId(R.id.serviceFragmentRv)).check(matches(isDisplayed())) }.isSuccess }
            onView(withId(R.id.serviceFragmentRv)).check { view, error ->
                if (error != null) throw error
                assertEquals(expectedRows, (view as RecyclerView).adapter?.itemCount)
            }
            onView(withId(R.id.serviceFragmentRv)).perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withId(R.id.widget1Button))))
            onView(withId(R.id.widget1Button)).check(matches(isEnabled()))
            assertEquals(emptyList<Int>(), calibrationCommands())
            onView(withId(R.id.widget1Button)).perform(touch(MotionEvent.ACTION_DOWN))
            waitUntil { calibrationCommands() == listOf(3) }
            onView(withId(R.id.widget1Button)).perform(touch(MotionEvent.ACTION_UP))
            waitUntil { calibrationCommands() == listOf(3, 0) }

            onView(withId(R.id.widget1Button)).perform(touch(MotionEvent.ACTION_DOWN))
            waitUntil { calibrationCommands() == listOf(3, 0, 3) }
            scenario.onActivity { UiState.v3WidgetsInteractionEnabled.value = false }
            waitUntil { calibrationCommands() == listOf(3, 0, 3, 0) }
            onView(withId(R.id.widget1Button)).perform(touch(MotionEvent.ACTION_CANCEL))
            onView(withId(R.id.widget1Button)).check(matches(isNotEnabled()))
            scenario.onActivity { UiState.v3WidgetsInteractionEnabled.value = true }
            onView(withId(R.id.widget1Button)).check(matches(isEnabled()))
            assertEquals(listOf(3, 0, 3, 0), calibrationCommands())

            // The existing log entry remains a navigation action without a device command.
            onView(withId(R.id.serviceFragmentRv)).perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withId(R.id.bleLogClick))))
            onView(withId(R.id.bleLogClick)).perform(click())
            assertEquals(listOf(3, 0, 3, 0), calibrationCommands())
        }
    }

    private fun calibrationCommands() = V3BleEmulatorTestHooks.outgoingPacketsSnapshot().filter {
        it.size == 5 && it[1].toInt() == 15 && it[2].toInt() in setOf(0, 3)
    }.map { it[2].toInt() }

    private fun touch(action: Int) = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isDisplayed()
        override fun getDescription() = "dispatch calibration touch $action"
        override fun perform(uiController: UiController, view: View) {
            val now = SystemClock.uptimeMillis()
            val event = MotionEvent.obtain(now, now, action, view.width / 2f, view.height / 2f, 0)
            try { view.dispatchTouchEvent(event) } finally { event.recycle() }
            uiController.loopMainThreadUntilIdle()
        }
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        throw AssertionError("Timed out waiting for Service calibration UI/command")
    }
}
