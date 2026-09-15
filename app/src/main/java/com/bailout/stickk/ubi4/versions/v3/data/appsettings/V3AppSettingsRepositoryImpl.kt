package com.bailout.stickk.ubi4.versions.v3.data.appsettings

import android.content.SharedPreferences
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3AppSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3SpecialSettingsSection
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class V3AppSettingsRepositoryImpl(private val preferences: SharedPreferences) : V3AppSettingsRepository {
    override fun getSpecialSettingsSection() = if (preferences.getBoolean(PreferenceKeysUbi4.LAST_ACTIVE_SETTINGS_FILTER, false)) {
        V3SpecialSettingsSection.APPLICATION
    } else {
        V3SpecialSettingsSection.PROSTHESIS
    }

    override fun setSpecialSettingsSection(section: V3SpecialSettingsSection) {
        preferences.edit().putBoolean(
            PreferenceKeysUbi4.LAST_ACTIVE_SETTINGS_FILTER, section == V3SpecialSettingsSection.APPLICATION,
        ).apply()
    }

    override fun getAutoLoginEnabled() = preferences.getBoolean(PreferenceKeysUbi4.SET_MODE_SMART_CONNECTION, false)

    override fun setAutoLoginEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(PreferenceKeysUbi4.SET_MODE_SMART_CONNECTION, enabled).apply()
    }

    override fun observeAutoLoginEnabled() = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key == PreferenceKeysUbi4.SET_MODE_SMART_CONNECTION) {
                runCatching { getAutoLoginEnabled() }
                    .onSuccess { trySend(it) }
                    .onFailure { close(it) }
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        try {
            trySend(getAutoLoginEnabled())
            awaitClose()
        } finally {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.distinctUntilChanged()
}
