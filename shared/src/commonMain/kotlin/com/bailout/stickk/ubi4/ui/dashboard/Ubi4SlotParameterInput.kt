package com.bailout.stickk.ubi4.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private object Ubi4DashboardColors {
    val Back = Color(0xFF2A2A2A)
    val Gray = Color(0xFF373737)
    val GrayBorder = Color(0xFF444444)
    val DeactivateText = Color(0xFF838383)
    val White = Color(0xFFFCFCFC)
}

@Composable
fun Ubi4SlotParameterInput(
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
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val outerShape = RoundedCornerShape(12.dp)
    val valueShape = RoundedCornerShape(10.dp)
    val textColor = if (enabled) Ubi4DashboardColors.White else Ubi4DashboardColors.DeactivateText
    val fieldTextStyle = TextStyle(
        color = textColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.Light
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Ubi4DashboardColors.Back)
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(height)
            .clip(outerShape)
            .background(Ubi4DashboardColors.Back)
            .border(width = 1.dp, color = Ubi4DashboardColors.GrayBorder, shape = outerShape)
            .padding(start = 10.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = nameParameter,
            modifier = Modifier
                .weight(labelWeight)
                .padding(end = 8.dp),
            color = Ubi4DashboardColors.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(valueWeight)
                .fillMaxHeight()
                .clip(valueShape)
                .background(Ubi4DashboardColors.Gray)
                .border(width = 1.dp, color = Ubi4DashboardColors.GrayBorder, shape = valueShape)
                .padding(horizontal = 10.dp),
            enabled = enabled,
            singleLine = singleLine,
            textStyle = fieldTextStyle,
            cursorBrush = SolidColor(Ubi4DashboardColors.White),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = keyboardActions,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Ubi4DashboardColors.DeactivateText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
