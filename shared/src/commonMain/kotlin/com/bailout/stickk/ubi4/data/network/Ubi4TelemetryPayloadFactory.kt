package com.bailout.stickk.ubi4.data.network

import com.bailout.stickk.ubi4.data.state.TelemetryGestureCounters
import com.bailout.stickk.ubi4.models.network.TelemetryMessage
import com.bailout.stickk.ubi4.models.network.TelemetryMessageData
import com.bailout.stickk.ubi4.models.network.TelemetryMessagesRequest
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GestureEnum

internal object Ubi4TelemetryPayloadFactory {
    private val baseGestures = listOf(
        GestureEnum.GESTURE_NO_GESTURE,
        GestureEnum.GESTURE_FIST,
        GestureEnum.GESTURE_POINT,
        GestureEnum.GESTURE_PINCH,
        GestureEnum.GESTURE_FIST_THUMB_OVER,
        GestureEnum.GESTURE_KEY,
        GestureEnum.GESTURE_ROCK,
        GestureEnum.GESTURE_TWIZZERS,
        GestureEnum.GESTURE_CUPHOLDER,
        GestureEnum.GESTURE_HALF_GRAB,
        GestureEnum.GESTURE_OK,
        GestureEnum.GESTURE_THUMB_UP,
        GestureEnum.GESTURE_MIDDLE_FINGER,
        GestureEnum.GESTURE_DOUBLE_POINT,
        GestureEnum.GESTURE_CALL_ME,
        GestureEnum.GESTURE_NATURAL_POSITION
    )

    private val customGestures = listOf(
        GestureEnum.GESTURE_CUSTOM_0,
        GestureEnum.GESTURE_CUSTOM_1,
        GestureEnum.GESTURE_CUSTOM_2,
        GestureEnum.GESTURE_CUSTOM_3,
        GestureEnum.GESTURE_CUSTOM_4,
        GestureEnum.GESTURE_CUSTOM_5,
        GestureEnum.GESTURE_CUSTOM_6,
        GestureEnum.GESTURE_CUSTOM_7,
        GestureEnum.GESTURE_CUSTOM_8,
        GestureEnum.GESTURE_CUSTOM_9,
        GestureEnum.GESTURE_CUSTOM_10,
        GestureEnum.GESTURE_CUSTOM_11,
        GestureEnum.GESTURE_CUSTOM_12,
        GestureEnum.GESTURE_CUSTOM_13
    )

    fun build(
        deviceId: String,
        occurred: Long,
        counters: TelemetryGestureCounters
    ): TelemetryMessagesRequest {
        val baseGestureCounts = countsByGesture(baseGestures, counters.baseGestureMovementCount)
        val customGestureCounts = countsByGesture(customGestures, counters.customGestureMovementCount)
        val grips = baseGestureCounts
            .filterKeys { it != GestureEnum.GESTURE_NO_GESTURE.name }
            .values
            .sum() + customGestureCounts.values.sum()

        return TelemetryMessagesRequest(
            messages = listOf(
                TelemetryMessage(
                    deviceId = deviceId,
                    occurred = occurred,
                    data = TelemetryMessageData(
                        version = counters.telemetryVersionString(),
                        gestureMovementCount = baseGestureCounts,
                        userGestureMovementCount = customGestureCounts,
                        grips = grips
                    )
                )
            )
        )
    }

    private fun countsByGesture(gestures: List<GestureEnum>, counts: List<Long>): Map<String, Long> =
        gestures.associate { gesture ->
            gesture.name to (counts.getOrNull(gesture.number.toTelemetryIndex()) ?: 0L)
        }

    private fun Int.toTelemetryIndex(): Int =
        if (this >= GestureEnum.GESTURE_CUSTOM_0.number) {
            this - GestureEnum.GESTURE_CUSTOM_0.number
        } else {
            this
        }

    private fun TelemetryGestureCounters.telemetryVersionString(): String =
        when {
            telemetryVersion != null && telemetrySubversion != null -> "$telemetryVersion.$telemetrySubversion"
            telemetryVersion != null -> telemetryVersion.toString()
            else -> ""
        }
}
