package com.bailout.stickk.ubi4.versions.v3.data.gestures

import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.*
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.models.ble.RotationGroupV3
import com.bailout.stickk.ubi4.models.ble.CurrentGestureV3
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_GROUPE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_CURRENT_GESTURE
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
class V3GesturesRepositoryTest {
    private val originalMac = WidgetRepoProvider.mac()
    private val originalMode = UiState.isInterfaceV3Activated
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val originalProfile = UiState.activeV3DeviceProfile
    private val originalDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
    private val info = ParameterInfoRegistry.require(P_KEY_CURRENT_GESTURE)
    private val cache = BaseParameterInfoStruct(ID = info.parameterID, dataCode = info.dataCode, data = "{\"currentGesture\":4}")
    private val packets = mutableListOf<ByteArray>()
    private val saved = mutableListOf<ParameterTypedValueV3>()
    private val repository = V3GesturesRepositoryImpl(packets::add) { key, value ->
        assertEquals(if (value is ParameterTypedValueV3.RotationGroup) ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE) else info, key)
        assertEquals(value, ParameterStoreV3.get(key))
        saved.add(value)
    }
    private val select = SelectGestureUseCaseV3(repository)
    private val request = RequestActiveGestureUseCaseV3(repository)

    @BeforeEach fun setUp() {
        WidgetRepoProvider.setCurrentMac("device")
        UiState.isInterfaceV3Activated = true
        UiState.v3WidgetsInteractionEnabled.value = true
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf(BaseSubDeviceInfoStruct(
            deviceAddress = info.deviceAddress, parametersList = arrayListOf(cache)))
        ParameterStoreV3.clear()
    }
    @AfterEach fun tearDown() {
        WidgetRepoProvider.setCurrentMac(originalMac)
        UiState.isInterfaceV3Activated = originalMode
        UiState.v3WidgetsInteractionEnabled.value = originalInteraction
        UiState.activeV3DeviceProfile = originalProfile
        GlobalParameters.baseSubDevicesInfoStructSetV3 = originalDevices
        ParameterStoreV3.clear()
    }

    @ParameterizedTest @EnumSource(value = V3DeviceProfile::class, names = ["STANDARD_V3", "INDY3"])
    fun `both profiles preserve GET and SET packets and optimistic persistence`(profile: V3DeviceProfile) {
        UiState.activeV3DeviceProfile = profile
        assertEquals(4, repository.getActiveGesture().gestureId)
        assertTrue(request("device"))
        assertPacket(packets.single(), listOf(0, 15, 36, 0))
        assertTrue(saved.isEmpty())
        assertTrue(select("device", 77))
        assertPacket(packets.last(), listOf(0, 15, 37, 77))
        val typed = ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(77))
        assertEquals(typed, ParameterStoreV3.get(info))
        assertEquals(listOf(typed), saved)
        assertEquals("{\"currentGesture\":77}", cache.data)
        assertEquals(77, repository.getActiveGesture().gestureId)
    }

    @Test fun `all protocol gesture IDs including hidden factory gesture are selectable`() {
        val ids = (1..15).toList() + (64..77).toList()
        ids.forEach { assertTrue(select("device", it)) }
        assertEquals(ids, packets.map { it[3].toInt() and 255 })
    }

    @Test fun `cache and incoming store observations never persist or send`() = runTest {
        val seen = mutableListOf<Int?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.updates.collect { seen.add(repository.getActiveGesture().gestureId) }
        }
        runCurrent()
        ParameterStoreV3.put(info, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(64)))
        runCurrent()
        assertTrue(4 in seen)
        assertEquals(64, seen.last())
        assertTrue(packets.isEmpty()); assertTrue(saved.isEmpty())
    }

    @ParameterizedTest @ValueSource(strings = ["", " ", "null", "other"])
    fun `different or invalid device rejects reads and writes`(address: String) {
        assertFalse(request(address)); assertFalse(select(address, 2))
        assertTrue(packets.isEmpty()); assertTrue(saved.isEmpty())
    }

    @Test fun `missing current device and UBI4 and lock reject direct repository writes too`() {
        WidgetRepoProvider.setCurrentMac("")
        assertFalse(repository.selectGesture("", 2)); assertFalse(repository.requestActiveGesture(""))
        WidgetRepoProvider.setCurrentMac("device")
        UiState.isInterfaceV3Activated = false
        assertFalse(repository.selectGesture("device", 2)); assertFalse(repository.requestActiveGesture("device"))
        UiState.isInterfaceV3Activated = true
        UiState.v3WidgetsInteractionEnabled.value = false
        assertFalse(repository.selectGesture("device", 2)); assertFalse(repository.requestActiveGesture("device"))
        assertEquals(4, repository.getActiveGesture().gestureId)
        assertTrue(packets.isEmpty()); assertTrue(saved.isEmpty())
    }

    @Test fun `absent gesture parameter blocks requests and selections despite a stale typed value`() {
        ParameterStoreV3.put(info, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(64)))
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf()
        assertFalse(repository.getActiveGesture().isInteractionEnabled)
        assertFalse(request("device")); assertFalse(select("device", 2))
        assertTrue(packets.isEmpty()); assertTrue(saved.isEmpty())
    }

    @Test fun `malformed cache has unknown current gesture without writes`() {
        cache.data = "invalid"
        assertNull(repository.getActiveGesture().gestureId)
        assertTrue(packets.isEmpty()); assertTrue(saved.isEmpty())
    }

    @Test fun `rotation group reads ordered IDs from cache and store without persisting`() {
        addRotationParameter("""{"gesture1Id":5,"gesture2Id":0,"gesture3Id":2,"gesture4Id":5}""")
        assertEquals(listOf(5, 2, 5), repository.getRotationGroupGestureIds())
        ParameterStoreV3.put(ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE),
            ParameterTypedValueV3.RotationGroup(RotationGroupV3(gesture1Id = 77)))
        assertEquals(listOf(77), repository.getRotationGroupGestureIds())
        assertTrue(packets.isEmpty()); assertTrue(saved.isEmpty())
    }

    @Test fun `rotation GET preserves existing bytes and CRC without a SET`() {
        addRotationParameter()
        assertTrue(repository.requestRotationGroup("device"))
        assertPacket(packets.single(), listOf(0, 15, 53, 0))
        assertTrue(saved.isEmpty())
    }

    @Test fun `absent rotation parameter ignores stale store and blocks GET`() {
        ParameterStoreV3.put(ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE),
            ParameterTypedValueV3.RotationGroup(RotationGroupV3(gesture1Id = 1)))
        assertNull(repository.getRotationGroupGestureIds())
        assertFalse(repository.requestRotationGroup("device"))
        assertTrue(packets.isEmpty())
    }

    @Test fun `rotation GET checks device lock and UBI4 at send time`() {
        addRotationParameter()
        assertFalse(repository.requestRotationGroup("other"))
        UiState.v3WidgetsInteractionEnabled.value = false
        assertFalse(repository.requestRotationGroup("device"))
        UiState.v3WidgetsInteractionEnabled.value = true
        UiState.isInterfaceV3Activated = false
        assertFalse(repository.requestRotationGroup("device"))
        assertTrue(packets.isEmpty())
    }

    @Test fun `identical rotation responses are observed while unrelated updates are ignored`() = runTest {
        addRotationParameter()
        var responses = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.rotationGroupUpdates.collect { responses++ }
        }
        val rotationInfo = ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE)
        val value = ParameterTypedValueV3.RotationGroup(RotationGroupV3(gesture1Id = 1))
        repeat(2) { ParameterStoreV3.put(rotationInfo, value); runCurrent() }
        ParameterStoreV3.put(info, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(4)))
        runCurrent()
        assertEquals(2, responses)
        assertTrue(packets.isEmpty()); assertTrue(saved.isEmpty())
    }

    @Test fun `empty rotation group differs from malformed cache`() {
        val rotationCache = addRotationParameter("invalid")
        assertNull(repository.getRotationGroupGestureIds())
        rotationCache.data = "{}"
        assertEquals(emptyList<Int>(), repository.getRotationGroupGestureIds())
    }

    @ParameterizedTest @ValueSource(ints = [0, 1, 2])
    fun `removal preserves ordered duplicate slots persistence and exact packet`(position: Int) {
        val rotationCache = addRotationParameter("""{"gesture1Id":4,"gesture2Id":64,"gesture3Id":4}""")
        val original = listOf(4, 64, 4)
        val expected = original.filterIndexed { index, _ -> index != position }
        assertTrue(RemoveGestureFromRotationGroupUseCaseV3(repository)("device", position, original))
        assertEquals(expected, repository.getRotationGroupGestureIds())
        assertEquals(1, saved.size)
        assertEquals(saved.single(), ParameterStoreV3.get(ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE)))
        ParameterStoreV3.clear()
        assertEquals(expected, repository.getRotationGroupGestureIds(), "Serialized cache must match the new order")
        assertTrue(rotationCache.data.contains("gesture1Id"))
        assertRotationPacket(packets.single(), expected)
    }

    @Test fun `removing last slot persists empty group and writes eight zero pairs`() {
        addRotationParameter("""{"gesture1Id":77}""")
        assertTrue(RemoveGestureFromRotationGroupUseCaseV3(repository)("device", 0, listOf(77)))
        assertEquals(emptyList<Int>(), repository.getRotationGroupGestureIds())
        assertEquals(listOf(ParameterTypedValueV3.RotationGroup(RotationGroupV3())), saved)
        assertRotationPacket(packets.single(), emptyList())
    }

    @Test fun `full group removal retains all seven remaining positions`() {
        addRotationParameter()
        val original = (1..8).toList()
        assertTrue(repository.setRotationGroup("device", original))
        assertRotationPacket(packets.single(), original)
        packets.clear(); saved.clear()
        assertTrue(RemoveGestureFromRotationGroupUseCaseV3(repository)("device", 3, original))
        val expected = listOf(1, 2, 3, 5, 6, 7, 8)
        assertEquals(expected, repository.getRotationGroupGestureIds())
        assertRotationPacket(packets.single(), expected)
        assertEquals(1, saved.size)
    }

    @Test fun `invalid position stale group and missing or malformed parameter never write`() {
        val remove = RemoveGestureFromRotationGroupUseCaseV3(repository)
        assertFalse(remove("device", 0, listOf(4)))
        val rotationCache = addRotationParameter("invalid")
        assertFalse(remove("device", 0, listOf(4)))
        rotationCache.data = """{"gesture1Id":4,"gesture2Id":64}"""
        assertFalse(remove("device", -1, listOf(4, 64)))
        assertFalse(remove("device", 2, listOf(4, 64)))
        assertFalse(remove("device", 0, listOf(64, 4)))
        assertTrue(packets.isEmpty()); assertTrue(saved.isEmpty())
    }

    @Test fun `rotation writes guard device lock UBI4 missing parameter and invalid payload`() {
        assertFalse(repository.setRotationGroup("device", listOf(4)))
        addRotationParameter()
        assertFalse(repository.setRotationGroup("other", listOf(4)))
        UiState.v3WidgetsInteractionEnabled.value = false
        assertFalse(repository.setRotationGroup("device", listOf(4)))
        UiState.v3WidgetsInteractionEnabled.value = true
        UiState.isInterfaceV3Activated = false
        assertFalse(repository.setRotationGroup("device", listOf(4)))
        UiState.isInterfaceV3Activated = true
        listOf(List(9) { 1 }, listOf(0), listOf(-1), listOf(256)).forEach {
            assertFalse(repository.setRotationGroup("device", it))
        }
        assertTrue(packets.isEmpty()); assertTrue(saved.isEmpty())
    }

    @Test fun `selection saves checked IDs with retained duplicates and new IDs in collection order`() {
        addRotationParameter("""{"gesture1Id":64,"gesture2Id":4,"gesture3Id":64}""")
        val selection = requireNotNull(GetRotationGroupSelectionUseCaseV3(repository)())
        assertEquals(setOf(64, 4), selection.selectedGestureIds)
        assertEquals((1..11).toList() + (13..15).toList() + (64..77).toList(), selection.availableGestureIds)
        val edit = EditRotationGroupSelectionUseCaseV3()
        val updated = listOf(4, 77, 2, 1).fold(selection) { draft, id -> edit(draft, id).selection }
        assertTrue(SaveRotationGroupSelectionUseCaseV3(repository)("device", updated))
        val expected = listOf(64, 64, 1, 2, 77)
        assertEquals(expected, repository.getRotationGroupGestureIds())
        assertEquals(1, saved.size)
        assertRotationPacket(packets.single(), expected)
        ParameterStoreV3.clear()
        assertEquals(expected, repository.getRotationGroupGestureIds())
        assertFalse(SaveRotationGroupSelectionUseCaseV3(repository)("device", updated), "Old snapshot cannot save twice")
        assertEquals(1, packets.size)
    }

    @Test fun `selection may clear the entire group or populate an empty group`() {
        addRotationParameter("""{"gesture1Id":4,"gesture2Id":4}""")
        val get = GetRotationGroupSelectionUseCaseV3(repository)
        val save = SaveRotationGroupSelectionUseCaseV3(repository)
        assertTrue(save("device", requireNotNull(get()).copy(selectedGestureIds = emptySet())))
        assertRotationPacket(packets.single(), emptyList())
        assertTrue(save("device", requireNotNull(get()).copy(selectedGestureIds = setOf(77, 1))))
        assertRotationPacket(packets.last(), listOf(1, 77))
        assertEquals(listOf(1, 77), repository.getRotationGroupGestureIds())
    }

    @Test fun `selection preserves checkbox limit and existing eight slot serialization with duplicates`() {
        addRotationParameter("""{"gesture1Id":4,"gesture2Id":4}""")
        val original = requireNotNull(GetRotationGroupSelectionUseCaseV3(repository)())
        val save = SaveRotationGroupSelectionUseCaseV3(repository)
        listOf(setOf(0), setOf(12), setOf(16), setOf(78), setOf(256), (1..9).toSet()).forEach {
            assertFalse(save("device", original.copy(selectedGestureIds = it)))
        }
        val edit = EditRotationGroupSelectionUseCaseV3()
        assertEquals(original, edit(original, 12).selection)
        assertFalse(edit(original, 12).isLimitReached)
        val sevenChecks = original.copy(selectedGestureIds = (1..7).toSet())
        val full = edit(sevenChecks, 8)
        assertFalse(full.isLimitReached)
        assertEquals(8, full.selection.selectedGestureIds.size)
        assertTrue(edit(full.selection, 9).isLimitReached)
        assertEquals(full.selection, edit(full.selection, 9).selection)
        assertTrue(packets.isEmpty()); assertTrue(saved.isEmpty())
        assertTrue(save("device", full.selection))
        assertEquals(listOf(4, 4, 1, 2, 3, 5, 6, 7), repository.getRotationGroupGestureIds())
        assertRotationPacket(packets.single(), listOf(4, 4, 1, 2, 3, 5, 6, 7))
    }

    @Test fun `unchanged selection still writes and invalid context prevents subsequent saves`() {
        val get = GetRotationGroupSelectionUseCaseV3(repository)
        val save = SaveRotationGroupSelectionUseCaseV3(repository)
        assertNull(get())
        val rotationCache = addRotationParameter("invalid")
        assertNull(get())
        rotationCache.data = "{}"
        val original = requireNotNull(get())
        assertTrue(save("device", original))
        assertRotationPacket(packets.single(), emptyList())
        assertEquals(1, saved.size)
        packets.clear(); saved.clear()
        val draft = original.copy(selectedGestureIds = setOf(64))
        assertFalse(save("other", draft))
        UiState.v3WidgetsInteractionEnabled.value = false
        assertFalse(save("device", draft))
        UiState.v3WidgetsInteractionEnabled.value = true
        UiState.isInterfaceV3Activated = false
        assertFalse(save("device", draft))
        UiState.isInterfaceV3Activated = true
        ParameterStoreV3.put(ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE),
            ParameterTypedValueV3.RotationGroup(RotationGroupV3(gesture1Id = 4)))
        assertFalse(save("device", draft))
        assertTrue(packets.isEmpty()); assertTrue(saved.isEmpty())
    }

    @Test fun `drag order moves one position and preserves duplicate IDs and persistence`() {
        addRotationParameter()
        val move = MoveGestureInRotationGroupUseCaseV3(repository)
        val original = listOf(4, 64, 4, 77)
        val cases = listOf(
            Triple(0, 3, listOf(64, 4, 77, 4)),
            Triple(3, 0, listOf(77, 4, 64, 4)),
            Triple(1, 2, listOf(4, 4, 64, 77)),
            Triple(2, 1, listOf(4, 4, 64, 77)),
        )
        cases.forEach { (from, to, expected) ->
            assertTrue(repository.setRotationGroup("device", original))
            packets.clear(); saved.clear()
            assertTrue(move("device", from, to, original))
            assertEquals(expected, repository.getRotationGroupGestureIds())
            assertEquals(1, saved.size)
            assertRotationPacket(packets.single(), expected)
            ParameterStoreV3.clear()
            assertEquals(expected, repository.getRotationGroupGestureIds(), "Serialized cache retains the same order")
        }
    }

    @Test fun `moving equal gestures between different positions still persists and sends once`() {
        addRotationParameter("""{"gesture1Id":4,"gesture2Id":4,"gesture3Id":64}""")
        val original = listOf(4, 4, 64)
        val move = MoveGestureInRotationGroupUseCaseV3(repository)
        assertFalse(move("device", 0, 0, original))
        assertTrue(packets.isEmpty()); assertTrue(saved.isEmpty())
        assertTrue(move("device", 0, 1, original))
        assertEquals(original, repository.getRotationGroupGestureIds())
        assertEquals(1, saved.size)
        assertRotationPacket(packets.single(), original)
    }

    @Test fun `full group drag preserves all eight slots and original packet format`() {
        addRotationParameter()
        val original = (1..8).toList()
        repository.setRotationGroup("device", original)
        packets.clear(); saved.clear()
        assertTrue(MoveGestureInRotationGroupUseCaseV3(repository)("device", 7, 0, original))
        val expected = listOf(8, 1, 2, 3, 4, 5, 6, 7)
        assertEquals(expected, repository.getRotationGroupGestureIds())
        assertRotationPacket(packets.single(), expected)
        assertEquals(1, saved.size)
    }

    @Test fun `stale invalid or unavailable drag cannot write another group`() {
        val move = MoveGestureInRotationGroupUseCaseV3(repository)
        assertFalse(move("device", 0, 1, listOf(4, 64)))
        val rotationCache = addRotationParameter("invalid")
        assertFalse(move("device", 0, 1, listOf(4, 64)))
        rotationCache.data = """{"gesture1Id":4,"gesture2Id":64}"""
        val original = listOf(4, 64)
        listOf(-1 to 0, 2 to 0, 0 to -1, 0 to 2).forEach { (from, to) ->
            assertFalse(move("device", from, to, original))
        }
        assertFalse(move("device", 0, 1, listOf(64, 4)))
        assertFalse(move("other", 0, 1, original))
        UiState.v3WidgetsInteractionEnabled.value = false
        assertFalse(move("device", 0, 1, original))
        UiState.v3WidgetsInteractionEnabled.value = true
        UiState.isInterfaceV3Activated = false
        assertFalse(move("device", 0, 1, original))
        assertTrue(packets.isEmpty()); assertTrue(saved.isEmpty())
    }

    private fun assertRotationPacket(packet: ByteArray, ids: List<Int>) {
        assertEquals(23, packet.size)
        assertPacket(packet.copyOfRange(0, 5), listOf(128, 15, 17, 0))
        val slots = ids + List(8 - ids.size) { 0 }
        assertPacket(packet.copyOfRange(5, 23), listOf(54) + slots.flatMap { listOf(it, it) })
    }

    private fun addRotationParameter(data: String = "{}"): BaseParameterInfoStruct {
        val rotationInfo = ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE)
        return BaseParameterInfoStruct(ID = rotationInfo.parameterID, dataCode = rotationInfo.dataCode, data = data).also {
            GlobalParameters.baseSubDevicesInfoStructSetV3.first().parametersList.add(it)
        }
    }

    private fun assertPacket(packet: ByteArray, header: List<Int>) {
        var crc = 0
        header.forEach { byte ->
            crc = crc xor byte
            repeat(8) { crc = if (crc and 1 != 0) (crc ushr 1) xor 0x8C else crc ushr 1 }
        }
        assertArrayEquals((header + crc).map(Int::toByte).toByteArray(), packet)
    }
}
