package com.bailout.stickk.ubi4.data.local

import com.bailout.stickk.ubi4.models.BindingGestureItem
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


@Serializable(with = BindingGroupSerializer::class)
data class BindingGestureGroup(
    var gestureSpr1Id: Int = 0, var gesture1Id: Int = 0,
    var gestureSpr2Id: Int = 0, var gesture2Id: Int = 0,
    var gestureSpr3Id: Int = 0, var gesture3Id: Int = 0,
    var gestureSpr4Id: Int = 0, var gesture4Id: Int = 0,
    var gestureSpr5Id: Int = 0, var gesture5Id: Int = 0,
    var gestureSpr6Id: Int = 0, var gesture6Id: Int = 0,
    var gestureSpr7Id: Int = 0, var gesture7Id: Int = 0,
    var gestureSpr8Id: Int = 0, var gesture8Id: Int = 0,
    var gestureSpr9Id: Int = 0, var gesture9Id: Int = 0,
    var gestureSpr10Id: Int = 0, var gesture10Id: Int = 0,
    var gestureSpr11Id: Int = 0, var gesture11Id: Int = 0,
    var gestureSpr12Id: Int = 0, var gesture12Id: Int = 0,
) {
    fun toGestureList(): List<Pair<Int, Int>> {
        return listOf(
            gestureSpr1Id to gesture1Id,
            gestureSpr2Id to gesture2Id,
            gestureSpr3Id to gesture3Id,
            gestureSpr4Id to gesture4Id,
            gestureSpr5Id to gesture5Id,
            gestureSpr6Id to gesture6Id,
            gestureSpr7Id to gesture7Id,
            gestureSpr8Id to gesture8Id,
            gestureSpr9Id to gesture9Id,
            gestureSpr10Id to gesture10Id,
            gestureSpr11Id to gesture11Id,
            gestureSpr12Id to gesture12Id,
        )
    }
}


object BindingGroupSerializer : KSerializer<BindingGestureGroup> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BindingGroupSerializer", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BindingGestureGroup {
        val string = decoder.decodeString()

        var gestureSpr1Id = 0
        var gestureSpr2Id = 0
        var gestureSpr3Id = 0
        var gestureSpr4Id = 0
        var gestureSpr5Id = 0
        var gestureSpr6Id = 0
        var gestureSpr7Id = 0
        var gestureSpr8Id = 0
        var gestureSpr9Id = 0
        var gestureSpr10Id = 0
        var gestureSpr11Id = 0
        var gestureSpr12Id = 0

        var gesture1Id = 0
        var gesture2Id = 0
        var gesture3Id = 0
        var gesture4Id = 0
        var gesture5Id = 0
        var gesture6Id = 0
        var gesture7Id = 0
        var gesture8Id = 0
        var gesture9Id = 0
        var gesture10Id = 0
        var gesture11Id = 0
        var gesture12Id = 0

        if (string.length >= 52) {
            gestureSpr1Id = castUnsignedCharToInt(string.substring(0, 2).toInt(16).toByte())
            gesture1Id = castUnsignedCharToInt(string.substring(2, 4).toInt(16).toByte())
            gestureSpr2Id = castUnsignedCharToInt(string.substring(4, 6).toInt(16).toByte())
            gesture2Id = castUnsignedCharToInt(string.substring(6, 8).toInt(16).toByte())
            gestureSpr3Id = castUnsignedCharToInt(string.substring(8, 10).toInt(16).toByte())
            gesture3Id = castUnsignedCharToInt(string.substring(10, 12).toInt(16).toByte())
            gestureSpr4Id = castUnsignedCharToInt(string.substring(12, 14).toInt(16).toByte())
            gesture4Id = castUnsignedCharToInt(string.substring(14, 16).toInt(16).toByte())
            gestureSpr5Id = castUnsignedCharToInt(string.substring(16, 18).toInt(16).toByte())
            gesture5Id = castUnsignedCharToInt(string.substring(18, 20).toInt(16).toByte())
            gestureSpr6Id = castUnsignedCharToInt(string.substring(20, 22).toInt(16).toByte())
            gesture6Id = castUnsignedCharToInt(string.substring(22, 24).toInt(16).toByte())
            gestureSpr7Id = castUnsignedCharToInt(string.substring(24, 26).toInt(16).toByte())
            gesture7Id = castUnsignedCharToInt(string.substring(26, 28).toInt(16).toByte())
            gestureSpr8Id = castUnsignedCharToInt(string.substring(28, 30).toInt(16).toByte())
            gesture8Id = castUnsignedCharToInt(string.substring(30, 32).toInt(16).toByte())
            gestureSpr9Id = castUnsignedCharToInt(string.substring(32, 34).toInt(16).toByte())
            gesture9Id = castUnsignedCharToInt(string.substring(34, 36).toInt(16).toByte())
            gestureSpr10Id = castUnsignedCharToInt(string.substring(36, 38).toInt(16).toByte())
            gesture10Id = castUnsignedCharToInt(string.substring(38, 40).toInt(16).toByte())
            gestureSpr11Id = castUnsignedCharToInt(string.substring(40, 42).toInt(16).toByte())
            gesture11Id = castUnsignedCharToInt(string.substring(42, 44).toInt(16).toByte())
            gestureSpr12Id = castUnsignedCharToInt(string.substring(44, 46).toInt(16).toByte())
            gesture12Id = castUnsignedCharToInt(string.substring(46, 48).toInt(16).toByte())

        }

        return BindingGestureGroup (

            gestureSpr1Id = gestureSpr1Id,
            gestureSpr2Id = gestureSpr2Id,
            gestureSpr3Id = gestureSpr3Id,
            gestureSpr4Id = gestureSpr4Id,
            gestureSpr5Id = gestureSpr5Id,
            gestureSpr6Id = gestureSpr6Id,
            gestureSpr7Id = gestureSpr7Id,
            gestureSpr8Id = gestureSpr8Id,
            gestureSpr9Id = gestureSpr9Id,
            gestureSpr10Id = gestureSpr10Id,
            gestureSpr11Id = gestureSpr11Id,
            gestureSpr12Id = gestureSpr12Id,

            gesture1Id = gesture1Id,
            gesture2Id = gesture2Id,
            gesture3Id = gesture3Id,
            gesture4Id = gesture4Id,
            gesture5Id = gesture5Id,
            gesture6Id = gesture6Id,
            gesture7Id = gesture7Id,
            gesture8Id = gesture8Id,
            gesture9Id = gesture9Id,
            gesture10Id = gesture10Id,
            gesture11Id = gesture11Id,
            gesture12Id = gesture12Id,
        )
    }

    override fun serialize(encoder: Encoder, value: BindingGestureGroup) {
        val code = ""
        encoder.encodeString("$code")
    }



}
