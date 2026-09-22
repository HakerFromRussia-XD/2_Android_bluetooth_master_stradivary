package com.bailout.stickk.ubi4.versions.v3.presentation.accountstatistics

import android.content.Context
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.ui.gestures.GestureCollectionFactory
import com.bailout.stickk.ubi4.ui.widgets.GestureUsageChartItem

internal object V3AccountStatisticsChartMapper {
    fun map(context: Context, state: V3AccountStatisticsUiState): List<GestureUsageChartItem> {
        val names = GestureCollectionFactory.create(context) { _, default -> default }
            .associate { it.gestureId to it.gestureName }
        return state.gestures.map { gesture ->
            val index = gesture.customGestureIndex
            val title = if (index == null) {
                names[gesture.gestureId]?.takeIf { it.isNotBlank() } ?: "Gesture ${gesture.gestureId}"
            } else {
                gesture.customName ?: context.getString(customNameResources.getOrNull(index) ?: R.string.gesture_1_btn)
            }
            GestureUsageChartItem(gesture.gestureId, title, gesture.count)
        }
    }

    private val customNameResources = listOf(
        R.string.gesture_1_btn, R.string.gesture_2_btn, R.string.gesture_3_btn,
        R.string.gesture_4_btn, R.string.gesture_5_btn, R.string.gesture_6_btn,
        R.string.gesture_7_btn, R.string.gesture_8_btn, R.string.gesture_9_btn,
        R.string.gesture_10_btn, R.string.gesture_11_btn, R.string.gesture_12_btn,
        R.string.gesture_13_btn, R.string.gesture_14_btn, R.string.gesture_15_btn,
    )
}
