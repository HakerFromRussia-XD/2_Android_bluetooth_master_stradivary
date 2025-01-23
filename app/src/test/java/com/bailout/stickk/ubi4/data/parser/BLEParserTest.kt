package com.bailout.stickk.ubi4.data.parser

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bailout.stickk.ubi4.ble.ParameterProvider
import com.bailout.stickk.ubi4.data.BaseParameterInfoStruct
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4
import com.bailout.stickk.ubi4.ui.main.MainActivityUBI4
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import io.mockk.verify
import junit.framework.TestCase.assertEquals

class BLEParserTest {
    @Mock
    private lateinit var mockActivity: AppCompatActivity // Замените на реальный тип
    private lateinit var bleParser: BLEParser
    private val mockMain = mock<MainActivityUBI4>()
    private val mockProvider = mock<ParameterProvider>()
    private val mockRxUpdate = mock<RxUpdateMainEventUbi4>()


    @BeforeEach
    fun setup() {
        MainActivityUBI4.baseParametrInfoStructArray = ArrayList()
        MockitoAnnotations.openMocks(this)
        bleParser = BLEParser(mockActivity) // Replace with actual initialization logic
        mockkObject(ParameterProvider.Companion)
        mockkStatic(android.util.Log::class)
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
}