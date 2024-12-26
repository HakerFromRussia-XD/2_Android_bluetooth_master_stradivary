package com.bailout.stickk.ubi4.data.widget.endStructures

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

@Serializable(with = ThresholdParameterWidgetSSerializer::class)
data class ThresholdParameterWidgetSStruct(
    val baseParameterWidgetSStruct: BaseParameterWidgetSStruct = BaseParameterWidgetSStruct(),
    val openThresholdUpper : Int = 0,
    val openThresholdLower : Int = 0,
    val closeThresholdUpper : Int = 0,
    val closeThresholdLower : Int = 0,
)

object ThresholdParameterWidgetSSerializer: KSerializer<ThresholdParameterWidgetSStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FullInicializeConnection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ThresholdParameterWidgetSStruct {
        val string = decoder.decodeString()
        var baseParameterWidgetSStruct = BaseParameterWidgetSStruct(BaseParameterWidgetStruct(),"NOT VALID")
        var openThresholdUpper = 0
        var openThresholdLower = 0
        var closeThresholdUpper = 0
        var closeThresholdLower = 0


        if (string.length >= 88) {
            baseParameterWidgetSStruct = Json.decodeFromString<BaseParameterWidgetSStruct>("\"${string.substring(0, 80)}\"")
            openThresholdUpper = castUnsignedCharToInt(string.substring(80, 82).toInt(16).toByte())
            openThresholdLower = castUnsignedCharToInt(string.substring(82, 84).toInt(16).toByte())
            closeThresholdUpper = castUnsignedCharToInt(string.substring(84, 86).toInt(16).toByte())
            closeThresholdLower = castUnsignedCharToInt(string.substring(86, 88).toInt(16).toByte())
        }

        return ThresholdParameterWidgetSStruct (
            baseParameterWidgetSStruct = baseParameterWidgetSStruct,
            openThresholdUpper = openThresholdUpper,
            openThresholdLower = openThresholdLower,
            closeThresholdUpper = closeThresholdUpper,
            closeThresholdLower = closeThresholdLower,
        )
    }

    override fun serialize(encoder: Encoder, value: ThresholdParameterWidgetSStruct) {
        val code = ""
        encoder.encodeString("$code")
    }

}