package com.bailout.stickk.ubi4.utility

import android.content.Context
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.models.SprGestureItem

class SprGestureItemsProvider {

    fun getSprGestureItemList(context: Context): ArrayList<SprGestureItem> {
        val sprGestureItemList: ArrayList<SprGestureItem> = ArrayList()
        sprGestureItemList.add(
            SprGestureItem(
                context.getString(R.string.thumb_finger), R.drawable.grip_the_ball, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                context.getString(R.string.palm_closing), R.drawable.koza, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                context.getString(R.string.palm_opening), R.drawable.grip_the_ball, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                context.getString(R.string.ok_pinch), R.drawable.ok, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                context.getString(R.string.flexion), R.drawable.koza, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                context.getString(R.string.extension), R.drawable.grip_the_ball, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                context.getString(R.string.gesture_key), R.drawable.kulak, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                context.getString(R.string.pistol_pointer_gesture), R.drawable.ok, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                context.getString(R.string.adduction), R.drawable.grip_the_ball, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                context.getString(R.string.abduction), R.drawable.koza, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                context.getString(R.string.pronation), R.drawable.kulak, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                context.getString(R.string.supination), R.drawable.grip_the_ball, false
            )
        )
        return sprGestureItemList
    }
}