package com.bailout.stickk.ubi4.versions.v3.presentation.games

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.di.V3GamesViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.games.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class V3GamesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeV3GamesRepository()
    private val store = ViewModelStore()
    private lateinit var vm: V3GamesViewModel
    @BeforeEach fun setUp() {
        Dispatchers.setMain(dispatcher)
        vm = V3GamesViewModelFactory(repository).create(V3GamesViewModel::class.java)
        store.put("games", vm)
    }
    @AfterEach fun tearDown() { store.clear(); Dispatchers.resetMain() }
    private fun attach() = vm.onAction(V3GamesAction.ViewAttached)
    private fun click() = vm.onAction(V3GamesAction.PrimaryClicked)
    private fun commands() = vm.uiState.value.effects.map { it.command }
    private fun clearEffects() = vm.uiState.value.effects.forEach { vm.onAction(V3GamesAction.EffectHandled(it.id)) }

    @Test fun `attach and first resume share active request and later resume reloads`() = runTest(dispatcher) {
        val pending = CompletableDeferred<V3Game>()
        repository.load = { pending.await() }
        attach(); vm.onAction(V3GamesAction.ViewResumed); runCurrent()
        assertEquals(1, repository.loads)
        assertEquals(V3GameAction.UNAVAILABLE, vm.uiState.value.availability!!.action)
        pending.complete(repository.game); runCurrent()
        assertEquals(V3GameAction.INSTALL, vm.uiState.value.availability!!.action)
        assertTrue(vm.uiState.value.isActionEnabled)
        assertTrue(commands().isEmpty())
        vm.onAction(V3GamesAction.ViewResumed); runCurrent()
        assertEquals(2, repository.loads)
    }

    @Test fun `installed game plays offline and uses default launcher`() = runTest(dispatcher) {
        repository.installed = 10
        repository.load = { error("offline") }
        attach(); runCurrent()
        assertTrue(commands().isEmpty())
        click()
        val command = commands().single() as V3GamesCommand.LaunchGame
        assertEquals("${repository.packageName}.SuperTuxKartActivity", command.game.launcherActivity)
        assertTrue(vm.uiState.value.availability!!.isInstalled)
        assertEquals(0, repository.checks)
    }

    @Test fun `missing catalog failure retains message while blank url is silent until requested`() = runTest(dispatcher) {
        repository.load = { error("network message") }
        attach(); runCurrent()
        assertEquals(listOf(V3GamesCommand.ShowMessage(V3GamesMessage.MANIFEST_LOAD_FAILED, "network message")), commands())
        clearEffects()
        vm.onAction(V3GamesAction.ViewDetached)
        repository.manifestUrl = ""
        attach(); runCurrent()
        assertTrue(commands().isEmpty())
        assertFalse(vm.uiState.value.isActionEnabled)
        click()
        assertEquals(listOf(V3GamesCommand.ShowMessage(V3GamesMessage.MANIFEST_URL_MISSING)), commands())
    }

    @Test fun `install checks store once and opens it only after a positive result`() = runTest(dispatcher) {
        val result = CompletableDeferred<Boolean>()
        repository.check = { assertEquals(repository.packageName, it); result.await() }
        attach(); runCurrent(); click(); click(); runCurrent()
        assertEquals(1, repository.checks)
        assertFalse(vm.uiState.value.isActionEnabled)
        assertTrue(commands().isEmpty())
        result.complete(true); runCurrent()
        assertEquals(listOf(V3GamesCommand.OpenStore), commands())
        assertTrue(vm.uiState.value.isActionEnabled)
        clearEffects()
        assertTrue(commands().isEmpty())
    }

    @Test fun `unpublished game and store errors keep previous messages and restore button`() = runTest(dispatcher) {
        attach(); runCurrent()
        repository.check = { false }; click(); runCurrent()
        assertEquals(listOf(V3GamesCommand.ShowMessage(V3GamesMessage.NOT_PUBLISHED)), commands())
        assertTrue(vm.uiState.value.isActionEnabled)
        clearEffects()
        repository.check = { error("server") }; click(); runCurrent()
        assertEquals(listOf(V3GamesCommand.ShowMessage(V3GamesMessage.STORE_CHECK_FAILED)), commands())
        assertTrue(vm.uiState.value.isActionEnabled)
    }

    @Test fun `update opens store without availability request and play retains remote launcher`() = runTest(dispatcher) {
        repository.installed = 9
        attach(); runCurrent(); click()
        assertEquals(listOf(V3GamesCommand.OpenStore), commands())
        assertEquals(0, repository.checks)
        clearEffects()
        repository.installed = 10
        vm.onAction(V3GamesAction.ViewResumed); runCurrent(); click()
        assertEquals(listOf(V3GamesCommand.LaunchGame(repository.game)), commands())
    }

    @Test fun `delete rereads installed state and UI failures do not retry external actions`() = runTest(dispatcher) {
        repository.installed = 10
        attach(); runCurrent()
        vm.onAction(V3GamesAction.DeleteClicked)
        assertEquals(listOf(V3GamesCommand.UninstallGame), commands())
        clearEffects()
        vm.onAction(V3GamesAction.PlatformActionFailed(V3GamesMessage.UNINSTALL_FAILED))
        assertEquals(listOf(V3GamesCommand.ShowMessage(V3GamesMessage.UNINSTALL_FAILED)), commands())
        clearEffects()
        repository.installed = null
        vm.onAction(V3GamesAction.DeleteClicked)
        assertFalse(vm.uiState.value.availability!!.isInstalled)
        assertTrue(commands().isEmpty())
    }

    @Test fun `resume preserves previous render behavior even during store check`() = runTest(dispatcher) {
        val pending = CompletableDeferred<Boolean>()
        repository.check = { pending.await() }
        attach(); runCurrent(); click(); runCurrent()
        assertFalse(vm.uiState.value.isActionEnabled)
        vm.onAction(V3GamesAction.ViewResumed); runCurrent()
        assertTrue(vm.uiState.value.isActionEnabled)
        click(); runCurrent()
        assertEquals(1, repository.checks)
        pending.complete(false); runCurrent()
    }

    @Test fun `late noncooperative catalog cannot affect recreated view`() = runTest(dispatcher) {
        val pending = CompletableDeferred<V3Game>()
        repository.load = { withContext(NonCancellable) { pending.await() } }
        attach(); runCurrent()
        vm.onAction(V3GamesAction.ViewDetached)
        repository.load = { repository.game.copy(title = "New") }
        attach(); runCurrent()
        pending.complete(repository.game.copy(title = "Old")); runCurrent()
        assertEquals("New", vm.uiState.value.availability!!.game.title)
        assertTrue(commands().isEmpty())
    }

    @Test fun `detaching cancels store actions and clears queued effects`() = runTest(dispatcher) {
        val pending = CompletableDeferred<Boolean>()
        repository.check = { withContext(NonCancellable) { pending.await() } }
        attach(); runCurrent(); click(); runCurrent()
        vm.onAction(V3GamesAction.ViewDetached)
        pending.complete(true); runCurrent()
        click(); vm.onAction(V3GamesAction.DeleteClicked)
        assertTrue(commands().isEmpty())
        attach(); runCurrent(); repository.installed = 10
        vm.onAction(V3GamesAction.ViewResumed); runCurrent(); click()
        assertTrue(commands().isNotEmpty())
        vm.onAction(V3GamesAction.ViewDetached)
        assertTrue(commands().isEmpty())
    }

    @Test fun `cleared viewmodel ignores future input`() = runTest(dispatcher) {
        attach(); runCurrent(); store.clear()
        val loads = repository.loads
        attach(); vm.onAction(V3GamesAction.ViewResumed); click(); runCurrent()
        assertEquals(loads, repository.loads)
        assertTrue(commands().isEmpty())
    }
}
