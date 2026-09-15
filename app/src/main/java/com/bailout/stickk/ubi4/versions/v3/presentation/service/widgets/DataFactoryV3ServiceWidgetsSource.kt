package com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets

import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.UiState
import com.bailout.stickk.ubi4.data.state.WidgetState
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import kotlinx.coroutines.flow.map

class DataFactoryV3ServiceWidgetsSource(
    private val dataFactory: DataFactory = DataFactory(),
    private val mapper: V3ServiceWidgetMapper = V3ServiceWidgetMapper(),
) : V3ServiceWidgetsSource {
    override val updates = UiState.updateFlow.map { Unit }

    override fun snapshot(): V3ServiceWidgetsSnapshot {
        val profile = if (UiState.isInterfaceV3Activated) UiState.activeV3DeviceProfile else V3DeviceProfile.NOT_V3
        val items = if (profile == V3DeviceProfile.NOT_V3) emptyList() else dataFactory.prepareData(display = 4)
        return V3ServiceWidgetsSnapshot(profile, WidgetRepoProvider.mac(), mapper.fromItems(items),
            animationsEnabled = !WidgetState.dbSnapshotAppliedWithCrc)
    }
}
