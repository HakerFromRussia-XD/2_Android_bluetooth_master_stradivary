package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets

import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.V3SpecialSettingsSection
import kotlinx.coroutines.flow.map

class DataFactoryV3SpecialSettingsWidgetsSource(
    private val dataFactory: DataFactory = DataFactory(),
    private val mapper: V3SpecialSettingsWidgetMapper = V3SpecialSettingsWidgetMapper(),
) : V3SpecialSettingsWidgetsSource {
    override val updates = UiState.updateFlow.map { Unit }

    override fun snapshot(section: V3SpecialSettingsSection): V3SpecialSettingsWidgetsSnapshot {
        val profile = if (UiState.isInterfaceV3Activated) UiState.activeV3DeviceProfile else V3DeviceProfile.NOT_V3
        val items = when {
            profile == V3DeviceProfile.NOT_V3 -> emptyList()
            section == V3SpecialSettingsSection.APPLICATION -> dataFactory.mobileWidgets()
            else -> dataFactory.prepareData(display = 2)
        }
        return V3SpecialSettingsWidgetsSnapshot(profile, WidgetRepoProvider.mac(), mapper.fromItems(items))
    }
}
