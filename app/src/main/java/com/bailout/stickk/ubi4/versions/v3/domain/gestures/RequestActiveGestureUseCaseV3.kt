package com.bailout.stickk.ubi4.versions.v3.domain.gestures

class RequestActiveGestureUseCaseV3(private val repository: V3GesturesRepository) {
    operator fun invoke(deviceAddress: String): Boolean {
        val current = repository.getActiveGesture()
        if (!current.isInteractionEnabled || current.deviceAddress != deviceAddress) return false
        return repository.requestActiveGesture(deviceAddress)
    }
}
