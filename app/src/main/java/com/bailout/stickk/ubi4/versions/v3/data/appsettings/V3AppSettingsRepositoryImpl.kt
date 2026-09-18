package com.bailout.stickk.ubi4.versions.v3.data.appsettings

import android.content.SharedPreferences
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3AppSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3GesturesPreferences
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3CustomGestureNames
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3SpecialSettingsSection
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class V3AppSettingsRepositoryImpl(private val preferences: SharedPreferences) : V3AppSettingsRepository {
    override fun getCustomGestureNames(): V3CustomGestureNames {
        // Preserve both existing readers, including their different fallback MACs and names.
        val directMac = preferences.getString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, "NOT SET!").toString()
        val collectionMac = preferences.getString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, "") ?: ""
        return V3CustomGestureNames(
            names = List(PreferenceKeysUbi4.NUM_GESTURES) { index ->
                preferences.getString(PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM + directMac + index, "NOT SET!").toString()
            },
            collectionNames = List(PreferenceKeysUbi4.NUM_GESTURES) { index ->
                preferences.getString(PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM + collectionMac + index, null)
            },
        )
    }

    override fun getGesturesPreferences() = V3GesturesPreferences(
        selectedSection = preferences.getInt(PreferenceKeysUbi4.LAST_ACTIVE_GESTURE_FILTER, 1),
        isFactoryCollectionExpanded = preferences.getInt(PreferenceKeysUbi4.LAST_HIDE_COLLECTION_BTN_STATE, 1) == 1,
    )

    override fun setGesturesSection(section: Int) {
        preferences.edit().putInt(PreferenceKeysUbi4.LAST_ACTIVE_GESTURE_FILTER, section).apply()
    }

    override fun setFactoryGestureCollectionExpanded(expanded: Boolean) {
        preferences.edit().putInt(PreferenceKeysUbi4.LAST_HIDE_COLLECTION_BTN_STATE, if (expanded) 1 else 0).apply()
    }

    override fun setGestureSettingsNumber(gestureNumber: Int) {
        preferences.edit().putInt(PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM, gestureNumber).apply()
    }

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
