package com.bailout.stickk.ubi4.versions.v3.domain.appsettings

class GetSpecialSettingsSectionUseCaseV3(private val repository: V3AppSettingsRepository) {
    operator fun invoke(): V3SpecialSettingsSection = repository.getSpecialSettingsSection()
}
