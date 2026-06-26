package com.bailout.stickk.ubi4.data.parser

import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.local.BindingGestureGroup
import com.bailout.stickk.ubi4.data.state.ParameterTypedValueV3
import com.bailout.stickk.ubi4.models.ble.ParameterCodecIdV3
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.BaseCommandsV3.PROSTHESIS_MODULE_CONTROL
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_GET_BINDING_DATA
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ProsthesisModuleControlEnum.PWCE_SET_BINDING_DATA
import com.bailout.stickk.ubi4.resources.com.bailout.stickk.ubi4.bridges.WidgetCommandBridgeV3
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.CRC_TABLE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_BINDING_DATA
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BindingGroupV3Test {

    @Test
    fun `binding group should round trip old 12-pair payload`() {
        val original = BindingGestureGroup().apply {
            setGestureAt(0, 1 to 64)
            setGestureAt(1, 2 to 65)
            setGestureAt(2, 7 to 3)
            setGestureAt(11, 12 to 80)
        }

        val payload = original.toPayloadBytes()
        val decoded = BindingGestureGroup.fromPayloadBytes(payload)

        assertEquals(BindingGestureGroup.PAYLOAD_SIZE, payload.size)
        assertEquals(original.toGestureList(), decoded.toGestureList())
        assertEquals(original.toHexString(), decoded.toHexString())
    }

    @Test
    fun `binding codec should decode get response payload and serialize as hex`() {
        val data = BindingGestureGroup().apply {
            setGestureAt(0, 3 to 66)
            setGestureAt(3, 9 to 12)
        }
        val payload = byteArrayOf(PWCE_GET_BINDING_DATA.number) + data.toPayloadBytes()

        val typed = ParameterCodecRegistryV3.decodeFromPayload(
            codecId = ParameterCodecIdV3.BINDING_GROUP,
            payload = ByteArrayView(payload, offset = 0, length = payload.size)
        ) as ParameterTypedValueV3.BindingGroup

        assertEquals(data.toGestureList(), typed.value.toGestureList())
        assertEquals(
            data.toHexString(),
            ParameterCodecRegistryV3.encodeToSerialized(ParameterCodecIdV3.BINDING_GROUP, typed)
        )
    }

    @Test
    fun `binding set command should build V3 long packet`() {
        val bindingGroup = BindingGestureGroup().apply {
            setGestureAt(0, 1 to 64)
            setGestureAt(1, 2 to 65)
        }
        val packet = BLECommandsV3.sendBindingGroup(bindingGroup)
        val header = packet.copyOfRange(0, 5)
        val payload = packet.copyOfRange(5, packet.size)

        assertEquals(0x80, header[0].toInt() and 0xFF)
        assertEquals(PROSTHESIS_MODULE_CONTROL.number.toInt(), header[1].toInt() and 0xFF)
        assertEquals(payload.size - 1, header[2].toInt() and 0xFF)
        assertEquals(crcExcludeLast(header), header.last().toInt() and 0xFF)

        assertEquals(PWCE_SET_BINDING_DATA.number.toInt(), payload[0].toInt() and 0xFF)
        assertContentEquals(bindingGroup.toPayloadBytes(), payload.copyOfRange(1, payload.size - 1))
        assertEquals(crcExcludeLast(payload), payload.last().toInt() and 0xFF)
    }

    @Test
    fun `binding set command should map to binding readback`() {
        val packet = WidgetCommandBridgeV3.buildReadRequest(
            parameterID = PROSTHESIS_MODULE_CONTROL.number.toInt(),
            dataCode = PWCE_SET_BINDING_DATA.number.toInt()
        )

        assertNotNull(packet)
        assertEquals(PROSTHESIS_MODULE_CONTROL.number.toInt(), packet[1].toInt() and 0xFF)
        assertEquals(PWCE_GET_BINDING_DATA.number.toInt(), packet[2].toInt() and 0xFF)
        assertEquals(crcExcludeLast(packet), packet.last().toInt() and 0xFF)
    }

    @Test
    fun `binding response route should target V3 binding flow`() {
        val route = WidgetResponseRoutesV3.find(
            command = PROSTHESIS_MODULE_CONTROL.number.toInt(),
            responseSubcommand = PWCE_GET_BINDING_DATA.number.toInt()
        )

        assertNotNull(route)
        assertEquals(P_KEY_BINDING_DATA, route.parameterKey)
        assertEquals(WidgetEmitTargetV3.BINDING_GROUP_FLOW, route.emitTarget)
    }

    private fun crcExcludeLast(data: ByteArray): Int {
        var result = 0
        for (i in 0 until data.size - 1) {
            result = CRC_TABLE[result xor (data[i].toInt() and 0xFF)]
        }
        return result
    }
}
