package com.bailout.stickk.ubi4.versions.v3.data.service

import com.bailout.stickk.ubi4.data.state.*
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.versions.v3.domain.service.*
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoField.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class V3DeviceInfoRepositoryTest {
    private val originalProfile = UiState.activeV3DeviceProfile
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val serialInfo = ParameterInfoRegistry.require(P_KEY_SET_SERIAL_NUMBER)
    private var serial: String? = null
    private var name: String? = null
    private var connected: String? = null
    private var intent: String? = null
    private var address = "first"
    private val packets = mutableListOf<ByteArray>()
    private val callbacks = mutableListOf<() -> Unit>()
    private val appliedNames = mutableListOf<String>()
    private var customizations = 0
    private val repository = V3DeviceInfoRepositoryImpl(
        currentSerial = { serial }, deviceName = { name }, intentDeviceName = { intent },
        applyDeviceName = { appliedNames += it },
        enqueuePacket = { packet, callback -> packets += packet; callbacks += callback },
        recordNameCustomization = { customizations++ }, currentDeviceAddress = { address },
        connectedName = { connected },
    )
    @BeforeEach fun setUp() {
        ParameterStoreV3.clear()
        UiState.activeV3DeviceProfile = V3DeviceProfile.STANDARD_V3
        UiState.v3WidgetsInteractionEnabled.value = true
    }
    @AfterEach fun tearDown() {
        ParameterStoreV3.clear()
        UiState.activeV3DeviceProfile = originalProfile
        UiState.v3WidgetsInteractionEnabled.value = originalInteraction
    }

    @Test fun `prefill respects every fallback and strips only the name prefix`() {
        assertNull(repository.getTextForInput(DEVICE_NAME))
        intent = "INDY3-intent"
        assertEquals("intent", repository.getTextForInput(DEVICE_NAME))
        connected = "INDY3-connected"
        assertEquals("connected", repository.getTextForInput(DEVICE_NAME))
        name = "INDY3-name"
        assertEquals("name", repository.getTextForInput(DEVICE_NAME))
        serial = "INDY3-000123"
        assertEquals("000123", repository.getTextForInput(DEVICE_NAME))
        assertEquals("INDY3-000123", repository.getTextForInput(SERIAL_NUMBER))
        ParameterStoreV3.put(serialInfo, ParameterTypedValueV3.Text(" 000001 "))
        assertEquals(" 000001 ", repository.getTextForInput(SERIAL_NUMBER))
        ParameterStoreV3.put(serialInfo, ParameterTypedValueV3.Text(" "))
        assertEquals("INDY3-000123", repository.getTextForInput(SERIAL_NUMBER))
        serial = " "; name = " "; connected = " "
        assertEquals("INDY3-intent", repository.getTextForInput(SERIAL_NUMBER))
        assertTrue(packets.isEmpty())
        assertTrue(appliedNames.isEmpty())
        assertEquals(0, customizations)
    }

    @ParameterizedTest
    @CsvSource("STANDARD_V3,FTHS3-", "INDY3,INDY3-")
    fun `name uses active transport prefix and only send completion records customization`(profile: V3DeviceProfile, prefix: String) {
        UiState.activeV3DeviceProfile = profile
        serial = prefix + "Old"
        assertEquals(V3DeviceInfoWriteResult.SENT, SetDeviceInfoTextUseCaseV3(repository)(DEVICE_NAME, " Рука "))
        assertEquals(listOf(prefix + "Рука"), appliedNames)
        assertTextPacket(packets.single(), 13, prefix + "Рука")
        assertEquals(0, customizations)
        callbacks.single().invoke(); callbacks.single().invoke()
        assertEquals(1, customizations)
        assertEquals(1, packets.size)
        assertNull(ParameterStoreV3.get(serialInfo))
    }

    @Test fun `unchanged display name still writes without awarding customization`() {
        serial = "FTHS3-Name"
        assertTrue(repository.sendText(DEVICE_NAME, "Name"))
        callbacks.single().invoke()
        assertEquals(listOf("FTHS3-Name"), appliedNames)
        assertEquals(0, customizations)
        assertEquals(1, packets.size)
    }

    @ParameterizedTest
    @ValueSource(strings = ["000001", "INDY3-000123", "номер-😀"])
    fun `serial remains nul terminated text and read follows set completion exactly once`(text: String) {
        ParameterStoreV3.put(serialInfo, ParameterTypedValueV3.Text("previous"))
        assertEquals(V3DeviceInfoWriteResult.SENT, SetDeviceInfoTextUseCaseV3(repository)(SERIAL_NUMBER, " $text "))
        assertTextPacket(packets.single(), 11, text)
        assertEquals(ParameterTypedValueV3.Text("previous"), ParameterStoreV3.get(serialInfo))
        assertTrue(appliedNames.isEmpty())
        val onSent = callbacks.single()
        onSent(); onSent()
        assertEquals(2, packets.size)
        // Existing short DEVICE_INFORMATION / GET_SERIAL_NUMBER packet, with its original CRC.
        val read = packets.last()
        assertArrayEquals(byteArrayOf(0, 1, 10, 0), read.copyOfRange(0, 4))
        assertEquals(crc(read.copyOfRange(0, 4)), read.last().toInt() and 255)
        assertEquals(ParameterTypedValueV3.Text("previous"), ParameterStoreV3.get(serialInfo))
        assertEquals(0, customizations)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `completion cannot read from a different device or profile`(changeProfile: Boolean) {
        repository.sendText(SERIAL_NUMBER, "00001")
        repository.sendText(DEVICE_NAME, "Name")
        if (changeProfile) UiState.activeV3DeviceProfile = V3DeviceProfile.INDY3 else address = "second"
        callbacks.toList().forEach { it() }
        assertEquals(2, packets.size)
        assertEquals(0, customizations)
    }

    @Test fun `domain rejects blank and locked writes and enforces unicode limit without the adapter`() {
        val setText = SetDeviceInfoTextUseCaseV3(repository)
        assertEquals(V3DeviceInfoWriteResult.EMPTY, setText(SERIAL_NUMBER, " \n "))
        UiState.v3WidgetsInteractionEnabled.value = false
        assertEquals(V3DeviceInfoWriteResult.BLOCKED, setText(DEVICE_NAME, "Name"))
        assertTrue(packets.isEmpty())
        UiState.v3WidgetsInteractionEnabled.value = true
        assertEquals(V3DeviceInfoWriteResult.SENT, setText(DEVICE_NAME, "ПротезAB"))
        assertTextPacket(packets.single(), 13, "FTHS3-ПротезA")
    }

    private fun assertTextPacket(packet: ByteArray, command: Int, text: String) {
        val payload = byteArrayOf(command.toByte()) + text.encodeToByteArray() + byteArrayOf(0)
        assertArrayEquals(byteArrayOf(0x80.toByte(), 1, payload.size.toByte(), 0), packet.copyOfRange(0, 4))
        assertEquals(crc(packet.copyOfRange(0, 4)), packet[4].toInt() and 255)
        assertArrayEquals(payload, packet.copyOfRange(5, packet.size - 1))
        assertEquals(crc(payload), packet.last().toInt() and 255)
    }

    // Independent bitwise CRC-8/MAXIM check; does not call the production command builder/table.
    private fun crc(bytes: ByteArray): Int {
        var result = 0
        bytes.forEach { byte ->
            result = result xor (byte.toInt() and 255)
            repeat(8) { result = if (result and 1 != 0) (result ushr 1) xor 0x8C else result ushr 1 }
        }
        return result
    }
}
