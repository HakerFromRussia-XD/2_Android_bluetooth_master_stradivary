package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets

import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.versions.v3.domain.appsettings.V3SpecialSettingsSection

class DataFactoryV3SpecialSettingsWidgetsSource(
    private val dataFactory: DataFactory = DataFactory(),
    private val mapper: V3SpecialSettingsWidgetMapper = V3SpecialSettingsWidgetMapper(),
) : V3SpecialSettingsWidgetsSource {
    override fun widgets(profile: V3DeviceProfile, section: V3SpecialSettingsSection): List<V3SpecialSettingsWidget> {
        val items = when {
            profile == V3DeviceProfile.NOT_V3 -> emptyList()
            section == V3SpecialSettingsSection.APPLICATION -> dataFactory.mobileWidgets()
            else -> dataFactory.prepareData(display = 2)
        }
        return mapper.fromItems(items)
    }
}
