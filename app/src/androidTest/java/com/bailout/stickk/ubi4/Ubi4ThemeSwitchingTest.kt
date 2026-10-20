package com.bailout.stickk.ubi4

import android.Manifest
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.core.view.WindowCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.di.ThemeDependencies
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.testing.V3BleEmulatorTestHooks
import com.bailout.stickk.ubi4.ui.fragments.SpecialSettingsFragment
import com.bailout.stickk.ubi4.ui.fragments.SprGestureFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Exercises a real theme-triggered Activity recreation, using the existing emulator BLE fixture. */
class Ubi4ThemeSwitchingTest {
    @get:Rule val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.POST_NOTIFICATIONS,
    )

    @Test fun themeSelectionRestoresSettingsAndNavigationAfterRecreation() {
        withEmulatedActivity("DARK") { scenario ->
            openMobileSettings(scenario)
            selectTheme(R.string.ubi4_theme_light)
            waitUntil { onView(withId(R.id.appearanceSelectedTheme)).check(matches(withText(R.string.ubi4_theme_light))) }
            assertTheme(scenario, light = true)
            screenshot("light")
            selectTheme(R.string.ubi4_theme_dark)
            waitUntil { onView(withId(R.id.appearanceSelectedTheme)).check(matches(withText(R.string.ubi4_theme_dark))) }
            assertTheme(scenario, light = false)
            screenshot("dark")
        }
    }

    @Test fun themeChangeRestoresLegacyGestureFragmentOnBackStack() {
        withEmulatedActivity("DARK") { scenario ->
            lateinit var originalGestureFragment: SprGestureFragment
            scenario.onActivity { activity ->
                // The emulator transport stays enabled. Only fragment restoration takes
                // the UBI4 branch that reads host preferences from Fragment.onCreate.
                activity.saveBoolean(PreferenceKeysUbi4.LAST_ACTIVE_SETTINGS_FILTER, true)
                val fragments = activity.supportFragmentManager
                originalGestureFragment = SprGestureFragment()
                fragments.beginTransaction()
                    .replace(R.id.fragmentContainer, originalGestureFragment, "legacy-gestures")
                    .commitNow()
                fragments.beginTransaction()
                    .replace(R.id.fragmentContainer, SpecialSettingsFragment(), "theme-settings")
                    .addToBackStack("settings-from-legacy-gestures")
                    .commit()
                fragments.executePendingTransactions()
                activity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                    .menu.findItem(R.id.page_4).isChecked = true
            }
            waitUntil { onView(withId(R.id.mobileSettingsBtn)).check(matches(isDisplayed())) }
            onView(withId(R.id.mobileSettingsBtn)).perform(click())
            waitUntil { onView(withId(R.id.appearanceSettingsRow)).check(matches(isDisplayed())) }
            scenario.onActivity { UiState.isInterfaceV3Activated = false }
            selectTheme(R.string.ubi4_theme_light)
            waitUntil { onView(withId(R.id.appearanceSelectedTheme)).check(matches(withText(R.string.ubi4_theme_light))) }
            assertTheme(scenario, light = true)
            scenario.onActivity { activity ->
                val fragments = activity.supportFragmentManager
                assertEquals(1, fragments.backStackEntryCount)
                assertTrue(fragments.findFragmentByTag("theme-settings") is SpecialSettingsFragment)
                val restoredGestureFragment = fragments.findFragmentByTag("legacy-gestures")
                assertTrue(restoredGestureFragment is SprGestureFragment)
                assertNotSame(originalGestureFragment, restoredGestureFragment)
                assertTrue(fragments.popBackStackImmediate())
                assertTrue(fragments.findFragmentById(R.id.fragmentContainer) is SprGestureFragment)
            }
            waitUntil { onView(withId(R.id.sprGesturesRv)).check(matches(isDisplayed())) }
            screenshot("gestures-light")
        }
    }

    @Test fun defaultSystemThemeFollowsDeviceAndExplicitChoicesOverrideIt() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        assumeEmulator()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val uiModeManager = instrumentation.targetContext.getSystemService(UiModeManager::class.java)
        val previousSystemMode = uiModeManager.nightMode
        try {
            setSystemNightMode(uiModeManager, UiModeManager.MODE_NIGHT_NO)
            withEmulatedActivity(initialMode = null) { scenario ->
                openMobileSettings(scenario)
                waitUntil { onView(withId(R.id.appearanceSelectedTheme)).check(matches(withText(R.string.ubi4_theme_system))) }
                waitUntil { assertTheme(scenario, light = true) }
                setSystemNightMode(uiModeManager, UiModeManager.MODE_NIGHT_YES)
                waitUntil { assertTheme(scenario, light = false) }
                selectTheme(R.string.ubi4_theme_light)
                waitUntil { onView(withId(R.id.appearanceSelectedTheme)).check(matches(withText(R.string.ubi4_theme_light))) }
                waitUntil { assertTheme(scenario, light = true) }
                setSystemNightMode(uiModeManager, UiModeManager.MODE_NIGHT_NO)
                waitUntil { assertTheme(scenario, light = true) }
                selectTheme(R.string.ubi4_theme_dark)
                waitUntil { onView(withId(R.id.appearanceSelectedTheme)).check(matches(withText(R.string.ubi4_theme_dark))) }
                waitUntil { assertTheme(scenario, light = false) }
                selectTheme(R.string.ubi4_theme_system)
                waitUntil { onView(withId(R.id.appearanceSelectedTheme)).check(matches(withText(R.string.ubi4_theme_system))) }
                waitUntil { assertTheme(scenario, light = true) }
            }
        } finally {
            setSystemNightMode(uiModeManager, previousSystemMode)
        }
    }

    private fun withEmulatedActivity(initialMode: String?, test: (ActivityScenario<MainActivityUBI4>) -> Unit) {
        assumeEmulator()
        val previousInterfaceV3 = UiState.isInterfaceV3Activated
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = context.getSharedPreferences("ubi4_appearance", Context.MODE_PRIVATE)
        val previousMode = preferences.getString("theme_mode", null)
        val devicePreferences = context.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE)
        val previousMobileSettings = if (devicePreferences.contains(PreferenceKeysUbi4.LAST_ACTIVE_SETTINGS_FILTER)) {
            devicePreferences.getBoolean(PreferenceKeysUbi4.LAST_ACTIVE_SETTINGS_FILTER, false)
        } else null
        preferences.edit().putString("theme_mode", initialMode).commit()
        instrumentation.runOnMainSync { ThemeDependencies.applySavedTheme(context) }
        V3BleEmulatorTestHooks.reset()
        V3BleEmulatorTestHooks.enable()
        val intent = Intent(context, MainActivityUBI4::class.java).apply {
            putExtra(ConstantManagerUBI4.EXTRAS_DEVICE_NAME, "FTHS3-EMU")
            putExtra(ConstantManagerUBI4.EXTRAS_DEVICE_ADDRESS, "00:11:22:33:44:55")
            putExtra(V3BleEmulatorTestHooks.EXTRA_ENABLED, true)
        }
        try {
            ActivityScenario.launch<MainActivityUBI4>(intent).use { scenario ->
                test(scenario)
            }
        } finally {
            V3BleEmulatorTestHooks.disable()
            preferences.edit().putString("theme_mode", previousMode).commit()
            devicePreferences.edit().apply {
                if (previousMobileSettings == null) remove(PreferenceKeysUbi4.LAST_ACTIVE_SETTINGS_FILTER)
                else putBoolean(PreferenceKeysUbi4.LAST_ACTIVE_SETTINGS_FILTER, previousMobileSettings)
            }.commit()
            instrumentation.runOnMainSync {
                UiState.isInterfaceV3Activated = previousInterfaceV3
                ThemeDependencies.applySavedTheme(context)
            }
        }
    }

    private fun assumeEmulator() {
        assumeTrue(Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("sdk_gphone"))
    }

    private fun openMobileSettings(scenario: ActivityScenario<MainActivityUBI4>) {
        scenario.onActivity {
            it.findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId = R.id.page_4
        }
        waitUntil { onView(withId(R.id.mobileSettingsBtn)).check(matches(isDisplayed())) }
        onView(withId(R.id.mobileSettingsBtn)).perform(click())
        waitUntil { onView(withId(R.id.appearanceSettingsRow)).check(matches(isDisplayed())) }
    }

    private fun assertTheme(scenario: ActivityScenario<MainActivityUBI4>, light: Boolean) {
        scenario.onActivity {
            assertEquals(
                if (light) Configuration.UI_MODE_NIGHT_NO else Configuration.UI_MODE_NIGHT_YES,
                it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK,
            )
            assertEquals(Color.parseColor(if (light) "#F4F5F2" else "#2A2A2A"), it.getColor(R.color.ubi4_back))
            assertEquals(Color.parseColor("#C6F158"), it.getColor(R.color.ubi4_active))
            val bars = WindowCompat.getInsetsController(it.window, it.window.decorView)
            assertEquals(light, bars.isAppearanceLightStatusBars)
            assertEquals(light, bars.isAppearanceLightNavigationBars)
            assertEquals(R.id.page_4, it.findViewById<BottomNavigationView>(R.id.bottom_navigation).selectedItemId)
        }
    }

    private fun setSystemNightMode(uiModeManager: UiModeManager, mode: Int) {
        val value = when (mode) {
            UiModeManager.MODE_NIGHT_NO -> "no"
            UiModeManager.MODE_NIGHT_YES -> "yes"
            UiModeManager.MODE_NIGHT_AUTO -> "auto"
            else -> "custom"
        }
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("cmd uimode night $value")).use {
            it.bufferedReader().readText()
        }
        assertEquals(mode, uiModeManager.nightMode)
    }

    private fun selectTheme(label: Int) {
        onView(withId(R.id.appearanceSettingsRow)).perform(click())
        onView(withText(label)).perform(click())
    }

    private fun screenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        SystemClock.sleep(300) // Allow the system bars' color transition to finish before capturing pixels.
        val directory = requireNotNull(instrumentation.targetContext.getExternalFilesDir("theme-previews"))
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    private fun waitUntil(check: () -> Unit) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        var failure: Throwable? = null
        while (SystemClock.elapsedRealtime() < deadline) {
            runCatching(check).onSuccess { return }.onFailure { failure = it }
            SystemClock.sleep(100)
        }
        throw AssertionError("Timed out waiting for themed settings", failure)
    }
}
