package com.bailout.stickk.ubi4.versions.v3.domain.vtosc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class V3VtoscConfigTest {
    @Test
    fun `defaults match UBI41_VTFB firmware constants`() {
        val config = V3VtoscConfig()
        assertEquals(2000, config.fsrThreshold)
        assertEquals(200, config.fsrHysteresis)
        assertEquals(50, config.motorsPwmPercent)
        assertEquals(100, config.motorsPulseDurationMs)
        assertEquals(100, config.motorsPostPulsePauseMs)
        assertEquals(9, config.encode().size)
    }

    @Test
    fun `encode decode round trip preserves packed little endian fields`() {
        val original = V3VtoscConfig(
            fsrThreshold = 2100,
            fsrHysteresis = 150,
            motorsPwmPercent = 75,
            motorsPulseDurationMs = 250,
            motorsPostPulsePauseMs = 300,
        )
        val decoded = V3VtoscConfig.decode(original.encode().map { it.toInt() and 0xFF })
        assertEquals(original, decoded)
    }

    @Test
    fun `short payload is rejected`() {
        assertNull(V3VtoscConfig.decode(listOf(1, 2, 3)))
    }
}
