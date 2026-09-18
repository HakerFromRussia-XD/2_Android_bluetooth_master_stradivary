package com.bailout.stickk.ubi4.versions.v3.presentation.accountprofile

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.di.V3AccountProfileViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.accountprofile.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3AccountProfileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val remote = FakeAccountProfileRemote()
    private val local = FakeAccountProfileLocal()
    private lateinit var vm: V3AccountProfileViewModel
    @BeforeEach fun setUp() {
        Dispatchers.setMain(dispatcher)
        vm = V3AccountProfileViewModelFactory(remote, local).create(V3AccountProfileViewModel::class.java)
        store.put("account", vm)
    }
    @AfterEach fun tearDown() { store.clear(); Dispatchers.resetMain() }
    private fun attach(background: Boolean = true, boards: Boolean = false) = vm.onAction(V3AccountProfileAction.ViewAttached(background, boards))
    private fun load() = vm.onAction(V3AccountProfileAction.LoadRequested)

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

    @Test fun `foreground waits for authorization but either cache makes content visible`() = runTest(dispatcher) {
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
