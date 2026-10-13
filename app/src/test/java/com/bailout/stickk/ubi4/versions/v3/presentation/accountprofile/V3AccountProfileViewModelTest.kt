package com.bailout.stickk.ubi4.versions.v3.presentation.accountprofile

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.di.V3AccountProfileViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.*
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceRole
import com.bailout.stickk.ubi4.versions.v3.presentation.service.FakeV3DeviceRoleRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3AccountProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val remote = FakeAccountProfileRemote()
    private val local = FakeAccountProfileLocal()
    private val role = FakeV3DeviceRoleRepository()
    private lateinit var vm: V3AccountProfileViewModel
    @BeforeEach fun setUp() {
        Dispatchers.setMain(dispatcher)
        vm = V3AccountProfileViewModelFactory(remote, local, role).create(V3AccountProfileViewModel::class.java)
        store.put("account", vm)
    }
    @AfterEach fun tearDown() { store.clear(); Dispatchers.resetMain() }
    private fun attach(background: Boolean = true, boards: Boolean = false) = vm.onAction(V3AccountProfileAction.ViewAttached(background, boards))
    private fun load() = vm.onAction(V3AccountProfileAction.LoadRequested)

    @ParameterizedTest @EnumSource(V3DeviceRole::class)
    fun `stored role determines initial board actions before rendering without writes`(selected: V3DeviceRole) = runTest(dispatcher) {
        role.selected = selected
        val allowed = selected == V3DeviceRole.SERVICE_ENGINEER
        role.serviceEngineerAccess.value = !allowed
        role.interactionEnabled.value = false
        attach()
        assertEquals(allowed, vm.uiState.value.areBoardServiceActionsVisible)
        runCurrent()
        assertEquals(allowed, vm.uiState.value.areBoardServiceActionsVisible)
        assertEquals(selected, role.access)
        assertTrue(role.writes.isEmpty())
        assertTrue(local.writes.isEmpty())
        assertEquals(0, local.cacheWrites)
        assertEquals(0, remote.tokenCalls)
    }

    @Test fun `access changes affect only board actions without reloading or recaching the profile`() = runTest(dispatcher) {
        attach(); load(); runCurrent()
        val before = vm.uiState.value
        vm.onAction(V3AccountProfileAction.HeaderRendered(before.headerRevision))
        val writes = local.writes.toList()
        role.updateRoleAccess(V3DeviceRole.SERVICE_ENGINEER); runCurrent()
        assertEquals(before.copy(areBoardServiceActionsVisible = true), vm.uiState.value)
        role.interactionEnabled.value = false; runCurrent()
        assertTrue(vm.uiState.value.areBoardServiceActionsVisible)
        role.updateRoleAccess(V3DeviceRole.USER); runCurrent()
        assertEquals(before, vm.uiState.value)
        assertEquals(1, remote.tokenCalls)
        assertEquals(1, local.cacheWrites)
        assertEquals(writes, local.writes)
        assertTrue(role.writes.isEmpty())
    }

    @Test fun `detaching cancels role observation and reattaching restores the current role once`() = runTest(dispatcher) {
        attach(); attach(); runCurrent()
        assertEquals(1, role.serviceEngineerAccess.subscriptionCount.value)
        vm.onAction(V3AccountProfileAction.ViewDetached)
        val detached = vm.uiState.value
        role.setSelectedRole(V3DeviceRole.SERVICE_ENGINEER); runCurrent()
        assertEquals(0, role.serviceEngineerAccess.subscriptionCount.value)
        assertEquals(detached, vm.uiState.value)
        attach()
        assertTrue(vm.uiState.value.areBoardServiceActionsVisible)
        runCurrent()
        assertEquals(1, role.serviceEngineerAccess.subscriptionCount.value)
        assertEquals(listOf(V3DeviceRole.SERVICE_ENGINEER), role.writes)
        assertEquals(0, remote.tokenCalls)
    }

    @Test fun `clearing the screen cancels role observation and late attach cannot restart it`() = runTest(dispatcher) {
        attach(); runCurrent()
        store.clear()
        val cleared = vm.uiState.value
        role.setSelectedRole(V3DeviceRole.SERVICE_ENGINEER)
        attach(); runCurrent()
        assertEquals(cleared, vm.uiState.value)
        assertEquals(0, role.serviceEngineerAccess.subscriptionCount.value)
        assertEquals(0, remote.tokenCalls)
    }

    @Test fun `background attach shows initial header without requesting or caching until transition action`() = runTest(dispatcher) {
        attach(); attach(); runCurrent()
        assertTrue(vm.uiState.value.isContentVisible)
        assertEquals(V3AccountProfileHeader(), vm.uiState.value.header)
        assertEquals(0, remote.tokenCalls)
        assertEquals(0, local.cacheWrites)
        load(); runCurrent()
        val state = vm.uiState.value
        assertEquals("First", state.header?.firstName)
        assertEquals(V3AccountProfileVersions("1.23", "2.34", "3.45"), state.header?.versions)
        assertTrue(state.isTokenLoaded)
        assertEquals(1, state.refreshCompletionId)
        assertEquals(1, remote.tokenCalls)
        assertEquals(0, local.cacheWrites)
        vm.onAction(V3AccountProfileAction.HeaderRendered(state.headerRevision))
        assertEquals(state.header, local.storedHeader)
        assertEquals(1, local.cacheWrites)
    }

    @Test fun `foreground accepts profile cache only after the current serial has been observed`() = runTest(dispatcher) {
        attach(background = false)
        assertFalse(vm.uiState.value.isContentVisible)
        assertNull(vm.uiState.value.header)
        vm.onAction(V3AccountProfileAction.ViewDetached)
        attach(background = false, boards = true)
        assertTrue(vm.uiState.value.isContentVisible)
        assertNull(vm.uiState.value.header)
        vm.onAction(V3AccountProfileAction.ViewDetached)
        local.storedHeader = V3AccountProfileHeader("Cached", "Name")
        attach(background = false)
        assertTrue(vm.uiState.value.isContentVisible)
        assertTrue(vm.uiState.value.hasCachedProfile)
        assertEquals("Cached", vm.uiState.value.header?.firstName)
        assertEquals(0, remote.tokenCalls)
    }

    @Test fun `unknown serial hides previous profile and waits without using Bluetooth name or fallback`() = runTest(dispatcher) {
        local.serialNumbers.value = null
        local.currentEnvironment = local.currentEnvironment.copy(device = local.currentEnvironment.device.copy(name = "00001"))
        local.storedHeader = V3AccountProfileHeader("Previous", "User")
        local.values[V3AccountDetail.SIDE] = "Right"

        attach(background = false); load(); runCurrent()

        assertEquals(0, remote.tokenCalls)
        assertTrue(remote.trace.isEmpty())
        assertFalse(vm.uiState.value.isTokenLoaded)
        assertFalse(vm.uiState.value.hasCachedProfile)
        assertNull(vm.uiState.value.header)
        assertEquals("", local.getDetail(V3AccountDetail.SIDE))

        local.currentEnvironment = local.currentEnvironment.copy(device = local.currentEnvironment.device.copy(name = "FEST-F-06879"))
        vm.onAction(V3AccountProfileAction.HeaderRefreshRequested)
        load(); runCurrent()
        assertEquals(0, remote.tokenCalls)
    }

    @Test fun `late real serial resumes the requested load and normalized duplicate does not authenticate again`() = runTest(dispatcher) {
        local.serialNumbers.value = null
        local.currentEnvironment = local.currentEnvironment.copy(device = local.currentEnvironment.device.copy(name = "00001"))
        remote.user = { _, _ -> V3AccountProfileResult.Success(V3AccountProfile("", "", 16207, "", "")) }
        remote.devices = { _, _, _ -> V3AccountProfileResult.Success(listOf(
            V3AccountDevice(9352, "FEST-H-08670"), V3AccountDevice(14063, "FEST-HM-13261"),
            V3AccountDevice(14402, "FEST-HM-13557"),
        )) }
        remote.info = { id, _, _ ->
            assertEquals(14063, id)
            V3AccountProfileResult.Success(V3AccountDeviceInfo("FEST.HS.30.000.000", "L", "Left", "In repair", "", "", emptyList()))
        }
        attach(); load(); runCurrent()
        assertEquals(0, remote.tokenCalls)

        local.serialNumbers.value = " FEST-HM-13261\u0000 "
        runCurrent()
        assertEquals(1, remote.tokenCalls)
        assertEquals("token:FEST-HM-13261", remote.trace.first())
        assertTrue(remote.trace.contains("devices:16207:token:ru"))
        assertTrue(remote.trace.contains("info:14063:token:ru"))
        assertEquals("Left", local.getDetail(V3AccountDetail.SIDE))
        assertTrue(vm.uiState.value.isTokenLoaded)

        local.serialNumbers.value = "FEST-HM-13261"
        runCurrent()
        assertEquals(1, remote.tokenCalls)
        assertEquals(listOf(null, "FEST-HM-13261"), local.selectedSerialNumbers)
    }

    @Test fun `retrying while serial is missing starts a fresh read and authenticates exactly once when it arrives`() = runTest(dispatcher) {
        var serialReads = 0
        var activeObservers = 0
        val retryLocal = object : V3AccountProfileLocalRepository by local {
            override fun observeSerialNumber() = flow<String?> {
                serialReads++
                activeObservers++
                try {
                    emit(if (serialReads == 1) null else "FEST-test")
                    awaitCancellation()
                } finally {
                    activeObservers--
                }
            }
        }
        vm = V3AccountProfileViewModelFactory(remote, retryLocal, role).create(V3AccountProfileViewModel::class.java)
        store.put("account", vm)

        attach(); load(); runCurrent()
        assertEquals(1, serialReads)
        assertEquals(1, activeObservers)
        assertEquals(0, remote.tokenCalls)

        load(); runCurrent()
        assertEquals(2, serialReads)
        assertEquals(1, activeObservers)
        assertEquals(1, remote.tokenCalls)
        assertEquals(listOf("token:FEST-test"), remote.trace.filter { it.startsWith("token:") })
        assertTrue(vm.uiState.value.isTokenLoaded)
        assertEquals("First", vm.uiState.value.header?.firstName)

        attach(); runCurrent()
        assertEquals(2, serialReads)
        assertEquals(1, remote.tokenCalls)
        vm.onAction(V3AccountProfileAction.ViewDetached)
        runCurrent()
        assertEquals(0, activeObservers)
    }

    @Test fun `serial observation alone does not start network and detach removes every observer`() = runTest(dispatcher) {
        local.serialNumbers.value = null
        attach(); runCurrent()
        local.serialNumbers.value = "FEST-HM-13261"
        runCurrent()
        assertEquals(0, remote.tokenCalls)
        assertEquals(1, local.serialNumbers.subscriptionCount.value)

        vm.onAction(V3AccountProfileAction.ViewDetached)
        runCurrent()
        assertEquals(0, local.serialNumbers.subscriptionCount.value)
        assertEquals(0, role.serviceEngineerAccess.subscriptionCount.value)
        local.serialNumbers.value = "FEST-HM-13557"
        runCurrent()
        assertEquals(0, remote.tokenCalls)
        assertEquals("FEST-HM-13261", local.selectedSerialNumber)
    }

    @Test fun `losing the serial invalidates previous details and restarts a requested load only on fresh identity`() = runTest(dispatcher) {
        attach(); load(); runCurrent()
        val original = vm.uiState.value
        vm.onAction(V3AccountProfileAction.HeaderRendered(original.headerRevision))
        assertEquals("First", local.storedHeader?.firstName)

        local.serialNumbers.value = null
        runCurrent()
        assertEquals(1, remote.tokenCalls)
        assertFalse(vm.uiState.value.isTokenLoaded)
        assertFalse(vm.uiState.value.hasCachedProfile)
        assertEquals("", vm.uiState.value.header?.firstName)
        assertNull(local.storedHeader)
        assertTrue(local.values.values.all { it.isEmpty() })

        local.serialNumbers.value = "FEST-test"
        runCurrent()
        assertEquals(2, remote.tokenCalls)
        assertTrue(vm.uiState.value.isTokenLoaded)
        assertEquals("First", vm.uiState.value.header?.firstName)
    }

    @Test fun `authorized content appears before slow user response and later error keeps its existing header`() = runTest(dispatcher) {
        val response = CompletableDeferred<V3AccountProfileResult<V3AccountProfile>>()
        remote.user = { _, _ -> response.await() }
        attach(background = false); load(); runCurrent()
        assertTrue(vm.uiState.value.isContentVisible)
        assertTrue(vm.uiState.value.isTokenLoaded)
        assertNull(vm.uiState.value.header)
        response.complete(V3AccountProfileResult.Error(500, "user error")); runCurrent()
        assertEquals("user error", vm.uiState.value.messages.single().serverMessage)
        assertNull(vm.uiState.value.header)
        assertTrue(local.writes.isEmpty())
    }

    @Test fun `retry counter survives refresh actions and resets only for a new view`() = runTest(dispatcher) {
        remote.token = { V3AccountProfileResult.Error(500, "HTTP 500") }
        attach(); load(); runCurrent()
        assertEquals(4, remote.tokenCalls)
        assertEquals(4, vm.uiState.value.refreshCompletionId)
        val first = vm.uiState.value.messages.single()
        assertNull(first.serverMessage)
        vm.onAction(V3AccountProfileAction.MessageShown(first.id))
        load(); runCurrent()
        assertEquals(5, remote.tokenCalls)
        assertEquals(1, vm.uiState.value.messages.size)
        assertNotEquals(first.id, vm.uiState.value.messages.single().id)
        vm.onAction(V3AccountProfileAction.ViewDetached); attach(); load(); runCurrent()
        assertEquals(9, remote.tokenCalls)
        assertEquals(1, vm.uiState.value.messages.size)
    }

    @Test fun `render acknowledgements and local UI refresh do not send network requests or replay messages`() = runTest(dispatcher) {
        remote.token = { V3AccountProfileResult.Error(null, "offline") }
        attach(); load(); runCurrent()
        val failed = vm.uiState.value
        vm.onAction(V3AccountProfileAction.MessageShown(failed.messages.single().id))
        vm.onAction(V3AccountProfileAction.HeaderRefreshRequested)
        vm.onAction(V3AccountProfileAction.HeaderRendered(failed.headerRevision))
        assertEquals(0, local.cacheWrites)
        vm.onAction(V3AccountProfileAction.HeaderRendered(vm.uiState.value.headerRevision))
        assertEquals(1, local.cacheWrites)
        assertTrue(vm.uiState.value.messages.isEmpty())
        assertEquals(1, remote.tokenCalls)
    }

    @Test fun `token failure clears details but displays current names rather than cached names`() = runTest(dispatcher) {
        local.storedHeader = V3AccountProfileHeader("Cached", "Different")
        remote.token = { V3AccountProfileResult.Error(401, "error") }
        attach(); load(); runCurrent()
        assertEquals("", vm.uiState.value.header?.firstName)
        assertTrue(vm.uiState.value.isContentVisible)
        assertEquals(V3AccountDetail.entries.toSet(), local.values.keys)
        assertTrue(local.values.values.all { it.isEmpty() })
    }

    @ParameterizedTest @ValueSource(strings = ["token", "user", "devices", "info"])
    fun `destroying view cancels every request stage and ignores late noncooperative results`(stage: String) = runTest(dispatcher) {
        val release = CompletableDeferred<Unit>()
        suspend fun waitForRelease() = withContext(NonCancellable) { release.await() }
        when (stage) {
            "token" -> remote.token = { waitForRelease(); V3AccountProfileResult.Success("late") }
            "user" -> remote.user = { _, _ -> waitForRelease(); V3AccountProfileResult.Success(V3AccountProfile("Late", "", 8, "", "")) }
            "devices" -> remote.devices = { _, _, _ -> waitForRelease(); V3AccountProfileResult.Success(listOf(V3AccountDevice(9, "FEST-test"))) }
            "info" -> remote.info = { _, _, _ -> waitForRelease(); V3AccountProfileResult.Success(V3AccountDeviceInfo("Late", "", "", "", "", "", emptyList())) }
        }
        attach(); load(); runCurrent()
        vm.onAction(V3AccountProfileAction.ViewDetached)
        val detached = vm.uiState.value
        val writeCount = local.writes.size
        release.complete(Unit); runCurrent()
        load(); vm.onAction(V3AccountProfileAction.HeaderRefreshRequested)
        vm.onAction(V3AccountProfileAction.HeaderRendered(detached.headerRevision)); runCurrent()
        assertEquals(detached, vm.uiState.value)
        assertEquals(writeCount, local.writes.size)
        assertEquals(0, local.cacheWrites)
        assertEquals(1, remote.tokenCalls)
    }

    @ParameterizedTest @ValueSource(strings = ["token", "user", "devices", "info"])
    fun `serial switch cancels every old request stage and ignores late noncooperative results`(stage: String) = runTest(dispatcher) {
        val release = CompletableDeferred<Unit>()
        suspend fun delayOldResponse(requestStage: String) {
            if (stage == requestStage) withContext(NonCancellable) { release.await() }
        }
        remote.token = { serial ->
            if (serial == "FEST-test") delayOldResponse("token")
            V3AccountProfileResult.Success(if (serial == "FEST-test") "old" else "new")
        }
        remote.user = { token, _ ->
            if (token == "old") delayOldResponse("user")
            V3AccountProfileResult.Success(V3AccountProfile(if (token == "old") "Old user" else "Current user", "", if (token == "old") 7 else 16207, "", ""))
        }
        remote.devices = { _, token, _ ->
            if (token == "old") delayOldResponse("devices")
            V3AccountProfileResult.Success(listOf(V3AccountDevice(9, "FEST-test"), V3AccountDevice(14063, "FEST-HM-13261")))
        }
        remote.info = { id, _, _ ->
            if (id == 9) delayOldResponse("info")
            V3AccountProfileResult.Success(V3AccountDeviceInfo(if (id == 9) "Old model" else "Current model", "L", if (id == 9) "Right" else "Left", "", "", "", emptyList()))
        }
        attach(); load(); runCurrent()

        local.serialNumbers.value = "FEST-HM-13261"
        runCurrent()
        assertEquals(2, remote.tokenCalls)
        assertEquals("Current user", vm.uiState.value.header?.firstName)
        assertEquals("Current model", local.getDetail(V3AccountDetail.MODEL))
        assertEquals("Left", local.getDetail(V3AccountDetail.SIDE))
        val currentState = vm.uiState.value
        val currentWrites = local.writes.toList()

        release.complete(Unit)
        runCurrent()
        assertEquals(currentState, vm.uiState.value)
        assertEquals(currentWrites, local.writes)
        assertEquals("Left", local.getDetail(V3AccountDetail.SIDE))
    }

    @Test fun `viewmodel clearing prevents late actions from creating another request session`() = runTest(dispatcher) {
        attach(); load(); runCurrent()
        store.clear()
        val before = vm.uiState.value
        attach(); load(); vm.onAction(V3AccountProfileAction.HeaderRefreshRequested); runCurrent()
        assertEquals(1, remote.tokenCalls)
        assertEquals(before, vm.uiState.value)
    }

    @Test fun `foreground restoration of collectors and duplicate attach do not launch a new load`() = runTest(dispatcher) {
        attach(); load(); runCurrent()
        val first = launch { vm.uiState.collect {} }; runCurrent(); first.cancel()
        attach()
        val second = launch { vm.uiState.collect {} }; runCurrent(); second.cancel()
        assertEquals(1, remote.tokenCalls)
    }
}
