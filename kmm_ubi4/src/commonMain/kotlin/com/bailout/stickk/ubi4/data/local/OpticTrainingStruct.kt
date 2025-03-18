package com.bailout.stickk.ubi4.data.local


import com.bailout.stickk.ubi4.logging.platformLog
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

        platformLog("TestOptic","OpticTrainingStruct length = ${string.length}")


        if (string.length >= 8) {
             numberOfFrame = castUnsignedCharToInt(string.substring(0, 2).toInt(16).toByte()).toLong() +
                             castUnsignedCharToInt(string.substring(2, 4).toInt(16).toByte()).toLong()*256 +
                             castUnsignedCharToInt(string.substring(4, 6).toInt(16).toByte()).toLong()*256*256 +
                             castUnsignedCharToInt(string.substring(6, 8).toInt(16).toByte()).toLong()*256*256*256


            val totalBytes = string.length / 2
            val dataBytesCount = totalBytes - 4 //вычитаем первые 4 байта под numberOfFrame

            //просто берем целочисленное деление
            val floatCount = dataBytesCount / 4

            if (floatCount > 0) {
                val usedBytesCount = floatCount * 4  // реальное количество используемых байт
                val byteList = mutableListOf<Byte>()

                for (i in 0 until usedBytesCount) {
                    val startIndex = 8 + i * 2
                    val endIndex = startIndex + 2
                    val oneByteValue = string.substring(startIndex, endIndex).toInt(16).toByte()
                    byteList.add(oneByteValue)
                }

                val byteArrayData = byteList.toByteArray()
                data = (arrayListOf(numberOfFrame) + CastBytesToFloat.castBytesToFloatArray(byteArrayData)) as ArrayList<Float>

                platformLog("OpticTrainingSerializer", "data :$data")
                platformLog("OpticTrainingSerializer", "numberOfFrame :$numberOfFrame")
            }
        }

        return OpticTrainingStruct(
            numberOfFrame = numberOfFrame,
            data = data
        )
    }

    override fun serialize(encoder: Encoder, value: OpticTrainingStruct) {


    }

}


 //        string.length/2 div 4 = 18 = x +1 - целое кол-во 4х байтных кусков в пришедшей посылке
 //       (x +1) * 4 * 2 = string.length new - data.lenght new
 //       нужно что бы string.lenght регулировал размер data(выходной массив)

//(x - кол во полезных Float)