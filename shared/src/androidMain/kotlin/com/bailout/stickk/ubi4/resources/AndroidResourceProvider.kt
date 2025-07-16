package com.bailout.stickk.ubi4.resources

import android.content.Context
import com.bailout.stickk.ubi4.shared.R

class AndroidResourceProvider(private val context: Context) : ResourceProvider {
    override fun getString(id: ResourceString): String {
        return when(id) {
            ResourceString.THUMB_FINGER -> context.getString(R.string.thumb_finger)
            ResourceString.FLEXION -> context.getString(R.string.flexion)
            ResourceString.EXTENSION -> context.getString(R.string.extension)
            ResourceString.PALM_CLOSING -> context.getString(R.string.palm_closing)
            ResourceString.PALM_OPENING -> context.getString(R.string.palm_opening)
            ResourceString.OK_PINCH -> context.getString(R.string.ok_pinch)
            ResourceString.PISTOL_POINTER_GESTURE -> context.getString(R.string.pistol_pointer_gesture)
            ResourceString.GESTURE_KEY -> context.getString(R.string.gesture_key)
            ResourceString.ADDUCTION -> context.getString(R.string.adduction)
            ResourceString.ABDUCTION -> context.getString(R.string.abduction)
            ResourceString.PRONATION -> context.getString(R.string.pronation)
            ResourceString.SUPINATION -> context.getString(R.string.supination)
            ResourceString.FIST -> context.getString(R.string.fist)
            ResourceString.GESTURE_POINT -> context.getString(R.string.gesture_point)
            ResourceString.GESTURE_PINCH -> context.getString(R.string.gesture_pinch)
            ResourceString.GESTURE_FIST_THUMB_OVER -> context.getString(R.string.gesture_fist_thumb_over)
            ResourceString.GESTURE_ROCK -> context.getString(R.string.gesture_rock)
            ResourceString.GESTURE_TWIZZERS -> context.getString(R.string.gesture_twizzers)
            ResourceString.GESTURE_CUPHOLDER -> context.getString(R.string.gesture_cupholder)
            ResourceString.GESTURE_HALF_GRAB -> context.getString(R.string.gesture_half_grab)
            ResourceString.GESTURE_OK -> context.getString(R.string.gesture_ok)
            ResourceString.GESTURE_THUMB_UP -> context.getString(R.string.gesture_thumb_up)
            ResourceString.GESTURE_MIDDLE_FINGER -> context.getString(R.string.gesture_middle_finger)
            ResourceString.GESTURE_DOUBLE_POINT -> context.getString(R.string.gesture_double_point)
            ResourceString.GESTURE_CALL_ME -> context.getString(R.string.gesture_call_me)
            ResourceString.GESTURE_NATURAL_POSITION -> context.getString(R.string.gesture_natural_position)
            // Добавляем остальные строки для custom-кнопок
            ResourceString.GESTURE_1_BTN -> context.getString(R.string.gesture_1_btn)
            ResourceString.GESTURE_2_BTN -> context.getString(R.string.gesture_2_btn)
            ResourceString.GESTURE_3_BTN -> context.getString(R.string.gesture_3_btn)
            ResourceString.GESTURE_4_BTN -> context.getString(R.string.gesture_4_btn)
            ResourceString.GESTURE_5_BTN -> context.getString(R.string.gesture_5_btn)
            ResourceString.GESTURE_6_BTN -> context.getString(R.string.gesture_6_btn)
            ResourceString.GESTURE_7_BTN -> context.getString(R.string.gesture_7_btn)
            ResourceString.GESTURE_8_BTN -> context.getString(R.string.gesture_8_btn)
            ResourceString.GESTURE_9_BTN -> context.getString(R.string.gesture_9_btn)
            ResourceString.GESTURE_10_BTN -> context.getString(R.string.gesture_10_btn)
            ResourceString.GESTURE_11_BTN -> context.getString(R.string.gesture_11_btn)
            ResourceString.GESTURE_12_BTN -> context.getString(R.string.gesture_12_btn)
            ResourceString.GESTURE_13_BTN -> context.getString(R.string.gesture_13_btn)
            ResourceString.GESTURE_14_BTN -> context.getString(R.string.gesture_14_btn)
        }
    }

    override fun getRaw(id: ResourceRaw): Int {
        return when(id) {
            ResourceRaw.THUMB_FINGERS -> R.raw.thumb_fingers
            ResourceRaw.WRIST_FLEX -> R.raw.wrist_flex
            ResourceRaw.WRIST_EXTEND -> R.raw.wrist_extend
            ResourceRaw.CLOSE -> R.raw.close
            ResourceRaw.OPEN -> R.raw.open
            ResourceRaw.PINCH -> R.raw.pinch
            ResourceRaw.INDICATION -> R.raw.indication
            ResourceRaw.KEY -> R.raw.key
            ResourceRaw.ADDUCTION -> R.raw.adduction
            ResourceRaw.ABDUCTION -> R.raw.abduction
            ResourceRaw.PRONATION -> R.raw.pronation
            ResourceRaw.SUPINATION -> R.raw.supination
        }
    }

    override fun getDrawable(id: ResourceDrawable): Int  = when (id){
        ResourceDrawable.COLLECTION_FIST_1 -> R.drawable.collection_fist_1
        ResourceDrawable.COLLECTION_POINT -> R.drawable.collection_point
        ResourceDrawable.COLLECTION_PINCH -> R.drawable.collection_pinch
        ResourceDrawable.COLLECTION_FIST_2 -> R.drawable.collection_fist_2
        ResourceDrawable.COLLECTION_KEY -> R.drawable.collection_key
        ResourceDrawable.COLLECTION_ROCK -> R.drawable.collection_rock
        ResourceDrawable.COLLECTION_TWIZZERS -> R.drawable.collection_twizzers
        ResourceDrawable.COLLECTION_CUPHOLDER -> R.drawable.collection_cupholder
        ResourceDrawable.COLLECT_HALF_GRAB -> R.drawable.collect_half_grab
        ResourceDrawable.COLLECTION_OK -> R.drawable.collection_ok
        ResourceDrawable.COLLECTION_THUMB_UP -> R.drawable.collection_thumb_up
        ResourceDrawable.COLLECTION_MIDDLE_FINGER -> R.drawable.collection_middle_finger
        ResourceDrawable.COLLECTION_DOUBLE_POINT -> R.drawable.collection_double_point
        ResourceDrawable.COLLECTION_CALL_ME -> R.drawable.collection_call_me
        ResourceDrawable.COLLECTION_NATURAL_POSITION -> R.drawable.collection_natural_position
    }
}