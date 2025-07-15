package com.bailout.stickk.ubi4.data.local

import com.bailout.stickk.ubi4.data.FirmwareInfoStructSerializer
import kotlinx.serialization.Serializable

@Serializable(with = FirmwareInfoStructSerializer::class)
data class FirmwareInfoStruct(
    val fwName:               String = "",
    val fwMajor:              Int    = 0,
    val fwMinor:              Int    = 0,
    val fwQuickFix:           Int    = 0,
    val fwSinceLastTag:       Int    = 0,

    val fwLabel:              String = "",
    val fwType:               Int    = 0,
    val fwCode:               Int    = 0,

    val fwStartAddress:       Long   = 0,
    val fwSize:               Long   = 0,
    val fwCrc:                Long   = 0,

    val sdkMajor:             Int    = 0,
    val sdkMinor:             Int    = 0,
    val sdkQuickFix:          Int    = 0,
    val sdkSinceLastTag:      Int    = 0,

    val fwAdditionalInfoType: Int    = 0,
    val fwAdditionalInfo:     Long   = 0,


    val deviceAddress: Int = 0,
    val deviceCode:    Int = 0

) {
    /** Итоговая строка «1.9.3» */
    val fwVersion: String
        get() = "$fwMajor.$fwMinor.$fwQuickFix"

}