package com.bailout.stickk.ubi4.utility

import android.content.Context
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication.Companion.applicationContext
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.ui.gestures.GestureCollectionFactory

class CollectionGesturesProvider {
    companion object {
        fun getCollectionGestures(): ArrayList<Gesture> {
            val ctx = applicationContext()
            val prefs = ctx.getSharedPreferences(PreferenceKeysUbi4.APP_PREFERENCES, Context.MODE_PRIVATE)
            val macKey = prefs.getString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, "") ?: ""
            return GestureCollectionFactory.create(ctx) { index, fallback ->
                val key = PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM + macKey + index
                prefs.getString(key, fallback) ?: fallback
            }
        }

        fun getGesture(gestureId: Int): Gesture =
            getCollectionGestures().firstOrNull { it.gestureId == gestureId } ?: Gesture(0)
    }
}
