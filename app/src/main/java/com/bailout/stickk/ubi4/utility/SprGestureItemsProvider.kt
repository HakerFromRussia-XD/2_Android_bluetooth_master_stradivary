package com.bailout.stickk.ubi4.utility

import android.content.Context
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

    fun getAnimationIdByKeyNameGesture(keyNameGesture: String) : Int {
        val sprGestureItem = getSprGestureItemList().find { it.keyNameGesture == keyNameGesture }
        return sprGestureItem?.animationId ?: R.drawable.sleeping
    }


    fun getKeyNameGestureByGestureName(gestureName: String): String? {
        return gestureNameToKeyNameMap[gestureName]
    }

     fun getSprGestureItemList(): ArrayList<SprGestureItem> {
        val sprGestureItemList: ArrayList<SprGestureItem> = ArrayList()
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.thumb_finger), R.raw.thumb_fingers, false, "ThumbFingers"))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.palm_closing), R.raw.close, false, "Close"))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.palm_opening), R.raw.open, false, "Open"))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.ok_pinch), R.raw.pinch, false, "Pinch"))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.flexion), R.raw.wrist_flex, false, "Wrist_Flex"))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.extension), R.raw.wrist_extend, false, "Wrist_Extend"))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.gesture_key), R.raw.key, false, "Key"))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.pistol_pointer_gesture), R.raw.indication, false, "Indication"))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.adduction), R.raw.adduction, false, "Adduction"))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.abduction), R.raw.abduction, false, "Abduction"))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.pronation), R.raw.pronation, false, "Pronation"))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.supination), R.raw.supination, false, "Supination"))
        return sprGestureItemList
    }
}