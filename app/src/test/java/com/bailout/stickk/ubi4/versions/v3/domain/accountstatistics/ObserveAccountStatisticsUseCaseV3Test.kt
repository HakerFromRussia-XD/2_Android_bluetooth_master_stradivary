package com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ObserveAccountStatisticsUseCaseV3Test {
    private val repository = mockk<V3AccountStatisticsRepository>()
    private suspend fun usage(value: V3AccountStatistics): List<V3GestureUsage> {
        every { repository.observeStatistics() } returns flowOf(value)
        return ObserveAccountStatisticsUseCaseV3(repository)().first()
    }

    @Test fun `factory telemetry preserves all fifteen ids including hidden twelve and ignores extra slots`() = runTest {
        val rows = usage(V3AccountStatistics(baseGestureCounts = List(18) { 5L }))
        assertEquals((1..15).toList(), rows.map { it.gestureId })
        assertTrue(rows.all { it.customGestureIndex == null && it.count == 5L })
        verify(exactly = 0) { repository.requestStatistics() }
    }

    @Test fun `custom telemetry is not truncated to fourteen names and uses indexes rather than protocol ids`() = runTest {
        val rows = usage(V3AccountStatistics(customGestureCounts = List(17) { 1L }, customGestureNames = listOf("", "Saved")))
        assertEquals((64..80).toList(), rows.map { it.gestureId })
        assertEquals((0..16).toList(), rows.map { it.customGestureIndex })
        assertEquals("", rows[0].customName)
        assertEquals("Saved", rows[1].customName)
        assertNull(rows[14].customName)
    }

    @Test fun `positive counts sort descending as longs with ids breaking ties`() = runTest {
        val rows = usage(V3AccountStatistics(
            baseGestureCounts = listOf(999L, 5L, -1L, 0L, Long.MAX_VALUE),
            customGestureCounts = listOf(5L, Long.MAX_VALUE, 0L, -2L),
        ))
        assertEquals(listOf(4, 65, 1, 64), rows.map { it.gestureId })
        assertEquals(listOf(Long.MAX_VALUE, Long.MAX_VALUE, 5L, 5L), rows.map { it.count })
        assertTrue(usage(V3AccountStatistics()).isEmpty())
    }
}
