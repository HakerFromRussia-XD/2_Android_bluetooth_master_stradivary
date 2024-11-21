package com.bailout.stickk.ubi4.data.widget.endStructures

import android.util.Pair
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetSStruct
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetStruct
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

@Serializable(with = SwitchParameterWidgetSSerializer::class)
data class SwitchParameterWidgetSStruct(
    val baseParameterWidgetSStruct: BaseParameterWidgetSStruct,
    val switchChecked: Boolean = false
)

object SwitchParameterWidgetSSerializer: KSerializer<SwitchParameterWidgetSStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FullInicializeConnection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): SwitchParameterWidgetSStruct {
        val string = decoder.decodeString()
        val baseParameterWidgetSStruct: BaseParameterWidgetSStruct
        var switchChecked = false



        if (string.length >= 82) {
            baseParameterWidgetSStruct = Json.decodeFromString<BaseParameterWidgetSStruct>("\"${string.substring(0, 80)}\"")
            switchChecked = castUnsignedCharToInt(string.substring(80, 82).toInt(16).toByte()) !=0

        } else {
            baseParameterWidgetSStruct = BaseParameterWidgetSStruct (
                BaseParameterWidgetStruct(0, 0, 0, 0, 0, 0, 0, 0, 0, mutableSetOf(
                    Pair(0,0)
                )),"")
        }

        return SwitchParameterWidgetSStruct (
            baseParameterWidgetSStruct = baseParameterWidgetSStruct,
            switchChecked = switchChecked

        )
    }

    override fun serialize(encoder: Encoder, value: SwitchParameterWidgetSStruct) {
        val code = ""
        encoder.encodeString("$code")
    }

}