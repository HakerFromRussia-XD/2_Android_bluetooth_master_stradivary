package com.bailout.stickk.ubi4.versions.v3.data.device

import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetStateBridgeV3
import com.bailout.stickk.ubi4.versions.v3.domain.device.V3HandSideRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class V3HandSideRepositoryImpl : V3HandSideRepository {
    private val parameter = ParameterInfo(0x10, 0x0E, 1, 0)

    override fun getHandSide(): Int = WidgetStateBridgeV3.getSpinnerValueOrDefault(
        parameter.deviceAddress, parameter.parameterID, parameter.dataCode, -1,
    )

    override fun observeHandSideResponses(onResponse: (Int?) -> Unit) = WidgetStateBridgeV3.observeUpdates { snapshot ->
        if (snapshot.addressDevice == parameter.deviceAddress &&
            snapshot.parameterID == parameter.parameterID && snapshot.dataCode == parameter.dataCode
        ) {
            onResponse(runCatching {
                Json.parseToJsonElement(snapshot.serializedValue).jsonObject["spinnerValue"]?.jsonPrimitive?.intOrNull
            }.getOrNull())
        }
    }

    override fun storeHandSide(side: Int) {
        WidgetStateBridgeV3.setSpinnerValue(parameter.deviceAddress, parameter.parameterID, parameter.dataCode, side)
    }
}
