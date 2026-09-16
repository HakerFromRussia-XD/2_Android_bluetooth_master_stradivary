package com.bailout.stickk.ubi4.versions.v3.domain.gestures

class MoveGestureInRotationGroupUseCaseV3(private val repository: V3GesturesRepository) {
    operator fun invoke(
        deviceAddress: String,
        fromPosition: Int,
        toPosition: Int,
        expectedGestureIds: List<Int>,
    ): Boolean {
        val current = repository.getActiveGesture()
        if (!current.isInteractionEnabled || current.deviceAddress != deviceAddress) return false
        val group = repository.getRotationGroupGestureIds() ?: return false
        if (group != expectedGestureIds || fromPosition !in group.indices || toPosition !in group.indices) return false
        if (fromPosition == toPosition) return false
        val reordered = group.toMutableList().apply { add(toPosition, removeAt(fromPosition)) }
        // Preserve the drag callback: moving between different positions always sends,
        // including a swap of identical gestures.
        return repository.setRotationGroup(deviceAddress, reordered)
    }
}
