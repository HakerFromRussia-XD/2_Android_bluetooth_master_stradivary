package com.bailout.stickk.ubi4.versions.v3.presentation.dashboard

import androidx.lifecycle.ViewModelStore
import com.bailout.stickk.ubi4.ble.BLECommandsV3
import com.bailout.stickk.ubi4.data.state.DashboardSlotContentSchemas
import com.bailout.stickk.ubi4.data.state.DashboardSlotContentState
import com.bailout.stickk.ubi4.data.state.DashboardSlotContentUiState
import com.bailout.stickk.ubi4.ui.fragments.dashboard.toSharedUiState
import com.bailout.stickk.ubi4.versions.v3.data.dashboard.V3DashboardSlotContentRepositoryImpl
import com.bailout.stickk.ubi4.versions.v3.di.V3DashboardSlotContentViewModelFactory
import com.bailout.stickk.ubi4.versions.v3.domain.dashboard.V3DashboardSlotContentTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

/** Covers the production path through DI, domain and data, including the unchanged shared state. */
@OptIn(ExperimentalCoroutinesApi::class)
class V3DashboardSlotContentViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val original = DashboardSlotContentState.stateFlow.value
    private val packets = mutableListOf<ByteArray>()
    private val statusBeforeWrites = mutableListOf<String?>()
    private val store = ViewModelStore()
    private val target = V3DashboardSlotContentTarget(8, 0x21, "Настройки платы", 3, 2, 3)
    private lateinit var viewModel: V3DashboardSlotContentViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val repository = V3DashboardSlotContentRepositoryImpl { packet ->
            val state = DashboardSlotContentState.stateFlow.value
            val command = packet[if (packet.first() == 0x80.toByte()) 5 else 2].toInt()
            if (command in listOf(0x03, 0x09)) {
                assertTrue(state.isLoading)
                assertTrue(state.data.isEmpty())
                assertTrue(state.editedValues.isEmpty())
                assertNull(state.errorMessage)
            } else {
                statusBeforeWrites += state.statusMessage
            }
            packets += packet
        }
        viewModel = V3DashboardSlotContentViewModelFactory(repository)
            .create(V3DashboardSlotContentViewModel::class.java)
        store.put("content", viewModel)
    }

    @AfterEach
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
        // Restore every shared field without adding a production reset API just for tests.
        @Suppress("UNCHECKED_CAST")
        val state = DashboardSlotContentState.javaClass.getDeclaredField("_stateFlow")
            .apply { isAccessible = true }.get(DashboardSlotContentState) as MutableStateFlow<DashboardSlotContentUiState>
        state.value = original
    }

    @ParameterizedTest
    @CsvSource("-1,1,6000", "0,1,6000", "1,1,6000", "200,1,6000", "201,1,7000",
        "250,1,7000", "251,2,7000", "400,2,7000", "401,3,8000")
    fun `read boundaries preserve packet count offsets and exact timeout`(size: Int, count: Int, timeout: Long) = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target.copy(declaredSize = size)))
        runCurrent()
        assertEquals(count, packets.size)
        if (size <= 250) {
            assertArrayEquals(BLECommandsV3.requestSlotData(8, 0x21), packets.single())
            assertEquals("80 02 03 00 C3 03 08 21 EF", packets.single().toHex())
        } else {
            packets.forEachIndexed { index, packet ->
                assertArrayEquals(BLECommandsV3.requestSlotDataPart(8, 0x21, index * 200,
                    minOf(200, size - index * 200)), packet)
            }
        }
        assertEquals(target.copy(declaredSize = size), viewModel.uiState.value.content.slot)
        advanceTimeBy(timeout - 1)
        runCurrent()
        assertTrue(viewModel.uiState.value.content.isLoading)
        advanceTimeBy(1)
        runCurrent()
        assertEquals("Плата не ответила на запрос содержимого", viewModel.uiState.value.content.errorMessage)
        assertFalse(viewModel.uiState.value.content.isLoading)
        assertNull(viewModel.uiState.value.content.statusMessage)
    }

    @Test
    fun `loaded content and subsequent edits reach the same shared screen without extra commands`() = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        assertEquals(DashboardSlotContentState.stateFlow.value, viewModel.uiState.value.toSharedUiState())
        DashboardSlotContentState.updateData(0x21, listOf(0, 127, 255))
        runCurrent()
        assertEquals(listOf(0, 127, 255), viewModel.uiState.value.content.data)
        assertEquals(3, viewModel.uiState.value.content.loadedSize)
        DashboardSlotContentState.updateParameterValue("raw:1", "42")
        runCurrent()
        assertEquals(listOf(0, 42, 255), viewModel.uiState.value.content.data)
        assertEquals(mapOf("raw:1" to "42"), viewModel.uiState.value.content.editedValues)
        val shared = DashboardSlotContentState.stateFlow.value
        assertEquals(shared, viewModel.uiState.value.toSharedUiState())
        assertEquals(DashboardSlotContentSchemas.parse(shared),
            DashboardSlotContentSchemas.parse(viewModel.uiState.value.toSharedUiState()))
        DashboardSlotContentState.updateStatus("Данные отправлены")
        runCurrent()
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals("Данные отправлены", viewModel.uiState.value.content.statusMessage)
        assertNull(viewModel.uiState.value.content.errorMessage)
        assertEquals(1, packets.size)
    }

    @Test
    fun `parts preserve progress and assembly and late completion clears the timeout`() = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target.copy(declaredSize = 401)))
        runCurrent()
        assertEquals(listOf(
            "80 02 0B 00 B5 09 08 21 00 00 00 00 C8 00 00 00 E1",
            "80 02 0B 00 B5 09 08 21 C8 00 00 00 C8 00 00 00 10",
            "80 02 0B 00 B5 09 08 21 90 01 00 00 01 00 00 00 B0",
        ), packets.map { it.toHex() })
        DashboardSlotContentState.updateDataPart(0x22, 0, listOf(99))
        runCurrent()
        assertEquals(0, viewModel.uiState.value.content.loadedSize)
        val first = List(200) { it }
        DashboardSlotContentState.updateDataPart(0x21, 0, first)
        runCurrent()
        assertEquals(200, viewModel.uiState.value.content.loadedSize)
        assertEquals("Загружено 200/401 байт", viewModel.uiState.value.content.statusMessage)
        assertEquals(first + List(201) { 0 }, viewModel.uiState.value.content.data)
        advanceTimeBy(8_000)
        runCurrent()
        assertNotNull(viewModel.uiState.value.content.errorMessage)
        DashboardSlotContentState.updateDataPart(0x21, 200, List(200) { 255 })
        runCurrent()
        assertTrue(viewModel.uiState.value.content.isLoading)
        assertNull(viewModel.uiState.value.content.errorMessage)
        DashboardSlotContentState.updateDataPart(0x21, 400, listOf(42))
        runCurrent()
        assertFalse(viewModel.uiState.value.content.isLoading)
        assertEquals(first + List(200) { 255 } + 42, viewModel.uiState.value.content.data)
        assertEquals("Данные загружены", viewModel.uiState.value.content.statusMessage)
        assertEquals(3, packets.size)
    }

    @Test
    fun `empty and out of order replies retain the shared parser's completion rules`() = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        DashboardSlotContentState.updateData(0x21, emptyList())
        runCurrent()
        assertFalse(viewModel.uiState.value.content.isLoading)
        assertEquals("Данные загружены", viewModel.uiState.value.content.statusMessage)
        viewModel.onAction(V3DashboardSlotContentAction.Refresh)
        runCurrent()
        // Existing parser treats the tail reaching declaredSize as complete; do not change that here.
        DashboardSlotContentState.updateDataPart(0x21, 2, listOf(42))
        runCurrent()
        assertEquals(listOf(0, 0, 42), viewModel.uiState.value.content.data)
        assertFalse(viewModel.uiState.value.content.isLoading)
        advanceTimeBy(6_000)
        runCurrent()
        assertNull(viewModel.uiState.value.content.errorMessage)
    }

    @Test
    fun `refresh clears edits but preserves independent timeout jobs from the same view`() = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        DashboardSlotContentState.updateData(0x21, listOf(1, 2, 3))
        DashboardSlotContentState.updateParameterValue("raw:0", "42")
        advanceTimeBy(2_000)
        viewModel.onAction(V3DashboardSlotContentAction.Refresh)
        runCurrent()
        assertEquals(2, packets.size)
        assertTrue(viewModel.uiState.value.content.data.isEmpty())
        assertTrue(viewModel.uiState.value.content.editedValues.isEmpty())
        advanceTimeBy(4_000)
        runCurrent()
        // The old Fragment also kept the first request's timer after Refresh.
        assertNotNull(viewModel.uiState.value.content.errorMessage)
        DashboardSlotContentState.updateData(0x21, listOf(9))
        runCurrent()
        advanceTimeBy(2_000)
        runCurrent()
        assertNull(viewModel.uiState.value.content.errorMessage)
        assertEquals(listOf(9), viewModel.uiState.value.content.data)
    }

    @ParameterizedTest
    @CsvSource("9,33", "8,34")
    fun `timeout cannot fail another board or slot`(address: Int, code: Int) = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        DashboardSlotContentState.requestStarted(address, code, "Другой слот", 1, 0, 10)
        advanceTimeBy(6_000)
        runCurrent()
        assertTrue(viewModel.uiState.value.content.isLoading)
        assertNull(viewModel.uiState.value.content.errorMessage)
        assertEquals(address, viewModel.uiState.value.content.slot.deviceAddress)
        assertEquals(code, viewModel.uiState.value.content.slot.dataCode)
    }

    @Test
    fun `destroying the view cancels all timers and observation and recreation loads again`() = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotContentAction.Refresh)
        runCurrent()
        assertTrue(packets.isEmpty())
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        advanceTimeBy(2_000)
        viewModel.onAction(V3DashboardSlotContentAction.Refresh)
        runCurrent()
        advanceTimeBy(1_000)
        viewModel.onAction(V3DashboardSlotContentAction.ViewDestroyed)
        runCurrent()
        val detached = viewModel.uiState.value
        DashboardSlotContentState.updateData(0x21, listOf(42))
        viewModel.onAction(V3DashboardSlotContentAction.Refresh)
        runCurrent()
        assertEquals(detached, viewModel.uiState.value)
        assertEquals(2, packets.size)
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        assertTrue(viewModel.uiState.value.content.isLoading)
        advanceTimeBy(1_000)
        runCurrent()
        assertNotNull(viewModel.uiState.value.content.errorMessage)
        assertEquals(3, packets.size)
    }

    @Test
    fun `cleared viewmodel cannot restart requests or fire its old timeout`() = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        store.clear()
        runCurrent()
        viewModel.onAction(V3DashboardSlotContentAction.Refresh)
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(1, packets.size)
        assertTrue(DashboardSlotContentState.stateFlow.value.isLoading)
        assertNull(DashboardSlotContentState.stateFlow.value.errorMessage)
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = [
        "raw:1;0xFF;0 255 0 0 0 0 0 0",
        "field|0|2|uint16_t|1;0x1234;52 18 0 0 0 0 0 0",
        "field|0|2|int16_t|1;-2;254 255 0 0 0 0 0 0",
        "field|0|1|uint8_t|3;[1, 0xFF, 300];1 255 44 0 0 0 0 0",
        "field|0|4|string|1;ABCDE;65 66 67 68 0 0 0 0",
        "field|0|4|float|1;1.5;0 0 192 63 0 0 0 0",
        "field|0|2|bitfield|1;0xFF;255 0 0 0 0 0 0 0",
    ])
    fun `editing preserves existing field encodings without sending commands`(path: String, input: String, expected: String) = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        DashboardSlotContentState.updateData(0x21, List(8) { 0 })
        viewModel.onAction(V3DashboardSlotContentAction.ParameterChanged(path, input))
        runCurrent()
        assertEquals(expected.split(' ').map { it.toInt() }, viewModel.uiState.value.content.data)
        assertEquals(mapOf(path to input), viewModel.uiState.value.content.editedValues)
        assertEquals("Есть несохраненные изменения", viewModel.uiState.value.content.statusMessage)
        assertEquals(DashboardSlotContentState.stateFlow.value, viewModel.uiState.value.toSharedUiState())
        assertEquals(1, packets.size) // Only the initial read.
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "-", "not a number"])
    fun `unfinished input remains visible while its bytes retain the previous value`(input: String) = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        DashboardSlotContentState.updateData(0x21, listOf(42))
        viewModel.onAction(V3DashboardSlotContentAction.ParameterChanged("raw:0", input))
        runCurrent()
        assertEquals(listOf(42), viewModel.uiState.value.content.data)
        assertEquals(mapOf("raw:0" to input), viewModel.uiState.value.content.editedValues)
        assertEquals("Есть несохраненные изменения", viewModel.uiState.value.content.statusMessage)
        assertEquals(1, packets.size)
    }

    @Test
    fun `editing before any bytes arrive does nothing and sending empty content reports the existing status`() = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        val before = DashboardSlotContentState.stateFlow.value
        viewModel.onAction(V3DashboardSlotContentAction.ParameterChanged("raw:0", "42"))
        assertEquals(before, DashboardSlotContentState.stateFlow.value)
        viewModel.onAction(V3DashboardSlotContentAction.Send)
        runCurrent()
        assertEquals("Нет данных для отправки", viewModel.uiState.value.content.statusMessage)
        assertFalse(viewModel.uiState.value.content.isLoading)
        advanceTimeBy(6_000)
        runCurrent()
        assertNull(viewModel.uiState.value.content.errorMessage)
        assertEquals(1, packets.size)
    }

    @ParameterizedTest
    @CsvSource("1,1", "200,1", "201,1", "250,1", "251,2", "400,2", "401,3")
    fun `send uses actual draft size and preserves bytes offsets and packet order`(size: Int, count: Int) = runTest(dispatcher) {
        // declaredSize deliberately differs: the old sender used current.data.size.
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        val data = List(size) { it and 0xFF }
        DashboardSlotContentState.updateData(0x21, data)
        packets.clear()
        viewModel.onAction(V3DashboardSlotContentAction.Send)
        assertEquals(count, packets.size)
        val bytes = ByteArray(size) { data[it].toByte() }
        if (size <= 250) {
            assertArrayEquals(BLECommandsV3.writeSlotData(8, 0x21, bytes), packets.single())
        } else {
            packets.forEachIndexed { index, packet ->
                val offset = index * 200
                assertArrayEquals(BLECommandsV3.writeSlotDataPart(8, 0x21, offset,
                    bytes.copyOfRange(offset, minOf(size, offset + 200))), packet)
            }
        }
        assertEquals(List(count) { "Данные загружены" }, statusBeforeWrites)
        runCurrent()
        assertEquals("Данные отправлены", viewModel.uiState.value.content.statusMessage)
        assertFalse(viewModel.uiState.value.content.isLoading)
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(count, packets.size) // Rendering and the old read timer do not send anything.
        assertEquals(data, viewModel.uiState.value.content.data)
    }

    @Test
    fun `immediate send sees the latest edit before collection and explicit repeated sends stay allowed`() = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        DashboardSlotContentState.updateData(0x21, listOf(0, 128, 255))
        runCurrent()
        viewModel.onAction(V3DashboardSlotContentAction.ParameterChanged("raw:0", "42"))
        assertEquals(listOf(0, 128, 255), viewModel.uiState.value.content.data)
        viewModel.onAction(V3DashboardSlotContentAction.Send)
        viewModel.onAction(V3DashboardSlotContentAction.Send)
        assertEquals(3, packets.size)
        assertArrayEquals(BLECommandsV3.writeSlotData(8, 0x21, byteArrayOf(42, 128.toByte(), 255.toByte())), packets[1])
        assertArrayEquals(packets[1], packets[2])
        assertEquals(listOf("Есть несохраненные изменения", "Данные отправлены"), statusBeforeWrites)
        runCurrent()
        assertEquals(mapOf("raw:0" to "42"), viewModel.uiState.value.content.editedValues)
        viewModel.onAction(V3DashboardSlotContentAction.Refresh)
        runCurrent()
        assertTrue(viewModel.uiState.value.content.data.isEmpty())
        assertTrue(viewModel.uiState.value.content.editedValues.isEmpty())
        assertEquals(4, packets.size)
    }

    @Test
    fun `partial content can still be sent without introducing a new loading lock`() = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        DashboardSlotContentState.updateDataPart(0x21, 0, listOf(7))
        runCurrent()
        assertTrue(viewModel.uiState.value.content.isLoading)
        viewModel.onAction(V3DashboardSlotContentAction.Send)
        assertArrayEquals(BLECommandsV3.writeSlotData(8, 0x21, byteArrayOf(7, 0, 0)), packets.last())
        runCurrent()
        assertEquals("Данные отправлены", viewModel.uiState.value.content.statusMessage)
    }

    @Test
    fun `edit and send cannot act without a view after destroy or after viewmodel clearing`() = runTest(dispatcher) {
        DashboardSlotContentState.requestStarted(8, 0x21, "Слот", 1, 0, 1)
        DashboardSlotContentState.updateData(0x21, listOf(7))
        val before = DashboardSlotContentState.stateFlow.value
        viewModel.onAction(V3DashboardSlotContentAction.ParameterChanged("raw:0", "42"))
        viewModel.onAction(V3DashboardSlotContentAction.Send)
        assertEquals(before, DashboardSlotContentState.stateFlow.value)
        assertTrue(packets.isEmpty())
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        DashboardSlotContentState.updateData(0x21, listOf(7))
        viewModel.onAction(V3DashboardSlotContentAction.ViewDestroyed)
        viewModel.onAction(V3DashboardSlotContentAction.ParameterChanged("raw:0", "42"))
        viewModel.onAction(V3DashboardSlotContentAction.Send)
        assertEquals(listOf(7), DashboardSlotContentState.stateFlow.value.data)
        assertEquals(1, packets.size)
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        DashboardSlotContentState.updateData(0x21, listOf(9))
        store.clear()
        viewModel.onAction(V3DashboardSlotContentAction.ParameterChanged("raw:0", "42"))
        viewModel.onAction(V3DashboardSlotContentAction.Send)
        runCurrent()
        assertEquals(listOf(9), DashboardSlotContentState.stateFlow.value.data)
        assertEquals(2, packets.size)
    }

    @ParameterizedTest
    @CsvSource("0,1", "8,33", "255,254")
    fun `save and reset use the screen target without sending its draft or reloading`(address: Int, code: Int) = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target.copy(deviceAddress = address, dataCode = code)))
        runCurrent()
        DashboardSlotContentState.updateData(code, listOf(1, 2, 3))
        viewModel.onAction(V3DashboardSlotContentAction.ParameterChanged("raw:0", "42"))
        runCurrent()
        val before = viewModel.uiState.value
        packets.clear()
        viewModel.onAction(V3DashboardSlotContentAction.Save)
        viewModel.onAction(V3DashboardSlotContentAction.Reset)
        assertEquals(2, packets.size)
        assertArrayEquals(BLECommandsV3.saveSlots(address), packets[0])
        assertArrayEquals(BLECommandsV3.resetSlot(address, code), packets[1])
        if (address == 8) {
            assertEquals("00 02 06 08 27", packets[0].toHex())
            assertEquals("80 02 03 00 C3 05 08 21 3E", packets[1].toHex())
        }
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(before, viewModel.uiState.value) // No invented success state or clearing of edits.
        assertEquals(2, packets.size)
    }

    @Test
    fun `save and reset keep working without loaded bytes and repeated explicit clicks stay allowed`() = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        val loading = viewModel.uiState.value
        repeat(2) {
            viewModel.onAction(V3DashboardSlotContentAction.Save)
            viewModel.onAction(V3DashboardSlotContentAction.Reset)
        }
        runCurrent()
        assertEquals(5, packets.size)
        assertArrayEquals(packets[1], packets[3])
        assertArrayEquals(packets[2], packets[4])
        assertEquals(loading, viewModel.uiState.value)
        assertNull(viewModel.uiState.value.resetAllConfirmationId)
    }

    @Test
    fun `reset all only sends once after the matching confirmation and preserves content`() = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        viewModel.onAction(V3DashboardSlotContentAction.ResetAllConfirmed(0))
        viewModel.onAction(V3DashboardSlotContentAction.ResetAll)
        val id = requireNotNull(viewModel.uiState.value.resetAllConfirmationId)
        viewModel.onAction(V3DashboardSlotContentAction.ResetAll)
        assertEquals(id, viewModel.uiState.value.resetAllConfirmationId)
        assertEquals(1, packets.size)
        // A response or timeout while the dialog is visible must not consume the request.
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(id, viewModel.uiState.value.resetAllConfirmationId)
        DashboardSlotContentState.updateData(0x21, listOf(1, 2, 3))
        runCurrent()
        assertEquals(id, viewModel.uiState.value.resetAllConfirmationId)
        val content = viewModel.uiState.value.content
        viewModel.onAction(V3DashboardSlotContentAction.ResetAllConfirmed(id + 1))
        assertEquals(1, packets.size)
        viewModel.onAction(V3DashboardSlotContentAction.ResetAllConfirmed(id))
        assertNull(viewModel.uiState.value.resetAllConfirmationId)
        assertEquals(2, packets.size)
        assertArrayEquals(BLECommandsV3.resetAllSlots(8), packets.last())
        assertEquals("80 02 03 00 C3 05 08 FF 76", packets.last().toHex())
        viewModel.onAction(V3DashboardSlotContentAction.ResetAllConfirmed(id))
        viewModel.onAction(V3DashboardSlotContentAction.ResetAllDismissed(id))
        runCurrent()
        assertEquals(2, packets.size)
        assertEquals(content, viewModel.uiState.value.content)
    }

    @Test
    fun `cancel never resets and stale dialog callbacks cannot consume a later request`() = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        viewModel.onAction(V3DashboardSlotContentAction.ResetAll)
        val first = requireNotNull(viewModel.uiState.value.resetAllConfirmationId)
        viewModel.onAction(V3DashboardSlotContentAction.ResetAllDismissed(first))
        assertNull(viewModel.uiState.value.resetAllConfirmationId)
        viewModel.onAction(V3DashboardSlotContentAction.ResetAllConfirmed(first))
        assertEquals(1, packets.size)
        viewModel.onAction(V3DashboardSlotContentAction.ResetAll)
        val second = requireNotNull(viewModel.uiState.value.resetAllConfirmationId)
        assertNotEquals(first, second)
        viewModel.onAction(V3DashboardSlotContentAction.ResetAllDismissed(first))
        viewModel.onAction(V3DashboardSlotContentAction.ResetAllConfirmed(first))
        assertEquals(second, viewModel.uiState.value.resetAllConfirmationId)
        assertEquals(1, packets.size)
        viewModel.onAction(V3DashboardSlotContentAction.ResetAllDismissed(second))
        runCurrent()
        assertNull(viewModel.uiState.value.resetAllConfirmationId)
        assertEquals(1, packets.size)
    }

    @Test
    fun `destroy and recreation discard old confirmations including callbacks for another board`() = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotContentAction.Save)
        viewModel.onAction(V3DashboardSlotContentAction.Reset)
        viewModel.onAction(V3DashboardSlotContentAction.ResetAll)
        assertNull(viewModel.uiState.value.resetAllConfirmationId)
        assertTrue(packets.isEmpty())
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        viewModel.onAction(V3DashboardSlotContentAction.ResetAll)
        val oldId = requireNotNull(viewModel.uiState.value.resetAllConfirmationId)
        viewModel.onAction(V3DashboardSlotContentAction.ViewDestroyed)
        viewModel.onAction(V3DashboardSlotContentAction.Save)
        viewModel.onAction(V3DashboardSlotContentAction.Reset)
        viewModel.onAction(V3DashboardSlotContentAction.ResetAll)
        viewModel.onAction(V3DashboardSlotContentAction.ResetAllConfirmed(oldId))
        assertNull(viewModel.uiState.value.resetAllConfirmationId)
        assertEquals(1, packets.size)
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target.copy(deviceAddress = 9)))
        runCurrent()
        viewModel.onAction(V3DashboardSlotContentAction.ResetAll)
        val newId = requireNotNull(viewModel.uiState.value.resetAllConfirmationId)
        viewModel.onAction(V3DashboardSlotContentAction.ResetAllConfirmed(oldId))
        assertEquals(2, packets.size)
        assertEquals(newId, viewModel.uiState.value.resetAllConfirmationId)
        viewModel.onAction(V3DashboardSlotContentAction.ResetAllConfirmed(newId))
        assertEquals(3, packets.size)
        assertArrayEquals(BLECommandsV3.resetAllSlots(9), packets.last())
    }

    @Test
    fun `cleared viewmodel cannot save reset or confirm its former dialog`() = runTest(dispatcher) {
        viewModel.onAction(V3DashboardSlotContentAction.ViewCreated(target))
        runCurrent()
        viewModel.onAction(V3DashboardSlotContentAction.ResetAll)
        val id = requireNotNull(viewModel.uiState.value.resetAllConfirmationId)
        store.clear()
        viewModel.onAction(V3DashboardSlotContentAction.Save)
        viewModel.onAction(V3DashboardSlotContentAction.Reset)
        viewModel.onAction(V3DashboardSlotContentAction.ResetAll)
        viewModel.onAction(V3DashboardSlotContentAction.ResetAllConfirmed(id))
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(1, packets.size)
    }

    private fun ByteArray.toHex() = joinToString(" ") { (it.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0') }
}
