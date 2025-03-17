package com.bailout.stickk.ubi4.data.parser

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
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
import io.mockk.mockkObject
import io.mockk.mockkStatic
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
        bleParser = BLEParser() // Replace with actual initialization logic
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

        val testData =
            byteArrayOf(0b01000000.toByte(), 0x03, 0x00, 0x01, 0x00, 0x00, 0x01, 0x03, 0x04, 0x05)
        bleParser.parseReceivedData(testData)
        verify { ParameterProvider.getParameter(deviceAddress = 1, parameterID = 3) }
        Log.d("testDebugUnitTestttt", " expectedParam.data = ${expectedParam.data}")
        assertEquals(
            expectedParam.data,
            ParameterProvider.getParameter(deviceAddress = 1, parameterID = 3).data
        )

    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test updateAllUI with PDCE_EMG_CH_1_3_VAL`() = runTest {
        // 1) Создаем BLEParser и нужный параметр с dataCode=8 (PDCE_EMG_CH_1_3_VAL).
        //    Допустим, ожидаем 6 байт полезной нагрузки.
        val bleParser = BLEParser()
        val param = BaseParameterInfoStruct(
            ID = 5,
            dataCode = PreferenceKeysUBI4.ParameterDataCodeEnum.PDCE_EMG_CH_1_3_VAL.number, // 8
            parameterDataSize = 6,
            data = ""
        )
        baseParametrInfoStructArray.add(param)

        // Мокаем доступ к параметру через ParameterProvider.
        every { ParameterProvider.getParameter(0, 5) } returns param

        // 2) Определяем набор тестовых входных данных.
        //    - (A) Корректный набор байтов → 6 байтов payload.
        //    - (B) Обрезанные данные (меньше 6 байт).
        //    - (C) Лишние данные (больше 6 байт).
        //    - (D) Неверный dataCode (например, 0x99 вместо 0x08).
        //    - (E) Пустой массив (или слишком короткий).
        val testDataList = listOf(
            byteArrayOf(
                0x40.toByte(), 0x05, 0x00,
                0x06, 0x00, // dataLength = 6
                0x00,       // CRC
                0x00,       // deviceAddress = 0
                0x08,       // packageCodeRequest=8
                0x0A, 0x1B, 0x2C, 0x3D, 0x4E, 0x5F
            ),

            // (B) Обрезанные данные: допустим, payload=3 байта вместо 6.
            // Твоя логика в BLEParser может дополнять нулями (или выбрасывать исключение).
            // Проверим, что не упадет и выдаст что-то адекватное.
            byteArrayOf(
                0x40.toByte(), 0x05, 0x00,
                0x03, 0x00, // dataLength = 3
                0x00,
                0x00,
                0x08,
                0x0A, 0x1B, 0x2C
            ),

            // (C) Лишние данные: payload=8 байт (хотя нужно 6).
            // Обычно парсер берет первые 6, остальные игнорирует (или твой код может упасть — проверим).
            byteArrayOf(
                0x40.toByte(), 0x05, 0x00,
                0x08, 0x00, // dataLength = 8
                0x00,
                0x00,
                0x08,
                0x0A, 0x1B, 0x2C, 0x3D, 0x4E, 0x5F, 0x6A, 0x7B
            ),

            // (D) Неверный dataCode: скажем, 0x99 вместо 0x08.
            // Проверим, что твой код либо пропустит обновление, либо сделает что-то другое.
            byteArrayOf(
                0x40.toByte(), 0x05, 0x00,
                0x06, 0x00,
                0x00,
                0x00,
                0x99.toByte(), // packageCodeRequest=0x99
                0x0A, 0x1B, 0x2C, 0x3D, 0x4E, 0x5F
            ),

            // (E) Пустой или слишком короткий массив.
            // Тут просто проверяем, что parseReceivedData не падает.
            byteArrayOf()
        )

        // 3) Обходим тестовые данные и проверяем результат.
        //    При корректном dataCode = 8 ожидаем, что plotArrayFlow будет эмитить данные (6 int-значений).
        //    Иначе, возможно, BLEParser пропустит обновление.
        testDataList.forEachIndexed { index, testData ->

            // Перед каждым прогоном обнулим param.data
            // и сбросим plotArrayFlow в изначальное состояние
            param.data = ""
            plotArrayFlow.value = PlotParameterRef(0, 0, arrayListOf())

            // Парсим
            bleParser.parseReceivedData(testData)

            // Прокручиваем тестовый диспетчер, чтобы корутины успели эмитить.
            // Можно `advanceUntilIdle()`, но если твой код внутри задержек нет,
            // достаточно `runCurrent()`. Используй то, что подходит.
            advanceUntilIdle()

            // Если dataCode действительно 8 и данные были достаточно длинные,
            // BLEParser должен положить результат в plotArrayFlow.
            // При этом в твоем коде первая эмиссия происходит сразу при setValue(),
            // поэтому берем drop(1), чтобы дождаться следующего реального emit.
            // Но будь аккуратен с replay и buffer у MutableStateFlow (у тебя 0, значит всё ок).

            // Для удобства можно решить, что когда dataCode корректный и payload >= 6,
            // результат в plotArrayFlow.dataPlots будет не пуст.
            // В остальных случаях оно останется пустым (или заполнится нулями).

            // Если есть риск "UncompletedCoroutinesError", убедиcь, что:
            //  1) parseReceivedData не запускает бесконечных отложенных операций.
            //  2) Тест действительно завершается (нет suspend-функций, зависающих вечно).

            // Пример проверки — пробуем считать единственную эмиссию:
            val actualEmission = plotArrayFlow.value
            when (index) {
                0 -> {
                    // (A) Ожидаем, что будет [10, 27, 44, 61, 78, 95]
                    assertEquals(
                        listOf(10, 27, 44, 61, 78, 95), actualEmission.dataPlots,
                        "Набор (A) не совпал с ожидаемым"
                    )
                }

                1 -> {
                    // (B) Обрезанные данные, твой парсер либо кинет исключение,
                    //     либо заполнит часть массива, остальное нулями, либо вообще проигнорирует.
                    // Допустим, твой код заполняет первые 3 значения, а остальные нулями.
                    // Проверяй по своей логике.
                    assertEquals(
                        listOf(10, 27, 44, 0, 0, 0), actualEmission.dataPlots,
                        "Набор (B) - ожидали, что три байта используются, остальное нули"
                    )
                }

                2 -> {
                    // (C) Лишние данные (8 байт). Обычно берем первые 6.
                    // Ожидаем [10, 27, 44, 61, 78, 95].
                    assertEquals(
                        listOf(10, 27, 44, 61, 78, 95), actualEmission.dataPlots,
                        "Набор (C) - ожидали, что берутся первые 6 байт, остальные игнорируются"
                    )
                }

                3 -> {
                    // (D) dataCode=0x99. BLEParser, скорее всего, не обновил plotArrayFlow (или обновил мусором).
                    // Для простоты считаем, что парсер не записал данные (или оставил dataPlots пустым).
                    // Подставь свою логику, если нужно другое поведение.
                    assertTrue(
                        actualEmission.dataPlots.isEmpty(),
                        "Набор (D) - ожидали, что при неверном dataCode парсер проигнорирует обновление"
                    )
                }

                4 -> {
                    // (E) Пустой массив, ничего не должно упасть, но и никаких данных не будет.
                    assertTrue(
                        actualEmission.dataPlots.isEmpty(),
                        "Набор (E) - при пустом ByteArray не должно быть данных в plotArrayFlow"
                    )
                }
            }
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

            assertTrue("data должно быть непустым", param.data.isNotEmpty())

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

            assertTrue("data должно быть непустым", param.data.isNotEmpty())
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

            assertTrue("data должно быть непустым", param.data.isNotEmpty())

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

            assertTrue("data должно быть непустым", param.data.isNotEmpty())

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
            assertTrue("data должно быть непустым", param.data.isNotEmpty())
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
            assertTrue("data должно быть непустым", param.data.isNotEmpty())
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
            assertTrue("data должно быть непустым", param.data.isNotEmpty())

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

            assertTrue("data должно быть непустым", param.data.isNotEmpty())
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
            assertTrue("data должно быть непустым", param.data.isNotEmpty())

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

            assertTrue("data должно быть непустым", param.data.isNotEmpty())
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
            assertTrue("data должно быть непустым", param.data.isNotEmpty())

        }

//
//    @Test
//    fun `when parseWidgets is triggered, listWidgets should contain new widget`() {
//        val bleParserUnderTest = BLEParser(mockActivity)
//
//        // Изначально список пуст
//        assertEquals(0, MainActivityUBI4.listWidgets.size)
//
////        bleParserUnderTest.parseReceivedData(yourTestBytes)
//
//        val updatedList = MainActivityUBI4.listWidgets
//        assertEquals(1, updatedList.size)
//        val widget = updatedList.first()
//
//    }


    }
}