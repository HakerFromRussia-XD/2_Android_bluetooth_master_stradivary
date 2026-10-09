package com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor

class ObserveGestureSettingsUseCaseV3(private val repository: V3GestureEditorRepository) {
    operator fun invoke() = repository.observeSettings()

    fun observeResponses(callback: (V3GestureSettingsResponse) -> Unit): () -> Unit =
        repository.subscribeSettingsResponses(callback)
}
