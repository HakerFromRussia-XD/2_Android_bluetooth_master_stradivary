package com.bailout.stickk.ubi4.versions.v3.presentation.firmware

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.InstallServiceFirmwareForDebugUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.InstallServiceFirmwareUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3ServiceFirmwareLocalFile
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3ServiceFirmwareUpdateRepository
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3ServiceFirmwareUpdateResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class V3ServiceFirmwareViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val uiScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val attempts = mutableListOf<Attempt>()
    private val debugAttempts = mutableListOf<DebugAttempt>()
    private val effects = mutableListOf<V3ServiceFirmwareEffect>()
    private val file = V3ServiceFirmwareLocalFile("FAM-12.bin", "/cache/FAM-12.bin")
    private var nonCooperative = false
    private var nonCooperativeDebug = false
    private var debugGuardCalls = 0
    private var debugGuardFailure: IllegalStateException? = null
    private var collector: Job? = null
    private lateinit var vm: V3ServiceFirmwareViewModel

    private class Attempt(
        val boardAddress: Int,
        val file: V3ServiceFirmwareLocalFile,
        val onProgress: (Int) -> Unit,
        val onPhaseChanged: (String, Long) -> Unit,
    ) {
        val result = CompletableDeferred<V3ServiceFirmwareUpdateResult>()
        var ended = false
    }

    private class DebugAttempt(val file: V3ServiceFirmwareLocalFile) {
        val result = CompletableDeferred<Unit>()
        var ended = false
    }

    // Extra state prevents coroutine stacktrace recovery from copying this exception.
    private class FirmwareReadFailure(val filePath: String) : Exception("package cannot be read")

    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
        val repository = object : V3ServiceFirmwareUpdateRepository {
            override fun requireDebugInstallationAllowed() {
                debugGuardCalls++
                debugGuardFailure?.let { throw it }
            }

            override suspend fun installForDebug(file: V3ServiceFirmwareLocalFile) {
                val attempt = DebugAttempt(file)
                debugAttempts += attempt
                try {
                    if (nonCooperativeDebug) withContext(NonCancellable) { attempt.result.await() }
                    else attempt.result.await()
                } finally {
                    attempt.ended = true
                }
            }

            override suspend fun install(
                boardAddress: Int,
                file: V3ServiceFirmwareLocalFile,
                onProgress: (Int) -> Unit,
                onPhaseChanged: (String, Long) -> Unit,
            ): V3ServiceFirmwareUpdateResult {
                val attempt = Attempt(boardAddress, file, onProgress, onPhaseChanged)
                attempts += attempt
                try {
                    return if (nonCooperative) withContext(NonCancellable) { attempt.result.await() }
                    else attempt.result.await()
                } finally {
                    attempt.ended = true
                }
            }
        }
        vm = V3ServiceFirmwareViewModel(
            InstallServiceFirmwareUseCaseV3(repository),
            InstallServiceFirmwareForDebugUseCaseV3(repository),
        )
        store.put("serviceFirmware", vm)
        collectEffects()
    }

    @AfterEach fun cleanup() {
        store.clear()
        attempts.forEach { it.result.complete(V3ServiceFirmwareUpdateResult.Success) }
        debugAttempts.forEach { it.result.complete(Unit) }
        uiScope.cancel()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    private fun collectEffects() {
        collector = uiScope.launch { vm.effects.collect { effects += it } }
        dispatcher.scheduler.runCurrent()
    }

    private fun create() { vm.onAction(V3ServiceFirmwareAction.ViewCreated) }

    private fun confirm(requestId: Long = 1, address: Int = 0, selectedFile: V3ServiceFirmwareLocalFile = file) {
        vm.onAction(V3ServiceFirmwareAction.InstallConfirmed(requestId, address, selectedFile))
        dispatcher.scheduler.runCurrent()
    }

    private fun debug(selectedFile: V3ServiceFirmwareLocalFile = file) {
        vm.onAction(V3ServiceFirmwareAction.DebugInstallRequested(selectedFile))
        dispatcher.scheduler.runCurrent()
    }

    private fun complete(attempt: Attempt, result: V3ServiceFirmwareUpdateResult = V3ServiceFirmwareUpdateResult.Success) {
        assertTrue(attempt.result.complete(result))
        dispatcher.scheduler.runCurrent()
    }

    private fun complete(attempt: DebugAttempt) {
        assertTrue(attempt.result.complete(Unit))
        dispatcher.scheduler.runCurrent()
    }

    private fun progress(attempt: Attempt, percent: Int) {
        attempt.onProgress(percent)
        dispatcher.scheduler.runCurrent()
    }

    private fun advanceTime(milliseconds: Long) {
        dispatcher.scheduler.advanceTimeBy(milliseconds)
        dispatcher.scheduler.runCurrent()
    }

    @Test fun `confirmation forwards selected board and file only while the activity exists`() {
        confirm()
        assertTrue(attempts.isEmpty())
        assertEquals(V3ServiceFirmwareUiState(), vm.uiState.value)
        create()
        val selectedFile = V3ServiceFirmwareLocalFile(" BOARD 7.bin ", "/cache/ BOARD 7.bin ")
        confirm(14, 7, selectedFile)
        assertEquals(1, attempts.size)
        assertEquals(7, attempts.single().boardAddress)
        assertSame(selectedFile, attempts.single().file)
        assertEquals(mapOf(14L to 0), vm.uiState.value.progressByRequest)
        assertTrue(effects.isEmpty())
    }

    @Test fun `progress rendering and repeated creation never start a second installation`() {
        create()
        confirm()
        val attempt = attempts.single()
        val initialState = vm.uiState.value
        for (percent in listOf(-1, 0, 39, 100, 110)) {
            progress(attempt, percent)
            repeat(3) { create(); assertEquals(percent, vm.uiState.value.progressByRequest[1L]) }
        }
        assertEquals(mapOf(1L to 0), initialState.progressByRequest)
        assertEquals(1, attempts.size)
        assertFalse(attempt.ended)
        assertTrue(effects.isEmpty())
    }

    @Test fun `concurrent confirmations keep independent progress and acknowledge only their own result`() {
        create()
        confirm(1, 0)
        val secondFile = V3ServiceFirmwareLocalFile("HHS-4.bin", "/cache/HHS-4.bin")
        confirm(2, 2, secondFile)
        assertEquals(listOf(0, 2), attempts.map { it.boardAddress })
        assertEquals(listOf(file, secondFile), attempts.map { it.file })
        progress(attempts[0], 20)
        progress(attempts[1], 75)
        assertEquals(mapOf(1L to 20, 2L to 75), vm.uiState.value.progressByRequest)
        complete(attempts[1], V3ServiceFirmwareUpdateResult.CrcMismatch)
        assertEquals(listOf(V3ServiceFirmwareEffect.Completed(2, V3ServiceFirmwareUpdateResult.CrcMismatch)), effects)
        assertFalse(attempts[0].ended)
        vm.onAction(V3ServiceFirmwareAction.ResultShown(2))
        assertEquals(mapOf(1L to 20), vm.uiState.value.progressByRequest)
        complete(attempts[0])
        assertEquals(V3ServiceFirmwareEffect.Completed(1, V3ServiceFirmwareUpdateResult.Success), effects.last())
        vm.onAction(V3ServiceFirmwareAction.ResultShown(1))
        assertEquals(V3ServiceFirmwareUiState(), vm.uiState.value)
        assertEquals(2, attempts.size)
    }

    @Test fun `all protocol outcomes reach UI unchanged and acknowledgement never retries`() {
        create()
        val results = listOf(
            V3ServiceFirmwareUpdateResult.Success,
            V3ServiceFirmwareUpdateResult.BootEntryVerified,
            V3ServiceFirmwareUpdateResult.StartSystemUpdateRejected("status=2"),
            V3ServiceFirmwareUpdateResult.CheckNewFirmwareRejected("status=0", true),
            V3ServiceFirmwareUpdateResult.CheckNewFirmwareRejected("status=5", false),
            V3ServiceFirmwareUpdateResult.PreloadFailed,
            V3ServiceFirmwareUpdateResult.CrcMismatch,
        )
        results.forEachIndexed { index, result ->
            val id = index.toLong() + 1
            confirm(id)
            complete(attempts.last(), result)
            assertEquals(V3ServiceFirmwareEffect.Completed(id, result), effects.last())
            assertEquals(0, vm.uiState.value.progressByRequest[id])
            vm.onAction(V3ServiceFirmwareAction.ResultShown(id))
            vm.onAction(V3ServiceFirmwareAction.ResultShown(id))
            assertTrue(vm.uiState.value.progressByRequest.isEmpty())
            assertEquals(index + 1, attempts.size)
        }
        assertEquals(results.size, effects.size)
    }

    @Test fun `stall is nonterminal and handled effects are not replayed to a replacement collector`() {
        create()
        confirm()
        val attempt = attempts.single()
        progress(attempt, 40)
        attempt.onPhaseChanged("coordinator", 240L)
        advanceTime(60_000)
        assertEquals(listOf(V3ServiceFirmwareEffect.Stalled(1, "coordinator", 40, 240L)), effects)
        assertEquals(40, vm.uiState.value.progressByRequest[1L])
        assertFalse(attempt.ended)
        complete(attempt)
        assertEquals(V3ServiceFirmwareEffect.Completed(1, V3ServiceFirmwareUpdateResult.Success), effects.last())
        collector?.cancel()
        dispatcher.scheduler.runCurrent()
        effects.clear()
        collectEffects()
        assertTrue(effects.isEmpty())
        vm.onAction(V3ServiceFirmwareAction.ResultShown(1))
        assertTrue(vm.uiState.value.progressByRequest.isEmpty())
        assertEquals(1, attempts.size)
    }

    @Test fun `failure retains the original exception and progress until the result is shown`() {
        create()
        confirm()
        val failure = FirmwareReadFailure(file.path)
        progress(attempts.single(), 9)
        assertTrue(attempts.single().result.completeExceptionally(failure))
        dispatcher.scheduler.runCurrent()
        assertEquals(1, effects.size)
        val effect = effects.single() as V3ServiceFirmwareEffect.Failed
        assertEquals(1L, effect.requestId)
        assertSame(failure, effect.error)
        assertEquals(mapOf(1L to 9), vm.uiState.value.progressByRequest)
        vm.onAction(V3ServiceFirmwareAction.ResultShown(1))
        assertTrue(vm.uiState.value.progressByRequest.isEmpty())
        confirm(2)
        assertEquals(2, attempts.size)
    }

    @Test fun `cancellation is silent and does not report failure or automatically retry`() {
        create()
        confirm()
        progress(attempts.single(), 17)
        assertTrue(attempts.single().result.completeExceptionally(CancellationException("activity cancelled")))
        dispatcher.scheduler.runCurrent()
        assertTrue(attempts.single().ended)
        assertTrue(effects.isEmpty())
        assertEquals(mapOf(1L to 17), vm.uiState.value.progressByRequest)
        create()
        assertEquals(1, attempts.size)
        vm.onAction(V3ServiceFirmwareAction.ViewDestroyed)
        assertEquals(V3ServiceFirmwareUiState(), vm.uiState.value)
    }

    @Test fun `pausing UI collection for navigation or STOP does not cancel an activity owned installation`() {
        create()
        confirm()
        collector?.cancel()
        dispatcher.scheduler.runCurrent()
        val attempt = attempts.single()
        progress(attempt, 66)
        assertFalse(attempt.ended)
        assertEquals(mapOf(1L to 66), vm.uiState.value.progressByRequest)
        complete(attempt)
        assertTrue(effects.isEmpty())
        collectEffects()
        assertEquals(listOf(V3ServiceFirmwareEffect.Completed(1, V3ServiceFirmwareUpdateResult.Success)), effects)
        assertEquals(1, attempts.size)
    }

    @Test fun `destroy cancels active attempts and retained viewmodel only starts a new explicit confirmation`() {
        create()
        confirm(1)
        confirm(2)
        val previousAttempts = attempts.toList()
        vm.onAction(V3ServiceFirmwareAction.ViewDestroyed)
        assertEquals(V3ServiceFirmwareUiState(), vm.uiState.value)
        dispatcher.scheduler.runCurrent()
        assertTrue(previousAttempts.all { it.ended })
        assertTrue(effects.isEmpty())
        confirm(3)
        assertEquals(2, attempts.size)
        create()
        create()
        dispatcher.scheduler.runCurrent()
        assertEquals(2, attempts.size)
        assertEquals(V3ServiceFirmwareUiState(), vm.uiState.value)
        confirm(4)
        assertEquals(3, attempts.size)
        assertEquals(mapOf(4L to 0), vm.uiState.value.progressByRequest)
    }

    @Test fun `destroy drains queued stalls completions and failures before the replacement UI collects`() {
        collector?.cancel()
        dispatcher.scheduler.runCurrent()
        create()
        confirm(1)
        advanceTime(30_000)
        complete(attempts[0])
        confirm(2)
        assertTrue(attempts[1].result.completeExceptionally(IllegalArgumentException("bad descriptor")))
        dispatcher.scheduler.runCurrent()
        assertTrue(effects.isEmpty())
        vm.onAction(V3ServiceFirmwareAction.ViewDestroyed)
        create()
        collectEffects()
        assertTrue(effects.isEmpty())
        assertEquals(V3ServiceFirmwareUiState(), vm.uiState.value)
        confirm(3)
        complete(attempts.last())
        assertEquals(listOf(V3ServiceFirmwareEffect.Completed(3, V3ServiceFirmwareUpdateResult.Success)), effects)
    }

    @Test fun `noncooperative callbacks and results from the destroyed activity do not change a recreated session`() {
        nonCooperative = true
        create()
        confirm(1)
        val oldAttempt = attempts.single()
        progress(oldAttempt, 30)
        vm.onAction(V3ServiceFirmwareAction.ViewDestroyed)
        create()
        nonCooperative = false
        confirm(2)
        progress(attempts[1], 45)
        val replacementState = vm.uiState.value
        oldAttempt.onProgress(99)
        oldAttempt.onPhaseChanged("late", 321L)
        complete(oldAttempt)
        assertTrue(oldAttempt.ended)
        assertEquals(replacementState, vm.uiState.value)
        assertTrue(effects.isEmpty())
        complete(attempts[1])
        assertEquals(listOf(V3ServiceFirmwareEffect.Completed(2, V3ServiceFirmwareUpdateResult.Success)), effects)
    }

    @Test fun `cleared viewmodel ignores lifecycle stale confirmations and late noncooperative callbacks`() {
        nonCooperative = true
        create()
        confirm()
        val attempt = attempts.single()
        store.clear()
        assertEquals(V3ServiceFirmwareUiState(), vm.uiState.value)
        vm.onAction(V3ServiceFirmwareAction.ViewCreated)
        confirm(2)
        attempt.onProgress(100)
        attempt.onPhaseChanged("late", 321L)
        complete(attempt)
        vm.onAction(V3ServiceFirmwareAction.ResultShown(1))
        assertEquals(1, attempts.size)
        assertEquals(V3ServiceFirmwareUiState(), vm.uiState.value)
        assertTrue(effects.isEmpty())
    }

    @Test fun `unchanged progress warns at thirty seconds once while the transfer continues`() {
        create()
        confirm()
        attempts.single().onPhaseChanged("read_package", 101L)
        advanceTime(29_999)
        assertTrue(effects.isEmpty())
        advanceTime(1)
        assertEquals(listOf(V3ServiceFirmwareEffect.Stalled(1, "read_package", 0, 101L)), effects)
        assertFalse(attempts.single().ended)
        advanceTime(90_000)
        assertEquals(1, effects.size)
        progress(attempts.single(), 50)
        complete(attempts.single())
        assertEquals(V3ServiceFirmwareEffect.Completed(1, V3ServiceFirmwareUpdateResult.Success), effects.last())
    }

    @Test fun `stall checks compare consecutive thirty second snapshots and use the latest phase`() {
        create()
        confirm()
        advanceTime(15_000)
        progress(attempts.single(), 10)
        advanceTime(15_000)
        assertTrue(effects.isEmpty())
        advanceTime(15_000)
        progress(attempts.single(), 20)
        attempts.single().onPhaseChanged("checking_new_firmware", 202L)
        advanceTime(15_000)
        assertTrue(effects.isEmpty())
        advanceTime(30_000)
        assertEquals(listOf(V3ServiceFirmwareEffect.Stalled(1, "checking_new_firmware", 20, 202L)), effects)
        assertFalse(attempts.single().ended)
    }

    @Test fun `one hundred percent suppresses the warning even while waiting for the final result`() {
        create()
        confirm()
        progress(attempts.single(), 100)
        advanceTime(120_000)
        assertTrue(effects.isEmpty())
        assertFalse(attempts.single().ended)
        complete(attempts.single())
        assertEquals(listOf(V3ServiceFirmwareEffect.Completed(1, V3ServiceFirmwareUpdateResult.Success)), effects)
    }

    @Test fun `completion failure and activity destruction cancel their stall timers`() {
        create()
        confirm(1)
        complete(attempts[0])
        confirm(2)
        assertTrue(attempts[1].result.completeExceptionally(IllegalStateException("read failed")))
        dispatcher.scheduler.runCurrent()
        assertEquals(2, effects.size)
        advanceTime(60_000)
        assertEquals(2, effects.size)
        confirm(3)
        vm.onAction(V3ServiceFirmwareAction.ViewDestroyed)
        dispatcher.scheduler.runCurrent()
        effects.clear()
        create()
        advanceTime(60_000)
        assertTrue(effects.isEmpty())
        assertTrue(attempts[2].ended)
        assertEquals(V3ServiceFirmwareUiState(), vm.uiState.value)
    }

    @Test fun `queued progress before destruction cannot restore an old request after recreation`() {
        create()
        confirm(1)
        attempts[0].onProgress(97)
        vm.onAction(V3ServiceFirmwareAction.ViewDestroyed)
        create()
        confirm(2)
        assertEquals(mapOf(2L to 0), vm.uiState.value.progressByRequest)
        assertTrue(effects.isEmpty())
        assertEquals(2, attempts.size)
    }

    @Test fun `queued progress cannot recreate an acknowledged result or disturb another request`() {
        create()
        confirm(1)
        complete(attempts[0])
        confirm(2)
        attempts[1].onProgress(35)
        attempts[0].onProgress(100)
        vm.onAction(V3ServiceFirmwareAction.ResultShown(1))
        dispatcher.scheduler.runCurrent()
        assertEquals(mapOf(2L to 35), vm.uiState.value.progressByRequest)
        progress(attempts[0], 99)
        assertEquals(mapOf(2L to 35), vm.uiState.value.progressByRequest)
        assertEquals(2, attempts.size)
        assertEquals(listOf(V3ServiceFirmwareEffect.Completed(1, V3ServiceFirmwareUpdateResult.Success)), effects)
    }

    @Test fun `debug probe rejection is synchronous and prevents any installation or UI change`() {
        debug()
        assertEquals(0, debugGuardCalls)
        create()
        val failure = IllegalStateException("Full-update autorun is not permitted in the boot-entry probe build")
        debugGuardFailure = failure
        val thrown = assertThrows(IllegalStateException::class.java) {
            vm.onAction(V3ServiceFirmwareAction.DebugInstallRequested(file))
        }
        assertSame(failure, thrown)
        assertEquals(1, debugGuardCalls)
        assertTrue(debugAttempts.isEmpty())
        dispatcher.scheduler.runCurrent()
        assertTrue(debugAttempts.isEmpty())
        assertTrue(attempts.isEmpty())
        assertEquals(V3ServiceFirmwareUiState(), vm.uiState.value)
        assertTrue(effects.isEmpty())
    }

    @Test fun `debug guard runs before dispatch and selected file never creates progress warning or result UI`() {
        create()
        val selectedFile = V3ServiceFirmwareLocalFile(" DEBUG FAM.ZIP ", "/cache/ DEBUG FAM.ZIP ")
        vm.onAction(V3ServiceFirmwareAction.DebugInstallRequested(selectedFile))
        assertEquals(1, debugGuardCalls)
        assertTrue(debugAttempts.isEmpty())
        assertEquals(V3ServiceFirmwareUiState(), vm.uiState.value)
        dispatcher.scheduler.runCurrent()
        val attempt = debugAttempts.single()
        assertSame(selectedFile, attempt.file)
        advanceTime(120_000)
        assertFalse(attempt.ended)
        assertEquals(V3ServiceFirmwareUiState(), vm.uiState.value)
        assertTrue(effects.isEmpty())
        complete(attempt)
        repeat(3) { create() }
        dispatcher.scheduler.runCurrent()
        assertTrue(attempt.ended)
        assertEquals(1, debugAttempts.size)
        assertTrue(attempts.isEmpty())
        assertEquals(V3ServiceFirmwareUiState(), vm.uiState.value)
        assertTrue(effects.isEmpty())
    }

    @Test fun `debug and confirmed installations may run concurrently without sharing progress or results`() {
        create()
        debug()
        val secondFile = V3ServiceFirmwareLocalFile("DEBUG-HHS.bin", "/cache/DEBUG-HHS.bin")
        debug(secondFile)
        confirm(8, 4)
        assertEquals(listOf(file, secondFile), debugAttempts.map { it.file })
        assertEquals(2, debugGuardCalls)
        assertEquals(4, attempts.single().boardAddress)
        progress(attempts.single(), 56)
        assertEquals(mapOf(8L to 56), vm.uiState.value.progressByRequest)
        complete(debugAttempts[1])
        assertFalse(debugAttempts[0].ended)
        assertFalse(attempts.single().ended)
        assertEquals(mapOf(8L to 56), vm.uiState.value.progressByRequest)
        assertTrue(effects.isEmpty())
        complete(attempts.single())
        assertEquals(listOf(V3ServiceFirmwareEffect.Completed(8, V3ServiceFirmwareUpdateResult.Success)), effects)
        vm.onAction(V3ServiceFirmwareAction.ResultShown(8))
        complete(debugAttempts[0])
        assertEquals(V3ServiceFirmwareUiState(), vm.uiState.value)
        assertEquals(1, effects.size)
        assertEquals(2, debugAttempts.size)
        assertEquals(1, attempts.size)
    }

    @Test fun `pausing UI collection for navigation or STOP does not cancel a debug installation`() {
        create()
        debug()
        collector?.cancel()
        dispatcher.scheduler.runCurrent()
        val attempt = debugAttempts.single()
        advanceTime(90_000)
        assertFalse(attempt.ended)
        assertEquals(V3ServiceFirmwareUiState(), vm.uiState.value)
        collectEffects()
        assertTrue(effects.isEmpty())
        complete(attempt)
        assertTrue(attempt.ended)
        assertTrue(effects.isEmpty())
        assertEquals(1, debugAttempts.size)
    }

    @Test fun `destroy cancels active debug attempts and recreation requires a new explicit debug action`() {
        create()
        debug()
        debug()
        val previousAttempts = debugAttempts.toList()
        vm.onAction(V3ServiceFirmwareAction.ViewDestroyed)
        dispatcher.scheduler.runCurrent()
        assertTrue(previousAttempts.all { it.ended })
        debug()
        assertEquals(2, debugGuardCalls)
        assertEquals(2, debugAttempts.size)
        create()
        create()
        dispatcher.scheduler.runCurrent()
        assertEquals(2, debugAttempts.size)
        assertEquals(V3ServiceFirmwareUiState(), vm.uiState.value)
        assertTrue(effects.isEmpty())
        debug()
        assertEquals(3, debugGuardCalls)
        assertEquals(3, debugAttempts.size)
        assertFalse(debugAttempts.last().ended)
    }

    @Test fun `destroy cancels queued debug work before the repository starts and recreation does not restart it`() {
        create()
        vm.onAction(V3ServiceFirmwareAction.DebugInstallRequested(file))
        assertEquals(1, debugGuardCalls)
        assertTrue(debugAttempts.isEmpty())
        vm.onAction(V3ServiceFirmwareAction.ViewDestroyed)
        create()
        dispatcher.scheduler.runCurrent()
        assertTrue(debugAttempts.isEmpty())
        assertEquals(V3ServiceFirmwareUiState(), vm.uiState.value)
        assertTrue(effects.isEmpty())
        debug()
        assertEquals(2, debugGuardCalls)
        assertEquals(1, debugAttempts.size)
    }

    @Test fun `cleared viewmodel cancels started debug work and ignores stale lifecycle and debug actions`() {
        create()
        debug()
        val attempt = debugAttempts.single()
        store.clear()
        dispatcher.scheduler.runCurrent()
        assertTrue(attempt.ended)
        create()
        debug()
        assertEquals(1, debugGuardCalls)
        assertEquals(1, debugAttempts.size)
        assertEquals(V3ServiceFirmwareUiState(), vm.uiState.value)
        assertTrue(effects.isEmpty())
    }

    @Test fun `cleared viewmodel cancels queued debug work before the repository starts`() {
        create()
        vm.onAction(V3ServiceFirmwareAction.DebugInstallRequested(file))
        assertEquals(1, debugGuardCalls)
        assertTrue(debugAttempts.isEmpty())
        store.clear()
        create()
        debug()
        assertEquals(1, debugGuardCalls)
        assertTrue(debugAttempts.isEmpty())
        assertEquals(V3ServiceFirmwareUiState(), vm.uiState.value)
        assertTrue(effects.isEmpty())
    }

    @Test fun `late noncooperative debug completion cannot change the recreated confirmed session`() {
        nonCooperativeDebug = true
        create()
        debug()
        val previousAttempt = debugAttempts.single()
        vm.onAction(V3ServiceFirmwareAction.ViewDestroyed)
        create()
        confirm(9)
        progress(attempts.single(), 45)
        val replacementState = vm.uiState.value
        assertFalse(previousAttempt.ended)
        complete(previousAttempt)
        assertTrue(previousAttempt.ended)
        assertEquals(replacementState, vm.uiState.value)
        assertTrue(effects.isEmpty())
        assertEquals(1, debugAttempts.size)
        complete(attempts.single())
        assertEquals(listOf(V3ServiceFirmwareEffect.Completed(9, V3ServiceFirmwareUpdateResult.Success)), effects)
    }
}
