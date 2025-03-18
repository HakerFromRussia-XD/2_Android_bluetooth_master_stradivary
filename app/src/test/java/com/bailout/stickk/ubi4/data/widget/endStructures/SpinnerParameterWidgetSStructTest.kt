//package com.bailout.stickk.ubi4.data.widget.endStructures
//
//import kotlinx.serialization.json.Json
//import org.junit.jupiter.api.Assertions.*
//import org.junit.jupiter.api.Test
//
//class SpinnerParameterWidgetSStructTest{
//
//    @Test
//    fun `test spinner parameter widget S deserialization`() {
//
//
//        val baseDummy = "0".repeat(80)
//        val dataPart = "350A48656C6C6F0A576F726C64"
//        val input = "\"${baseDummy + dataPart}\""
//        val json = Json { ignoreUnknownKeys = true }
//        val result = json.decodeFromString<SpinnerParameterWidgetSStruct>(input)
//        println("result: $result")
//        assertEquals(5, result.dataSpinnerParameterWidgetSStruct.selectedIndex)
//        assertEquals(listOf("Hello", "World"), result.dataSpinnerParameterWidgetSStruct.spinnerItems)
//    }
//
//
//}