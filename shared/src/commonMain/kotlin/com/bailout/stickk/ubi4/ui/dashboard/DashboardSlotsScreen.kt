package com.bailout.stickk.ubi4.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bailout.stickk.ubi4.data.state.DashboardSlotInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4

@Immutable
data class DashboardSlotUiItem(
    val deviceAddress: Int,
    val dataCode: Int,
    val title: String,
    val version: Int,
    val subVersion: Int,
    val dataSize: Int,
    val startAddressShift: Int,
    val crc: Int
) {
    val hexCode: String
        get() = dataCode.toString(16).uppercase().padStart(2, '0')

    val versionLabel: String
        get() = "v$version.$subVersion"
}

fun DashboardSlotInfo.toDashboardSlotUiItem(): DashboardSlotUiItem =
    DashboardSlotUiItem(
        deviceAddress = deviceAddress,
        dataCode = dataCode,
        title = dataCode.toDashboardSlotTitle(),
        version = dataTypeVersion,
        subVersion = dataTypeSubVersion,
        dataSize = dataSize,
        startAddressShift = startAddressShift,
        crc = crc
    )

@Composable
fun DashboardSlotsScreen(
    onSlotClick: (DashboardSlotUiItem) -> Unit,
    modifier: Modifier = Modifier,
    slots: List<DashboardSlotUiItem> = emptyList(),
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    val colors = LocalDashboardPalette.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Список слотов",
            modifier = Modifier.fillMaxWidth(),
            color = colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
        ) {
            when {
                isLoading -> StatusRow(text = "Загрузка слотов...")
                errorMessage != null -> StatusRow(text = errorMessage)
                slots.isEmpty() -> StatusRow(text = "Список слотов пуст")
                else -> {
                    slots.forEachIndexed { index, slot ->
                        SlotRow(
                            slot = slot,
                            showDivider = index != slots.lastIndex,
                            onClick = { onSlotClick(slot) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SlotRow(
    slot: DashboardSlotUiItem,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalDashboardPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "0x${slot.hexCode} - ${slot.title} (${slot.versionLabel})",
                modifier = Modifier.weight(1f),
                color = colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "›",
                color = colors.text,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light
            )
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 8.dp)
                    .background(colors.border)
            )
        }
    }
}

@Composable
private fun StatusRow(text: String) {
    val colors = LocalDashboardPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun Int.toDashboardSlotTitle(): String =
    PreferenceKeysUbi4.DataTableSlotsCode.entries
        .firstOrNull { (it.number.toInt() and 0xFF) == this }
        ?.name
        ?.removePrefix("DTCE_")
        ?.removePrefix("DCTE_")
        ?.removeSuffix("_TYPE")
        ?: "Unknown"
