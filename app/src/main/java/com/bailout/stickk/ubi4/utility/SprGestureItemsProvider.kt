package com.bailout.stickk.ubi4.utility

import com.bailout.stickk.R
import com.bailout.stickk.ubi4.adapters.models.SprGestureItem

class SprGestureItemsProvider {
    fun getSprGestureItemList(): ArrayList<SprGestureItem> {
        val sprGestureItemList: ArrayList<SprGestureItem> = ArrayList()
        sprGestureItemList.add(
            SprGestureItem(
                "Neutral", R.drawable.ok, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                "Thumb Bend", R.drawable.grip_the_ball, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                "Palm Closing", R.drawable.koza, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                "Palm Opening", R.drawable.grip_the_ball, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                "OK Pinch", R.drawable.ok, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                "Flexion", R.drawable.koza, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                "Extension", R.drawable.grip_the_ball, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                "Gesture Key", R.drawable.kulak, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                "Pistol Pointer", R.drawable.ok, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                "Adduction", R.drawable.grip_the_ball, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                "Abduction", R.drawable.koza, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                "Pronation", R.drawable.kulak, false
            )
        )
        sprGestureItemList.add(
            SprGestureItem(
                "Supination", R.drawable.grip_the_ball, false
            )
        )
        return sprGestureItemList
    }

}