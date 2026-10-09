package com.bailout.stickk.ubi4.versions.v3.domain.appsettings

import kotlinx.coroutines.flow.Flow

interface V3CustomGestureNamesReader {
    fun getCustomGestureNames(): V3CustomGestureNames
}

interface V3CustomGestureNamesRepository : V3CustomGestureNamesReader {
    /** The repository owns indexed-edit storage ordering and returns the resulting names, including for invalid indices. */
    fun renameGesture(index: Int, name: String): List<String>
}

interface V3AutoLoginSettingsRepository {
    fun getAutoLoginEnabled(): Boolean
    fun setAutoLoginEnabled(enabled: Boolean)
}

interface V3GesturesPreferencesRepository {
    fun getGesturesPreferences(): V3GesturesPreferences
    fun setGesturesSection(section: Int)
    fun setFactoryGestureCollectionExpanded(expanded: Boolean)
}

interface V3AppSettingsRepository : V3CustomGestureNamesReader, V3AutoLoginSettingsRepository, V3GesturesPreferencesRepository {
    fun getGestureEditorNames(): V3GestureEditorNames
    fun saveGestureEditorNames(names: List<String>)
    fun setGestureSettingsNumber(gestureNumber: Int)
    fun getSpecialSettingsSection(): V3SpecialSettingsSection
    fun setSpecialSettingsSection(section: V3SpecialSettingsSection)
    fun observeAutoLoginEnabled(): Flow<Boolean>
}
