package com.bailout.stickk.ubi4.versions.v3.domain.gestures

data class V3Gestures(
    val activeGesture: V3ActiveGesture,
    val rotationGroupGestureIds: List<Int>,
    val isRotationGroupAvailable: Boolean,
)

class GetGesturesUseCaseV3(private val repository: V3GesturesRepository) {
    operator fun invoke(): V3Gestures {
        val group = repository.getRotationGroupGestureIds()
        return V3Gestures(repository.getActiveGesture(), group.orEmpty(), group != null)
    }
}
