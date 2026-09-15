package com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets

import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import kotlinx.coroutines.flow.map

class DataFactoryV3SensorsWidgetsSource(
    private val dataFactory: DataFactory = DataFactory(),
    private val mapper: V3SensorsWidgetMapper = V3SensorsWidgetMapper(),
) : V3SensorsWidgetsSource {
    override val updates = UiState.updateFlow.map { Unit }

    override fun snapshot(): V3SensorsWidgetsSnapshot {
        val profile = if (UiState.isInterfaceV3Activated) UiState.activeV3DeviceProfile else V3DeviceProfile.NOT_V3
        val items = if (profile == V3DeviceProfile.NOT_V3) emptyList() else dataFactory.prepareData(display = 1)
        return V3SensorsWidgetsSnapshot(
            profile, WidgetRepoProvider.mac(), mapper.fromItems(items),
            animationsEnabled = !WidgetState.dbSnapshotAppliedWithCrc,
        )
    }
}
