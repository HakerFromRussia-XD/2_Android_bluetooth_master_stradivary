package com.bailout.stickk.ubi4

import android.Manifest
import android.content.Intent
import android.os.SystemClock
import android.view.View
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_BINDING_DATA
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_GESTURE_GROUPE
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_BINDING_DATA
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_CURRENT_GESTURE_NUM
import com.bailout.stickk.ubi4.testing.V3BleEmulatorTestHooks
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V3GestureBindingEmulatorTest {

    @get:Rule
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @After
    fun tearDown() {
        V3BleEmulatorTestHooks.disable()
    }

    @Test
    fun v3GestureBindingTab_usesFakeBleReadbackWithoutRealDevice() {
        V3BleEmulatorTestHooks.reset()
        V3BleEmulatorTestHooks.enable()
        UiState.isInterfaceV3Activated = true
        UiState.v3WidgetsInteractionEnabled.value = true

        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            MainActivityUBI4::class.java
        ).apply {
            putExtra(ConstantManagerUBI4.EXTRAS_DEVICE_NAME, "FTHS3-EMU")
            putExtra(ConstantManagerUBI4.EXTRAS_DEVICE_ADDRESS, "00:11:22:33:44:55")
            putExtra(V3BleEmulatorTestHooks.EXTRA_ENABLED, true)
            putExtra(V3BleEmulatorTestHooks.EXTRA_OPEN_GESTURES, true)
        }

        ActivityScenario.launch<MainActivityUBI4>(intent).use {
            waitForView(withId(R.id.bindingGroupTv), 30_000)

            onView(withId(R.id.sprGesturesSelectBtn)).perform(click())
            waitForCondition("binding read request", 15_000) {
                V3BleEmulatorTestHooks.hasOutgoingSubcommand(PWCE_GET_BINDING_DATA.number.toInt())
            }
            waitForView(withId(R.id.selectedSprGesturesRv), 30_000)
            onView(withId(R.id.selectedSprGesturesRv)).check(matches(isDisplayed()))
            onView(withId(R.id.selectedSprGesturesRv)).check { view, error ->
                if (error != null) throw error
                val recycler = view as RecyclerView
                assertTrue("Expected injected binding rows", (recycler.adapter?.itemCount ?: 0) > 0)
            }

            onView(withId(R.id.selectedSprGesturesRv)).perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    0,
                    clickChildWithId(R.id.dotsThreeBtnSpr)
                )
            )
            waitForView(withId(R.id.dialogAddGesturesRv), 15_000)
            onView(withId(R.id.dialogAddGesturesRv)).perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    1,
                    clickChildWithId(R.id.ubi4DialogCollectionGestureItemBtn)
                )
            )
            onView(withId(R.id.dialogAddGesturesToSaveBtn)).perform(click())

            waitForCondition("binding set command", 15_000) {
                V3BleEmulatorTestHooks.hasOutgoingSubcommand(PWCE_SET_BINDING_DATA.number.toInt())
            }
            waitForCondition("binding readback after set", 15_000) {
                V3BleEmulatorTestHooks.bindingPairsSnapshot().firstOrNull()?.second == 2
            }
            assertEquals(2, V3BleEmulatorTestHooks.bindingPairsSnapshot().first().second)

            onView(withId(R.id.collectionOfGesturesSelectBtn)).perform(click())
            waitForView(withId(R.id.collectionGesturesCl), 15_000)
            onView(withId(R.id.gestureCollection0Btn)).perform(click())
            waitForCondition("active gesture set command", 15_000) {
                V3BleEmulatorTestHooks.hasOutgoingSubcommand(PWCE_SET_CURRENT_GESTURE_NUM.number.toInt())
            }

            onView(withId(R.id.rotationGroupSelectBtn)).perform(click())
            waitForCondition("rotation group read request", 15_000) {
                V3BleEmulatorTestHooks.hasOutgoingSubcommand(PWCE_GET_GESTURE_GROUPE.number.toInt())
            }
        }
    }

    private fun clickChildWithId(@IdRes id: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> = isAssignableFrom(View::class.java)
            override fun getDescription(): String = "click child view with id $id"

            override fun perform(uiController: UiController, view: View) {
                val child = view.findViewById<View>(id)
                    ?: throw PerformException.Builder()
                        .withActionDescription(description)
                        .withViewDescription(view.toString())
                        .withCause(IllegalStateException("Child view $id not found"))
                        .build()
                child.performClick()
                uiController.loopMainThreadUntilIdle()
            }
        }
    }

    private fun waitForView(matcher: Matcher<View>, timeoutMs: Long) {
        waitForCondition("view $matcher", timeoutMs) {
            try {
                onView(matcher).check(matches(isDisplayed()))
                true
            } catch (_: NoMatchingViewException) {
                false
            } catch (_: AssertionError) {
                false
            }
        }
    }

    private fun waitForCondition(label: String, timeoutMs: Long, condition: () -> Boolean) {
        val start = SystemClock.elapsedRealtime()
        var lastError: Throwable? = null
        while (SystemClock.elapsedRealtime() - start < timeoutMs) {
            try {
                if (condition()) return
            } catch (t: Throwable) {
                lastError = t
            }
            SystemClock.sleep(150)
        }
        throw AssertionError("Timed out waiting for $label", lastError)
    }
}
