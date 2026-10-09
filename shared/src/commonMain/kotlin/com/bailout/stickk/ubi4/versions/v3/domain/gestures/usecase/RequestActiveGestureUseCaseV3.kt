package com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase

import com.bailout.stickk.ubi4.versions.v3.domain.gestures.V3GesturesRepository

class RequestActiveGestureUseCaseV3(
    private val repository: V3GesturesRepository,
    private val requireInteractionEnabled: Boolean = true,
) {
    operator fun invoke(deviceAddress: String): Boolean {
        if (requireInteractionEnabled) {
            val current = repository.getActiveGesture()
            if (!current.isInteractionEnabled || current.deviceAddress != deviceAddress) return false
        }
        return repository.requestActiveGesture(deviceAddress)
    }
}
