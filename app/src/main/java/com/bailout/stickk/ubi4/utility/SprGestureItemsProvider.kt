package com.bailout.stickk.ubi4.utility

import android.content.Context
import android.util.Log
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.models.dialog.SprGestureItem
import com.bailout.stickk.ubi4.shared.SharedRes

class SprGestureItemsProvider(private val context: Context) {


    private val gestureNameToKeyNameMap: Map<String, String> = mapOf(
        context.getString(SharedRes.strings.thumb_finger.resourceId) to "ThumbFingers",
        context.getString(SharedRes.strings.palm_closing.resourceId) to "Close",
        context.getString(SharedRes.strings.palm_opening.resourceId) to "Open",
        context.getString(SharedRes.strings.ok_pinch.resourceId) to "Pinch",
        context.getString(SharedRes.strings.flexion.resourceId) to "Wrist_Flex",
        context.getString(SharedRes.strings.extension.resourceId) to "Wrist_Extend",
        context.getString(SharedRes.strings.gesture_key.resourceId) to "Key",
        context.getString(SharedRes.strings.pistol_pointer_gesture.resourceId) to "Indication",
        context.getString(SharedRes.strings.adduction.resourceId) to "Adduction",
        context.getString(SharedRes.strings.abduction.resourceId) to "Abduction",
        context.getString(SharedRes.strings.pronation.resourceId) to "Pronation",
        context.getString(SharedRes.strings.supination.resourceId) to "Supination"
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
        return sprGestureItem?.animationId ?: R.raw.open
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
            SprGestureItem(sprGestureId = 1, title = context.getString(SharedRes.strings.thumb_finger.resourceId), animationId = R.raw.thumb_fingers, check = false, keyNameGesture = "ThumbFingers"),
            SprGestureItem(sprGestureId = 2, title = context.getString(SharedRes.strings.flexion.resourceId), animationId = R.raw.wrist_flex, check = false, keyNameGesture = "Wrist_Flex"),
            SprGestureItem(sprGestureId = 3, title = context.getString(SharedRes.strings.extension.resourceId), animationId = R.raw.wrist_extend, check = false, keyNameGesture = "Wrist_Extend"),
            SprGestureItem(sprGestureId = 4, title = context.getString(SharedRes.strings.palm_closing.resourceId), animationId = R.raw.close, check = false, keyNameGesture = "Close"),
            SprGestureItem(sprGestureId = 5, title = context.getString(SharedRes.strings.palm_opening.resourceId), animationId = R.raw.open, check = false, keyNameGesture = "Open"),
            SprGestureItem(sprGestureId = 6, title = context.getString(SharedRes.strings.ok_pinch.resourceId), animationId = R.raw.pinch, check = false, keyNameGesture = "Pinch"),
            SprGestureItem(sprGestureId = 7, title = context.getString(SharedRes.strings.pistol_pointer_gesture.resourceId), animationId = R.raw.indication, check = false, keyNameGesture = "Indication"),
            SprGestureItem(sprGestureId = 8, title = context.getString(SharedRes.strings.gesture_key.resourceId), animationId = R.raw.key, check = false, keyNameGesture = "Key"),
            SprGestureItem(sprGestureId = 9, title = context.getString(SharedRes.strings.adduction.resourceId), animationId = R.raw.adduction, check = false, keyNameGesture = "Adduction"),
            SprGestureItem(sprGestureId = 10, title = context.getString(SharedRes.strings.abduction.resourceId), animationId = R.raw.abduction, check = false, keyNameGesture = "Abduction"),
            SprGestureItem(sprGestureId = 11, title = context.getString(SharedRes.strings.pronation.resourceId), animationId = R.raw.pronation, check = false, keyNameGesture = "Pronation"),
            SprGestureItem(sprGestureId = 12, title = context.getString(SharedRes.strings.supination.resourceId), animationId = R.raw.supination, check = false, keyNameGesture = "Supination")
        )

        Log.d("GestureDebug", "SprGestureItemList: $sprGestureItemList")
        return ArrayList(sprGestureItemList)
    }



    fun getSprGesture(sprGestureId: Int): SprGestureItem {
        val sprGestureList = getSprGestureItemList().filter{ it.sprGestureId == sprGestureId }
        if (sprGestureList.isNotEmpty()) { return sprGestureList[0] }
        else { return  SprGestureItem()
        }
    }


}