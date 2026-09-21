package com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class RequestGestureSettingsUseCaseV3(private val repository: V3GestureEditorRepository) {
    suspend operator fun invoke(gestureId: Int) {
        repository.awaitReady()
        currentCoroutineContext().ensureActive()
        repository.requestSettings(gestureId)
    }
}
