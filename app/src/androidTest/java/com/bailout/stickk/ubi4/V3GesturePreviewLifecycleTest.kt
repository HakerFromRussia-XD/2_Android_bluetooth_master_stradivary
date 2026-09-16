package com.bailout.stickk.ubi4

import android.Manifest
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.testing.V3BleEmulatorTestHooks
import com.bailout.stickk.ubi4.ui.fragments.SensorsFragment
import com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3.CollectionGestureCardGLSurfaceViewV3
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

/** Exercises actual EGL creation/destruction; no real BLE connection is established. */
@RunWith(AndroidJUnit4::class)
class V3GesturePreviewLifecycleTest {
    @get:Rule val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.POST_NOTIFICATIONS)

    @After fun tearDown() = V3BleEmulatorTestHooks.disable()

    @Test fun previewSurvivesRepeatedNavigationAndSurfaceReattachment() {
        V3BleEmulatorTestHooks.reset(); V3BleEmulatorTestHooks.enable()
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivityUBI4::class.java).apply {
            putExtra(ConstantManagerUBI4.EXTRAS_DEVICE_NAME, "FTHS3-EMU")
            putExtra(ConstantManagerUBI4.EXTRAS_DEVICE_ADDRESS, "00:11:22:33:44:55")
            putExtra(V3BleEmulatorTestHooks.EXTRA_ENABLED, true)
        }
        ActivityScenario.launch<MainActivityUBI4>(intent).use { scenario ->
            waitUntil {
                var ready = false
                scenario.onActivity { ready = it.supportFragmentManager.findFragmentById(R.id.fragmentContainer) is SensorsFragment }
                ready
            }
            scenario.onActivity {
                it.saveInt(PreferenceKeysUbi4.LAST_ACTIVE_GESTURE_FILTER, 1)
                it.saveInt(PreferenceKeysUbi4.LAST_HIDE_COLLECTION_BTN_STATE, 1)
            }
            repeat(12) { iteration ->
                navigate(scenario, R.id.page_1)
                awaitFrame(scenario, "navigation $iteration")
                scenario.onActivity {
                    it.findViewById<View>(R.id.gestureCollection0PlayBtn).performClick()
                }
                // Same TextureView receives a new SurfaceTexture while its old GL worker is shutting down.
                if (iteration % 3 == 0) {
                    scenario.onActivity {
                        val preview = it.findViewById<CollectionGestureCardGLSurfaceViewV3>(R.id.gestureCollection0GlView)
                        val parent = preview.parent as ViewGroup
                        val position = parent.indexOfChild(preview)
                        parent.removeView(preview)
                        parent.addView(preview, position)
                    }
                    awaitFrame(scenario, "reattachment $iteration")
                }
                navigate(scenario, if (iteration % 2 == 0) R.id.page_2 else R.id.page_4)
            }
            navigate(scenario, R.id.page_1)
            awaitFrame(scenario, "final navigation")
            assertTrue(V3BleEmulatorTestHooks.outgoingPacketsSnapshot().none {
                it.size == 5 && it[1].toInt() == 15 && it[2].toInt() == 37
            })
        }
    }

    private fun navigate(scenario: ActivityScenario<MainActivityUBI4>, itemId: Int) {
        // Invoke the real menu item listener without racing the bottom bar's entrance animation.
        scenario.onActivity {
            val item = it.findViewById<View>(itemId)
            assertTrue("Navigation item must be visible", item.isShown)
            assertTrue("Navigation item must handle the click", item.performClick())
        }
    }

    private fun awaitFrame(scenario: ActivityScenario<MainActivityUBI4>, step: String) {
        val ready = AtomicBoolean()
        var lastState = ""
        waitUntil({ "$step: $lastState" }) {
            // A normal widget rebind clears preview listeners. Subscribe again while waiting.
            scenario.onActivity {
                val preview = it.findViewById<CollectionGestureCardGLSurfaceViewV3>(R.id.gestureCollection0GlView)
                val fragment = it.supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                val state = "${fragment?.javaClass?.simpleName}, preview=${preview != null}, " +
                    "shown=${preview?.isShown}, available=${preview?.isAvailable}, size=${preview?.width}x${preview?.height}"
                if (state != lastState) Log.i("V3PreviewLifecycleTest", "$step: $state")
                lastState = state
                preview?.setOnFirstFrameReadyListener { ready.set(true) }
            }
            ready.get()
        }
    }

    private fun waitUntil(description: () -> String = { "initial screen" }, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 30_000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        throw AssertionError("Timed out waiting for ${description()}")
    }
}
