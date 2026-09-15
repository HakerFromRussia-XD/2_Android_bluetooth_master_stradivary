package com.bailout.stickk.ubi4.versions.v3.data.appsettings

import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3SpecialSettingsSection
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.SetSpecialSettingsSectionUseCaseV3
import android.content.SharedPreferences
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
    private val sectionKey = PreferenceKeysUbi4.LAST_ACTIVE_SETTINGS_FILTER
    private var storedApplicationSection = false
    private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()
    private val repository = V3AppSettingsRepositoryImpl(preferences)

    @BeforeEach
    fun setUp() {
        every { preferences.getBoolean(key, false) } answers { storedAutoLogin }
        every { preferences.getBoolean(sectionKey, false) } answers { storedApplicationSection }
        every { preferences.edit() } returns editor
        every { editor.putBoolean(sectionKey, any()) } answers { storedApplicationSection = secondArg(); editor }
        every { editor.putBoolean(key, any()) } answers { storedAutoLogin = secondArg(); editor }
        every { editor.apply() } answers { listeners.toList().forEach { it.onSharedPreferenceChanged(preferences, key) } }
        every { preferences.registerOnSharedPreferenceChangeListener(any()) } answers { listeners.add(firstArg()); Unit }
        every { preferences.unregisterOnSharedPreferenceChangeListener(any()) } answers { listeners.remove(firstArg()); Unit }
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
}
