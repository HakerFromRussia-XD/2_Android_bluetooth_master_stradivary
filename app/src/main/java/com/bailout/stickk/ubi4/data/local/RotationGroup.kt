package com.bailout.stickk.ubi4.data.local

import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = RotationGroupSerializer::class)
data class RotationGroup (
    var gesture1Id: Int = 0, var gesture1ImageId: Int = 0,
    var gesture2Id: Int = 0, var gesture2ImageId: Int = 0,
    var gesture3Id: Int = 0, var gesture3ImageId: Int = 0,
    var gesture4Id: Int = 0, var gesture4ImageId: Int = 0,
    var gesture5Id: Int = 0, var gesture5ImageId: Int = 0,
    var gesture6Id: Int = 0, var gesture6ImageId: Int = 0,
    var gesture7Id: Int = 0, var gesture7ImageId: Int = 0,
    var gesture8Id: Int = 0, var gesture8ImageId: Int = 0,
) {
    fun toGestureList(): List<Pair<Int, Int>> {
        return listOf(
            gesture1Id to gesture1ImageId,
            gesture2Id to gesture2ImageId,
            gesture3Id to gesture3ImageId,
            gesture4Id to gesture4ImageId,
            gesture5Id to gesture5ImageId,
            gesture6Id to gesture6ImageId,
            gesture7Id to gesture7ImageId,
            gesture8Id to gesture8ImageId
        )
    }
}

object RotationGroupSerializer: KSerializer<RotationGroup> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("RotationGroupSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): RotationGroup {
        val string = decoder.decodeString()

        var gesture1Id = 0
        var gesture2Id = 0
        var gesture3Id = 0
        var gesture4Id = 0
        var gesture5Id = 0
        var gesture6Id = 0
        var gesture7Id = 0
        var gesture8Id = 0

        var gesture1ImageId = 0
        var gesture2ImageId = 0
        var gesture3ImageId = 0
        var gesture4ImageId = 0
        var gesture5ImageId = 0
        var gesture6ImageId = 0
        var gesture7ImageId = 0
        var gesture8ImageId = 0

        if (string.length >= 32) {
            gesture1Id      =     castUnsignedCharToInt(string.substring(0, 2).toInt(16).toByte())
            gesture1ImageId =     castUnsignedCharToInt(string.substring(2, 4).toInt(16).toByte())
            gesture2Id =     castUnsignedCharToInt(string.substring(4, 6).toInt(16).toByte())
            gesture2ImageId =     castUnsignedCharToInt(string.substring(6, 8).toInt(16).toByte())
            gesture3Id =     castUnsignedCharToInt(string.substring(8,  10).toInt(16).toByte())
            gesture3ImageId =     castUnsignedCharToInt(string.substring(10, 12).toInt(16).toByte())
            gesture4Id =     castUnsignedCharToInt(string.substring(12, 14).toInt(16).toByte())
            gesture4ImageId =    castUnsignedCharToInt(string.substring(14, 16).toInt(16).toByte())
            gesture5Id =    castUnsignedCharToInt(string.substring(16, 18).toInt(16).toByte())
            gesture5ImageId =    castUnsignedCharToInt(string.substring(18, 20).toInt(16).toByte())
            gesture6Id =    castUnsignedCharToInt(string.substring(20, 22).toInt(16).toByte())
            gesture6ImageId =    castUnsignedCharToInt(string.substring(22, 24).toInt(16).toByte())
            gesture7Id =    castUnsignedCharToInt(string.substring(24, 26).toInt(16).toByte())
            gesture7ImageId =    castUnsignedCharToInt(string.substring(26, 28).toInt(16).toByte())
            gesture8Id =    castUnsignedCharToInt(string.substring(28, 30).toInt(16).toByte())
            gesture8ImageId =    castUnsignedCharToInt(string.substring(30, 32).toInt(16).toByte())
        }

        return RotationGroup (
            gesture1Id =  gesture1Id,
            gesture2Id =  gesture2Id,
            gesture3Id =  gesture3Id,
            gesture4Id =  gesture4Id,
            gesture5Id =  gesture5Id,
            gesture6Id =  gesture6Id,
            gesture7Id =  gesture7Id,
            gesture8Id =  gesture8Id,

            gesture1ImageId = gesture1ImageId,
            gesture2ImageId = gesture2ImageId,
            gesture3ImageId = gesture3ImageId,
            gesture4ImageId = gesture4ImageId,
            gesture5ImageId = gesture5ImageId,
            gesture6ImageId = gesture6ImageId,
            gesture7ImageId = gesture7ImageId,
            gesture8ImageId = gesture8ImageId,
        )
    }

    override fun serialize(encoder: Encoder, value: RotationGroup) {
        val code = ""
        encoder.encodeString("$code")
    }
}
