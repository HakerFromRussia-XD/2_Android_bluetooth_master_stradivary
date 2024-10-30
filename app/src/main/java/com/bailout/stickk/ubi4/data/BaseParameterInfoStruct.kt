package com.bailout.stickk.ubi4.data

import com.bailout.stickk.ubi4.utility.CastToUnsignedInt.Companion.castUnsignedCharToInt
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


@Serializable(with = BaseParametrInfoSerializer::class)
data class BaseParameterInfoStruct(
    val ID: Int,
    val broadcastID: Int,
    val dataCode: Int,
    val dataInstance: Int,
    val parametrSize: Int,//2 байта
    val flagShift: Int,
    val optimisation: Int,
    val valueLimit: Int,

    val initRead: Int,      //1 bit
    val initWrite: Int,     //1 bit
    val synchType: Int,     //3 bit
    val synchDirection: Int,//3 bit
    val synchPeriod: Int,

    val type: Int,
    val saveInMaster: Int,//1 bit
    val saveInSlave: Int, //1 bit
    val saveReserv: Int,  //6 bit

    val additionalInfoSize: Int,

    val relatedParametrID: Int,
    val relatedDataCode: Int,

    var data: String)

object BaseParametrInfoSerializer: KSerializer<BaseParameterInfoStruct> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FullInicializeConnection", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BaseParameterInfoStruct {
        val string = decoder.decodeString()
        var ID = 0
        var broadcastID = 0
        var dataCode = 0
        var dataInstance = 0
        var parametrSize = 0
        var flagShift = 0
        var optimisation = 0
        var valueLimit = 0

        var initRead = 0
        var initWrite = 0
        var synchType = 0
        var synchDirection = 0
        var synchPeriod = 0

        var type = 0
        var saveInMaster = 0
        var saveInSlave = 0
        var saveReserv = 0

        var additionalInfoSize = 0

        var relatedParametrID = 0
        var relatedDataCode = 0

        val data = ""

        System.err.println("TEST deserialize BaseParametrInfoSerializer count ${string.length}")
        if (string.length >= 32) {
            ID = castUnsignedCharToInt(string.substring(0, 2).toInt(16).toByte())
            broadcastID = castUnsignedCharToInt(string.substring(2, 4).toInt(16).toByte())
            dataCode = castUnsignedCharToInt(string.substring(4, 6).toInt(16).toByte())
            dataInstance = castUnsignedCharToInt(string.substring(6, 8).toInt(16).toByte())
            parametrSize = castUnsignedCharToInt(string.substring(8, 10).toInt(16).toByte()) +
                           castUnsignedCharToInt(string.substring(10, 12).toInt(16).toByte())*256// 2
            flagShift = castUnsignedCharToInt(string.substring(12, 14).toInt(16).toByte())
            optimisation = castUnsignedCharToInt(string.substring(14, 16).toInt(16).toByte())
            valueLimit = castUnsignedCharToInt(string.substring(16, 18).toInt(16).toByte())

            initRead = castUnsignedCharToInt(string.substring(18, 20).toInt(16).toByte()) shr 0 and 0b00000001
            initWrite = castUnsignedCharToInt(string.substring(18, 20).toInt(16).toByte()) shr 1 and 0b00000001
            synchType = castUnsignedCharToInt(string.substring(18, 20).toInt(16).toByte()) shr 2 and 0b00000111
            synchDirection = castUnsignedCharToInt(string.substring(18, 20).toInt(16).toByte()) shr 5 and 0b00000111
            synchPeriod = castUnsignedCharToInt(string.substring(20, 22).toInt(16).toByte())

            type = castUnsignedCharToInt(string.substring(22, 24).toInt(16).toByte())
            saveInMaster = castUnsignedCharToInt(string.substring(24, 26).toInt(16).toByte()) shr 0 and 0b00000001
            saveInSlave = castUnsignedCharToInt(string.substring(24, 26).toInt(16).toByte()) shr 1 and 0b00000001
            saveReserv = castUnsignedCharToInt(string.substring(24, 26).toInt(16).toByte()) shr 2 and 0b00111111

            additionalInfoSize = castUnsignedCharToInt(string.substring(26, 28).toInt(16).toByte())

            relatedParametrID = castUnsignedCharToInt(string.substring(28, 30).toInt(16).toByte())
            relatedDataCode = castUnsignedCharToInt(string.substring(30, 32).toInt(16).toByte())
        }

        return BaseParameterInfoStruct (
            ID = ID,
            broadcastID = broadcastID,
            dataCode = dataCode,
            dataInstance = dataInstance,
            parametrSize = parametrSize,

            flagShift = flagShift,
            optimisation = optimisation,
            valueLimit = valueLimit,

            initRead = initRead,
            initWrite = initWrite,
            synchType = synchType,
            synchDirection = synchDirection,
            synchPeriod = synchPeriod,

            type = type,
            saveInMaster = saveInMaster,
            saveInSlave = saveInSlave,
            saveReserv = saveReserv,

            additionalInfoSize = additionalInfoSize,

            relatedParametrID = relatedParametrID,
            relatedDataCode = relatedDataCode,

            data = data
        )
    }

    override fun serialize(encoder: Encoder, value: BaseParameterInfoStruct) {
        val code: String =
            value.ID.toString()

        encoder.encodeString("$code")
    }
}