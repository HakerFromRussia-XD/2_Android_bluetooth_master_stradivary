package com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class V3FingerPositionMappingTest {

    @Test
    fun `thumb axis endpoints match calibrated degrees`() {
        assertEquals(49, V3FingerPositionMapping.thumbFirstAxisAngle(0))
        assertEquals(-35, V3FingerPositionMapping.thumbFirstAxisAngle(100))
        assertEquals(22, V3FingerPositionMapping.thumbSecondAxisAngle(0))
        assertEquals(-68, V3FingerPositionMapping.thumbSecondAxisAngle(100))
        assertEquals(20, V3FingerPositionMapping.thumbSecondPhalanxAngle(0))
        assertEquals(-25, V3FingerPositionMapping.thumbSecondPhalanxAngle(100))
    }

    @Test
    fun `neutral renderer angles map back to themselves`() {
        val firstNeutral = V3FingerPositionMapping.thumbFirstAxisPercent(0)
        val secondNeutral = V3FingerPositionMapping.thumbSecondAxisPercent(
            V3FingerPositionMapping.THUMB_SECOND_AXIS_INITIAL_DEGREES
        )

        assertEquals(0, V3FingerPositionMapping.thumbFirstAxisAngle(firstNeutral))
        assertEquals(
            V3FingerPositionMapping.THUMB_SECOND_AXIS_INITIAL_DEGREES,
            V3FingerPositionMapping.thumbSecondAxisAngle(secondNeutral)
        )
    }

    @Test
    fun `percent round trip stays within one point`() {
        for (percent in 0..100) {
            val firstRoundTrip = V3FingerPositionMapping.thumbFirstAxisPercent(
                V3FingerPositionMapping.thumbFirstAxisAngle(percent)
            )
            val secondRoundTrip = V3FingerPositionMapping.thumbSecondAxisPercent(
                V3FingerPositionMapping.thumbSecondAxisAngle(percent)
            )

            assertTrue(abs(percent - firstRoundTrip) <= 1)
            assertTrue(abs(percent - secondRoundTrip) <= 1)
        }
    }
}
