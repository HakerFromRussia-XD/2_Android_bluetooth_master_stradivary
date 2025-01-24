package com.bailout.stickk.ubi4.data.parser

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.models.PlotParameterRef
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUBI4
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.activeGestureFlow
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.baseParametrInfoStructArray
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.baseSubDevicesInfoStructSet
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.bindingGroupFlow
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.plotArrayFlow
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.rotationGroupFlow
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.slidersFlow
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.switcherFlow
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4.Companion.thresholdFlow
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock

class BLEParserTest {
    @Mock
    private lateinit var mockActivity: AppCompatActivity // Замените на реальный тип
    private lateinit var bleParser: BLEParser
    private val mockMain = mock<MainActivityUBI4>()
    private val mockProvider = mock<ParameterProvider>()
    private val mockRxUpdate = mock<RxUpdateMainEventUbi4>()
    private val testDispatcher = StandardTestDispatcher()


    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeEach
    fun setup() {
        baseSubDevicesInfoStructSet = linkedSetOf()
        baseParametrInfoStructArray = arrayListOf()
        thresholdFlow = MutableSharedFlow(replay = 1)
        slidersFlow = MutableSharedFlow()
        plotArrayFlow = MutableStateFlow(PlotParameterRef(0, 0, arrayListOf()))
        switcherFlow = MutableSharedFlow()
        bindingGroupFlow = MutableSharedFlow()
        activeGestureFlow = MutableSharedFlow()
        rotationGroupFlow = MutableSharedFlow()
        MockitoAnnotations.openMocks(this)
        bleParser = BLEParser(mockActivity) // Replace with actual initialization logic
        mockkObject(ParameterProvider.Companion)
        mockkStatic(android.util.Log::class)
        baseParametrInfoStructArray.clear()
        baseSubDevicesInfoStructSet.clear()

        Dispatchers.setMain(testDispatcher)
        // Или подменяем именно Default:
//        Dispatchers.setDefault(testDispatcher)

//        every { Log.d(any(), any()) } returns 0 // Игнорируем логи
        // Перенаправляем логи в консоль
        every { Log.d(any(), any()) } answers {
            val tag = args[0] as String   // Первый аргумент (tag)
            val message = args[1] as String // Второй аргумент (message)
            println("DEBUG: [$tag] $message")
            0 // Возвращаем 0, как настоящий Log.d
        }
    }

    @Test
    fun parseReceivedData_nullData_shouldNotCrash() {
        val data: ByteArray? = null
        bleParser.parseReceivedData(data) // Example test logic
    }

