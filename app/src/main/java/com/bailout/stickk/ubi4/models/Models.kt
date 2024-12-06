package com.bailout.stickk.ubi4.models

import com.bailout.stickk.ubi4.data.local.Gesture
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

//widgets items
data class GesturesItem(val title: String, val widget: Any)
data class OneButtonItem(val title: String, val description: String, val widget: Any)
data class PlotItem(val title: String, val widget: Any)
data class SliderItem(val title: String, val widget: Any)
data class SwitchItem(val title: String, val widget: Any)
data class TrainingGestureItem(val title: String, val widget: Any)
data class FileItem(val name: String, val file: File)

// dialogs
data class DialogCollectionGestureItem(val gesture: Gesture, var check: Boolean = false)
data class SprGestureItem(val title: String, val image: Int, var check: Boolean)
data class BindingGestureItem(
    val position: Int,
    var nameOfUserGesture: String,
    val sprGestureItem: SprGestureItem
)

// приём и передача данных в потоках ble
data class ParameterRef(
    val addressDevice: Int, val parameterID: Int
)

data class PlotParameterRef(
    val addressDevice: Int, val parameterID: Int, val dataPlots: ArrayList<Int>
)

// 3D конфигуратор и передача информации о жесте
data class GestureInfo(
    val deviceAddress: Int, val parameterID: Int, val gestureID: Int
)

data class GestureWithAddress(
    val addressDevice: Int, val parameterID: Int,
    val gesture: Gesture, val gestureState: Int
)

data class GestureConfig(
    val baselineDuration: Double,
    val preGestDuration: Double,
    val atGestDuration: Double,
    val postGestDuration: Double,
    val gestureSequence: List<String>,
//    val gesturesId: Map<String, Int>
)

data class GesturePhase(
    var prePhase: Double = 0.0,
    var timeGesture: Double = 0.0,
    var postPhase: Double = 0.0,
    var animation: Int = 0,
    var headerText: String = "",
    var description: String = "",
    var gestureName: String = "",
    var gestureId: Int = 0
)

data class Config(
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

//data class GesturesId(
//    @SerializedName("GESTURES_ID")
//    val gesturesId: Map<String, String>  = emptyMap()
//)
data class GesturesId(
    @SerializedName("Neutral") var Neutral: String? = null,
    @SerializedName("ThumbFingers") var ThumbFingers: String? = null,
    @SerializedName("Close") var Close: String? = null,
    @SerializedName("Open") var Open: String? = null,
    @SerializedName("Pinch") var Pinch: String? = null,
    @SerializedName("Indication") var Indication: String? = null,
    @SerializedName("Wrist_Flex") var WristFlex: String? = null,
    @SerializedName("Wrist_Extend") var WristExtend: String? = null
)

// Базовая фаза (Baseline)
//data class PhaseBaseline(
//    override val prePhase: Int = 0,
//    override val timeGesture: Double = 5.0, // BASELINE_DURATION
//    override val postPhase: Int = 0,
//    override val animation: Int = 0,
//    override val headerText: String = "Подготовьтесь к выполнению жеста",
//    override val description: String = "",
//    override val gestureName: String = "Baseline", // так как это baseline состояние
//    override val gestureId: Int = -1 // baseline всегда -1
//) : GesturePhase()
//
//// Pre-фаза
//data class PhasePre(
//    override val prePhase: Int = 0,
//    override val timeGesture: Double = 2.0, // PRE_GEST_DURATION
//    override val postPhase: Int = 0,
//    override val animation: Int = 0,
//    override val headerText: String = "Подготовьтесь к выполнению жеста",
//    override val description: String = "",
//    override val gestureName: String,
//    override val gestureId: Int
//) : GesturePhase()
//
//// At-фаза (выполнение жеста)
//data class PhaseAt(
//    override val prePhase: Int = 0,
//    override val timeGesture: Double = 2.0, // AT_GEST_DURATION
//    override val postPhase: Int = 0,
//    override val animation: Int = 0,
//    override val headerText: String = "Выполните жест",
//    override val description: String = "",
//    override val gestureName: String,
//    override val gestureId: Int
//) : GesturePhase()
//
//// Post-фаза
//data class PhasePost(
//    override val prePhase: Int = 0,
//    override val timeGesture: Double = 2.0, // POST_GEST_DURATION
//    override val postPhase: Int = 0,
//    override val animation: Int = 0,
//    override val headerText: String = "Подготовьтесь к выполнению жеста",
//    override val description: String = "",
//    override val gestureName: String,
//    override val gestureId: Int
//) : GesturePhase()


