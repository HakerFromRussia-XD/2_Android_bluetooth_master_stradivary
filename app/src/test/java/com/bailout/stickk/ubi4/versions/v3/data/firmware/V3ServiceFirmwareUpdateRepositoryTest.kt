package com.bailout.stickk.ubi4.versions.v3.data.firmware

import com.bailout.stickk.ubi4.firmware.FirmwareUpdateLogger
import com.bailout.stickk.ubi4.firmware.FirmwareUpdatePackage
import com.bailout.stickk.ubi4.firmware.FirmwareUpdateResult
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.CheckNewFwStatus
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.StartSystemUpdateStatus
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.InstallServiceFirmwareForDebugUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.InstallServiceFirmwareUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3ServiceFirmwareLocalFile
import com.bailout.stickk.ubi4.versions.v3.domain.firmware.V3ServiceFirmwareUpdateResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class V3ServiceFirmwareUpdateRepositoryTest {
    private class Fixture(
        private val scope: TestScope,
        probeOnly: Boolean = false,
    ) {
        val file = V3ServiceFirmwareLocalFile("Selected FAM.ZIP", "/cache/manual/selected.zip")
        val firmware = FirmwareUpdatePackage(
            "archive actual.zip", byteArrayOf(0, -1, 16), byteArrayOf(1, 2, 3), 19, 0x1234, "v1.2",
        )
        val calls = mutableListOf<String>()
        val progress = mutableListOf<Int>()
        val messages = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<Throwable?>()
        val debugMessages = mutableListOf<Pair<String, String>>()
        val debugErrors = mutableListOf<Triple<String, String, Throwable?>>()
        val phases = mutableListOf<Pair<String, Long>>()
        var clockReads = 0
        var preparation: suspend () -> Boolean = { true }
        var readFailure: Throwable? = null
        var engine: suspend (Int, FirmwareUpdatePackage, (Int, Int) -> Unit) -> FirmwareUpdateResult =
            { _, _, _ -> FirmwareUpdateResult.Success }
        var bootloader: suspend (Int) -> Unit = {}
        private val repository = V3ServiceFirmwareUpdateRepositoryImpl(
            readPackage = { path ->
                calls += "read:$path"
                readFailure?.let { throw it }
                firmware
            },
            prepareTransfer = { calls += "prepare"; preparation() },
            setSessionActive = { calls += "active:$it" },
            runUpdate = { address, archive, onProgress ->
                calls += "run:$address"
                assertSame(firmware, archive)
                engine(address, archive, onProgress)
            },
            ensureBootloader = { address -> calls += "boot:$address"; bootloader(address) },
            elapsedRealtime = { clockReads++; scope.testScheduler.currentTime },
            probeOnly = probeOnly,
            logger = object : FirmwareUpdateLogger {
                override fun info(tag: String, message: String) { messages += message }
                override fun warn(tag: String, message: String) { warnings += message }
                override fun error(tag: String, message: String, throwable: Throwable?) { errors += throwable }
            },
            debugLogger = object : FirmwareUpdateLogger {
                override fun info(tag: String, message: String) { debugMessages += tag to message }
                override fun error(tag: String, message: String, throwable: Throwable?) {
                    debugErrors += Triple(tag, message, throwable)
                }
            },
        )
        private val install = InstallServiceFirmwareUseCaseV3(repository)
        private val installForDebug = InstallServiceFirmwareForDebugUseCaseV3(repository)
        fun requireDebugAllowed() = installForDebug.requireAllowed()
        suspend fun installDebug() {
            requireDebugAllowed()
            installForDebug(file)
        }
        suspend fun install(address: Int = 9) = install(
            address, file, { progress += it }, { phase, startedAt -> phases += phase to startedAt },
        )
        fun expectedCalls(address: Int = 9) =
            listOf("active:true", "prepare", "read:${file.path}", "run:$address", "active:false")
        fun expectedDebugCalls() = listOf("active:true", "read:${file.path}", "run:0", "active:false")
    }

    @Test fun `debug reads the selected archive and runs address zero without preparation boot checks or clock reads`() = runTest {
        val fixture = Fixture(this).apply {
            preparation = { error("debug must not prepare notifications") }
            bootloader = { error("debug must not run a boot probe") }
        }

        fixture.installDebug()

        assertEquals(fixture.expectedDebugCalls(), fixture.calls)
        assertEquals(0, fixture.clockReads)
        assertEquals(listOf(
            "DFU_V2_TRACE" to "debug_autorun start file=Selected FAM.ZIP",
            "DFU_V2_TRACE" to "debug_autorun result=${FirmwareUpdateResult.Success}",
        ), fixture.debugMessages)
        assertTrue(fixture.debugErrors.isEmpty())
        assertTrue(fixture.messages.isEmpty())
        assertTrue(fixture.warnings.isEmpty())
        assertTrue(fixture.errors.isEmpty())
        assertTrue(fixture.progress.isEmpty())
        assertTrue(fixture.phases.isEmpty())
    }

    @Test fun `debug probe rejection is synchronous before session archive engine or logging`() = runTest {
        val fixture = Fixture(this, probeOnly = true)

        val failure = assertThrows(IllegalStateException::class.java) { fixture.requireDebugAllowed() }

        assertEquals("Full-update autorun is not permitted in the boot-entry probe build", failure.message)
        assertTrue(fixture.calls.isEmpty())
        assertEquals(0, fixture.clockReads)
        assertTrue(fixture.debugMessages.isEmpty())
        assertTrue(fixture.debugErrors.isEmpty())
        assertTrue(fixture.messages.isEmpty())
        assertTrue(fixture.errors.isEmpty())
        assertTrue(runCatching { fixture.installDebug() }.exceptionOrNull() is IllegalStateException)
        assertTrue(fixture.calls.isEmpty())
        assertTrue(fixture.debugMessages.isEmpty())
    }

    @Test fun `debug logs every original protocol result without confirmed install handling`() = runTest {
        val results = mutableListOf<FirmwareUpdateResult>(
            FirmwareUpdateResult.Success, FirmwareUpdateResult.PreloadFailed, FirmwareUpdateResult.CrcMismatch,
        )
        StartSystemUpdateStatus.entries.forEach { results += FirmwareUpdateResult.StartSystemUpdateRejected(it) }
        CheckNewFwStatus.entries.forEach { results += FirmwareUpdateResult.CheckNewFirmwareRejected(it) }
        for (result in results) {
            val fixture = Fixture(this).apply { engine = { _, _, _ -> result } }

            fixture.installDebug()

            assertEquals(fixture.expectedDebugCalls(), fixture.calls)
            assertEquals(listOf(
                "DFU_V2_TRACE" to "debug_autorun start file=Selected FAM.ZIP",
                "DFU_V2_TRACE" to "debug_autorun result=$result",
            ), fixture.debugMessages, result.toString())
            assertTrue(fixture.debugErrors.isEmpty())
            assertTrue(fixture.messages.isEmpty())
            assertTrue(fixture.phases.isEmpty())
            assertEquals(0, fixture.clockReads)
        }
    }

    @Test fun `debug logs each progress callback including repeats invalid totals and original Int overflow`() = runTest {
        val samples = listOf(
            Triple(1, 100, 1), Triple(1, 100, 1), Triple(4, 100, 4), Triple(5, 100, 5),
            Triple(9, 100, 9), Triple(10, 100, 10), Triple(-1, 100, 0), Triple(150, 100, 100),
            Triple(Int.MAX_VALUE, Int.MAX_VALUE, 0), Triple(30, 0, 0), Triple(30, -1, 0),
        )
        val fixture = Fixture(this).apply {
            engine = { _, _, report ->
                samples.forEach { (offset, total, _) -> report(offset, total) }
                FirmwareUpdateResult.Success
            }
        }

        fixture.installDebug()

        assertEquals(listOf("DFU_V2_TRACE" to "debug_autorun start file=Selected FAM.ZIP") +
            samples.map { (offset, total, percent) ->
                "DFU_V2_TRACE" to "debug_autorun progress=$percent offset=$offset total=$total"
            } + listOf("DFU_V2_TRACE" to "debug_autorun result=${FirmwareUpdateResult.Success}"),
            fixture.debugMessages)
        assertEquals(fixture.expectedDebugCalls(), fixture.calls)
        assertTrue(fixture.progress.isEmpty())
        assertTrue(fixture.debugErrors.isEmpty())
    }

    @Test fun `debug reader and engine swallow Exception Error and cancellation and release the session once`() = runTest {
        for (source in listOf("read", "engine")) {
            for (failure in listOf(IOException("archive unavailable"), AssertionError("diagnostic error"),
                CancellationException("diagnostic cancellation"))) {
                val fixture = Fixture(this).apply {
                    if (source == "read") readFailure = failure
                    else engine = { _, _, _ -> throw failure }
                }

                fixture.installDebug()

                val expectedCalls = if (source == "read")
                    listOf("active:true", "read:${fixture.file.path}", "active:false")
                else fixture.expectedDebugCalls()
                assertEquals(expectedCalls, fixture.calls)
                assertEquals(listOf("DFU_V2_TRACE" to "debug_autorun start file=Selected FAM.ZIP"),
                    fixture.debugMessages)
                assertEquals(1, fixture.debugErrors.size)
                assertEquals("DFU_V2_TRACE", fixture.debugErrors.single().first)
                assertEquals("debug_autorun failed", fixture.debugErrors.single().second)
                assertSame(failure, fixture.debugErrors.single().third)
                assertEquals(1, fixture.calls.count { it == "active:false" })
                assertEquals(0, fixture.clockReads)
                assertTrue(fixture.errors.isEmpty())
                assertTrue(fixture.warnings.isEmpty())
            }
        }
    }

    @Test fun `cancelling a suspended debug transfer logs cancellation and finalizes the session once`() = runTest {
        var engineEnded = false
        val fixture = Fixture(this).apply {
            engine = { _, _, _ -> try { awaitCancellation() } finally { engineEnded = true } }
        }
        val job = async { fixture.installDebug() }
        runCurrent()
        assertFalse(engineEnded)

        job.cancel(CancellationException("Activity destroyed"))
        runCurrent()

        assertTrue(engineEnded)
        assertTrue(job.isCancelled)
        assertEquals(fixture.expectedDebugCalls(), fixture.calls)
        assertEquals(1, fixture.calls.count { it == "active:false" })
        assertEquals(1, fixture.debugErrors.size)
        assertEquals("DFU_V2_TRACE", fixture.debugErrors.single().first)
        assertEquals("debug_autorun failed", fixture.debugErrors.single().second)
        assertTrue(fixture.debugErrors.single().third is CancellationException)
        assertEquals("Activity destroyed", fixture.debugErrors.single().third?.message)
        assertEquals(listOf("DFU_V2_TRACE" to "debug_autorun start file=Selected FAM.ZIP"), fixture.debugMessages)
        assertTrue(fixture.errors.isEmpty())
        assertTrue(fixture.warnings.isEmpty())
    }

    @Test fun `confirmed install prepares reads and runs the existing engine before session release`() = runTest {
        val fixture = Fixture(this)
        advanceTimeBy(42)

        assertEquals(V3ServiceFirmwareUpdateResult.Success, fixture.install(0x25))

        assertEquals(fixture.expectedCalls(0x25), fixture.calls)
        assertEquals(listOf("prepare_notifications", "read_package", "coordinator", "handle_result", "success")
            .map { it to 42L }, fixture.phases)
        assertTrue(fixture.messages.any {
            it.contains("file=Selected FAM.ZIP") && it.contains("interface_v3=true")
        })
        assertTrue(fixture.messages.any {
            it.contains("bytes=3 declared_size=19 crc=1234 descriptor=00ff10")
        })
        assertTrue(fixture.progress.isEmpty())
    }

    @Test fun `failed notification preparation stops before archive reading and releases the session`() = runTest {
        val fixture = Fixture(this).apply { preparation = { false } }

        val failure = runCatching { fixture.install() }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertEquals("Не удалось включить уведомления канала прошивки", failure?.message)
        assertEquals(listOf("active:true", "prepare", "active:false"), fixture.calls)
        assertEquals(listOf("prepare_notifications" to 0L), fixture.phases)
        assertEquals(listOf(failure, failure), fixture.errors)
    }

    @Test fun `archive error is forwarded without starting or retrying the engine`() = runTest {
        val failure = IOException("selected archive is missing")
        val fixture = Fixture(this).apply { readFailure = failure }

        assertSame(failure, runCatching { fixture.install() }.exceptionOrNull())
        assertEquals(listOf("active:true", "prepare", "read:${fixture.file.path}", "active:false"), fixture.calls)
        assertEquals(listOf("prepare_notifications", "read_package").map { it to 0L }, fixture.phases)
        assertEquals(listOf(failure, failure), fixture.errors)
    }

    @Test fun `probe reads the package then verifies only address zero without firmware transfer`() = runTest {
        val fixture = Fixture(this, probeOnly = true)

        assertEquals(V3ServiceFirmwareUpdateResult.BootEntryVerified, fixture.install(0))
        assertEquals(listOf("active:true", "prepare", "read:${fixture.file.path}", "boot:0", "active:false"), fixture.calls)
        assertEquals(listOf("prepare_notifications", "read_package", "coordinator", "boot_entry_probe")
            .map { it to 0L }, fixture.phases)
        assertTrue(fixture.messages.any { it.contains("entry_probe VERIFIED") })
        assertFalse(fixture.messages.any { it.contains("attempt RESULT") || it.contains("attempt SUCCESS") })
    }

    @Test fun `probe rejects other addresses after package reading and still releases the session`() = runTest {
        val fixture = Fixture(this, probeOnly = true)

        val failure = runCatching { fixture.install(9) }.exceptionOrNull()

        assertTrue(failure is IllegalStateException)
        assertEquals("Эта диагностическая сборка проверяет только вход FAM в boot", failure?.message)
        assertEquals(listOf("active:true", "prepare", "read:${fixture.file.path}", "active:false"), fixture.calls)
        assertEquals(listOf("prepare_notifications", "read_package", "coordinator")
            .map { it to 0L }, fixture.phases)
        assertFalse(fixture.messages.any { it.contains("entry_probe START") })
    }

    @Test fun `all engine results keep status labels and only zero means board incompatibility`() = runTest {
        val cases = mutableListOf<Pair<FirmwareUpdateResult, V3ServiceFirmwareUpdateResult>>(
            FirmwareUpdateResult.Success to V3ServiceFirmwareUpdateResult.Success,
            FirmwareUpdateResult.PreloadFailed to V3ServiceFirmwareUpdateResult.PreloadFailed,
            FirmwareUpdateResult.CrcMismatch to V3ServiceFirmwareUpdateResult.CrcMismatch,
        )
        StartSystemUpdateStatus.entries.forEach { status ->
            cases += FirmwareUpdateResult.StartSystemUpdateRejected(status) to
                V3ServiceFirmwareUpdateResult.StartSystemUpdateRejected(status.toString())
        }
        CheckNewFwStatus.entries.forEach { status ->
            cases += FirmwareUpdateResult.CheckNewFirmwareRejected(status) to
                V3ServiceFirmwareUpdateResult.CheckNewFirmwareRejected(status.toString(), status.code == 0)
        }
        for ((engineResult, expected) in cases) {
            val fixture = Fixture(this).apply { engine = { _, _, _ -> engineResult } }
            assertEquals(expected, fixture.install(), engineResult.toString())
            assertEquals(fixture.expectedCalls(), fixture.calls)
            assertEquals(engineResult == FirmwareUpdateResult.Success,
                fixture.messages.any { it.contains("attempt SUCCESS") })
            val phases = listOf("prepare_notifications", "read_package", "coordinator", "handle_result") +
                if (engineResult == FirmwareUpdateResult.Success) listOf("success") else emptyList()
            assertEquals(phases.map { it to 0L }, fixture.phases)
            assertTrue(fixture.errors.isEmpty())
        }
    }

    @Test fun `engine errors are forwarded once after progress and release the session`() = runTest {
        val failure = IOException("transport disconnected")
        val fixture = Fixture(this).apply {
            engine = { _, _, report -> report(50, 100); throw failure }
        }

        assertSame(failure, runCatching { fixture.install() }.exceptionOrNull())

        assertEquals(fixture.expectedCalls(), fixture.calls)
        assertEquals(listOf(50), fixture.progress)
        assertEquals(listOf("prepare_notifications", "read_package", "coordinator").map { it to 0L }, fixture.phases)
        assertEquals(listOf(failure, failure), fixture.errors)
        assertFalse(fixture.messages.any { it.contains("attempt RESULT") || it.contains("attempt SUCCESS") })
    }

    @Test fun `progress keeps original Int overflow clamping invalid totals and five percent logs`() = runTest {
        val release = CompletableDeferred<FirmwareUpdateResult>()
        lateinit var report: (Int, Int) -> Unit
        val fixture = Fixture(this).apply {
            engine = { _, _, onProgress -> report = onProgress; release.await() }
        }
        val result = async { fixture.install() }
        runCurrent()
        listOf(1 to 100, 4 to 100, 5 to 100, 9 to 100, 10 to 100,
            -1 to 100, 150 to 100, Int.MAX_VALUE to Int.MAX_VALUE, 30 to 0, 30 to -1).forEach { (offset, total) ->
            report(offset, total)
        }
        assertEquals(listOf(1, 4, 5, 9, 10, 0, 100, 0), fixture.progress)
        assertEquals(7, fixture.messages.count { it.contains("attempt PROGRESS") })
        release.complete(FirmwareUpdateResult.Success)
        runCurrent()
        assertEquals(V3ServiceFirmwareUpdateResult.Success, result.await())
    }

    @Test fun `cancellation from preparation read probe and engine propagates without failure logging`() = runTest {
        for (phase in listOf("prepare", "read", "probe", "engine")) {
            val cancellation = CancellationException("cancel $phase")
            val fixture = Fixture(this, probeOnly = phase == "probe").apply {
                when (phase) {
                    "prepare" -> preparation = { throw cancellation }
                    "read" -> readFailure = cancellation
                    "probe" -> bootloader = { throw cancellation }
                    "engine" -> engine = { _, _, _ -> throw cancellation }
                }
            }
            assertSame(cancellation, runCatching { fixture.install(0) }.exceptionOrNull())
            assertEquals("active:false", fixture.calls.last())
            assertTrue(fixture.errors.isEmpty())
            assertEquals(1, fixture.warnings.count { it.contains("attempt CANCELLED") })
        }
    }

    @Test fun `cancelling a suspended transfer ends the engine and finalizes the session once`() = runTest {
        var engineEnded = false
        val fixture = Fixture(this).apply {
            engine = { _, _, _ -> try { awaitCancellation() } finally { engineEnded = true } }
        }
        val result = async { fixture.install() }
        runCurrent()
        assertFalse(engineEnded)

        result.cancel(CancellationException("Activity destroyed"))
        runCurrent()

        assertTrue(runCatching { result.await() }.exceptionOrNull() is CancellationException)
        assertTrue(engineEnded)
        assertEquals(fixture.expectedCalls(), fixture.calls)
        assertTrue(fixture.errors.isEmpty())
        assertEquals(1, fixture.warnings.count { it.contains("attempt CANCELLED") })
        assertEquals(1, fixture.calls.count { it == "active:false" })
    }
}
