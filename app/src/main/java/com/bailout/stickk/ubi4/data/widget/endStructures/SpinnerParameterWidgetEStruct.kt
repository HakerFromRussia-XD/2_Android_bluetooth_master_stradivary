package com.bailout.stickk.ubi4.data.widget.endStructures

import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
import com.bailout.stickk.ubi4.utility.EncodeByteToHex.Companion.decodeHex
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

@Serializable(with = SpinnerParameterWidgetESerializer::class)
data class SpinnerParameterWidgetEStruct(
    val baseParameterWidgetEStruct: BaseParameterWidgetEStruct = BaseParameterWidgetEStruct(),
    val dataSpinnerParameterWidgetStruct : DataSpinnerParameterWidgetStruct = DataSpinnerParameterWidgetStruct()
)
object SpinnerParameterWidgetESerializer : KSerializer<SpinnerParameterWidgetEStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SpinnerParameterWidgetESerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): SpinnerParameterWidgetEStruct {
        val string = decoder.decodeString()
        val paddedString = string.padEnd(118, '0')
        println("paddedString $paddedString")
        val basePart = paddedString.substring(0, 18)
        val dataPart = paddedString.substring(18, 118)

        val baseParameterWidgetEStruct: BaseParameterWidgetEStruct =
            Json.decodeFromString("\"$basePart\"")

        val dataSpinnerParameterWidgetStruct: DataSpinnerParameterWidgetStruct =
            Json.decodeFromString("\"$dataPart\"")


        return SpinnerParameterWidgetEStruct(
            baseParameterWidgetEStruct = baseParameterWidgetEStruct,
            dataSpinnerParameterWidgetStruct = dataSpinnerParameterWidgetStruct
        )

    }

    override fun serialize(encoder: Encoder, value: SpinnerParameterWidgetEStruct) {
        encoder.encodeString("")
    }
}

@Serializable(with = DataSpinnerParameterWidgetSerializer::class)
data class DataSpinnerParameterWidgetStruct(
    val spinnerItems: List<String> = listOf(),
    val selectedIndex: Int = 0,
)
object DataSpinnerParameterWidgetSerializer : KSerializer<DataSpinnerParameterWidgetStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SpinnerParameterWidgetSSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): DataSpinnerParameterWidgetStruct {
        val inputString = decoder.decodeString()
        val rawItems = inputString.split("0A")
        val cleanedItems = rawItems.map { it.trimEnd('0') }

        val selectedIndexHex = cleanedItems.firstOrNull().orEmpty()
        val selectedIndex = selectedIndexHex.decodeHex().toIntOrNull() ?: 0

        val spinnerItems = cleanedItems.drop(1).map { it.decodeHex() }

        return DataSpinnerParameterWidgetStruct(
            spinnerItems = spinnerItems,
            selectedIndex = selectedIndex
        )
    }

    override fun serialize(encoder: Encoder, value: DataSpinnerParameterWidgetStruct) {
        encoder.encodeString("")
    }

}