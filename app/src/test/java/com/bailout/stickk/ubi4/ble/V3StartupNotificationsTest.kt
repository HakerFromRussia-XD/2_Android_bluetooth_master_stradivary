package com.bailout.stickk.ubi4.ble

import com.bailout.stickk.ubi4.ble.V3StartupNotifications.Channel.MAIN
import com.bailout.stickk.ubi4.ble.V3StartupNotifications.Channel.SERIAL
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class V3StartupNotificationsTest {
    @Test fun `subscribes both channels in order`() = runTest {
        val calls = mutableListOf<V3StartupNotifications.Channel>()
        val failures = mutableListOf<V3StartupNotifications.Channel>()
        val setup = V3StartupNotifications({ true }, { calls += it; true }, { failures += it })

        assertTrue(setup.awaitSubscriptions())
        assertEquals(listOf(SERIAL, MAIN), calls)
        assertTrue(failures.isEmpty())
    }

    @Test fun `retries subscription failures without advancing past a failed serial subscription`() = runTest {
        val calls = mutableListOf<V3StartupNotifications.Channel>()
        val failures = mutableListOf<V3StartupNotifications.Channel>()
        val results = ArrayDeque(listOf(false, true, false, true, true))
        val setup = V3StartupNotifications({ true }, { calls += it; results.removeFirst() }, { failures += it })

        assertTrue(setup.awaitSubscriptions())
        assertEquals(listOf(SERIAL, SERIAL, MAIN, SERIAL, MAIN), calls)
        assertEquals(listOf(SERIAL, MAIN), failures)
    }

    @Test fun `connection loss while waiting prevents next subscription and success`() = runTest {
        var current = true
        val serialResult = CompletableDeferred<Boolean>()
        val calls = mutableListOf<V3StartupNotifications.Channel>()
        val failures = mutableListOf<V3StartupNotifications.Channel>()
        val setup = V3StartupNotifications({ current }, { calls += it; serialResult.await() }, { failures += it })
        val result = async { setup.awaitSubscriptions() }
        runCurrent()
        current = false
        serialResult.complete(true)

        assertFalse(result.await())
        assertEquals(listOf(SERIAL), calls)
        assertTrue(failures.isEmpty())
    }

    @Test fun `old connection and firmware session do not start subscriptions`() = runTest {
        val calls = mutableListOf<V3StartupNotifications.Channel>()
        val setup = V3StartupNotifications({ false }, { calls += it; true }, { fail("Unexpected failure callback") })

        assertFalse(setup.awaitSubscriptions())
        assertTrue(calls.isEmpty())
    }

    @Test fun `connection loss during main subscription does not report success`() = runTest {
        var current = true
        val calls = mutableListOf<V3StartupNotifications.Channel>()
        val setup = V3StartupNotifications({ current }, { channel ->
            calls += channel
            if (channel == MAIN) current = false
            true
        }, { fail("Unexpected failure callback") })

        assertFalse(setup.awaitSubscriptions())
        assertEquals(listOf(SERIAL, MAIN), calls)
    }
}
