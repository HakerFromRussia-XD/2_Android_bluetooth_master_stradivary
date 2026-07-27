package com.bailout.stickk.ubi4.achievements

import com.bailout.stickk.ubi4.data.state.TelemetryGestureCounters
import com.bailout.stickk.ubi4.data.state.totalPerformedGestures
import kotlin.test.Test
import kotlin.test.assertEquals

class AchievementsStateTest {

    @Test
    fun `total gestures excludes no gesture counter and includes custom gestures`() {
        val counters = TelemetryGestureCounters(
            baseGestureMovementCount = listOf(999L, 20L, 30L),
            customGestureMovementCount = listOf(40L, 10L)
        )

        assertEquals(100L, counters.totalPerformedGestures())
    }
}
