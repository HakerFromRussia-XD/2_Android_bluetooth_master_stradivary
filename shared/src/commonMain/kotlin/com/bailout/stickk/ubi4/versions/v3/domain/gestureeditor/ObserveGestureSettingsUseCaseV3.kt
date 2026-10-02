package com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor

class ObserveGestureSettingsUseCaseV3(private val repository: V3GestureEditorRepository) {
    operator fun invoke() = repository.observeSettings()
}
