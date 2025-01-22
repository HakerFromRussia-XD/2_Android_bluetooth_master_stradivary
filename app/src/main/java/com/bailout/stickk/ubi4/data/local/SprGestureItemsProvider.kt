package com.bailout.stickk.ubi4.data.local

import android.content.Context
import android.util.Log
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.models.SprGestureItem

class SprGestureItemsProvider(private val context: Context) {

    private val gestureNameToKeyNameMap: Map<String, String> = mapOf(
        context.getString(R.string.thumb_finger) to "ThumbFingers",
        context.getString(R.string.palm_closing) to "Close",
        context.getString(R.string.palm_opening) to "Open",
        context.getString(R.string.ok_pinch) to "Pinch",
        context.getString(R.string.flexion) to "Wrist_Flex",
        context.getString(R.string.extension) to "Wrist_Extend",
        context.getString(R.string.gesture_key) to "Key",
        context.getString(R.string.pistol_pointer_gesture) to "Indication",
        context.getString(R.string.adduction) to "Adduction",
        context.getString(R.string.abduction) to "Abduction",
        context.getString(R.string.pronation) to "Pronation",
        context.getString(R.string.supination) to "Supination"
    )

    private val gestureNameToGestureId: Map<Int, String> = mapOf(
        -1 to "BaseLine",
        0 to "Neutral",
        1 to "ThumbFingers",
        2 to "Close",
        3 to "Open",
        4 to "Pinch",
        5 to "Indication",
        6 to "Wrist_Flex",
        7 to "Wrist_Extend"
    )


    fun getAnimationIdByKeyNameGesture(keyNameGesture: String): Int {
        val sprGestureItem = getSprGestureItemList().find { it.keyNameGesture == keyNameGesture }
        return sprGestureItem?.animationId ?: R.drawable.sleeping
    }

    fun getNameGestureByKeyName(keyNameGesture: String): String {
        val sprGestureItem = getSprGestureItemList().find { it.keyNameGesture == keyNameGesture }
        return sprGestureItem?.title ?: ""
    }


    fun getKeyNameGestureByGestureName(gestureName: String): String? {
        Log.d("GestureDebug", "GestureName passed: $gestureName")
        return gestureNameToKeyNameMap[gestureName]
    }

    fun getGestureNameByGestureId(gestureId: Int): String? {
        return gestureNameToGestureId[gestureId]

    }


    fun getSprGestureItemList(): ArrayList<SprGestureItem> {
        val sprGestureItemList = listOf(
            SprGestureItem(sprGestureId = 1, title = context.getString(R.string.thumb_finger), animationId = R.raw.thumb_fingers, check = false, keyNameGesture = "ThumbFingers"),
            SprGestureItem(sprGestureId = 2, title = context.getString(R.string.flexion), animationId = R.raw.wrist_flex, check = false, keyNameGesture = "Wrist_Flex"),
            SprGestureItem(sprGestureId = 3, title = context.getString(R.string.extension), animationId = R.raw.wrist_extend, check = false, keyNameGesture = "Wrist_Extend"),
            SprGestureItem(sprGestureId = 4, title = context.getString(R.string.palm_closing), animationId = R.raw.close, check = false, keyNameGesture = "Close"),
            SprGestureItem(sprGestureId = 5, title = context.getString(R.string.palm_opening), animationId = R.raw.open, check = false, keyNameGesture = "Open"),
            SprGestureItem(sprGestureId = 6, title = context.getString(R.string.ok_pinch), animationId = R.raw.pinch, check = false, keyNameGesture = "Pinch"),
            SprGestureItem(sprGestureId = 7, title = context.getString(R.string.pistol_pointer_gesture), animationId = R.raw.indication, check = false, keyNameGesture = "Indication"),
            //TODO уточнить оставшиемся ID жестов
            SprGestureItem(sprGestureId = 8, title = context.getString(R.string.gesture_key), animationId = R.raw.key, check = false, keyNameGesture = "Key"),
            SprGestureItem(sprGestureId = 9, title = context.getString(R.string.adduction), animationId = R.raw.adduction, check = false, keyNameGesture = "Adduction"),
            SprGestureItem(sprGestureId = 10, title = context.getString(R.string.abduction), animationId = R.raw.abduction, check = false, keyNameGesture = "Abduction"),
            SprGestureItem(sprGestureId = 11, title = context.getString(R.string.pronation), animationId = R.raw.pronation, check = false, keyNameGesture = "Pronation"),
            SprGestureItem(sprGestureId = 12, title = context.getString(R.string.supination), animationId = R.raw.supination, check = false, keyNameGesture = "Supination")
        )

        Log.d("GestureDebug", "SprGestureItemList: $sprGestureItemList")
        return ArrayList(sprGestureItemList)
    }

    fun getSprGesture(sprGestureId: Int): SprGestureItem {
        val sprGestureList = getSprGestureItemList().filter{ it.sprGestureId == sprGestureId }
        if (sprGestureList.isNotEmpty()) { return sprGestureList[0] }
        else { return  SprGestureItem()}
    }


}