package com.bailout.stickk.ubi4.versions.v3.presentation.sensors.plot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class V3PlotSmootherTest {
    @Test
    fun `six channels retain three tick interpolation and invalid sample handling`() {
        val smoother = V3PlotSmoother()
        val raw = listOf(255, -1, 256, 150, 0, 90)
        assertEquals(listOf(85, 0, 0, 50, 0, 30), smoother.next(raw))
        assertEquals(listOf(170, 0, 0, 100, 0, 60), smoother.next(raw))
        assertEquals(listOf(255, 0, 0, 150, 0, 90), smoother.next(raw))
        assertEquals(listOf(255, 0, 0, 150, 0, 90), smoother.next(raw))
    }

    @Test
    fun `new target ramps from the currently displayed point`() {
        val smoother = V3PlotSmoother()
        assertEquals(30, smoother.next(listOf(90))[0])
        assertEquals(20, smoother.next(listOf(0))[0])
        assertEquals(10, smoother.next(listOf(0))[0])
        assertEquals(List(6) { 0 }, smoother.next(listOf(0)))
    }
}
