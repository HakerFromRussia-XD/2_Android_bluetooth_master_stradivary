package com.bailout.stickk.ubi4.versions.v3.domain.gestures

class GetRotationGroupSelectionUseCaseV3(private val repository: V3GesturesRepository) {
    operator fun invoke(): V3RotationGroupSelection? {
        val group = repository.getRotationGroupGestureIds()?.toList() ?: return null
        val selection = V3RotationGroupSelection(group, emptySet())
        return selection.copy(selectedGestureIds = group.filter { it in selection.availableGestureIds }.toSet())
            .takeIf { it.isValid() }
    }
}
