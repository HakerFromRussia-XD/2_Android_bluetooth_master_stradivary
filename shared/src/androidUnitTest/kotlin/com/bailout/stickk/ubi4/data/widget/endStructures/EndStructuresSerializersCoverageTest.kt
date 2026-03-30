package com.bailout.stickk.ubi4.data.widget.endStructures

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EndStructuresSerializersCoverageTest {

    private val json = Json

    @Test
    fun `serializers should parse and serialize E and S structs`() {
        val baseE = "010405060708090A0B" // 18 chars
        val baseS = "010405060708090A" + "41".repeat(32) // 80 chars

        val cmdE = json.decodeFromString<CommandParameterWidgetEStruct>("\"${baseE}010203\"")
        val cmdS = json.decodeFromString<CommandParameterWidgetSStruct>("\"${baseS}040506\"")
        assertEquals(1, cmdE.clickCommand)
        assertEquals(4, cmdS.clickCommand)

        val gestureOpticE = json.decodeFromString<GestureOpticParameterWidgetEStruct>("\"${baseE}00\"")
        val gestureE = json.decodeFromString<GestureParameterWidgetEStruct>("\"${baseE}00\"")
        val gestureS = json.decodeFromString<GestureParameterWidgetSStruct>("\"${baseS}00\"")
        assertEquals(4, gestureOpticE.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode)
        assertEquals(4, gestureE.baseParameterWidgetEStruct.baseParameterWidgetStruct.widgetCode)
        assertTrue(gestureS.baseParameterWidgetSStruct.label.isNotEmpty())

        val opticE = json.decodeFromString<OpticStartLearningWidgetEStruct>("\"${baseE}\"")
        val opticS = json.decodeFromString<OpticStartLearningWidgetSStruct>("\"${baseS}00\"")
        assertEquals(0, opticE.clickCommand)
        assertTrue(opticS.baseParameterWidgetSStruct.label.isNotEmpty())

        val plotE = json.decodeFromString<PlotParameterWidgetEStruct>("\"${baseE}0A0B0C\"")
        val plotS = json.decodeFromString<PlotParameterWidgetSStruct>("\"${baseS}0D0E0F\"")
        assertEquals(10, plotE.color)
        assertEquals(13, plotS.color)

        val sliderE_mul = json.decodeFromString<SliderParameterWidgetEStruct>("\"${baseE}020305\"")
        val sliderE_div = json.decodeFromString<SliderParameterWidgetEStruct>("\"${baseE}020385\"")
        val sliderS_mul = json.decodeFromString<SliderParameterWidgetSStruct>("\"${baseS}040607\"")
        val sliderS_div = json.decodeFromString<SliderParameterWidgetSStruct>("\"${baseS}040687\"")
        assertEquals(5f, sliderE_mul.increment)
        assertEquals(0.2f, sliderE_div.increment)
        assertEquals(7f, sliderS_mul.increment)
        assertEquals(1f / 7f, sliderS_div.increment)

        val spinnerDataHex = "31" + "0A" + "4F70656E" + "0A" + "436C6F7365"
        val spinnerE = json.decodeFromString<SpinnerParameterWidgetEStruct>("\"${baseE}${spinnerDataHex}\"")
        val spinnerS = json.decodeFromString<SpinnerParameterWidgetSStruct>("\"${baseS}${spinnerDataHex}\"")
        val spinnerData = json.decodeFromString<DataSpinnerParameterWidgetStruct>("\"${spinnerDataHex}\"")
        assertEquals(1, spinnerData.selectedIndex)
        assertTrue(spinnerData.spinnerItems.isNotEmpty())
        assertEquals(spinnerData.selectedIndex, spinnerE.dataSpinnerParameterWidgetStruct.selectedIndex)
        assertEquals(spinnerData.selectedIndex, spinnerS.dataSpinnerParameterWidgetStruct.selectedIndex)

        val switchE = json.decodeFromString<SwitchParameterWidgetEStruct>("\"${baseE}01\"")
        val switchS = json.decodeFromString<SwitchParameterWidgetSStruct>("\"${baseS}00\"")
        assertTrue(switchE.switchChecked)
        assertFalse(switchS.switchChecked)

        val thE = json.decodeFromString<ThresholdParameterWidgetEStruct>("\"${baseE}\"")
        val thS = json.decodeFromString<ThresholdParameterWidgetSStruct>("\"${baseS}01020304\"")
        assertEquals(0, thE.openThresholdUpper)
        assertEquals(1, thS.openThresholdUpper)
        assertEquals(4, thS.closeThresholdLower)

        val toggleE_mul = json.decodeFromString<ToggleSliderParameterWidgetEStruct>("\"${baseE}020305\"")
        val toggleE_div = json.decodeFromString<ToggleSliderParameterWidgetEStruct>("\"${baseE}020385\"")
        val toggleS_mul = json.decodeFromString<ToggleSliderParameterWidgetSStruct>("\"${baseS}040607\"")
        val toggleS_div = json.decodeFromString<ToggleSliderParameterWidgetSStruct>("\"${baseS}040687\"")
        assertEquals(5f, toggleE_mul.increment)
        assertEquals(0.2f, toggleE_div.increment)
        assertEquals(7f, toggleS_mul.increment)
        assertEquals(1f / 7f, toggleS_div.increment)

        // explicit serialize paths
        assertEquals("\"\"", json.encodeToString(cmdE))
        assertEquals("\"\"", json.encodeToString(cmdS))
        assertEquals("\"\"", json.encodeToString(gestureOpticE))
        assertEquals("\"\"", json.encodeToString(gestureE))
        assertEquals("\"\"", json.encodeToString(gestureS))
        assertEquals("\"\"", json.encodeToString(opticE))
        assertEquals("\"\"", json.encodeToString(opticS))
        assertEquals("\"\"", json.encodeToString(plotE))
        assertEquals("\"\"", json.encodeToString(plotS))
        assertEquals("\"\"", json.encodeToString(sliderE_mul))
        assertEquals("\"\"", json.encodeToString(sliderS_mul))
        assertEquals("\"\"", json.encodeToString(spinnerE))
        assertEquals("\"\"", json.encodeToString(spinnerS))
        assertEquals("\"\"", json.encodeToString(spinnerData))
        assertEquals("\"\"", json.encodeToString(switchE))
        assertEquals("\"\"", json.encodeToString(switchS))
        assertEquals("\"\"", json.encodeToString(thE))
        assertEquals("\"\"", json.encodeToString(thS))
        assertEquals("\"\"", json.encodeToString(toggleE_mul))
        assertEquals("\"\"", json.encodeToString(toggleS_mul))
    }

    @Test
    fun `serializers should keep defaults on short input`() {
        val cmdE = json.decodeFromString<CommandParameterWidgetEStruct>("\"AA\"")
        val cmdS = json.decodeFromString<CommandParameterWidgetSStruct>("\"AA\"")
        val swE = json.decodeFromString<SwitchParameterWidgetEStruct>("\"AA\"")
        val swS = json.decodeFromString<SwitchParameterWidgetSStruct>("\"AA\"")
        val thS = json.decodeFromString<ThresholdParameterWidgetSStruct>("\"AA\"")

        assertEquals(0, cmdE.clickCommand)
        assertEquals(0, cmdS.clickCommand)
        assertFalse(swE.switchChecked)
        assertFalse(swS.switchChecked)
        assertEquals(0, thS.openThresholdUpper)
    }
}