    @Test
    fun `when requestType is 1, update UI with correct parameters`() {
        // Устанавливаем ожидаемый параметр
        val expectedParam = BaseParameterInfoStruct(data = "030405")

//        every { ParameterProvider.getParameter(1, 3) } returns expectedParam

        val testData = byteArrayOf(0b01000000.toByte(), 0x03, 0x00, 0x01, 0x00, 0x00, 0x01, 0x03, 0x04 , 0x05)
        bleParser.parseReceivedData(testData)
        verify { ParameterProvider.getParameter(deviceAddress = 1, parameterID = 3)}
        Log.d("testDebugUnitTestttt", " expectedParam.data = ${expectedParam.data}")
        assertEquals(expectedParam.data, ParameterProvider.getParameter(deviceAddress = 1, parameterID = 3).data)

    }


//    @OptIn(ExperimentalCoroutinesApi::class)
//    @ParameterizedTest
//    @MethodSource("testDataProvider")
//    fun `test updateAllUI with PDCE_OPEN_CLOSE_THRESHOLD`(testData: ByteArray) = runTest{
//
//        val bleParser = BLEParser(mockActivity)
//
//
//        val paramWithThreshold = BaseParameterInfoStruct(
//            ID = 7,              // parameterID = 7
//            dataCode = 26,       // PDCE_OPEN_CLOSE_THRESHOLD
//            parameterDataSize = 3,  // чтобы не упали при чтении
//            data = ""            // пока пустая строка
//        )
//        baseParametrInfoStructArray.add(paramWithThreshold)
//
//        every { ParameterProvider.getParameter(0, 7) } returns paramWithThreshold
//
//        val testDataList = listOf(
//            byteArrayOf(0b01000000, 0x07, 0x00, 0x02, 0x00, 0x00, 0x00),
//            byteArrayOf(0b01000000, 0x07, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00),
//            byteArrayOf(0b01000000, 0x07, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01, 0x00),
//            byteArrayOf(0b01000000, 0x07, 0x00, 0x02, 0x00, 0x00, 0x00, 0x02, 0x00),
//            byteArrayOf(0b01000000, 0x07, 0x00, 0x02, 0x00, 0x00, 0x00, 0x02, 0x00),
//            byteArrayOf(0b01000000, 0x07, 0x00, 0x02, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00),
//        )
//
//        testDataList.forEachIndexed { index, testData ->
//            bleParser.parseReceivedData(testData)
//            advanceUntilIdle()
//            //assertTrue("Ожидаем, что data не пустое", paramWithThreshold.data.isNotEmpty())
////            assertTrue(paramWithThreshold.data.isNotEmpty())
//
//            val emitted = thresholdFlow.first()
//            assertEquals(0, emitted.addressDevice, "Fail on testData[$index]")
//            assertEquals(7, emitted.parameterID)
//            assertEquals(26, emitted.dataCode)
//
//            // Можем проверить paramWithThreshold.data, если нужно
//            assertTrue(paramWithThreshold.data.isNotEmpty(), "Fail on testData[$index]")
//        }
//    }

//    @OptIn(ExperimentalCoroutinesApi::class)
//    @Test
//    fun `test PDCE_OPEN_CLOSE_THRESHOLD with multiple testData`() = runTest {
//        // 1) Подготавливаем BLEParser, параметр, т.д.
//        val bleParser = BLEParser(mockActivity)
//        val paramWithThreshold = BaseParameterInfoStruct(
//            ID = 7,
//            dataCode = 26,
//            parameterDataSize = 2,
//            data = ""
//        )
//        baseParametrInfoStructArray.add(paramWithThreshold)
//        every { ParameterProvider.getParameter(0, 7) } returns paramWithThreshold
//
//        // 2) Создаём список разных testData
//        val testDataList = listOf(
//            byteArrayOf(0b01000000, 0x07, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00),
//            byteArrayOf(0b01000000, 0x07, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01, 0x00),
//            byteArrayOf(0b01000000, 0x07, 0x00, 0x02, 0x00, 0x00, 0x00, 0x02, 0x00),
//        )
//
//        // 3) Для каждой testData: вызываем parseReceivedData и проверяем результат
//        testDataList.forEachIndexed { index, testData ->
//            // Вызываем
//            bleParser.parseReceivedData(testData)
//            advanceUntilIdle()
//
//            // Проверяем: читаем одно событие из thresholdFlow
//            val emitted = thresholdFlow.first()  // first() “съест” первое пришедшее событие
//            // Убедимся, что deviceAddress=0, parameterID=7, dataCode=26 (как обычно)
//            assertEquals(0, emitted.addressDevice, "Fail on testData[$index]")
//            assertEquals(7, emitted.parameterID, "Fail on testData[$index]")
//            assertEquals(26, emitted.dataCode, "Fail on testData[$index]")
//
//            // Можем проверить paramWithThreshold.data, если нужно
//            assertTrue(paramWithThreshold.data.isNotEmpty(), "Fail on testData[$index]")
//        }
//    }

    @Test
    fun `test updateAllUI with PDCE_EMG_CH_1_3_VAL`() = runTest {
        // dataCode = 8
        val param = BaseParameterInfoStruct(
            ID = 5,
            dataCode = 8, // PDCE_EMG_CH_1_3_VAL
            parameterDataSize = 3,
            data = ""
        )
        baseParametrInfoStructArray.add(param)
        every { ParameterProvider.getParameter(0, 5) } returns param

        val testData = byteArrayOf(
            0b01000000.toByte(), // 0x40 => requestType=1
            0x05,                // parameterID=5
            0x00,
            0x03, 0x00,          // dataLength=3 (значит ждём 3 байта payload)
            0x00,                // CRC
            0x00,                // deviceAddress=0
            0x00,                // packageCodeRequest
            0x00,                // ID
            0x11,                // payload байт #1
            0x22,                // payload байт #2
            0x33                 // payload байт #3
        )
        bleParser.parseReceivedData(testData)

        assertTrue("data должно быть непустым", param.data.isNotEmpty())

    }

