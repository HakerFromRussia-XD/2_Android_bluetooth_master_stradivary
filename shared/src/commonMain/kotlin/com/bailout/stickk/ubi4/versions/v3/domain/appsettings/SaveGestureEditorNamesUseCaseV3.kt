package com.bailout.stickk.ubi4.versions.v3.domain.appsettings

class SaveGestureEditorNamesUseCaseV3(private val repository: V3AppSettingsRepository) {
    operator fun invoke(names: List<String>) = repository.saveGestureEditorNames(names)
}

class RenameCustomGestureUseCaseV3(private val repository: V3CustomGestureNamesRepository) {
    operator fun invoke(index: Int, name: String): List<String> = repository.renameGesture(index, name)
}
