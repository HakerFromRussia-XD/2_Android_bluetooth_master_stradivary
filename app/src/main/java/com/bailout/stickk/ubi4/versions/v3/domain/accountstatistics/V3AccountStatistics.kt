package com.bailout.stickk.ubi4.versions.v3.domain.accountstatistics

data class V3AccountStatistics(
    val baseGestureCounts: List<Long> = emptyList(),
    val customGestureCounts: List<Long> = emptyList(),
    // Null uses the localized default in UI; a saved empty name stays empty.
    val customGestureNames: List<String?> = emptyList(),
)

data class V3GestureUsage(
    val gestureId: Int,
    val count: Long,
    val customGestureIndex: Int? = null,
    val customName: String? = null,
)
