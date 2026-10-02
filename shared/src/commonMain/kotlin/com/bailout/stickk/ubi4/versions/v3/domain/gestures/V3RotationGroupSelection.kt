package com.bailout.stickk.ubi4.versions.v3.domain.gestures

data class V3RotationGroupSelection(
    val originalGestureIds: List<Int>,
    val selectedGestureIds: Set<Int>,
) {
    // Same selectable protocol IDs as the existing collection; gesture 12 stays hidden.
    val availableGestureIds: List<Int> get() = (1..11).toList() + (13..15).toList() + (64..77).toList()

    internal fun resultingGestureIds(): List<Int> =
        originalGestureIds.filter { it in selectedGestureIds } +
            availableGestureIds.filter { it in selectedGestureIds && it !in originalGestureIds }

    // Preserve the existing dialog limit: it counts checked gestures, not occupied slots.
    internal fun isValid(): Boolean = selectedGestureIds.all { it in availableGestureIds } &&
        selectedGestureIds.size <= 8
}