    @Test
    fun `test updateAllUI with PDCE_GESTURE_GROUP`() = runTest {
        // dataCode=32
        val param = BaseParameterInfoStruct(
            ID = 12,
            dataCode = 32, // PDCE_GESTURE_GROUP
            parameterDataSize = 1,
            data = ""
        )
        baseParametrInfoStructArray.add(param)
        every { ParameterProvider.getParameter(0, 12) } returns param

        val testData = byteArrayOf(
            0x40,
            0x0C,   // parameterID=12
            0x00,
            0x01, 0x00,
            0x00,
            0x00,
            0x00,
            0x00
        )
        bleParser.parseReceivedData(testData)

        assertTrue(param.data.isNotEmpty())

    }

    @Test
    fun `test updateAllUI with PDCE_GESTURE_SETTINGS`() = runTest {
        // dataCode=31
        val param = BaseParameterInfoStruct(
            ID = 11,
            dataCode = 31, // PDCE_GESTURE_SETTINGS
            parameterDataSize = 1,
            data = ""
        )
        baseParametrInfoStructArray.add(param)
        every { ParameterProvider.getParameter(0, 11) } returns param

        val testData = byteArrayOf(
            0x40, // requestType=1
            0x0B, // parameterID=11
            0x00,
            0x01, 0x00,
            0x00,
            0x00,
            0x00,
            0x00
        )
        bleParser.parseReceivedData(testData)

        assertTrue(param.data.isNotEmpty())
    }


    @Test
    fun `test updateAllUI with PDCE_OPTIC_LEARNING_DATA`() = runTest {
        // dataCode=33
        val param = BaseParameterInfoStruct(
            ID = 13,
            dataCode = 33,
            parameterDataSize = 1,
            data = ""
        )
        baseParametrInfoStructArray.add(param)
        every { ParameterProvider.getParameter(0, 13) } returns param

        val testData = byteArrayOf(
            0x40,
            0x0D, // ID=13
            0x00,
            0x01, 0x00,
            0x00,
            0x00,
            0x00,
            0x00
        )
        bleParser.parseReceivedData(testData)

        assertTrue(param.data.isNotEmpty())

    }

    @Test
    fun `test updateAllUI with PDCE_GLOBAL_SENSITIVITY`() = runTest {
        // dataCode=4
        val param = BaseParameterInfoStruct(
            ID = 15,
            dataCode = 4,
            parameterDataSize = 1,
            data = ""
        )
        baseParametrInfoStructArray.add(param)
        every { ParameterProvider.getParameter(0, 15) } returns param

        val testData = byteArrayOf(
            0x40,
            0x0F,   // paramID=15
            0x00,
            0x01, 0x00,
            0x00,
            0x00,
            0x00,
            0x00
        )
        bleParser.parseReceivedData(testData)

        assertTrue(param.data.isNotEmpty())

    }

    @Test
    fun `test updateAllUI with PDCE_ENERGY_SAVE_MODE`() = runTest {
        // dataCode=39
        val param = BaseParameterInfoStruct(
            ID = 16,
            dataCode = 39,
            parameterDataSize = 1,
            data = ""
        )
        baseParametrInfoStructArray.add(param)
        every { ParameterProvider.getParameter(0, 16) } returns param

        val testData = byteArrayOf(
            0x40,
            0x10, // paramID=16
            0x00,
            0x01, 0x00,
            0x00,
            0x00,
            0x00,
            0x00
        )
        bleParser.parseReceivedData(testData)
        assertTrue(param.data.isNotEmpty())
    }

