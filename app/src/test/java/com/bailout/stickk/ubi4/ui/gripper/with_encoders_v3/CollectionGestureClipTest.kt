package com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class CollectionGestureClipTest {

    @Test
    fun fistKeepsDelayedFingersStillUntilTheirDelayExpires() {
        val clip = requireNotNull(CollectionGestureClip.forGesture(1))

        // Fist's four fingers have 15 units = 150 ms start delay; thumb does not.
        assertArrayEquals(intArrayOf(100, 100, 100, 100, 50, 0), clip.sample(149).pose.values)
        assertEquals(50, clip.sample(150).pose.values[4])
        assertEquals(100, clip.sample(150).pose.values[0])
        assertEquals(50, clip.sample(300).pose.values[0])
    }

    @Test
    fun fixedClipRestoresItsExactInitialPose() {
        val clip = requireNotNull(CollectionGestureClip.forGesture(13))
        val start = clip.initialPose().values

        assertArrayEquals(start, clip.sample(900).pose.values)
        assertArrayEquals(start, clip.sample(10_000).pose.values)
        assertEquals(true, clip.sample(900).isComplete)
    }

    @Test
    fun fistReturnDelayAppliesOnlyToThumb() {
        val clip = requireNotNull(CollectionGestureClip.forGesture(1))

        // Return phase begins at 600 ms; non-delayed fingers begin immediately.
        assertEquals(50, clip.sample(750).pose.values[0])
        // Thumb return is delayed 150 ms and stays at the closed target.
        assertEquals(0, clip.sample(750).pose.values[4])
        assertEquals(50, clip.sample(900).pose.values[4])
    }

    @Test
    fun approvedIPhoneStartStatesAreStoredInAndroidMechanicalOrder() {
        assertArrayEquals(
            intArrayOf(100, 100, 100, 0, 100, 0),
            requireNotNull(CollectionGestureClip.forGesture(2)).initialPose().values
        )
        assertArrayEquals(
            intArrayOf(100, 100, 100, 50, 70, 100),
            requireNotNull(CollectionGestureClip.forGesture(7)).initialPose().values
        )
        assertArrayEquals(
            intArrayOf(100, 100, 0, 0, 70, 100),
            requireNotNull(CollectionGestureClip.forGesture(13)).initialPose().values
        )
    }

    @Test
    fun finalIPhoneCardTransformsAreAssignedToTheCorrectGestureGroups() {
        val natural = requireNotNull(CollectionGestureClip.forGesture(15)).cardTransform()
        val fist = requireNotNull(CollectionGestureClip.forGesture(1)).cardTransform()
        val key = requireNotNull(CollectionGestureClip.forGesture(5)).cardTransform()
        val cup = requireNotNull(CollectionGestureClip.forGesture(8)).cardTransform()
        val board = requireNotNull(CollectionGestureClip.forGesture(9)).cardTransform()
        val thumbsUp = requireNotNull(CollectionGestureClip.forGesture(11)).cardTransform()

        assertArrayEquals(natural, fist, 0.000001f)
        assertArrayEquals(key, cup, 0.000001f)
        assertArrayEquals(key, thumbsUp, 0.000001f)
        assertNotEquals(key.toList(), board.toList())

        // The persisted iOS calibration matrix is intentionally not
        // re-orthonormalized, so the recovered basis length differs by a few
        // ten-thousandths from the user scale value.
        assertEquals(0.4654335f, naturalBasisScale(natural), 0.0001f)
        assertEquals(-4.250008f, natural[12], 0.000001f)
        assertEquals(-4.410004f, natural[13], 0.000001f)
        assertEquals(0.4059688f, naturalBasisScale(board), 0.0001f)
        assertEquals(-15.0f, board[12], 0.000001f)
        assertEquals(-12.82666f, board[13], 0.000001f)
    }

    private fun naturalBasisScale(matrix: FloatArray): Float =
        kotlin.math.sqrt(matrix[0] * matrix[0] + matrix[1] * matrix[1] + matrix[2] * matrix[2])
}
