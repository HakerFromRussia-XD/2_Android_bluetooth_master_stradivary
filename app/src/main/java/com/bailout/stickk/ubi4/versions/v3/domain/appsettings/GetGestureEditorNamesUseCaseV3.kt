package com.bailout.stickk.ubi4.versions.v3.domain.appsettings

data class V3GestureEditorNames(val gestureNumber: Int, val names: List<String>)

class GetGestureEditorNamesUseCaseV3(private val repository: V3AppSettingsRepository) {
    operator fun invoke(): V3GestureEditorNames = repository.getGestureEditorNames()
}
