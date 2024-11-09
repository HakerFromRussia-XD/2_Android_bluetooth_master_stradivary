package com.bailout.stickk.ubi4.data.local


import android.util.Log
import com.bailout.stickk.ubi4.data.FullInicializeConnectionSerializer
import com.bailout.stickk.ubi4.utility.CastBytesToFloat
import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = OpticTrainingSerializer::class)
data class OpticTrainingStruct(
    val numberOfFrame: Long = 0L,
    val data: ArrayList<Float> = arrayListOf()

)

object OpticTrainingSerializer : KSerializer<OpticTrainingStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FullInicializeConnection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): OpticTrainingStruct {
        val string = decoder.decodeString()
        var numberOfFrame = 0L
        var data = arrayListOf<Float>()


        Log.d("TestOptic","OpticTrainingStruct length = ${string.length}")
        if (string.length >= 808) {
             numberOfFrame = castUnsignedCharToInt(string.substring(0, 2).toInt(16).toByte()).toLong() +
                             castUnsignedCharToInt(string.substring(2, 4).toInt(16).toByte()).toLong()*256 +
                             castUnsignedCharToInt(string.substring(4, 6).toInt(16).toByte()).toLong()*256*256 +
                             castUnsignedCharToInt(string.substring(6, 8).toInt(16).toByte()).toLong()*256*256*256


            val byteList  = mutableListOf<Byte>()
            for (i in 0 until 400) {
                val oneByteValue = string.substring(i * 2 + 8, i * 2 + 10).toInt(16).toByte()
                byteList.add(oneByteValue)
            }
            val byteArrayData = byteList.toByteArray()

            data = CastBytesToFloat.castBytesToFloatArray(byteArrayData)
            Log.d("OpticTrainingSerializer", "data :$data")
            Log.d("OpticTrainingSerializer", "numberOfFrame :$numberOfFrame")

        }

        return OpticTrainingStruct(
            numberOfFrame = numberOfFrame,
            data = data
        )
    }
    override fun serialize(encoder: Encoder, value: OpticTrainingStruct) {


    }

}
