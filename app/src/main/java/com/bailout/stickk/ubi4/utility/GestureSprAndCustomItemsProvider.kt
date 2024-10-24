package com.bailout.stickk.ubi4.utility

import android.content.Context
import com.bailout.stickk.R
import com.bailout.stickk.ubi4.adapters.models.SprGestureItem

class GestureSprAndCustomItemsProvider {
    fun getSprAndCustomGestureItemList(context: Context): ArrayList<SprGestureItem> {
        val sprGestureItemList: ArrayList<SprGestureItem> = ArrayList()
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.thumb_bend), R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.palm_closing), R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.palm_opening), R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.ok_pinch), R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.flexion), R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.extension), R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.gesture_1_btn), R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.gesture_2_btn), R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.gesture_3_btn), R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.gesture_4_btn), R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.gesture_5_btn), R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.gesture_6_btn), R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.gesture_7_btn), R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem(context.getString(R.string.gesture_8_btn), R.drawable.ok, false))
        return sprGestureItemList
    }
}
