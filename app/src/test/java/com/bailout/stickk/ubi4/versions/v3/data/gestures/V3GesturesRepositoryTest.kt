package com.bailout.stickk.ubi4.versions.v3.data.gestures

import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.local.repository.WidgetRepoProvider
import com.bailout.stickk.ubi4.data.state.*
import com.bailout.stickk.ubi4.data.subdevices.BaseSubDeviceInfoStruct
import com.bailout.stickk.ubi4.models.ble.RotationGroupV3
import com.bailout.stickk.ubi4.models.ble.CurrentGestureV3
import com.bailout.stickk.ubi4.models.ble.ParameterRef
import com.bailout.stickk.ubi4.models.ble.SliderV3
import com.bailout.stickk.ubi4.models.device.V3DeviceProfile
import com.bailout.stickk.ubi4.models.commonModels.ParameterInfo
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.ParameterInfoRegistry
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_GESTURE_GROUPE
import com.bailout.stickk.ubi4.utility.ConstantManagerUBI4.Companion.P_KEY_CURRENT_GESTURE
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.EditRotationGroupSelectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.GetRotationGroupSelectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.GetActiveGestureUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.ObserveGesturesChangesUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.MoveGestureInRotationGroupUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.RemoveGestureFromRotationGroupUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.RequestActiveGestureUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.RequestRotationGroupUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.SaveRotationGroupSelectionUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.SelectGestureUseCaseV3
import com.bailout.stickk.ubi4.versions.v3.domain.gestures.usecase.SetRotationGroupUseCaseV3

