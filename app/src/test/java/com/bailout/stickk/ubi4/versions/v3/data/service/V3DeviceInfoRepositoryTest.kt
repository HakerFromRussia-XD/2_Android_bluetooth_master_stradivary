package com.bailout.stickk.ubi4.versions.v3.data.service

import com.bailout.stickk.ubi4.data.state.*
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_SERIAL_NUMBER
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_SET_DEVICE_NAME
import com.bailout.stickk.ubi4.versions.v3.domain.service.*
import com.bailout.stickk.ubi4.versions.v3.domain.service.V3DeviceInfoField.*
import com.bailout.stickk.ubi4.versions.v3.data.device.V3DeviceIdentityStore
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.V3DeviceInfoWriteResult
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.SetDeviceInfoTextUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.GetDeviceInfoTextUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.EditDeviceInfoTextUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.service.usecase.V3DeviceInfoTextEdit

class V3DeviceInfoRepositoryTest {
    private val originalProfile = UiState.activeV3DeviceProfile
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val originalMode = UiState.isInterfaceV3Activated
    private val originalAddress = WidgetRepoProvider.mac()
    private val serialInfo = ParameterInfoRegistry.require(P_KEY_SET_SERIAL_NUMBER)
    private val previousConnectionName = runCatching { ConnectionState.connectedDeviceName }.getOrDefault("")
    private val identity = V3DeviceIdentityStore()
    private var serial: String?
        get() = identity.identity.value?.serial
        set(value) { identity.update(value.orEmpty(), name) }
    private var name: String?
        get() = identity.identity.value?.deviceName
        set(value) { identity.update(serial.orEmpty(), value) }
    private var connected: String? = null
    private var intent: String?
        get() = identity.intentDeviceName
        set(value) { identity.intentDeviceName = value }
    private var address = "first"
    private val packets = mutableListOf<ByteArray>()
    private val callbacks = mutableListOf<() -> Unit>()
    private var customizations = 0
    private val repository = V3DeviceInfoRepositoryImpl(
        deviceIdentity = identity,
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
        ConnectionState.connectedDeviceName = previousConnectionName
        ParameterStoreV3.clear()
        UiState.activeV3DeviceProfile = originalProfile
        UiState.v3WidgetsInteractionEnabled.value = originalInteraction
        UiState.isInterfaceV3Activated = originalMode
        WidgetRepoProvider.setCurrentMac(originalAddress)
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
        assertEquals(" ", identity.identity.value?.serial)
        assertEquals(0, customizations)
    }

