package com.bailout.stickk.ubi4.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bailout.stickk.ubi4.data.state.DashboardSlotContentInput
import com.bailout.stickk.ubi4.data.state.DashboardSlotContentSchemas
import com.bailout.stickk.ubi4.data.state.DashboardSlotContentUiState

private object DashboardSlotContentColors {
    val Back = Color(0xFF2A2A2A)
    val Card = Color(0xFF3A3A3A)
    val Border = Color(0xFF444444)
    val White = Color(0xFFFCFCFC)
    val Muted = Color(0xFFBDBDBD)
}

sealed interface DashboardSlotContentAction {
    data object Refresh : DashboardSlotContentAction
    data object Send : DashboardSlotContentAction
    data object Save : DashboardSlotContentAction
    data object Reset : DashboardSlotContentAction
    data object ResetAll : DashboardSlotContentAction
    data class ParameterChanged(val path: String, val value: String) : DashboardSlotContentAction
}

@Composable
fun DashboardSlotContentScreen(
    state: DashboardSlotContentUiState,
    onActionClick: (DashboardSlotContentAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DashboardSlotContentColors.Back)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 132.dp)
        ) {
            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Содержимое Слота",
                modifier = Modifier.fillMaxWidth(),
                color = DashboardSlotContentColors.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            InfoCard(state = state)

            Spacer(modifier = Modifier.height(12.dp))

            ContentCard(
                state = state,
                onActionClick = onActionClick
            )
        }

        BottomActionBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onActionClick = onActionClick
        )
    }
}

@Composable
private fun InfoCard(state: DashboardSlotContentUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DashboardSlotContentColors.Card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "0x${state.dataCode.toHexByte()} - ${state.title} (v${state.version}.${state.subVersion})",
            color = DashboardSlotContentColors.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Размер: ${state.declaredSize} байт",
            color = DashboardSlotContentColors.Muted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        state.statusMessage?.let { message ->
            Text(
                text = message,
                color = DashboardSlotContentColors.Muted,
                fontSize = 12.sp
            )
        }
        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = DashboardSlotContentColors.Muted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ContentCard(
    state: DashboardSlotContentUiState,
    onActionClick: (DashboardSlotContentAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        when {
            state.isLoading -> ContentMessage("Загрузка содержимого...")
            state.data.isEmpty() -> ContentMessage("Данные слота пустые")
            else -> SlotParameters(
                state = state,
                onActionClick = onActionClick
            )
        }
    }
}

@Composable
private fun ContentMessage(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        color = DashboardSlotContentColors.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal
    )
}

@Composable
private fun SlotParameters(
    state: DashboardSlotContentUiState,
    onActionClick: (DashboardSlotContentAction) -> Unit
) {
    val parsed = DashboardSlotContentSchemas.parse(state)

    parsed.inputs.forEach { input ->
        SlotParameterInput(
            nameParameter = input.name,
            value = input.value,
            onValueChange = { value ->
                onActionClick(DashboardSlotContentAction.ParameterChanged(input.path, value))
            },
            horizontalPadding = 0.dp,
            keyboardType = input.keyboardType()
        )
    }

    parsed.structureGroups.forEach { group ->
        SlotParameterStructureGroup(
            title = group.title,
            structures = group.structures.map { structure ->
                SlotParameterStructureModel(
                    index = structure.index,
                    title = structure.title,
                    parameters = structure.parameters.map { it.toSlotParameterItem() }
                )
            },
            onParameterValueChange = { structureIndex, parameterIndex, value ->
                val input = group.structures
                    .firstOrNull { it.index == structureIndex }
                    ?.parameters
                    ?.getOrNull(parameterIndex)
                if (input != null) {
                    onActionClick(DashboardSlotContentAction.ParameterChanged(input.path, value))
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun BottomActionBar(
    modifier: Modifier,
    onActionClick: (DashboardSlotContentAction) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DashboardSlotContentColors.Back)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton("Обновить", Modifier.weight(1f)) { onActionClick(DashboardSlotContentAction.Refresh) }
            ActionButton("Отправить", Modifier.weight(1f)) { onActionClick(DashboardSlotContentAction.Send) }
            ActionButton("Сохранить", Modifier.weight(1f)) { onActionClick(DashboardSlotContentAction.Save) }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton("Сбросить", Modifier.weight(1f)) { onActionClick(DashboardSlotContentAction.Reset) }
            ActionButton("Сбросить все", Modifier.weight(1f)) { onActionClick(DashboardSlotContentAction.ResetAll) }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DashboardSlotContentColors.Card)
            .border(
                BorderStroke(1.dp, DashboardSlotContentColors.Border),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = DashboardSlotContentColors.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun DashboardSlotContentInput.toSlotParameterItem(): SlotParameterStructureItem =
    SlotParameterStructureItem(
        nameParameter = name,
        value = value,
        path = path,
        keyboardType = keyboardType()
    )

private fun DashboardSlotContentInput.keyboardType(): KeyboardType =
    if (value.startsWith("[") || value.startsWith("0x", ignoreCase = true)) {
        KeyboardType.Text
    } else {
        KeyboardType.Number
    }

private fun Int.toHexByte(): String =
    toString(16).uppercase().padStart(2, '0')
