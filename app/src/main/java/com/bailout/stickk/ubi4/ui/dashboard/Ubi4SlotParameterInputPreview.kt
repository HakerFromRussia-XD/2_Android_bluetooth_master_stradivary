package com.bailout.stickk.ubi4.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
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
private fun Ubi4SlotParameterInputPreview() {
    Column(
        modifier = Modifier
            .width(390.dp)
            .background(Color(0xFF2A2A2A))
            .padding(vertical = 8.dp)
    ) {

        Ubi4SlotParameterInput(
            nameParameter = "ProductUUID_Prefix",
            value = "",
            onValueChange = {},
            placeholder = "ELBOW-H"
        )
        Ubi4SlotParameterInput(
            nameParameter = "ProductAdditionalInfoType",
            value = "0",
            onValueChange = {},
            enabled = false
        )
    }
}
