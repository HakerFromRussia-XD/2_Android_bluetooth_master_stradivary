package com.bailout.stickk.ubi4.versions.v3.domain.appsettings

import kotlinx.coroutines.flow.Flow

interface V3AppSettingsRepository {
    fun getSpecialSettingsSection(): V3SpecialSettingsSection
    fun setSpecialSettingsSection(section: V3SpecialSettingsSection)
    fun getAutoLoginEnabled(): Boolean
    fun observeAutoLoginEnabled(): Flow<Boolean>
    fun setAutoLoginEnabled(enabled: Boolean)
}
