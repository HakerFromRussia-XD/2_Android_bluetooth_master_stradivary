package com.bailout.stickk.ubi4.data.widget.endStructures

import android.util.Pair
import com.bailout.stickk.ubi4.data.widget.subStructures.BaseParameterWidgetEStruct
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

@Serializable(with = ThresholdParameterWidgetESerializer::class)
data class ThresholdParameterWidgetEStruct(
    val baseParameterWidgetEStruct: BaseParameterWidgetEStruct = BaseParameterWidgetEStruct(),
    val openThresholdUpper : Int = 0,
    val openThresholdLower : Int = 0,
    val closeThresholdUpper : Int = 0,
    val closeThresholdLower : Int = 0,
)

object ThresholdParameterWidgetESerializer: KSerializer<ThresholdParameterWidgetEStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FullInicializeConnection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): ThresholdParameterWidgetEStruct {
        val string = decoder.decodeString()
        var baseParameterWidgetEStruct = BaseParameterWidgetEStruct()
        var openThresholdUpper = 0
        var openThresholdLower = 0
        var closeThresholdUpper = 0
        var closeThresholdLower = 0


        if (string.length >= 26) {
            baseParameterWidgetEStruct = Json.decodeFromString<BaseParameterWidgetEStruct>("\"${string.substring(0, 18)}\"")
            openThresholdUpper = castUnsignedCharToInt(string.substring(18, 20).toInt(16).toByte())
            openThresholdLower = castUnsignedCharToInt(string.substring(20, 22).toInt(16).toByte())
            closeThresholdUpper = castUnsignedCharToInt(string.substring(22, 24).toInt(16).toByte())
            closeThresholdLower = castUnsignedCharToInt(string.substring(24, 26).toInt(16).toByte())
        }

        return ThresholdParameterWidgetEStruct (
            baseParameterWidgetEStruct = baseParameterWidgetEStruct,
            openThresholdUpper = openThresholdUpper,
            openThresholdLower = openThresholdLower,
            closeThresholdUpper = closeThresholdUpper,
            closeThresholdLower = closeThresholdLower,
        )
    }

    override fun serialize(encoder: Encoder, value: ThresholdParameterWidgetEStruct) {
        val code = ""
        encoder.encodeString("$code")
    }

}