    @Test
    fun `test updateAllUI with PDCE_SELECT_GESTURE`() = runTest {
        // dataCode=1
        val param = BaseParameterInfoStruct(
            ID = 6,
            dataCode = 1,
            parameterDataSize = 1,
            data = ""
        )
        baseParametrInfoStructArray.add(param)
        every { ParameterProvider.getParameter(0, 6) } returns param

        val testData = byteArrayOf(
            0x40,
            0x06, // paramID=6
            0x00,
            0x01, 0x00,
            0x00,
            0x00,
            0x00,
            0x00
        )
        bleParser.parseReceivedData(testData)
        assertTrue(param.data.isNotEmpty())
    }

    @Test
    fun `test PDCE_EMG_CH_4_6_GAIN`() = runTest {
        val param = BaseParameterInfoStruct(
            ID = 6,
            dataCode = 12,
            parameterDataSize = 1,
            data = ""
        )
        baseParametrInfoStructArray.add(param)
        every { ParameterProvider.getParameter(0, 6) } returns param

        val testData = byteArrayOf(
            0x40,
            0x06,
            0x00,
            0x01, 0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x11
        )
        bleParser.parseReceivedData(testData)
        assertTrue(param.data.isNotEmpty())

    }

    @Test
    fun `test PDCE_INTERFECE_ERROR_COUNTER`() = runTest {
        val param = BaseParameterInfoStruct(
            ID = 9,
            dataCode = 37,
            parameterDataSize = 2,
            data = ""
        )
        baseParametrInfoStructArray.add(param)
        every { ParameterProvider.getParameter(0, 9) } returns param

        val testData = byteArrayOf(
            0x40,
            0x09,
            0x00,
            0x02, 0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x22, 0x33
        )
        bleParser.parseReceivedData(testData)

        assertTrue(param.data.isNotEmpty())
    }


    @Test
    fun `test PDCE_CALIBRATION_CURRENT_PERCENT`() = runTest {
        val param = BaseParameterInfoStruct(
            ID = 10,
            dataCode = 38,
            parameterDataSize = 1,
            data = ""
        )
        baseParametrInfoStructArray.add(param)
        every { ParameterProvider.getParameter(0, 10) } returns param

        val testData = byteArrayOf(
            0x40,
            0x0A,
            0x00,
            0x01, 0x00,
            0x00,
            0x00,
            0x00,
            0x00,
            0x44
        )
        bleParser.parseReceivedData(testData)
        assertTrue(param.data.isNotEmpty())

    }

    @Test
    fun `test PDCE_GENERIC_0`() = runTest {
        val param = BaseParameterInfoStruct(
            ID = 14,
            dataCode = 255,
            parameterDataSize = 5,
            data = ""
        )
        baseParametrInfoStructArray.add(param)
        every { ParameterProvider.getParameter(0, 14) } returns param

        val testData = byteArrayOf(
            0x40,
            0x0E,     // paramID=14
            0x00,
            0x05, 0x00, // dataLength=5
            0x00,
            0x00,
            0x00,
            0x00,
            // payload
            0x01,  // newStatusExist
            0x00, 0x02,  // packIndex => 0x0002
            0x00,  // errorStatus=0
            0x7F   // 5й байт (неважно)
        )
        bleParser.parseReceivedData(testData)

        assertTrue(param.data.isNotEmpty())
    }

    @Test
    fun `test PDCE_OPTIC_BINDING_DATA`() = runTest {
        val param = BaseParameterInfoStruct(
            ID = 16,
            dataCode = 43,
            parameterDataSize = 2,
            data = ""
        )
        baseParametrInfoStructArray.add(param)
        every { ParameterProvider.getParameter(0, 16) } returns param

        val testData = byteArrayOf(
            0x40,
            0x10,
            0x00,
            0x02, 0x00,
            0x00,
            0x00,
            0x00,
            0x00,
        )
        bleParser.parseReceivedData(testData)
        assertTrue(param.data.isNotEmpty())

    }


    @Test
    fun `when parseWidgets is triggered, listWidgets should contain new widget`() {
        val bleParserUnderTest = BLEParser(mockActivity)

        // Изначально список пуст
        assertEquals(0, MainActivityUBI4.listWidgets.size)

//        bleParserUnderTest.parseReceivedData(yourTestBytes)

        val updatedList = MainActivityUBI4.listWidgets
        assertEquals(1, updatedList.size)
        val widget = updatedList.first()

    }


}