package com.bailout.stickk.ubi4.models.config

import com.google.gson.annotations.SerializedName

data class ConfigOMGDataCollection(
    @SerializedName("FPATH") var fPath: String? = null,
    @SerializedName("DF_PROTOCOL_HEADER") var dFProtocolHeader: ArrayList<String> = arrayListOf(),
    @SerializedName("BASELINE_DURATION") var baselineDuration: Int? = null,
    @SerializedName("PRE_GEST_DURATION") var preGestDuration: Int? = null,
    @SerializedName("AT_GEST_DURATION") var atGestDuration: Int? = null,
    @SerializedName("POST_GEST_DURATION") var postGestDuration: Int? = null,
    @SerializedName("N_CYCLES") var nCycles: Int? = null,
    @SerializedName("N_OMG_CH") var nOmgCh: Int? = null,
    @SerializedName("N_EMG_CH") var nEmgCh: Int? = null,
    @SerializedName("N_BNO_CH") var nBnoCh: Int? = null,
    @SerializedName("IS_ML") var isMl: Boolean? = null,
    @SerializedName("N_ML_CH") var nMlCh: Int? = null,
    @SerializedName("GESTURES_ID") var gesturesId: GesturesId? = GesturesId(),
    @SerializedName("GESTURE_SEQUENCE") var gestureSequence: ArrayList<String> = arrayListOf(),
    @SerializedName("HEADER") var header: String? = null,
    @SerializedName("FNAME") var fName: String? = null,
    @SerializedName("FPROTNAME") var fProtName: String? = null,
    @SerializedName("FHEADER") var fHeader: String? = null,
    @SerializedName("N_COLS") var nCols: Int? = null
)

data class GesturesId(
    @SerializedName("Neutral")      var Neutral: String? = null,
    @SerializedName("ThumbFingers") var ThumbFingers: String? = null,
    @SerializedName("Close")        var Close: String? = null,
    @SerializedName("Open")         var Open: String? = null,
    @SerializedName("Pinch")        var Pinch: String? = null,
    @SerializedName("Indication")   var Indication: String? = null,
    @SerializedName("Wrist_Flex")   var WristFlex: String? = null,
    @SerializedName("Wrist_Extend") var WristExtend: String? = null,
    @SerializedName("Supination")   var Supination: String? = null,
    @SerializedName("Pronation")    var Pronation: String? = null,
    @SerializedName("Abduction")    var Abduction: String? = null,
    @SerializedName("Adduction")    var Adduction: String? = null,
    @SerializedName("Key")          var Key: String? = null,
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