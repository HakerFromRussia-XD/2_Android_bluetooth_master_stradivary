package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings

import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3SpecialSettingsSection
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3AppSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll

class FakeV3AppSettingsRepository : V3AppSettingsRepository {
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