@OptIn(ExperimentalCoroutinesApi::class)
class V3GesturesRepositoryTest {
    private val originalMac = WidgetRepoProvider.mac()
    private val originalMode = UiState.isInterfaceV3Activated
    private val originalInteraction = UiState.v3WidgetsInteractionEnabled.value
    private val originalProfile = UiState.activeV3DeviceProfile
    private val originalDevices = GlobalParameters.baseSubDevicesInfoStructSetV3
    private val originalValues = ParameterStoreV3.values.value
    private val info = ParameterInfoRegistry.require(P_KEY_CURRENT_GESTURE)
    private val cache = BaseParameterInfoStruct(ID = info.parameterID, dataCode = info.dataCode, data = "{\"currentGesture\":4}")
    private val packets = mutableListOf<ByteArray>()
    private val saved = mutableListOf<ParameterTypedValueV3>()
    private val repository = V3GesturesRepositoryImpl(packets::add, saveBleValue = { key, value ->
        assertEquals(if (value is ParameterTypedValueV3.RotationGroup) ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE) else info, key)
        assertEquals(value, ParameterStoreV3.get(key))
        saved.add(value)
    })
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
        originalValues.forEach { (key, value) ->
            ParameterStoreV3.put(ParameterInfo(key.parameterID, key.dataCode, key.deviceAddress, 0), value)
        }
    }

    @Test fun `native active cache uses exact raw snapshot target and mandatory field without serialized provider fallback`() {
        val native = nativeSnapshotRepository()
        val get = GetActiveGestureUseCaseV3(native)
        assertEquals(4, repository.getActiveGesture().gestureId)
        assertEquals(repository.getActiveGesture().copy(gestureId = null), get())
        val ignoredTarget = V3GesturesRepositoryImpl(packets::add, activeGestureTarget = ParameterRef(91, 92, 93))
        assertEquals(repository.getActiveGesture(), GetActiveGestureUseCaseV3(ignoredTarget)())

        ParameterStoreV3.put(info, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(64)))
        assertEquals(64, get().gestureId)
        listOf(
            null,
            ParameterRef(info.deviceAddress + 1, info.parameterID, info.dataCode),
            ParameterRef(info.deviceAddress, info.parameterID + 1, info.dataCode),
            ParameterRef(info.deviceAddress, info.parameterID, 0x24),
        ).forEach { target -> assertNull(GetActiveGestureUseCaseV3(nativeSnapshotRepository(target))().gestureId) }
        // A GET-code key is not substituted with SET-code storage, even when it contains a typed value.
        ParameterStoreV3.put(ParameterInfo(info.parameterID, 0x24, info.deviceAddress, 0),
            ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(77)))
        assertNull(GetActiveGestureUseCaseV3(nativeSnapshotRepository(ParameterRef(info.deviceAddress, info.parameterID, 0x24)))().gestureId)
        assertEquals(64, get().gestureId)

        ParameterStoreV3.put(info, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(0)))
        assertNull(get().gestureId)
        assertEquals(0, repository.getActiveGesture().gestureId)
        ParameterStoreV3.put(info, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(Int.MIN_VALUE)))
        assertEquals(Int.MIN_VALUE, get().gestureId)
        UiState.v3WidgetsInteractionEnabled.value = false
        WidgetRepoProvider.setCurrentMac("")
        assertEquals(V3ActiveGesture("", Int.MIN_VALUE, false), get())
        ParameterStoreV3.put(info, ParameterTypedValueV3.Slider(SliderV3(sliderValue = 4)))
        assertNull(get().gestureId)
        ParameterStoreV3.clear()
        assertNull(get().gestureId)
        assertEquals(4, repository.getActiveGesture().gestureId)
        assertEquals("{\"currentGesture\":4}", cache.data)
        assertTrue(ParameterStoreV3.values.value.isEmpty())
        assertTrue(packets.isEmpty())
        assertTrue(saved.isEmpty())
    }

    @Test fun `native active callback preserves raw repeated events filters nil and cancels without initial or replay or getter reread`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val native = nativeSnapshotRepository()
        val sourceWithoutGetter = object : V3GesturesRepository by native {
            override fun getActiveGesture(): V3ActiveGesture = error("RX must not reread the current getter")
        }
        val observe = ObserveGesturesChangesUseCaseV3(sourceWithoutGetter)
        val callbackIds = mutableListOf<Int>()
        val rawIds = mutableListOf<Int?>()
        val unboundIds = mutableListOf<Int>()
        val jobs = mutableListOf<Job>()
        try {
            ParameterStoreV3.put(info, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(64)))
            val first = observe.observeActiveGesture { callbackIds += it }.also { jobs += it }
            jobs += launch { native.observeActiveGesture().collect { rawIds += it } }
            jobs += ObserveGesturesChangesUseCaseV3(nativeSnapshotRepository(null)).observeActiveGesture { unboundIds += it }
            runCurrent()
            assertTrue(callbackIds.isEmpty())
            assertTrue(rawIds.isEmpty())
            assertEquals(64, GetActiveGestureUseCaseV3(native)().gestureId)
            UiState.v3WidgetsInteractionEnabled.value = false
            UiState.isInterfaceV3Activated = false
            runCurrent()
            assertTrue(callbackIds.isEmpty())
            repeat(2) {
                ParameterStoreV3.put(info, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(65)))
                runCurrent()
            }
            ParameterStoreV3.put(info, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(0)))
            runCurrent()
            ParameterStoreV3.put(info, ParameterTypedValueV3.Slider(SliderV3(sliderValue = 4)))
            runCurrent()
            assertEquals(listOf(65, 65), callbackIds)
            assertEquals(listOf(65, 65, null, null), rawIds)
            listOf(
                ParameterInfo(info.parameterID, 0x24, info.deviceAddress, 0),
                ParameterInfo(info.parameterID, info.dataCode, info.deviceAddress + 1, 0),
                ParameterInfo(info.parameterID + 1, info.dataCode, info.deviceAddress, 0),
            ).forEach { other ->
                ParameterStoreV3.put(other, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(77)))
                runCurrent()
            }
            assertEquals(4, rawIds.size)
            ParameterStoreV3.put(info, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(Int.MIN_VALUE)))
            runCurrent()
            assertEquals(listOf(65, 65, Int.MIN_VALUE), callbackIds)
            first.cancel()
            runCurrent()
            assertTrue(first.isCancelled)
            ParameterStoreV3.put(info, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(66)))
            runCurrent()
            assertEquals(3, callbackIds.size)
            jobs += observe.observeActiveGesture { callbackIds += it }
            runCurrent()
            assertEquals(3, callbackIds.size)
            ParameterStoreV3.put(info, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(67)))
            runCurrent()
            assertEquals(listOf(65, 65, Int.MIN_VALUE, 67), callbackIds)
            assertEquals(listOf(65, 65, null, null, Int.MIN_VALUE, 66, 67), rawIds)
            assertTrue(unboundIds.isEmpty())
            assertTrue(packets.isEmpty())
            assertTrue(saved.isEmpty())
            assertEquals("{\"currentGesture\":4}", cache.data)
        } finally {
            jobs.forEach { it.cancel() }
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test fun `native snapshot opt in keeps broad Android Unit updates while raw equal responses remain independent`() = runTest {
        val native = nativeSnapshotRepository()
        var unitUpdates = 0
        val nativeIds = mutableListOf<Int?>()
        val defaultIds = mutableListOf<Int?>()
        val jobs = listOf(
            launch { ObserveGesturesChangesUseCaseV3(native)().collect { unitUpdates++ } },
            launch { native.observeActiveGesture().collect { nativeIds += it } },
            launch { repository.observeActiveGesture().collect { defaultIds += it } },
        )
        try {
            runCurrent()
            assertTrue(unitUpdates > 0)
            assertTrue(nativeIds.isEmpty())
            assertTrue(4 in defaultIds)
            ParameterStoreV3.put(info, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(64)))
            runCurrent()
            val afterResponse = unitUpdates
            assertEquals(listOf(64), nativeIds)
            assertEquals(64, defaultIds.last())
            ParameterStoreV3.put(info, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(64)))
            runCurrent()
            assertEquals(afterResponse, unitUpdates)
            assertEquals(listOf(64, 64), nativeIds)
            UiState.v3WidgetsInteractionEnabled.value = false
            runCurrent()
            assertTrue(unitUpdates > afterResponse)
            assertEquals(listOf(64, 64), nativeIds)
            assertTrue(packets.isEmpty())
            assertTrue(saved.isEmpty())
        } finally {
            jobs.forEach { it.cancel() }
            runCurrent()
        }
    }

    @Test fun `native rotation callback preserves raw slots repeats and empty clears without initial replay or getter reread`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val rotationCache = addRotationParameter("{\"gesture1Id\":64}")
        val rotationInfo = ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE)
        val native = nativeRotationSnapshotRepository()
        val sourceWithoutGetters = object : V3GesturesRepository by native {
            override fun getRotationGroupGestureIds(): List<Int>? = error("RX must not reread the rotation getter")
            override fun getActiveGesture(): V3ActiveGesture = error("RX must not reread device context")
        }
        val observe = ObserveGesturesChangesUseCaseV3(sourceWithoutGetters)
        val groups = mutableListOf<List<Int>>()
        val ignoredGroups = mutableListOf<List<Int>>()
        val jobs = mutableListOf<Job>()
        try {
            ParameterStoreV3.put(rotationInfo, ParameterTypedValueV3.RotationGroup(RotationGroupV3(gesture1Id = 5)))
            val first = observe.observeRotationGroup { groups += it }.also { jobs += it }
            jobs += ObserveGesturesChangesUseCaseV3(nativeRotationSnapshotRepository(null))
                .observeRotationGroup { ignoredGroups += it }
            jobs += ObserveGesturesChangesUseCaseV3(nativeRotationSnapshotRepository(
                ParameterRef(rotationInfo.deviceAddress, rotationInfo.parameterID, 0x35)))
                .observeRotationGroup { ignoredGroups += it }
            runCurrent()
            assertTrue(groups.isEmpty())
            UiState.v3WidgetsInteractionEnabled.value = false
            UiState.isInterfaceV3Activated = false
            WidgetRepoProvider.setCurrentMac("")
            runCurrent()
            assertTrue(groups.isEmpty())
            // Zero slots are omitted by actual serialization; images do not change ordered raw IDs.
            val value = ParameterTypedValueV3.RotationGroup(RotationGroupV3(
                gesture1Id = 4, gesture1ImageId = 77, gesture3Id = 64, gesture4Id = 4,
                gesture5Id = 12, gesture6Id = 18, gesture8Id = Int.MIN_VALUE,
            ))
            repeat(2) { ParameterStoreV3.put(rotationInfo, value); runCurrent() }
            val ids = listOf(4, 64, 4, 12, 18, Int.MIN_VALUE)
            assertEquals(listOf(ids, ids), groups)
            listOf(
                ParameterInfo(rotationInfo.parameterID, 0x35, rotationInfo.deviceAddress, 0),
                ParameterInfo(rotationInfo.parameterID, rotationInfo.dataCode, rotationInfo.deviceAddress + 1, 0),
                ParameterInfo(rotationInfo.parameterID + 1, rotationInfo.dataCode, rotationInfo.deviceAddress, 0),
                info,
            ).forEach { other ->
                ParameterStoreV3.put(other, ParameterTypedValueV3.RotationGroup(RotationGroupV3(gesture1Id = 77)))
                runCurrent()
            }
            assertEquals(listOf(ids, ids), groups)
            assertTrue(ignoredGroups.isEmpty())
            ParameterStoreV3.put(rotationInfo, ParameterTypedValueV3.Slider(SliderV3(sliderValue = 4)))
            runCurrent()
            repeat(2) { ParameterStoreV3.put(rotationInfo, ParameterTypedValueV3.RotationGroup(RotationGroupV3())); runCurrent() }
            assertEquals(listOf(ids, ids, emptyList(), emptyList(), emptyList()), groups)
            first.cancel()
            runCurrent()
            assertTrue(first.isCancelled)
            ParameterStoreV3.put(rotationInfo, ParameterTypedValueV3.RotationGroup(RotationGroupV3(gesture1Id = 77)))
            runCurrent()
            assertEquals(5, groups.size)
            jobs += observe.observeRotationGroup { groups += it }
            runCurrent()
            assertEquals(5, groups.size)
            ParameterStoreV3.put(rotationInfo, ParameterTypedValueV3.RotationGroup(RotationGroupV3(gesture1Id = 64)))
            runCurrent()
            assertEquals(listOf(64), groups.last())
            assertEquals(6, groups.size)
            assertTrue(ignoredGroups.isEmpty())
            assertTrue(packets.isEmpty())
            assertTrue(saved.isEmpty())
            assertEquals("{\"gesture1Id\":64}", rotationCache.data)
            assertEquals("{\"currentGesture\":4}", cache.data)
        } finally {
            jobs.forEach { it.cancel() }
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test fun `default rotation observation retains canonical Unit source parameter guards repeats and nullable getter behavior`() = runTest {
        val rotationCache = addRotationParameter("{\"gesture1Id\":64}")
        val rotationInfo = ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE)
        val defaultWithForeignTarget = V3GesturesRepositoryImpl(
            enqueuePacket = { packets += it }, saveBleValue = { _, value -> saved += value },
            rotationGroupTarget = ParameterRef(91, 92, 93),
        )
        val groups = mutableListOf<List<Int>?>()
        var units = 0
        ParameterStoreV3.put(rotationInfo, ParameterTypedValueV3.RotationGroup(RotationGroupV3(gesture1Id = 5)))
        val jobs = listOf(
            launch { defaultWithForeignTarget.observeRotationGroup().collect { groups += it } },
            launch { defaultWithForeignTarget.rotationGroupUpdates.collect { units++ } },
        )
        try {
            runCurrent()
            assertTrue(groups.isEmpty())
            assertEquals(0, units)
            val value = ParameterTypedValueV3.RotationGroup(RotationGroupV3(gesture1Id = 4, gesture3Id = 64))
            repeat(2) { ParameterStoreV3.put(rotationInfo, value); runCurrent() }
            assertEquals(listOf(listOf(4, 64), listOf(4, 64)), groups)
            assertEquals(2, units)
            ParameterStoreV3.put(rotationInfo, ParameterTypedValueV3.RotationGroup(RotationGroupV3()))
            runCurrent()
            assertEquals(emptyList<Int>(), groups.last())
            assertEquals(3, units)
            ParameterStoreV3.put(rotationInfo, ParameterTypedValueV3.Slider(SliderV3(sliderValue = 4)))
            ParameterStoreV3.put(info, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(77)))
            runCurrent()
            assertEquals(3, groups.size)
            assertEquals(3, units)
            assertNull(defaultWithForeignTarget.getRotationGroupGestureIds())
            val devices = GlobalParameters.baseSubDevicesInfoStructSetV3
            GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf()
            ParameterStoreV3.put(rotationInfo, value)
            runCurrent()
            assertEquals(3, units)
            assertNull(defaultWithForeignTarget.getRotationGroupGestureIds())
            GlobalParameters.baseSubDevicesInfoStructSetV3 = devices
            UiState.v3WidgetsInteractionEnabled.value = false
            UiState.isInterfaceV3Activated = false
            ParameterStoreV3.put(rotationInfo, value)
            runCurrent()
            assertEquals(4, units)
            assertEquals(listOf(4, 64), groups.last())
            assertFalse(defaultWithForeignTarget.requestRotationGroup("device"))
            assertFalse(defaultWithForeignTarget.setRotationGroup("device", listOf(4)))
            assertTrue(packets.isEmpty())
            assertTrue(saved.isEmpty())
            assertEquals("{\"gesture1Id\":64}", rotationCache.data)
        } finally {
            jobs.forEach { it.cancel() }
            runCurrent()
        }
    }

    @Test fun `native active command policy preserves offline GET SET bytes repeats and raw IDs without optimistic shared writes`() {
        val typed = ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(42))
        ParameterStoreV3.put(info, typed)
        UiState.v3WidgetsInteractionEnabled.value = false
        UiState.isInterfaceV3Activated = false
        UiState.activeV3DeviceProfile = V3DeviceProfile.NOT_V3
        WidgetRepoProvider.setCurrentMac("")
        val native = V3GesturesRepositoryImpl(
            enqueuePacket = {
                assertEquals(typed, ParameterStoreV3.get(info))
                assertEquals("{\"currentGesture\":4}", cache.data)
                packets += it
            },
            saveBleValue = { _, _ -> error("Native active selection must not save a profile") },
            saveActiveGestureBeforeSending = false,
            validateActiveGestureDeviceContext = false,
        )
        val current = native.getActiveGesture()
        assertEquals(V3ActiveGesture("", 42, false), current)
        assertFalse(RequestActiveGestureUseCaseV3(native)(""))
        assertFalse(SelectGestureUseCaseV3(native)("", 77))
        val request = RequestActiveGestureUseCaseV3(native, requireInteractionEnabled = false)
        val select = SelectGestureUseCaseV3(native, requireInteractionEnabled = false, validateGestureId = false)
        repeat(2) { assertTrue(request("")) }
        val rawIds = listOf(77, 77, 0, -1, 256, Int.MIN_VALUE, Int.MAX_VALUE)
        rawIds.forEach { assertTrue(select("", it)) }
        val expected = listOf(
            byteArrayOf(0, 15, 36, 0, 0xA5.toByte()), byteArrayOf(0, 15, 36, 0, 0xA5.toByte()),
            byteArrayOf(0, 15, 37, 77, 0xDA.toByte()), byteArrayOf(0, 15, 37, 77, 0xDA.toByte()),
            byteArrayOf(0, 15, 37, 0, 0x61), byteArrayOf(0, 15, 37, 0xFF.toByte(), 0x54),
            byteArrayOf(0, 15, 37, 0, 0x61), byteArrayOf(0, 15, 37, 0, 0x61),
            byteArrayOf(0, 15, 37, 0xFF.toByte(), 0x54),
        )
        assertEquals(expected.size, packets.size)
        expected.zip(packets).forEach { (bytes, packet) -> assertArrayEquals(bytes, packet) }
        assertEquals(current, native.getActiveGesture())
        WidgetRepoProvider.setCurrentMac("changed-device")
        assertTrue(request("stale-device"))
        assertTrue(select("stale-device", 77))
        assertEquals("changed-device", native.getActiveGesture().deviceAddress)
        assertFalse(native.getActiveGesture().isInteractionEnabled)
        assertEquals(42, native.getActiveGesture().gestureId)
        assertEquals(mapOf(ParameterStoreV3.toKey(info) to typed), ParameterStoreV3.values.value)
        assertEquals("{\"currentGesture\":4}", cache.data)
        assertTrue(saved.isEmpty())
    }

    @Test fun `active policy flags remain independent and do not relax rotation context or persistence`() {
        val rotationCache = addRotationParameter()
        val native = V3GesturesRepositoryImpl(
            enqueuePacket = { packets += it }, saveBleValue = { _, value -> saved += value },
            saveActiveGestureBeforeSending = false, validateActiveGestureDeviceContext = false,
        )
        val guarded = V3GesturesRepositoryImpl(
            enqueuePacket = { packets += it }, saveBleValue = { _, value -> saved += value },
            saveActiveGestureBeforeSending = false,
        )
        val unlockedRequest = RequestActiveGestureUseCaseV3(guarded, requireInteractionEnabled = false)
        val unlockedSelect = SelectGestureUseCaseV3(guarded, requireInteractionEnabled = false, validateGestureId = false)
        assertFalse(unlockedRequest("other"))
        assertFalse(unlockedSelect("other", 77))
        UiState.v3WidgetsInteractionEnabled.value = false
        assertFalse(unlockedRequest("device"))
        assertFalse(unlockedSelect("device", 77))
        assertFalse(native.requestRotationGroup("device"))
        assertFalse(native.setRotationGroup("device", listOf(4)))
        UiState.v3WidgetsInteractionEnabled.value = true
        UiState.isInterfaceV3Activated = false
        assertFalse(unlockedRequest("device"))
        assertFalse(native.requestRotationGroup("device"))
        UiState.isInterfaceV3Activated = true
        WidgetRepoProvider.setCurrentMac("")
        assertFalse(unlockedRequest(""))
        assertFalse(native.requestRotationGroup(""))
        WidgetRepoProvider.setCurrentMac("device")
        val devices = GlobalParameters.baseSubDevicesInfoStructSetV3
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf()
        assertFalse(unlockedRequest("device"))
        assertFalse(unlockedSelect("device", 77))
        assertFalse(native.requestRotationGroup("device"))
        assertTrue(packets.isEmpty())
        assertTrue(saved.isEmpty())
        GlobalParameters.baseSubDevicesInfoStructSetV3 = devices

        // Native data alone does not disable the UC's ID rule.
        assertFalse(SelectGestureUseCaseV3(native, requireInteractionEnabled = false)("", 0))
        assertTrue(packets.isEmpty())
        assertTrue(native.requestRotationGroup("device"))
        assertTrue(native.setRotationGroup("device", listOf(4)))
        assertPacket(packets.first(), listOf(0, 15, 53, 0))
        assertRotationPacket(packets.last(), listOf(4))
        assertEquals(1, saved.size)
        assertEquals(saved.single(), ParameterStoreV3.get(ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE)))
        assertEquals(listOf(4), native.getRotationGroupGestureIds())
        assertTrue(rotationCache.data.contains("\"gesture1Id\":4"))
        assertNull(ParameterStoreV3.get(info))
        assertEquals("{\"currentGesture\":4}", cache.data)
    }

    @Test fun `default active selection still persists store profile and cache before enqueue`() {
        val events = mutableListOf<String>()
        val typed = ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(77))
        val android = V3GesturesRepositoryImpl(
            enqueuePacket = {
                assertEquals(typed, ParameterStoreV3.get(info))
                assertEquals("{\"currentGesture\":77}", cache.data)
                assertEquals(listOf("profile"), events)
                events += "queue"
                packets += it
            },
            saveBleValue = { key, value ->
                assertEquals(info, key)
                assertEquals(typed, value)
                assertEquals(typed, ParameterStoreV3.get(info))
                assertEquals("{\"currentGesture\":4}", cache.data)
                assertTrue(packets.isEmpty())
                events += "profile"
            },
        )
        assertTrue(SelectGestureUseCaseV3(android)("device", 77))
        assertEquals(listOf("profile", "queue"), events)
        assertArrayEquals(byteArrayOf(0, 15, 37, 77, 0xDA.toByte()), packets.single())
        assertEquals(77, android.getActiveGesture().gestureId)
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

    @ParameterizedTest @EnumSource(value = V3DeviceProfile::class, names = ["STANDARD_V3", "INDY3"])
    fun `native rotation commands queue exact raw repeated packets offline without changing facts store profile or cache`(profile: V3DeviceProfile) {
        UiState.activeV3DeviceProfile = profile
        val rotationCache = addRotationParameter("{\"gesture1Id\":64}")
        val rotationInfo = ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE)
        ParameterStoreV3.put(rotationInfo, ParameterTypedValueV3.RotationGroup(RotationGroupV3(gesture1Id = 77)))
        ParameterStoreV3.put(info, ParameterTypedValueV3.CurrentGesture(CurrentGestureV3(42)))
        val beforeValues = ParameterStoreV3.values.value
        UiState.v3WidgetsInteractionEnabled.value = false
        UiState.isInterfaceV3Activated = false
        WidgetRepoProvider.setCurrentMac("")
        GlobalParameters.baseSubDevicesInfoStructSetV3 = mutableSetOf()
        val native = V3GesturesRepositoryImpl(
            enqueuePacket = {
                assertEquals(beforeValues, ParameterStoreV3.values.value)
                assertEquals("{\"gesture1Id\":64}", rotationCache.data)
                assertEquals("{\"currentGesture\":4}", cache.data)
                assertEquals(profile, UiState.activeV3DeviceProfile)
                assertTrue(saved.isEmpty())
                packets += it
            },
            saveBleValue = { _, value -> saved += value },
            saveRotationGroupBeforeSending = false,
            validateRotationGroupBeforeSending = false,
        )
        assertEquals(V3ActiveGesture("", 42, false), native.getActiveGesture())
        assertFalse(RequestRotationGroupUseCaseV3(native)(""))
        assertFalse(SetRotationGroupUseCaseV3(native)("", listOf(4)))
        val request = RequestRotationGroupUseCaseV3(native, requireInteractionEnabled = false)
        val set = SetRotationGroupUseCaseV3(native, requireInteractionEnabled = false)
        repeat(2) { assertTrue(request("")) }
        repeat(2) { assertTrue(set("", listOf(4, 4, 77))) }
        assertTrue(set("", emptyList()))
        assertTrue(set("", listOf(0, -1, 256, Int.MIN_VALUE, Int.MAX_VALUE, 4, 4, 77, 2)))
        val getPacket = listOf(0, 15, 53, 0, 141)
        val draftPacket = listOf(128, 15, 17, 0, 174, 54, 4, 4, 4, 4, 77, 77, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 177)
        val emptyPacket = listOf(128, 15, 17, 0, 174, 54, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 189)
        val rawPacket = listOf(128, 15, 17, 0, 174, 54, 0, 0, 255, 255, 0, 0, 0, 0, 255, 255, 4, 4, 4, 4, 77, 77, 160)
        val expected = listOf(getPacket, getPacket, draftPacket, draftPacket, emptyPacket, rawPacket)
        assertEquals(expected.size, packets.size)
        expected.zip(packets).forEach { (bytes, packet) -> assertArrayEquals(bytes.map(Int::toByte).toByteArray(), packet) }
        WidgetRepoProvider.setCurrentMac("changed-device")
        assertTrue(request("stale-device"))
        assertTrue(set("stale-device", listOf(4, 4, 77)))
        assertArrayEquals(getPacket.map(Int::toByte).toByteArray(), packets[6])
        assertArrayEquals(draftPacket.map(Int::toByte).toByteArray(), packets[7])
        assertEquals(V3ActiveGesture("changed-device", 42, false), native.getActiveGesture())
        assertFalse(native.requestActiveGesture("changed-device"))
        assertFalse(native.selectGesture("changed-device", 77))
        assertEquals(8, packets.size)
        assertEquals(beforeValues, ParameterStoreV3.values.value)
        assertTrue(saved.isEmpty())
    }

    @Test fun `rotation data policies remain independent and default narrow commands preserve guards and persistence order`() {
        val rotationCache = addRotationParameter("{\"gesture1Id\":64}")
        val rotationInfo = ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE)
        val events = mutableListOf<String>()
        val typed = ParameterTypedValueV3.RotationGroup(RotationGroupV3(
            gesture1Id = 4, gesture1ImageId = 4, gesture2Id = 4, gesture2ImageId = 4,
            gesture3Id = 77, gesture3ImageId = 77,
        ))
        val android = V3GesturesRepositoryImpl(
            enqueuePacket = {
                if (it[0].toInt() and 255 == 128) {
                    assertEquals(typed, ParameterStoreV3.get(rotationInfo))
                    assertTrue(rotationCache.data.contains("\"gesture3Id\":77"))
                    assertEquals("profile", events.last())
                }
                events += "queue"
                packets += it
            },
            saveBleValue = { key, value ->
                assertEquals(rotationInfo, key)
                assertEquals(typed, value)
                assertEquals(value, ParameterStoreV3.get(key))
                events += "profile"
            },
        )
        assertTrue(RequestRotationGroupUseCaseV3(android)("device"))
        repeat(2) { assertTrue(SetRotationGroupUseCaseV3(android)("device", listOf(4, 4, 77))) }
        assertEquals(listOf("queue", "profile", "queue", "profile", "queue"), events)
        assertRotationPacket(packets.last(), listOf(4, 4, 77))
        val values = ParameterStoreV3.values.value
        val serialized = rotationCache.data
        packets.clear()
        val noSave = V3GesturesRepositoryImpl(
            enqueuePacket = { packets += it }, saveBleValue = { _, value -> saved += value },
            saveRotationGroupBeforeSending = false,
        )
        val request = RequestRotationGroupUseCaseV3(noSave, requireInteractionEnabled = false)
        val set = SetRotationGroupUseCaseV3(noSave, requireInteractionEnabled = false)
        assertFalse(request("other"))
        assertFalse(set("other", listOf(4)))
        listOf(listOf(0), listOf(-1), listOf(256), List(9) { 4 }).forEach { assertFalse(set("device", it)) }
        UiState.v3WidgetsInteractionEnabled.value = false
        assertFalse(request("device"))
        assertFalse(set("device", listOf(4)))
        assertTrue(packets.isEmpty())
        UiState.v3WidgetsInteractionEnabled.value = true
        assertTrue(set("device", listOf(1)))
        assertRotationPacket(packets.single(), listOf(1))
        assertEquals(values, ParameterStoreV3.values.value)
        assertEquals(serialized, rotationCache.data)
        assertTrue(saved.isEmpty())

        val noValidation = V3GesturesRepositoryImpl(
            enqueuePacket = { packets += it }, saveBleValue = { _, value -> saved += value },
            validateRotationGroupBeforeSending = false,
        )
        UiState.v3WidgetsInteractionEnabled.value = false
        assertFalse(SetRotationGroupUseCaseV3(noValidation)("device", listOf(4)))
        assertTrue(SetRotationGroupUseCaseV3(noValidation, requireInteractionEnabled = false)("stale-device", listOf(2)))
        val savedRotation = ParameterTypedValueV3.RotationGroup(RotationGroupV3(gesture1Id = 2, gesture1ImageId = 2))
        assertEquals(listOf(savedRotation), saved)
        assertEquals(savedRotation, ParameterStoreV3.get(rotationInfo))
        assertTrue(rotationCache.data.contains("\"gesture1Id\":2"))
        assertFalse(noValidation.requestActiveGesture("device"))
        assertFalse(noValidation.selectGesture("device", 77))
        assertEquals(2, packets.size)
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

    private fun nativeSnapshotRepository(target: ParameterRef? = ParameterRef(info.deviceAddress, info.parameterID, info.dataCode)) =
        V3GesturesRepositoryImpl(
            enqueuePacket = { packets += it }, saveBleValue = { _, value -> saved += value },
            saveActiveGestureBeforeSending = false, validateActiveGestureDeviceContext = false,
            useActiveGestureSnapshots = true, activeGestureTarget = target,
        )

    private fun nativeRotationSnapshotRepository(target: ParameterRef? = ParameterInfoRegistry.require(P_KEY_GESTURE_GROUPE).let {
        ParameterRef(it.deviceAddress, it.parameterID, it.dataCode)
    }) = V3GesturesRepositoryImpl(
        enqueuePacket = { packets += it }, saveBleValue = { _, value -> saved += value },
        observeRotationGroupSnapshots = true, rotationGroupTarget = target,
    )

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
