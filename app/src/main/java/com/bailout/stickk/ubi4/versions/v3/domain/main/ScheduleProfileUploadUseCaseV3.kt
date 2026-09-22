package com.bailout.stickk.ubi4.versions.v3.domain.main

class ScheduleProfileUploadUseCaseV3(private val repository: V3MainRepository) {
    private var requested = false

    fun resetRequest() { requested = false }

    fun cancelPendingUpload() {
        requested = false
        repository.cancelAppCloseProfileUpload()
    }

    operator fun invoke(openingScanAfterDisconnect: Boolean, deviceConnected: Boolean, language: String) {
        if (openingScanAfterDisconnect || requested || !deviceConnected) return
        // Preserve the guard even if the scheduler skips a blank serial or throws.
        requested = true
        repository.enqueueAppCloseProfileUpload(language.takeIf { it.isNotBlank() } ?: "en")
    }
}
