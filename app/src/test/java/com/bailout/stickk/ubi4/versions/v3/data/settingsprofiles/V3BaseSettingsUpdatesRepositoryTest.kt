package com.bailout.stickk.ubi4.versions.v3.data.settingsprofiles

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.local.repository.*
import com.bailout.stickk.ubi4.data.network.*
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SETTINGS_PROFILE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3BaseSettingsOffer
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class V3BaseSettingsUpdatesRepositoryTest {
    private val storage = SettingsProfileRepository(ProfileDao())
    private val context = mockk<Context>(relaxed = true)
    private val preferences = mockk<SharedPreferences>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val sender = mockk<Ubi4SettingsProfileSender>()
    private val applied = mutableListOf<List<SettingsProfileApplyValue>>()
    private val scheduled = mutableListOf<String>()
    private val userScheduled = mutableListOf<String>()
    private val handled = mutableMapOf<String, Long>()
    private var pending: Pair<String, Long>? = null
    private var role = 2
    private var serial = "serial"
    private val speed = ParameterInfoRegistry.require(P_KEY_SPEED_SETTINGS)
    private val oldV3 = UiState.isInterfaceV3Activated
    private val oldInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val oldInit = UiState.fullInitInProgress.value
    private val oldStartup = UiState.startupInProgress.value
    private lateinit var offer: V3BaseSettingsOffer

    private fun repository() = V3BaseSettingsUpdatesRepositoryImpl(context, preferences, applied::add, sender,
        scheduleUpload = { id, _ -> scheduled += id }, scheduleUserUpload = { id, _ -> userScheduled += id })

    @BeforeEach fun setUp() = runBlocking {
        every { context.applicationContext } returns context
        every { context.getSystemService(ConnectivityManager::class.java) } returns null
        every { preferences.getInt(any(), any()) } answers { role }
        every { preferences.getLong(any(), any()) } answers { handled[firstArg()] ?: 0 }
        every { preferences.edit() } returns editor
        every { editor.putLong(any(), any()) } answers { pending = firstArg<String>() to secondArg<Long>(); editor }
        every { editor.commit() } answers { pending?.let { handled[it.first] = it.second }; true }
        mockkObject(SettingsProfileRepositoryProvider, SettingsProfileManager, WidgetRepoProvider, ParameterProvider)
        every { SettingsProfileRepositoryProvider.getOrNull() } returns storage
        every { SettingsProfileManager.serial() } answers { serial }
        every { WidgetRepoProvider.mac() } returns "MAC"
        val selection = ParameterInfoRegistry.require(P_KEY_SETTINGS_PROFILE)
        every { ParameterProvider.getParameterV3(selection) } returns BaseParameterInfoStruct(ID = selection.parameterID, dataCode = selection.dataCode, data = "{}")
        UiState.isInterfaceV3Activated = true
        UiState.v3WidgetsInteractionEnabled.value = true
        UiState.fullInitInProgress.value = false
        UiState.startupInProgress.value = false
        ParameterStoreV3.clear()
        ParameterStoreV3.put(ParameterInfoRegistry.require(P_KEY_SET_SERIAL_NUMBER), ParameterTypedValueV3.Text("serial"))
        storage.ensureBaseProfile("serial")
        storage.saveBleValue("serial", speed, ParameterTypedValueV3.Slider(SliderV3(42)))
        offer = V3BaseSettingsOffer("serial", storage.getBaseSettingsRevision("serial"))
        storage.createProfileFromActive("serial")
        storage.saveBleValue("serial", speed, ParameterTypedValueV3.Slider(SliderV3(80)), canEditBaseSettings = false)
    }

    @AfterEach fun tearDown() {
        unmockkObject(SettingsProfileRepositoryProvider, SettingsProfileManager, WidgetRepoProvider, ParameterProvider)
        UiState.isInterfaceV3Activated = oldV3
        UiState.v3WidgetsInteractionEnabled.value = oldInteraction
        UiState.fullInitInProgress.value = oldInit
        UiState.startupInProgress.value = oldStartup
        ParameterStoreV3.clear()
    }

    @Test fun `skip persists handled revision without touching active selection or BLE`() = runBlocking {
        assertTrue(repository().respond(offer, false))
        assertTrue(applied.isEmpty())
        assertEquals(1, storage.ensureState("serial").activeProfileId)
        val reopened = repository().observeEnvironment().first()
        assertNull(reopened.offer)
        assertEquals(offer.revision, reopened.handledRevision)
        assertEquals(offer.revision, storage.getBaseSettingsRevision("serial"))
    }

    @Test fun `accept selects baseline applies its values and updates spinner cache`() = runBlocking {
        assertTrue(repository().respond(offer, true))
        assertEquals(0, storage.ensureState("serial").activeProfileId)
        assertEquals(ParameterTypedValueV3.Slider(SliderV3(42)), applied.single().single().typedValue)
        val selection = ParameterStoreV3.get(ParameterInfoRegistry.require(P_KEY_SETTINGS_PROFILE)) as ParameterTypedValueV3.Spinner
        assertEquals(0, selection.value.spinnerValue)
        assertNull(repository().observeEnvironment().first().offer)
    }

    @Test fun `engineer role cannot respond and queues baseline publication only once per revision`() = runBlocking {
        role = 1
        val repo = repository()
        assertFalse(repo.respond(offer, true))
        assertNull(repo.observeEnvironment().first().offer)
        repo.observeEnvironment().first()
        assertEquals(listOf("serial"), scheduled)
        assertTrue(applied.isEmpty())
    }

    @Test fun `stale device or stale revision cannot be accepted or dismissed`() = runBlocking {
        serial = "other"
        assertFalse(repository().respond(offer, true))
        serial = "serial"
        storage.switchToProfile("serial", 0)
        storage.saveBleValue("serial", speed, ParameterTypedValueV3.Slider(SliderV3(55)))
        assertFalse(repository().respond(offer, true))
        assertFalse(repository().respond(offer, false))
        assertTrue(handled.isEmpty())
        assertTrue(applied.isEmpty())
    }

    @Test fun `old manager serial cannot apply a baseline to a different connected prosthesis`() = runBlocking {
        ParameterStoreV3.put(ParameterInfoRegistry.require(P_KEY_SET_SERIAL_NUMBER), ParameterTypedValueV3.Text("other"))
        assertNull(repository().observeEnvironment().first().offer)
        assertFalse(repository().respond(offer, true))
        assertTrue(applied.isEmpty())
    }

    @Test fun `blocked interaction prevents acceptance but permits skip`() = runBlocking {
        UiState.v3WidgetsInteractionEnabled.value = false
        assertFalse(repository().respond(offer, true))
        assertTrue(repository().respond(offer, false))
        assertTrue(applied.isEmpty())
    }

    @Test fun `a firmware transfer starting during pending writes prevents applying settings`() = runBlocking {
        coEvery { SettingsProfileManager.awaitPendingWrites("serial") } answers { UiState.fullInitInProgress.value = true }
        assertFalse(repository().respond(offer, true))
        assertTrue(applied.isEmpty())
        assertEquals(1, storage.ensureState("serial").activeProfileId)
    }

    @Test fun `pending engineer edit is published even after role switches to user`() = runBlocking {
        role = 2
        repository().observeEnvironment().first()
        assertEquals(listOf("serial"), scheduled)
    }

    @Test fun `refresh requests preservation and never applies downloaded values`() = runBlocking {
        coEvery { sender.downloadProfileSettingsForSerial("serial", any(), true) } returns
            SettingsProfileDownloadResult("9", "{}", SettingsProfileState(1, 0), listOf(
                SettingsProfileApplyValue("BLE", speed, null, ParameterTypedValueV3.Slider(SliderV3(99)), null, null),
            ))
        repository().refresh("serial")
        assertTrue(applied.isEmpty())
        assertEquals(listOf("serial"), userScheduled)
        coVerify(exactly = 1) { sender.downloadProfileSettingsForSerial("serial", any(), true) }
    }
}
