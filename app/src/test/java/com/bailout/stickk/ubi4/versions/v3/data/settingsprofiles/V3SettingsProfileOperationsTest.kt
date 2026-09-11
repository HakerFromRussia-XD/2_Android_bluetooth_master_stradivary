package com.bailout.stickk.ubi4.versions.v3.data.settingsprofiles

import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.local.repository.*
import com.bailout.stickk.ubi4.data.state.GlobalParameters
import com.bailout.stickk.ubi4.data.state.ParameterStoreV3
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.models.ble.ParameterCodecIdV3
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SETTINGS_PROFILE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SPEED_SETTINGS
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.CreateSettingsProfileUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.SelectSettingsProfileUseCaseV3
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlinx.coroutines.flow.drop
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.RenameSettingsProfileUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.settingsprofiles.V3SettingsProfileNameRules
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.Collections

@OptIn(ExperimentalCoroutinesApi::class)
class V3SettingsProfileOperationsTest {
    private val dispatcher = StandardTestDispatcher()
    private val originalDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
    private val originalV3 = UiState.isInterfaceV3Activated
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val info = ParameterInfoRegistry.require(P_KEY_SETTINGS_PROFILE)
    private val speedInfo = ParameterInfoRegistry.require(P_KEY_SPEED_SETTINGS)
    private val cache = BaseParameterInfoStruct(ID = info.parameterID, dataCode = info.dataCode, data = "{}")
    private val sharedRepository = mockk<SettingsProfileRepository>()
    private var serial = "current"
    private var address = "device"
    private var activeId = 1
    private val profileIds = mutableListOf(1, 3)
    private val order = Collections.synchronizedList(mutableListOf<String>())
    private val releases = mutableListOf<CompletableDeferred<Unit>>()
    private val values = listOf(SettingsProfileApplyValue(
        "BLE", speedInfo, ParameterCodecIdV3.SLIDER, ParameterTypedValueV3.Slider(SliderV3(42)), null, null,
    ))
    private val repository = V3SettingsProfilesRepositoryImpl {
        assertEquals(values, it)
        val cachedIndex = (ParameterStoreV3.get(info) as ParameterTypedValueV3.Spinner).value.spinnerValue
        assertEquals(profileIds.sorted().indexOf(activeId), cachedIndex)
        order.add("apply")
    }
    private val select = SelectSettingsProfileUseCaseV3(repository)
    private val create = CreateSettingsProfileUseCaseV3(repository)
    private val rename = RenameSettingsProfileUseCaseV3(repository)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(SettingsProfileManager, SettingsProfileRepositoryProvider, WidgetRepoProvider)
        every { SettingsProfileManager.serial() } answers { serial }
        every { WidgetRepoProvider.mac() } answers { address }
        every { SettingsProfileRepositoryProvider.get() } returns sharedRepository
        every { SettingsProfileRepositoryProvider.getOrNull() } returns sharedRepository
        coEvery { sharedRepository.getProfiles(any()) } answers {
            profileIds.sorted().map { SettingsProfileInfo(it, if (it == 3) "Sport" else null, activeId == it) }
        }
        coEvery { sharedRepository.switchToProfile(any(), any()) } answers {
            activeId = secondArg()
            order.add("select:$activeId")
            SettingsProfileState(2, activeId) to values
        }
        coEvery { sharedRepository.createProfileFromActive(any()) } answers {
            activeId = 2
            profileIds.add(2)
            order.add("create:2")
            SettingsProfileState(3, 2) to values
        }
        coEvery { sharedRepository.renameProfile(any(), any(), any()) } answers {
            SettingsProfileInfo(secondArg(), thirdArg(), secondArg<Int>() == activeId)
        }
        ParameterStoreV3.clear()
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf(BaseSubDeviceInfoStruct(
            deviceAddress = info.deviceAddress, parametersList = arrayListOf(cache),
        ))
        UiState.isInterfaceV3Activated = true
        UiState.v3WidgetsInteractionEnabled.value = true
    }

    @AfterEach
    fun tearDown() {
        releases.forEach { it.complete(Unit) }
        runBlocking {
            SettingsProfileManager.awaitPendingWrites("current")
            SettingsProfileManager.awaitPendingWrites("other")
        }
        unmockkObject(SettingsProfileManager, SettingsProfileRepositoryProvider, WidgetRepoProvider)
        ParameterStoreV3.clear()
        GlobalParameters.baseSubDevicesInfoStructSetV3 = originalDevices
        UiState.isInterfaceV3Activated = originalV3
        UiState.v3WidgetsInteractionEnabled.value = originalInteraction
        Dispatchers.resetMain()
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 3])
    fun `selection applies stored values once and caches an index rather than an ID`(id: Int) = runTest(dispatcher) {
        select("current", id)
        assertEquals(listOf("select:$id", "apply"), order)
        coVerify(exactly = 1) { sharedRepository.switchToProfile("current", id) }
        assertEquals(if (id == 3) "{\"spinnerValue\":1}" else "{}", cache.data)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `real manager waits for submitted BLE and mobile saves before changing the active profile`(creating: Boolean) = runTest(dispatcher) {
        val releaseBle = CompletableDeferred<Unit>().also(releases::add)
        val releaseMobile = CompletableDeferred<Unit>().also(releases::add)
        val bleStarted = CompletableDeferred<Unit>()
        val mobileStarted = CompletableDeferred<Unit>()
        coEvery { sharedRepository.saveBleValue("current", any(), any()) } coAnswers {
            bleStarted.complete(Unit)
            releaseBle.await()
            assertEquals(1, activeId)
            order.add("save:ble")
        }
        coEvery { sharedRepository.saveMobileBoolean("current", any(), any()) } coAnswers {
            mobileStarted.complete(Unit)
            releaseMobile.await()
            assertEquals(1, activeId)
            order.add("save:mobile")
        }
        SettingsProfileManager.saveBleValue(speedInfo, ParameterTypedValueV3.Slider(SliderV3(80)))
        SettingsProfileManager.saveMobileBoolean("auto-login", true)
        bleStarted.await(); mobileStarted.await()
        val selection = async { if (creating) create("current") else select("current", 3) }
        runCurrent()
        coVerify(exactly = 0) { sharedRepository.switchToProfile(any(), any()); sharedRepository.createProfileFromActive(any()) }
        releaseBle.complete(Unit)
        // One completed save cannot release the switch while another save is still pending.
        runCurrent()
        assertFalse(selection.isCompleted)
        releaseMobile.complete(Unit)
        selection.await()
        assertEquals(setOf("save:ble", "save:mobile"), order.take(2).toSet())
        assertEquals(listOf(if (creating) "create:2" else "select:3", "apply"), order.takeLast(2))
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `cancelling a profile operation does not cancel already accepted saving`(creating: Boolean) = runTest(dispatcher) {
        val release = CompletableDeferred<Unit>().also(releases::add)
        val started = CompletableDeferred<Unit>()
        coEvery { sharedRepository.saveBleValue("current", any(), any()) } coAnswers {
            started.complete(Unit); release.await(); order.add("saved")
        }
        SettingsProfileManager.saveBleValue(speedInfo, ParameterTypedValueV3.Slider(SliderV3(80)))
        started.await()
        val selection = launch { if (creating) create("current") else select("current", 3) }
        runCurrent(); selection.cancelAndJoin()
        release.complete(Unit)
        SettingsProfileManager.awaitPendingWrites("current")
        assertEquals(listOf("saved"), order)
        coVerify(exactly = 0) { sharedRepository.switchToProfile(any(), any()); sharedRepository.createProfileFromActive(any()) }
    }

    @Test
    fun `pending writes for another serial do not block the current device`() = runTest(dispatcher) {
        val release = CompletableDeferred<Unit>().also(releases::add)
        val started = CompletableDeferred<Unit>()
        coEvery { sharedRepository.saveBleValue("other", any(), any()) } coAnswers { started.complete(Unit); release.await() }
        serial = "other"
        SettingsProfileManager.saveBleValue(speedInfo, ParameterTypedValueV3.Slider(SliderV3(80)))
        started.await()
        serial = "current"
        select("current", 3)
        assertEquals(listOf("select:3", "apply"), order)
        release.complete(Unit)
        SettingsProfileManager.awaitPendingWrites("other")
    }

    @ParameterizedTest
    @ValueSource(strings = ["select:serial", "select:address", "select:disconnected", "select:cancelled",
        "create:serial", "create:address", "create:disconnected", "create:cancelled"])
    fun `a delayed DB result never applies values after its device or request is invalidated`(reason: String) = runTest(dispatcher) {
        val finish = CompletableDeferred<Unit>()
        coEvery { sharedRepository.switchToProfile("current", 3) } coAnswers {
            finish.await()
            activeId = 3
            SettingsProfileState(2, 3) to values
        }
        coEvery { sharedRepository.createProfileFromActive("current") } coAnswers {
            finish.await()
            activeId = 2
            profileIds.add(2)
            SettingsProfileState(3, 2) to values
        }
        val result = async { runCatching { if (reason.startsWith("create:")) create("current") else select("current", 3) } }
        runCurrent()
        when (reason.substringAfter(':')) {
            "serial" -> serial = "new"
            "address" -> address = "new"
            "disconnected" -> UiState.v3WidgetsInteractionEnabled.value = false
            "cancelled" -> result.cancel()
        }
        finish.complete(Unit)
        if (reason.endsWith(":cancelled")) result.join() else assertTrue(result.await().isFailure)
        assertTrue(order.isEmpty())
        assertNull(ParameterStoreV3.get(info))
    }

    @Test
    fun `creation at the domain limit or without a serial never changes profiles`() = runTest(dispatcher) {
        profileIds.add(2)
        create("current")
        assertTrue(runCatching { create("  ") }.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { sharedRepository.createProfileFromActive(any()) }
        assertTrue(order.isEmpty())
    }

    @Test
    fun `creation rechecks capacity after pending saves and does not reapply at the limit`() = runTest(dispatcher) {
        // Simulate an import filling the last slot between the use case read and the data layer read.
        var reads = 0
        coEvery { sharedRepository.getProfiles("current") } answers {
            if (++reads == 2) profileIds.add(2)
            profileIds.sorted().map { SettingsProfileInfo(it, null, it == activeId) }
        }
        create("current")
        coVerify(exactly = 0) { sharedRepository.createProfileFromActive(any()) }
        assertTrue(order.isEmpty())
    }

    @Test
    fun `device change during the last pre-creation read prevents copying current BLE values`() = runTest(dispatcher) {
        val readFinished = CompletableDeferred<Unit>()
        var reads = 0
        coEvery { sharedRepository.getProfiles("current") } coAnswers {
            if (++reads == 2) readFinished.await()
            profileIds.map { SettingsProfileInfo(it, null, it == activeId) }
        }
        val result = async { runCatching { create("current") } }
        runCurrent()
        serial = "other"
        readFinished.complete(Unit)
        assertTrue(result.await().exceptionOrNull() is CancellationException)
        coVerify(exactly = 0) { sharedRepository.createProfileFromActive(any()) }
        assertTrue(order.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(strings = ["  Работа  ", "123456789012345678901234567890"])
    fun `rename trims the name saves by ID and invalidates without applying or caching`(name: String) = runTest(dispatcher) {
        var updates = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.updates.drop(1).collect { updates++ } }
        rename("current", 3, name)
        coVerify(exactly = 1) { sharedRepository.renameProfile("current", 3, name.trim()) }
        coVerify(exactly = 0) { sharedRepository.switchToProfile(any(), any()); sharedRepository.createProfileFromActive(any()) }
        assertEquals(1, activeId)
        assertEquals(1, updates)
        assertTrue(order.isEmpty())
        assertNull(ParameterStoreV3.get(info))
        assertEquals("{}", cache.data)
        assertEquals(SETTINGS_PROFILE_NAME_MAX_LENGTH, V3SettingsProfileNameRules.MAX_LENGTH)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   ", "1234567890123456789012345678901"])
    fun `invalid names are rejected before any database write`(name: String) = runTest(dispatcher) {
        assertTrue(runCatching { rename("current", 3, name) }.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { sharedRepository.renameProfile(any(), any(), any()) }
        assertTrue(order.isEmpty())
    }

    @ParameterizedTest
    @ValueSource(ints = [-1, 0, 2, 4])
    fun `rename rejects missing and invalid profile IDs`(id: Int) = runTest(dispatcher) {
        assertTrue(runCatching { rename("current", id, "New") }.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { sharedRepository.renameProfile(any(), any(), any()) }
    }

    @Test
    fun `unchanged name and blank serial never write to the database`() = runTest(dispatcher) {
        rename("current", 3, "  Sport  ")
        assertTrue(runCatching { rename(" ", 3, "New") }.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { sharedRepository.renameProfile(any(), any(), any()) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["serial", "disconnected", "not-v3"])
    fun `rename cannot write when device access was lost`(reason: String) = runTest(dispatcher) {
        when (reason) {
            "serial" -> serial = "other"
            "disconnected" -> UiState.v3WidgetsInteractionEnabled.value = false
            "not-v3" -> UiState.isInterfaceV3Activated = false
        }
        assertTrue(runCatching { rename("current", 3, "New") }.isFailure)
        coVerify(exactly = 0) { sharedRepository.renameProfile(any(), any(), any()) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["missing", "error", "serial", "address", "disconnected", "cancelled"])
    fun `rename failure or late result invalidates once without applying values`(reason: String) = runTest(dispatcher) {
        val finish = CompletableDeferred<Unit>()
        var updates = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.updates.drop(1).collect { updates++ } }
        coEvery { sharedRepository.renameProfile("current", 3, "New") } coAnswers {
            finish.await()
            when (reason) {
                "missing" -> null
                "error" -> error("Database unavailable")
                else -> SettingsProfileInfo(3, "New", false)
            }
        }
        val result = async { runCatching { rename("current", 3, "New") } }
        runCurrent()
        when (reason) {
            "serial" -> serial = "other"
            "address" -> address = "other"
            "disconnected" -> UiState.v3WidgetsInteractionEnabled.value = false
            "cancelled" -> result.cancel()
        }
        finish.complete(Unit)
        if (reason == "cancelled") result.join() else assertTrue(result.await().isFailure)
        assertEquals(1, updates)
        assertTrue(order.isEmpty())
        assertNull(ParameterStoreV3.get(info))
    }

    @ParameterizedTest
    @ValueSource(ints = [-1, 0, 2, 4])
    fun `domain rejects invalid or missing profile IDs without selecting anything`(id: Int) = runTest(dispatcher) {
        assertTrue(runCatching { select("current", id) }.exceptionOrNull() is IllegalArgumentException)
        coVerify(exactly = 0) { sharedRepository.switchToProfile(any(), any()) }
        assertTrue(order.isEmpty())
    }
}
