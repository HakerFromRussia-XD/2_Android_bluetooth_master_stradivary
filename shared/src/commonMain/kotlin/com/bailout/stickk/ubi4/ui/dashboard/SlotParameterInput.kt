package com.bailout.stickk.ubi4.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private object DashboardColors {
    val Back = Color(0xFF2A2A2A)
    val Card = Color(0xFF3A3A3A)
    val GrayBorder = Color(0xFF444444)
    val DeactivateText = Color(0xFF838383)
    val White = Color(0xFFFCFCFC)
}

data class SlotParameterStructureItem(
    val nameParameter: String,
    val value: String,
    val path: String = nameParameter,
    val placeholder: String = "Введите значение",
    val enabled: Boolean = true,
    val keyboardType: KeyboardType = KeyboardType.Text
)

data class SlotParameterStructureModel(
    val index: Int,
    val title: String,
    val parameters: List<SlotParameterStructureItem>
)

@Composable
fun SlotParameterStructureGroup(
    title: String,
    structures: List<SlotParameterStructureModel>,
    onParameterValueChange: (structureIndex: Int, parameterIndex: Int, value: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupShape = RoundedCornerShape(16.dp)
    val expectedParameterCount = structures.firstOrNull()?.parameters?.size

    require(structures.all { it.parameters.size == expectedParameterCount }) {
        "All SlotParameterStructureModel items must contain the same number of SlotParameterInput items"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(groupShape)
            .background(DashboardColors.Back)
            .border(width = 1.dp, color = DashboardColors.GrayBorder, shape = groupShape)
            .padding(top = 12.dp, bottom = 8.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            color = DashboardColors.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Light,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        structures.forEach { structure ->
            SlotParameterStructure(
                title = structure.title,
                parameters = structure.parameters,
                onParameterValueChange = { parameterIndex, value ->
                    onParameterValueChange(structure.index, parameterIndex, value)
                }
            )
        }
    }
}

@Composable
fun SlotParameterStructure(
    title: String,
    parameters: List<SlotParameterStructureItem>,
    onParameterValueChange: (index: Int, value: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val structureShape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
            .clip(structureShape)
            .background(DashboardColors.Back)
            .border(width = 1.dp, color = DashboardColors.GrayBorder, shape = structureShape)
            .padding(top = 12.dp, bottom = 8.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            color = DashboardColors.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        parameters.forEachIndexed { index, parameter ->
            SlotParameterInput(
                nameParameter = parameter.nameParameter,
                value = parameter.value,
                onValueChange = { onParameterValueChange(index, it) },
                placeholder = parameter.placeholder,
                enabled = parameter.enabled,
                keyboardType = parameter.keyboardType
            )
        }
    }
}

@Composable
fun SlotParameterInput(
    nameParameter: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Введите значение",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    height: Dp = 48.dp,
    labelWeight: Float = 0.62f,
    valueWeight: Float = 0.38f,
    horizontalPadding: Dp = 16.dp,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val outerShape = RoundedCornerShape(12.dp)
    val textColor = if (enabled) DashboardColors.White else DashboardColors.DeactivateText
    val underlineColor = if (enabled) DashboardColors.White else DashboardColors.DeactivateText
    val isByteArrayValue = value.startsWith("[") && value.contains(",")
    val effectiveSingleLine = singleLine && !isByteArrayValue
    val effectiveHeight = if (isByteArrayValue && height == 48.dp) 76.dp else height
    val effectiveLabelWeight = if (isByteArrayValue) 0.44f else labelWeight
    val effectiveValueWeight = if (isByteArrayValue) 0.56f else valueWeight
    val effectiveValueAlignment = if (isByteArrayValue) TextAlign.Start else TextAlign.End
    val effectiveContentAlignment = if (isByteArrayValue) Alignment.CenterStart else Alignment.CenterEnd
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }
    val fieldTextStyle = TextStyle(
        color = textColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.Light,
        textAlign = effectiveValueAlignment
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DashboardColors.Back)
            .padding(horizontal = horizontalPadding, vertical = 4.dp)
            .height(effectiveHeight)
            .clip(outerShape)
            .background(DashboardColors.Card)
            .border(width = 1.dp, color = DashboardColors.GrayBorder, shape = outerShape)
            .padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = nameParameter,
            modifier = Modifier
                .weight(effectiveLabelWeight)
                .padding(end = 8.dp),
            color = DashboardColors.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(effectiveValueWeight)
                .fillMaxHeight()
                .heightIn(min = 48.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
                .padding(start = 10.dp, end = 8.dp),
            enabled = enabled,
            singleLine = effectiveSingleLine,
            textStyle = fieldTextStyle,
            cursorBrush = SolidColor(DashboardColors.White),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = keyboardActions,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = effectiveContentAlignment
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            modifier = Modifier.fillMaxWidth(),
                            color = DashboardColors.DeactivateText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light,
                            textAlign = effectiveValueAlignment,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(if (isFocused) 2.dp else 1.dp)
                            .background(underlineColor)
                    )
                }
            }
        )
    }
}
