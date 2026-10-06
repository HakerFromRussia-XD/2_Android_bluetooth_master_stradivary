package com.bailout.stickk.ubi4.versions.v3.domain.appsettings

class SetGesturesSectionUseCaseV3(private val repository: V3AppSettingsRepository) {
    operator fun invoke(section: Int) = repository.setGesturesSection(section)
}
