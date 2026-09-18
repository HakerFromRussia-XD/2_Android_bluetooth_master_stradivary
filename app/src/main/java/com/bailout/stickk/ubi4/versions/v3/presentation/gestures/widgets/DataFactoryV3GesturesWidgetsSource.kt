package com.bailout.stickk.ubi4.versions.v3.presentation.gestures.widgets

import com.bailout.stickk.ubi4.data.DataFactory
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile

class DataFactoryV3GesturesWidgetsSource(
    private val dataFactory: DataFactory = DataFactory(),
    private val mapper: V3GesturesWidgetMapper = V3GesturesWidgetMapper(),
) : V3GesturesWidgetsSource {
    override fun widgets(profile: V3DeviceProfile): List<V3GesturesWidget> =
        if (profile == V3DeviceProfile.NOT_V3) emptyList() else mapper.fromItems(dataFactory.prepareData(display = 0))
}
