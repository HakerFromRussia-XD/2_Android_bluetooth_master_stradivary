package com.bailout.stickk.ubi4.utility

import com.bailout.stickk.new_electronic_by_Rodeon.WDApplication.Companion.applicationContext
import com.bailout.stickk.ubi4.data.local.Gesture
import com.bailout.stickk.ubi4.persistence.preference.PreferenceKeysUbi4.GestureEnum
import com.bailout.stickk.ubi4.shared.SharedRes

class CollectionGesturesProvider {

    companion object {

        private var cached: ArrayList<Gesture>? = null


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

            // кастомные кнопки
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_0.number, gestureName = ctx.getString(SharedRes.strings.gesture_1_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_1.number, gestureName = ctx.getString(SharedRes.strings.gesture_2_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_2.number, gestureName = ctx.getString(SharedRes.strings.gesture_3_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_3.number, gestureName = ctx.getString(SharedRes.strings.gesture_4_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_4.number, gestureName = ctx.getString(SharedRes.strings.gesture_5_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_5.number, gestureName = ctx.getString(SharedRes.strings.gesture_6_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_6.number, gestureName = ctx.getString(SharedRes.strings.gesture_7_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_7.number, gestureName = ctx.getString(SharedRes.strings.gesture_8_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_8.number, gestureName = ctx.getString(SharedRes.strings.gesture_9_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_9.number, gestureName = ctx.getString(SharedRes.strings.gesture_10_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_10.number, gestureName = ctx.getString(SharedRes.strings.gesture_11_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_11.number, gestureName = ctx.getString(SharedRes.strings.gesture_12_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_12.number, gestureName = ctx.getString(SharedRes.strings.gesture_13_btn.resourceId)))
            list.add(Gesture(GestureEnum.GESTURE_CUSTOM_13.number, gestureName = ctx.getString(SharedRes.strings.gesture_14_btn.resourceId)))

            cached = list
            return list
        }

        fun getGesture(gestureId: Int): Gesture {
            val gestureList = getCollectionGestures().filter { it.gestureId == gestureId }
            if (gestureList.isNotEmpty()) { return gestureList[0] }
            else { return Gesture(0) }
        }
        // 2) точечное обновление имени кастомного жеста без пересборки всего приложения
        fun updateCustomGestureName(customIndex: Int, newName: String) {
            val list = getCollectionGestures()

            val customId = when (customIndex) {
                0 -> GestureEnum.GESTURE_CUSTOM_0.number
                1 -> GestureEnum.GESTURE_CUSTOM_1.number
                2 -> GestureEnum.GESTURE_CUSTOM_2.number
                3 -> GestureEnum.GESTURE_CUSTOM_3.number
                4 -> GestureEnum.GESTURE_CUSTOM_4.number
                5 -> GestureEnum.GESTURE_CUSTOM_5.number
                6 -> GestureEnum.GESTURE_CUSTOM_6.number
                7 -> GestureEnum.GESTURE_CUSTOM_7.number
                8 -> GestureEnum.GESTURE_CUSTOM_8.number
                9 -> GestureEnum.GESTURE_CUSTOM_9.number
                10 -> GestureEnum.GESTURE_CUSTOM_10.number
                11 -> GestureEnum.GESTURE_CUSTOM_11.number
                12 -> GestureEnum.GESTURE_CUSTOM_12.number
                13 -> GestureEnum.GESTURE_CUSTOM_13.number
                else -> return
            }

            val idxInList = list.indexOfFirst { it.gestureId == customId }
            if (idxInList != -1) {
                val old = list[idxInList]
                // если Gesture у тебя data class и gestureName var — можно просто:
                old.gestureName = newName
                // если gestureName val — тогда заменить объект:
                // list[idxInList] = old.copy(gestureName = newName)
            }
        }

        // если нужно "снести кэш" (например смена языка/ресурсов) — опционально
        fun invalidateCache() {
            cached = null
        }
    }
}