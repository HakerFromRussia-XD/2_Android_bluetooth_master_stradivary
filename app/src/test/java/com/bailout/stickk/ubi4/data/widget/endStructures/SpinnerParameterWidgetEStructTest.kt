package com.bailout.stickk.ubi4.data.widget.endStructures

import android.util.Log
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SpinnerParameterWidgetEStructTest {

    @Test
    fun `test spinner parameter widget E deserialization`() {


        val baseDummy = "0".repeat(18)
        val dataPart = "350A48656C6C6F0A576F726C64"
        val input = "\"${baseDummy + dataPart}\""
        val json = Json { ignoreUnknownKeys = true }
        val result = json.decodeFromString<SpinnerParameterWidgetEStruct>(input)
        // Проверяем, что список элементов соответствует ожиданиям
        assertEquals(listOf("Hello", "World"), result.dataSpinnerParameterWidgetEStruct.spinnerItems)
        // Проверяем, что выбранный индекс равен 5
        assertEquals(5, result.dataSpinnerParameterWidgetEStruct.selectedIndex)
    }


}