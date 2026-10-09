package com.bailout.stickk.ubi4.versions.v3.domain.appsettings

class GetGesturesPreferencesUseCaseV3(private val repository: V3GesturesPreferencesRepository) {
    operator fun invoke(): V3GesturesPreferences = repository.getGesturesPreferences()
}
