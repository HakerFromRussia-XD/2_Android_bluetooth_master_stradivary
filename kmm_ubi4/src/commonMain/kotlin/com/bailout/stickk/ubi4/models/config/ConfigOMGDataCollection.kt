package com.bailout.stickk.ubi4.models.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConfigOMGDataCollection(
    @SerialName("FPATH") var fPath: String? = null,
    @SerialName("DF_PROTOCOL_HEADER") var dFProtocolHeader: List<String> = emptyList(),
    @SerialName("BASELINE_DURATION") var baselineDuration: Int? = null,
    @SerialName("PRE_GEST_DURATION") var preGestDuration: Int? = null,
    @SerialName("AT_GEST_DURATION") var atGestDuration: Int? = null,
    @SerialName("POST_GEST_DURATION") var postGestDuration: Int? = null,
    @SerialName("N_CYCLES") var nCycles: Int? = null,
    @SerialName("N_OMG_CH") var nOmgCh: Int? = null,
    @SerialName("N_EMG_CH") var nEmgCh: Int? = null,
    @SerialName("N_BNO_CH") var nBnoCh: Int? = null,
    @SerialName("IS_ML") var isMl: Boolean? = null,
    @SerialName("N_ML_CH") var nMlCh: Int? = null,
    @SerialName("GESTURES_ID") var gesturesId: GesturesId? = GesturesId(),
    @SerialName("GESTURE_SEQUENCE") var gestureSequence: List<String> = emptyList(),
    @SerialName("HEADER") var header: String? = null,
    @SerialName("FNAME") var fName: String? = null,
    @SerialName("FPROTNAME") var fProtName: String? = null,
    @SerialName("FHEADER") var fHeader: String? = null,
    @SerialName("N_COLS") var nCols: Int? = null
)

@Serializable
data class GesturesId(
    @SerialName("Neutral")      var Neutral: String? = null,
    @SerialName("ThumbFingers") var ThumbFingers: String? = null,
    @SerialName("Close")        var Close: String? = null,
    @SerialName("Open")         var Open: String? = null,
    @SerialName("Pinch")        var Pinch: String? = null,
    @SerialName("Indication")   var Indication: String? = null,
    @SerialName("Wrist_Flex")   var WristFlex: String? = null,
    @SerialName("Wrist_Extend") var WristExtend: String? = null,
    @SerialName("Supination")   var Supination: String? = null,
    @SerialName("Pronation")    var Pronation: String? = null,
    @SerialName("Abduction")    var Abduction: String? = null,
    @SerialName("Adduction")    var Adduction: String? = null,
    @SerialName("Key")          var Key: String? = null,
) {
    // Метод для получения имени жеста по значению
    fun getGestureNameByValue(value: Int): String? {
        return mapOf(
            Neutral?.toIntOrNull()      to "Neutral",
            ThumbFingers?.toIntOrNull() to "ThumbFingers",
            Close?.toIntOrNull()        to "Close",
            Open?.toIntOrNull()         to "Open",
            Pinch?.toIntOrNull()        to "Pinch",
            Indication?.toIntOrNull()   to "Indication",
            WristFlex?.toIntOrNull()    to "Wrist_Flex",
            WristExtend?.toIntOrNull()  to "Wrist_Extend",
            Supination?.toIntOrNull()   to "Supination",
            Pronation?.toIntOrNull()    to "Pronation",
            Abduction?.toIntOrNull()    to "Abduction",
            Adduction?.toIntOrNull()    to "Adduction",
            Key?.toIntOrNull()          to "Key",
        )[value]
    }

    // Метод для получения значения жеста по имени
    fun getGestureValueByName(name: String): Int? {
        return when (name) {
            "Neutral"      -> Neutral?.toIntOrNull()
            "ThumbFingers" -> ThumbFingers?.toIntOrNull()
            "Close"        -> Close?.toIntOrNull()
            "Open"         -> Open?.toIntOrNull()
            "Pinch"        -> Pinch?.toIntOrNull()
            "Indication"   -> Indication?.toIntOrNull()
            "Wrist_Flex"   -> WristFlex?.toIntOrNull()
            "Wrist_Extend" -> WristExtend?.toIntOrNull()
            "Supination"   -> Supination?.toIntOrNull()
            "Pronation"    -> Pronation?.toIntOrNull()
            "Abduction"    -> Abduction?.toIntOrNull()
            "Adduction"    -> Adduction?.toIntOrNull()
            "Key"          -> Key?.toIntOrNull()
            else -> null
        }
    }
}