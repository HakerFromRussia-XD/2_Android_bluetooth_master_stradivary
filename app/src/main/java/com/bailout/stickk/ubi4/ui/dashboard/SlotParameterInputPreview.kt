package com.bailout.stickk.ubi4.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(
    name = "Slot parameter input",
    showBackground = true,
    backgroundColor = 0xFF2A2A2A,
    widthDp = 390
)
@Composable
private fun SlotParameterInputPreview() {
    var structures by remember {
        mutableStateOf(
            listOf(
                SlotParameterStructureModel(
                    index = 0,
                    title = "Product info structure",
                    parameters = listOf(
                        SlotParameterStructureItem(
                            nameParameter = "ProductName",
                            value = "INDY"
                        ),
                        SlotParameterStructureItem(
                            nameParameter = "ProductVersion",
                            value = "4",
                            keyboardType = KeyboardType.Number
                        ),
                        SlotParameterStructureItem(
                            nameParameter = "ProductUUID_Prefix",
                            value = "",
                            placeholder = "ELBOW-H"
                        ),
                        SlotParameterStructureItem(
                            nameParameter = "ProductAdditionalInfoType",
                            value = "0",
                            keyboardType = KeyboardType.Number
                        )
                    )
                ),
                SlotParameterStructureModel(
                    index = 1,
                    title = "BLE identity structure",
                    parameters = listOf(
                        SlotParameterStructureItem(
                            nameParameter = "DeviceName",
                            value = "UBI4"
                        ),
                        SlotParameterStructureItem(
                            nameParameter = "DeviceAddress",
                            value = "A0:B1:C2:D3:E4:F5"
                        ),
                        SlotParameterStructureItem(
                            nameParameter = "ServiceUUID",
                            value = "",
                            placeholder = "0000180F-0000"
                        ),
                        SlotParameterStructureItem(
                            nameParameter = "MtuSize",
                            value = "247",
                            keyboardType = KeyboardType.Number
                        )
                    )
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .width(390.dp)
            .background(Color(0xFF2A2A2A))
            .padding(vertical = 8.dp)
    ) {
        SlotParameterStructureGroup(
            title = "Slot configuration group",
            structures = structures,
            onParameterValueChange = { structureIndex, parameterIndex, value ->
                structures = structures.map { structure ->
                    if (structure.index == structureIndex) {
                        structure.copy(
                            parameters = structure.parameters.mapIndexed { currentParameterIndex, parameter ->
                                if (currentParameterIndex == parameterIndex) {
                                    parameter.copy(value = value)
                                } else {
                                    parameter
                                }
                            }
                        )
                    } else {
                        structure
                    }
                }
            }
        )
    }
}
