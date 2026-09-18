package com.bailout.stickk.ubi4.ui.gestures

import android.content.Context
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GestureEnum
import com.bailout.stickk.ubi4.shared.SharedRes

/** Shared visual catalog. Names come from the caller; no preferences or global context are read here. */
object GestureCollectionFactory {
    fun create(ctx: Context, customName: (index: Int, defaultName: String) -> String): ArrayList<Gesture> {
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
        list.add(Gesture(GestureEnum.GESTURE_DOUBLE_POINT.number, gestureName = ctx.getString(SharedRes.strings.gesture_double_point.resourceId), gestureImage = SharedRes.images.collection_double_point.drawableResId))
        list.add(Gesture(GestureEnum.GESTURE_CALL_ME.number, gestureName = ctx.getString(SharedRes.strings.gesture_call_me.resourceId), gestureImage = SharedRes.images.collection_call_me.drawableResId))
        list.add(Gesture(GestureEnum.GESTURE_NATURAL_POSITION.number, gestureName = ctx.getString(SharedRes.strings.gesture_natural_position.resourceId), gestureImage = SharedRes.images.collection_natural_position.drawableResId))


        // кастомные кнопки
        list.add(Gesture(GestureEnum.GESTURE_CUSTOM_0.number, gestureName = customName(0, ctx.getString(SharedRes.strings.gesture_1_btn.resourceId))))
        list.add(Gesture(GestureEnum.GESTURE_CUSTOM_1.number, gestureName = customName(1, ctx.getString(SharedRes.strings.gesture_2_btn.resourceId))))
        list.add(Gesture(GestureEnum.GESTURE_CUSTOM_2.number, gestureName = customName(2, ctx.getString(SharedRes.strings.gesture_3_btn.resourceId))))
        list.add(Gesture(GestureEnum.GESTURE_CUSTOM_3.number, gestureName = customName(3, ctx.getString(SharedRes.strings.gesture_4_btn.resourceId))))
        list.add(Gesture(GestureEnum.GESTURE_CUSTOM_4.number, gestureName = customName(4, ctx.getString(SharedRes.strings.gesture_5_btn.resourceId))))
        list.add(Gesture(GestureEnum.GESTURE_CUSTOM_5.number, gestureName = customName(5, ctx.getString(SharedRes.strings.gesture_6_btn.resourceId))))
        list.add(Gesture(GestureEnum.GESTURE_CUSTOM_6.number, gestureName = customName(6, ctx.getString(SharedRes.strings.gesture_7_btn.resourceId))))
        list.add(Gesture(GestureEnum.GESTURE_CUSTOM_7.number, gestureName = customName(7, ctx.getString(SharedRes.strings.gesture_8_btn.resourceId))))
        list.add(Gesture(GestureEnum.GESTURE_CUSTOM_8.number, gestureName = customName(8, ctx.getString(SharedRes.strings.gesture_9_btn.resourceId))))
        list.add(Gesture(GestureEnum.GESTURE_CUSTOM_9.number, gestureName = customName(9, ctx.getString(SharedRes.strings.gesture_10_btn.resourceId))))
        list.add(Gesture(GestureEnum.GESTURE_CUSTOM_10.number, gestureName = customName(10, ctx.getString(SharedRes.strings.gesture_11_btn.resourceId))))
        list.add(Gesture(GestureEnum.GESTURE_CUSTOM_11.number, gestureName = customName(11, ctx.getString(SharedRes.strings.gesture_12_btn.resourceId))))
        list.add(Gesture(GestureEnum.GESTURE_CUSTOM_12.number, gestureName = customName(12, ctx.getString(SharedRes.strings.gesture_13_btn.resourceId))))
        list.add(Gesture(GestureEnum.GESTURE_CUSTOM_13.number, gestureName = customName(13, ctx.getString(SharedRes.strings.gesture_14_btn.resourceId))))

        return list
    }
}
