package com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor

class WriteGestureSettingsUseCaseV3(
    private val repository: V3GestureEditorRepository,
    private val clampPositions: Boolean = true,
) {
    operator fun invoke(settings: V3GestureSettings, command: V3GestureCommand, name: String) {
        repository.writeSettings(if (clampPositions) settings.copy(
            openPositions = settings.openPositions.map { it.coerceIn(0, 100) },
            closePositions = settings.closePositions.map { it.coerceIn(0, 100) },
        ) else settings, command, name)
    }
}
