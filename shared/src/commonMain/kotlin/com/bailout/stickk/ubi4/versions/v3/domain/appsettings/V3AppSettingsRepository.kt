package com.bailout.stickk.ubi4.versions.v3.domain.appsettings

import kotlinx.coroutines.flow.Flow

interface V3AppSettingsRepository {
    fun getGestureEditorNames(): V3GestureEditorNames
    fun saveGestureEditorNames(names: List<String>)
    fun getCustomGestureNames(): V3CustomGestureNames
    fun getGesturesPreferences(): V3GesturesPreferences
    fun setGesturesSection(section: Int)
    fun setFactoryGestureCollectionExpanded(expanded: Boolean)
    fun setGestureSettingsNumber(gestureNumber: Int)
    fun getSpecialSettingsSection(): V3SpecialSettingsSection
    fun setSpecialSettingsSection(section: V3SpecialSettingsSection)
    fun getAutoLoginEnabled(): Boolean
    fun observeAutoLoginEnabled(): Flow<Boolean>
    fun setAutoLoginEnabled(enabled: Boolean)
}
