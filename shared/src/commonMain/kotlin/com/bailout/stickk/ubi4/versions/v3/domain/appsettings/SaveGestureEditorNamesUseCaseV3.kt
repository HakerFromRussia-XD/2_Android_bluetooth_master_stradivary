package com.bailout.stickk.ubi4.versions.v3.domain.appsettings

class SaveGestureEditorNamesUseCaseV3(private val repository: V3AppSettingsRepository) {
    operator fun invoke(names: List<String>) = repository.saveGestureEditorNames(names)
}
