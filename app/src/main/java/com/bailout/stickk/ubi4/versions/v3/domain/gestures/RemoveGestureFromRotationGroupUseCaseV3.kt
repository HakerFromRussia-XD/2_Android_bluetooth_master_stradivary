package com.bailout.stickk.ubi4.versions.v3.domain.gestures

class RemoveGestureFromRotationGroupUseCaseV3(private val repository: V3GesturesRepository) {
    operator fun invoke(deviceAddress: String, position: Int, expectedGestureIds: List<Int>): Boolean {
        val current = repository.getActiveGesture()
        if (!current.isInteractionEnabled || current.deviceAddress != deviceAddress) return false
        val group = repository.getRotationGroupGestureIds() ?: return false
        // The same gesture may occupy several slots. Remove only the confirmed position
        // and reject a confirmation made against an earlier order or another group.
        if (group != expectedGestureIds || position !in group.indices || group.size > 8) return false
        return repository.setRotationGroup(deviceAddress, group.filterIndexed { index, _ -> index != position })
    }
}
