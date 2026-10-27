package com.bailout.stickk.ubi4.ui.dashboard

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class DashboardPalette(
    val background: Color = Color(0xFF2A2A2A),
    val surface: Color = Color(0xFF3A3A3A),
    val border: Color = Color(0xFF444444),
    val text: Color = Color(0xFFFCFCFC),
    val mutedText: Color = Color(0xFFBDBDBD),
    val disabledText: Color = Color(0xFF838383)
)

val LocalDashboardPalette = staticCompositionLocalOf { DashboardPalette() }
