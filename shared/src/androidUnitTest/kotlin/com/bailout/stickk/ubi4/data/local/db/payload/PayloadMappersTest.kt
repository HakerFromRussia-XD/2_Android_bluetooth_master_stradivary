package com.bailout.stickk.ubi4.data.local.db.payload

import com.bailout.stickk.ubi4.data.widget.endStructures.CommandParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.endStructures.SliderParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PayloadMappersTest {

    @Test
    fun `toEndStruct should restore slider with string label`() {
        val payload = basePayload(
            widgetCode = PreferenceKeysUbi4.ParameterWidgetCode.PWCE_SLIDER.number.toInt(),
            widgetLabelType = PreferenceKeysUbi4.ParameterWidgetLabelType.PWLTE_STRING_LABEL.number.toInt(),
            label = "Speed",
            minProgress = 5,
            maxProgress = 70,
            increment = 0.5f
        )

        val restored = payload.toEndStruct()
        assertTrue(restored is SliderParameterWidgetSStruct)
        assertEquals("Speed", restored.baseParameterWidgetSStruct.label)
        assertEquals(5, restored.minProgress)
        assertEquals(70, restored.maxProgress)
        assertEquals(0.5f, restored.increment)
    }

    @Test
    fun `toEndStruct should restore button with numeric label code`() {
        val payload = basePayload(
            widgetCode = PreferenceKeysUbi4.ParameterWidgetCode.PWCE_BUTTON.number.toInt(),
            widgetLabelType = PreferenceKeysUbi4.ParameterWidgetLabelType.PWLTE_CODE_LABEL.number.toInt(),
            labelCode = 9,
            clickCommand = 11,
            pressedCommand = 12,
            releasedCommand = 13
        )

        val restored = payload.toEndStruct()
        assertTrue(restored is CommandParameterWidgetEStruct)
        assertEquals(9, restored.baseParameterWidgetEStruct.labelCode)
        assertEquals(11, restored.clickCommand)
        assertEquals(12, restored.pressedCommand)
        assertEquals(13, restored.releasedCommand)
    }

    @Test
    fun `toEndStruct should fallback to base struct for unknown widget`() {
        val payload = basePayload(
            widgetCode = 0x7F,
            widgetLabelType = PreferenceKeysUbi4.ParameterWidgetLabelType.PWLTE_CODE_LABEL.number.toInt(),
            labelCode = 3
        )

        val restored = payload.toEndStruct()
        assertTrue(restored is BaseParameterWidgetEStruct)
        assertEquals(3, restored.labelCode)
    }

    @Test
    fun `toWidgetPayloadOrNull should preserve slider limits and increment`() {
        val base = BaseParameterWidgetStruct(
            widgetType = 1,
            widgetLabelType = PreferenceKeysUbi4.ParameterWidgetLabelType.PWLTE_CODE_LABEL.number.toInt(),
            widgetCode = PreferenceKeysUbi4.ParameterWidgetCode.PWCE_SLIDER.number.toInt(),
            display = 1,
            widgetPosition = 2,
            deviceId = 3,
            widgetId = 4,
            dataOffset = 5,
            dataSize = 6,
            channelOffset = 0,
            parameterInfoSet = mutableSetOf(ParameterInfo(10, 20, 30, 40)),
            keyMobileSettings = "mobile_key"
        )
        val slider = SliderParameterWidgetEStruct(
            baseParameterWidgetEStruct = BaseParameterWidgetEStruct(base, labelCode = 77),
            minProgress = 2,
            maxProgress = 98,
            increment = 2.5f
        )

        val payload = slider.toWidgetPayloadOrNull()
        assertNotNull(payload)
        assertEquals(PreferenceKeysUbi4.ParameterWidgetCode.PWCE_SLIDER.number.toInt(), payload.widgetCode)
        assertEquals(77, payload.labelCode)
        assertEquals(2, payload.minProgress)
        assertEquals(98, payload.maxProgress)
        assertEquals(2.5f, payload.increment)
        assertEquals(1, payload.parameterInfoSet.size)
    }

    @Test
    fun `ParameterInfo payload mapping should round trip`() {
        val source = ParameterInfo(100, 200, 5, 8)
        val payload = source.toPayload()
        val model = payload.toModel()

        assertEquals(source.parameterID, model.parameterID)
        assertEquals(source.dataCode, model.dataCode)
        assertEquals(source.deviceAddress, model.deviceAddress)
        assertEquals(source.dataOffsets, model.dataOffsets)
    }

    private fun basePayload(
        widgetCode: Int,
        widgetLabelType: Int,
        labelCode: Int = 0,
        label: String? = null,
        minProgress: Int? = null,
        maxProgress: Int? = null,
        increment: Float? = null,
        clickCommand: Int? = null,
        pressedCommand: Int? = null,
        releasedCommand: Int? = null,
    ): BaseParameterWidgetPayload =
        BaseParameterWidgetPayload(
            widgetType = 1,
            widgetLabelType = widgetLabelType,
            widgetCode = widgetCode,
            display = 1,
            widgetPosition = 1,
            deviceId = 1,
            widgetId = 1,
            dataOffset = 0,
            dataSize = 1,
            channelOffset = 0,
            parameterInfoSet = listOf(ParameterInfoPayload(1, 2, 3, 4)),
            keyMobileSettings = "m",
            labelCode = labelCode,
            label = label,
            minProgress = minProgress,
            maxProgress = maxProgress,
            increment = increment,
            clickCommand = clickCommand,
            pressedCommand = pressedCommand,
            releasedCommand = releasedCommand
        )
}
