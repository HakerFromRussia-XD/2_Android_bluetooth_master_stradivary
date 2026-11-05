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
    private fun click(packageName: String = repository.packageName) = vm.onAction(V3GamesAction.PrimaryClicked(packageName))
    private fun card(packageName: String = repository.packageName) = vm.uiState.value.cards.single { it.availability.game.packageName == packageName }
    private fun commands() = vm.uiState.value.effects.map { it.command }
    private fun clearEffects() = vm.uiState.value.effects.forEach { vm.onAction(V3GamesAction.EffectHandled(it.id)) }

    @Test fun `attach and first resume share a catalog request and render absent Sushi as download`() = runTest(dispatcher) {
        val pending = CompletableDeferred<List<V3Game>>()
        repository.load = { pending.await() }
        attach(); vm.onAction(V3GamesAction.ViewResumed); runCurrent()
        assertEquals(1, repository.loads)
        assertEquals(V3GameAction.INSTALL, card(repository.sushi.packageName).availability.action)
        assertTrue(card(repository.sushi.packageName).isActionEnabled)
        pending.complete(listOf(repository.game)); runCurrent()
        assertEquals(V3GameAction.INSTALL, card().availability.action)
        assertTrue(commands().isEmpty())
        vm.onAction(V3GamesAction.ViewResumed); runCurrent()
        assertEquals(2, repository.loads)
    }

    @Test fun `installed game plays offline with its own fallback launcher`() = runTest(dispatcher) {
        repository.installedVersions[repository.sushi.packageName] = 10
        repository.load = { error("offline") }
        attach(); runCurrent(); click(repository.sushi.packageName)
        assertEquals(listOf(V3GamesCommand.LaunchGame(repository.sushi)), commands())
        assertTrue(card(repository.sushi.packageName).availability.isInstalled)
        assertEquals(V3GameAction.INSTALL, card().availability.action)
        assertEquals(0, repository.checks)
    }

    @Test fun `install checks selected package once and keeps other cards usable`() = runTest(dispatcher) {
        val result = CompletableDeferred<Boolean>()
        repository.check = { assertEquals(repository.sushi.packageName, it); result.await() }
        repository.installed = 10
        attach(); runCurrent(); click(repository.sushi.packageName); click(repository.sushi.packageName); runCurrent()
        assertEquals(1, repository.checks)
        assertFalse(card(repository.sushi.packageName).isActionEnabled)
        assertTrue(card().isActionEnabled)
        vm.onAction(V3GamesAction.ViewResumed); runCurrent()
        assertFalse(card(repository.sushi.packageName).isActionEnabled)
        result.complete(true); runCurrent()
        assertEquals(listOf(V3GamesCommand.OpenStore(repository.sushi.packageName)), commands())
        assertTrue(card(repository.sushi.packageName).isActionEnabled)
    }

    @Test fun `store checks for two cards are independent and route to their own packages`() = runTest(dispatcher) {
        val pending = CompletableDeferred<Boolean>()
        repository.check = { if (it == repository.sushi.packageName) pending.await() else true }
        attach(); runCurrent(); click(repository.sushi.packageName); click(); runCurrent()
        assertEquals(listOf(V3GamesCommand.OpenStore(repository.packageName)), commands())
        assertFalse(card(repository.sushi.packageName).isActionEnabled)
        pending.complete(true); runCurrent()
        assertEquals(V3GamesCommand.OpenStore(repository.sushi.packageName), commands().last())
    }

    @Test fun `unpublished game and network errors restore download button without launching`() = runTest(dispatcher) {
        attach(); runCurrent()
        repository.check = { false }; click(repository.sushi.packageName); runCurrent()
        assertEquals(listOf(V3GamesCommand.ShowMessage(V3GamesMessage.NOT_PUBLISHED)), commands())
        assertTrue(card(repository.sushi.packageName).isActionEnabled)
        clearEffects()
        repository.check = { error("server") }; click(repository.sushi.packageName); runCurrent()
        assertEquals(listOf(V3GamesCommand.ShowMessage(V3GamesMessage.STORE_CHECK_FAILED)), commands())
        assertTrue(card(repository.sushi.packageName).isActionEnabled)
    }

    @Test fun `update and delete route to selected game and removal is reread on resume`() = runTest(dispatcher) {
        repository.load = { listOf(repository.game, repository.sushi.copy(versionCode = 20)) }
        repository.installedVersions[repository.sushi.packageName] = 10
        attach(); runCurrent(); click(repository.sushi.packageName)
        assertEquals(listOf(V3GamesCommand.OpenStore(repository.sushi.packageName)), commands())
        assertEquals(0, repository.checks)
        clearEffects()
        vm.onAction(V3GamesAction.DeleteClicked(repository.sushi.packageName))
        assertEquals(listOf(V3GamesCommand.UninstallGame(repository.sushi.packageName)), commands())
        clearEffects()
        repository.installedVersions.remove(repository.sushi.packageName)
        vm.onAction(V3GamesAction.ViewResumed); runCurrent()
        assertFalse(card(repository.sushi.packageName).availability.isInstalled)
        assertEquals(V3GameAction.INSTALL, card(repository.sushi.packageName).availability.action)
        vm.onAction(V3GamesAction.DeleteClicked(repository.sushi.packageName))
        assertTrue(commands().isEmpty())
    }

    @Test fun `tap rechecks installation instead of using stale play state`() = runTest(dispatcher) {
        repository.installedVersions[repository.sushi.packageName] = 1
        attach(); runCurrent()
        assertEquals(V3GameAction.PLAY, card(repository.sushi.packageName).availability.action)
        repository.installedVersions.remove(repository.sushi.packageName)
        click(repository.sushi.packageName); runCurrent()
        assertEquals(listOf(V3GamesCommand.OpenStore(repository.sushi.packageName)), commands())
        assertEquals(V3GameAction.INSTALL, card(repository.sushi.packageName).availability.action)
    }

    @Test fun `catalog can add 200 games without game-specific actions and survives offline refresh`() = runTest(dispatcher) {
        val games = (1..200).map { V3Game("Game $it", "example.game$it", "example.game$it.Main", 10) }
        repository.load = { games }
        repository.installedVersions[games.last().packageName] = 10
        attach(); runCurrent()
        assertEquals(202, vm.uiState.value.cards.size)
        click(games.last().packageName)
        assertEquals(listOf(V3GamesCommand.LaunchGame(games.last())), commands())
        clearEffects()
        repository.load = { error("offline") }
        vm.onAction(V3GamesAction.ViewResumed); runCurrent()
        assertEquals(202, vm.uiState.value.cards.size)
        click(games.first().packageName); runCurrent()
        assertEquals(listOf(V3GamesCommand.OpenStore(games.first().packageName)), commands())
        clearEffects(); click("unknown.package")
        assertTrue(commands().isEmpty())
    }

    @Test fun `late noncooperative catalog cannot affect recreated view`() = runTest(dispatcher) {
        val pending = CompletableDeferred<List<V3Game>>()
        repository.load = { withContext(NonCancellable) { pending.await() } }
        attach(); runCurrent()
        vm.onAction(V3GamesAction.ViewDetached)
        repository.load = { listOf(repository.game.copy(title = "New")) }
        attach(); runCurrent()
        pending.complete(listOf(repository.game.copy(title = "Old"))); runCurrent()
        assertEquals("New", card().availability.game.title)
        assertTrue(commands().isEmpty())
    }

    @Test fun `detaching cancels store actions and clears queued effects`() = runTest(dispatcher) {
        val pending = CompletableDeferred<Boolean>()
        repository.check = { withContext(NonCancellable) { pending.await() } }
        attach(); runCurrent(); click(repository.sushi.packageName); runCurrent()
        vm.onAction(V3GamesAction.ViewDetached)
        pending.complete(true); runCurrent()
        click(); vm.onAction(V3GamesAction.DeleteClicked(repository.sushi.packageName))
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
