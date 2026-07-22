package com.bailout.stickk.ubi4.data.network

import com.bailout.stickk.ubi4.adapters.widgetDelegateAdaptersV3.SettingsProfileApplierV3

class Ubi4SettingsProfileReceiver(
    private val sender: Ubi4SettingsProfileSender = Ubi4SettingsProfileSender()
) {
    suspend fun downloadAndApplyForSerial(
        serial: String,
        lang: String
    ): SettingsProfileDownloadResult {
        val result = sender.downloadProfileSettingsForSerial(serial, lang)
        SettingsProfileApplierV3.apply(result.applyValues)
        return result
    }
}
