package com.bailout.stickk.ubi4.data.local

import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import com.bailout.stickk.ubi4.utility.logging.platformLog
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = PlotThresholdsSerializer::class)
data class PlotThresholds(
    var threshold1: Int = 0,
    var threshold2: Int = 0,
    var threshold3: Int = 0,
    var threshold4: Int = 0,
    var threshold5: Int = 0,
    var threshold6: Int = 0,
)

object PlotThresholdsSerializer: KSerializer<PlotThresholds> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PlotThresholdsSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): PlotThresholds {
        val string = decoder.decodeString()
        platformLog("PlotThresholds", "string $string")
        // Дополняем строку до 4 символов справа символом '0'.
        val paddedString = string.padEnd(22, '0')
        
        val threshold1 = castUnsignedCharToInt(paddedString.substring(0, 2).toInt(16).toByte())
        val threshold2 = castUnsignedCharToInt(paddedString.substring(4, 6).toInt(16).toByte())
        val threshold3 = castUnsignedCharToInt(paddedString.substring(8, 10).toInt(16).toByte())
        val threshold4 = castUnsignedCharToInt(paddedString.substring(12, 14).toInt(16).toByte())
        val threshold5 = castUnsignedCharToInt(paddedString.substring(16, 18).toInt(16).toByte())
        val threshold6 = castUnsignedCharToInt(paddedString.substring(20, 22).toInt(16).toByte())
            

        return PlotThresholds (
            threshold1 =  threshold1,
            threshold2 =  threshold2,
            threshold3 =  threshold3,
            threshold4 =  threshold4,
            threshold5 =  threshold5,
            threshold6 =  threshold6,
        )
    }

    override fun serialize(encoder: Encoder, value: PlotThresholds) {
        val code = ""
        encoder.encodeString("$code")
    }
}

