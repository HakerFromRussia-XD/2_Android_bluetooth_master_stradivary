package com.bailout.stickk.ubi4.data.widget.endStructures

import android.util.Log
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.utility.EncodeByteToHex.Companion.decodeHex
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

@Serializable(with = SpinnerParameterWidgetSSerializer::class)
data class SpinnerParameterWidgetSStruct(
    val baseParameterWidgetSStruct: BaseParameterWidgetSStruct = BaseParameterWidgetSStruct(
        BaseParameterWidgetStruct(),"NOT VALID"),
    val dataSpinnerParameterWidgetSStruct : DataSpinnerParameterWidgetSStruct = DataSpinnerParameterWidgetSStruct()
)
object SpinnerParameterWidgetSSerializer : KSerializer<SpinnerParameterWidgetSStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SpinnerParameterWidgetSSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): SpinnerParameterWidgetSStruct {
        val string = decoder.decodeString()
        val paddedString = string.padEnd(180, '0')
        println("paddedString $paddedString")
        val basePart = paddedString.substring(0, 80)
        val dataPart = paddedString.substring(80, 180)

        val baseParameterWidgetSStruct: BaseParameterWidgetSStruct =
            Json.decodeFromString("\"$basePart\"")

        val dataSpinnerParameterWidgetSStruct: DataSpinnerParameterWidgetSStruct =
            Json.decodeFromString("\"$dataPart\"")


        return SpinnerParameterWidgetSStruct(
            baseParameterWidgetSStruct = baseParameterWidgetSStruct,
            dataSpinnerParameterWidgetSStruct = dataSpinnerParameterWidgetSStruct
        )

    }

    override fun serialize(encoder: Encoder, value: SpinnerParameterWidgetSStruct) {
        encoder.encodeString("")
    }
}

@Serializable(with = DataSpinnerParameterWidgetSSerializer::class)
data class DataSpinnerParameterWidgetSStruct(
    val spinnerItems: List<String> = listOf(),
    val selectedIndex: Int = 0,
)
object DataSpinnerParameterWidgetSSerializer : KSerializer<DataSpinnerParameterWidgetSStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SpinnerParameterWidgetSSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): DataSpinnerParameterWidgetSStruct {
        val inputString = decoder.decodeString()
        val rawItems = inputString.split("0A")
        val cleanedItems = rawItems.map { it.trimEnd('0') }

        val selectedIndexHex = cleanedItems.firstOrNull().orEmpty()
        val selectedIndex = selectedIndexHex.decodeHex().toIntOrNull() ?: 0

        val spinnerItems = cleanedItems.drop(1).map { it.decodeHex() }

        return DataSpinnerParameterWidgetSStruct(
            spinnerItems = spinnerItems,
            selectedIndex = selectedIndex
        )
    }

    override fun serialize(encoder: Encoder, value: DataSpinnerParameterWidgetSStruct) {
        encoder.encodeString("")
    }

}