package com.bailout.stickk.ubi4.data.network

import com.bailout.stickk.ubi4.data.local.repository.SettingsProfileApplyValue
import com.bailout.stickk.ubi4.versions.v3.data.settingsprofiles.V3SettingsProfilesUpdates

class Ubi4SettingsProfileReceiver(
    private val sender: Ubi4SettingsProfileSender = Ubi4SettingsProfileSender(),
    private val applyProfileValues: (List<SettingsProfileApplyValue>) -> Unit = { defaultApplyProfileValues(it) },
) {
    companion object {
        // BLEController keeps its no-argument construction; DI binds this before it connects.
        @Volatile
        internal lateinit var defaultApplyProfileValues: (List<SettingsProfileApplyValue>) -> Unit
    }

    suspend fun downloadAndApplyForSerial(
        serial: String,
        lang: String
    ): SettingsProfileDownloadResult {
        val result = sender.downloadProfileSettingsForSerial(serial, lang)
        try {
            applyProfileValues(result.applyValues)
        } finally {
            V3SettingsProfilesUpdates.notifyChanged()
        }
        return result
    }
}
