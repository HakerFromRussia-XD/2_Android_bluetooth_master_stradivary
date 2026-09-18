package com.bailout.stickk.ubi4.versions.v3.domain.gestures

import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3AppSettingsRepository

class SaveGestureSettingsSelectionUseCaseV3(private val repository: V3AppSettingsRepository) {
    operator fun invoke(target: V3GestureSettingsTarget) = repository.setGestureSettingsNumber(target.gestureNumber)
}
