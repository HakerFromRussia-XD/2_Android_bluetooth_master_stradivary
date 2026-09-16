package com.bailout.stickk.ubi4.versions.v3.domain.gestures

class SaveRotationGroupSelectionUseCaseV3(private val repository: V3GesturesRepository) {
    operator fun invoke(deviceAddress: String, selection: V3RotationGroupSelection): Boolean {
        val current = repository.getActiveGesture()
        if (!current.isInteractionEnabled || current.deviceAddress != deviceAddress) return false
        val group = repository.getRotationGroupGestureIds() ?: return false
        if (group != selection.originalGestureIds || !selection.isValid()) return false
        // The existing save path always writes, even with unchanged checkmarks.
        // Its persistence and BLE packet keep the first eight slots when duplicates are present.
        return repository.setRotationGroup(deviceAddress, selection.resultingGestureIds().take(8))
    }
}
