package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings

import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3SpecialSettingsSection
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3AppSettingsRepository
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3GesturesPreferences
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3CustomGestureNames
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll

class FakeV3AppSettingsRepository : V3AppSettingsRepository {
    override fun getGestureEditorNames() = error("Editor names are not used by this fake")
    override fun saveGestureEditorNames(names: List<String>) = error("Editor names are not used by this fake")
    var storedCustomGestureNames = V3CustomGestureNames()
    var customGestureNameReads = 0
    override fun getCustomGestureNames(): V3CustomGestureNames {
        customGestureNameReads++
        return storedCustomGestureNames
    }

    var storedGesturesPreferences = V3GesturesPreferences()
    val gesturesSectionWrites = mutableListOf<Int>()
    val collectionExpandedWrites = mutableListOf<Boolean>()
    val gestureSettingsNumberWrites = mutableListOf<Int>()
    override fun setGestureSettingsNumber(gestureNumber: Int) {
        gestureSettingsNumberWrites.add(gestureNumber)
    }
    override fun getGesturesPreferences() = storedGesturesPreferences
    override fun setGesturesSection(section: Int) {
        gesturesSectionWrites.add(section)
        storedGesturesPreferences = storedGesturesPreferences.copy(selectedSection = section)
    }
    override fun setFactoryGestureCollectionExpanded(expanded: Boolean) {
        collectionExpandedWrites.add(expanded)
        storedGesturesPreferences = storedGesturesPreferences.copy(isFactoryCollectionExpanded = expanded)
    }

    var settingsSection = V3SpecialSettingsSection.PROSTHESIS
    val sectionWrites = mutableListOf<V3SpecialSettingsSection>()
    var sectionReadError: Exception? = null
    var sectionSaveError: Exception? = null
    override fun getSpecialSettingsSection(): V3SpecialSettingsSection {
        sectionReadError?.let { throw it }
        return settingsSection
    }
    override fun setSpecialSettingsSection(section: V3SpecialSettingsSection) {
        sectionSaveError?.let { throw it }
        sectionWrites.add(section)
        settingsSection = section
    }
    val autoLogin = MutableStateFlow(false)
    val writes = mutableListOf<Boolean>()
    var readError: Exception? = null
    var saveError: Exception? = null
    override fun getAutoLoginEnabled(): Boolean {
        readError?.let { throw it }
        return autoLogin.value
    }
    override fun observeAutoLoginEnabled() = flow {
        emit(getAutoLoginEnabled())
        emitAll(autoLogin)
    }
    override fun setAutoLoginEnabled(enabled: Boolean) {
        saveError?.let { throw it }
        writes.add(enabled)
        autoLogin.value = enabled
    }
}
