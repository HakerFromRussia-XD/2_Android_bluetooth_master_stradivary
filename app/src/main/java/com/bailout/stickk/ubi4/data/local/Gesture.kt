package com.bailout.stickk.ubi4.data.local

import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = GestureSerializer::class)
data class Gesture (
    val gestureId: Int,
    val openPosition1: Int, val openPosition2: Int,
    val openPosition3: Int, val openPosition4: Int,
    val openPosition5: Int, val openPosition6: Int,
    val closePosition1: Int, val closePosition2: Int,
    val closePosition3: Int, val closePosition4: Int,
    val closePosition5: Int, val closePosition6: Int,
    val openToCloseTimeShift1: Int, val openToCloseTimeShift2: Int,
    val openToCloseTimeShift3: Int, val openToCloseTimeShift4: Int,
    val openToCloseTimeShift5: Int, val openToCloseTimeShift6: Int,
    val closeToOpenTimeShift1: Int, val closeToOpenTimeShift2: Int,
    val closeToOpenTimeShift3: Int, val closeToOpenTimeShift4: Int,
    val closeToOpenTimeShift5: Int, val closeToOpenTimeShift6: Int
)

object GestureSerializer: KSerializer<Gesture> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FullInicializeConnection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Gesture {
        val string = decoder.decodeString()
        var gestureId = 0

        var openPosition1 = 0
        var openPosition2 = 0
        var openPosition3 = 0
        var openPosition4 = 0
        var openPosition5 = 0
        var openPosition6 = 0

        var closePosition1 = 0
        var closePosition2 = 0
        var closePosition3 = 0
        var closePosition4 = 0
        var closePosition5 = 0
        var closePosition6 = 0

        var openToCloseTimeShift1 = 0
        var openToCloseTimeShift2 = 0
        var openToCloseTimeShift3 = 0
        var openToCloseTimeShift4 = 0
        var openToCloseTimeShift5 = 0
        var openToCloseTimeShift6 = 0

        var closeToOpenTimeShift1 = 0
        var closeToOpenTimeShift2 = 0
        var closeToOpenTimeShift3 = 0
        var closeToOpenTimeShift4 = 0
        var closeToOpenTimeShift5 = 0
        var closeToOpenTimeShift6 = 0


        if (string.length >= 50) {
            gestureId =         castUnsignedCharToInt(string.substring(0, 2).toInt(16).toByte())

            openPosition1 =     castUnsignedCharToInt(string.substring(2, 4).toInt(16).toByte())
            openPosition2 =     castUnsignedCharToInt(string.substring(4, 6).toInt(16).toByte())
            openPosition3 =     castUnsignedCharToInt(string.substring(6, 8).toInt(16).toByte())
            openPosition4 =     castUnsignedCharToInt(string.substring(8,  10).toInt(16).toByte())
            openPosition5 =     castUnsignedCharToInt(string.substring(10, 12).toInt(16).toByte())
            openPosition6 =     castUnsignedCharToInt(string.substring(12, 14).toInt(16).toByte())

            closePosition1 =    castUnsignedCharToInt(string.substring(14, 16).toInt(16).toByte())
            closePosition2 =    castUnsignedCharToInt(string.substring(16, 18).toInt(16).toByte())
            closePosition3 =    castUnsignedCharToInt(string.substring(18, 20).toInt(16).toByte())
            closePosition4 =    castUnsignedCharToInt(string.substring(20, 22).toInt(16).toByte())
            closePosition5 =    castUnsignedCharToInt(string.substring(22, 24).toInt(16).toByte())
            closePosition6 =    castUnsignedCharToInt(string.substring(24, 26).toInt(16).toByte())

            openToCloseTimeShift1 =    castUnsignedCharToInt(string.substring(26, 28).toInt(16).toByte())
            openToCloseTimeShift2 =    castUnsignedCharToInt(string.substring(28, 30).toInt(16).toByte())
            openToCloseTimeShift3 =    castUnsignedCharToInt(string.substring(30, 32).toInt(16).toByte())
            openToCloseTimeShift4 =    castUnsignedCharToInt(string.substring(32, 34).toInt(16).toByte())
            openToCloseTimeShift5 =    castUnsignedCharToInt(string.substring(34, 36).toInt(16).toByte())
            openToCloseTimeShift6 =    castUnsignedCharToInt(string.substring(36, 38).toInt(16).toByte())

            closeToOpenTimeShift1 =    castUnsignedCharToInt(string.substring(38, 40).toInt(16).toByte())
            closeToOpenTimeShift2 =    castUnsignedCharToInt(string.substring(40, 42).toInt(16).toByte())
            closeToOpenTimeShift3 =    castUnsignedCharToInt(string.substring(42, 44).toInt(16).toByte())
            closeToOpenTimeShift4 =    castUnsignedCharToInt(string.substring(44, 46).toInt(16).toByte())
            closeToOpenTimeShift5 =    castUnsignedCharToInt(string.substring(46, 48).toInt(16).toByte())
            closeToOpenTimeShift6 =    castUnsignedCharToInt(string.substring(48, 50).toInt(16).toByte())
        }

        return Gesture (
            gestureId = gestureId,

            openPosition1 =  openPosition1,
            openPosition2 =  openPosition2,
            openPosition3 =  openPosition3,
            openPosition4 =  openPosition4,
            openPosition5 =  openPosition5,
            openPosition6 =  openPosition6,

            closePosition1 = closePosition1,
            closePosition2 = closePosition2,
            closePosition3 = closePosition3,
            closePosition4 = closePosition4,
            closePosition5 = closePosition5,
            closePosition6 = closePosition6,

            openToCloseTimeShift1 = openToCloseTimeShift1,
            openToCloseTimeShift2 = openToCloseTimeShift2,
            openToCloseTimeShift3 = openToCloseTimeShift3,
            openToCloseTimeShift4 = openToCloseTimeShift4,
            openToCloseTimeShift5 = openToCloseTimeShift5,
            openToCloseTimeShift6 = openToCloseTimeShift6,

            closeToOpenTimeShift1 = closeToOpenTimeShift1,
            closeToOpenTimeShift2 = closeToOpenTimeShift2,
            closeToOpenTimeShift3 = closeToOpenTimeShift3,
            closeToOpenTimeShift4 = closeToOpenTimeShift4,
            closeToOpenTimeShift5 = closeToOpenTimeShift5,
            closeToOpenTimeShift6 = closeToOpenTimeShift6
        )
    }

    override fun serialize(encoder: Encoder, value: Gesture) {
        val code = ""
        encoder.encodeString("$code")
    }
}
