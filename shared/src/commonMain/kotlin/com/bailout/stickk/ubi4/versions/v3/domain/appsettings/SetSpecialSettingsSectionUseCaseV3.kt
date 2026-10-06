package com.bailout.stickk.ubi4.versions.v3.domain.appsettings

class SetSpecialSettingsSectionUseCaseV3(private val repository: V3AppSettingsRepository) {
    operator fun invoke(section: V3SpecialSettingsSection) {
        if (repository.getSpecialSettingsSection() == section) return
        repository.setSpecialSettingsSection(section)
    }
}
