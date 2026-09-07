package com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3

import android.opengl.Matrix

/**
 * The collection-card animation contract shared by Android UBIv4 and V3.
 *
 * Values use the product convention: 0 is an open finger, 100 is a closed
 * finger.  The renderer stores fingers in mechanical order: little, ring,
 * middle, index, thumb-flex, thumb-rotation.
 */
data class CollectionFingerPose(val values: IntArray) {
    init {
        require(values.size == CHANNEL_COUNT) { "A gesture pose must have $CHANNEL_COUNT channels" }
    }

    fun copyValues() = values.copyOf()

    companion object {
        const val CHANNEL_COUNT = 6

        fun of(
            index: Int,
            middle: Int,
            ring: Int,
            little: Int,
            thumb: Int,
            rotation: Int
        ) = CollectionFingerPose(intArrayOf(little, ring, middle, index, thumb, rotation))
    }
}

/** A sample contains the exact per-channel pose to render at [elapsedMs]. */
data class CollectionGestureSample(
    val pose: CollectionFingerPose,
    val isComplete: Boolean
)

class CollectionGestureClip private constructor(
    val gestureId: Int,
    private val rest: CollectionFingerPose,
    private val active: CollectionFingerPose,
    private val forwardDelayMs: IntArray = IntArray(CollectionFingerPose.CHANNEL_COUNT),
    private val returnDelayMs: IntArray = IntArray(CollectionFingerPose.CHANNEL_COUNT)
) {
    init {
        require(forwardDelayMs.size == CollectionFingerPose.CHANNEL_COUNT)
        require(returnDelayMs.size == CollectionFingerPose.CHANNEL_COUNT)
    }

    fun initialPose(): CollectionFingerPose = rest.copy()

    /** Final iOS collection framing, applied directly in Android card mode. */
    fun cardTransform(): FloatArray = when (gestureId) {
        GESTURE_KEY, CUP_GRIP, THUMBS_UP -> cardTransform(
            KEY_GROUP_ROTATION, KEY_GROUP_SCALE, KEY_GROUP_POSITION_X, KEY_GROUP_POSITION_Y
        )
        HALF_GRAB -> cardTransform(
            BOARD_ROTATION, BOARD_SCALE, BOARD_POSITION_X, BOARD_POSITION_Y
        )
        else -> cardTransform(
            NATURAL_ROTATION, NATURAL_SCALE, NATURAL_POSITION_X, NATURAL_POSITION_Y
        )
    }

    fun objectAssetName(): String? = when (gestureId) {
        GESTURE_KEY -> "gesture_key.v3object"
        CUP_GRIP -> "gesture_cup.v3object"
        HALF_GRAB -> "gesture_board.v3object"
        else -> null
    }

    fun objectTransform(): FloatArray? = when (gestureId) {
        GESTURE_KEY -> objectTransform(21.17f, -26.50f, -32.22f, -9.56f, 171.56f, 199.94f, 0.8f)
        CUP_GRIP -> objectTransform(-1.42f, -30.17f, -30.28f, -92.67f, 143.44f, 7.71f, 1.154f)
        HALF_GRAB -> objectTransform(-32.41668f, -31.66668f, -7.33333f, -13.88885f, 83.99995f, 18.99203f, 2.14294f)
        else -> null
    }

    /**
     * One unit of the persisted delay arrays is 10 ms.  The collection clips
     * use the approved 300 ms visual transition; channel delays shift only
     * that channel's start, exactly as the iOS collection renderer does.
     */
    fun sample(elapsedMs: Long): CollectionGestureSample {
        val returnStart = RETURN_PHASE_START_MS
        val completeAt = RETURN_PHASE_START_MS + PHASE_DURATION_MS + returnDelayMs.maxOrNull()!!

        return when {
            elapsedMs < returnStart -> CollectionGestureSample(
                interpolate(rest, active, forwardDelayMs, elapsedMs), false
            )
            elapsedMs < completeAt -> CollectionGestureSample(
                interpolate(active, rest, returnDelayMs, elapsedMs - returnStart), false
            )
            else -> CollectionGestureSample(rest.copy(), true)
        }
    }

    private fun interpolate(
        from: CollectionFingerPose,
        to: CollectionFingerPose,
        delayMs: IntArray,
        elapsedMs: Long
    ): CollectionFingerPose {
        val result = IntArray(CollectionFingerPose.CHANNEL_COUNT)
        for (channel in result.indices) {
            val delay = delayMs[channel].toLong()
            val distance = to.values[channel] - from.values[channel]
            val progress = ((elapsedMs - delay).coerceAtLeast(0L).toFloat() / PHASE_DURATION_MS.toFloat())
                .coerceIn(0f, 1f)
            result[channel] = (from.values[channel] + distance * progress).toInt()
        }
        return CollectionFingerPose(result)
    }

    companion object {
        const val PHASE_DURATION_MS = 300L
        const val RETURN_PHASE_START_MS = 600L

        private const val GESTURE_KEY = 5
        private const val CUP_GRIP = 8
        private const val HALF_GRAB = 9
        private const val THUMBS_UP = 11

        private val NATURAL_ROTATION = floatArrayOf(
            0.02396739f, 0.98753860f, 0.15519880f, 0f,
            0.40552100f, 0.13228750f, -0.90438340f, 0f,
            -0.91372660f, 0.08462486f, -0.39731100f, 0f,
            0f, 0f, 0f, 1f
        )
        private const val NATURAL_SCALE = 0.4654335f
        private const val NATURAL_POSITION_X = -4.250008f
        private const val NATURAL_POSITION_Y = -4.410004f

        private val KEY_GROUP_ROTATION = floatArrayOf(
            -0.95077740f, -0.17839470f, 0.25330290f, 0f,
            -0.29913490f, 0.31576530f, -0.90043010f, 0f,
            0.08064778f, -0.93190720f, -0.35358260f, 0f,
            0f, 0f, 0f, 1f
        )
        private const val KEY_GROUP_SCALE = 0.5f
        private const val KEY_GROUP_POSITION_X = -13.66667f
        private const val KEY_GROUP_POSITION_Y = -15.82666f

        private val BOARD_ROTATION = floatArrayOf(
            0.02144533f, -0.96454210f, 0.26282900f, 0f,
            -0.45556490f, 0.22457320f, 0.86132560f, 0f,
            -0.88986420f, -0.13822900f, -0.43464840f, 0f,
            0f, 0f, 0f, 1f
        )
        private const val BOARD_SCALE = 0.4059688f
        private const val BOARD_POSITION_X = -15.0f
        private const val BOARD_POSITION_Y = -12.82666f

        private fun pose(i: Int, m: Int, r: Int, l: Int, t: Int, rotation: Int) =
            CollectionFingerPose.of(i, m, r, l, t, rotation)

        private fun objectTransform(
            x: Float, y: Float, z: Float,
            rotationX: Float, rotationY: Float, rotationZ: Float,
            scale: Float
        ): FloatArray {
            val result = FloatArray(16)
            Matrix.setIdentityM(result, 0)
            Matrix.translateM(result, 0, x, y, z)
            Matrix.rotateM(result, 0, rotationX, 1f, 0f, 0f)
            Matrix.rotateM(result, 0, rotationY, 0f, 1f, 0f)
            Matrix.rotateM(result, 0, rotationZ, 0f, 0f, 1f)
            Matrix.scaleM(result, 0, scale, scale, scale)
            return result
        }

        private fun cardTransform(
            rotation: FloatArray,
            scale: Float,
            positionX: Float,
            positionY: Float
        ): FloatArray = rotation.copyOf().also { result ->
            // iOS uses translation * rotation * scale. Android matrices are
            // column-major as well, so scaling the three basis columns and
            // writing the translation column reproduces it exactly.
            for (column in 0..2) {
                val offset = column * 4
                result[offset] *= scale
                result[offset + 1] *= scale
                result[offset + 2] *= scale
            }
            result[12] = positionX
            result[13] = positionY
            result[14] = 0f
            result[15] = 1f
        }

        /**
         * Values below are the approved iOS collection clips, not BLE command
         * values.  Keeping them here makes Android previews deterministic and
         * prevents one card from sharing pose state with another.
         */
        fun forGesture(gestureId: Int): CollectionGestureClip? = when (gestureId) {
            1 -> CollectionGestureClip(gestureId, pose(100, 100, 100, 100, 100, 0), pose(0, 0, 0, 0, 0, 0),
                forwardDelayMs = intArrayOf(150, 150, 150, 150, 0, 0),
                returnDelayMs = intArrayOf(0, 0, 0, 0, 150, 0))
            2 -> CollectionGestureClip(gestureId, pose(0, 100, 100, 100, 100, 0), pose(100, 100, 100, 100, 100, 0))
            3 -> CollectionGestureClip(gestureId, pose(50, 55, 0, 0, 70, 100), pose(0, 0, 0, 0, 0, 100))
            4 -> CollectionGestureClip(gestureId, pose(100, 100, 100, 100, 80, 100), pose(0, 0, 0, 0, 0, 100),
                forwardDelayMs = intArrayOf(100, 100, 100, 100, 0, 0),
                returnDelayMs = intArrayOf(0, 0, 0, 0, 100, 0))
            // Key has a special two-axis thumb conversion on iOS.  Its Android
            // renderer will receive those calibrated mechanical values below.
            5 -> CollectionGestureClip(gestureId, pose(100, 100, 100, 100, 100, 0), pose(100, 100, 100, 100, 50, 0))
            6 -> CollectionGestureClip(gestureId, pose(0, 100, 100, 0, 60, 67), pose(0, 0, 0, 0, 100, 0))
            7 -> CollectionGestureClip(gestureId, pose(50, 100, 100, 100, 70, 100), pose(0, 100, 100, 100, 70, 100))
            8 -> CollectionGestureClip(gestureId, pose(55, 58, 60, 100, 50, 100), pose(0, 0, 0, 100, 0, 100))
            9 -> CollectionGestureClip(gestureId, pose(50, 55, 55, 50, 100, 0), pose(0, 0, 0, 0, 100, 0))
            10 -> CollectionGestureClip(gestureId, pose(50, 0, 0, 0, 70, 100), pose(0, 0, 0, 0, 0, 100))
            11 -> CollectionGestureClip(gestureId, pose(100, 100, 100, 100, 100, 0), pose(100, 100, 100, 100, 0, 0))
            13 -> CollectionGestureClip(gestureId, pose(0, 0, 100, 100, 70, 100), pose(50, 55, 100, 100, 70, 100))
            14 -> CollectionGestureClip(gestureId, pose(100, 100, 100, 0, 0, 0), pose(100, 100, 100, 100, 100, 0))
            15 -> CollectionGestureClip(gestureId, pose(12, 10, 11, 13, 60, 67), pose(100, 100, 100, 100, 100, 67))
            else -> null
        }
    }
}
