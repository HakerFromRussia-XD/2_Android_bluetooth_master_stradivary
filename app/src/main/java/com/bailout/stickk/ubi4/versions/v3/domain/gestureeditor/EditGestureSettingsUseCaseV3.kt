package com.bailout.stickk.ubi4.versions.v3.domain.gestureeditor

/** Retains the editor's different position, delay and command conventions. */
class EditGestureSettingsUseCaseV3 {
    fun load(settings: V3GestureSettings, gestureId: Int) = settings.copy(
        // A response never replaced the gesture ID selected when opening the editor.
        gestureId = gestureId,
        openPositions = settings.openPositions.map { it.coerceIn(0, 100) },
        closePositions = settings.closePositions.map { it.coerceIn(0, 100) },
    )

    fun position(settings: V3GestureSettings, index: Int, value: Int, command: V3GestureCommand): V3GestureSettings {
        val position = if (index >= 4) value.coerceIn(0, 100) else value
        return if (command == V3GestureCommand.OPEN) {
            settings.copy(openPositions = settings.openPositions.replaced(index, position))
        } else {
            settings.copy(closePositions = settings.closePositions.replaced(index, position))
        }
    }

    fun delay(settings: V3GestureSettings, index: Int, value: Int, command: V3GestureCommand) =
        if (command == V3GestureCommand.CLOSE) {
            settings.copy(openToCloseDelays = settings.openToCloseDelays.replaced(index, value))
        } else {
            settings.copy(closeToOpenDelays = settings.closeToOpenDelays.replaced(index, value))
        }

    private fun List<Int>.replaced(index: Int, value: Int) = toMutableList().also { it[index] = value }.toList()
}
