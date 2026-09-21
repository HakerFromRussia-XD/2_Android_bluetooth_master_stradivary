package com.bailout.stickk.ubi4.versions.v3.data.appsettings

import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3SpecialSettingsSection
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.SetSpecialSettingsSectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.GetCustomGestureNamesUseCaseV3
import android.content.SharedPreferences
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.GetGesturesPreferencesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.SetGesturesSectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.SetFactoryGestureCollectionExpandedUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3GesturesPreferences
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.SaveGestureSettingsSelectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.V3GestureSettingsTarget
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.SetAutoLoginEnabledUseCaseV3
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class V3AppSettingsRepositoryTest {
    private val preferences = mockk<SharedPreferences>()
    private val editor = mockk<SharedPreferences.Editor>()
    private val key = PreferenceKeysUbi4.SET_MODE_SMART_CONNECTION
    private var storedAutoLogin = false
    private val storedStrings = mutableMapOf<String, String?>()
    private val storedInts = mutableMapOf<String, Int>()
    private val sectionKey = PreferenceKeysUbi4.LAST_ACTIVE_SETTINGS_FILTER
    private var storedApplicationSection = false
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()
    private val repository = V3AppSettingsRepositoryImpl(preferences)

    @BeforeEach
    fun setUp() {
        every { preferences.getBoolean(key, false) } answers { storedAutoLogin }
        every { preferences.getBoolean(sectionKey, false) } answers { storedApplicationSection }
        every { preferences.getString(any(), any()) } answers {
            if (storedStrings.containsKey(firstArg<String>())) storedStrings[firstArg()] else secondArg<String?>()
        }
        every { preferences.getInt(any(), any()) } answers { storedInts[firstArg<String>()] ?: secondArg<Int>() }
        every { editor.putInt(any(), any()) } answers { storedInts[firstArg()] = secondArg(); editor }
        every { preferences.edit() } returns editor
        every { editor.putBoolean(sectionKey, any()) } answers { storedApplicationSection = secondArg(); editor }
        every { editor.putBoolean(key, any()) } answers { storedAutoLogin = secondArg(); editor }
        every { editor.apply() } answers { listeners.toList().forEach { it.onSharedPreferenceChanged(preferences, key) } }
        every { preferences.registerOnSharedPreferenceChangeListener(any()) } answers { listeners.add(firstArg()); Unit }
        every { preferences.unregisterOnSharedPreferenceChangeListener(any()) } answers { listeners.remove(firstArg()); Unit }
    }

    @Test fun `editor names preserve their own defaults and one based selection`() {
        val prefix = PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM
        storedInts[prefix] = 2
        storedStrings[prefix + "load not work" + 1] = ""
        storedStrings[prefix + "load not work" + 2] = null
        val result = repository.getGestureEditorNames()
        assertEquals(2, result.gestureNumber)
        assertEquals(PreferenceKeysUbi4.NUM_GESTURES, result.names.size)
        assertEquals(listOf("load not work", "", "null"), result.names.take(3))
        verify(exactly = 0) { preferences.edit() }
    }

    @Test fun `editor writes each name with current MAC then publishes update`() = runTest {
        val previousUpdates = com.bailout.stickk.ubi4.data.state.UiState.updateFlow
        com.bailout.stickk.ubi4.data.state.UiState.updateFlow = kotlinx.coroutines.flow.MutableSharedFlow(replay = 1, extraBufferCapacity = 64)
        try {
        val events = mutableListOf<String>()
        every { editor.putString(any(), any()) } answers {
            events.add("${firstArg<String>()}=${secondArg<String>()}")
            editor
        }
        every { editor.apply() } answers { events.add("apply") }
        val watcher = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            com.bailout.stickk.ubi4.data.state.UiState.updateFlow.collect { events.add("update:$it") }
        }
        repository.saveGestureEditorNames(listOf("First", ""))
        runCurrent()
        val prefix = PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM
        assertEquals(listOf(prefix + "text0=First", "apply", prefix + "text1=", "apply", "update:0"), events)
        events.clear()
        storedStrings[PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4] = "NEW"
        repository.saveGestureEditorNames(listOf("Renamed"))
        runCurrent()
        assertEquals(listOf(prefix + "NEW0=Renamed", "apply", "update:0"), events)
        watcher.cancel()
        } finally {
            com.bailout.stickk.ubi4.data.state.UiState.updateFlow = previousUpdates
        }
    }

    @Test fun `custom names preserve MAC index keys and missing null and empty values without writing`() {
        val macKey = PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4
        val prefix = PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM
        storedStrings[macKey] = "AA:01"
        storedStrings[prefix + "AA:01" + 0] = "First"
        storedStrings[prefix + "AA:01" + 1] = ""
        storedStrings[prefix + "AA:01" + 2] = null
        storedStrings[prefix + "AA:01" + 13] = "Last"
        storedStrings[prefix + "AA:01" + 64] = "Not an index"
        val names = GetCustomGestureNamesUseCaseV3(repository)()
        assertEquals(14, names.names.size)
        assertEquals(14, names.collectionNames.size)
        assertEquals(listOf("First", "", "null", "NOT SET!"), names.names.take(4))
        assertEquals(listOf("First", "", null, null), names.collectionNames.take(4))
        assertEquals("Last", names.names.last())
        assertEquals("Last", names.collectionNames.last())
        verify(exactly = 0) { preferences.edit() }
    }

    @Test fun `missing and null MAC preserve the two original reader fallbacks`() {
        val prefix = PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM
        storedStrings[prefix + "NOT SET!0"] = "Missing MAC direct"
        storedStrings[prefix + "null0"] = "Null MAC direct"
        storedStrings[prefix + "0"] = "Empty MAC collection"
        val read = GetCustomGestureNamesUseCaseV3(repository)
        assertEquals("Missing MAC direct", read().names.first())
        assertEquals("Empty MAC collection", read().collectionNames.first())
        storedStrings[PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4] = null
        assertEquals("Null MAC direct", read().names.first())
        assertEquals("Empty MAC collection", read().collectionNames.first())
        storedStrings[PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4] = ""
        assertEquals("Empty MAC collection", read().names.first())
        verify(exactly = 0) { preferences.edit() }
    }

    @Test fun `name reads follow rename and device switch without mutating an earlier snapshot`() {
        val macKey = PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4
        val prefix = PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM
        val read = GetCustomGestureNamesUseCaseV3(repository)
        storedStrings[macKey] = "first"
        storedStrings[prefix + "first0"] = "Before"
        val old = read()
        storedStrings[prefix + "first0"] = "Renamed"
        assertEquals("Renamed", read().names.first())
        storedStrings[prefix + "second0"] = "Another device"
        storedStrings[macKey] = "second"
        assertEquals("Another device", read().names.first())
        assertEquals("Another device", read().collectionNames.first())
        assertEquals("Before", old.names.first())
        assertEquals("Before", old.collectionNames.first())
        verify(exactly = 0) { preferences.edit() }
    }

    @Test
    fun `use case writes only changes to the existing preference using apply`() {
        val setAutoLogin = SetAutoLoginEnabledUseCaseV3(repository)
        assertFalse(repository.getAutoLoginEnabled())
        setAutoLogin(true); setAutoLogin(true); setAutoLogin(false)
        verify(exactly = 1) { editor.putBoolean(key, true) }
        verify(exactly = 1) { editor.putBoolean(key, false) }
        verify(exactly = 2) { editor.apply() }
        confirmVerified(editor)
        assertFalse(repository.getAutoLoginEnabled())
    }

    @Test
    fun `observer receives external changes and clearing but never writes`() = runTest {
        val values = mutableListOf<Boolean>()
        val job = launch { repository.observeAutoLoginEnabled().collect(values::add) }
        runCurrent()
        storedAutoLogin = true
        listeners.single().onSharedPreferenceChanged(preferences, "unrelated"); runCurrent()
        assertEquals(listOf(false), values)
        listeners.single().onSharedPreferenceChanged(preferences, key); runCurrent()
        listeners.single().onSharedPreferenceChanged(preferences, key); runCurrent()
        storedAutoLogin = false
        listeners.single().onSharedPreferenceChanged(preferences, null); runCurrent()
        assertEquals(listOf(false, true, false), values)
        job.cancelAndJoin()
        assertTrue(listeners.isEmpty())
        verify(exactly = 0) { preferences.edit() }
    }

    @Test
    fun `section retains false prosthesis true application mapping and writes only its key`() {
        val setSection = SetSpecialSettingsSectionUseCaseV3(repository)
        assertEquals(V3SpecialSettingsSection.PROSTHESIS, repository.getSpecialSettingsSection())
        setSection(V3SpecialSettingsSection.APPLICATION)
        assertTrue(storedApplicationSection)
        assertEquals(V3SpecialSettingsSection.APPLICATION, repository.getSpecialSettingsSection())
        setSection(V3SpecialSettingsSection.APPLICATION)
        setSection(V3SpecialSettingsSection.PROSTHESIS)
        assertFalse(storedApplicationSection)
        verify(exactly = 1) { editor.putBoolean(sectionKey, true) }
        verify(exactly = 1) { editor.putBoolean(sectionKey, false) }
        verify(exactly = 2) { editor.apply() }
        confirmVerified(editor)
        assertFalse(storedAutoLogin)
    }

    @Test
    fun `auto login writes never change the selected section`() {
        storedApplicationSection = true
        SetAutoLoginEnabledUseCaseV3(repository)(true)
        assertEquals(V3SpecialSettingsSection.APPLICATION, repository.getSpecialSettingsSection())
        verify(exactly = 0) { editor.putBoolean(sectionKey, any()) }
    }

    @Test
    fun `failed initial read releases its preference listener`() = runTest {
        every { preferences.getBoolean(key, false) } throws IllegalStateException("Invalid stored type")
        val result = runCatching { repository.observeAutoLoginEnabled().collect {} }
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertTrue(listeners.isEmpty())
        verify(exactly = 1) { preferences.unregisterOnSharedPreferenceChangeListener(any()) }
    }
    @Test fun `editor selection preserves custom gesture numbering and the existing preference key`() {
        val save = SaveGestureSettingsSelectionUseCaseV3(repository)
        for (id in 64..77) {
            save(V3GestureSettingsTarget.fromGestureId(id)!!)
            assertEquals(id - 63, storedInts[PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM])
            verify(exactly = 1) { editor.putInt(PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM, id - 63) }
        }
        verify(exactly = 14) { editor.apply() }
        confirmVerified(editor)
        assertEquals(setOf(PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM), storedInts.keys)
    }

    @Test fun `gestures preferences preserve defaults and stored integer meanings without writing`() {
        val read = GetGesturesPreferencesUseCaseV3(repository)
        assertEquals(V3GesturesPreferences(1, true), read())
        for (section in listOf(1, 2, 3, 0, -1)) {
            for (expanded in listOf(1, 0, 2, -1)) {
                storedInts[PreferenceKeysUbi4.LAST_ACTIVE_GESTURE_FILTER] = section
                storedInts[PreferenceKeysUbi4.LAST_HIDE_COLLECTION_BTN_STATE] = expanded
                assertEquals(V3GesturesPreferences(section, expanded == 1), read())
            }
        }
        verify(exactly = 0) { preferences.edit() }
    }

    @Test fun `gestures writes retain existing keys integer encoding and explicit repeated selections`() {
        val setSection = SetGesturesSectionUseCaseV3(repository)
        val setExpanded = SetFactoryGestureCollectionExpandedUseCaseV3(repository)
        setSection(2); setSection(2)
        setExpanded(false)
        assertEquals(V3GesturesPreferences(2, false), repository.getGesturesPreferences())
        setExpanded(true); setSection(1)
        assertEquals(V3GesturesPreferences(1, true), repository.getGesturesPreferences())
        verify(exactly = 2) { editor.putInt(PreferenceKeysUbi4.LAST_ACTIVE_GESTURE_FILTER, 2) }
        verify(exactly = 1) { editor.putInt(PreferenceKeysUbi4.LAST_ACTIVE_GESTURE_FILTER, 1) }
        verify(exactly = 1) { editor.putInt(PreferenceKeysUbi4.LAST_HIDE_COLLECTION_BTN_STATE, 0) }
        verify(exactly = 1) { editor.putInt(PreferenceKeysUbi4.LAST_HIDE_COLLECTION_BTN_STATE, 1) }
        verify(exactly = 5) { editor.apply() }
        confirmVerified(editor)
        assertFalse(storedAutoLogin)
        assertFalse(storedApplicationSection)
    }

}
