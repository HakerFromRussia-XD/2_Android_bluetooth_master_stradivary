package com.bailout.stickk.ubi4.achievements

import com.bailout.stickk.ubi4.data.state.TelemetryGestureCounters
import com.bailout.stickk.ubi4.data.state.totalPerformedGestures
import com.bailout.stickk.ubi4.data.state.totalPrecisionGestures
import com.bailout.stickk.ubi4.data.state.totalPowerGestures
import com.bailout.stickk.ubi4.data.local.repository.isCustomGestureId
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GestureEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AchievementsStateTest {

    @Test
    fun `total gestures excludes no gesture counter and includes custom gestures`() {
        val counters = TelemetryGestureCounters(
            baseGestureMovementCount = listOf(999L, 20L, 30L),
            customGestureMovementCount = listOf(40L, 10L)
        )

        assertEquals(100L, counters.totalPerformedGestures())
    }

    @Test
    fun `power gestures include only fist and fist 2 counters`() {
        val counters = TelemetryGestureCounters(
            baseGestureMovementCount = listOf(
                1_000L,
                20L,
                2_000L,
                3_000L,
                50L
            ),
            customGestureMovementCount = listOf(4_000L)
        )

        assertEquals(70L, counters.totalPowerGestures())
    }

    @Test
    fun `precision gestures include only pinch key and tweezers counters`() {
        val counters = TelemetryGestureCounters(
            baseGestureMovementCount = listOf(
                1_000L,
                2_000L,
                3_000L,
                20L,
                4_000L,
                30L,
                5_000L,
                50L
            ),
            customGestureMovementCount = listOf(6_000L)
        )

        assertEquals(100L, counters.totalPrecisionGestures())
    }

    @Test
    fun `get a grip accepts only eight visible custom gesture ids`() {
        assertTrue(GestureEnum.GESTURE_CUSTOM_0.number.isCustomGestureId())
        assertTrue(GestureEnum.GESTURE_CUSTOM_7.number.isCustomGestureId())
        assertFalse(GestureEnum.GESTURE_NATURAL_POSITION.number.isCustomGestureId())
        assertFalse(GestureEnum.GESTURE_CUSTOM_8.number.isCustomGestureId())
    }
}