    @ParameterizedTest
    @CsvSource("STANDARD_V3,FTHS3-", "INDY3,INDY3-")
    fun `name uses active transport prefix and only send completion records customization`(profile: V3DeviceProfile, prefix: String) {
        UiState.activeV3DeviceProfile = profile
        serial = prefix + "Old"
        assertEquals(V3DeviceInfoWriteResult.SENT, SetDeviceInfoTextUseCaseV3(repository)(DEVICE_NAME, " Рука "))
        assertEquals(prefix + "Рука", identity.identity.value?.deviceName)
        assertEquals(prefix + "Рука", ConnectionState.connectedDeviceName)
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
        assertEquals("FTHS3-Name", identity.identity.value?.deviceName)
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
        assertNull(identity.identity.value)
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

    @Test fun `native get reads storage on each prefill without falling back to shared identity or connection name`() {
        ConnectionState.connectedDeviceName = "FTHS3-connected"
        serial = "FTHS3-identity"
        val nameInfo = ParameterInfoRegistry.require(P_KEY_SET_DEVICE_NAME)
        ParameterStoreV3.put(nameInfo, ParameterTypedValueV3.Text("FTHS3-typed"))
        UiState.v3WidgetsInteractionEnabled.value = false
        UiState.isInterfaceV3Activated = false
        var storedName: String? = null
        var reads = 0
        val nativeRepository = V3DeviceInfoRepositoryImpl(
            readDeviceNameInput = { reads++; storedName },
            enqueuePacket = { _, _ -> fail<Unit>("Prefill must not queue a packet") },
            onDeviceNameQueued = { fail<Unit>("Prefill must not update storage") },
        )
        val getText = GetDeviceInfoTextUseCaseV3(nativeRepository)
        assertSame(UiState.v3WidgetsInteractionEnabled, nativeRepository.interactionEnabled)
        for ((stored, expected) in listOf(null to "", " \n " to "", " INDY3-storage " to "storage", "FTHS3-next" to "next", "foreign" to "foreign")) {
            storedName = stored
            assertEquals(expected, getText(DEVICE_NAME))
        }
        assertEquals(5, reads)
        assertFalse(nativeRepository.interactionEnabled.value)
        assertEquals("FTHS3-connected", ConnectionState.connectedDeviceName)
        assertEquals("FTHS3-identity", identity.identity.value?.serial)
        assertEquals(ParameterTypedValueV3.Text("FTHS3-typed"), ParameterStoreV3.get(nameInfo))
    }

    @Test fun `injected edit policy and prepared set preserve platform text without another trim or edit`() {
        val edits = mutableListOf<String>()
        val edit = EditDeviceInfoTextUseCaseV3 { text -> edits += text; text.take(10) }
        val raw = " 123456789ABC "
        val value = edit(DEVICE_NAME, raw)
        assertEquals(V3DeviceInfoTextEdit(" 123456789", true), value)
        assertEquals(V3DeviceInfoTextEdit("short ", false), edit(DEVICE_NAME, "short "))
        assertEquals(V3DeviceInfoTextEdit(raw, false), edit(SERIAL_NUMBER, raw))
        assertEquals(listOf(raw, "short "), edits)
        val sent = mutableListOf<Pair<V3DeviceInfoField, String>>()
        val recordingRepository = object : V3DeviceInfoRepository {
            override val interactionEnabled = UiState.v3WidgetsInteractionEnabled
            override fun getTextForInput(field: V3DeviceInfoField): String? = null
            override fun sendText(field: V3DeviceInfoField, text: String): Boolean {
                sent += field to text
                return true
            }
        }
        val nativeSet = SetDeviceInfoTextUseCaseV3(recordingRepository, requireInteractionEnabled = false)
        UiState.v3WidgetsInteractionEnabled.value = false
        val defaultSet = SetDeviceInfoTextUseCaseV3(recordingRepository)
        assertEquals(V3DeviceInfoWriteResult.BLOCKED, defaultSet(DEVICE_NAME, value))
        assertEquals(V3DeviceInfoWriteResult.BLOCKED, defaultSet(DEVICE_NAME, V3DeviceInfoTextEdit("", false)))
        assertEquals(V3DeviceInfoWriteResult.SENT, nativeSet(DEVICE_NAME, value))
        val platformPrepared = "  ABCDEFGHIJKLMN  "
        assertEquals(V3DeviceInfoWriteResult.SENT, nativeSet(DEVICE_NAME, V3DeviceInfoTextEdit(platformPrepared, false)))
        assertEquals(V3DeviceInfoWriteResult.EMPTY, nativeSet(DEVICE_NAME, V3DeviceInfoTextEdit("", true)))
        assertEquals(listOf(DEVICE_NAME to value.text, DEVICE_NAME to platformPrepared), sent)
        assertEquals(listOf(raw, "short "), edits)
        assertFalse(recordingRepository.interactionEnabled.value)
        UiState.v3WidgetsInteractionEnabled.value = true
        assertEquals(V3DeviceInfoWriteResult.SENT, defaultSet(DEVICE_NAME, "ABCDEFGHIJKLMN"))
        assertEquals(DEVICE_NAME to "ABCDEFGHIJKLM", sent.last())
    }

    @ParameterizedTest
    @CsvSource("STANDARD_V3,FTHS3-", "INDY3,INDY3-")
    fun `native prepared name queues offline repeatedly and publishes the packet prefix once before any completion`(profile: V3DeviceProfile, prefix: String) {
        UiState.v3WidgetsInteractionEnabled.value = false
        UiState.isInterfaceV3Activated = false
        WidgetRepoProvider.setCurrentMac("")
        ConnectionState.connectedDeviceName = "unchanged-connected-name"
        val nameInfo = ParameterInfoRegistry.require(P_KEY_SET_DEVICE_NAME)
        ParameterStoreV3.put(nameInfo, ParameterTypedValueV3.Text("unchanged-typed-name"))
        val otherProfile = if (profile == V3DeviceProfile.INDY3) V3DeviceProfile.STANDARD_V3 else V3DeviceProfile.INDY3
        val otherPrefix = if (profile == V3DeviceProfile.INDY3) "FTHS3-" else "INDY3-"
        var storedName: String? = "FTHS3-old"
        var reads = 0
        val events = mutableListOf<String>()
        val publishedNames = mutableListOf<String>()
        val nativeRepository = V3DeviceInfoRepositoryImpl(
            readDeviceNameInput = { reads++; storedName },
            enqueuePacket = { packet, callback ->
                packets += packet
                callbacks += callback
                events += "enqueue"
                // Prefix evaluation after enqueue would disagree with the already prepared packet.
                UiState.activeV3DeviceProfile = if (UiState.activeV3DeviceProfile == V3DeviceProfile.INDY3) V3DeviceProfile.STANDARD_V3 else V3DeviceProfile.INDY3
            },
            onDeviceNameQueued = { transportText ->
                storedName = transportText
                events += "save:$transportText"
                publishedNames += transportText
                events += "notify:$transportText"
            },
        )
        val edit = EditDeviceInfoTextUseCaseV3 { it.take(10) }
        val set = SetDeviceInfoTextUseCaseV3(nativeRepository, requireInteractionEnabled = false)
        val prepared = edit(DEVICE_NAME, "ABCDEFGHIJK")
        assertEquals(V3DeviceInfoTextEdit("ABCDEFGHIJ", true), prepared)
        val expectedNames = mutableListOf<String>()
        for ((nextProfile, nextPrefix) in listOf(profile to prefix, profile to prefix, otherProfile to otherPrefix)) {
            UiState.activeV3DeviceProfile = nextProfile
            val expectedName = nextPrefix + prepared.text
            assertEquals(V3DeviceInfoWriteResult.SENT, set(DEVICE_NAME, prepared))
            expectedNames += expectedName
            assertTextPacket(packets.last(), 13, expectedName)
            assertEquals(expectedName, storedName)
            WidgetRepoProvider.setCurrentMac("changed-device")
        }
        assertEquals(expectedNames, publishedNames)
        assertEquals(expectedNames.flatMap { listOf("enqueue", "save:$it", "notify:$it") }, events)
        assertEquals(0, reads)
        val eventsBeforeCompletion = events.toList()
        callbacks.toList().forEach { it(); it() }
        assertEquals(eventsBeforeCompletion, events)
        assertEquals(3, packets.size)
        assertEquals(expectedNames, publishedNames)
        assertEquals("unchanged-connected-name", ConnectionState.connectedDeviceName)
        assertEquals(ParameterTypedValueV3.Text("unchanged-typed-name"), ParameterStoreV3.get(nameInfo))
        assertNull(identity.identity.value)
        assertSame(UiState.v3WidgetsInteractionEnabled, nativeRepository.interactionEnabled)
        assertFalse(nativeRepository.interactionEnabled.value)
        assertEquals(0, customizations)
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
