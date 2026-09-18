package com.bailout.stickk.ubi4

import android.Manifest
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.ViewAction
import androidx.test.espresso.UiController
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.Press
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.state.*
import com.bailout.stickk.ubi4.models.ble.RotationGroupV3
import com.bailout.stickk.ubi4.models.ble.CurrentGestureV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.testing.V3BleEmulatorTestHooks
import com.bailout.stickk.ubi4.ui.fragments.SensorsFragment
import com.bailout.stickk.ubi4.ui.fragments.SprGestureFragment
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3.UBI4GripperScreenWithEncodersActivityV3
import com.bailout.stickk.ubi4.utility.CollectionGesturesProvider
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_GROUPE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_CURRENT_GESTURE
import com.bailout.stickk.ubi4.versions.v3.presentation.gestures.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Enters through the real bottom navigation; all device packets go to debug BLE hooks. */
@RunWith(AndroidJUnit4::class)
class V3ActiveGestureEmulatorTest {
    @get:Rule val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.POST_NOTIFICATIONS)

    @After fun tearDown() = V3BleEmulatorTestHooks.disable()
    @Test fun standardV3ActiveGestureUsesScreenState() = checkGestures("FTHS3-EMU")
    @Test fun indy3KeepsGestureTabHidden() = checkGestures("INDY3-EMU")

    private fun checkGestures(name: String) {
        V3BleEmulatorTestHooks.reset(); V3BleEmulatorTestHooks.enable()
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivityUBI4::class.java).apply {
            putExtra(ConstantManagerUBI4.EXTRAS_DEVICE_NAME, name)
            putExtra(ConstantManagerUBI4.EXTRAS_DEVICE_ADDRESS, "00:11:22:33:44:55")
            putExtra(V3BleEmulatorTestHooks.EXTRA_ENABLED, true)
        }
        ActivityScenario.launch<MainActivityUBI4>(intent).use { scenario ->
            waitUntil {
                var ready = false
                scenario.onActivity { ready = it.supportFragmentManager.findFragmentById(R.id.fragmentContainer) is SensorsFragment }
                ready
            }
            if (name.startsWith("INDY3-")) runBlocking(Dispatchers.Main) {
                BLEState.bleParserV3.generatedHardcodeWidgetsINDY3()
            }
            var expectedRows = 0
            scenario.onActivity {
                it.saveInt(PreferenceKeysUbi4.LAST_ACTIVE_GESTURE_FILTER, 1)
                it.saveInt(PreferenceKeysUbi4.LAST_HIDE_COLLECTION_BTN_STATE, 1)
                // Both existing name readers need the same stored MAC and names instead of their different empty-preference defaults.
                it.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, 0).edit().apply {
                    val mac = "00:11:22:33:44:55"
                    putString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, mac)
                    repeat(PreferenceKeysUbi4.NUM_GESTURES) { index ->
                        putString(PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM + mac + index, "Gesture ${index + 1}")
                    }
                }.apply()
                expectedRows = DataFactory().prepareData(0).size
                it.refreshBottomNavVisibility()
            }
            if (name.startsWith("INDY3-")) {
                assertEquals(0, expectedRows)
                scenario.onActivity {
                    assertFalse(it.findViewById<View>(R.id.page_1).isShown)
                    assertTrue(it.supportFragmentManager.findFragmentById(R.id.fragmentContainer) is SensorsFragment)
                    val repository = com.bailout.stickk.ubi4.versions.v3.data.gestures.V3GesturesRepositoryImpl(enqueuePacket = {
                        fail("Absent gesture parameter must not enqueue a packet")
                    })
                    assertFalse(repository.getActiveGesture().isInteractionEnabled)
                    assertFalse(repository.requestActiveGesture("00:11:22:33:44:55"))
                    assertFalse(repository.selectGesture("00:11:22:33:44:55", 2))
                }
                assertTrue(selections().isEmpty())
                return
            }
            clickCard(scenario, R.id.page_1)
            waitForState(scenario, V3GesturesUiState(1, true))
            lateinit var vm: V3GesturesViewModel
            scenario.onActivity {
                val fragment = it.supportFragmentManager.findFragmentById(R.id.fragmentContainer) as SprGestureFragment
                vm = ViewModelProvider(fragment)[V3GesturesViewModel::class.java]
                assertEquals(expectedRows, it.findViewById<RecyclerView>(R.id.sprGesturesRv).adapter?.itemCount)
                assertEquals(expectedRows, vm.uiState.value.widgets.size)
                assertEquals(DataFactory().prepareData(0),
                    com.bailout.stickk.ubi4.versions.v3.presentation.gestures.widgets.V3GesturesWidgetMapper()
                        .toItems(vm.uiState.value.widgets))
                assertHeader(it, 1)
            }
            assertTrue(selections().isEmpty())
            checkRotationGroupState(scenario, vm)

            // Collection card 11 represents protocol gesture 13; 12 is intentionally absent.
            clickCard(scenario, R.id.gestureCollection11Btn)
            waitUntil { selections() == listOf(13) }
            waitForState(scenario, V3GesturesUiState(13, true))
            scenario.onActivity { assertHeader(it, 13) }
            clickCard(scenario, R.id.gestureCustom1Btn)
            waitUntil { selections() == listOf(13, 64) }
            waitForState(scenario, V3GesturesUiState(64, true))
            SystemClock.sleep(150) // Let the fake SET response settle before injecting device input.

            scenario.onActivity {
                ParameterStoreV3.put(ParameterInfoRegistry.require(P_KEY_CURRENT_GESTURE),
                    ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(4)))
            }
            waitForState(scenario, V3GesturesUiState(4, true))
            scenario.onActivity { assertHeader(it, 4); UiState.v3WidgetsInteractionEnabled.value = false }
            waitForState(scenario, V3GesturesUiState())
            scenario.onActivity {
                assertFalse(it.findViewById<View>(R.id.gestureCollection0Btn).isEnabled)
                assertEquals(it.getString(R.string.active_gesture_is, "Unknow"), it.findViewById<TextView>(R.id.activeGestureNameTv).text.toString())
                it.findViewById<View>(R.id.gestureCollection0Btn).performClick()
                vm.onAction(V3GesturesAction.GestureSelected(2))
                ParameterStoreV3.put(ParameterInfoRegistry.require(P_KEY_CURRENT_GESTURE),
                    ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(7)))
            }
            waitForState(scenario, V3GesturesUiState())
            assertEquals(listOf(13, 64), selections())
            scenario.onActivity { UiState.v3WidgetsInteractionEnabled.value = true }
            waitForState(scenario, V3GesturesUiState(64, true)) // Existing GET reads the emulator's current value.

            scenario.onActivity {
                val manager = it.supportFragmentManager
                val fragment = manager.findFragmentById(R.id.fragmentContainer)!!
                val composition = vm.uiState.value
                manager.beginTransaction().detach(fragment).commitNow()
                assertEquals(V3GesturesUiState(widgets = composition.widgets, widgetsUpdateId = composition.widgetsUpdateId,
                    customGestureNames = composition.customGestureNames), vm.uiState.value)
                vm.onAction(V3GesturesAction.GestureSelected(2))
                manager.beginTransaction().attach(fragment).commitNow()
                assertSame(vm, ViewModelProvider(fragment)[V3GesturesViewModel::class.java])
            }
            waitForState(scenario, V3GesturesUiState(64, true))
            // A list rebind must retain the screen state without a new SET.
            scenario.onActivity {
                val recycler = it.findViewById<RecyclerView>(R.id.sprGesturesRv)
                recycler.adapter?.notifyDataSetChanged()
            }
            SystemClock.sleep(150)
            scenario.onActivity { assertTrue(it.findViewById<View>(R.id.gestureCollection0Btn).isEnabled) }
            assertEquals(listOf(13, 64), selections())
            clickCard(scenario, R.id.gestureCollection0Btn)
            waitUntil { selections() == listOf(13, 64, 1) }

            clickCard(scenario, R.id.page_2)
            scenario.onActivity {
                vm.onAction(V3GesturesAction.ViewAttached)
                vm.onAction(V3GesturesAction.GestureSelected(15))
            }
            SystemClock.sleep(150)
            assertEquals(listOf(13, 64, 1), selections())
            clickCard(scenario, R.id.page_1)
            waitForState(scenario, V3GesturesUiState(1, true))
            assertEquals(listOf(13, 64, 1), selections())

            // Wait for this GET before injecting the removal fixture; its delayed response
            // otherwise can replace that fixture with the emulator's original group.
            runBlocking {
                val response = async(Dispatchers.Main, start = CoroutineStart.UNDISPATCHED) {
                    val key = ParameterStoreV3.toKey(ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE))
                    ParameterStoreV3.updates.first { it == key }
                }
                try {
                    clickCard(scenario, R.id.rotationGroupSelectBtn)
                    withTimeout(15_000) { response.await() }
                } finally {
                    response.cancel()
                }
            }
            waitUntil {
                var ready = false
                scenario.onActivity {
                    val list = it.findViewById<com.woxthebox.draglistview.DragListView>(R.id.rotationGroupDragLv).recyclerView
                    ready = list.findViewHolderForAdapterPosition(1) != null
                }
                ready
            }
            scenario.onActivity {
                val list = it.findViewById<com.woxthebox.draglistview.DragListView>(R.id.rotationGroupDragLv).recyclerView
                list.findViewHolderForAdapterPosition(1)!!.itemView.findViewById<View>(R.id.gestureInRotationGroupTv).performClick()
            }
            waitUntil { selections() == listOf(13, 64, 1, 2) }
            waitForState(scenario, V3GesturesUiState(2, true))
            scenario.onActivity { assertHeader(it, 2) }
            checkRotationGroupRemoval(scenario)
            checkRotationGroupSelection(scenario)
            checkRotationGroupOrder(scenario)
            checkGesturesDisplaySettings(scenario)
            checkGestureSettingsEntry(scenario)
        }
    }

    private fun checkGestureSettingsEntry(scenario: ActivityScenario<MainActivityUBI4>) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val selectionBefore = selections()
        val rotationWritesBefore = rotationWrites().size
        val preferences = instrumentation.targetContext.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, 0)
        val launches = mutableListOf<Pair<Intent, Int>>()
        // Test the navigation boundary without the existing editor's GPU-dependent renderer.
        // Its full launch is a separate device check; it also sends a finger-opening command after BLE data arrives.
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.component?.className != UBI4GripperScreenWithEncodersActivityV3::class.java.name) return null
                launches.add(Intent(intent) to preferences.getInt(PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM, -1))
                return Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null)
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            for (number in listOf(1, 14, 1)) {
                val gearId = if (number == 1) R.id.gesture1SettingsBtn else R.id.gesture14SettingsBtn
                val countBefore = launches.size
                val savedBefore = preferences.getInt(PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM, -1)
                scenario.onActivity { UiState.v3WidgetsInteractionEnabled.value = false }
                waitForDisplaySettings(scenario, section = 1, expanded = true, enabled = false)
                scenario.onActivity {
                    assertFalse(it.findViewById<View>(gearId).isEnabled)
                    it.findViewById<View>(gearId).performClick()
                    assertEquals(countBefore, launches.size)
                    UiState.v3WidgetsInteractionEnabled.value = true
                }
                waitForDisplaySettings(scenario, section = 1, expanded = true)
                clickCard(scenario, gearId)
                instrumentation.waitForIdleSync()
                assertEquals(countBefore + 1, launches.size)
                val (intent, numberAtLaunch) = launches.last()
                assertTrue(intent.getBooleanExtra(UBI4GripperScreenWithEncodersActivityV3.EXTRA_USE_V3_GESTURE_PROTOCOL, false))
                assertEquals(ParameterInfoRegistry.require(ConstantManagerUBI4.P_KEY_GESTURE_SETTING).dataCode,
                    intent.getIntExtra(PreferenceKeysUbi4.PARAMETER_ID_IN_SYSTEM_UBI4, -1))
                assertEquals(63 + number, intent.getIntExtra(PreferenceKeysUbi4.GESTURE_ID_IN_SYSTEM_UBI4, -1))
                assertFalse(intent.hasExtra(PreferenceKeysUbi4.DEVICE_ID_IN_SYSTEM_UBI4))
                assertEquals("Keep launch before saving, as in the original click", savedBefore, numberAtLaunch)
                assertEquals(number, preferences.getInt(PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM, -1))
                scenario.onActivity {
                    val fragment = it.supportFragmentManager.findFragmentById(R.id.fragmentContainer) as SprGestureFragment
                    assertNull(ViewModelProvider(fragment)[V3GesturesViewModel::class.java].uiState.value.gestureSettings)
                    it.findViewById<RecyclerView>(R.id.sprGesturesRv).adapter?.notifyDataSetChanged()
                }
                waitForDisplaySettings(scenario, section = 1, expanded = true)
                clickCard(scenario, R.id.page_2)
                clickCard(scenario, R.id.page_1)
                waitForDisplaySettings(scenario, section = 1, expanded = true)
                assertEquals(countBefore + 1, launches.size)
                assertEquals(number, preferences.getInt(PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM, -1))
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
        assertEquals(selectionBefore, selections())
        assertEquals(rotationWritesBefore, rotationWrites().size)
    }

    private fun checkRotationGroupState(scenario: ActivityScenario<MainActivityUBI4>, vm: V3GesturesViewModel) {
        val originalUbi4Group = WidgetState.rotationGroupGestures
        val unrelatedUbi4Group = arrayListOf(CollectionGesturesProvider.getGesture(15))
        scenario.onActivity { WidgetState.rotationGroupGestures = unrelatedUbi4Group }
        try {
            val info = ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE)
            val original = ParameterStoreV3.get(info)!!
            assertEquals((1..8).toList(), vm.uiState.value.rotationGroupGestureIds)
            val requestsBefore = rotationReads()
            clickCard(scenario, R.id.rotationGroupSelectBtn)
            waitUntil { rotationReads() > requestsBefore }
            SystemClock.sleep(900) // An identical GET response must stop the 400 ms retries.
            assertEquals(requestsBefore + 1, rotationReads())
            scenario.onActivity {
                assertEquals(View.GONE, it.findViewById<View>(R.id.addGestureToRotationGroupBtn).visibility)
                ParameterStoreV3.put(info, ParameterTypedValueV3.RotationGroup(
                    RotationGroupV3(gesture1Id = 4, gesture2Id = 64, gesture3Id = 4)))
            }
            waitUntil { vm.uiState.value.rotationGroupGestureIds == listOf(4, 64, 4) }
            waitUntil {
                var rendered = false
                scenario.onActivity {
                    val list = it.findViewById<com.woxthebox.draglistview.DragListView>(R.id.rotationGroupDragLv).recyclerView
                    rendered = list.adapter?.itemCount == 3 && (0..2).all { position ->
                        val name = CollectionGesturesProvider.getGesture(listOf(4, 64, 4)[position]).gestureName
                        list.findViewHolderForAdapterPosition(position)?.itemView
                            ?.findViewById<TextView>(R.id.gestureInRotationGroupTv)?.text?.toString() == name
                    }
                }
                rendered
            }
            scenario.onActivity {
                it.findViewById<RecyclerView>(R.id.sprGesturesRv).adapter?.notifyDataSetChanged()
            }
            SystemClock.sleep(500)
            assertEquals(requestsBefore + 1, rotationReads())
            scenario.onActivity { ParameterStoreV3.put(info, ParameterTypedValueV3.RotationGroup(RotationGroupV3())) }
            waitUntil {
                var empty = false
                scenario.onActivity {
                    val list = it.findViewById<com.woxthebox.draglistview.DragListView>(R.id.rotationGroupDragLv).recyclerView
                    empty = vm.uiState.value.rotationGroupGestureIds.isEmpty() && list.adapter?.itemCount == 0 &&
                        it.findViewById<View>(R.id.rotationGroupExplanationTv).visibility == View.VISIBLE &&
                        it.findViewById<View>(R.id.addGestureToRotationGroupBtn).visibility == View.VISIBLE
                }
                empty
            }
            scenario.onActivity { ParameterStoreV3.put(info, original) }
            waitUntil { vm.uiState.value.rotationGroupGestureIds == (1..8).toList() }
            clickCard(scenario, R.id.collectionOfGesturesSelectBtn)
            assertFalse(V3BleEmulatorTestHooks.hasOutgoingSubcommand(0x36))
            assertTrue(selections().isEmpty())
            scenario.onActivity {
                assertSame(unrelatedUbi4Group, WidgetState.rotationGroupGestures)
                assertEquals(listOf(15), WidgetState.rotationGroupGestures.map { gesture -> gesture.gestureId })
            }
        } finally {
            scenario.onActivity { WidgetState.rotationGroupGestures = originalUbi4Group }
        }
    }

    private fun checkRotationGroupRemoval(scenario: ActivityScenario<MainActivityUBI4>) {
        lateinit var vm: V3GesturesViewModel
        scenario.onActivity {
            val fragment = it.supportFragmentManager.findFragmentById(R.id.fragmentContainer) as SprGestureFragment
            vm = ViewModelProvider(fragment)[V3GesturesViewModel::class.java]
            ParameterStoreV3.put(ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE),
                ParameterTypedValueV3.RotationGroup(RotationGroupV3(gesture1Id = 4, gesture2Id = 64, gesture3Id = 4)))
        }
        waitForRotationRows(scenario, vm, listOf(4, 64, 4))
        val selectionsBefore = selections()
        clickRotationDelete(scenario, 0)
        waitUntil { vm.uiState.value.rotationGestureRemoval != null }
        val firstRequest = requireNotNull(vm.uiState.value.rotationGestureRemoval).requestId
        lateinit var expectedMessage: String
        scenario.onActivity {
            val name = CollectionGesturesProvider.getGesture(4).gestureName
            expectedMessage = it.getString(R.string.the_that_rocks_gesture_will_remain_available_in_the_gesture_collection_but_will_be_removed_from_the_rotation_group, "\"$name\"")
        }
        onView(withId(R.id.ubi4DialogRotationGroupMessageTv)).inRoot(isDialog()).check(matches(withText(expectedMessage)))
        onView(withId(R.id.ubi4DialogRotationGroupCancelBtn)).inRoot(isDialog()).perform(click())
        assertNull(vm.uiState.value.rotationGestureRemoval)
        assertTrue(rotationWrites().isEmpty())

        clickRotationDelete(scenario, 0)
        waitUntil { vm.uiState.value.rotationGestureRemoval != null }
        val confirmed = requireNotNull(vm.uiState.value.rotationGestureRemoval).requestId
        assertTrue(confirmed > firstRequest)
        onView(withId(R.id.ubi4DialogRotationGroupConfirmBtn)).inRoot(isDialog()).perform(click())
        waitForRotationRows(scenario, vm, listOf(64, 4))
        SystemClock.sleep(150) // Include the fake device response to SET in the check.
        assertEquals(listOf(64, 4), vm.uiState.value.rotationGroupGestureIds)
        scenario.onActivity {
            vm.onAction(V3GesturesAction.RotationGestureRemovalConfirmed(confirmed))
            it.findViewById<RecyclerView>(R.id.sprGesturesRv).adapter?.notifyDataSetChanged()
        }
        SystemClock.sleep(150)
        assertEquals(1, rotationWrites().size)
        assertEquals(listOf(64, 64, 4, 4) + List(12) { 0 }, rotationWrites().single().slice(6..21).map { it.toInt() and 255 })

        // A new device value closes the old dialog. Even its retained callback is harmless.
        clickRotationDelete(scenario, 0)
        waitUntil { vm.uiState.value.rotationGestureRemoval != null }
        lateinit var oldConfirmButton: View
        onView(withId(R.id.ubi4DialogRotationGroupConfirmBtn)).inRoot(isDialog()).check { view, error ->
            if (error != null) throw error
            oldConfirmButton = view
        }
        scenario.onActivity {
            ParameterStoreV3.put(ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE),
                ParameterTypedValueV3.RotationGroup(RotationGroupV3(gesture1Id = 4)))
        }
        waitForRotationRows(scenario, vm, listOf(4))
        onView(withId(R.id.ubi4DialogRotationGroupConfirmBtn)).check(doesNotExist())
        scenario.onActivity { oldConfirmButton.performClick() }
        assertEquals(1, rotationWrites().size)

        clickRotationDelete(scenario, 0)
        onView(withId(R.id.ubi4DialogRotationGroupConfirmBtn)).inRoot(isDialog()).perform(click())
        waitForRotationRows(scenario, vm, emptyList())
        SystemClock.sleep(150)
        assertTrue(vm.uiState.value.rotationGroupGestureIds.isEmpty())
        assertEquals(2, rotationWrites().size)
        assertEquals(List(16) { 0 }, rotationWrites().last().slice(6..21).map { it.toInt() and 255 })
        scenario.onActivity {
            assertEquals(View.VISIBLE, it.findViewById<View>(R.id.rotationGroupExplanationTv).visibility)
            assertEquals(View.VISIBLE, it.findViewById<View>(R.id.addGestureToRotationGroupBtn).visibility)
            ParameterStoreV3.put(ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE),
                ParameterTypedValueV3.RotationGroup(RotationGroupV3(gesture1Id = 4)))
        }
        waitForRotationRows(scenario, vm, listOf(4))
        clickRotationDelete(scenario, 0)
        onView(withId(R.id.ubi4DialogRotationGroupConfirmBtn)).inRoot(isDialog()).check { view, error ->
            if (error != null) throw error
            oldConfirmButton = view
        }
        clickCard(scenario, R.id.page_2)
        onView(withId(R.id.ubi4DialogRotationGroupConfirmBtn)).check(doesNotExist())
        scenario.onActivity { oldConfirmButton.performClick() }
        assertEquals(2, rotationWrites().size)
        assertEquals(selectionsBefore, selections())
    }

    private fun checkRotationGroupSelection(scenario: ActivityScenario<MainActivityUBI4>) {
        clickCard(scenario, R.id.page_1)
        waitForState(scenario, V3GesturesUiState(2, true))
        lateinit var vm: V3GesturesViewModel
        scenario.onActivity {
            vm = ViewModelProvider(it.supportFragmentManager.findFragmentById(R.id.fragmentContainer)!!)[V3GesturesViewModel::class.java]
        }
        clickCard(scenario, R.id.rotationGroupSelectBtn)
        SystemClock.sleep(200)
        val info = ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE)
        scenario.onActivity {
            ParameterStoreV3.put(info, ParameterTypedValueV3.RotationGroup(
                RotationGroupV3(gesture1Id = 64, gesture2Id = 4, gesture3Id = 64)))
        }
        waitForRotationRows(scenario, vm, listOf(64, 4, 64))
        val writesBefore = rotationWrites().size
        val selectionsBefore = selections()
        clickCard(scenario, R.id.addGestureToRotationGroupBtn)
        waitUntil { vm.uiState.value.rotationGroupSelection != null }
        scenario.onActivity {
            assertEquals(CollectionGesturesProvider.getCollectionGestures().map { it.gestureId },
                vm.uiState.value.rotationGroupSelection!!.availableGestureIds)
        }
        checkSelectionMark(vm, 4, true)
        checkSelectionMark(vm, 64, true)
        toggleSelectionGesture(vm, 4, false)
        onView(withId(R.id.dialogAddGesturesToGroupCancelBtn)).inRoot(isDialog()).perform(click())
        assertNull(vm.uiState.value.rotationGroupSelection)
        assertEquals(writesBefore, rotationWrites().size)
        clickCard(scenario, R.id.addGestureToRotationGroupBtn)
        checkSelectionMark(vm, 4, true)
        toggleSelectionGesture(vm, 4, false)
        listOf(77, 2, 1).forEach { toggleSelectionGesture(vm, it, true) }
        assertEquals(writesBefore, rotationWrites().size)
        assertEquals(listOf(64, 4, 64), vm.uiState.value.rotationGroupGestureIds)
        val request = vm.uiState.value.rotationGroupSelection!!.requestId
        onView(withId(R.id.dialogAddGesturesToGroupSaveBtn)).inRoot(isDialog()).perform(click())
        waitForRotationRows(scenario, vm, listOf(64, 64, 1, 2, 77))
        SystemClock.sleep(150)
        scenario.onActivity {
            vm.onAction(V3GesturesAction.RotationGroupSelectionSaved(request))
            it.findViewById<RecyclerView>(R.id.sprGesturesRv).adapter?.notifyDataSetChanged()
        }
        assertEquals(writesBefore + 1, rotationWrites().size)
        assertEquals(listOf(64, 64, 64, 64, 1, 1, 2, 2, 77, 77) + List(6) { 0 },
            rotationWrites().last().slice(6..21).map { it.toInt() and 255 })
        clickCard(scenario, R.id.addGestureToRotationGroupBtn)
        onView(withId(R.id.dialogAddGesturesToGroupSaveBtn)).inRoot(isDialog()).perform(click())
        assertEquals(writesBefore + 2, rotationWrites().size) // Preserve Save even when checkmarks have not changed.
        SystemClock.sleep(150)

        clickCard(scenario, R.id.addGestureToRotationGroupBtn)
        toggleSelectionGesture(vm, 3, true)
        lateinit var oldSave: View
        onView(withId(R.id.dialogAddGesturesToGroupSaveBtn)).inRoot(isDialog()).check { view, error ->
            if (error != null) throw error
            oldSave = view
        }
        scenario.onActivity {
            ParameterStoreV3.put(info, ParameterTypedValueV3.RotationGroup(
                RotationGroupV3(gesture1Id = 4, gesture2Id = 64, gesture3Id = 4)))
        }
        waitForRotationRows(scenario, vm, listOf(4, 64, 4))
        onView(withId(R.id.dialogAddGesturesToGroupSaveBtn)).check(doesNotExist())
        scenario.onActivity { oldSave.performClick() }
        assertEquals(writesBefore + 2, rotationWrites().size)

        clickCard(scenario, R.id.addGestureToRotationGroupBtn)
        listOf(1, 2, 3, 5, 6).forEach { toggleSelectionGesture(vm, it, true) }
        toggleSelectionGesture(vm, 7, true) // The existing limit counts checkmarks, including a gesture present twice.
        toggleSelectionGesture(vm, 8, false) // Ninth checkmark is rejected, as before.
        assertEquals(writesBefore + 2, rotationWrites().size)
        onView(withId(R.id.dialogAddGesturesToGroupSaveBtn)).inRoot(isDialog()).perform(click())
        val fullGroup = listOf(4, 64, 4, 1, 2, 3, 5, 6)
        waitUntil { vm.uiState.value.rotationGroupGestureIds == fullGroup }
        SystemClock.sleep(150)
        assertEquals(fullGroup, vm.uiState.value.rotationGroupGestureIds)
        assertEquals(writesBefore + 3, rotationWrites().size)
        assertEquals(fullGroup.flatMap { listOf(it, it) }, rotationWrites().last().slice(6..21).map { it.toInt() and 255 })
        scenario.onActivity {
            assertEquals(View.GONE, it.findViewById<View>(R.id.addGestureToRotationGroupBtn).visibility)
            ParameterStoreV3.put(info, ParameterTypedValueV3.RotationGroup(RotationGroupV3()))
        }
        waitForRotationRows(scenario, vm, emptyList())
        clickCard(scenario, R.id.addGestureToRotationGroupBtn)
        toggleSelectionGesture(vm, 64, true)
        onView(withId(R.id.dialogAddGesturesToGroupSaveBtn)).inRoot(isDialog()).check { view, error ->
            if (error != null) throw error
            oldSave = view
        }
        clickCard(scenario, R.id.page_2)
        onView(withId(R.id.dialogAddGesturesToGroupSaveBtn)).check(doesNotExist())
        scenario.onActivity { oldSave.performClick() }
        assertEquals(writesBefore + 3, rotationWrites().size)
        assertEquals(selectionsBefore, selections())
    }

    private fun toggleSelectionGesture(vm: V3GesturesViewModel, gestureId: Int, checked: Boolean) {
        waitUntil { vm.uiState.value.rotationGroupSelection != null }
        val position = vm.uiState.value.rotationGroupSelection!!.availableGestureIds.indexOf(gestureId)
        onView(withId(R.id.dialogAddGesturesToGroupRv)).inRoot(isDialog()).perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(position, object : ViewAction {
                override fun getConstraints() = isDisplayed()
                override fun getDescription() = "Toggle rotation gesture $gestureId"
                override fun perform(uiController: UiController, view: View) {
                    assertTrue(view.findViewById<View>(R.id.ubi4DialogCollectionGestureItemBtn).performClick())
                    uiController.loopMainThreadUntilIdle()
                }
            }))
        checkSelectionMark(vm, gestureId, checked)
    }

    private fun checkSelectionMark(vm: V3GesturesViewModel, gestureId: Int, checked: Boolean) {
        waitUntil { vm.uiState.value.rotationGroupSelection != null }
        val position = vm.uiState.value.rotationGroupSelection!!.availableGestureIds.indexOf(gestureId)
        assertEquals(checked, gestureId in vm.uiState.value.rotationGroupSelection!!.selectedGestureIds)
        onView(withId(R.id.dialogAddGesturesToGroupRv)).inRoot(isDialog()).perform(
            RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(position)
        ).check { view, error ->
            if (error != null) throw error
            val row = (view as RecyclerView).findViewHolderForAdapterPosition(position)!!.itemView
            assertEquals(if (checked) View.VISIBLE else View.GONE, row.findViewById<View>(R.id.usedGestureCheckIv).visibility)
        }
    }

    private fun checkRotationGroupOrder(scenario: ActivityScenario<MainActivityUBI4>) {
        clickCard(scenario, R.id.page_1)
        waitForState(scenario, V3GesturesUiState(2, true))
        lateinit var vm: V3GesturesViewModel
        scenario.onActivity {
            vm = ViewModelProvider(it.supportFragmentManager.findFragmentById(R.id.fragmentContainer)!!)[V3GesturesViewModel::class.java]
        }
        clickCard(scenario, R.id.rotationGroupSelectBtn)
        SystemClock.sleep(200)
        val info = ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE)
        scenario.onActivity {
            ParameterStoreV3.put(info, ParameterTypedValueV3.RotationGroup(
                RotationGroupV3(gesture1Id = 4, gesture2Id = 64, gesture3Id = 4)))
        }
        waitForRotationRows(scenario, vm, listOf(4, 64, 4))
        val writesBefore = rotationWrites().size
        val selectionsBefore = selections()
        scenario.onActivity {
            val list = it.findViewById<com.woxthebox.draglistview.DragListView>(R.id.rotationGroupDragLv).recyclerView
            val row = list.findViewHolderForAdapterPosition(0)!!.itemView
            assertEquals(View.GONE, row.findViewById<View>(R.id.copyBtn).visibility)
            assertEquals(View.GONE, row.findViewById<View>(R.id.copyIv).visibility)
        }
        dragRotationGesture(0, 2)
        waitForRotationRows(scenario, vm, listOf(64, 4, 4))
        SystemClock.sleep(150)
        assertEquals(writesBefore + 1, rotationWrites().size)
        assertEquals(listOf(64, 64, 4, 4, 4, 4) + List(10) { 0 },
            rotationWrites().last().slice(6..21).map { it.toInt() and 255 })
        scenario.onActivity { it.findViewById<RecyclerView>(R.id.sprGesturesRv).adapter?.notifyDataSetChanged() }
        waitForRotationRows(scenario, vm, listOf(64, 4, 4))
        assertEquals(writesBefore + 1, rotationWrites().size)

        dragRotationGesture(2, 0)
        waitForRotationRows(scenario, vm, listOf(4, 64, 4))
        SystemClock.sleep(150)
        assertEquals(writesBefore + 2, rotationWrites().size)
        dragRotationGesture(1, 1)
        SystemClock.sleep(350)
        assertEquals(writesBefore + 2, rotationWrites().size)
        assertEquals(listOf(4, 64, 4), vm.uiState.value.rotationGroupGestureIds)

        scenario.onActivity {
            ParameterStoreV3.put(info, ParameterTypedValueV3.RotationGroup(
                RotationGroupV3(gesture1Id = 4, gesture2Id = 4, gesture3Id = 64)))
        }
        waitForRotationRows(scenario, vm, listOf(4, 4, 64))
        dragRotationGesture(0, 1)
        waitUntil { rotationWrites().size == writesBefore + 3 }
        waitForRotationRows(scenario, vm, listOf(4, 4, 64))
        SystemClock.sleep(150)
        assertEquals(writesBefore + 3, rotationWrites().size)
        assertEquals(listOf(4, 4, 4, 4, 64, 64) + List(10) { 0 },
            rotationWrites().last().slice(6..21).map { it.toInt() and 255 })
        clickCard(scenario, R.id.page_2)
        scenario.onActivity { vm.onAction(V3GesturesAction.RotationGestureMoved(0, 2, listOf(4, 4, 64))) }
        assertEquals(writesBefore + 3, rotationWrites().size)
        assertEquals(selectionsBefore, selections())
    }

    private fun checkGesturesDisplaySettings(scenario: ActivityScenario<MainActivityUBI4>) {
        val writesBefore = rotationWrites().size
        val selectionsBefore = selections()
        val sharedFilter = UiState.activeGestureFragmentFilterFlow.value
        scenario.onActivity { UiState.activeGestureFragmentFilterFlow.value = 3 }
        try {
            clickCard(scenario, R.id.page_1)
            waitForDisplaySettings(scenario, section = 2, expanded = true)
            clickCard(scenario, R.id.collectionOfGesturesSelectBtn)
            waitForDisplaySettings(scenario, section = 1, expanded = true)
            clickCard(scenario, R.id.hideCollectionBtn)
            waitForDisplaySettings(scenario, section = 1, expanded = false)
            val readsBeforeRebind = rotationReads()
            scenario.onActivity { it.findViewById<RecyclerView>(R.id.sprGesturesRv).adapter?.notifyDataSetChanged() }
            waitForDisplaySettings(scenario, section = 1, expanded = false)
            SystemClock.sleep(450)
            assertEquals(readsBeforeRebind, rotationReads())

            // Both actions arrive before rendering; the last state must win without a delayed hide/show.
            scenario.onActivity {
                val toggle = it.findViewById<View>(R.id.hideCollectionBtn)
                toggle.performClick(); toggle.performClick()
            }
            SystemClock.sleep(500)
            waitForDisplaySettings(scenario, section = 1, expanded = false)
            repeat(2) {
                val readsBefore = rotationReads()
                clickCard(scenario, R.id.rotationGroupSelectBtn)
                waitForDisplaySettings(scenario, section = 2, expanded = false)
                waitUntil { rotationReads() > readsBefore }
                SystemClock.sleep(900)
                assertEquals(readsBefore + 1, rotationReads())
            }

            scenario.onActivity { UiState.v3WidgetsInteractionEnabled.value = false }
            waitForDisplaySettings(scenario, section = 2, expanded = false, enabled = false)
            clickCard(scenario, R.id.collectionOfGesturesSelectBtn)
            scenario.onActivity {
                assertFalse(it.findViewById<View>(R.id.hideCollectionBtn).isEnabled)
                it.findViewById<View>(R.id.hideCollectionBtn).performClick()
            }
            waitForDisplaySettings(scenario, section = 1, expanded = false, enabled = false)
            scenario.onActivity { UiState.v3WidgetsInteractionEnabled.value = true }
            waitForDisplaySettings(scenario, section = 1, expanded = false)
            clickCard(scenario, R.id.rotationGroupSelectBtn)
            waitForDisplaySettings(scenario, section = 2, expanded = false)
            clickCard(scenario, R.id.page_2)
            clickCard(scenario, R.id.page_1)
            waitForDisplaySettings(scenario, section = 2, expanded = false)
            SystemClock.sleep(900)
            val restoredReads = rotationReads()
            scenario.onActivity {
                assertEquals(2, it.getInt(PreferenceKeysUbi4.LAST_ACTIVE_GESTURE_FILTER, -1))
                assertEquals(0, it.getInt(PreferenceKeysUbi4.LAST_HIDE_COLLECTION_BTN_STATE, -1))
                it.findViewById<RecyclerView>(R.id.sprGesturesRv).adapter?.notifyDataSetChanged()
            }
            waitForDisplaySettings(scenario, section = 2, expanded = false)
            SystemClock.sleep(450)
            assertEquals(restoredReads, rotationReads())

            clickCard(scenario, R.id.collectionOfGesturesSelectBtn)
            clickCard(scenario, R.id.hideCollectionBtn)
            waitForDisplaySettings(scenario, section = 1, expanded = true)
            clickCard(scenario, R.id.hideCollectionBtn)
            clickCard(scenario, R.id.page_2) // Leave before the delayed collapse finishes.
            clickCard(scenario, R.id.page_1)
            waitForDisplaySettings(scenario, section = 1, expanded = false)
            clickCard(scenario, R.id.hideCollectionBtn)
            waitForDisplaySettings(scenario, section = 1, expanded = true)
            assertEquals(writesBefore, rotationWrites().size)
            assertEquals(selectionsBefore, selections())
            assertEquals(3, UiState.activeGestureFragmentFilterFlow.value)
        } finally {
            scenario.onActivity { UiState.activeGestureFragmentFilterFlow.value = sharedFilter }
        }
    }

    private fun waitForDisplaySettings(
        scenario: ActivityScenario<MainActivityUBI4>, section: Int, expanded: Boolean, enabled: Boolean = true,
    ) = waitUntil {
        var rendered = false
        scenario.onActivity {
            val fragment = it.supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (fragment is SprGestureFragment && fragment.isResumed && fragment.view != null) {
                val state = ViewModelProvider(fragment)[V3GesturesViewModel::class.java].uiState.value
                val factory = it.findViewById<View>(R.id.collectionFactoryGesturesCl)
                rendered = state.selectedSection == section && state.isFactoryCollectionExpanded == expanded &&
                    state.isInteractionEnabled == enabled &&
                    it.findViewById<View>(R.id.collectionGesturesCl).visibility == (if (section == 1) View.VISIBLE else View.GONE) &&
                    it.findViewById<View>(R.id.rotationGroupCl).visibility == (if (section == 1) View.GONE else View.VISIBLE) &&
                    factory.visibility == (if (expanded) View.VISIBLE else View.GONE) &&
                    kotlin.math.abs(factory.alpha - if (expanded) 1F else 0F) < 0.01F &&
                    kotlin.math.abs(it.findViewById<View>(R.id.hideCollectionBtn).rotation - if (expanded) 180F else 0F) < 0.1F &&
                    kotlin.math.abs(it.findViewById<View>(R.id.collectionUserGesturesCl).translationY) < 0.1F
            }
        }
        rendered
    }

    private fun dragRotationGesture(from: Int, to: Int) {
        fun handleCenter(view: View, position: Int): FloatArray {
            val list = (view as com.woxthebox.draglistview.DragListView).recyclerView
            val handle = list.findViewHolderForAdapterPosition(position)!!.itemView.findViewById<View>(R.id.swapIv)
            val location = IntArray(2)
            handle.getLocationOnScreen(location)
            return floatArrayOf(location[0] + handle.width / 2f, location[1] + handle.height / 2f)
        }
        onView(withId(R.id.rotationGroupDragLv)).perform(GeneralSwipeAction(
            Swipe.SLOW, { handleCenter(it, from) }, { handleCenter(it, to) }, Press.FINGER))
    }

    private fun clickRotationDelete(scenario: ActivityScenario<MainActivityUBI4>, position: Int) {
        scenario.onActivity {
            val list = it.findViewById<com.woxthebox.draglistview.DragListView>(R.id.rotationGroupDragLv).recyclerView
            assertTrue(list.findViewHolderForAdapterPosition(position)!!.itemView.findViewById<View>(R.id.deleteBtn).performClick())
        }
    }

    private fun waitForRotationRows(scenario: ActivityScenario<MainActivityUBI4>, vm: V3GesturesViewModel, ids: List<Int>) = waitUntil {
        var rendered = false
        scenario.onActivity {
            val list = it.findViewById<com.woxthebox.draglistview.DragListView>(R.id.rotationGroupDragLv).recyclerView
            rendered = vm.uiState.value.rotationGroupGestureIds == ids && list.adapter?.itemCount == ids.size &&
                ids.indices.all { position ->
                    list.findViewHolderForAdapterPosition(position)?.itemView?.findViewById<TextView>(R.id.gestureInRotationGroupTv)
                        ?.text?.toString() == CollectionGesturesProvider.getGesture(ids[position]).gestureName
                }
        }
        rendered
    }

    private fun rotationWrites() = V3BleEmulatorTestHooks.outgoingPacketsSnapshot().filter {
        it.size == 23 && it[0] == 0x80.toByte() && it[5].toInt() == 0x36
    }

    private fun rotationReads() = V3BleEmulatorTestHooks.outgoingPacketsSnapshot().count {
        it.size == 5 && it[1].toInt() == 15 && it[2].toInt() == 0x35
    }

    private fun clickCard(scenario: ActivityScenario<MainActivityUBI4>, id: Int) {
        scenario.onActivity { assertTrue(it.findViewById<View>(id).performClick()) }
    }

    private fun assertHeader(activity: MainActivityUBI4, id: Int) {
        val name = CollectionGesturesProvider.getGesture(id).gestureName
        assertEquals(activity.getString(R.string.active_gesture_is, name), activity.findViewById<TextView>(R.id.activeGestureNameTv).text.toString())
    }

    private fun selections() = V3BleEmulatorTestHooks.outgoingPacketsSnapshot().filter {
        it.size == 5 && it[1].toInt() == 15 && it[2].toInt() == 37
    }.map { it[3].toInt() and 255 }

    private fun waitForState(scenario: ActivityScenario<MainActivityUBI4>, expected: V3GesturesUiState) = waitUntil {
        var ready = false
        scenario.onActivity {
            val fragment = it.supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (fragment is SprGestureFragment && fragment.isResumed && fragment.view != null) {
                val state = ViewModelProvider(fragment)[V3GesturesViewModel::class.java].uiState.value
                ready = state.activeGestureId == expected.activeGestureId && state.isInteractionEnabled == expected.isInteractionEnabled
            }
        }
        ready
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 15_000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        throw AssertionError("Timed out waiting for V3 gesture UI/command")
    }
}
