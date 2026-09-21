package com.bailout.stickk.ubi4.ble

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.*
import android.util.Log
import io.mockk.*

@OptIn(ExperimentalCoroutinesApi::class)
class BleCommandWriterAsyncTest {
    @BeforeEach fun prepare() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
    }

    @AfterEach fun cleanup() { unmockkStatic(Log::class) }

    private fun execute(
        scope: CoroutineScope, dispatcher: CoroutineDispatcher,
        write: (ByteArray?, String, String) -> Unit, packet: ByteArray? = null,
        onSent: () -> Unit,
    ) {
        BleCommandWriter { bytes, command, type ->
            write(bytes, command, type)
            false
        }.writeAsync(scope, packet, "uuid", "WRITE", dispatcher, onSent)
    }

    @Test fun `dispatch is asynchronous and completion follows write with original arguments`() = runTest {
        val events = mutableListOf<String>()
        val packet = byteArrayOf(1, 2)
        execute(this, StandardTestDispatcher(testScheduler), { bytes, uuid, type ->
            assertSame(packet, bytes)
            assertEquals("uuid", uuid)
            assertEquals("WRITE", type)
            events += "write"
        }, packet) { events += "sent" }
        assertTrue(events.isEmpty())
        runCurrent()
        assertEquals(listOf("write", "sent"), events)
    }

    @Test fun `scope cancellation before dispatch prevents write and completion`() = runTest {
        val owner = Job()
        val scope = CoroutineScope(owner)
        var calls = 0
        execute(scope, StandardTestDispatcher(testScheduler), { _, _, _ -> calls++ }) { calls++ }
        owner.cancel()
        runCurrent()
        assertEquals(0, calls)
    }

    @Test fun `cancellation during synchronous write does not introduce a new completion check`() = runTest {
        val owner = Job()
        val events = mutableListOf<String>()
        execute(CoroutineScope(owner), StandardTestDispatcher(testScheduler), { bytes, _, _ ->
            assertNull(bytes)
            events += "write"
            owner.cancel()
        }) { events += "sent" }
        runCurrent()
        assertEquals(listOf("write", "sent"), events)
    }

    @Test fun `write failure reaches scope handler without completion or retry`() = runTest {
        val failure = IllegalStateException("write failed")
        val errors = mutableListOf<Throwable>()
        val owner = Job()
        val scope = CoroutineScope(owner + CoroutineExceptionHandler { _, error -> errors += error })
        var attempts = 0
        var completions = 0
        execute(scope, StandardTestDispatcher(testScheduler), { _, _, _ ->
            attempts++
            throw failure
        }) { completions++ }
        runCurrent()
        assertEquals(1, attempts)
        assertEquals(0, completions)
        assertSame(failure, errors.single())
        assertTrue(owner.isCancelled)
    }

    @Test fun `callback failure reaches scope handler without repeating write`() = runTest {
        val failure = IllegalStateException("callback failed")
        val errors = mutableListOf<Throwable>()
        val owner = Job()
        val scope = CoroutineScope(owner + CoroutineExceptionHandler { _, error -> errors += error })
        var writes = 0
        var completions = 0
        execute(scope, StandardTestDispatcher(testScheduler), { _, _, _ -> writes++ }) {
            completions++
            throw failure
        }
        runCurrent()
        assertEquals(1, writes)
        assertEquals(1, completions)
        assertSame(failure, errors.single())
        assertTrue(owner.isCancelled)
    }
}
