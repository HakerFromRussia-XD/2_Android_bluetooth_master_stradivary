package com.bailout.stickk.ubi4.versions.v3.domain.accountprofile

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveAccountSerialNumberUseCaseV3Test {
    private val local = FakeAccountProfileLocal()
    private val observe = ObserveAccountSerialNumberUseCaseV3(local)

    @Test fun `fresh serial is normalized and selected before it reaches the consumer`() = runTest {
        local.serialNumbers.value = " FEST-HM-13261 "
        val received = mutableListOf<String?>()
        val observer = backgroundScope.launch {
            observe().collect { serial ->
                assertEquals(serial, local.selectedSerialNumber)
                received.add(serial)
            }
        }
        runCurrent()
        assertEquals(listOf("FEST-HM-13261"), received)
        assertEquals(listOf("FEST-HM-13261"), local.selectedSerialNumbers)

        local.serialNumbers.value = "FEST-HM-13261"
        runCurrent()
        assertEquals(1, received.size)
        assertEquals(1, local.selectedSerialNumbers.size)

        local.serialNumbers.value = "  "
        runCurrent()
        local.serialNumbers.value = null
        runCurrent()
        assertEquals(listOf("FEST-HM-13261", null), received)
        assertEquals(listOf("FEST-HM-13261", null), local.selectedSerialNumbers)
        observer.cancel()
    }

    @Test fun `device identity change invalidates the previous header and characteristics`() = runTest {
        local.storedHeader = V3AccountProfileHeader("Previous", "User")
        local.values[V3AccountDetail.SIDE] = "Right"
        backgroundScope.launch { observe().collect {} }
        runCurrent()
        assertEquals("Previous", local.storedHeader?.firstName)
        assertEquals("Right", local.getDetail(V3AccountDetail.SIDE))

        local.serialNumbers.value = null
        runCurrent()
        assertNull(local.storedHeader)
        assertTrue(local.values.values.all { it.isEmpty() })

        local.serialNumbers.value = "FEST-HM-13261"
        runCurrent()
        assertEquals("FEST-HM-13261", local.selectedSerialNumber)
        assertEquals("", local.getDetail(V3AccountDetail.SIDE))
    }
}
