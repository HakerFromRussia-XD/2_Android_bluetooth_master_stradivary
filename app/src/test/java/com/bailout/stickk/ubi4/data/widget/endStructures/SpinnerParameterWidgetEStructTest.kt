package com.bailout.stickk.ubi4.data.widget.endStructures

import android.util.Log
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SpinnerParameterWidgetEStructTest {

    @Test
    fun `test spinner parameter widget E deserialization`() {


        val input = "\"48656C6C6F0A576F726C640A35\""
        val json = Json { ignoreUnknownKeys = true }
        val result = json.decodeFromString<SpinnerParameterWidgetEStruct>(input)
        println("result: $result")
        Log.d("TestSpinnerParameter", "result: $result")

        // Проверяем, что список элементов соответствует ожиданиям
        assertEquals(listOf("Hello", "World"), result.spinnerItems)
        // Проверяем, что выбранный индекс равен 5
        assertEquals(5, result.selectedIndex)
    }



}