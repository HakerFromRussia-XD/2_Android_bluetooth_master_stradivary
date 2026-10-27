package com.bailout.stickk.ubi4.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.colorResource
import com.bailout.stickk.R

@Composable
internal fun DashboardTheme(content: @Composable () -> Unit) {
    val colors = if (booleanResource(R.bool.ubi4_light_system_bars)) {
        DashboardPalette(
            background = colorResource(R.color.ubi4_back),
            surface = colorResource(R.color.ubi4_gray),
            border = colorResource(R.color.ubi4_gray_border),
            text = colorResource(R.color.ubi4_white),
            mutedText = colorResource(R.color.ubi4_text_muted),
            disabledText = colorResource(R.color.ubi4_deactivate_text)
        )
    } else {
        DashboardPalette()
    }
    CompositionLocalProvider(LocalDashboardPalette provides colors, content = content)
}
