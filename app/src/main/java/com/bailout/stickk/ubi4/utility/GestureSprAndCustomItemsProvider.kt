package com.bailout.stickk.ubi4.utility

import com.bailout.stickk.R
import com.bailout.stickk.ubi4.adapters.models.SprGestureItem

class GestureSprAndCustomItemsProvider {
    fun getSprAndCustomGestureItemList(): ArrayList<SprGestureItem> {
        val sprGestureItemList: ArrayList<SprGestureItem> = ArrayList()
        sprGestureItemList.add(SprGestureItem("Neutral", R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem("Thumb Bend", R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem("Palm Closing", R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem("Palm Opening", R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem("OK Pinch", R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem("Flexion", R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem("Extension", R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem("Gesture 1", R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem("Gesture 2", R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem("Gesture 3", R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem("Gesture 4", R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem("Gesture 5", R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem("Gesture 6", R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem("Gesture 7", R.drawable.ok, false))
        sprGestureItemList.add(SprGestureItem("Gesture 8", R.drawable.ok, false))
        return sprGestureItemList
    }
}
