package com.bailout.stickk.ubi4.versions.v3.domain.gestures

class SelectGestureUseCaseV3(private val repository: V3GesturesRepository) {
    operator fun invoke(deviceAddress: String, gestureId: Int): Boolean {
        // V3 protocol: factory gestures 1–15 (12 is hidden in the collection), custom gestures 64–77.
        if (gestureId !in 1..15 && gestureId !in 64..77) return false
        val current = repository.getActiveGesture()
        if (!current.isInteractionEnabled || current.deviceAddress != deviceAddress) return false
        return repository.selectGesture(deviceAddress, gestureId)
    }
}
