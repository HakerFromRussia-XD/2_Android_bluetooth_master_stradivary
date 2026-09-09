package com.bailout.stickk.ubi4.versions.v3.presentation.specialsettings.widgets

/** Display metadata. Parameter values and drafts live in the screen's UiState. */
data class V3SpecialSettingsWidgetInfo(
    val key: String,
    val title: String,
    val position: Int,
)

sealed interface V3SpecialSettingsWidget {
    val info: V3SpecialSettingsWidgetInfo

    data class Slider(
        override val info: V3SpecialSettingsWidgetInfo,
        val minProgress: Int,
        val maxProgress: Int,
        val increment: Float,
    ) : V3SpecialSettingsWidget

    data class ToggleSlider(
        override val info: V3SpecialSettingsWidgetInfo,
        val minProgress: Int,
        val maxProgress: Int,
        val increment: Float,
        val unitLabel: String,
    ) : V3SpecialSettingsWidget

    data class Spinner(
        override val info: V3SpecialSettingsWidgetInfo,
        val options: List<String>,
        val initialSelectedIndex: Int,
    ) : V3SpecialSettingsWidget

    data class SettingsProfile(
        override val info: V3SpecialSettingsWidgetInfo,
        val options: List<String>,
        val initialSelectedIndex: Int,
    ) : V3SpecialSettingsWidget

    data class Switch(
        override val info: V3SpecialSettingsWidgetInfo,
        val initialChecked: Boolean,
    ) : V3SpecialSettingsWidget
}
