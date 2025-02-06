package com.bailout.stickk.ubi4.data.widget.endStructures

import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
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
    val dataSpinnerParameterWidgetStruct : DataSpinnerParameterWidgetStruct = DataSpinnerParameterWidgetStruct()
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

        val dataSpinnerParameterWidgetStruct: DataSpinnerParameterWidgetStruct =
            Json.decodeFromString("\"$dataPart\"")


        return SpinnerParameterWidgetSStruct(
            baseParameterWidgetSStruct = baseParameterWidgetSStruct,
            dataSpinnerParameterWidgetStruct = dataSpinnerParameterWidgetStruct
        )

    }

    override fun serialize(encoder: Encoder, value: SpinnerParameterWidgetSStruct) {
        encoder.encodeString("")
    }
}