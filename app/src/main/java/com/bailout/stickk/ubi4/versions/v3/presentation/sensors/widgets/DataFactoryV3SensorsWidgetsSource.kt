package com.bailout.stickk.ubi4.versions.v3.presentation.sensors.widgets

import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile

class DataFactoryV3SensorsWidgetsSource(
    private val dataFactory: DataFactory = DataFactory(),
    private val mapper: V3SensorsWidgetMapper = V3SensorsWidgetMapper(),
) : V3SensorsWidgetsSource {
    override fun widgets(profile: V3DeviceProfile): List<V3SensorsWidget> {
        val items = if (profile == V3DeviceProfile.NOT_V3) emptyList() else dataFactory.prepareData(display = 1)
        return mapper.fromItems(items)
    }
}
