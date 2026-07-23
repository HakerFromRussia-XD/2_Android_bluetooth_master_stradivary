package com.bailout.stickk.ubi4.utility

import android.content.Context
import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication.Companion.applicationContext
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GestureEnum
import com.bailout.stickk.ubi4.shared.SharedRes

class CollectionGesturesProvider {

    companion object {

        fun getCollectionGestures(): ArrayList<Gesture> {
            val ctx = applicationContext()

            val list = ArrayList<Gesture>()

            list.add(Gesture(GestureEnum.GESTURE_FIST.number, gestureName = ctx.getString(SharedRes.strings.fist.resourceId), gestureImage = SharedRes.images.collection_fist_1.drawableResId))
            list.add(Gesture(GestureEnum.GESTURE_POINT.number, gestureName = ctx.getString(SharedRes.strings.gesture_point.resourceId), gestureImage = SharedRes.images.collection_point.drawableResId))
            list.add(Gesture(GestureEnum.GESTURE_PINCH.number, gestureName = ctx.getString(SharedRes.strings.gesture_pinch.resourceId), gestureImage = SharedRes.images.collection_pinch.drawableResId))
            list.add(Gesture(GestureEnum.GESTURE_FIST_THUMB_OVER.number, gestureName = ctx.getString(SharedRes.strings.gesture_fist_thumb_over.resourceId), gestureImage = SharedRes.images.collection_fist_2.drawableResId))
            list.add(Gesture(GestureEnum.GESTURE_KEY.number, gestureName = ctx.getString(SharedRes.strings.gesture_key.resourceId), gestureImage = SharedRes.images.collection_key.drawableResId))
            list.add(Gesture(GestureEnum.GESTURE_ROCK.number, gestureName = ctx.getString(SharedRes.strings.gesture_rock.resourceId), gestureImage = SharedRes.images.collection_rock.drawableResId))
            list.add(Gesture(GestureEnum.GESTURE_TWIZZERS.number, gestureName = ctx.getString(SharedRes.strings.gesture_twizzers.resourceId), gestureImage = SharedRes.images.collection_twizzers.drawableResId))
            list.add(Gesture(GestureEnum.GESTURE_CUPHOLDER.number, gestureName = ctx.getString(SharedRes.strings.gesture_cupholder.resourceId), gestureImage = SharedRes.images.collection_cupholder.drawableResId))
            list.add(Gesture(GestureEnum.GESTURE_HALF_GRAB.number, gestureName = ctx.getString(SharedRes.strings.gesture_half_grab.resourceId), gestureImage = SharedRes.images.collect_half_grab.drawableResId))
            list.add(Gesture(GestureEnum.GESTURE_OK.number, gestureName = ctx.getString(SharedRes.strings.gesture_ok.resourceId), gestureImage = SharedRes.images.collection_ok.drawableResId))
            list.add(Gesture(GestureEnum.GESTURE_THUMB_UP.number, gestureName = ctx.getString(SharedRes.strings.gesture_thumb_up.resourceId), gestureImage = SharedRes.images.collection_thumb_up.drawableResId))
            list.add(Gesture(GestureEnum.GESTURE_MIDDLE_FINGER.number, gestureName = ctx.getString(SharedRes.strings.gesture_middle_finger.resourceId), gestureImage = SharedRes.images.collection_middle_finger.drawableResId))
            list.add(Gesture(GestureEnum.GESTURE_DOUBLE_POINT.number, gestureName = ctx.getString(SharedRes.strings.gesture_double_point.resourceId), gestureImage = SharedRes.images.collection_double_point.drawableResId))
            list.add(Gesture(GestureEnum.GESTURE_CALL_ME.number, gestureName = ctx.getString(SharedRes.strings.gesture_call_me.resourceId), gestureImage = SharedRes.images.collection_call_me.drawableResId))
            list.add(Gesture(GestureEnum.GESTURE_NATURAL_POSITION.number, gestureName = ctx.getString(SharedRes.strings.gesture_natural_position.resourceId), gestureImage = SharedRes.images.collection_natural_position.drawableResId))


            // кастомные кнопки (имя из prefs, дефолт из ресурсов)
            val prefs = ctx.getSharedPreferences(
                PreferenceKeysUbi4.APP_PREFERENCES,
                Context.MODE_PRIVATE
            )

            val macKey = prefs.getString(PreferenceKeysUbi4.LAST_CONNECTION_MAC_UBI4, "") ?: ""

            fun customName(index: Int, defaultResId: Int): String {
                val key = PreferenceKeysUbi4.SELECT_GESTURE_SETTINGS_NUM + macKey + index
                val fallback = ctx.getString(defaultResId)
                return prefs.getString(key, fallback) ?: fallback
            }

            // кастомные кнопки
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_0.number, gestureName = customName(0,SharedRes.strings.gesture_1_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_1.number, gestureName = customName(1,SharedRes.strings.gesture_2_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_2.number, gestureName = customName(2,SharedRes.strings.gesture_3_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_3.number, gestureName = customName(3,SharedRes.strings.gesture_4_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_4.number, gestureName = customName(4,SharedRes.strings.gesture_5_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_5.number, gestureName = customName(5,SharedRes.strings.gesture_6_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_6.number, gestureName = customName(6,SharedRes.strings.gesture_7_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_7.number, gestureName = customName(7,SharedRes.strings.gesture_8_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_8.number, gestureName = customName(8,SharedRes.strings.gesture_9_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_9.number, gestureName = customName(9,SharedRes.strings.gesture_10_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_10.number, gestureName = customName(10,SharedRes.strings.gesture_11_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_11.number, gestureName = customName(11,SharedRes.strings.gesture_12_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_12.number, gestureName = customName(12,SharedRes.strings.gesture_13_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_13.number, gestureName = customName(13,SharedRes.strings.gesture_14_btn.resourceId)))

            return list
        }

        fun getGesture(gestureId: Int): Gesture {
            val gestureList = getCollectionGestures().filter { it.gestureId == gestureId }
            if (gestureList.isNotEmpty()) { return gestureList[0] }
            else { return Gesture(0) }
        }

    }
}
