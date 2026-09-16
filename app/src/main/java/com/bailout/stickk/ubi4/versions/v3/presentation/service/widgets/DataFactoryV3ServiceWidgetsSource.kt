package com.bailout.stickk.ubi4.versions.v3.presentation.service.widgets

import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile

class DataFactoryV3ServiceWidgetsSource(
    private val dataFactory: DataFactory = DataFactory(),
    private val mapper: V3ServiceWidgetMapper = V3ServiceWidgetMapper(),
) : V3ServiceWidgetsSource {
    override fun widgets(profile: V3DeviceProfile): List<V3ServiceWidget> {
        val items = if (profile == V3DeviceProfile.NOT_V3) emptyList() else dataFactory.prepareData(display = 4)
        return mapper.fromItems(items)
    }
}
